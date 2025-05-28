//
// Created by gust on 2017/12/16.
//

#ifndef MINI_JVM_BYTEBUF_H
#define MINI_JVM_BYTEBUF_H
#ifdef __cplusplus
extern "C" {
#endif

#include  "d_type.h"

typedef struct _ByteBuf {
    u32 _alloc_size;
    u32 rp, wp;
    c8 *buf;
} ByteBuf;


ByteBuf *bytebuf_create(u32 size);

void bytebuf_destroy(ByteBuf *bf);

u32 bytebuf_available(ByteBuf *bf);

s32 bytebuf_write(ByteBuf *bf, s32 i);

s32 bytebuf_write_byte(ByteBuf *bf, c8 v);

s32 bytebuf_write_short(ByteBuf *bf, s16 v);

s32 bytebuf_write_int(ByteBuf *bf, s32 v);

s32 bytebuf_write_long(ByteBuf *bf, s64 v);

s32 bytebuf_write_batch(ByteBuf *bf, c8 *src, s32 size);

s32 bytebuf_read(ByteBuf *bf);

c8 bytebuf_read_byte(ByteBuf *bf);

s16 bytebuf_read_short(ByteBuf *bf);

s32 bytebuf_read_int(ByteBuf *bf);

s64 bytebuf_read_long(ByteBuf *bf);

s32 bytebuf_read_batch(ByteBuf *bf, c8 *dst, s32 size);

s32 bytebuf_read_from(ByteBuf *bf, ByteBuf *src, s32 size);

s32 bytebuf_write_to(ByteBuf *bf, ByteBuf *dst, s32 size);

void bytebuf_expand(ByteBuf *bf, u32 size);

s32 bytebuf_chkread(ByteBuf *bf, u32 pos);

s32 bytebuf_hasmore(ByteBuf *bf, u32 size);

#ifdef __cplusplus
};
#endif
#endif //MINI_JVM_BYTEBUF_H
