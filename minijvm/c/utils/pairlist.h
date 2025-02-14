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
    Pairlist *list = (Pairlist *) jvm_calloc(sizeof(Pairlist));
    if (!list) {
        return NULL;
    }
    list->ptr = (Pair *) jvm_calloc(sizeof(Pair) * len);
    if (!list->ptr) {
        jvm_free(list);
        return NULL;
    }
    list->_alloced = len;
    list->count = 0;
    return list;
}

static inline void pairlist_destroy(Pairlist *list) {
    if (list) {
        if (list->ptr) {
            jvm_free(list->ptr);
        }
        jvm_free(list);
    }
}

static inline __refer pairlist_get(Pairlist *list, __refer left) {
    if (!list || !list->ptr) return NULL;

    Pair *start = list->ptr;
    Pair *end = start + list->count;
    for (; start < end; start++) {
        if (start->left == left) {
            return start->right;
        }
    }
    return NULL;
}

static inline intptr_t pairlist_getl(Pairlist *list, intptr_t left) {
    return (intptr_t) pairlist_get(list, (__refer) left);
}

static inline s32 pairlist_put(Pairlist *list, __refer left, __refer right) {
    if (!list) return -1;

    // First try to update existing entry
    Pair *start = list->ptr;
    Pair *end = start + list->count;
    for (; start < end; start++) {
        if (start->left == left) {
            start->right = right;
            return 1;
        }
    }

    // Need to add new entry, check if reallocation needed
    if (list->count >= list->_alloced) {
        s32 newSize = list->_alloced << 1;
        Pair *newPtr = (Pair *) jvm_realloc(list->ptr, newSize * sizeof(Pair));
        if (!newPtr) {
            return -1;
        }
        list->ptr = newPtr;
        list->_alloced = newSize;
    }

    // Add new entry
    list->ptr[list->count].left = left;
    list->ptr[list->count].right = right;
    list->count++;
    return 0;
}


static inline s32 pairlist_putl(Pairlist *list, intptr_t left, intptr_t right) {
    return pairlist_put(list, (__refer) left, (__refer) right);
}

static inline Pair pairlist_get_pair(Pairlist *list, s32 index) {
    Pair empty = {0};
    if (!list || !list->ptr || index < 0 || index >= list->count) {
        return empty;
    }
    return list->ptr[index];
}

static inline __refer pairlist_remove(Pairlist *list, __refer left) {
    if (!list || !list->ptr) return NULL;

    Pair *start = list->ptr;
    Pair *end = start + list->count;
    for (; start < end; start++) {
        if (start->left == left) {
            __refer right = start->right;
            if (start + 1 < end) {
                memmove(start, start + 1, (end - (start + 1)) * sizeof(Pair));
            }
            list->count--;
            return right;
        }
    }
    return NULL;
}

static inline intptr_t pairlist_removel(Pairlist *list, intptr_t left) {
    return (intptr_t) pairlist_remove(list, (__refer) left);
}


#ifdef __cplusplus
}
#endif

#endif //MINI_JVM_PAIRLIST_H
