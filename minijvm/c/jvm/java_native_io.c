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
#include <stdlib.h>
#include <limits.h>

#define   err jvm_printf

#ifdef __cplusplus
extern "C" {
#endif

#include <string.h>   // NULL and possibly memcpy, memset

#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__

#include <WinSock2.h>
#include <Ws2tcpip.h>

typedef int socklen_t;
#pragma comment(lib, "Ws2_32.lib")
#define SHUT_RDWR SD_BOTH
#define SHUT_RD SD_RECEIVE
#define SHUT_WR SD_SEND
#else

#include <sys/types.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <unistd.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <fcntl.h>

#define INVALID_SOCKET    -1
#define SOCKET_ERROR      -1
#define closesocket(fd)   close(fd)
#endif

#if __JVM_OS_VS__
#include "../utils/dirent_win.h"
#else

#include <dirent.h>
#include <unistd.h>

#endif

#include <errno.h>


//=================================  socket  ====================================
s32 sock_option(s32 sockfd, s32 opType, s32 opValue, s32 opValue2) {
    s32 ret = 0;
    switch (opType) {
        case SOCK_OP_TYPE_NON_BLOCK: {//阻塞设置

#if __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__ || __JVM_OS_VS__
#if __JVM_OS_CYGWIN__
            __ms_u_long ul = 1;
//fix cygwin bug ,cygwin FIONBIO = 0x8008667E
#undef FIONBIO
#define FIONBIO 0x8004667E
#else
            u_long ul = 1;
#endif
            if (!opValue) {
                ul = 0;
            }
            //jvm_printf(" FIONBIO:%x\n", FIONBIO);
            ret = ioctlsocket(sockfd, FIONBIO, &ul);
            if (ret == SOCKET_ERROR) {
                err("set socket non_block error: %s\n", strerror(errno));
                s32 ec = WSAGetLastError();
                //jvm_printf(" error code:%d\n", ec);
            }
#else
            if (opValue) {
                s32 flags = fcntl(sockfd, F_GETFL, 0);
                ret = fcntl(sockfd, F_SETFL, flags | O_NONBLOCK);
                if (ret) {
                    //err("set socket non_block error.\n");
                    //printf("errno.%02d is: %s\n", errno, strerror(errno));
                }
            } else {
                //fcntl(sockfd, F_SETFL, O_BLOCK);
            }
#endif
            break;
        }
        case SOCK_OP_TYPE_REUSEADDR: {//
            s32 x = 1;
            ret = setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, (char *) &x, sizeof(x));
            break;
        }
        case SOCK_OP_TYPE_RCVBUF: {//缓冲区设置
            int nVal = opValue;//设置为 opValue K
            ret = setsockopt(sockfd, SOL_SOCKET, SO_RCVBUF, (const char *) &nVal, sizeof(nVal));
            break;
        }
        case SOCK_OP_TYPE_SNDBUF: {//缓冲区设置
            s32 nVal = opValue;//设置为 opValue K
            ret = setsockopt(sockfd, SOL_SOCKET, SO_SNDBUF, (const char *) &nVal, sizeof(nVal));
            break;
        }
        case SOCK_OP_TYPE_TIMEOUT: {
#if __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__ || __JVM_OS_VS__
            s32 nTime = opValue;
            ret = setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, (const char *) &nTime, sizeof(nTime));
#else
            struct timeval timeout = {opValue / 1000, (opValue % 1000) * 1000};
            ret = setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
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
            ret = setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, (const char *) &m_sLinger, sizeof(m_sLinger));
            break;
        }
        case SOCK_OP_TYPE_KEEPALIVE: {
            s32 val = opValue;
            ret = setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, (const char *) &val, sizeof(val));
            break;
        }
    }
    return ret;
}

s32 sock_get_option(s32 sockfd, s32 opType) {
    s32 ret = 0;
    socklen_t len;

    switch (opType) {
        case SOCK_OP_TYPE_NON_BLOCK: {//阻塞设置
#if __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__ || __JVM_OS_VS__
            u_long flags = 1;
            ret = NO_ERROR == ioctlsocket(sockfd, FIONBIO, &flags);
#else
            int flags;
            if ((flags = fcntl(sockfd, F_GETFL, NULL)) < 0) {
                ret = -1;
            } else {
                ret = (flags & O_NONBLOCK);
            }
#endif
            break;
        }
        case SOCK_OP_TYPE_REUSEADDR: {//
            len = sizeof(ret);
            getsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, (void *) &ret, &len);

            break;
        }
        case SOCK_OP_TYPE_RCVBUF: {
            len = sizeof(ret);
            getsockopt(sockfd, SOL_SOCKET, SO_RCVBUF, (void *) &ret, &len);
            break;
        }
        case SOCK_OP_TYPE_SNDBUF: {//缓冲区设置
            len = sizeof(ret);
            getsockopt(sockfd, SOL_SOCKET, SO_SNDBUF, (void *) &ret, &len);
            break;
        }
        case SOCK_OP_TYPE_TIMEOUT: {

#if __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__ || __JVM_OS_VS__
            len = sizeof(ret);
            getsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, (void *) &ret, &len);
#else
            struct timeval timeout = {0, 0};
            len = sizeof(timeout);
            getsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, &timeout, &len);
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
            getsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, (void *) &m_sLinger, &len);
            ret = *((s32 *) &m_sLinger);
            break;
        }
        case SOCK_OP_TYPE_KEEPALIVE: {
            len = sizeof(ret);
            getsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, (void *) &ret, &len);
            break;
        }
    }
    return ret;
}

s32 sock_recv(s32 sockfd, c8 *buf, s32 count) {
    s32 len = (s32) recv(sockfd, buf, count, 0);

    if (len == 0) {//如果是正常断开，返回-1
        len = -1;
    } else if (len == -1) {//如果发生错误
        len = -1;
#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
        if (WSAEWOULDBLOCK == WSAGetLastError()) {//但是如果是非阻塞端口，说明连接仍正常
            //jvm_printf("sc send error client time = %f ;\n", (f64)clock());
            len = -2;
        }
#else
        if (errno == EWOULDBLOCK || errno == EAGAIN) {
            len = -2;
        }
#endif
    }
    return len;
}


s32 sock_send(s32 sockfd, c8 *buf, s32 count) {
    s32 len = (s32) send(sockfd, buf, count, 0);

    if (len == 0) {//如果是正常断开，返回-1
        len = -1;
    } else if (len == -1) {//如果发生错误
#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
        if (WSAEWOULDBLOCK == WSAGetLastError()) {//但是如果是非阻塞端口，说明连接仍正常
            //jvm_printf("sc send error server time = %f ;\n", (f64)clock());
            len = -2;
        }
#else
        if (errno == EWOULDBLOCK || errno == EAGAIN) {
            len = -2;
        }
#endif

    }
    return len;
}

s32 sock_open() {
    s32 sockfd = -1;

#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
    WSADATA wsaData;
    WSAStartup(MAKEWORD(1, 1), &wsaData);
#endif  /*  WIN32  */
    if ((sockfd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)) == -1) {
        //err(strerror(errno));
        //err("socket init error: %s\n", strerror(errno));
    }
    return sockfd;
}


s32 sock_connect(s32 sockfd, Utf8String *remote_ip, s32 remote_port) {
    s32 ret = 0;

    struct hostent *host;
    if ((host = gethostbyname(utf8_cstr(remote_ip))) == NULL) { /* get the host info */
        //err("get host by name error: %s\n", strerror(errno));
        ret = -1;
    } else {


        s32 x = 1;
        if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, (char *) &x, sizeof(x)) == -1) {
            //err("socket reuseaddr error: %s\n", strerror(errno));
            ret = -1;
        } else {
            struct sockaddr_in sock_addr; /* connector's address information */
            memset((char *) &sock_addr, 0, sizeof(sock_addr));
            sock_addr.sin_family = AF_INET; /* host byte order */
            sock_addr.sin_port = htons((u16) remote_port); /* short, network byte order */
#if __JVM_OS_MAC__ || __JVM_OS_LINUX__
            sock_addr.sin_addr = *((struct in_addr *) host->h_addr_list[0]);
#else
            sock_addr.sin_addr = *((struct in_addr *) host->h_addr);
#endif
            memset(&(sock_addr.sin_zero), 0, sizeof((sock_addr.sin_zero))); /* zero the rest of the struct */
            if (connect(sockfd, (struct sockaddr *) &sock_addr, sizeof(sock_addr)) == -1) {
                //err("socket connect error: %s\n", strerror(errno));
                ret = -1;
            }
        }
    }
    return ret;
}

s32 sock_bind(s32 sockfd, Utf8String *local_ip, s32 local_port) {
    s32 ret = 0;
    struct sockaddr_in addr;

    struct hostent *host;

    memset((char *) &addr, 0, sizeof(addr));//清0
    addr.sin_family = AF_INET;
    addr.sin_port = htons(local_port);
    if (local_ip->length) {//如果指定了ip
        if ((host = gethostbyname(utf8_cstr(local_ip))) == NULL) { /* get the host info */
            //err("get host by name error: %s\n", strerror(errno));
            ret = -1;
        }
#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
        addr.sin_addr = *((struct in_addr *) host->h_addr);
#else
        //server_addr.sin_len = sizeof(struct sockaddr_in);
        addr.sin_addr = *((struct in_addr *) host->h_addr_list[0]);
#endif
    } else {
        addr.sin_addr.s_addr = htonl(INADDR_ANY);
    }

    s32 on = 1;
    setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, (char *) &on, sizeof(on));
    if ((bind(sockfd, (struct sockaddr *) &addr, sizeof(addr))) < 0) {
        //err("Error binding serversocket: %s\n", strerror(errno));
        closesocket(sockfd);
        ret = -1;
    }
    return ret;
}


s32 sock_listen(s32 listenfd) {
    u16 MAX_LISTEN = 64;
    if ((listen(listenfd, MAX_LISTEN)) < 0) {
        //err("Error listening on serversocket: %s\n", strerror(errno));
        return -1;
    }
    return 0;
}

s32 sock_accept(s32 listenfd) {
    struct sockaddr_in clt_addr;
    memset(&clt_addr, 0, sizeof(clt_addr)); //清0
    s32 clt_addr_length = sizeof(clt_addr);
    s32 clt_socket_fd = accept(listenfd, (struct sockaddr *) &clt_addr, (socklen_t *) &clt_addr_length);
    if (clt_socket_fd == -1) {
        if (errno != EINTR) {
            //err("Error accepting on serversocket: %s\n", strerror(errno));
        }
    }

    return clt_socket_fd;
}

s32 sock_close(s32 listenfd) {
    shutdown(listenfd, SHUT_RDWR);
    closesocket(listenfd);
#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
    //can not cleanup , maybe other socket is alive
//        WSACancelBlockingCall();
//        WSACleanup();
#endif

    return 0;
}


s32 host_2_ip4(Utf8String *hostname) {
    s32 addr;

#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
    WSADATA wsaData;
    WSAStartup(MAKEWORD(1, 1), &wsaData);
#endif  /*  WIN32  */
    struct hostent *host;
    if ((host = gethostbyname(utf8_cstr(hostname))) == NULL) { /* get the host info */
        //err("get host by name error: %s\n", strerror(errno));
        addr = -1;
    }
#if __JVM_OS_MAC__ || __JVM_OS_LINUX__
    addr = ((struct in_addr *) host->h_addr_list[0])->s_addr;
#else
    addr = ((struct in_addr *) host->h_addr)->s_addr;
#endif
    return addr;
}


s32 isDir(Utf8String *path) {
    struct stat buf;
    stat(utf8_cstr(path), &buf);
    s32 a = S_ISDIR(buf.st_mode);
    return a;
}

Utf8String *getTmpDir() {
    Utf8String *tmps = utf8_create();
#if __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__ || __JVM_OS_VS__
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
    s32 sockfd = sock_open();
    jthread_block_exit(runtime);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_open0  \n");
#endif
    push_int(stack, sockfd);
    return 0;
}

s32 org_mini_net_SocketNative_bind0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    s32 port = localvar_getInt(runtime->localvar, 2);
    Utf8String *ip = utf8_create_part_c(jbyte_arr->arr_body, 0, jbyte_arr->arr_length);

    jthread_block_enter(runtime);
    s32 ret = sock_bind(sockfd, ip, port);
    jthread_block_exit(runtime);
    utf8_destory(ip);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_open0  \n");
#endif
    push_int(stack, ret);
    return 0;
}

s32 org_mini_net_SocketNative_connect0(Runtime *runtime, JClass *clazz) {
    RuntimeStack *stack = runtime->stack;
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    s32 port = localvar_getInt(runtime->localvar, 2);
    Utf8String *ip = utf8_create_part_c(jbyte_arr->arr_body, 0, jbyte_arr->arr_length);

    jthread_block_enter(runtime);
    s32 ret = sock_connect(sockfd, ip, port);
    jthread_block_exit(runtime);
    utf8_destory(ip);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_open0  \n");
#endif
    push_int(stack, ret);
    return 0;
}

s32 org_mini_net_SocketNative_readBuf(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    s32 offset = localvar_getInt(runtime->localvar, 2);
    s32 count = localvar_getInt(runtime->localvar, 3);

    jthread_block_enter(runtime);
    s32 len = sock_recv(sockfd, jbyte_arr->arr_body + offset, count);
    jthread_block_exit(runtime);
    push_int(runtime->stack, len);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_readBuf  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_readByte(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    c8 b = 0;
    jthread_block_enter(runtime);
    s32 len = sock_recv(sockfd, &b, 1);
    jthread_block_exit(runtime);
    if (len < 0) {
        push_int(runtime->stack, len);
    } else {
        push_int(runtime->stack, (u8) b);

    }

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_readByte  \n");
#endif
    return 0;
}


s32 org_mini_net_SocketNative_writeBuf(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 1);
    s32 offset = localvar_getInt(runtime->localvar, 2);
    s32 count = localvar_getInt(runtime->localvar, 3);

    jthread_block_enter(runtime);
    s32 len = sock_send(sockfd, jbyte_arr->arr_body + offset, count);
    jthread_block_exit(runtime);

    push_int(runtime->stack, len);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_writeBuf  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_writeByte(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    s32 val = localvar_getInt(runtime->localvar, 1);
    c8 b = (u8) val;
    jthread_block_enter(runtime);
    s32 len = sock_send(sockfd, &b, 1);
    jthread_block_exit(runtime);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_writeByte  \n");
#endif
    push_int(runtime->stack, len);
    return 0;
}

s32 org_mini_net_SocketNative_available0(Runtime *runtime, JClass *clazz) {
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_available0  \n");
#endif
    push_int(runtime->stack, 0);
    return 0;
}

s32 org_mini_net_SocketNative_close0(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    sock_close(sockfd);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_close0  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_setOption0(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    s32 type = localvar_getInt(runtime->localvar, 1);
    s32 val = localvar_getInt(runtime->localvar, 2);
    s32 val2 = localvar_getInt(runtime->localvar, 3);
    s32 ret = 0;
    if (sockfd) {
        ret = sock_option(sockfd, type, val, val2);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_setOption0  \n");
#endif
    return 0;
}


s32 org_mini_net_SocketNative_getOption0(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    s32 type = localvar_getInt(runtime->localvar, 1);

    s32 ret = 0;
    if (sockfd) {
        ret = sock_get_option(sockfd, type);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_getOption0  \n");
#endif
    return 0;
}


s32 org_mini_net_SocketNative_listen0(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    s32 ret = 0;
    if (sockfd) {
        jthread_block_enter(runtime);
        ret = sock_listen(sockfd);
        jthread_block_exit(runtime);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_listen0  \n");
#endif
    return 0;
}


s32 org_mini_net_SocketNative_accept0(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    s32 ret = 0;
    if (sockfd) {

        jthread_block_enter(runtime);
        ret = sock_accept(sockfd);
        jthread_block_exit(runtime);
    }
    push_int(runtime->stack, ret);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_accept0  \n");
#endif
    return 0;
}


s32 org_mini_net_SocketNative_registerCleanup(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    s32 ret = 0;
    if (sockfd) {

    }

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_registerCleanup  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_finalize(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    if (sockfd) {
        close(sockfd);
    }

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_finalize  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_getSockAddr(Runtime *runtime, JClass *clazz) {
    s32 sockfd = localvar_getInt(runtime->localvar, 0);
    s32 mode = localvar_getInt(runtime->localvar, 1);
    if (sockfd) {
        struct sockaddr_in sock;
        socklen_t slen = sizeof(sock);
        if (mode == 0) {
            getpeername(sockfd, (struct sockaddr *) &sock, &slen);
        } else if (mode == 1) {
            getsockname(sockfd, (struct sockaddr *) &sock, &slen);
        }
#if __JVM_OS_MAC__ || __JVM_OS_LINUX__
#else
#endif
        char ipAddr[INET_ADDRSTRLEN];//保存点分十进制的地址
        Utf8String *ustr = utf8_create();
#if __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
        c8 *ipstr = inet_ntoa(sock.sin_addr);
        strcpy(ipAddr, ipstr);
#else
        inet_ntop(AF_INET, &sock.sin_addr, ipAddr, sizeof(ipAddr));
#endif
        int port = ntohs(sock.sin_port);
        utf8_append_c(ustr, ipAddr);
        utf8_append_c(ustr, ":");
        utf8_append_s64(ustr, port, 10);
        Instance *jstr = jstring_create(ustr, runtime);
        utf8_destory(ustr);
        push_ref(runtime->stack, jstr);
    }

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_getSockAddr  \n");
#endif
    return 0;
}

s32 org_mini_net_SocketNative_host2ip4(Runtime *runtime, JClass *clazz) {
    s32 addr = -1;
    Instance *jbyte_arr = (Instance *) localvar_getRefer(runtime->localvar, 0);
    if (jbyte_arr) {
        Utf8String *ip = utf8_create_part_c(jbyte_arr->arr_body, 0, jbyte_arr->arr_length);
        addr = host_2_ip4(ip);
        utf8_destory(ip);
    }
    push_int(runtime->stack, addr);
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_net_SocketNative_host2ip4  \n");
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

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
                gc_refer_hold(jstr);
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
                gc_refer_release(ref);
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
    invoke_deepth(runtime);
    jvm_printf("org_mini_fs_InnerFile_getTmpDir  \n");
#endif
    return 0;
}

s32 org_mini_fs_InnerFile_mkdir0(Runtime *runtime, JClass *clazz) {
    Instance *path_arr = localvar_getRefer(runtime->localvar, 0);
    s32 ret = -1;
    if (path_arr) {
#if __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__ || __JVM_OS_VS__
        ret = mkdir(path_arr->arr_body);
#else
        ret = mkdir(path_arr->arr_body, S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH);
#endif
        push_int(runtime->stack, ret);
    } else {
        push_int(runtime->stack, ret);
    }
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
            gc_refer_hold(jarr);
            s32 i;
            for (i = 0; i < list->length; i++) {
                Utf8String *ustr = arraylist_get_value_unsafe(list, i);
                Instance *jstr = jstring_create(ustr, runtime);
                jarray_set_field(jarr, i, (s64) (intptr_t) jstr);
            }
            zip_destory_filenames_list(list);
            gc_refer_release(jarr);
            push_ref(runtime->stack, jarr);
            ret = 0;
        }
    }
    if (ret == -1) {
        push_ref(runtime->stack, NULL);
    }
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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

#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
#if _JVM_DEBUG_BYTECODE_DETAIL > 5
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
        {"org/mini/net/SocketNative", "open0",           "()I",                              org_mini_net_SocketNative_open0},
        {"org/mini/net/SocketNative", "bind0",           "(I[BI)I",                          org_mini_net_SocketNative_bind0},
        {"org/mini/net/SocketNative", "connect0",        "(I[BI)I",                          org_mini_net_SocketNative_connect0},
        {"org/mini/net/SocketNative", "listen0",         "(I)I",                             org_mini_net_SocketNative_listen0},
        {"org/mini/net/SocketNative", "accept0",         "(I)I",                             org_mini_net_SocketNative_accept0},
        {"org/mini/net/SocketNative", "registerCleanup", "()V",                              org_mini_net_SocketNative_registerCleanup},
        {"org/mini/net/SocketNative", "readBuf",         "(I[BII)I",                         org_mini_net_SocketNative_readBuf},
        {"org/mini/net/SocketNative", "readByte",        "(I)I",                             org_mini_net_SocketNative_readByte},
        {"org/mini/net/SocketNative", "writeBuf",        "(I[BII)I",                         org_mini_net_SocketNative_writeBuf},
        {"org/mini/net/SocketNative", "writeByte",       "(II)I",                            org_mini_net_SocketNative_writeByte},
        {"org/mini/net/SocketNative", "available0",      "(I)I",                             org_mini_net_SocketNative_available0},
        {"org/mini/net/SocketNative", "close0",          "(I)V",                             org_mini_net_SocketNative_close0},
        {"org/mini/net/SocketNative", "setOption0",      "(IIII)I",                          org_mini_net_SocketNative_setOption0},
        {"org/mini/net/SocketNative", "getOption0",      "(II)I",                            org_mini_net_SocketNative_getOption0},
        {"org/mini/net/SocketNative", "finalize",        "()V",                              org_mini_net_SocketNative_finalize},
        {"org/mini/net/SocketNative", "getSockAddr",     "(II)Ljava/lang/String;",           org_mini_net_SocketNative_getSockAddr},
        {"org/mini/net/SocketNative", "host2ip4",        "([B)I",                            org_mini_net_SocketNative_host2ip4},
        {"org/mini/fs/InnerFile",     "openFile",        "([B[B)J",                          org_mini_fs_InnerFile_openFile},
        {"org/mini/fs/InnerFile",     "closeFile",       "(J)I",                             org_mini_fs_InnerFile_closeFile},
        {"org/mini/fs/InnerFile",     "read0",           "(J)I",                             org_mini_fs_InnerFile_read0},
        {"org/mini/fs/InnerFile",     "write0",          "(JI)I",                            org_mini_fs_InnerFile_write0},
        {"org/mini/fs/InnerFile",     "readbuf",         "(J[BII)I",                         org_mini_fs_InnerFile_readbuf},
        {"org/mini/fs/InnerFile",     "writebuf",        "(J[BII)I",                         org_mini_fs_InnerFile_writebuf},
        {"org/mini/fs/InnerFile",     "seek0",           "(JJ)I",                            org_mini_fs_InnerFile_seek0},
        {"org/mini/fs/InnerFile",     "available0",      "(J)I",                             org_mini_fs_InnerFile_available0},
        {"org/mini/fs/InnerFile",     "setLength0",      "(JJ)I",                            org_mini_fs_InnerFile_setLength0},
        {"org/mini/fs/InnerFile",     "flush0",          "(J)I",                             org_mini_fs_InnerFile_flush0},
        {"org/mini/fs/InnerFile",     "loadFS",          "([BLorg/mini/fs/InnerFileStat;)I", org_mini_fs_InnerFile_loadFS},
        {"org/mini/fs/InnerFile",     "listDir",         "([B)[Ljava/lang/String;",          org_mini_fs_InnerFile_listDir},
        {"org/mini/fs/InnerFile",     "getcwd",          "([B)I",                            org_mini_fs_InnerFile_getcwd},
        {"org/mini/fs/InnerFile",     "chmod",           "([BI)I",                           org_mini_fs_InnerFile_chmod},
        {"org/mini/fs/InnerFile",     "mkdir0",          "([B)I",                            org_mini_fs_InnerFile_mkdir0},
        {"org/mini/fs/InnerFile",     "getOS",           "()I",                              org_mini_fs_InnerFile_getOS},
        {"org/mini/fs/InnerFile",     "delete0",         "([B)I",                            org_mini_fs_InnerFile_delete0},
        {"org/mini/fs/InnerFile",     "rename0",         "([B[B)I",                          org_mini_fs_InnerFile_rename0},
        {"org/mini/fs/InnerFile",     "getTmpDir",       "()Ljava/lang/String;",             org_mini_fs_InnerFile_getTmpDir},
        {"org/mini/zip/Zip",          "getEntry0",       "([B[B)[B",                         org_mini_zip_ZipFile_getEntry0},
        {"org/mini/zip/Zip",          "putEntry0",       "([B[B[B)I",                        org_mini_zip_ZipFile_putEntry0},
        {"org/mini/zip/Zip",          "fileCount0",      "([B)I",                            org_mini_zip_ZipFile_fileCount0},
        {"org/mini/zip/Zip",          "listFiles0",      "([B)[Ljava/lang/String;",          org_mini_zip_ZipFile_listFiles0},
        {"org/mini/zip/Zip",          "isDirectory0",    "([BI)I",                           org_mini_zip_ZipFile_isDirectory0},
        {"org/mini/zip/Zip",          "extract0",        "([B)[B",                           org_mini_zip_ZipFile_extract0},
        {"org/mini/zip/Zip",          "compress0",       "([B)[B",                           org_mini_zip_ZipFile_compress0},
        {"org/mini/crypt/XorCrypt",   "encrypt",         "([B[B)[B",                         org_mini_crypt_XorCrypt_encrypt},
        {"org/mini/crypt/XorCrypt",   "decrypt",         "([B[B)[B",                         org_mini_crypt_XorCrypt_decrypt},

};


void reg_net_native_lib() {
    native_reg_lib(&(method_net_table[0]), sizeof(method_net_table) / sizeof(java_native_method));
}


#ifdef __cplusplus
}
#endif
