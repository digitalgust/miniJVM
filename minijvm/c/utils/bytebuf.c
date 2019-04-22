//
// Created by gust on 2017/12/16.
//
#include "memory.h"
#include "bytebuf.h"

ByteBuf *bytebuf_create(u32 size) {
    if (!size)size = 256;
    if (size) {
        ByteBuf *bf = jvm_calloc(sizeof(ByteBuf));
        bf->buf = jvm_calloc(size);
        bf->_alloc_size = size;
        return bf;
    }
    return NULL;
}

void bytebuf_destory(ByteBuf *bf) {
    if (bf) {
        jvm_free(bf->buf);
        jvm_free(bf);
    }
}

u32 bytebuf_available(ByteBuf *bf) {
    return bf->wp - bf->rp;
}


s32 bytebuf_write(ByteBuf *bf, s32 v) {
    if (bf->wp + sizeof(c8) > bf->_alloc_size)
        bytebuf_expand(bf, bf->_alloc_size << 1);

    bf->buf[bf->wp++] = v;
    return 1;
}


s32 bytebuf_write_batch(ByteBuf *bf, c8 *data, s32 size) {
    if (size < 0) {
        return -1;
    }
    if (bf->_alloc_size < bf->wp + size)//
        bytebuf_expand(bf, bf->wp + size);
    memcpy(&bf->buf[bf->wp], data, size);
    bf->wp += size;
    return size;
}

//-----------------------------------------------------------------------------------


s32 bytebuf_read(ByteBuf *bf) {
    if (bf->rp + 1 > bf->wp) {
        return -1;
    }
    s32 i = (u8) bf->buf[bf->rp++];
    return i;
}


s32 bytebuf_read_batch(ByteBuf *bf, c8 *data, s32 size) {
    s32 len = size < bf->wp - bf->rp ? size : bf->wp - bf->rp;
    if (len <= 0) {
        return -1;
    }
    memcpy(data, &bf->buf[bf->rp], len);
    bf->rp += len;
    return len;
}


s32 bytebuf_read_from(ByteBuf *bf, ByteBuf *src, s32 size) {
    if (size <= 0) {
        return size;
    }
    s32 len = size < src->wp - src->rp ? size : src->wp - src->rp;
    bytebuf_write_batch(bf, &src->buf[src->rp], len);
    src->rp += len;
    return len;
}


s32 bytebuf_write_to(ByteBuf *bf, ByteBuf *dst, s32 size) {
    if (size <= 0) {
        return size;
    }
    s32 len = size < bf->wp - bf->rp ? size : bf->wp - bf->rp;
    bytebuf_write_batch(dst, &bf->buf[bf->rp], len);
    bf->rp += len;
    return len;
}
//------------------------------------private ---------------------------------------


void bytebuf_expand(ByteBuf *bf, u32 size) {
    if (size == 0) {
        return;
    }
//    if (size == 1854) {
//        int debug = 1;
//    }
    void *p = jvm_realloc(bf->buf, size);
    if (p) {
        bf->buf = p;
        bf->_alloc_size = size;
    }
}


s32 bytebuf_chkread(ByteBuf *bf, u32 pos) {
    if (pos > bf->wp) {
        return 0;
    }
    return 1;
}
