/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   ustring.h
 * Author: gust
 *
 * Created on 2017年7月24日, 下午1:20
 */
#include "hashtable.h"

#ifndef UNI_STRING_H
#define UNI_STRING_H

#ifdef __cplusplus
extern "C" {
#endif
/*
 * utf8 string
 * 每字符1-6字节
 */
typedef unsigned char utf8_char;

typedef struct _utf8_string {
    utf8_char *data;

    int length;
    int _alloced;
    unsigned long hash;
} Utf8String;


Utf8String *utf8_create(void);

Utf8String *utf8_create_c(char *str);

Utf8String *utf8_create_part_c(char *str, int start, int len);

Utf8String *utf8_create_copy(Utf8String *str);

Utf8String *utf8_create_part(Utf8String *str, int start, int len);

void utf8_destory(Utf8String *);

void utf8_clear(Utf8String *ustr);

void utf8_append(Utf8String *a1, Utf8String *a2);

void utf8_append_c(Utf8String *a1, char *a2);

void utf8_append_part(Utf8String *a1, Utf8String *a2, int start, int len);

void utf8_append_part_c(Utf8String *a1, unsigned char *a2, int start, int len);

void utf8_append_data(Utf8String *a1, char *a2, s32 size);

s64 utf8_aton(Utf8String *sp, int n);

void utf8_upcase(Utf8String *a1);

void utf8_append_s64(Utf8String *a1, s64 val, int radix);

void utf8_substring(Utf8String *a1, int start, int end);

char *utf8_cstr(Utf8String *a1);

int utf8_indexof(Utf8String *a1, Utf8String *a2);

int utf8_indexof_c(Utf8String *a1, char *a2);

int utf8_indexof_pos(Utf8String *a1, Utf8String *a2, int a1_pos); // find a2 from a1, at a1+pos

int utf8_indexof_pos_c(Utf8String *a1, char *a2, int a1_pos);

int utf8_last_indexof(Utf8String *a1, Utf8String *a2);

int utf8_last_indexof_c(Utf8String *a1, char *a2);

int utf8_last_indexof_pos(Utf8String *a1, Utf8String *a2, int a1_rightpos);

int utf8_last_indexof_pos_c(Utf8String *a1, char *a2, int a1_rightpos);

void utf8_replace(Utf8String *a1, Utf8String *a2, Utf8String *a3);

void utf8_replace_c(Utf8String *a1, char *a2, char *a3);

int utf8_equals(Utf8String *a1, Utf8String *a2);

int utf8_equals_c(Utf8String *a1, char *a2);

//utf8_char utf8_char_at(Utf8String *a1, int pos);
static inline utf8_char utf8_char_at(Utf8String *a1, int pos) {
    return a1->data[pos];
}

int UNICODE_STR_EQUALS_FUNC(HashtableValue value1, HashtableValue value2);

u64 UNICODE_STR_HASH_FUNC(HashtableKey kmer);

unsigned long _utf8_hashCode(Utf8String *ustr);


int utf8_index_of(Utf8String *ustr, utf8_char data);

void utf8_remove(Utf8String *ustr, int index);

void utf8_remove_range(Utf8String *ustr, int index, int length);

int utf8_pushfront(Utf8String *ustr, utf8_char data);

int utf8_pushback(Utf8String *ustr, utf8_char data);

int utf8_insert(Utf8String *ustr, int index, utf8_char data);

int _utf8_enlarge(Utf8String *ustr);

int _utf8_space_require(Utf8String *ustr, int size);

#ifdef __cplusplus
}
#endif

#endif /* UNI_STRING_H */

