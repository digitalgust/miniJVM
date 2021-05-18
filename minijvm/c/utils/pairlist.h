//
// Created by gust on 2017/8/25.
//

#ifndef MINI_JVM_PAIRLIST_H
#define MINI_JVM_PAIRLIST_H



#ifdef __cplusplus
extern "C" {
#endif

#include "d_type.h"

typedef struct _Pair {
    union {
        __refer left;
        intptr_t leftl;
    };
    union {
        __refer right;
        intptr_t rightl;
    };
} Pair;

typedef struct _Pairlist {
    Pair *ptr;
    s32 count;
    s32 _alloced;
} Pairlist;


static inline Pairlist *pairlist_create(s32 len) {
    if (len <= 0) {
        len = 4;
    }
    Pairlist *list = (Pairlist *) jvm_calloc(sizeof(Pairlist));//每个位置放两个指针
    if (list) {
        list->ptr = jvm_calloc(sizeof(Pair) * len);//每个位置放两个指针
        list->_alloced = len;
        list->count = 0;
        return list;
    }
    return NULL;
};

static inline void pairlist_destory(Pairlist *list) {
    if (list) {
        jvm_free(list->ptr);
        jvm_free(list);
    }
};

static inline __refer pairlist_get(Pairlist *list, __refer left) {
    Pair *start = list->ptr;
    Pair *end = start + list->count;
    for (; start < end; start++) {
        if (start->left == left) {
            return start->right;
        }
    }
    return NULL;
};

static inline intptr_t pairlist_getl(Pairlist *list, intptr_t left) {
    return (intptr_t) pairlist_get(list, (__refer) left);
}

static inline s32 pairlist_put(Pairlist *list, __refer left, __refer right) {
    if (list->count >= list->_alloced) {//空间不足
        s32 newSize = list->_alloced << 1;
        void *p = jvm_realloc(list->ptr, (newSize << 1) * (sizeof(__refer) << 1));
        list->_alloced = newSize;
        list->ptr = p;
    }

    Pair *start = list->ptr;
    Pair *end = start + list->count;
    for (; start < end; start++) {
        if (start->left == left) {
            start->right = right;
            return 1;
        }
    }
    start->left = left;
    start->right = right;
    list->count++;
    return 0;
};


static inline s32 pairlist_putl(Pairlist *list, intptr_t left, intptr_t right) {
    return pairlist_put(list, (__refer) left, (__refer) right);
}

static inline Pair pairlist_get_pair(Pairlist *list, s32 index) {
    return list->ptr[index];
};

static inline __refer pairlist_remove(Pairlist *list, __refer left) {
    __refer right = NULL;

    Pair *start = list->ptr;
    Pair *end = start + list->count;
    for (; start < end; start++) {
        if (start->left == left) {
            memmove(start, start + 1, ((c8 *) end) - ((c8 *) (start + 1)));
            list->count--;
            break;
        }
    }
    return right;
};

static inline intptr_t pairlist_removel(Pairlist *list, intptr_t left) {
    return (intptr_t) pairlist_remove(list, (__refer) left);
}


#ifdef __cplusplus
}
#endif

#endif //MINI_JVM_PAIRLIST_H
