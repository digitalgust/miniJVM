//
// Created by gust on 2017/9/1.
//

#include <sys/stat.h>
#include <string.h>   // NULL and possibly memcpy, memset
#include <locale.h>
#include "jvm.h"
#include "garbage.h"
#include "jvm_util.h"
#include "../utils/miniz_wrapper.h"


#ifdef __cplusplus
extern "C" {
#endif


#define  SOCK_OP_TYPE_NON_BLOCK   0
#define  SOCK_OP_TYPE_REUSEADDR   1
#define  SOCK_OP_TYPE_RCVBUF   2
#define  SOCK_OP_TYPE_SNDBUF   3
#define  SOCK_OP_TYPE_KEEPALIVE   4
#define  SOCK_OP_TYPE_LINGER   5
#define  SOCK_OP_TYPE_TIMEOUT   6

#define  SOCK_OP_VAL_NON_BLOCK   1
#define  SOCK_OP_VAL_BLOCK   0
#define  SOCK_OP_VAL_NON_REUSEADDR   1
#define  SOCK_OP_VAL_REUSEADDR   0


#if __JVM_OS_VS__ || __JVM_OS_MINGW__

#include <WinSock2.h>
#include <Ws2tcpip.h>

#include <wspiapi.h>

#pragma comment(lib, "Ws2_32.lib")
#else

#include <sys/types.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <unistd.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <fcntl.h>

#endif

#if __JVM_OS_VS__
#include "../utils/dirent_win.h"
#include <direct.h>
#include <io.h>
#else

#include <dirent.h>
#include "ctype.h"

#endif

#include "ssl_client.h"
#include "../utils/https/mbedtls/include/mbedtls/net_sockets.h"

#if __JVM_OS_MINGW__

#ifndef InetNtopA

c8 *inet_ntop(s32 af, const void *src, c8 *dst, socklen_t size);

#endif

#ifndef InetPtonA

s32 inet_pton(s32 af, const c8 *src, void *dst);

#endif

#endif //__JVM_OS_MINGW__

typedef struct _VmSock {
    mbedtls_net_context contex;
    //
    s32 rcv_time_out;
    u8 non_block;
    u8 reuseaddr;
    char hostname[256]; //domain name length max 253
    char hostport[6];  //port the max is 65535
} VmSock;

//=================================  socket  ====================================
s32 sock_option(VmSock *vmsock, s32 opType, s32 opValue, s32 opValue2) {
    s32 ret = 0;
    switch (opType) {
        case SOCK_OP_TYPE_NON_BLOCK: {// blocking setting
            if (opValue) {
                mbedtls_net_set_nonblock(&vmsock->contex);
                vmsock->non_block = 1;
            } else {
                mbedtls_net_set_block(&vmsock->contex);
            }
            break;
        }
        case SOCK_OP_TYPE_REUSEADDR: {//
            s32 x = 1;
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_REUSEADDR, (c8 *) &x, sizeof(x));
            vmsock->reuseaddr = 1;
            break;
        }
        case SOCK_OP_TYPE_RCVBUF: {// buffer configuration
            s32 nVal = opValue;// set to opValue K
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVBUF, (const c8 *) &nVal, sizeof(nVal));
            break;
        }
        case SOCK_OP_TYPE_SNDBUF: {// buffer configuration
            s32 nVal = opValue;// set to opValue K
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_SNDBUF, (const c8 *) &nVal, sizeof(nVal));
            break;
        }
        case SOCK_OP_TYPE_TIMEOUT: {
            vmsock->rcv_time_out = opValue;
#if __JVM_OS_MINGW__ || __JVM_OS_VS__
            s32 nTime = opValue;
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVTIMEO, (const c8 *) &nTime, sizeof(nTime));
#else
            struct timeval timeout = {opValue / 1000, (opValue % 1000) * 1000};
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
#endif
            break;
        }
        case SOCK_OP_TYPE_LINGER: {
            struct {
                u16 l_onoff;
                u16 l_linger;
            } m_sLinger;
            // (Allow lingering when closesocket() is called, but there is still data that has not been fully sent)
            // If m_sLinger.l_onoff = 0, it functions the same as in 2.)
            m_sLinger.l_onoff = opValue;
            m_sLinger.l_linger = opValue2;// The allowed lingering time is 5 seconds
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVTIMEO, (const c8 *) &m_sLinger, sizeof(m_sLinger));
            break;
        }
        case SOCK_OP_TYPE_KEEPALIVE: {
            s32 val = opValue;
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVTIMEO, (const c8 *) &val, sizeof(val));
            break;
        }
    }
    return ret;
}


s32 sock_get_option(VmSock *vmsock, s32 opType) {
    s32 ret = 0;
    socklen_t len;

    switch (opType) {
        case SOCK_OP_TYPE_NON_BLOCK: {// blocking configuration
#if __JVM_OS_MINGW__ || __JVM_OS_VS__
            u_long flags = 1;
            ret = NO_ERROR == ioctlsocket(vmsock->contex.fd, FIONBIO, &flags);
#else
            s32 flags;
            if ((flags = fcntl(vmsock->contex.fd, F_GETFL, NULL)) < 0) {
                ret = -1;
            } else {
                ret = (flags & O_NONBLOCK);
            }
#endif
            break;
        }
        case SOCK_OP_TYPE_REUSEADDR: {//
            len = sizeof(ret);
            getsockopt(vmsock->contex.fd, SOL_SOCKET, SO_REUSEADDR, (void *) &ret, &len);

            break;
        }
        case SOCK_OP_TYPE_RCVBUF: {
            len = sizeof(ret);
            getsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVBUF, (void *) &ret, &len);
            break;
        }
        case SOCK_OP_TYPE_SNDBUF: {// buffer configuration
            len = sizeof(ret);
            getsockopt(vmsock->contex.fd, SOL_SOCKET, SO_SNDBUF, (void *) &ret, &len);
            break;
        }
        case SOCK_OP_TYPE_TIMEOUT: {

#if __JVM_OS_MINGW__ || __JVM_OS_VS__
            len = sizeof(ret);
            getsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVTIMEO, (void *) &ret, &len);
#else
            struct timeval timeout = {0, 0};
            len = sizeof(timeout);
            getsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, &len);
            ret = timeout.tv_sec * 1000 + timeout.tv_usec / 1000;
#endif
            break;
        }
        case SOCK_OP_TYPE_LINGER: {
            struct {
                u16 l_onoff;
                u16 l_linger;
            } m_sLinger;
            // (Allow lingering when closesocket() is called, but there is still data that has not been fully sent)
            // If m_sLinger.l_onoff = 0, it behaves the same as in 2.)
            m_sLinger.l_onoff = 0;
            m_sLinger.l_linger = 0;// The allowed lingering time is 5 seconds
            len = sizeof(m_sLinger);
            getsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVTIMEO, (void *) &m_sLinger, &len);
            ret = *((s32 *) &m_sLinger);
            break;
        }
        case SOCK_OP_TYPE_KEEPALIVE: {
            len = sizeof(ret);
            getsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVTIMEO, (void *) &ret, &len);
            break;
        }
    }
    return ret;
}


s32 host_2_ip(c8 *hostname, c8 *buf, s32 buflen) {
#if __JVM_OS_VS__ || __JVM_OS_MINGW__
    WSADATA wsaData;
    WSAStartup(MAKEWORD(1, 1), &wsaData);
#endif  /*  WIN32  */
    struct addrinfo hints;
    struct addrinfo *result, *rp;
    s32 s;
    struct sockaddr_in *ipv4;
    struct sockaddr_in6 *ipv6;

    /* Obtain address(es) matching host/port */
    memset(&hints, 0, sizeof(struct addrinfo));
    hints.ai_family = AF_UNSPEC;    /* Allow IPv4 or IPv6 */
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = AI_CANONNAME;
    hints.ai_protocol = IPPROTO_TCP;

    s = getaddrinfo(hostname, NULL, &hints, &result);
    if (s != 0) {
        jvm_printf("getaddrinfo: %s\n", gai_strerror(s));
        return -1;
    }

    for (rp = result; rp != NULL; rp = rp->ai_next) {
        switch (rp->ai_family) {
            case AF_INET:
                ipv4 = (struct sockaddr_in *) rp->ai_addr;
                inet_ntop(rp->ai_family, &ipv4->sin_addr, buf, buflen);
                break;
            case AF_INET6:
                ipv6 = (struct sockaddr_in6 *) rp->ai_addr;
                inet_ntop(rp->ai_family, &ipv6->sin6_addr, buf, buflen);
                break;
        }

        //printf("[IPv%d]%s\n", rp->ai_family == AF_INET ? 4 : 6, buf);
    }

    /* No longer needed */
    freeaddrinfo(result);
    return 0;
}


extern s32 os_mkdir(const c8 *path);
//=================================  native  ====================================


s32 org_mini_net_SocketNative_open0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    jthread_block_enter(runtime);
    Instance *vmarr = jarray_create_by_type_index(runtime, sizeof(VmSock), DATATYPE_BYTE);
    mbedtls_net_context *ctx = &((VmSock *) vmarr->arr_body)->contex;
    mbedtls_net_init(ctx);
    jthread_block_exit(runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_open0  \n");
#endif
    push_ref(stack, vmarr);
    return 0;
}

s32 org_mini_net_SocketNative_bind0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    Instance *host = localvar_getRefer(runtime->localvar, 1);
    Instance *port = localvar_getRefer(runtime->localvar, 2);
    s32 proto = localvar_getInt(runtime->localvar, 3);
    s32 ret = -1;
    if (vmarr && host && port) {
        VmSock *vmsock = (VmSock *) vmarr->arr_body;
        mbedtls_net_context *ctx = &vmsock->contex;
        jthread_block_enter(runtime);
        ret = mbedtls_net_bind(ctx, strlen(host->arr_body) == 0 ? NULL : host->arr_body, port->arr_body, proto);
        if (ret >= 0)ret = mbedtls_net_set_nonblock(ctx);//set as non_block , for vm destroy
        jthread_block_exit(runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
        invoke_deepth(runtime);
        jvm_printf("org_mini_net_SocketNative_open0  \n");
#endif
    }
    push_int(stack, ret < 0 ? -1 : 0);
    return 0;
}

s32 org_mini_net_SocketNative_accept0(Runtime *runtime, JClass *clazz) {
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    if (vmarr) {
        mbedtls_net_context *ctx = &((VmSock *) vmarr->arr_body)->contex;
        Instance *cltarr = jarray_create_by_type_index(runtime, sizeof(VmSock), DATATYPE_BYTE);
        VmSock *cltsock = (VmSock *) cltarr->arr_body;
        gc_obj_hold(runtime->jvm->collector, cltarr);
        s32 ret = 0;
        while (1) {
            jthread_block_enter(runtime);
            ret = mbedtls_net_accept(ctx, &cltsock->contex, NULL, 0, NULL);
            jthread_block_exit(runtime);
            if (runtime->thrd_info->is_interrupt) {//vm notify thread destroy
                ret = -1;
                break;
            }
            if (ret == MBEDTLS_ERR_SSL_WANT_READ) {
                thrd_yield();
                mbedtls_net_usleep(10000);//10ms
                continue;
            } else if (ret < 0) {
                ret = -1;
                break;
            } else {
                break;
            }
        }
        gc_obj_release(runtime->jvm->collector, cltarr);
        push_ref(runtime->stack, ret < 0 ? NULL : cltarr);
    } else {
        push_ref(runtime->stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_accept0  \n");
#endif
    return 0;
}


s32 org_mini_net_SocketNative_connect0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    Instance *host = localvar_getRefer(runtime->localvar, 1);
    Instance *port = localvar_getRefer(runtime->localvar, 2);
    s32 proto = localvar_getInt(runtime->localvar, 3);

    s32 ret = -1;
    if (vmarr && host && port) {
        VmSock *vmsock = (VmSock *) vmarr->arr_body;
        mbedtls_net_context *ctx = &vmsock->contex;

        s32 hostlen = host->arr_length;
        if (hostlen > sizeof(vmsock->hostname)) {
            hostlen = sizeof(vmsock->hostname);
        }
        memcpy(&vmsock->hostname, host->arr_body, hostlen);//copy with 0
        memcpy(&vmsock->hostport, port->arr_body, port->arr_length);
        jthread_block_enter(runtime);
        ret = mbedtls_net_connect(ctx, host->arr_body, port->arr_body, proto);
        jthread_block_exit(runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
        invoke_deepth(runtime);
        jvm_printf("org_mini_net_SocketNative_open0  \n");
#endif
    }
    push_int(stack, ret < 0 ? -1 : 0);
    return 0;
}

/**
 * non_block and block socket both not block
 * if vm notify destroy then terminate read and return -1
 * if receive timeout is set, then timeout return -2
 * if error return -1
 * if receive bytes return len
 *
 * @param vmsock
 * @param buf
 * @param count
 * @param runtime
 * @return
 */
static s32 sock_recv(VmSock *vmsock, u8 *buf, s32 count, Runtime *runtime) {
    s32 ret;
    while (1) {
        jthread_block_enter(runtime);
        if (vmsock->non_block) {
            ret = mbedtls_net_recv(&vmsock->contex, buf, count);
        } else {
            ret = mbedtls_net_recv_timeout(&vmsock->contex, buf, count, vmsock->rcv_time_out ? vmsock->rcv_time_out : 100);
        }
        jthread_block_exit(runtime);
        if (runtime->thrd_info->is_interrupt) {//vm waiting for destroy
            ret = -1;
            break;
        }
        if (ret == MBEDTLS_ERR_SSL_TIMEOUT) {
            if (vmsock->rcv_time_out) {
                ret = -2;
                break;
            }
            thrd_yield();
            continue;
        } else if (ret == MBEDTLS_ERR_SSL_WANT_READ) {//nonblock
            ret = 0;
            break;
        } else if (ret <= 0) {
            ret = -1;
            break;
        } else {
            break;
        }
    }
    return ret;
}

s32 org_mini_net_SocketNative_readBuf(Runtime *runtime, JClass *clazz) {
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    Instance *jbyte_arr = localvar_getRefer(runtime->localvar, 1);
    s32 offset = localvar_getInt(runtime->localvar, 2);
    s32 count = localvar_getInt(runtime->localvar, 3);
    s32 ret = -1;
    if (vmarr && jbyte_arr) {
        VmSock *vmsock = (VmSock *) vmarr->arr_body;

        jthread_block_enter(runtime);
        ret = sock_recv(vmsock, (u8 *) jbyte_arr->arr_body + offset, count, runtime);
        jthread_block_exit(runtime);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_readBuf  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_readByte(Runtime *runtime, JClass *clazz) {
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    s32 ret = -1;
    u8 b = 0;
    if (vmarr) {
        VmSock *vmsock = (VmSock *) vmarr->arr_body;
        jthread_block_enter(runtime);
        ret = sock_recv(vmsock, &b, 1, runtime);
        jthread_block_exit(runtime);
    }
    push_int(runtime->stack, ret < 0 ? ret : (u8) b);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_readByte  \n");
#endif
    return 0;
}


s32 org_mini_net_SocketNative_writeBuf(Runtime *runtime, JClass *clazz) {
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    s32 offset = localvar_getInt(runtime->localvar, 2);
    s32 count = localvar_getInt(runtime->localvar, 3);
    s32 ret = -1;
    if (vmarr && jbyte_arr) {
        VmSock *vmsock = (VmSock *) vmarr->arr_body;
        mbedtls_net_context *ctx = &vmsock->contex;
        jthread_block_enter(runtime);
        ret = mbedtls_net_send(ctx, (const u8 *) jbyte_arr->arr_body + offset, count);
        jthread_block_exit(runtime);
        if (ret == MBEDTLS_ERR_SSL_WANT_WRITE) {
            ret = 0;
        } else if (ret < 0) {
            ret = -1;
        }
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_writeBuf  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_writeByte(Runtime *runtime, JClass *clazz) {
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    s32 val = localvar_getInt(runtime->localvar, 1);
    u8 b = (u8) val;
    s32 ret = -1;
    if (vmarr) {
        VmSock *vmsock = (VmSock *) vmarr->arr_body;
        mbedtls_net_context *ctx = &vmsock->contex;
        jthread_block_enter(runtime);
        ret = mbedtls_net_send(ctx, &b, 1);
        jthread_block_exit(runtime);
        if (ret == MBEDTLS_ERR_SSL_WANT_WRITE) {
            ret = 0;
        } else if (ret < 0) {
            ret = -1;
        }
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_writeByte  \n");
#endif
    push_int(runtime->stack, ret);
    return 0;
}

s32 org_mini_net_SocketNative_available0(Runtime *runtime, JClass *clazz) {
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_available0  \n");
#endif
    push_int(runtime->stack, 0);
    return 0;
}

s32 org_mini_net_SocketNative_close0(Runtime *runtime, JClass *clazz) {
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    VmSock *vmsock = (VmSock *) vmarr->arr_body;
    mbedtls_net_context *ctx = &vmsock->contex;
    mbedtls_net_free(ctx);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_close0  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_setOption0(Runtime *runtime, JClass *clazz) {
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    s32 type = localvar_getInt(runtime->localvar, 1);
    s32 val = localvar_getInt(runtime->localvar, 2);
    s32 val2 = localvar_getInt(runtime->localvar, 3);
    s32 ret = -1;
    if (vmarr) {
        ret = sock_option((VmSock *) vmarr->arr_body, type, val, val2);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_setOption0  \n");
#endif
    return 0;
}


s32 org_mini_net_SocketNative_getOption0(Runtime *runtime, JClass *clazz) {
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    s32 type = localvar_getInt(runtime->localvar, 1);

    s32 ret = -1;
    if (vmarr) {
        ret = sock_get_option((VmSock *) vmarr->arr_body, type);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_getOption0  \n");
#endif
    return 0;
}


s32 org_mini_net_SocketNative_getSockAddr(Runtime *runtime, JClass *clazz) {
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    s32 mode = localvar_getInt(runtime->localvar, 1);
    if (vmarr) {
        VmSock *vmsock = (VmSock *) vmarr->arr_body;
        struct sockaddr_storage sock;
        socklen_t slen = sizeof(sock);
        if (mode == 0) {
            getpeername(vmsock->contex.fd, (struct sockaddr *) &sock, &slen);
        } else if (mode == 1) {
            getsockname(vmsock->contex.fd, (struct sockaddr *) &sock, &slen);
        }

        struct sockaddr_in *ipv4 = NULL;
        struct sockaddr_in6 *ipv6 = NULL;
        c8 ipAddr[INET6_ADDRSTRLEN];// save the address in dotted decimal format
        s32 port = -1;
        if (sock.ss_family == AF_INET) {// IPv4 address
            ipv4 = ((struct sockaddr_in *) &sock);
            port = ipv4->sin_port;
            inet_ntop(AF_INET, &ipv4->sin_addr, ipAddr, sizeof(ipAddr));
        } else {//IPv6 address
            ipv6 = ((struct sockaddr_in6 *) &sock);
            port = ipv6->sin6_port;
            inet_ntop(AF_INET6, &ipv6->sin6_addr, ipAddr, sizeof(ipAddr));
        }

        Utf8String *ustr = utf8_create();
        utf8_append_c(ustr, ipAddr);
        utf8_append_c(ustr, ":");
        utf8_append_s64(ustr, port, 10);
        Instance *jstr = jstring_create(ustr, runtime);
        utf8_destroy(ustr);
        push_ref(runtime->stack, jstr);
    } else {
        push_ref(runtime->stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_getSockAddr  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_host2ip(Runtime *runtime, JClass *clazz) {
    Instance *host = (Instance *) localvar_getRefer(runtime->localvar, 0);

    Instance *jbyte_arr = NULL;
    if (host) {

        c8 buf[50];
        jthread_block_enter(runtime);
        s32 ret = host_2_ip(host->arr_body, buf, sizeof(buf));
        jthread_block_exit(runtime);
        if (ret >= 0) {
            s32 buflen = strlen(buf);
            jbyte_arr = jarray_create_by_type_index(runtime, buflen, DATATYPE_BYTE);
            memmove(jbyte_arr->arr_body, buf, buflen);
        }
    }
    push_ref(runtime->stack, jbyte_arr);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_host2ip4  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_sslc_construct_entry(Runtime *runtime, JClass *clazz) {
    Instance *jbyte_arr = jarray_create_by_type_index(runtime, sizeof(struct _SSLC_Entry), DATATYPE_BYTE);
    push_ref(runtime->stack, jbyte_arr);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_sslc_construct_entry  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_sslc_init(Runtime *runtime, JClass *clazz) {
    s32 v = 0;
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    if (jbyte_arr) {
        jthread_block_enter(runtime);
        v = sslc_init((SSLC_Entry *) jbyte_arr->arr_body);
        jthread_block_exit(runtime);
    }
    push_int(runtime->stack, v);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_sslc_init  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_sslc_wrap(Runtime *runtime, JClass *clazz) {
    s32 ret = -1;
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *conn_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    Instance *host_arr = (Instance *) localvar_getRefer(runtime->localvar, 2);
    if (jbyte_arr && conn_arr && host_arr) {
        VmSock *vmsock = (VmSock *) conn_arr->arr_body;
        mbedtls_net_context *ctx = &vmsock->contex;
        ret = sslc_wrap((SSLC_Entry *) jbyte_arr->arr_body, ctx->fd, host_arr->arr_body);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_http_open  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_sslc_connect(Runtime *runtime, JClass *clazz) {
    s32 ret = -1;
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *host_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    Instance *port_arr = (Instance *) localvar_getRefer(runtime->localvar, 2);
    if (jbyte_arr && host_arr && port_arr) {
        jthread_block_enter(runtime);
        ret = sslc_connect((SSLC_Entry *) jbyte_arr->arr_body, host_arr->arr_body, port_arr->arr_body);
        jthread_block_exit(runtime);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_http_open  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_sslc_close(Runtime *runtime, JClass *clazz) {
    s32 ret = -1;
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    if (jbyte_arr) {
        jthread_block_enter(runtime);
        ret = sslc_close((SSLC_Entry *) jbyte_arr->arr_body);
        jthread_block_exit(runtime);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_http_close  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_sslc_read(Runtime *runtime, JClass *clazz) {
    s32 ret = -1;
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *data_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    s32 offset = localvar_getInt(runtime->localvar, 2);
    s32 len = localvar_getInt(runtime->localvar, 3);
    if (jbyte_arr && data_arr) {
        jthread_block_enter(runtime);
        ret = sslc_read((SSLC_Entry *) jbyte_arr->arr_body, data_arr->arr_body + offset, len);
        jthread_block_exit(runtime);
    }
    push_int(runtime->stack, ret < 0 ? -1 : ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_http_init  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_sslc_write(Runtime *runtime, JClass *clazz) {
    s32 ret = -1;
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *data_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    s32 offset = localvar_getInt(runtime->localvar, 2);
    s32 len = localvar_getInt(runtime->localvar, 3);
    if (jbyte_arr && data_arr) {
        jthread_block_enter(runtime);
        ret = sslc_write((SSLC_Entry *) jbyte_arr->arr_body, data_arr->arr_body + offset, len);
        jthread_block_exit(runtime);
    }
    push_int(runtime->stack, ret < 0 ? -1 : ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_http_close  \n");
#endif
    return 0;
}

//------------------------------------------------------------------------------------
//                              File
//------------------------------------------------------------------------------------

s32 isDir(Utf8String *path) {
    struct stat buf;
    stat(utf8_cstr(path), &buf);
    s32 a = S_ISDIR(buf.st_mode);
    return a;
}

extern Utf8String *os_get_tmp_dir();

extern s32 conv_platform_encoding_2_utf8(Utf8String *dst, const c8 *src);

extern s32 conv_utf8_2_platform_encoding(ByteBuf *dst, Utf8String *src);

s32 org_mini_fs_InnerFile_openFile(Runtime *runtime, JClass *clazz) {
    Instance *name_arr = localvar_getRefer(runtime->localvar, 0);
    Instance *mode_arr = localvar_getRefer(runtime->localvar, 1);
    if (name_arr) {
        Utf8String *filepath = utf8_create_c(name_arr->arr_body);
        ByteBuf *platformPath = bytebuf_create(0);
        conv_utf8_2_platform_encoding(platformPath, filepath);

        FILE *fd = fopen(platformPath->buf, mode_arr->arr_body);
        push_long(runtime->stack, (s64) (intptr_t) fd);

        bytebuf_destroy(platformPath);
        utf8_destroy(filepath);
    } else {
        push_long(runtime->stack, 0);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_openFile  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_openFD(Runtime *runtime, JClass *clazz) {
    s32 pfd = localvar_getInt(runtime->localvar, 0);
    Instance *mode_arr = localvar_getRefer(runtime->localvar, 1);
    if (pfd >= 0) {
        FILE *fd = fdopen(pfd, mode_arr->arr_body);
        push_long(runtime->stack, (s64) (intptr_t) fd);
    } else {
        push_long(runtime->stack, 0);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_openFD  \n");
#endif
    return 0;
}

extern s32 os_fileno(FILE *fd);

s32 org_mini_fs_InnerFile_fileno(Runtime *runtime, JClass *clazz) {
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    FILE *fd = (FILE *) (intptr_t) l2d.l;
    if (fd) {
        s32 fileno = os_fileno(fd);
        push_int(runtime->stack, fileno);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_openFile  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_closeFile(Runtime *runtime, JClass *clazz) {
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    FILE *fd = (FILE *) (intptr_t) l2d.l;
    s32 ret = -1;
    if (fd) {
        ret = fclose(fd);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_closeFile  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_read0(Runtime *runtime, JClass *clazz) {
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    FILE *fd = (FILE *) (intptr_t) l2d.l;
    s32 ret = -1;
    if (fd) {
        ret = fgetc(fd);
        if (ret == EOF) {
            push_int(runtime->stack, -1);
        } else {
            push_int(runtime->stack, ret);
        }
    } else {
        push_int(runtime->stack, ret);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_read0  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_write0(Runtime *runtime, JClass *clazz) {
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    FILE *fd = (FILE *) (intptr_t) l2d.l;
    u8 byte = (u8) localvar_getInt(runtime->localvar, 2);
    s32 ret = -1;
    if (fd) {
        ret = fputc(byte, fd);
        if (ret == EOF) {
            push_int(runtime->stack, -1);
        } else {
            push_int(runtime->stack, byte);
        }
    } else {
        push_int(runtime->stack, ret);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_write0  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_readbuf(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    FILE *fd = (FILE *) (intptr_t) l2d.l;
    Instance *bytes_arr = localvar_getRefer(runtime->localvar, pos++);
    s32 offset = localvar_getInt(runtime->localvar, pos++);
    s32 len = localvar_getInt(runtime->localvar, pos++);
    s32 ret = -1;
    if (fd && bytes_arr) {
        ret = (s32) fread(bytes_arr->arr_body + offset, 1, len, fd);
    }
    if (ret == 0) {
        ret = -1;
    }
    push_int(runtime->stack, ret);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_readbuf  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_writebuf(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    FILE *fd = (FILE *) (intptr_t) l2d.l;
    Instance *bytes_arr = localvar_getRefer(runtime->localvar, pos++);
    s32 offset = localvar_getInt(runtime->localvar, pos++);
    s32 len = localvar_getInt(runtime->localvar, pos++);
    s32 ret = -1;
    if (fd && bytes_arr) {
        ret = (s32) fwrite(bytes_arr->arr_body + offset, 1, len, fd);
        if (ret == 0) {
            ret = -1;
        }
        push_int(runtime->stack, ret);
    } else {
        push_int(runtime->stack, ret);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_writebuf  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_seek0(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    FILE *fd = (FILE *) (intptr_t) l2d.l;
    l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s64 filepos = l2d.l;
    s32 ret = -1;
    if (fd) {
        ret = fseek(fd, (long) filepos, SEEK_SET);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_seek0  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_available0(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    FILE *fd = (FILE *) (intptr_t) l2d.l;

    s32 cur = 0, end = 0;
    if (fd) {
        cur = ftell(fd);
        fseek(fd, (long) 0, SEEK_END);
        end = ftell(fd);
        fseek(fd, (long) cur, SEEK_SET);
    }
    push_int(runtime->stack, end - cur);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_available0  \n");
#endif
    return 0;
}

extern void os_set_file_length(FILE *file, s64 len);

s32 org_mini_fs_InnerFile_setLength0(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    FILE *fd = (FILE *) (intptr_t) l2d.l;
    l2d.l = localvar_getLong(runtime->localvar, pos);
    pos += 2;
    s64 filelen = l2d.l;
    s32 ret = 0;
    if (fd) {
        s64 pos;
        ret = fseek(fd, 0, SEEK_END);
        if (!ret) {
            ret = ftell(fd);
            if (!ret) {
                if (filelen < pos) {
                    os_set_file_length(fd, filelen);
                } else {
                    u8 d = 0;
                    s64 i, imax = filelen - pos;
                    for (i = 0; i < imax; i++) {
                        fwrite(&d, 1, 1, fd);
                    }
                    fflush(fd);
                }
            }
        }
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_setLength0  \n");
#endif
    return 0;
}


s32 org_mini_fs_InnerFile_flush0(Runtime *runtime, JClass *clazz) {
    Long2Double l2d;
    l2d.l = localvar_getLong(runtime->localvar, 0);
    FILE *fd = (FILE *) (intptr_t) l2d.l;
    s32 ret = -1;
    if (fd) {
        ret = fflush(fd);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_flush0  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_loadFS(Runtime *runtime, JClass *clazz) {
    Instance *name_arr = localvar_getRefer(runtime->localvar, 0);
    Instance *fd = localvar_getRefer(runtime->localvar, 1);
    s32 ret = RUNTIME_STATUS_NORMAL;
    if (name_arr) {
        Utf8String *filepath = utf8_create_part_c(name_arr->arr_body, 0, name_arr->arr_length);
        struct stat buf;
        ByteBuf *platformPath = bytebuf_create(0);
        s32 len = conv_utf8_2_platform_encoding(platformPath, filepath);
        if (len < 0) {
            Instance *exception = exception_create(JVM_EXCEPTION_ILLEGALARGUMENT, runtime);
            push_ref(runtime->stack, (__refer) exception);
            ret = RUNTIME_STATUS_EXCEPTION;
        } else {
            s32 state = stat(platformPath->buf, &buf);
            s32 a = S_ISDIR(buf.st_mode);
            if (state == 0) {
                c8 *className = "org/mini/fs/InnerFileStat";
                c8 *ptr;
                ptr = getFieldPtr_byName_c(fd, className, "st_dev", "I", runtime);
                setFieldInt(ptr, buf.st_dev);
                ptr = getFieldPtr_byName_c(fd, className, "st_ino", "S", runtime);
                setFieldShort(ptr, buf.st_ino);
                ptr = getFieldPtr_byName_c(fd, className, "st_mode", "S", runtime);
                setFieldShort(ptr, buf.st_mode);
                ptr = getFieldPtr_byName_c(fd, className, "st_nlink", "S", runtime);
                setFieldShort(ptr, buf.st_nlink);
                ptr = getFieldPtr_byName_c(fd, className, "st_uid", "S", runtime);
                setFieldShort(ptr, buf.st_uid);
                ptr = getFieldPtr_byName_c(fd, className, "st_gid", "S", runtime);
                setFieldShort(ptr, buf.st_gid);
                ptr = getFieldPtr_byName_c(fd, className, "st_rdev", "S", runtime);
                setFieldShort(ptr, buf.st_rdev);
                ptr = getFieldPtr_byName_c(fd, className, "st_size", "J", runtime);
                setFieldLong(ptr, buf.st_size);
                ptr = getFieldPtr_byName_c(fd, className, "st_atime", "J", runtime);
                setFieldLong(ptr, buf.st_atime);
                ptr = getFieldPtr_byName_c(fd, className, "st_mtime", "J", runtime);
                setFieldLong(ptr, buf.st_mtime);
                ptr = getFieldPtr_byName_c(fd, className, "st_ctime", "J", runtime);
                setFieldLong(ptr, buf.st_ctime);
                ptr = getFieldPtr_byName_c(fd, className, "exists", "Z", runtime);
                setFieldByte(ptr, 1);
            }
            bytebuf_destroy(platformPath);
            utf8_destroy(filepath);
            push_int(runtime->stack, state);
        }
    }
    return ret;
}

s32 org_mini_fs_InnerFile_listDir(Runtime *runtime, JClass *clazz) {
    Instance *name_arr = localvar_getRefer(runtime->localvar, 0);
    if (name_arr) {
        Utf8String *filepath = utf8_create_part_c(name_arr->arr_body, 0, name_arr->arr_length);

        ArrayList *files = arraylist_create(0);
        DIR *dirp;
        struct dirent *dp;
        ByteBuf *platformPath = bytebuf_create(0);
        conv_utf8_2_platform_encoding(platformPath, filepath);
        dirp = opendir(platformPath->buf); // pointer to the opened directory
        if (dirp) {
            while ((dp = readdir(dirp)) != NULL) { // read the directory through the directory pointer
                if (strcmp(dp->d_name, ".") == 0) {
                    continue;
                }
                if (strcmp(dp->d_name, "..") == 0) {
                    continue;
                }
//                Utf8String *ustr = utf8_create_c(dp->d_name);
                Utf8String *ustr = utf8_create();
                conv_platform_encoding_2_utf8(ustr, dp->d_name);//
                Instance *jstr = jstring_create(ustr, runtime);
                instance_hold_to_thread(jstr, runtime);
                utf8_destroy(ustr);
                arraylist_push_back(files, jstr);
            }
            (void) closedir(dirp); // close the directory

            s32 i;
            Utf8String *ustr = utf8_create_c(STR_CLASS_JAVA_LANG_STRING);
            Instance *jarr = jarray_create_by_type_name(runtime, files->length, ustr, NULL);
            utf8_destroy(ustr);
            for (i = 0; i < files->length; i++) {
                __refer ref = arraylist_get_value(files, i);
                instance_release_from_thread(ref, runtime);
                jarray_set_field(jarr, i, (intptr_t) ref);
            }
            push_ref(runtime->stack, jarr);
        } else {
            push_ref(runtime->stack, NULL);
        }
        bytebuf_destroy(platformPath);
        arraylist_destroy(files);
        utf8_destroy(filepath);
    } else {
        push_ref(runtime->stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_listDir  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_listWinDrivers(Runtime *runtime, JClass *clazz) {
#if  defined(__JVM_OS_MAC__) || defined(__JVM_OS_LINUX__)

    push_ref(runtime->stack, NULL);

#else

#define MAX_PATH_BUF_LEN 120
    DWORD mydrives = MAX_PATH_BUF_LEN;// buffer length
    c8 lpBuffer[MAX_PATH_BUF_LEN];// buffer for drive string storage
    DWORD fillLen = GetLogicalDriveStrings(mydrives, lpBuffer);

    lpBuffer[fillLen] = '\0';
    s64 i;
    for (i = 0; i < fillLen; i++) {
        if (lpBuffer[i] == 0) {
            lpBuffer[i] = 0x20;
        }
    }
    Instance *jstr = jstring_create_cstr(lpBuffer, runtime);
    push_ref(runtime->stack, jstr);
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_getcwd(Runtime *runtime, JClass *clazz) {
    ByteBuf *platformPath = bytebuf_create(1024);

    __refer ret = getcwd(platformPath->buf, platformPath->_alloc_size);
    if (ret) {
        Utf8String *filepath = utf8_create();
        conv_platform_encoding_2_utf8(filepath, platformPath->buf);

        Instance *jstr = jstring_create(filepath, runtime);
        push_ref(runtime->stack, jstr);

        utf8_destroy(filepath);
    } else {
        push_ref(runtime->stack, NULL);
    }

    bytebuf_destroy(platformPath);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_getcwd  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_chmod(Runtime *runtime, JClass *clazz) {
    Instance *path_arr = localvar_getRefer(runtime->localvar, 0);
    s32 mode = localvar_getInt(runtime->localvar, 1);
    if (path_arr) {
        Utf8String *filepath = utf8_create_c(path_arr->arr_body);
        ByteBuf *platformPath = bytebuf_create(0);
        conv_utf8_2_platform_encoding(platformPath, filepath);

        s32 ret = chmod(platformPath->buf, mode);
        push_int(runtime->stack, ret);

        bytebuf_destroy(platformPath);
        utf8_destroy(filepath);
    } else {
        push_int(runtime->stack, -1);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_fullpath  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_rename0(Runtime *runtime, JClass *clazz) {
    Instance *old_arr = localvar_getRefer(runtime->localvar, 0);
    Instance *new_arr = localvar_getRefer(runtime->localvar, 1);
    if (old_arr && new_arr) {
        Utf8String *filepath = utf8_create_c(old_arr->arr_body);
        ByteBuf *oldPath = bytebuf_create(0);
        conv_utf8_2_platform_encoding(oldPath, filepath);
        utf8_clear(filepath);
        utf8_append_c(filepath, new_arr->arr_body);
        ByteBuf *newPath = bytebuf_create(0);
        conv_utf8_2_platform_encoding(newPath, filepath);

        s32 ret = rename(oldPath->buf, newPath->buf);
        push_int(runtime->stack, ret);

        bytebuf_destroy(oldPath);
        bytebuf_destroy(newPath);
        utf8_destroy(filepath);
    } else {
        push_int(runtime->stack, -1);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_rename0  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_getTmpDir(Runtime *runtime, JClass *clazz) {
    Utf8String *key = utf8_create_c("glfm.save.root");//mobile platform get the write privilege dir
    Utf8String *val = hashtable_get(runtime->jvm->sys_prop, key);//don't destroy the val

    Utf8String *tdir;
    if (val) {
        tdir = utf8_create_copy(val);
        utf8_append_c(tdir, "/tmp/");
        os_mkdir(utf8_cstr(tdir));
    } else {
        tdir = os_get_tmp_dir();
    }
    utf8_destroy(key);

    if (tdir) {
        Utf8String *utf8 = utf8_create();
        conv_platform_encoding_2_utf8(utf8, utf8_cstr(tdir));

        Instance *jstr = jstring_create(utf8, runtime);
        push_ref(runtime->stack, jstr);

        utf8_destroy(utf8);
        utf8_destroy(tdir);
    } else {
        push_ref(runtime->stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_getTmpDir  \n");
#endif
    return 0;
}


s32 org_mini_fs_InnerFile_mkdir0(Runtime *runtime, JClass *clazz) {
    Instance *path_arr = localvar_getRefer(runtime->localvar, 0);
    s32 ret = -1;
    if (path_arr) {
        Utf8String *filepath = utf8_create_c(path_arr->arr_body);
        ByteBuf *platformPath = bytebuf_create(0);
        conv_utf8_2_platform_encoding(platformPath, filepath);

        ret = os_mkdir(platformPath->buf);
        push_int(runtime->stack, ret);

        bytebuf_destroy(platformPath);
        utf8_destroy(filepath);
    } else {
        push_int(runtime->stack, ret);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_mkdir  \n");
#endif
    return 0;
}

extern s32 os_iswin();

s32 org_mini_fs_InnerFile_getOS(Runtime *runtime, JClass *clazz) {
    s32 win = os_iswin();
    push_int(runtime->stack, win);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_getOS  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_delete0(Runtime *runtime, JClass *clazz) {
    Instance *path_arr = localvar_getRefer(runtime->localvar, 0);
    s32 ret = -1;
    if (path_arr) {
        Utf8String *filepath = utf8_create_c(path_arr->arr_body);
        ByteBuf *platformPath = bytebuf_create(0);
        conv_utf8_2_platform_encoding(platformPath, filepath);

        struct stat buf;
        stat(platformPath->buf, &buf);
        s32 a = S_ISDIR(buf.st_mode);
        if (a) {
            ret = rmdir(platformPath->buf);
        } else {
            ret = remove(platformPath->buf);
        }
        push_int(runtime->stack, ret);

        bytebuf_destroy(platformPath);
        utf8_destroy(filepath);
    } else {
        push_int(runtime->stack, ret);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_delete0  \n");
#endif
    return 0;
}

//------------------------------------------------------------------------------------
//                              Zip
//------------------------------------------------------------------------------------

s32 org_mini_zip_ZipFile_getEntryIndex0(Runtime *runtime, JClass *clazz) {
    Instance *zip_path_arr = localvar_getRefer(runtime->localvar, 0);
    Instance *name_arr = localvar_getRefer(runtime->localvar, 1);
    s32 ret = -1;
    if (zip_path_arr && name_arr) {

        ret = zip_get_file_index(zip_path_arr->arr_body, name_arr->arr_body);

    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_getEntryIndex0  \n");
#endif
    return 0;
}

s32 org_mini_zip_ZipFile_getEntrySize0(Runtime *runtime, JClass *clazz) {
    Instance *zip_path_arr = localvar_getRefer(runtime->localvar, 0);
    Instance *name_arr = localvar_getRefer(runtime->localvar, 1);
    s64 ret = -1;
    if (zip_path_arr && name_arr) {

        ret = zip_get_file_unzip_size(zip_path_arr->arr_body, name_arr->arr_body);

    }
    push_long(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_getEntrySize0  \n");
#endif
    return 0;
}

s32 org_mini_zip_ZipFile_getEntry0(Runtime *runtime, JClass *clazz) {
    Instance *zip_path_arr = localvar_getRefer(runtime->localvar, 0);
    Instance *name_arr = localvar_getRefer(runtime->localvar, 1);
    s32 ret = -1;
    if (zip_path_arr && name_arr) {
        s64 filesize = zip_get_file_unzip_size(zip_path_arr->arr_body, name_arr->arr_body);
        if (filesize >= 0) {
            Instance *arr = jarray_create_by_type_index(runtime, (s32) filesize, DATATYPE_BYTE);
            ret = zip_loadfile_to_mem(zip_path_arr->arr_body, name_arr->arr_body, arr->arr_body, filesize);
            if (ret == 0) {
                push_ref(runtime->stack, arr);
            }
        }
    }
    if (ret) {
        push_ref(runtime->stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_getEntry0  \n");
#endif
    return 0;
}

s32 org_mini_zip_ZipFile_putEntry0(Runtime *runtime, JClass *clazz) {
    Instance *zip_path_arr = localvar_getRefer(runtime->localvar, 0);
    Instance *name_arr = localvar_getRefer(runtime->localvar, 1);
    Instance *content_arr = localvar_getRefer(runtime->localvar, 2);
    s32 ret = -1;
    if (zip_path_arr && name_arr) {

        zip_savefile_mem(zip_path_arr->arr_body, name_arr->arr_body, content_arr ? content_arr->arr_body : NULL, content_arr ? content_arr->arr_length : 0);
        ret = 0;

    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_putEntry0  \n");
#endif
    return 0;
}

s32 org_mini_zip_ZipFile_fileCount0(Runtime *runtime, JClass *clazz) {
    Instance *zip_path_arr = localvar_getRefer(runtime->localvar, 0);

    s32 ret = 0;
    if (zip_path_arr) {

        ret = zip_filecount(zip_path_arr->arr_body);

    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_fileCount0  \n");
#endif
    return 0;
}

s32 org_mini_zip_ZipFile_listFiles0(Runtime *runtime, JClass *clazz) {
    Instance *zip_path_arr = localvar_getRefer(runtime->localvar, 0);
    s32 ret = -1;
    if (zip_path_arr) {

        ArrayList *list = zip_get_filenames(zip_path_arr->arr_body);
        if (list) {
            Utf8String *clustr = utf8_create_c(STR_CLASS_JAVA_LANG_STRING);
            Instance *jarr = jarray_create_by_type_name(runtime, list->length, clustr, NULL);
            utf8_destroy(clustr);
            instance_hold_to_thread(jarr, runtime);
            s32 i;
            for (i = 0; i < list->length; i++) {
                Utf8String *ustr = arraylist_get_value_unsafe(list, i);
                Instance *jstr = jstring_create(ustr, runtime);
                jarray_set_field(jarr, i, (s64) (intptr_t) jstr);
            }
            zip_destroy_filenames_list(list);
            instance_release_from_thread(jarr, runtime);
            push_ref(runtime->stack, jarr);
            ret = 0;
        }

    }
    if (ret == -1) {
        push_ref(runtime->stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_listFiles0  \n");
#endif
    return 0;
}

s32 org_mini_zip_ZipFile_isDirectory0(Runtime *runtime, JClass *clazz) {
    Instance *zip_path_arr = localvar_getRefer(runtime->localvar, 0);
    s32 index = localvar_getInt(runtime->localvar, 1);
    s32 ret = -1;
    if (zip_path_arr) {

        ret = zip_is_directory(zip_path_arr->arr_body, index);

    }

    push_int(runtime->stack, ret);

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_isDirectory0  \n");
#endif
    return 0;
}

s32 org_mini_zip_ZipFile_extract0(Runtime *runtime, JClass *clazz) {
    Instance *zip_data = localvar_getRefer(runtime->localvar, 0);
    s32 ret = 0;
    ByteBuf *data = bytebuf_create(0);
    if (zip_data) {
        ret = zip_extract(zip_data->arr_body, zip_data->arr_length, data);
    }
    if (ret == -1) {
        push_ref(runtime->stack, NULL);
    } else {
        Instance *byte_arr = jarray_create_by_type_index(runtime, data->wp, DATATYPE_BYTE);
        bytebuf_read_batch(data, byte_arr->arr_body, data->wp);
        push_ref(runtime->stack, byte_arr);
    }
    bytebuf_destroy(data);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_extract0  \n");
#endif
    return 0;
}

s32 org_mini_zip_ZipFile_compress0(Runtime *runtime, JClass *clazz) {
    Instance *data = localvar_getRefer(runtime->localvar, 0);
    s32 ret = 0;
    ByteBuf *zip_data = bytebuf_create(0);
    if (data) {
        ret = zip_compress(data->arr_body, data->arr_length, zip_data);
    }
    if (ret == -1) {
        push_ref(runtime->stack, NULL);
    } else {
        Instance *byte_arr = jarray_create_by_type_index(runtime, zip_data->wp, DATATYPE_BYTE);
        bytebuf_read_batch(zip_data, byte_arr->arr_body, zip_data->wp);
        push_ref(runtime->stack, byte_arr);
    }
    bytebuf_destroy(zip_data);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_compress0  \n");
#endif
    return 0;
}

s32 org_mini_zip_ZipFile_gzipExtract0(Runtime *runtime, JClass *clazz) {
    Instance *gzip_data = localvar_getRefer(runtime->localvar, 0);
    s32 ret = 0;
    ByteBuf *data = bytebuf_create(0);
    if (gzip_data) {
        ret = gzip_extract(gzip_data->arr_body, gzip_data->arr_length, data);
    }
    if (ret == -1) {
        push_ref(runtime->stack, NULL);
    } else {
        Instance *byte_arr = jarray_create_by_type_index(runtime, data->wp, DATATYPE_BYTE);
        bytebuf_read_batch(data, byte_arr->arr_body, data->wp);
        push_ref(runtime->stack, byte_arr);
    }
    bytebuf_destroy(data);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_gzipExtract0  \n");
#endif
    return 0;
}

s32 org_mini_zip_ZipFile_gzipCompress0(Runtime *runtime, JClass *clazz) {
    Instance *data = localvar_getRefer(runtime->localvar, 0);
    s32 ret = 0;
    ByteBuf *gzip_data = bytebuf_create(0);
    if (data) {
        ret = gzip_compress(data->arr_body, data->arr_length, gzip_data);
    }
    if (ret == -1) {
        push_ref(runtime->stack, NULL);
    } else {
        Instance *byte_arr = jarray_create_by_type_index(runtime, gzip_data->wp, DATATYPE_BYTE);
        bytebuf_read_batch(gzip_data, byte_arr->arr_body, gzip_data->wp);
        push_ref(runtime->stack, byte_arr);
    }
    bytebuf_destroy(gzip_data);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_gzipCompress0  \n");
#endif
    return 0;
}

s32 org_mini_crypt_XorCrypt_encrypt(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *data = localvar_getRefer(runtime->localvar, pos++);
    Instance *key = localvar_getRefer(runtime->localvar, pos++);
    if (data && key) {
        Instance *r = jarray_create_by_type_index(runtime, data->arr_length, DATATYPE_BYTE);
        s32 i, j, imax, jmax;
        for (i = 0, imax = data->arr_length; i < imax; i++) {
            u32 v = jarray_get_field(data, i) & 0xff;
            for (j = 0, jmax = key->arr_length; j < jmax; j++) {
                u32 k = jarray_get_field(key, j) & 0xff;

                u32 bitshift = k % 8;

                u32 v1 = (v << bitshift);
                u32 v2 = (v >> (8 - bitshift));
                v = (v1 | v2);

                v = (v ^ k) & 0xff;
            }
            jarray_set_field(r, i, v & 0xff);
        }
        push_ref(runtime->stack, r);
    } else {
        push_ref(runtime->stack, NULL);
    }
    return 0;
}

s32 org_mini_crypt_XorCrypt_decrypt(Runtime *runtime, JClass *clazz) {
    s32 pos = 0;
    Instance *data = localvar_getRefer(runtime->localvar, pos++);
    Instance *key = localvar_getRefer(runtime->localvar, pos++);
    if (data && key) {
        Instance *r = jarray_create_by_type_index(runtime, data->arr_length, DATATYPE_BYTE);
        s32 i, j, imax;
        for (i = 0, imax = data->arr_length; i < imax; i++) {
            u32 v = jarray_get_field(data, i) & 0xff;
            for (j = key->arr_length - 1; j >= 0; j--) {
                u32 k = jarray_get_field(key, j) & 0xff;
                v = (v ^ k) & 0xff;

                u32 bitshift = k % 8;

                u32 v1 = (v >> bitshift);
                u32 v2 = (v << (8 - bitshift));
                v = (v1 | v2);

            }
            jarray_set_field(r, i, v & 0xff);
        }
        push_ref(runtime->stack, r);
    } else {
        push_ref(runtime->stack, NULL);
    }
    return 0;
}

static java_native_method METHODS_IO_TABLE[] = {
        {"org/mini/net/SocketNative", "open0",                "()[B",                             org_mini_net_SocketNative_open0},
        {"org/mini/net/SocketNative", "bind0",                "([B[B[BI)I",                       org_mini_net_SocketNative_bind0},
        {"org/mini/net/SocketNative", "connect0",             "([B[B[BI)I",                       org_mini_net_SocketNative_connect0},
        {"org/mini/net/SocketNative", "accept0",              "([B)[B",                           org_mini_net_SocketNative_accept0},
        {"org/mini/net/SocketNative", "readBuf",              "([B[BII)I",                        org_mini_net_SocketNative_readBuf},
        {"org/mini/net/SocketNative", "readByte",             "([B)I",                            org_mini_net_SocketNative_readByte},
        {"org/mini/net/SocketNative", "writeBuf",             "([B[BII)I",                        org_mini_net_SocketNative_writeBuf},
        {"org/mini/net/SocketNative", "writeByte",            "([BI)I",                           org_mini_net_SocketNative_writeByte},
        {"org/mini/net/SocketNative", "available0",           "([B)I",                            org_mini_net_SocketNative_available0},
        {"org/mini/net/SocketNative", "close0",               "([B)V",                            org_mini_net_SocketNative_close0},
        {"org/mini/net/SocketNative", "setOption0",           "([BIII)I",                         org_mini_net_SocketNative_setOption0},
        {"org/mini/net/SocketNative", "getOption0",           "([BI)I",                           org_mini_net_SocketNative_getOption0},
        {"org/mini/net/SocketNative", "getSockAddr",          "([BI)Ljava/lang/String;",          org_mini_net_SocketNative_getSockAddr},
        {"org/mini/net/SocketNative", "host2ip",              "([B)[B",                           org_mini_net_SocketNative_host2ip},
        {"org/mini/net/SocketNative", "sslc_construct_entry", "()[B",                             org_mini_net_SocketNative_sslc_construct_entry},
        {"org/mini/net/SocketNative", "sslc_init",            "([B)I",                            org_mini_net_SocketNative_sslc_init},
        {"org/mini/net/SocketNative", "sslc_wrap",            "([B[B[B)I",                        org_mini_net_SocketNative_sslc_wrap},
        {"org/mini/net/SocketNative", "sslc_connect",         "([B[B[B)I",                        org_mini_net_SocketNative_sslc_connect},
        {"org/mini/net/SocketNative", "sslc_close",           "([B)I",                            org_mini_net_SocketNative_sslc_close},
        {"org/mini/net/SocketNative", "sslc_read",            "([B[BII)I",                        org_mini_net_SocketNative_sslc_read},
        {"org/mini/net/SocketNative", "sslc_write",           "([B[BII)I",                        org_mini_net_SocketNative_sslc_write},
        {"org/mini/fs/InnerFile",     "openFile",             "([B[B)J",                          org_mini_fs_InnerFile_openFile},
        {"org/mini/fs/InnerFile",     "openFD",               "(I[B)J",                           org_mini_fs_InnerFile_openFD},
        {"org/mini/fs/InnerFile",     "fileno",               "(J)I",                             org_mini_fs_InnerFile_fileno},
        {"org/mini/fs/InnerFile",     "closeFile",            "(J)I",                             org_mini_fs_InnerFile_closeFile},
        {"org/mini/fs/InnerFile",     "read0",                "(J)I",                             org_mini_fs_InnerFile_read0},
        {"org/mini/fs/InnerFile",     "write0",               "(JI)I",                            org_mini_fs_InnerFile_write0},
        {"org/mini/fs/InnerFile",     "readbuf",              "(J[BII)I",                         org_mini_fs_InnerFile_readbuf},
        {"org/mini/fs/InnerFile",     "writebuf",             "(J[BII)I",                         org_mini_fs_InnerFile_writebuf},
        {"org/mini/fs/InnerFile",     "seek0",                "(JJ)I",                            org_mini_fs_InnerFile_seek0},
        {"org/mini/fs/InnerFile",     "available0",           "(J)I",                             org_mini_fs_InnerFile_available0},
        {"org/mini/fs/InnerFile",     "setLength0",           "(JJ)I",                            org_mini_fs_InnerFile_setLength0},
        {"org/mini/fs/InnerFile",     "flush0",               "(J)I",                             org_mini_fs_InnerFile_flush0},
        {"org/mini/fs/InnerFile",     "loadFS",               "([BLorg/mini/fs/InnerFileStat;)I", org_mini_fs_InnerFile_loadFS},
        {"org/mini/fs/InnerFile",     "listDir",              "([B)[Ljava/lang/String;",          org_mini_fs_InnerFile_listDir},
        {"org/mini/fs/InnerFile",     "getcwd",               "()Ljava/lang/String;",             org_mini_fs_InnerFile_getcwd},
        {"org/mini/fs/InnerFile",     "chmod",                "([BI)I",                           org_mini_fs_InnerFile_chmod},
        {"org/mini/fs/InnerFile",     "mkdir0",               "([B)I",                            org_mini_fs_InnerFile_mkdir0},
        {"org/mini/fs/InnerFile",     "getOS",                "()I",                              org_mini_fs_InnerFile_getOS},
        {"org/mini/fs/InnerFile",     "delete0",              "([B)I",                            org_mini_fs_InnerFile_delete0},
        {"org/mini/fs/InnerFile",     "rename0",              "([B[B)I",                          org_mini_fs_InnerFile_rename0},
        {"org/mini/fs/InnerFile",     "getTmpDir",            "()Ljava/lang/String;",             org_mini_fs_InnerFile_getTmpDir},
        {"org/mini/fs/InnerFile",     "listWinDrivers",       "()Ljava/lang/String;",             org_mini_fs_InnerFile_listWinDrivers},
        {"org/mini/zip/Zip",          "getEntry0",            "([B[B)[B",                         org_mini_zip_ZipFile_getEntry0},
        {"org/mini/zip/Zip",          "putEntry0",            "([B[B[B)I",                        org_mini_zip_ZipFile_putEntry0},
        {"org/mini/zip/Zip",          "getEntryIndex0",       "([B[B)I",                          org_mini_zip_ZipFile_getEntryIndex0},
        {"org/mini/zip/Zip",          "getEntrySize0",        "([B[B)J",                          org_mini_zip_ZipFile_getEntrySize0},
        {"org/mini/zip/Zip",          "fileCount0",           "([B)I",                            org_mini_zip_ZipFile_fileCount0},
        {"org/mini/zip/Zip",          "listFiles0",           "([B)[Ljava/lang/String;",          org_mini_zip_ZipFile_listFiles0},
        {"org/mini/zip/Zip",          "isDirectory0",         "([BI)I",                           org_mini_zip_ZipFile_isDirectory0},
        {"org/mini/zip/Zip",          "extract0",             "([B)[B",                           org_mini_zip_ZipFile_extract0},
        {"org/mini/zip/Zip",          "compress0",            "([B)[B",                           org_mini_zip_ZipFile_compress0},
        {"org/mini/zip/Zip",          "gzipExtract0",         "([B)[B",                           org_mini_zip_ZipFile_gzipExtract0},
        {"org/mini/zip/Zip",          "gzipCompress0",        "([B)[B",                           org_mini_zip_ZipFile_gzipCompress0},
        {"org/mini/crypt/XorCrypt",   "encrypt",              "([B[B)[B",                         org_mini_crypt_XorCrypt_encrypt},
        {"org/mini/crypt/XorCrypt",   "decrypt",              "([B[B)[B",                         org_mini_crypt_XorCrypt_decrypt},

};


void reg_net_native_lib(MiniJVM *jvm) {
    native_reg_lib(jvm, &(METHODS_IO_TABLE[0]), sizeof(METHODS_IO_TABLE) / sizeof(java_native_method));
}


#ifdef __cplusplus
}
#endif
