//
// Created by Gust on 2020/5/9.
//
#include <stdio.h>
#include <string.h>

#include <stdarg.h>
#include "jvm.h"
#include "bytebuf.h"
#include "miniz_wrapper.h"


#define NANO_2_SEC_SCALE 1000000000
#define NANO_2_MILLS_SCALE 1000000
#define MILL_2_SEC_SCALE 1000
static s64 NANO_START = 0;
FILE *logfile = NULL;
s64 last_flush = 0;

void open_log() {
#if _JVM_DEBUG_PRINT_FILE
    if (!logfile) {
        logfile = fopen("./jvmlog.txt", "wb+");
    }
#endif
}

void close_log() {
#if _JVM_DEBUG_PRINT_FILE
    if (!logfile) {
        fclose(logfile);
        logfile = NULL;
        last_flush = 0;
    }
#endif
}

int jvm_printf(const char *format, ...) {
    va_list vp;
    va_start(vp, format);
    int result = 0;
#if _JVM_DEBUG_PRINT_FILE
    if (logfile) {

        result = vfprintf(logfile, format, vp);
        if (currentTimeMillis() - last_flush > 1000) {
            fflush(logfile);
            last_flush = currentTimeMillis();
        }
    }
#else
    result = vfprintf(stderr, format, vp);
#endif
    va_end(vp);
    return result;
}


s64 currentTimeMillis() {

    struct timespec tv;
    clock_gettime(CLOCK_REALTIME, &tv);
    return ((s64) tv.tv_sec) * MILL_2_SEC_SCALE + tv.tv_nsec / NANO_2_MILLS_SCALE;
}

s64 nanoTime() {

    struct timespec tv;
    clock_gettime(CLOCK_REALTIME, &tv);

    if (!NANO_START) {
        NANO_START = ((s64) tv.tv_sec) * NANO_2_SEC_SCALE + tv.tv_nsec;
    }
    s64 v = ((s64) tv.tv_sec) * NANO_2_SEC_SCALE + tv.tv_nsec;
    return v - NANO_START;
}

s64 threadSleep(s64 ms) {
    //wait time
    struct timespec req;
    clock_gettime(CLOCK_REALTIME, &req);
    req.tv_sec += ms / MILL_2_SEC_SCALE;
    req.tv_nsec += (ms % MILL_2_SEC_SCALE) * NANO_2_MILLS_SCALE;
    //if notify or notifyall ,the thread is active again, rem record remain wait time
    struct timespec rem;
    rem.tv_sec = 0;
    rem.tv_nsec = 0;
    thrd_sleep(&req, &rem);
    return (rem.tv_sec * MILL_2_SEC_SCALE + rem.tv_nsec / NANO_2_MILLS_SCALE);
}


void thread_lock_init(ThreadLock *lock) {
    if (lock) {
        cnd_init(&lock->thread_cond);
//        pthread_mutexattr_init(&lock->lock_attr);
//        pthread_mutexattr_settype(&lock->lock_attr, PTHREAD_MUTEX_RECURSIVE);
        mtx_init(&lock->mutex_lock, mtx_recursive);
    }
}

void thread_lock_dispose(ThreadLock *lock) {
    if (lock) {
        cnd_destroy(&lock->thread_cond);
//        pthread_mutexattr_destroy(&lock->lock_attr);
        mtx_destroy(&lock->mutex_lock);
    }
}


s32 find_global_string_index(c8 const *str) {
    s32 len = strlen(str);
    s32 i, j;
    for (i = 0; i < g_strings_count; i++) {
        UtfRaw *utfraw = &g_strings[i];
        if (len == utfraw->utf8_size) {
            s32 found = 1;
            for (j = 0; j < len; j++) {
                if (str[j] != utfraw->str[j]) {
                    found = 0;
                    break;
                }
            }
            if (!found) {
                continue;
            }
            //printf("found :%d = %s\n", i, ustr->str);
            return i;
        }
    }
    return -1;
}

Utf8String *get_utf8str(UtfRaw *utfraw) {
    if (!utfraw)return NULL;
    if (!utfraw->ustr) {
        utfraw->ustr = utf8_create_c(utfraw->str);
    }
    return utfraw->ustr;
}

Utf8String *get_utf8str_by_utfraw_index(s32 index) {
    if (index > g_strings_count) {
        jvm_printf("utfraw string index outof bounds :%d / %d\n", index, g_strings_count);
    }
    UtfRaw *utfraw = &g_strings[index];
    return get_utf8str(utfraw);
}

ClassRaw *find_classraw(c8 const *className) {

    s32 classNameIndex = find_global_string_index(className);
    if (classNameIndex < 0) {
        return NULL;
    }
    s32 i;
    for (i = 0; i < g_classes_count; i++) {
        ClassRaw *raw = &g_classes[i];
        if (classNameIndex == raw->name) {
            return raw;
        }
    }
    return NULL;
}

MethodRaw *get_methodraw_by_index(s32 index) {
    return &g_methods[index];
}


MethodRaw *find_methodraw(c8 const *className, c8 const *methodName, c8 const *signature) {

    s32 classNameIndex = find_global_string_index(className);
    if (classNameIndex < 0) {
        return NULL;
    }
    s32 methodNameIndex = find_global_string_index(methodName);
    if (methodNameIndex < 0) {
        return NULL;
    }
    s32 signatureIndex = find_global_string_index(signature);
    if (signatureIndex < 0) {
        return NULL;
    }
    s32 i;
    for (i = 0; i < g_methods_count; i++) {
        MethodRaw *raw = &g_methods[i];
        if (classNameIndex == raw->class_name && methodNameIndex == raw->name && signatureIndex == raw->desc_name) {
            return raw;
        }
    }
    return NULL;
}

//MethodInfo *get_stackframe_methodinfo(StackFrame *frame) {
//    if (frame) {
//        MethodRaw *raw = get_methodraw_by_index(frame->methodRawIndex);
//        JClass *clazz = get_class_by_nameIndex(raw->class_name);
//        get_
//    }
//    return NULL;
//}

MethodInfo *get_methodinfo_by_rawindex(s32 methodRawIndex) {
    MethodRaw *raw = get_methodraw_by_index(methodRawIndex);
    JClass *clazz = get_class_by_nameIndex(raw->class_name);
    s32 i, imax;
    for (i = 0, imax = clazz->methods->length; i < imax; i++) {
        MethodInfo *m = arraylist_get_value(clazz->methods, i);
        if (utf8_equals_c(m->name, g_strings[raw->name].str) && utf8_equals_c(m->desc, g_strings[raw->desc_name].str)) {
            return m;
        }
    }
    return NULL;
}

MethodInfo *find_methodInfo_by_name(c8 const *clsName, c8 const *methodName, c8 const *methodType) {
    MethodInfo *mi = NULL;
    JClass *other = classes_get_c(clsName);

    while (mi == NULL && other) {
        s32 i, imax;
        for (i = 0, imax = other->methods->length; i < imax; i++) {
            MethodInfo *tmp = arraylist_get_value(other->methods, i);
            if (utf8_equals_c(tmp->name, methodName) == 1
                && utf8_equals_c(tmp->desc, methodType) == 1) {
                mi = tmp;
                break;
            }
        }
        //find interface default method implementation JDK8
        if (mi == NULL) {
            for (i = 0, imax = other->interfaces->length; i < imax; i++) {
                JClass *icl = arraylist_get_value(other->interfaces, i);
//                if (utf8_equals_c(icl_name, "java/util/List")&&utf8_equals_c(methodName, "size")) {
//                    int debug = 1;
//                }
                MethodInfo *imi = find_methodInfo_by_name(utf8_cstr(icl->name), methodName, methodType);
                if (imi != NULL) {
                    mi = imi;
                    break;
                }
            }
        }
        //find superclass
        other = getSuperClass(other);
    }

    return mi;
}


JClass *get_class_by_name_c(c8 const *name) {
    return classes_get_c(name);
}

JClass *get_class_by_name(Utf8String *name) {
    return classes_get(name);
}

JClass *get_class_by_nameIndex(s32 index) {
    Utf8String *ustr = get_utf8str(&g_strings[index]);
    return classes_get(ustr);
}

void classes_put(JClass *clazz) {
    hashtable_put(g_jvm->classes, clazz->name, clazz);
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
 * @param arr
 * @return
 */
s32 utf8_2_unicode(Utf8String *ustr, u16 *arr) {
    c8 const *pInput = utf8_cstr(ustr);
    //assert(pInput != NULL && Unic != NULL);
    int outputSize = 0; //记录转换后的Unicode字符串的字节数
    // b1 表示UTF-8编码的pInput中的高字节, b2 表示次高字节, ...
    c8 b1, b2, b3, b4, b5, b6;
    s32 codepoint = 0;
    c8 *pOutput = (c8 *) &codepoint;

    while (*pInput) {
        //*Unic = 0x0; // 把 *Unic 初始化为全零
        int utfbytes = enc_get_utf8_size(pInput);
        //printf("%d", utfbytes);
        codepoint = 0;
        switch (utfbytes) {
            case 1:
                *pOutput = *pInput;
                *(pOutput + 1) = 0;
                *arr = (u16) codepoint;
                arr++;
                break;

            case 2:
                b1 = *pInput;
                b2 = *(pInput + 1);
                if ((b2 & 0xc0) != 0x80)
                    return -1;
                *pOutput = (b1 << 6) + (b2 & 0x3F);
                *(pOutput + 1) = (b1 >> 2) & 0x07;
                *arr = (u16) codepoint;
                arr++;
                break;

            case 3:
                b1 = *pInput;
                b2 = *(pInput + 1);
                b3 = *(pInput + 2);
                if (((b2 & 0xC0) != 0x80) || ((b3 & 0xC0) != 0x80))
                    return -1;
                *pOutput = (b2 << 6) + (b3 & 0x3F);
                *(pOutput + 1) = (b1 << 4) + ((b2 >> 2) & 0x0F);
                *arr = (u16) codepoint;
                arr++;
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
            codepoint -= 0x10000;
            u16 c1 = codepoint >> 10;
            *arr = (u16) (0xD800 | (c1 & 0x3ff));
            arr++;
            *arr = (u16) (0xDC00 | (codepoint & 0x3ff));
            arr++;
            outputSize += 2;
        } else {
            outputSize++;
        }

        pInput += utfbytes;
    }
    return outputSize;
}


s32 unicode_2_utf8(u16 *jchar_arr, Utf8String *ustr, s32 totalSize) {
    s32 i;
    for (i = 0; i < totalSize; i++) {
        s32 unic = jchar_arr[i];
        if (unic >= 0xd800 && unic <= 0xdbff) {
            if (i + 1 < totalSize) {
                int c1 = jchar_arr[i + 1];
                if (c1 >= 0xdc00 && c1 <= 0xdfff) {
                    i++;
                    int lead = unic & 0x3ff;
                    int trail = c1 & 0x3ff;
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

JClass *getSuperClass(JClass *clazz) {
    return clazz->superclass;
}


s32 isSonOfInterface(JClass *parent, JClass *son) {
    s32 i;
    for (i = 0; i < son->interfaces->length; i++) {
        JClass *ccr = arraylist_get_value(son->interfaces, i);
        if (parent == ccr) {
            return 1;
        } else {
            u8 sure = isSonOfInterface(parent, ccr);
            if (sure)return 1;
        }
    }
    return 0;
}

s32 instance_of_class_name(InstProp *ins, s32 classNameIndex) {
    JClass *clazz = get_class_by_nameIndex(classNameIndex);
    return instance_of(ins, clazz);
}

s32 instance_of(InstProp *ins, JClass *clazz) {
    if (!ins)return 0;
    JClass *ins_of_class = ins->clazz;
    while (ins_of_class) {
        if (ins_of_class == clazz || isSonOfInterface(clazz, ins_of_class)) {
            return 1;
        }
        ins_of_class = getSuperClass(ins_of_class);
    }

    return 0;
}

s32 assignable_from(JClass *clazzSon, JClass *clazzSuper) {

    while (clazzSuper) {
        if (clazzSon == clazzSuper) {
            return 1;
        }
        clazzSuper = getSuperClass(clazzSuper);
    }
    return 0;
}


s32 checkcast(JObject *jobj, s32 classNameIdx) {
    InstProp *ins = (InstProp *) jobj;
    JClass *cinfo = get_class_by_nameIndex(classNameIdx);
    if (ins != NULL) {
        if (ins->type == INS_TYPE_OBJECT) {
            if (instance_of(ins, cinfo)) {
                return 1;
            }
        } else if (ins->type == INS_TYPE_ARRAY) {
            return jobj->prop.clazz->array_cell_type == cinfo->array_cell_type;//
        } else if (ins->type == INS_TYPE_CLASS) {
            return utf8_equals_c(jobj->prop.clazz->name, STR_JAVA_LANG_CLASS);
        }
    } else {
        return 1;
    }
    return 0;
}


c8 *data_type_str = "    ZCFDBSIJL[";

s32 data_type_bytes[] = {0, 0, 0, 0,
                         sizeof(c8),
                         sizeof(u16),
                         sizeof(f32),
                         sizeof(f64),
                         sizeof(c8),
                         sizeof(s16),
                         sizeof(s32),
                         sizeof(s64),
                         sizeof(__refer),
                         sizeof(__refer),
};

s32 getDataTypeIndex(c8 ch) {
    switch (ch) {
        case 'I':
            return 10;
        case 'L':
            return 12;
        case '[':
            return 13;
        case 'C':
            return 5;
        case 'B':
            return 8;
        case 'Z':
            return 4;
        case 'J':
            return 11;
        case 'F':
            return 6;
        case 'D':
            return 7;
        case 'S':
            return 9;
        default:
            jvm_printf("datatype not found %c\n", ch);
    }
    return 0;
}

c8 *getDataTypeFullName(c8 ch) {
    switch (ch) {
        case 'I':
            return "int";
        case 'C':
            return "char";
        case 'B':
            return "byte";
        case 'Z':
            return "boolean";
        case 'J':
            return "long";
        case 'F':
            return "float";
        case 'D':
            return "double";
        case 'S':
            return "short";
    }
    return NULL;
}

c8 getDataTypeTagByName(Utf8String *name) {
    if (utf8_equals_c(name, "int")) {
        return 'I';
    }
    if (utf8_equals_c(name, "long")) {
        return 'J';
    }
    if (utf8_equals_c(name, "char")) {
        return 'C';
    }
    if (utf8_equals_c(name, "byte")) {
        return 'B';
    }
    if (utf8_equals_c(name, "boolean")) {
        return 'Z';
    }
    if (utf8_equals_c(name, "short")) {
        return 'S';
    }
    if (utf8_equals_c(name, "double")) {
        return 'D';
    }
    if (utf8_equals_c(name, "float")) {
        return 'F';
    }
    return 'V';
}


c8 getDataTypeTag(s32 index) {
    return data_type_str[index];
}

s32 isDataReferByTag(c8 c) {
    if (c == 'L' || c == '[') {
        return 1;
    }
    return 0;
}

s32 isDataReferByIndex(s32 index) {
    if (index == 12 || index == 13) {
        return 1;
    }
    return 0;
}

s32 isData8ByteByTag(c8 c) {
    if (c == 'D' || c == 'J') {
        return 1;
    }
    return 0;
}


s32 parseMethodPara(Utf8String *methodType, Utf8String *out) {
    s32 count = 0;
    Utf8String *para = utf8_create_copy(methodType);
    utf8_substring(para, utf8_indexof_c(para, "(") + 1, utf8_last_indexof_c(para, ")"));
    //从后往前拆分方法参数，从栈中弹出放入本地变量
    int i = 0;
    while (para->length > 0) {
        c8 ch = utf8_char_at(para, 0);
        switch (ch) {
            case 'S':
            case 'C':
            case 'B':
            case 'I':
            case 'F':
            case 'Z':
                utf8_substring(para, 1, para->length);
                utf8_pushback(out, ch);
                count++;
                break;
            case 'D':
            case 'J': {
                utf8_substring(para, 1, para->length);
                utf8_pushback(out, ch);
                count += 2;
                break;
            }
            case 'L':
                utf8_substring(para, utf8_indexof_c(para, ";") + 1, para->length);
                utf8_pushback(out, ch);
                count += 1;
                break;
            case '[':
                while (utf8_char_at(para, 1) == '[') {
                    utf8_substring(para, 1, para->length);//去掉多维中的 [[[[LObject; 中的 [符
                }
                if (utf8_char_at(para, 1) == 'L') {
                    utf8_substring(para, utf8_indexof_c(para, ";") + 1, para->length);
                } else {
                    utf8_substring(para, 2, para->length);
                }
                utf8_pushback(out, ch);
                count += 1;
                break;
        }
        i++;
    }
    utf8_destory(para);
    return count;
}


s32 _loadFileContents(c8 const *file, ByteBuf *buf) {

    FILE *pFile;
    long lSize;
    char *buffer;
    size_t result;

    /* 若要一个byte不漏地读入整个文件，只能采用二进制方式打开 */
    pFile = fopen(file, "rb");
    if (pFile == NULL) {
        //jvm_printf("File error");
        return -1;
    }

    /* 获取文件大小 */
    fseek(pFile, 0, SEEK_END);
    lSize = ftell(pFile);
    rewind(pFile);

    /* 分配内存存储整个文件 */
    buffer = jvm_malloc((u32) lSize);
    if (buffer == NULL) {
        //jvm_printf("Memory error");
        return -1;
    }

    /* 将文件拷贝到buffer中 */
    result = fread(buffer, 1, lSize, pFile);
    if (result != lSize) {
        //jvm_printf("Reading error");
        return -1;
    }
    /* 现在整个文件已经在buffer中，可由标准输出打印内容 */
    //printf("%s", buffer);

    /* 结束演示，关闭文件并释放内存 */
    fclose(pFile);
    bytebuf_write_batch(buf, buffer, (s32) lSize);
    jvm_free(buffer);

    return 0;
}

ByteBuf *load_file_from_classpath(Utf8String *path) {
    ByteBuf *bytebuf = NULL;
    s32 i, iret;
    Utf8String *filepath = utf8_create_copy(path);

    bytebuf = bytebuf_create(16);
    iret = _loadFileContents(utf8_cstr(filepath), bytebuf);
    utf8_destory(filepath);
    //回收
    if (iret != 0) {
        bytebuf_destory(bytebuf);
        bytebuf = NULL;
    }
    return bytebuf;
}
