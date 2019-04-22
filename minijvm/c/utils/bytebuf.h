//
// Created by gust on 2017/12/16.
//

#ifndef MINI_JVM_BYTEBUF_H
#define MINI_JVM_BYTEBUF_H

#include  "d_type.h"

typedef struct _ByteBuf {
    u32 _alloc_size;
    u32 rp, wp;
    c8 *buf;
} ByteBuf;


ByteBuf *bytebuf_create(u32 size);

void bytebuf_destory(ByteBuf *bf);

u32 bytebuf_available(ByteBuf *bf);

s32 bytebuf_write(ByteBuf *bf, s32 i);

s32 bytebuf_write_batch(ByteBuf *bf, c8 *src, s32 size);

s32 bytebuf_read(ByteBuf *bf);

s32 bytebuf_read_batch(ByteBuf *bf, c8 *dst, s32 size);

s32 bytebuf_read_from(ByteBuf *bf, ByteBuf *src, s32 size);

s32 bytebuf_write_to(ByteBuf *bf, ByteBuf *dst, s32 size);

void bytebuf_expand(ByteBuf *bf, u32 size);

s32 bytebuf_chkread(ByteBuf *bf, u32 pos);

#endif //MINI_JVM_BYTEBUF_H
