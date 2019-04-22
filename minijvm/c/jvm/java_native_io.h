//
// Created by gust on 2017/9/20.
//

#ifndef MINI_JVM_JAVA_NATIVE_IO_H
#define MINI_JVM_JAVA_NATIVE_IO_H

#ifdef __cplusplus
extern "C" {
#endif

#include "../utils/utf8_string.h"

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

s32 sock_open();

s32 sock_connect(s32 sockfd, Utf8String *remote_ip, s32 remote_port);

s32 sock_bind(s32 sockfd, Utf8String *local_ip, s32 local_port);

s32 sock_send(s32 sockfd, c8 *buf, s32 count);

s32 sock_recv(s32 sockfd, c8 *buf, s32 count);

s32 sock_option(s32 sockfd, s32 opType, s32 opValue, s32 opValue2);

s32 sock_listen(s32 listenfd);

s32 sock_accept(s32 listenfd);

s32 sock_close(s32 listenfd);

s32 isDir(Utf8String *path);

#ifdef __cplusplus
}
#endif


#endif //MINI_JVM_JAVA_NATIVE_IO_H
