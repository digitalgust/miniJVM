//
// Created by Gust on 2020/10/18.
//

#ifndef MINI_JVM_SSL_CLIENT_H
#define MINI_JVM_SSL_CLIENT_H


#if !defined(MBEDTLS_CONFIG_FILE)

#include "mbedtls/config.h"

#else
#include MBEDTLS_CONFIG_FILE
#endif

#if defined(MBEDTLS_PLATFORM_C)

#include "mbedtls/platform.h"

#else
#include <stdio.h>
#include <stdlib.h>
#define mbedtls_time       time
#define mbedtls_time_t     time_t
#define mbedtls_fprintf    fprintf
#define mbedtls_printf     printf
#endif


#include "mbedtls/net_sockets.h"
#include "mbedtls/debug.h"
#include "mbedtls/ssl.h"
#include "mbedtls/entropy.h"
#include "mbedtls/ctr_drbg.h"
#include "mbedtls/error.h"
#include "mbedtls/certs.h"

#include <string.h>


struct _SSLC_Entry {
    mbedtls_net_context server_fd;
    char pers[32];

    mbedtls_entropy_context entropy;
    mbedtls_ctr_drbg_context ctr_drbg;
    mbedtls_ssl_context ssl;
    mbedtls_ssl_config conf;
    mbedtls_x509_crt cacert;
};

typedef struct _SSLC_Entry SSLC_Entry;

int sslc_init(SSLC_Entry *entry);

int sslc_connect(SSLC_Entry *entry, char *hostname, char *port);

int sslc_wrap(SSLC_Entry *entry, int netfd, char *hostname);

int sslc_write(SSLC_Entry *entry, char *buf, int len);

int sslc_read(SSLC_Entry *entry, char *buf, int len);

int sslc_close(SSLC_Entry *entry);


#endif //MINI_JVM_SSL_CLIENT_H
