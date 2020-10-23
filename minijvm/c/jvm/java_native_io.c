//
// Created by gust on 2017/9/1.
//

#include "jvm.h"
#include "java_native_std.h"
#include "garbage.h"
#include "java_native_io.h"
#include "jvm_util.h"
#include "../utils/miniz_wrapper.h"
#include <sys/stat.h>


#ifdef __cplusplus
extern "C" {
#endif

#include <string.h>   // NULL and possibly memcpy, memset

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
#else

#include <dirent.h>

#endif

#include "ssl_client.h"
#include "mbedtls/net_sockets.h"


#if __JVM_OS_MINGW__

#ifndef AI_ALL
#define    AI_ALL        0x00000100
#endif

/*--------------------------------------------------------------------------------------

    By Marco Ladino - mladinox.. jan/2016


    MinGW 3.45 thru 4.5 versions, don't have the socket functions:

    --> inet_ntop(..)
    --> inet_pton(..)

    But with this adapted code using the original functions from FreeBSD,
    one can to use it in the C/C++ Applications, without problem..!

    This implementation, include tests for IPV4 and IPV6 addresses,
    and is full C/C++ compatible..

--------------------------------------------------------------------------------------*/


/*
 * Copy src to string dst of size siz.  At most siz-1 characters
 * will be copied.  Always NUL terminates (unless siz == 0).
 * Returns strlen(src); if retval >= siz, truncation occurred.
 */
size_t strlcpy(char *dst, const char *src, size_t siz) {
    char *d = dst;
    const char *s = src;
    size_t n = siz;

    /* Copy as many bytes as will fit */
    if (n != 0) {
        while (--n != 0) {
            if ((*d++ = *s++) == '\0')
                break;
        }
    }

    /* Not enough room in dst, add NUL and traverse rest of src */
    if (n == 0) {
        if (siz != 0)
            *d = '\0';        /* NUL-terminate dst */
        while (*s++);
    }

    return (s - src - 1);    /* count does not include NUL */
}


#include <Ws2tcpip.h>
#include <stdio.h>


/*%
 * WARNING: Don't even consider trying to compile this on a system where
 * sizeof(int) < 4.  sizeof(int) > 4 is fine; all the world's not a VAX.
 */

static char *inet_ntop4(const unsigned char *src, char *dst, socklen_t size);

static char *inet_ntop6(const unsigned char *src, char *dst, socklen_t size);

/* char *
 * inet_ntop(af, src, dst, size)
 *	convert a network format address to presentation format.
 * return:
 *	pointer to presentation format address (`dst'), or NULL (see errno).
 * author:
 *	Paul Vixie, 1996.
 */
char *inet_ntop(int af, const void *src, char *dst, socklen_t size) {
    switch (af) {
        case AF_INET:
            return (inet_ntop4((const unsigned char *) src, dst, size));
        case AF_INET6:
            return (inet_ntop6((const unsigned char *) src, dst, size));
        default:
            return (NULL);
    }
    /* NOTREACHED */
}

/* const char *
 * inet_ntop4(src, dst, size)
 *	format an IPv4 address
 * return:
 *	`dst' (as a const)
 * notes:
 *	(1) uses no statics
 *	(2) takes a u_char* not an in_addr as input
 * author:
 *	Paul Vixie, 1996.
 */
static char *inet_ntop4(const unsigned char *src, char *dst, socklen_t size) {
    static const char fmt[] = "%u.%u.%u.%u";
    char tmp[sizeof "255.255.255.255"];
    int l;

    l = snprintf(tmp, sizeof(tmp), fmt, src[0], src[1], src[2], src[3]);
    if (l <= 0 || (socklen_t) l >= size) {
        return (NULL);
    }
    strlcpy(dst, tmp, size);
    return (dst);
}

/* const char *
 * inet_ntop6(src, dst, size)
 *	convert IPv6 binary address into presentation (printable) format
 * author:
 *	Paul Vixie, 1996.
 */
static char *inet_ntop6(const unsigned char *src, char *dst, socklen_t size) {
    /*
     * Note that int32_t and int16_t need only be "at least" large enough
     * to contain a value of the specified size.  On some systems, like
     * Crays, there is no such thing as an integer variable with 16 bits.
     * Keep this in mind if you think this function should have been coded
     * to use pointer overlays.  All the world's not a VAX.
     */
    char tmp[sizeof "ffff:ffff:ffff:ffff:ffff:ffff:255.255.255.255"], *tp;
    struct {
        int base, len;
    } best, cur;
#define NS_IN6ADDRSZ    16
#define NS_INT16SZ    2
    u_int words[NS_IN6ADDRSZ / NS_INT16SZ];
    int i;

    /*
     * Preprocess:
     *	Copy the input (bytewise) array into a wordwise array.
     *	Find the longest run of 0x00's in src[] for :: shorthanding.
     */
    memset(words, '\0', sizeof words);
    for (i = 0; i < NS_IN6ADDRSZ; i++)
        words[i / 2] |= (src[i] << ((1 - (i % 2)) << 3));
    best.base = -1;
    best.len = 0;
    cur.base = -1;
    cur.len = 0;
    for (i = 0; i < (NS_IN6ADDRSZ / NS_INT16SZ); i++) {
        if (words[i] == 0) {
            if (cur.base == -1)
                cur.base = i, cur.len = 1;
            else
                cur.len++;
        } else {
            if (cur.base != -1) {
                if (best.base == -1 || cur.len > best.len)
                    best = cur;
                cur.base = -1;
            }
        }
    }
    if (cur.base != -1) {
        if (best.base == -1 || cur.len > best.len)
            best = cur;
    }
    if (best.base != -1 && best.len < 2)
        best.base = -1;

    /*
     * Format the result.
     */
    tp = tmp;
    for (i = 0; i < (NS_IN6ADDRSZ / NS_INT16SZ); i++) {
        /* Are we inside the best run of 0x00's? */
        if (best.base != -1 && i >= best.base &&
            i < (best.base + best.len)) {
            if (i == best.base)
                *tp++ = ':';
            continue;
        }
        /* Are we following an initial run of 0x00s or any real hex? */
        if (i != 0)
            *tp++ = ':';
        /* Is this address an encapsulated IPv4? */
        if (i == 6 && best.base == 0 && (best.len == 6 ||
                                         (best.len == 7 && words[7] != 0x0001) ||
                                         (best.len == 5 && words[5] == 0xffff))) {
            if (!inet_ntop4(src + 12, tp, sizeof tmp - (tp - tmp)))
                return (NULL);
            tp += strlen(tp);
            break;
        }
        tp += sprintf(tp, "%x", words[i]);
    }
    /* Was it a trailing run of 0x00's? */
    if (best.base != -1 && (best.base + best.len) ==
                           (NS_IN6ADDRSZ / NS_INT16SZ))
        *tp++ = ':';
    *tp++ = '\0';

    /*
     * Check for overflow, copy, and we're done.
     */
    if ((socklen_t) (tp - tmp) > size) {
        return (NULL);
    }
    strcpy(dst, tmp);
    return (dst);
}

/*%
 * WARNING: Don't even consider trying to compile this on a system where
 * sizeof(int) < 4.  sizeof(int) > 4 is fine; all the world's not a VAX.
 */

static int inet_pton4(const char *src, u_char *dst);

static int inet_pton6(const char *src, u_char *dst);

/* int
 * inet_pton(af, src, dst)
 *	convert from presentation format (which usually means ASCII printable)
 *	to network format (which is usually some kind of binary format).
 * return:
 *	1 if the address was valid for the specified address family
 *	0 if the address wasn't valid (`dst' is untouched in this case)
 *	-1 if some other error occurred (`dst' is untouched in this case, too)
 * author:
 *	Paul Vixie, 1996.
 */
int inet_pton(int af, const char *src, void *dst) {
    switch (af) {
        case AF_INET:
            return (inet_pton4(src, (unsigned char *) dst));
        case AF_INET6:
            return (inet_pton6(src, (unsigned char *) dst));
        default:
            return (-1);
    }
    /* NOTREACHED */
}

/* int
 * inet_pton4(src, dst)
 *	like inet_aton() but without all the hexadecimal and shorthand.
 * return:
 *	1 if `src' is a valid dotted quad, else 0.
 * notice:
 *	does not touch `dst' unless it's returning 1.
 * author:
 *	Paul Vixie, 1996.
 */
static int inet_pton4(const char *src, u_char *dst) {
    static const char digits[] = "0123456789";
    int saw_digit, octets, ch;
#define NS_INADDRSZ    4
    u_char tmp[NS_INADDRSZ], *tp;

    saw_digit = 0;
    octets = 0;
    *(tp = tmp) = 0;
    while ((ch = *src++) != '\0') {
        const char *pch;

        if ((pch = strchr(digits, ch)) != NULL) {
            u_int uiNew = *tp * 10 + (pch - digits);

            if (saw_digit && *tp == 0)
                return (0);
            if (uiNew > 255)
                return (0);
            *tp = uiNew;
            if (!saw_digit) {
                if (++octets > 4)
                    return (0);
                saw_digit = 1;
            }
        } else if (ch == '.' && saw_digit) {
            if (octets == 4)
                return (0);
            *++tp = 0;
            saw_digit = 0;
        } else
            return (0);
    }
    if (octets < 4)
        return (0);
    memcpy(dst, tmp, NS_INADDRSZ);
    return (1);
}

/* int
 * inet_pton6(src, dst)
 *	convert presentation level address to network order binary form.
 * return:
 *	1 if `src' is a valid [RFC1884 2.2] address, else 0.
 * notice:
 *	(1) does not touch `dst' unless it's returning 1.
 *	(2) :: in a full address is silently ignored.
 * credit:
 *	inspired by Mark Andrews.
 * author:
 *	Paul Vixie, 1996.
 */
static int inet_pton6(const char *src, u_char *dst) {
    static const char xdigits_l[] = "0123456789abcdef",
            xdigits_u[] = "0123456789ABCDEF";
#define NS_IN6ADDRSZ    16
#define NS_INT16SZ    2
    u_char tmp[NS_IN6ADDRSZ], *tp, *endp, *colonp;
    const char *xdigits, *curtok;
    int ch, seen_xdigits;
    u_int val;

    memset((tp = tmp), '\0', NS_IN6ADDRSZ);
    endp = tp + NS_IN6ADDRSZ;
    colonp = NULL;
    /* Leading :: requires some special handling. */
    if (*src == ':')
        if (*++src != ':')
            return (0);
    curtok = src;
    seen_xdigits = 0;
    val = 0;
    while ((ch = *src++) != '\0') {
        const char *pch;

        if ((pch = strchr((xdigits = xdigits_l), ch)) == NULL)
            pch = strchr((xdigits = xdigits_u), ch);
        if (pch != NULL) {
            val <<= 4;
            val |= (pch - xdigits);
            if (++seen_xdigits > 4)
                return (0);
            continue;
        }
        if (ch == ':') {
            curtok = src;
            if (!seen_xdigits) {
                if (colonp)
                    return (0);
                colonp = tp;
                continue;
            } else if (*src == '\0') {
                return (0);
            }
            if (tp + NS_INT16SZ > endp)
                return (0);
            *tp++ = (u_char) (val >> 8) & 0xff;
            *tp++ = (u_char) val & 0xff;
            seen_xdigits = 0;
            val = 0;
            continue;
        }
        if (ch == '.' && ((tp + NS_INADDRSZ) <= endp) &&
            inet_pton4(curtok, tp) > 0) {
            tp += NS_INADDRSZ;
            seen_xdigits = 0;
            break;    /*%< '\\0' was seen by inet_pton4(). */
        }
        return (0);
    }
    if (seen_xdigits) {
        if (tp + NS_INT16SZ > endp)
            return (0);
        *tp++ = (u_char) (val >> 8) & 0xff;
        *tp++ = (u_char) val & 0xff;
    }
    if (colonp != NULL) {
        /*
         * Since some memmove()'s erroneously fail to handle
         * overlapping regions, we'll do the shift by hand.
         */
        const int n = tp - colonp;
        int i;

        if (tp == endp)
            return (0);
        for (i = 1; i <= n; i++) {
            endp[-i] = colonp[n - i];
            colonp[n - i] = 0;
        }
        tp = endp;
    }
    if (tp != endp)
        return (0);
    memcpy(dst, tmp, NS_IN6ADDRSZ);
    return (1);
}


/*------------------------------------------
    MAIN  -  tester
--------------------------------------------
    by Marco Ladino - mladinox
------------------------------------------*/
//int main()
//{
//    in_addr ipv4_address;
//    in_addr6 ipv6_address;
//    char strIP[128];
//    char *pBytes=0;
//    int i=0;
//
//    //ipv4 addresses..
//    pBytes = (char *)&ipv4_address;
//    inet_pton_2( AF_INET, "127.0.0.1", &ipv4_address); //31.175.162.251
//    printf("inet_pton=(ipv4) -> %02x, %02x, %02x, %02x\n", pBytes[0],pBytes[1],pBytes[2],pBytes[3]);
//    inet_ntop_2(AF_INET,&ipv4_address, strIP, INET_ADDRSTRLEN);
//    printf("inet_ntop=(ipv4) -> %s\n", strIP);
//
//    //ipv6 addresses..
//    pBytes = (char *)&ipv6_address;
//    inet_pton_2( AF_INET6, "2001:DB8:CAFE:0:beef:800:200C:417A", &ipv6_address); //2001:DB8:0:0:8:800:200C:417A  fe80:cafe:250:8dff:fecb:e3dc:beef
//    printf("inet_pton=(ipv6) -> ");
//    for (i=0;i<16;i++)
//    {
//        printf("%02x ",pBytes[i]);
//    }
//    printf("\n");
//    inet_ntop_2(AF_INET6,&ipv6_address, strIP, INET6_ADDRSTRLEN);
//    printf("inet_ntop=(ipv6) -> %s\n", strIP);
//
//
//
//    return 0;
//
//}

#endif


typedef struct _VmSock {
    mbedtls_net_context contex;
    //
    s32 rcv_time_out;
    u8 non_block;
    u8 reuseaddr;
} VmSock;

//=================================  socket  ====================================
s32 sock_option(VmSock *vmsock, s32 opType, s32 opValue, s32 opValue2) {
    s32 ret = 0;
    switch (opType) {
        case SOCK_OP_TYPE_NON_BLOCK: {//阻塞设置
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
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_REUSEADDR, (char *) &x, sizeof(x));
            vmsock->reuseaddr = 1;
            break;
        }
        case SOCK_OP_TYPE_RCVBUF: {//缓冲区设置
            int nVal = opValue;//设置为 opValue K
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVBUF, (const char *) &nVal, sizeof(nVal));
            break;
        }
        case SOCK_OP_TYPE_SNDBUF: {//缓冲区设置
            s32 nVal = opValue;//设置为 opValue K
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_SNDBUF, (const char *) &nVal, sizeof(nVal));
            break;
        }
        case SOCK_OP_TYPE_TIMEOUT: {
            vmsock->rcv_time_out = opValue;
#if __JVM_OS_MINGW__ || __JVM_OS_VS__
            s32 nTime = opValue;
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVTIMEO, (const char *) &nTime, sizeof(nTime));
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
            //(在closesocket()调用,但是还有数据没发送完毕的时候容许逗留)
            // 如果m_sLinger.l_onoff=0;则功能和2.)作用相同;
            m_sLinger.l_onoff = opValue;
            m_sLinger.l_linger = opValue2;//(容许逗留的时间为5秒)
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVTIMEO, (const char *) &m_sLinger, sizeof(m_sLinger));
            break;
        }
        case SOCK_OP_TYPE_KEEPALIVE: {
            s32 val = opValue;
            ret = setsockopt(vmsock->contex.fd, SOL_SOCKET, SO_RCVTIMEO, (const char *) &val, sizeof(val));
            break;
        }
    }
    return ret;
}


s32 sock_get_option(VmSock *vmsock, s32 opType) {
    s32 ret = 0;
    socklen_t len;

    switch (opType) {
        case SOCK_OP_TYPE_NON_BLOCK: {//阻塞设置
#if __JVM_OS_MINGW__ || __JVM_OS_VS__
            u_long flags = 1;
            ret = NO_ERROR == ioctlsocket(vmsock->contex.fd, FIONBIO, &flags);
#else
            int flags;
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
        case SOCK_OP_TYPE_SNDBUF: {//缓冲区设置
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
            //(在closesocket()调用,但是还有数据没发送完毕的时候容许逗留)
            // 如果m_sLinger.l_onoff=0;则功能和2.)作用相同;
            m_sLinger.l_onoff = 0;
            m_sLinger.l_linger = 0;//(容许逗留的时间为5秒)
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


s32 host_2_ip(c8 *hostname, char *buf, s32 buflen) {
#if __JVM_OS_VS__ || __JVM_OS_MINGW__
    WSADATA wsaData;
    WSAStartup(MAKEWORD(1, 1), &wsaData);
#endif  /*  WIN32  */
    struct addrinfo hints;
    struct addrinfo *result, *rp;
    int s;
    struct sockaddr_in *ipv4;
    struct sockaddr_in6 *ipv6;

    /* Obtain address(es) matching host/port */
    memset(&hints, 0, sizeof(struct addrinfo));
    hints.ai_family = AF_UNSPEC;    /* Allow IPv4 or IPv6 */
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = AI_ALL;
    hints.ai_protocol = IPPROTO_TCP;

    s = getaddrinfo(hostname, NULL, &hints, &result);
    if (s != 0) {
        fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(s));
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


s32 isDir(Utf8String *path) {
    struct stat buf;
    stat(utf8_cstr(path), &buf);
    s32 a = S_ISDIR(buf.st_mode);
    return a;
}

Utf8String *getTmpDir() {
    Utf8String *tmps = utf8_create();
#if __JVM_OS_MINGW__ || __JVM_OS_VS__
    c8 buf[128];
    s32 len = GetTempPath(128, buf);
    utf8_append_data(tmps, buf, len);
#else

#ifndef P_tmpdir
#define P_tmpdir "/tmp"
#endif
    utf8_append_c(tmps, P_tmpdir);
#endif
    return tmps;
}
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

    VmSock *vmsock = (VmSock *) vmarr->arr_body;
    mbedtls_net_context *ctx = &vmsock->contex;
    jthread_block_enter(runtime);
    s32 ret = mbedtls_net_bind(ctx, strlen(host->arr_body) == 0 ? NULL : host->arr_body, port->arr_body, proto);
    if (ret >= 0)ret = mbedtls_net_set_nonblock(ctx);//set as non_block , for vm destroy
    jthread_block_exit(runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_open0  \n");
#endif
    push_int(stack, ret < 0 ? -1 : 0);
    return 0;
}

s32 org_mini_net_SocketNative_accept0(Runtime *runtime, JClass *clazz) {
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);
    if (vmarr) {
        mbedtls_net_context *ctx = &((VmSock *) vmarr->arr_body)->contex;
        Instance *cltarr = jarray_create_by_type_index(runtime, sizeof(VmSock), DATATYPE_BYTE);
        VmSock *cltsock = (VmSock *) cltarr->arr_body;
        gc_refer_hold(cltarr);
        s32 ret = 0;
        while (1) {
            jthread_block_enter(runtime);
            ret = mbedtls_net_accept(ctx, &cltsock->contex, NULL, 0, NULL);
            jthread_block_exit(runtime);
            if (runtime->threadInfo->is_interrupt) {//vm notify thread destroy
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
        gc_refer_release(cltarr);
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

    VmSock *vmsock = (VmSock *) vmarr->arr_body;
    mbedtls_net_context *ctx = &vmsock->contex;
//    host_2_ip4()
//    memcpy(&vmsock->server_ip, host->arr_body, strlen(host->arr_body) + 1);//copy with 0
//    memcpy(&vmsock->server_port, port->arr_body, strlen(port->arr_body) + 1);
    jthread_block_enter(runtime);
    s32 ret = mbedtls_net_connect(ctx, host->arr_body, port->arr_body, proto);
    jthread_block_exit(runtime);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_open0  \n");
#endif
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
        if (vmsock->non_block) {
            ret = mbedtls_net_recv(&vmsock->contex, buf, count);
        } else {
            ret = mbedtls_net_recv_timeout(&vmsock->contex, buf, count, vmsock->rcv_time_out ? vmsock->rcv_time_out : 100);
        }
        if (runtime->threadInfo->is_interrupt) {//vm waiting for destroy
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

    VmSock *vmsock = (VmSock *) vmarr->arr_body;

    jthread_block_enter(runtime);
    s32 ret = sock_recv(vmsock, (u8 *) jbyte_arr->arr_body + offset, count, runtime);
    jthread_block_exit(runtime);
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_readBuf  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_readByte(Runtime *runtime, JClass *clazz) {
    Instance *vmarr = localvar_getRefer(runtime->localvar, 0);

    VmSock *vmsock = (VmSock *) vmarr->arr_body;
    u8 b = 0;
    jthread_block_enter(runtime);
    s32 ret = sock_recv(vmsock, &b, 1, runtime);
    jthread_block_exit(runtime);
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

    VmSock *vmsock = (VmSock *) vmarr->arr_body;
    mbedtls_net_context *ctx = &vmsock->contex;
    jthread_block_enter(runtime);
    s32 ret = mbedtls_net_send(ctx, (const u8 *) jbyte_arr->arr_body + offset, count);
    jthread_block_exit(runtime);
    if (ret == MBEDTLS_ERR_SSL_WANT_WRITE) {
        ret = 0;
    } else if (ret < 0) {
        ret = -1;
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

    VmSock *vmsock = (VmSock *) vmarr->arr_body;
    mbedtls_net_context *ctx = &vmsock->contex;
    jthread_block_enter(runtime);
    s32 ret = mbedtls_net_send(ctx, &b, 1);
    jthread_block_exit(runtime);
    if (ret == MBEDTLS_ERR_SSL_WANT_WRITE) {
        ret = 0;
    } else if (ret < 0) {
        ret = -1;
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
    s32 ret = 0;
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

    s32 ret = 0;
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
        char ipAddr[INET6_ADDRSTRLEN];//保存点分十进制的地址
        int port = -1;
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
        utf8_destory(ustr);
        push_ref(runtime->stack, jstr);
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

        char buf[50];
        s32 ret = host_2_ip(host->arr_body, buf, sizeof(buf));
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
        v = sslc_init((SSLC_Entry *) jbyte_arr->arr_body);
    }
    push_int(runtime->stack, v);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_sslc_init  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_sslc_connect(Runtime *runtime, JClass *clazz) {
    s32 v = 0;
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *host_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    Instance *port_arr = (Instance *) localvar_getRefer(runtime->localvar, 2);
    if (jbyte_arr && host_arr && port_arr) {
        v = sslc_connect((SSLC_Entry *) jbyte_arr->arr_body, host_arr->arr_body, port_arr->arr_body);
    }
    push_int(runtime->stack, v);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_http_open  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_sslc_close(Runtime *runtime, JClass *clazz) {
    s32 v = 0;
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    if (jbyte_arr) {
        v = sslc_close((SSLC_Entry *) jbyte_arr->arr_body);
    }
    push_int(runtime->stack, v);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_http_close  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_sslc_read(Runtime *runtime, JClass *clazz) {
    s32 v = 0;
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *data_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    s32 offset = localvar_getInt(runtime->localvar, 2);
    s32 len = localvar_getInt(runtime->localvar, 3);
    if (jbyte_arr && data_arr) {
        jthread_block_enter(runtime);
        v = sslc_read((SSLC_Entry *) jbyte_arr->arr_body, data_arr->arr_body + offset, len);
        jthread_block_exit(runtime);
    }
    push_int(runtime->stack, v < 0 ? -1 : v);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_http_init  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_sslc_write(Runtime *runtime, JClass *clazz) {
    s32 v = 0;
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    Instance *data_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    s32 offset = localvar_getInt(runtime->localvar, 2);
    s32 len = localvar_getInt(runtime->localvar, 3);
    if (jbyte_arr && data_arr) {
        jthread_block_enter(runtime);
        v = sslc_write((SSLC_Entry *) jbyte_arr->arr_body, data_arr->arr_body + offset, len);
        jthread_block_exit(runtime);
    }
    push_int(runtime->stack, v < 0 ? -1 : v);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_http_close  \n");
#endif
    return 0;
}


s32 org_mini_fs_InnerFile_openFile(Runtime *runtime, JClass *clazz) {
    Instance *name_arr = localvar_getRefer(runtime->localvar, 0);
    Instance *mode_arr = localvar_getRefer(runtime->localvar, 1);
    if (name_arr) {
        FILE *fd = fopen(name_arr->arr_body, mode_arr->arr_body);
        push_long(runtime->stack, (s64) (intptr_t) fd);
    } else {
        push_long(runtime->stack, 0);
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
        long pos;
        ret = fseek(fd, 0, SEEK_END);
        if (!ret) {
            ret = ftell(fd);
            if (!ret) {
                if (filelen < pos) {
#if __JVM_OS_VS__ || __JVM_OS_MINGW__
                    fseek(fd, (long) filelen, SEEK_SET);
                    SetEndOfFile(fd);
#else
                    ret = ftruncate(fileno(fd), (off_t) filelen);
#endif
                } else {
                    u8 d = 0;
                    s32 i, imax = filelen - pos;
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
    s32 ret = -1;
    if (name_arr) {
        Utf8String *filepath = utf8_create_part_c(name_arr->arr_body, 0, name_arr->arr_length);
        struct stat buf;
        ret = stat(utf8_cstr(filepath), &buf);
        s32 a = S_ISDIR(buf.st_mode);
        if (ret == 0) {
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
        utf8_destory(filepath);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_loadFD  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_listDir(Runtime *runtime, JClass *clazz) {
    Instance *name_arr = localvar_getRefer(runtime->localvar, 0);
    if (name_arr) {
        Utf8String *filepath = utf8_create_part_c(name_arr->arr_body, 0, name_arr->arr_length);

        ArrayList *files = arraylist_create(0);
        DIR *dirp;
        struct dirent *dp;
        dirp = opendir(utf8_cstr(filepath)); //打开目录指针
        if (dirp) {
            while ((dp = readdir(dirp)) != NULL) { //通过目录指针读目录
                if (strcmp(dp->d_name, ".") == 0) {
                    continue;
                }
                if (strcmp(dp->d_name, "..") == 0) {
                    continue;
                }
                Utf8String *ustr = utf8_create_c(dp->d_name);
                Instance *jstr = jstring_create(ustr, runtime);
                instance_hold_to_thread(jstr, runtime);
                utf8_destory(ustr);
                arraylist_push_back(files, jstr);
            }
            (void) closedir(dirp); //关闭目录

            s32 i;
            Utf8String *ustr = utf8_create_c(STR_CLASS_JAVA_LANG_STRING);
            Instance *jarr = jarray_create_by_type_name(runtime, files->length, ustr);
            utf8_destory(ustr);
            for (i = 0; i < files->length; i++) {
                __refer ref = arraylist_get_value(files, i);
                instance_release_from_thread(ref, runtime);
                jarray_set_field(jarr, i, (intptr_t) ref);
            }
            push_ref(runtime->stack, jarr);
        } else {
            push_ref(runtime->stack, NULL);
        }
        arraylist_destory(files);
        utf8_destory(filepath);
    } else {
        push_ref(runtime->stack, NULL);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_listDir  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_getcwd(Runtime *runtime, JClass *clazz) {
    Instance *path_arr = localvar_getRefer(runtime->localvar, 0);
    if (path_arr) {
        __refer ret = getcwd(path_arr->arr_body, path_arr->arr_length);
        push_int(runtime->stack, ret == path_arr->arr_body ? 0 : -1);
    } else {
        push_int(runtime->stack, -1);
    }
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
        s32 ret = chmod(path_arr->arr_body, mode);
        push_int(runtime->stack, ret);
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
        s32 ret = rename(old_arr->arr_body, new_arr->arr_body);
        push_int(runtime->stack, ret);
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
    Utf8String *tdir = getTmpDir();
    if (tdir) {
        Instance *jstr = jstring_create(tdir, runtime);
        utf8_destory(tdir);
        push_ref(runtime->stack, jstr);
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
#if __JVM_OS_MINGW__ || __JVM_OS_VS__
        ret = mkdir(path_arr->arr_body);
#else
        ret = mkdir(path_arr->arr_body, S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH);
#endif
        push_int(runtime->stack, ret);
    } else {
        push_int(runtime->stack, ret);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_mkdir  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_getOS(Runtime *runtime, JClass *clazz) {
#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
    push_int(runtime->stack, 1);
#else
    push_int(runtime->stack, 0);
#endif
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
        struct stat buf;
        stat(path_arr->arr_body, &buf);
        s32 a = S_ISDIR(buf.st_mode);
        if (a) {
            ret = rmdir(path_arr->arr_body);
        } else {
            ret = remove(path_arr->arr_body);
        }
        push_int(runtime->stack, ret);
    } else {
        push_int(runtime->stack, ret);
    }
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_delete0  \n");
#endif
    return 0;
}

s32 org_mini_zip_ZipFile_getEntry0(Runtime *runtime, JClass *clazz) {
    Instance *zip_path_arr = localvar_getRefer(runtime->localvar, 0);
    Instance *name_arr = localvar_getRefer(runtime->localvar, 1);
    s32 ret = -1;
    if (zip_path_arr && name_arr) {
        ByteBuf *buf = bytebuf_create(0);
        zip_loadfile(zip_path_arr->arr_body, name_arr->arr_body, buf);
        if (buf->wp) {
            Instance *arr = jarray_create_by_type_index(runtime, buf->wp, DATATYPE_BYTE);
            memmove(arr->arr_body, buf->buf, buf->wp);
            push_ref(runtime->stack, arr);
            ret = 0;
        }
        bytebuf_destory(buf);
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
    if (zip_path_arr && name_arr && content_arr) {
        zip_savefile_mem(zip_path_arr->arr_body, name_arr->arr_body, content_arr->arr_body, content_arr->arr_length);
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
            Instance *jarr = jarray_create_by_type_name(runtime, list->length, clustr);
            utf8_destory(clustr);
            instance_hold_to_thread(jarr, runtime);
            s32 i;
            for (i = 0; i < list->length; i++) {
                Utf8String *ustr = arraylist_get_value_unsafe(list, i);
                Instance *jstr = jstring_create(ustr, runtime);
                jarray_set_field(jarr, i, (s64) (intptr_t) jstr);
            }
            zip_destory_filenames_list(list);
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
    bytebuf_destory(data);
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
    bytebuf_destory(zip_data);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_zip_ZipFile_compress0  \n");
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

static java_native_method method_net_table[] = {
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
        {"org/mini/net/SocketNative", "sslc_connect",         "([B[B[B)I",                        org_mini_net_SocketNative_sslc_connect},
        {"org/mini/net/SocketNative", "sslc_close",           "([B)I",                            org_mini_net_SocketNative_sslc_close},
        {"org/mini/net/SocketNative", "sslc_read",            "([B[BII)I",                        org_mini_net_SocketNative_sslc_read},
        {"org/mini/net/SocketNative", "sslc_write",           "([B[BII)I",                        org_mini_net_SocketNative_sslc_write},
        {"org/mini/fs/InnerFile",     "openFile",             "([B[B)J",                          org_mini_fs_InnerFile_openFile},
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
        {"org/mini/fs/InnerFile",     "getcwd",               "([B)I",                            org_mini_fs_InnerFile_getcwd},
        {"org/mini/fs/InnerFile",     "chmod",                "([BI)I",                           org_mini_fs_InnerFile_chmod},
        {"org/mini/fs/InnerFile",     "mkdir0",               "([B)I",                            org_mini_fs_InnerFile_mkdir0},
        {"org/mini/fs/InnerFile",     "getOS",                "()I",                              org_mini_fs_InnerFile_getOS},
        {"org/mini/fs/InnerFile",     "delete0",              "([B)I",                            org_mini_fs_InnerFile_delete0},
        {"org/mini/fs/InnerFile",     "rename0",              "([B[B)I",                          org_mini_fs_InnerFile_rename0},
        {"org/mini/fs/InnerFile",     "getTmpDir",            "()Ljava/lang/String;",             org_mini_fs_InnerFile_getTmpDir},
        {"org/mini/zip/Zip",          "getEntry0",            "([B[B)[B",                         org_mini_zip_ZipFile_getEntry0},
        {"org/mini/zip/Zip",          "putEntry0",            "([B[B[B)I",                        org_mini_zip_ZipFile_putEntry0},
        {"org/mini/zip/Zip",          "fileCount0",           "([B)I",                            org_mini_zip_ZipFile_fileCount0},
        {"org/mini/zip/Zip",          "listFiles0",           "([B)[Ljava/lang/String;",          org_mini_zip_ZipFile_listFiles0},
        {"org/mini/zip/Zip",          "isDirectory0",         "([BI)I",                           org_mini_zip_ZipFile_isDirectory0},
        {"org/mini/zip/Zip",          "extract0",             "([B)[B",                           org_mini_zip_ZipFile_extract0},
        {"org/mini/zip/Zip",          "compress0",            "([B)[B",                           org_mini_zip_ZipFile_compress0},
        {"org/mini/crypt/XorCrypt",   "encrypt",              "([B[B)[B",                         org_mini_crypt_XorCrypt_encrypt},
        {"org/mini/crypt/XorCrypt",   "decrypt",              "([B[B)[B",                         org_mini_crypt_XorCrypt_decrypt},

};


void reg_net_native_lib() {
    native_reg_lib(&(method_net_table[0]), sizeof(method_net_table) / sizeof(java_native_method));
}


#ifdef __cplusplus
}
#endif
