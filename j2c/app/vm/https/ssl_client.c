
#include "ssl_client.h"
#include "ca_cert.h"

//#define SERVER_PORT "443"
//#define SERVER_NAME "www.baidu.com"
//#define GET_REQUEST "GET / HTTP/1.0\r\n\r\n"

#define DEBUG_LEVEL 1

#if defined(MBEDTLS_DEBUG_C)
#define SSL_LOG_PRINTF(...)     mbedtls_printf(__VA_ARGS__)
#else
#define SSL_LOG_PRINTF(...)     do { } while( 0 )
#endif

static void my_debug(void *ctx, int level,
                     const char *file, int line,
                     const char *str) {
    ((void) level);

    mbedtls_fprintf((FILE *) ctx, "%s:%04d: %s", file, line, str);
    fflush((FILE *) ctx);
}


//    SSL_Entry *entry = malloc(sizeof(SSL_Entry));
//    entry->pers = "ssl_client1";
static char *LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890~!@#$%^&*()_+`{}[]|\\:\"<>?;',./";

int sslc_init(SSLC_Entry *entry) {
    int ret;


    int letters_len = strlen(LETTERS);
    int i;
    for (i = 0; i < sizeof(entry->pers) - 1; i++) {
        entry->pers[1] = LETTERS[rand() % letters_len];
    }
#if defined(MBEDTLS_DEBUG_C)
    mbedtls_debug_set_threshold(DEBUG_LEVEL);
#endif

    /*
     * 0. Initialize the RNG and the session data
     */
    mbedtls_net_init(&entry->server_fd);
    mbedtls_ssl_init(&entry->ssl);
    mbedtls_ssl_config_init(&entry->conf);
    mbedtls_x509_crt_init(&entry->cacert);
    mbedtls_ctr_drbg_init(&entry->ctr_drbg);

    //mbedtls_printf("\n  . Seeding the random number generator...");
    //fflush(stdout);

    mbedtls_entropy_init(&entry->entropy);
    if ((ret = mbedtls_ctr_drbg_seed(&entry->ctr_drbg, mbedtls_entropy_func, &entry->entropy,
                                     (const unsigned char *) entry->pers,
                                     strlen(entry->pers))) != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_ctr_drbg_seed returned %d\n", ret);
        return -1;
    }

    //mbedtls_printf(" ok\n");
    return 0;
}

int sslc_connect(SSLC_Entry *entry, char *hostname, char *port) {

    uint32_t flags;
    /*
     * 0. Initialize certificates
     */
    //mbedtls_printf("  . Loading the CA root certificate ...");
    //fflush(stdout);

//    int ret = mbedtls_x509_crt_parse(&entry->cacert, (const unsigned char *) mbedtls_test_cas_pem,
//                                     mbedtls_test_cas_pem_len);
    ca_crt_rsa[ca_crt_rsa_size - 1] = 0;
    int ret = mbedtls_x509_crt_parse(&entry->cacert, (const unsigned char *) ca_crt_rsa, ca_crt_rsa_size);
    if (ret < 0) {
        mbedtls_printf(" failed\n  !  mbedtls_x509_crt_parse returned -0x%x\n\n", -ret);
        return -1;
    }

    //mbedtls_printf(" ok (%d skipped)\n", ret);

    /*
     * 1. Start the connection
     */
    //mbedtls_printf("  . Connecting to tcp/%s:%s...", hostname, port);
    //fflush(stdout);

    if ((ret = mbedtls_net_connect(&entry->server_fd, hostname,
                                   port, MBEDTLS_NET_PROTO_TCP)) != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_net_connect returned %d\n\n", ret);
//        if( ret != 0 )
//        {
//            char error_buf[100];
//            mbedtls_strerror( ret, error_buf, 100 );
//            mbedtls_printf("Last error was: %d - %s\n\n", ret, error_buf );
//        }
        return -1;
    }

    //mbedtls_printf(" ok\n");

    /*
     * 2. Setup stuff
     */
    //mbedtls_printf("  . Setting up the SSL/TLS structure...");
    //fflush(stdout);

    if ((ret = mbedtls_ssl_config_defaults(&entry->conf,
                                           MBEDTLS_SSL_IS_CLIENT,
                                           MBEDTLS_SSL_TRANSPORT_STREAM,
                                           MBEDTLS_SSL_PRESET_DEFAULT)) != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_ssl_config_defaults returned %d\n\n", ret);
        return -1;
    }

    //mbedtls_printf(" ok\n");

    /* OPTIONAL is not optimal for security,
     * but makes interop easier in this simplified example */
    mbedtls_ssl_conf_authmode(&entry->conf, MBEDTLS_SSL_VERIFY_OPTIONAL);
    mbedtls_ssl_conf_ca_chain(&entry->conf, &entry->cacert, NULL);
    mbedtls_ssl_conf_rng(&entry->conf, mbedtls_ctr_drbg_random, &entry->ctr_drbg);
    mbedtls_ssl_conf_dbg(&entry->conf, my_debug, stdout);

    if ((ret = mbedtls_ssl_setup(&entry->ssl, &entry->conf)) != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_ssl_setup returned %d\n\n", ret);
        return -1;
    }

    if ((ret = mbedtls_ssl_set_hostname(&entry->ssl, hostname)) != 0) {
        mbedtls_printf(" failed\n  ! mbedtls_ssl_set_hostname returned %d\n\n", ret);
        return -1;
    }

    mbedtls_ssl_set_bio(&entry->ssl, &entry->server_fd, mbedtls_net_send, mbedtls_net_recv, NULL);

    /*
     * 4. Handshake
     */
    //mbedtls_printf("  . Performing the SSL/TLS handshake...");
    //flush(stdout);

    while ((ret = mbedtls_ssl_handshake(&entry->ssl)) != 0) {
        if (ret != MBEDTLS_ERR_SSL_WANT_READ && ret != MBEDTLS_ERR_SSL_WANT_WRITE) {
            mbedtls_printf(" failed\n  ! mbedtls_ssl_handshake returned -0x%x\n\n", -ret);
            return -1;
        }
    }

    //mbedtls_printf(" ok\n");

    /*
     * 5. Verify the server certificate
     */
    mbedtls_printf("  . Verifying peer X.509 certificate...");

    /* In real life, we probably want to bail out when ret != 0 */
    if ((flags = mbedtls_ssl_get_verify_result(&entry->ssl)) != 0) {
        char vrfy_buf[512];

        mbedtls_printf(" failed\n");

        mbedtls_x509_crt_verify_info(vrfy_buf, sizeof(vrfy_buf), "  ! ", flags);

        mbedtls_printf("%s\n", vrfy_buf);
    } else {
        mbedtls_printf(" ok\n");
    }
    return 0;
}


/**
 * 包装一个已有连接
 * @param entry
 * @param netfd
 * @param hostname
 *
 * @return
 */
int sslc_wrap(SSLC_Entry *entry, int netfd, char *hostname) {

    uint32_t flags;
    /*
     * 0. Initialize certificates
     */
    SSL_LOG_PRINTF("  . Loading the CA root certificate ... ");
    //fflush(stdout);

//    int ret = mbedtls_x509_crt_parse(&entry->cacert, (const unsigned char *) mbedtls_test_cas_pem,
//                                     mbedtls_test_cas_pem_len);
    //ca_crt_rsa[ca_crt_rsa_size - 1] = 0;
    int ret = mbedtls_x509_crt_parse(&entry->cacert, (const unsigned char *) ca_crt_rsa, ca_crt_rsa_size);
    if (ret < 0) {
        SSL_LOG_PRINTF(" failed\n  !  mbedtls_x509_crt_parse returned -0x%x\n\n", -ret);
        return -1;
    }

    SSL_LOG_PRINTF(" ok (%d skipped)\n", ret);

    /*
     * 1. Start the connection
     */
    SSL_LOG_PRINTF("  . Connecting to tcp/%s... ", hostname);
    //fflush(stdout);

//    if ((ret = mbedtls_net_connect(&entry->server_fd, hostname,
//                                   port, MBEDTLS_NET_PROTO_TCP)) != 0) {
//        SSL_LOG_PRINTF(" failed\n  ! mbedtls_net_connect returned %d\n\n", ret);
//        return -1;
//    }
    entry->server_fd.fd = netfd;

    SSL_LOG_PRINTF(" ok\n");

    /*
     * 2. Setup stuff
     */
    SSL_LOG_PRINTF("  . Setting up the SSL/TLS structure... ");
    //fflush(stdout);

    if ((ret = mbedtls_ssl_config_defaults(&entry->conf,
                                           MBEDTLS_SSL_IS_CLIENT,
                                           MBEDTLS_SSL_TRANSPORT_STREAM,
                                           MBEDTLS_SSL_PRESET_DEFAULT)) != 0) {
        SSL_LOG_PRINTF(" failed\n  ! mbedtls_ssl_config_defaults returned %d\n\n", ret);
        return -1;
    }

    SSL_LOG_PRINTF(" ok\n");

    /* OPTIONAL is not optimal for security,
     * but makes interop easier in this simplified example */
    mbedtls_ssl_conf_authmode(&entry->conf, MBEDTLS_SSL_VERIFY_OPTIONAL);
    mbedtls_ssl_conf_ca_chain(&entry->conf, &entry->cacert, NULL);
    mbedtls_ssl_conf_rng(&entry->conf, mbedtls_ctr_drbg_random, &entry->ctr_drbg);
    mbedtls_ssl_conf_dbg(&entry->conf, my_debug, stdout);

    if ((ret = mbedtls_ssl_setup(&entry->ssl, &entry->conf)) != 0) {
        SSL_LOG_PRINTF(" failed\n  ! mbedtls_ssl_setup returned %d\n\n", ret);
        return -1;
    }

    if ((ret = mbedtls_ssl_set_hostname(&entry->ssl, hostname)) != 0) {
        SSL_LOG_PRINTF(" failed\n  ! mbedtls_ssl_set_hostname returned %d\n\n", ret);
        return -1;
    }

    mbedtls_ssl_set_bio(&entry->ssl, &entry->server_fd, mbedtls_net_send, mbedtls_net_recv, NULL);

    /*
     * 4. Handshake
     */
    SSL_LOG_PRINTF("  . Performing the SSL/TLS handshake... ");
    //flush(stdout);

    while ((ret = mbedtls_ssl_handshake(&entry->ssl)) != 0) {
        if (ret != MBEDTLS_ERR_SSL_WANT_READ && ret != MBEDTLS_ERR_SSL_WANT_WRITE) {
            SSL_LOG_PRINTF(" failed\n  ! mbedtls_ssl_handshake returned -0x%x\n\n", -ret);
            return -1;
        }
    }

    SSL_LOG_PRINTF(" ok\n");

    /*
     * 5. Verify the server certificate
     */
    SSL_LOG_PRINTF("  . Verifying peer X.509 certificate... ");

    /* In real life, we probably want to bail out when ret != 0 */
    if ((flags = mbedtls_ssl_get_verify_result(&entry->ssl)) != 0) {
        char vrfy_buf[512];

        SSL_LOG_PRINTF(" failed\n");

        mbedtls_x509_crt_verify_info(vrfy_buf, sizeof(vrfy_buf), "  ! ", flags);

        SSL_LOG_PRINTF("%s\n", vrfy_buf);
    } else {
        SSL_LOG_PRINTF(" ok\n");
    }

    return 0;
}

int sslc_write(SSLC_Entry *entry, char *buf, int len) {
    /*
     * 3. Write the GET request
     */
    //mbedtls_printf("  > Write to server:");
    //fflush(stdout);

    int ret;

    while ((ret = mbedtls_ssl_write(&entry->ssl, (const unsigned char *) buf, len)) <= 0) {
        if (ret != MBEDTLS_ERR_SSL_WANT_READ && ret != MBEDTLS_ERR_SSL_WANT_WRITE) {
            mbedtls_printf(" failed\n  ! mbedtls_ssl_write returned %d\n\n", ret);
            return -1;
        }
    }

    len = ret;
    //mbedtls_printf(" %d bytes written\n\n%s", len, (char *) buf);

    return ret;
}

int sslc_read(SSLC_Entry *entry, char *buf, int len) {
    /*
     * 7. Read the HTTP response
     */
    //mbedtls_printf("  < Read from server:");
    //fflush(stdout);

    int ret;
    do { ;
        memset(buf, 0, len);
        ret = mbedtls_ssl_read(&entry->ssl, (unsigned char *) buf, len);

        if (ret == MBEDTLS_ERR_SSL_WANT_READ || ret == MBEDTLS_ERR_SSL_WANT_WRITE)
            continue;

        if (ret == MBEDTLS_ERR_SSL_PEER_CLOSE_NOTIFY) {
            ret = -1;
            break;
        }
        if (ret < 0) {
            mbedtls_printf("failed\n  ! mbedtls_ssl_read returned %d\n\n", ret);
            ret = -1;
            break;
        }

        if (ret == 0) {
            mbedtls_printf("\n\nEOF\n\n");
            break;
        }

        break;
    } while (1);

    return ret;
}


int sslc_close(SSLC_Entry *entry) {
    int ret = 0;
    mbedtls_ssl_close_notify(&entry->ssl);
    mbedtls_net_free(&entry->server_fd);

    mbedtls_x509_crt_free(&entry->cacert);
    mbedtls_ssl_free(&entry->ssl);
    mbedtls_ssl_config_free(&entry->conf);
    mbedtls_ctr_drbg_free(&entry->ctr_drbg);
    mbedtls_entropy_free(&entry->entropy);

    return (ret);
}
