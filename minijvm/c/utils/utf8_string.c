#include <stdlib.h>
#include <string.h>
#include "utf8_string.h"
#include "d_type.h"

Utf8String *utf8_create() {

    int length = 32;
    Utf8String *uni_str;

    uni_str = (Utf8String *) jvm_calloc(sizeof(Utf8String));

    if (uni_str == NULL) {
        return NULL;
    }

    uni_str->_alloced = length;
    uni_str->length = 0;
    uni_str->hash = 0;


    /* Allocate the data array */

    uni_str->data = jvm_calloc(length * sizeof(utf8_char));

    if (uni_str->data == NULL) {
        jvm_free(uni_str);
        return NULL;
    }
    return uni_str;
}

void utf8_destroy(Utf8String *uni_str) {
    if (uni_str != NULL) {
        jvm_free(uni_str->data);
        jvm_free(uni_str);
    }
}


Utf8String *utf8_create_c(char const *str) {
    Utf8String *uni_str = utf8_create();
    utf8_append_c(uni_str, str);
    return uni_str;
}

Utf8String *utf8_create_copy(Utf8String *str) {
    Utf8String *uni_str = utf8_create();
    utf8_append_part(uni_str, str, 0, str->length);
    return uni_str;
}

Utf8String *utf8_create_part(Utf8String *str, int start, int len) {
    Utf8String *uni_str = utf8_create();
    utf8_append_part(uni_str, str, start, len);
    return uni_str;
}

Utf8String *utf8_create_part_c(char const *str, int start, int len) {
    Utf8String *uni_str = utf8_create();
    int i;
    for (i = 0; i < len; i++) {
        utf8_pushback(uni_str, str[start + i]);
    }
    return uni_str;
}


void utf8_clear(Utf8String *ustr) {
    /* To clear the list, simply set the arr_length to zero */

    ustr->length = 0;
    ustr->hash = 0;
}

void utf8_append(Utf8String *a1, Utf8String *a2) {
    if (a2 == NULL)return;
    utf8_append_part(a1, a2, 0, a2->length);
}

void utf8_append_part(Utf8String *a1, Utf8String *a2, int start, int len) {
    if (a1 == NULL || a2 == NULL)return;
    _utf8_space_require(a1, len);
    memcpy(a1->data + a1->length, a2->data + start * sizeof(utf8_char), len * sizeof(utf8_char));
    a1->length += len;
}

void utf8_append_c(Utf8String *a1, char const *a2) {
    int i = 0;
    for (i = 0;; i++) {
        char ch = a2[i];
        if (ch == 0)break;
        utf8_pushback(a1, ch);
    }
}

void utf8_append_data(Utf8String *a1, char const *a2, s32 size) {
    int i = 0;
    for (i = 0; i < size; i++) {
        char ch = a2[i];
        utf8_pushback(a1, ch);
    }
}

void utf8_append_s64(Utf8String *a1, s64 val, int radix) {
    if (a1) {
        int pos = a1->length;
        if (val == 0) {
            utf8_insert(a1, pos, '0');
        } else {
            s64 tv = val;
            if (val < 0) {
                utf8_insert(a1, pos, '-');
                pos++;
                tv = -val;
            }
            while (tv) {
                s64 m = tv % radix;
                if (m < 10)utf8_insert(a1, pos, (utf8_char) ('0' + m));
                else utf8_insert(a1, pos, (utf8_char) ('A' + (m - 10)));
                tv = tv / radix;
            }
        }
    }

}

void utf8_upcase(Utf8String *a1) {
    int i;
    for (i = 0; i < a1->length; i++) {
        if (a1->data[i] >= 'a' && a1->data[i] <= 'z') {
            a1->data[i] = a1->data[i] - ('a' - 'A');
        }
    }
    a1->hash = 0;
}

void utf8_lowercase(Utf8String *a1) {
    int i;
    for (i = 0; i < a1->length; i++) {
        if (a1->data[i] >= 'A' && a1->data[i] <= 'A') {
            a1->data[i] = a1->data[i] + ('a' - 'A');
        }
    }
    a1->hash = 0;
}

s64 utf8_aton(Utf8String *sp, int n) {
    s64 v = 0;
    c8 negative = 0;
    utf8_upcase(sp);
    int i;
    for (i = 0; i < sp->length; i++) {
        utf8_char ch = sp->data[i];
        if (ch == '-')
            negative = 1;
        else if (ch >= '0' && ch <= '9') {
            v *= n;
            v += ch - '0';
        } else if (ch >= 'A' && ch <= 'Z') {
            v *= n;
            v += 10 + (ch - 'A');
        }
    }
    if (negative)
        v = -v;
    return v;
}

void utf8_append_part_c(Utf8String *a1, unsigned char const *a2, int start, int len) {
    int i = 0;
    for (i = start; i < len; i++) {
        char ch = a2[i];
        if (ch == 0)break;
        utf8_pushback(a1, ch);
    }
}


void utf8_substring(Utf8String *a1, int start, int end) {
    //printf("utf8_substring start=%d,end=%d\n", start, end);
    utf8_remove_range(a1, end, a1->length - end);
    utf8_remove_range(a1, 0, start);
}

char const *utf8_cstr(Utf8String *a1) {
    _utf8_space_require(a1, 1);
    a1->data[a1->length] = 0;
    return (char *) &(a1->data[0]);
}

//----------------------------    indexof     -----------------------------

static inline int _utf8_indexof_pos_impl(Utf8String *a1, char const *a2, int count2, int a1_pos) {

    int i = 0;
    for (i = a1_pos; i < a1->length; i++) {
        utf8_char ch = a1->data[i];
        if (ch == a2[0]) {
            int match = 1; //标识
            int j = 0;
            for (j = 1; j < count2; j++) {
                if (i + j >= a1->length)return -1; //超界

                if (a2[j] != a1->data[i + j]) {//不匹配
                    match = 0;
                    break;
                }
            }
            if (match)return i;
        }
    }
    return -1;
}


int utf8_indexof_pos(Utf8String *a1, Utf8String *a2, int a1_pos) {
    if (a1 == NULL || a2 == NULL)return -1; //无法查找
    if (a1_pos >= a1->length || a1_pos < 0)return -1;
    if (a2->length == 0)return 0; //
    return _utf8_indexof_pos_impl(a1, (char *) a2->data, a2->length, a1_pos);
}

int utf8_indexof(Utf8String *a1, Utf8String *a2) {
    return utf8_indexof_pos(a1, a2, 0);
}

int utf8_indexof_pos_c(Utf8String *a1, char const *a2, int a1_pos) {
    if (a1 == NULL || a2 == NULL)return -1; //无法查找
    if (a1_pos >= a1->length || a1_pos < 0)return -1;
    int count2 = strlen(a2);
    if (count2 == 0)return 0;
    return _utf8_indexof_pos_impl(a1, a2, count2, a1_pos);
}

int utf8_indexof_c(Utf8String *a1, char const *a2) {
    return utf8_indexof_pos_c(a1, a2, 0);
}


static inline int _utf8_last_indexof_pos_impl(Utf8String *a1, char const *a2, int count2, int a1_rightpos) {

    int i = 0;
    for (i = a1_rightpos; i > -1; i--) {
        utf8_char ch = a1->data[i];
        if (ch == a2[0]) {
            int match = 1; //标识
            int j = 1;
            for (j = 1; j < count2; j++) {
                if (i + j >= a1->length)return -1; //超界

                if (a2[j] != a1->data[i + j]) {//不匹配
                    match = 0;
                    break;
                }
            }
            if (match)return i;
        }
    }
    return -1;
}


int utf8_last_indexof_pos(Utf8String *a1, Utf8String *a2, int a1_rightpos) {
    if (a1 == NULL || a2 == NULL)return -1; //无法查找
    if (a1_rightpos >= a1->length || a1_rightpos < 0)return -1;
    if (a2->length == 0)return a1->length; //

    return _utf8_last_indexof_pos_impl(a1, (char *) a2->data, a2->length, a1_rightpos);
}

int utf8_last_indexof(Utf8String *a1, Utf8String *a2) {
    if (a1 == NULL || a2 == NULL)return -1; //无法查找
    if (a2->length == 0)return a1->length; //
    return utf8_last_indexof_pos(a1, a2, a1->length - 1);
}

int utf8_last_indexof_pos_c(Utf8String *a1, char const *a2, int a1_rightpos) {
    if (a1 == NULL || a2 == NULL)return -1; //无法查找
    if (a1_rightpos >= a1->length || a1_rightpos < 0)return -1;
    int count2 = strlen(a2);
    if (count2 == 0)return a1->length; //
    int index = _utf8_last_indexof_pos_impl(a1, a2, count2, a1_rightpos);
    return index;
}

int utf8_last_indexof_c(Utf8String *a1, char const *a2) {
    if (a1 == NULL || a2 == NULL)return -1; //无法查找
    int count2 = strlen(a2);
    if (count2 == 0)return a1->length;
    return utf8_last_indexof_pos_c(a1, a2, a1->length - 1);
}


void utf8_split_get_part(Utf8String *a1, char const *splitor, int index, Utf8String *result) {
    int i = 0, count = 0, prePos = 0;
    int count2 = strlen(splitor);
    for (i = 0; i < a1->length; i++) {
        utf8_char ch = a1->data[i];
        if (ch == splitor[0]) {
            int match = 1; //标识
            int j = 0;
            for (j = 1; j < count2; j++) {
                if (i + j >= a1->length)return; //超界

                if (splitor[j] != a1->data[i + j]) {//不匹配
                    match = 0;
                    break;
                }
            }
            if (count == index) {
                break;
            }
            if (match) {
                count++;
                prePos = i + j;
            }
        }
    }
    if (count == index) {
        utf8_append_part(result, a1, prePos, i - prePos);
    }
    return;
}


static inline void _utf8_replace_impl(Utf8String *a1, char const *a2, int count2, char const *a3, int count3) {
    Utf8String *tmps = utf8_create();
    utf8_append(tmps, a1);
    utf8_clear(a1);
    int end = 0, start = 0, len = 0;
    for (;;) {
        end = utf8_indexof_pos_c(tmps, a2, start);
        //printf("find indexof :%d\n", end);
        if (end == -1) {
            utf8_append_part(a1, tmps, start, tmps->length - start);
            break;
        }
        len = end - start;
        utf8_append_part(a1, tmps, start, len);
        utf8_append_data(a1, a3, count3);
        start = end + count2;
    }
    utf8_destroy(tmps);
}

void utf8_replace(Utf8String *a1, Utf8String *a2, Utf8String *a3) {
    if (a1 == NULL || a2 == NULL || a3 == NULL)return;
    if (a1->length == 0 || a2->length == 0)return;
    utf8_cstr(a2);
    utf8_cstr(a3);
    _utf8_replace_impl(a1, (char *) a2->data, a2->length, (char *) a3->data, a3->length);
}

void utf8_replace_c(Utf8String *a1, char const *a2, char const *a3) {
    if (a1 == NULL || a2 == NULL || a3 == NULL)return;
    int count2 = strlen(a2);
    int count3 = strlen(a3);
    if (a1->length == 0 || count2 == 0)return;
    _utf8_replace_impl(a1, a2, count2, a3, count3);
}

static inline int _utf8_equals_impl(utf8_char *a1, int count1, utf8_char *a2, int count2) {
    if (a1 == NULL && a2 == NULL)return 1;
    if (a1 == NULL || a2 == NULL)return 0;
    if (count1 != count2)return 0;

    int i = 0;
    for (; i < count1; i++) {
        if (a1[i] != a2[i]) {
            return 0;
        }
    }
    return 1;
}

int utf8_equals(Utf8String *a1, Utf8String *a2) {
    if (a1 && a2) {
        if (_utf8_hashCode(a1) != _utf8_hashCode(a2))return 0;
        return _utf8_equals_impl(a1->data, a1->length, a2->data, a2->length);
    }
    return 0;
}

int utf8_equals_c(Utf8String *a1, char const *a2) {
    if (a1 && a2) {
        return _utf8_equals_impl(a1->data, a1->length, (utf8_char *) a2, strlen(a2));
    }
    return 0;
}

//为 hashtable / hashset 准备的 hash函数

int UNICODE_STR_EQUALS_FUNC(HashtableValue value1, HashtableValue value2) {
    return utf8_equals(value1, value2) == 1;
}

s64 UNICODE_STR_HASH_FUNC(HashtableKey kmer) {
    s64 v = _utf8_hashCode(kmer);
    return v < 0 ? -v : v;
}


//****************************************************************************
unsigned long _utf8_hashCode(Utf8String *ustr) {
    if (!ustr->hash) {//如果未有赋值，则需计算
        int i;
        for (i = 0; i < ustr->length; i++) {
            ustr->hash = 31 * ustr->hash + ustr->data[i];
        }
    }
    return ustr->hash;
}

int _utf8_space_require(Utf8String *ustr, int need) {
    if ((!ustr) || ustr->length + need <= ustr->_alloced) {
        return 0;
    }

    utf8_char *data;
    int newsize;

    /* Double the allocated size */

    newsize = ustr->_alloced << 1;
    while (newsize < need + ustr->length) {
        newsize <<= 1;
    }

    /* Reallocate the array to the new size */

    data = jvm_malloc(sizeof(utf8_char) * newsize);
    if (data == NULL) {
        return 0;
    }

    /* Copy existing data to new memory */
    memmove(data, ustr->data, sizeof(utf8_char) * ustr->length);

    /* Free old memory */
    jvm_free(ustr->data);

    ustr->data = data;
    ustr->_alloced = newsize;

    return 1;
}

int utf8_expand(Utf8String *ustr, int newlen) {
    if (newlen <= ustr->length)return 0;
    if (newlen > ustr->_alloced) {
        if (!_utf8_space_require(ustr, newlen - ustr->length)) {
            return 0;
        }
    }
    ustr->length = newlen;
    ustr->hash = 0;
    return 1;
}

int utf8_insert(Utf8String *ustr, int index, utf8_char data) {
    /* Sanity check the index */

    if (index < 0 || index > ustr->length) {
        return 0;
    }

    /* Increase the size if necessary */

    if (ustr->length + 1 > ustr->_alloced) {
        if (!_utf8_space_require(ustr, 1)) {
            return 0;
        }
    }

    /* Move the contents of the array forward from the index
     * onwards */

    memmove(&ustr->data[index + 1],
            &ustr->data[index],
            (ustr->length - index) * sizeof(utf8_char));

    /* Insert the new entry at the index */

    ustr->data[index] = data;
    ++ustr->length;
    ustr->hash = 0;
    return 1;
}

int utf8_pushback(Utf8String *ustr, utf8_char data) {
    return utf8_insert(ustr, ustr->length, data);
}

int utf8_pushfront(Utf8String *ustr, utf8_char data) {
    return utf8_insert(ustr, 0, data);
}

void utf8_remove_range(Utf8String *ustr, int index, int length) {
    /* Check this is a valid range */

    if (index < 0 || length < 0 || index + length > ustr->length) {
        return;
    }

    /* Move back the entries following the range to be removed */

    memmove(&ustr->data[index],
            &ustr->data[index + length],
            (ustr->length - (index + length)) * sizeof(utf8_char));

    /* Decrease the counter */

    ustr->length -= length;
    ustr->hash = 0;
}

void utf8_remove(Utf8String *ustr, int index) {
    utf8_remove_range(ustr, index, 1);
}

int utf8_index_of(Utf8String *ustr, utf8_char data) {
    int i;

    for (i = 0; i < ustr->length; ++i) {
        if (ustr->data[i] == data)
            return i;
    }

    return -1;
}


/**
 * =============================== utf8 ==============================
 */
/**
 * 把utf字符串转为 java unicode 双字节串
 * @param ustr in
 * @param arr out
 */


s32 enc_get_utf8_size(const c8 *pInput) {
    u8 c = *((u8 *) pInput);
    //printf("---c=%c---\n", c);
    if (c < 0x80) return 1;                // 0xxxxxxx 返回0
    if (c >= 0x80 && c < 0xC0) return -1;     // 10xxxxxx 返回-1
    if (c >= 0xC0 && c < 0xE0) return 2;      // 110xxxxx 返回2
    if (c >= 0xE0 && c < 0xF0) return 3;      // 1110xxxx 返回3
    if (c >= 0xF0 && c < 0xF8) return 4;      // 11110xxx 返回4
    if (c >= 0xF8 && c < 0xFC) return 5;      // 111110xx 返回5
    if (c >= 0xFC) return 6;                // 1111110x 返回6
    return 0;
}

//https://yuncode.net/code/c_5715d18940eb269

/**
 *
 * @param ustr
 * @param jchar_arr
 * @return
 */
s32 utf8_2_unicode(Utf8String *ustr, u16 *jchar_arr, s32 jchar_arr_u16_len) {
    if (ustr == NULL)return 0;
    if (jchar_arr == NULL)jchar_arr_u16_len = 0;
    c8 const *pInput = utf8_cstr(ustr);
    //assert(pInput != NULL && Unic != NULL);
    s32 outputSize = 0; //记录转换后的Unicode字符串的字节数
    // b1 表示UTF-8编码的pInput中的高字节, b2 表示次高字节, ...
    c8 b1, b2, b3, b4, b5, b6;
    s32 codepoint = 0;
    c8 *pOutput = (c8 *) &codepoint;

    while (*pInput) {
        //*Unic = 0x0; // 把 *Unic 初始化为全零
        s32 utfbytes = enc_get_utf8_size(pInput);
        if (utfbytes < 1) return -1; // 无效的 UTF-8 序列
        //printf("%d", utfbytes);
        codepoint = 0;
        switch (utfbytes) {
            case 1:
                *pOutput = *pInput;
                *(pOutput + 1) = 0;
                if (outputSize < jchar_arr_u16_len) {
                    *jchar_arr = (u16) codepoint;
                    jchar_arr++;
                }
                break;

            case 2:
                b1 = *pInput;
                b2 = *(pInput + 1);
                if ((b2 & 0xc0) != 0x80)
                    return -1;
                *pOutput = (b1 << 6) + (b2 & 0x3F);
                *(pOutput + 1) = (b1 >> 2) & 0x07;
                if (outputSize < jchar_arr_u16_len) {
                    *jchar_arr = (u16) codepoint;
                    jchar_arr++;
                }
                break;

            case 3:
                b1 = *pInput;
                b2 = *(pInput + 1);
                b3 = *(pInput + 2);
                if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80))
                    return -1;
                *pOutput = (b2 << 6) + (b3 & 0x3F);
                *(pOutput + 1) = (b1 << 4) + ((b2 >> 2) & 0x0F);

                if (outputSize < jchar_arr_u16_len) {
                    *jchar_arr = (u16) codepoint;
                    jchar_arr++;
                }
                break;

            case 4:
                b1 = *pInput;
                b2 = *(pInput + 1);
                b3 = *(pInput + 2);
                b4 = *(pInput + 3);
                if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80)
                    || ((b4 & 0xC0) != 0x80))
                    return -1;
                *pOutput = (b3 << 6) + (b4 & 0x3F);
                *(pOutput + 1) = (b2 << 4) + ((b3 >> 2) & 0x0F);
                *(pOutput + 2) = ((b1 << 2) & 0x1C) + ((b2 >> 4) & 0x03);
                break;

            case 5:
                b1 = *pInput;
                b2 = *(pInput + 1);
                b3 = *(pInput + 2);
                b4 = *(pInput + 3);
                b5 = *(pInput + 4);
                if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80)
                    || ((b4 & 0xC0) != 0x80) || ((b5 & 0xC0) != 0x80))
                    return -1;
                *pOutput = (b4 << 6) + (b5 & 0x3F);
                *(pOutput + 1) = (b3 << 4) + ((b4 >> 2) & 0x0F);
                *(pOutput + 2) = (b2 << 2) + ((b3 >> 4) & 0x03);
                *(pOutput + 3) = (b1 << 6);
                break;

            case 6:
                b1 = *pInput;
                b2 = *(pInput + 1);
                b3 = *(pInput + 2);
                b4 = *(pInput + 3);
                b5 = *(pInput + 4);
                b6 = *(pInput + 5);
                if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80)
                    || ((b4 & 0xC0) != 0x80) || ((b5 & 0xC0) != 0x80)
                    || ((b6 & 0xC0) != 0x80))
                    return -1;
                *pOutput = (b5 << 6) + (b6 & 0x3F);
                *(pOutput + 1) = (b5 << 4) + ((b6 >> 2) & 0x0F);
                *(pOutput + 2) = (b3 << 2) + ((b4 >> 4) & 0x03);
                *(pOutput + 3) = ((b1 << 6) & 0x40) + (b2 & 0x3F);
                break;

            default:
                return -1;
                break;
        }
        if (utfbytes >= 4) {
            if (outputSize < jchar_arr_u16_len) {
                codepoint -= 0x10000;
                u16 c1 = codepoint >> 10;
                *jchar_arr = (u16) (0xD800 | (c1 & 0x3ff));
                jchar_arr++;
                *jchar_arr = (u16) (0xDC00 | (codepoint & 0x3ff));
                jchar_arr++;
            }
            outputSize += 2;
        } else {
            outputSize++;
        }

        pInput += utfbytes;
    }
    return outputSize;
}


s32 unicode_2_utf8(u16 *jchar_arr, Utf8String *ustr, s32 jchar_arr_u16_len) {
    if (!jchar_arr || !ustr || jchar_arr_u16_len < 0) return 0;

    s32 i;
    for (i = 0; i < jchar_arr_u16_len; i++) {
        s32 unic = jchar_arr[i];
        if (unic >= 0xd800 && unic <= 0xdbff) {
            if (i + 1 < jchar_arr_u16_len) {
                s32 c1 = jchar_arr[i + 1];
                if (c1 >= 0xdc00 && c1 <= 0xdfff) {
                    i++;
                    s32 lead = unic & 0x3ff;
                    s32 trail = c1 & 0x3ff;
                    unic = (lead << 10) | trail | 0x10000;
                }
            }
        }

        if (unic <= 0x0000007F) {
            // * U-00000000 - U-0000007F:  0xxxxxxx
            utf8_pushback(ustr, unic & 0x7F);

        } else if (unic >= 0x00000080 && unic <= 0x000007FF) {
            // * U-00000080 - U-000007FF:  110xxxxx 10xxxxxx
            utf8_pushback(ustr, ((unic >> 6) & 0x1F) | 0xC0);
            utf8_pushback(ustr, (unic & 0x3F) | 0x80);

        } else if (unic >= 0x00000800 && unic <= 0x0000FFFF) {
            // * U-00000800 - U-0000FFFF:  1110xxxx 10xxxxxx 10xxxxxx
            utf8_pushback(ustr, ((unic >> 12) & 0x0F) | 0xE0);
            utf8_pushback(ustr, ((unic >> 6) & 0x3F) | 0x80);
            utf8_pushback(ustr, (unic & 0x3F) | 0x80);

        } else if (unic >= 0x00010000 && unic <= 0x001FFFFF) {
            // * U-00010000 - U-001FFFFF:  11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
            utf8_pushback(ustr, ((unic >> 18) & 0x07) | 0xF0);
            utf8_pushback(ustr, ((unic >> 12) & 0x3F) | 0x80);
            utf8_pushback(ustr, ((unic >> 6) & 0x3F) | 0x80);
            utf8_pushback(ustr, (unic & 0x3F) | 0x80);

        } else if (unic >= 0x00200000 && unic <= 0x03FFFFFF) {
            // * U-00200000 - U-03FFFFFF:  111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
            utf8_pushback(ustr, ((unic >> 24) & 0x03) | 0xF8);
            utf8_pushback(ustr, ((unic >> 18) & 0x3F) | 0x80);
            utf8_pushback(ustr, ((unic >> 12) & 0x3F) | 0x80);
            utf8_pushback(ustr, ((unic >> 6) & 0x3F) | 0x80);
            utf8_pushback(ustr, (unic & 0x3F) | 0x80);

        } else if (unic >= 0x04000000 && unic <= 0x7FFFFFFF) {
            // * U-04000000 - U-7FFFFFFF:  1111110x 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
            utf8_pushback(ustr, ((unic >> 30) & 0x01) | 0xFC);
            utf8_pushback(ustr, ((unic >> 24) & 0x3F) | 0x80);
            utf8_pushback(ustr, ((unic >> 18) & 0x3F) | 0x80);
            utf8_pushback(ustr, ((unic >> 12) & 0x3F) | 0x80);
            utf8_pushback(ustr, ((unic >> 6) & 0x3F) | 0x80);
            utf8_pushback(ustr, (unic & 0x3F) | 0x80);

        }
    }
    return ustr->length;
}


