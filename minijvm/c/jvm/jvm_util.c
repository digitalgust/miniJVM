//
// Created by gust on 2017/8/8.
//


#include <stdarg.h>
#include <sys/stat.h>
#include "jvm.h"

#include "../utils/miniz_wrapper.h"
#include "jvm_util.h"
#include "garbage.h"
#include "jdwp.h"



//==================================================================================

static FILE *logfile = NULL;
static s64 last_flush = 0;
static s64 nano_sec_start_at = 0;

/**
 * =============================== JClass ==============================
 */

JClass *classes_get_c(MiniJVM *jvm, Instance *jloader, c8 *clsName) {
    Utf8String *ustr = utf8_create_c(clsName);
    JClass *clazz = classes_get(jvm, jloader, ustr);
    utf8_destory(ustr);
    return clazz;
}

JClass *classes_get(MiniJVM *jvm, Instance *jloader, Utf8String *clsName) {
    JClass *cl = NULL;
    if (clsName) {
        PeerClassLoader *pcl = jloader ? classLoaders_find_by_instance(jvm, jloader) : jvm->boot_classloader;
        cl = hashtable_get(pcl->classes, clsName);
        if (!cl) {
            if (jloader) {
                cl = classes_get(jvm, pcl->parent, clsName);
            }
        }
    }
    return cl;
}

JClass *classes_load_get_without_resolve(Instance *jloader, Utf8String *ustr, Runtime *runtime) {
    if (!ustr)return NULL;
    MiniJVM *jvm = runtime->jvm;
    JClass *cl;
    spin_lock(&jvm->lock_cloader);//fast lock
    if (utf8_index_of(ustr, '.') >= 0) utf8_replace_c(ustr, ".", "/");
    spin_unlock(&jvm->lock_cloader);
    cl = classes_get(jvm, jloader, ustr);
    if (!cl) {
        vm_share_lock(jvm);//slow lock
        cl = classes_get(jvm, jloader, ustr);
        if (!cl) {
            cl = load_class(jloader, ustr, runtime);
        }
        vm_share_unlock(jvm);

    }
    return cl;
}

JClass *classes_load_get_c(Instance *jloader, c8 *pclassName, Runtime *runtime) {
    Utf8String *ustr = utf8_create_c(pclassName);
    JClass *clazz = classes_load_get(jloader, ustr, runtime);
    utf8_destory(ustr);
    return clazz;
}

JClass *classes_load_get(Instance *jloader, Utf8String *ustr, Runtime *runtime) {
    JClass *cl = classes_load_get_without_resolve(jloader, ustr, runtime);
    if (cl && cl->status < CLASS_STATUS_CLINITED) {
        class_clinit(cl, runtime);
    }
    return cl;
}

s32 classes_put(MiniJVM *jvm, JClass *clazz) {
    if (clazz) {
        //jvm_printf("sys_classloader %d : %s\n", sys_classloader->classes->entries, utf8_cstr(clazz->name));
        PeerClassLoader *pcl = classLoaders_find_by_instance(jvm, clazz->jloader);
        hashtable_put(pcl->classes, clazz->name, clazz);
        return 0;
    }
    return -1;
}

JClass *primitive_class_create_get(Runtime *runtime, Utf8String *ustr) {
    MiniJVM *jvm = runtime->jvm;
    JClass *cl = classes_get(jvm, NULL, ustr);
    if (!cl) {
        vm_share_lock(jvm);
        cl = class_create(runtime);
        cl->name = ustr;
        cl->primitive = 1;
        classes_put(jvm, cl);
        gc_obj_hold(jvm->collector, cl);
        vm_share_unlock(jvm);
    } else {
        utf8_destory(ustr);
    }
    if (!cl->ins_class) {
        cl->ins_class = insOfJavaLangClass_create_get(runtime, cl);
    }
    return cl;
}

JClass *array_class_create_get(Runtime *runtime, Utf8String *desc) {
    if (desc && desc->length && utf8_char_at(desc, 0) == '[') {
        PeerClassLoader *cloader = runtime->jvm->boot_classloader;
        MiniJVM *jvm = cloader->jvm;
        JClass *clazz = hashtable_get(cloader->classes, desc);
        if (!clazz) {
            vm_share_lock(jvm);
            clazz = hashtable_get(cloader->classes, desc);//maybe other thread created
            if (!clazz) {
                clazz = class_create(runtime);
                clazz->mb.arr_type_index = getDataTypeIndex(utf8_char_at(desc, 1));
                clazz->name = utf8_create_copy(desc);
                clazz->superclass = classes_get_c(jvm, NULL, STR_CLASS_JAVA_LANG_OBJECT);
                classes_put(jvm, clazz);
                gc_obj_hold(jvm->collector, clazz);
                //this arrayclass need to set loader with element type
                //JClass *typec=classes_load_get();
                //clazz->jloader=typec;

#if _JVM_DEBUG_LOG_LEVEL > 5
                jvm_printf("load class:  %s \n", utf8_cstr(desc));
#endif

            }
            vm_share_unlock(jvm);
        }
        return clazz;
    }
    return NULL;
}

/**
 * array class get accepted name :
 *
 *    7                     -> [D
 *
 * @param runtime
 * @param name
 * @return
 */
JClass *array_class_get_by_index(Runtime *runtime, s32 typeIdx) {
    JClass *clazz = NULL;
    ShortCut *cache = &runtime->jvm->shortcut;
    if (cache->array_classes[typeIdx] == NULL) {
        Utf8String *ustr = utf8_create_c("[");
        utf8_insert(ustr, ustr->length, getDataTypeTag(typeIdx));
        clazz = cache->array_classes[typeIdx] = array_class_create_get(runtime, ustr);
        utf8_destory(ustr);
    } else {
        clazz = cache->array_classes[typeIdx];
    }
    return clazz;
}


/**
 * array class get accepted name :
 *
 *    [D                    -> [[D
 *    java/lang/String      -> [Ljava/lang/String;
 *    Ljava/lang/Float;     -> [Ljava/lang/Float;
 *
 *
 * @param runtime
 * @param name
 * @return
 */
JClass *array_class_get_by_name(Runtime *runtime, Utf8String *name) {
    JClass *clazz = NULL;
    if (name) {
        Utf8String *ustr = utf8_create_c("[");
        if (!isDataReferByTag(utf8_char_at(name, 0))) {  //not : L [
            if (!isDataReferByTag(utf8_char_at(name, 0))) {
                utf8_append_c(ustr, "L");
            }
            utf8_append(ustr, name);
            if (utf8_char_at(name, name->length - 1) != ';') {
                utf8_append_c(ustr, ";");
            }
        } else {
            utf8_append(ustr, name);
        }
        clazz = array_class_create_get(runtime, ustr);
        utf8_destory(ustr);

    }
    return clazz;
}

/**
 * =============================== threadlist ==============================
 */

Runtime *threadlist_get(MiniJVM *jvm, s32 i) {
    Runtime *r = NULL;
    spin_lock(&jvm->thread_list->spinlock);
    if (i < jvm->thread_list->length) {
        r = (Runtime *) arraylist_get_value_unsafe(jvm->thread_list, i);
    }
    spin_unlock(&jvm->thread_list->spinlock);
    return r;
}

void threadlist_remove(Runtime *runtime) {
    if (runtime)arraylist_remove(runtime->jvm->thread_list, runtime);
}

void threadlist_add(Runtime *runtime) {
    if (runtime)arraylist_push_back(runtime->jvm->thread_list, runtime);
}

s32 threadlist_count_none_daemon(MiniJVM *jvm) {
    spin_lock(&jvm->thread_list->spinlock);
    s32 count = 0;
    s32 i;
    for (i = 0; i < jvm->thread_list->length; i++) {
        Runtime *r = (Runtime *) arraylist_get_value_unsafe(jvm->thread_list, i);
        Instance *ins = r->thrd_info->jthread;
        s32 daemon = jthread_get_daemon_value(ins, r);
        if (!daemon) {
            count++;
        }
    }
    spin_unlock(&jvm->thread_list->spinlock);
    return count;
}

s64 threadlist_sum_heap(MiniJVM *jvm) {
    s64 hsize = 0;
    spin_lock(&jvm->thread_list->spinlock);
    s32 i;
    for (i = 0; i < jvm->thread_list->length; i++) {
        Runtime *r = arraylist_get_value_unsafe(jvm->thread_list, i);
        hsize += r->thrd_info->objs_heap_of_thread;
    }
    spin_unlock(&jvm->thread_list->spinlock);
    return hsize;
}

void thread_stop_all(MiniJVM *jvm) {
    spin_lock(&jvm->thread_list->spinlock);
    s32 i;
    for (i = 0; i < jvm->thread_list->length; i++) {
        Runtime *r = arraylist_get_value_unsafe(jvm->thread_list, i);

        jthread_suspend(r);
        r->thrd_info->no_pause = 1;
        r->thrd_info->is_interrupt = 1;
        MemoryBlock *tl = r->thrd_info->curThreadLock;
        if (tl) {
            jthread_lock(tl, r);
            jthread_notify(tl, r);
            jthread_unlock(tl, r);
        }

    }
    spin_unlock(&jvm->thread_list->spinlock);
}


void thread_lock_init(ThreadLock *lock) {
    if (lock) {
        cnd_init(&lock->thread_cond);
        mtx_init(&lock->mutex_lock, mtx_recursive);
    }
}

void thread_lock_dispose(ThreadLock *lock) {
    if (lock) {
        cnd_destroy(&lock->thread_cond);
        mtx_destroy(&lock->mutex_lock);
    }
}


s32 vm_share_trylock(MiniJVM *jvm) {
    return mtx_trylock(&jvm->threadlock.mutex_lock);
}

void vm_share_lock(MiniJVM *jvm) {
    mtx_lock(&jvm->threadlock.mutex_lock);
}

void vm_share_unlock(MiniJVM *jvm) {
    mtx_unlock(&jvm->threadlock.mutex_lock);
}

void vm_share_wait(MiniJVM *jvm) {
    cnd_wait(&jvm->threadlock.thread_cond, &jvm->threadlock.mutex_lock);
}

void vm_share_timedwait(MiniJVM *jvm, s64 ms) {
    struct timespec t;
    clock_gettime(CLOCK_REALTIME, &t);
    t.tv_sec += ms / 1000;
    t.tv_nsec += (ms % 1000) * 1000000;
    s32 ret = cnd_timedwait(&jvm->threadlock.thread_cond, &jvm->threadlock.mutex_lock, &t);
//    if (ret == ETIMEDOUT) {
//        s32 debug = 1;
//    }
}

void vm_share_notify(MiniJVM *jvm) {
    cnd_signal(&jvm->threadlock.thread_cond);
}

void vm_share_notifyall(MiniJVM *jvm) {
    cnd_broadcast(&jvm->threadlock.thread_cond);
}

/**
 * =============================== utf8 ==============================
 */
/**
 * 把utf字符串转为 java unicode 双字节串
 * @param ustr in
 * @param arr out
 */
s32 utf8_2_unicode(Utf8String *ustr, u16 *arr) {
    char *pInput = utf8_cstr(ustr);

    int outputSize = 0; //记录转换后的Unicode字符串的字节数

    char *tmp = (c8 *) arr; //临时变量，用于遍历输出字符串
    while (*pInput) {
        if (*pInput > 0x00 && *pInput <= 0x7F) //处理单字节UTF8字符（英文字母、数字）
        {
            *tmp = *pInput;
            pInput++;
            tmp++;
            *tmp = 0; //小端法表示，在高地址填补0
            tmp++;
        } else if (((*pInput) & 0xE0) == 0xC0) //处理双字节UTF8字符
        {
            char high = *pInput;
            pInput++;
            char low = *pInput;
            pInput++;

            if ((low & 0xC0) != 0x80)  //检查是否为合法的UTF8字符表示
            {
                return -1; //如果不是则报错
            }

            *tmp = (high << 6) + (low & 0x3F);
            tmp++;
            *tmp = (high >> 2) & 0x07;
            tmp++;
        } else if (((*pInput) & 0xF0) == 0xE0) //处理三字节UTF8字符
        {
            char high = *pInput;
            pInput++;
            char middle = *pInput;
            pInput++;
            char low = *pInput;
            pInput++;
            if (((middle & 0xC0) != 0x80) || ((low & 0xC0) != 0x80)) {
                return -1;
            }

            *tmp = (middle << 6) + (low & 0x7F);
            tmp++;
            *tmp = (high << 4) + ((middle >> 2) & 0x0F);
            tmp++;
        } else if (((*pInput) & 0xF8) == 0xF0) //处理四字节UTF8字符
        {
            Int2Float i2f;
            i2f.c0 = *pInput;
            pInput++;
            i2f.c1 = *pInput;
            pInput++;
            i2f.c2 = *pInput;
            pInput++;
            i2f.c3 = *pInput;
            pInput++;
            if (((i2f.c1 & 0xC0) != 0x80) || ((i2f.c2 & 0xC0) != 0x80) || ((i2f.c3 & 0xC0) != 0x80)) {
                return -1;
            }
            i2f.c0 = (c8) (i2f.c0 << 5) >> 5;
            i2f.c1 = (c8) (i2f.c1 << 2) >> 2;
            i2f.c2 = (c8) (i2f.c2 << 2) >> 2;
            i2f.c3 = (c8) (i2f.c3 << 2) >> 2;
            s32 code = i2f.c3 + (i2f.c2 << 6) + (i2f.c1 << 12) + (i2f.c0 << 18);
            //jchar 两字节表示不下， 得使用双jchar,这里简易处理，替换成了空格
            *tmp = ' ';
            tmp++;
            *tmp = 0;
            tmp++;
        } else //对于其他字节数的UTF8字符不进行处理
        {
            return -1;
        }
        outputSize += 1;
    }
    return outputSize;
}

int unicode_2_utf8(u16 *jchar_arr, Utf8String *ustr, s32 totalSize) {
    s32 i;
    s32 utf_len = 0;
    for (i = 0; i < totalSize; i++) {
        s32 unic = jchar_arr[i];

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
        utf_len++;
    }
    return i;
}

/**
 * 交换高低位，little endian 和 big endian 互转时用到
 * @param ptr addr
 * @param size len
 */
void swap_endian_little_big(u8 *ptr, s32 size) {
    int i;
    for (i = 0; i < size / 2; i++) {
        u8 tmp = ptr[i];
        ptr[i] = ptr[size - 1 - i];
        ptr[size - 1 - i] = tmp;
    }
}

/*
boolean   4
char  5
float  6
double 7
unsigned char 8
short   9
int  10
long  11
 reference 12
 */
s32 getDataTypeIndex(c8 ch) {
    switch (ch) {
        case 'I':
            return 10;
        case 'L':
        case '[':
            return 12;
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

u8 getDataTypeTagByName(Utf8String *name) {
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


u8 getDataTypeTag(s32 index) {
    return DATA_TYPE_STR[index];
}

s32 isDataReferByTag(c8 c) {
    if (c == 'L' || c == '[') {
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

s32 isDataReferByIndex(s32 index) {
    if (index == DATATYPE_REFERENCE || index == DATATYPE_ARRAY) {
        return 1;
    }
    return 0;
}


void sys_properties_set_c(MiniJVM *jvm, c8 *key, c8 *val) {
    Utf8String *ukey = utf8_create_c(key);
    Utf8String *uval = utf8_create_c(val);
#if __JVM_OS_MAC__ || __JVM_OS_LINUX__
    if (utf8_equals_c(ukey, "java.class.path") || utf8_equals_c(ukey, "sun.boot.class.path")) {
        utf8_replace_c(uval, ";", ":");
    }
#elif __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__ || __JVM_OS_VS__
#endif
    hashtable_put(jvm->sys_prop, ukey, uval);
}

s32 sys_properties_load(MiniJVM *jvm) {
    jvm->sys_prop = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);
    hashtable_register_free_functions(jvm->sys_prop,
                                      (HashtableKeyFreeFunc) utf8_destory,
                                      (HashtableValueFreeFunc) utf8_destory);
    Utf8String *ustr = NULL;
    Utf8String *prop_name = utf8_create_c("sys.properties");
    ByteBuf *buf = load_file_from_classpath(jvm->boot_classloader, prop_name);
    if (buf) {
        ustr = utf8_create();
        while (bytebuf_available(buf)) {
            c8 ch = (c8) bytebuf_read(buf);
            utf8_insert(ustr, ustr->length, ch);
        }
        bytebuf_destory(buf);
    }
    utf8_destory(prop_name);
    //parse
    if (ustr) {
        utf8_replace_c(ustr, "\r\n", "\n");
        utf8_replace_c(ustr, "\r", "\n");
        Utf8String *line = utf8_create();
        while (ustr->length > 0) {
            s32 lineEndAt = utf8_indexof_c(ustr, "\n");
            utf8_clear(line);
            if (lineEndAt >= 0) {
                utf8_append_part(line, ustr, 0, lineEndAt);
                utf8_substring(ustr, lineEndAt + 1, ustr->length);
            } else {
                utf8_append_part(line, ustr, 0, ustr->length);
                utf8_substring(ustr, ustr->length, ustr->length);
            }
            s32 eqAt = utf8_indexof_c(line, "=");
            if (eqAt > 0) {
                Utf8String *key = utf8_create();
                Utf8String *val = utf8_create();
                utf8_append_part(key, line, 0, eqAt);
                utf8_append_part(val, line, eqAt + 1, line->length - (eqAt + 1));
                hashtable_put(jvm->sys_prop, key, val);
            }
        }
        utf8_destory(line);
        utf8_destory(ustr);
    }

    //modify os para
#if __JVM_OS_MAC__
    sys_properties_set_c(jvm, "os.name", "Mac");
    sys_properties_set_c(jvm, "path.separator", ":");
    sys_properties_set_c(jvm, "file.separator", "/");
    sys_properties_set_c(jvm, "line.separator", "\n");
    sys_properties_set_c(jvm, "XstartOnFirstThread", "1");
#elif __JVM_OS_LINUX__
    sys_properties_set_c(jvm, "os.name", "Linux");
    sys_properties_set_c(jvm, "path.separator", ":");
    sys_properties_set_c(jvm, "file.separator", "/");
    sys_properties_set_c(jvm, "line.separator", "\n");
#elif __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__ || __JVM_OS_VS__
    sys_properties_set_c(jvm, "os.name", "Windows");
    sys_properties_set_c(jvm, "path.separator", ";");
    sys_properties_set_c(jvm, "file.separator", "\\");
    sys_properties_set_c(jvm, "line.separator", "\r\n");
#endif


    return 0;
}

void sys_properties_dispose(MiniJVM *jvm) {
    hashtable_destory(jvm->sys_prop);
}

void open_log() {
#if _JVM_DEBUG_LOG_TO_FILE
    if (!logfile) {
        logfile = fopen("./jvmlog.txt", "wb+");
    }
#endif
}

void close_log() {
#if _JVM_DEBUG_LOG_TO_FILE
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
#if _JVM_DEBUG_LOG_TO_FILE
    if (logfile) {

        result = vfprintf(logfile, format, vp);
        fflush(logfile);
        if (currentTimeMillis() - last_flush > 1000) {
            fflush(logfile);
            last_flush = currentTimeMillis();
        }
    }
#else
    result = vfprintf(stderr, format, vp);
#endif
    va_end(vp);
    fflush(stderr);
    return result;
}

void invoke_deepth(Runtime *runtime) {
    vm_share_lock(runtime->jvm);
    int i = 0;
    Runtime *r = runtime;
    while (r) {
        i++;
        r = r->parent;
    }
    s32 len = i;

#if _JVM_DEBUG_LOG_TO_FILE
    fprintf(logfile, "%llx", (s64) (intptr_t) thrd_current());
    for (i = 0; i < len; i++) {
        fprintf(logfile, "  ");
    }
#else
    fprintf(stderr, "%llx", (s64) (intptr_t) thrd_current());
    for (i = 0; i < len; i++) {
        fprintf(stderr, "  ");
    }
#endif
    vm_share_unlock(runtime->jvm);
}

//===============================    java 线程  ==================================

s32 jthread_init(Instance *jthread, Runtime *runtime) {
    jthread_set_stackframe_value(runtime->jvm, jthread, runtime);
    runtime->clazz = jthread->mb.clazz;
    runtime->thrd_info->jthread = jthread;
    runtime->thrd_info->thread_status = THREAD_STATUS_RUNNING;
    threadlist_add(runtime);
    return 0;
}

s32 jthread_dispose(Instance *jthread, Runtime *runtime) {
    gc_move_objs_thread_2_gc(runtime);
    threadlist_remove(runtime);
    if (runtime->jvm->jdwp_enable)event_on_thread_death(runtime->jvm->jdwpserver, runtime->thrd_info->jthread);
    //destory
    jthread_set_stackframe_value(runtime->jvm, jthread, NULL);

    return 0;
}

s32 jthread_run(void *para) {
    Runtime *runtime = (Runtime *) para;
    Instance *jthread = runtime->thrd_info->jthread;
    MiniJVM *jvm = runtime->jvm;

#if _JVM_DEBUG_LOG_LEVEL > 0
    s64 startAt = currentTimeMillis();
    jvm_printf("[INFO]thread start %llx\n", (s64) (intptr_t) jthread);
#endif
    s32 ret = 0;
    runtime->thrd_info->pthread = thrd_current();

    Utf8String *methodName = utf8_create_c("run");
    Utf8String *methodType = utf8_create_c("()V");
    MethodInfo *method = NULL;
    method = find_instance_methodInfo_by_name(jthread, methodName, methodType, runtime);
    utf8_destory(methodName);
    utf8_destory(methodType);
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("therad_loader    %s.%s%s  \n", utf8_cstr(method->_this_class->name),
               utf8_cstr(method->name), utf8_cstr(method->descriptor));
#endif
    //gc_refer_reg(runtime, jthread);//20201019 gust comment it , duplicate reg
    if (jvm->jdwp_enable && jvm->jdwpserver)event_on_thread_start(jvm->jdwpserver, runtime->thrd_info->jthread);
    runtime->thrd_info->thread_status = THREAD_STATUS_RUNNING;
    push_ref(runtime->stack, (__refer) jthread);
    ret = execute_method_impl(method, runtime);
    if (ret != RUNTIME_STATUS_NORMAL && ret != RUNTIME_STATUS_INTERRUPT) {
        print_exception(runtime);
    }
    runtime->thrd_info->thread_status = THREAD_STATUS_ZOMBIE;
    jthread_dispose(jthread, runtime);
    gc_move_objs_thread_2_gc(runtime);
    runtime_destory(runtime);
#if _JVM_DEBUG_LOG_LEVEL > 0
    s64 spent = currentTimeMillis() - startAt;
    jvm_printf("[INFO]thread over %llx , return %d , spent : %lld\n", (s64) (intptr_t) jthread, ret, spent);
#endif
    thrd_exit(ret);
    return ret;
}

thrd_t jthread_start(Instance *ins, Runtime *parent) {//
    Runtime *runtime = runtime_create(parent->jvm);
    runtime->thrd_info->jthread = ins;
    runtime->thrd_info->context_classloader = parent->thrd_info->context_classloader;//copy context classloader

    jthread_init(ins, runtime);
    thrd_create(&runtime->thrd_info->pthread, jthread_run, runtime);
    return runtime->thrd_info->pthread;
}

__refer jthread_get_name_value(MiniJVM *jvm, Instance *ins) {
    c8 *ptr = getInstanceFieldPtr(ins, jvm->shortcut.thread_name);
    return getFieldRefer(ptr);
}

__refer jthread_get_stackframe_value(MiniJVM *jvm, Instance *ins) {
    c8 *ptr = getInstanceFieldPtr(ins, jvm->shortcut.thread_stackFrame);
    return (__refer) (intptr_t) getFieldLong(ptr);
}

void jthread_set_stackframe_value(MiniJVM *jvm, Instance *ins, __refer val) {
    c8 *ptr = getInstanceFieldPtr(ins, jvm->shortcut.thread_stackFrame);
    setFieldLong(ptr, (s64) (intptr_t) val);
}

s32 jthread_get_daemon_value(Instance *ins, Runtime *runtime) {
    c8 *ptr = getFieldPtr_byName_c(ins, STR_CLASS_JAVA_LANG_THREAD, "daemon", "Z", runtime);
    if (ptr) {
        return getFieldByte(ptr);
    }
    return 0;
}

void jthread_set_daemon_value(Instance *ins, Runtime *runtime, s32 daemon) {
    c8 *ptr = getFieldPtr_byName_c(ins, STR_CLASS_JAVA_LANG_THREAD, "daemon", "Z", runtime);
    if (ptr) {
        setFieldByte(ptr, (s8) daemon);
    }
}

void jthreadlock_create(Runtime *runtime, MemoryBlock *mb) {
    spin_lock(&runtime->jvm->lock_cloader);
    if (!mb->thread_lock) {
        ThreadLock *tl = jvm_calloc(sizeof(ThreadLock));
        thread_lock_init(tl);
        mb->thread_lock = tl;
    }
    spin_unlock(&runtime->jvm->lock_cloader);
}

void jthreadlock_destory(MemoryBlock *mb) {
    thread_lock_dispose(mb->thread_lock);
    if (mb->thread_lock) {
        jvm_free(mb->thread_lock);
        mb->thread_lock = NULL;
    }
}

s32 jthread_lock(MemoryBlock *mb, Runtime *runtime) { //可能会重入，同一个线程多次锁同一对象
    if (mb == NULL)return -1;
    if (!mb->thread_lock) {
        jthreadlock_create(runtime, mb);
    }
    ThreadLock *jtl = mb->thread_lock;
    //can pause when lock
    while (mtx_trylock(&jtl->mutex_lock) != thrd_success) {
        check_suspend_and_pause(runtime);
        jthread_yield(runtime);
    }

#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("  lock: %llx   lock holder: %s \n", (s64) (intptr_t) (runtime->thrd_info->jthread),
               utf8_cstr(mb->clazz->name));
#endif
    return 0;
}

s32 jthread_unlock(MemoryBlock *mb, Runtime *runtime) {
    if (mb == NULL)return -1;
    if (!mb->thread_lock) {
        jthreadlock_create(runtime, mb);
    }
    ThreadLock *jtl = mb->thread_lock;
    mtx_unlock(&jtl->mutex_lock);
#if _JVM_DEBUG_LOG_LEVEL > 5
    invoke_deepth(runtime);
    jvm_printf("unlock: %llx   lock holder: %s, \n", (s64) (intptr_t) (runtime->thrd_info->jthread),
               utf8_cstr(mb->clazz->name));
#endif
    return 0;
}

s32 jthread_notify(MemoryBlock *mb, Runtime *runtime) {
    if (mb == NULL)return -1;
    if (mb->thread_lock == NULL) {
        jthreadlock_create(runtime, mb);
    }
    cnd_signal(&mb->thread_lock->thread_cond);
    return 0;
}

s32 jthread_notifyAll(MemoryBlock *mb, Runtime *runtime) {
    if (mb == NULL)return -1;
    if (mb->thread_lock == NULL) {
        jthreadlock_create(runtime, mb);
    }
    cnd_broadcast(&mb->thread_lock->thread_cond);
    return 0;
}

s32 jthread_yield(Runtime *runtime) {
    thrd_yield();
    return 0;
}

s32 jthread_suspend(Runtime *runtime) {
    spin_lock(&runtime->thrd_info->lock);
    runtime->thrd_info->suspend_count++;
    spin_unlock(&runtime->thrd_info->lock);
    return 0;
}

void jthread_block_enter(Runtime *runtime) {
    runtime->thrd_info->is_blocking = 1;
}

void jthread_block_exit(Runtime *runtime) {
    runtime->thrd_info->is_blocking = 0;
    check_suspend_and_pause(runtime);
}

s32 jthread_resume(Runtime *runtime) {
    spin_lock(&runtime->thrd_info->lock);
    if (runtime->thrd_info->suspend_count > 0)runtime->thrd_info->suspend_count--;
    spin_unlock(&runtime->thrd_info->lock);
    return 0;
}

s32 jthread_waitTime(MemoryBlock *mb, Runtime *runtime, s64 waitms) {
    if (mb == NULL)return -1;
    if (!mb->thread_lock) {
        jthreadlock_create(runtime, mb);
    }
    jthread_block_enter(runtime);
    runtime->thrd_info->curThreadLock = mb;
    runtime->thrd_info->thread_status = THREAD_STATUS_WAIT;
    if (waitms) {
        waitms += currentTimeMillis();
        struct timespec t;
        //clock_gettime(CLOCK_REALTIME, &t);
        t.tv_sec = waitms / 1000;
        t.tv_nsec = (waitms % 1000) * 1000000;
        cnd_timedwait(&mb->thread_lock->thread_cond, &mb->thread_lock->mutex_lock, &t);
    } else {
        cnd_wait(&mb->thread_lock->thread_cond, &mb->thread_lock->mutex_lock);
    }
    runtime->thrd_info->thread_status = THREAD_STATUS_RUNNING;
    runtime->thrd_info->curThreadLock = NULL;
    jthread_block_exit(runtime);
    return 0;
}

s32 jthread_sleep(Runtime *runtime, s64 ms) {
    jthread_block_enter(runtime);
    runtime->thrd_info->thread_status = THREAD_STATUS_SLEEPING;
    threadSleep(ms);
    runtime->thrd_info->thread_status = THREAD_STATUS_RUNNING;
    jthread_block_exit(runtime);
    return 0;
}

s32 check_suspend_and_pause(Runtime *runtime) {
    JavaThreadInfo *threadInfo = runtime->thrd_info;
    MiniJVM *jvm = runtime->jvm;
    if (threadInfo->suspend_count && !threadInfo->no_pause) {
        vm_share_lock(jvm);
        threadInfo->is_suspend = 1;
        while (threadInfo->suspend_count) {
            vm_share_notifyall(jvm);
            vm_share_timedwait(jvm, 20);
        }
        threadInfo->is_suspend = 0;
        //jvm_printf(".");
        vm_share_unlock(jvm);
    }
    return 0;
}

//===============================    实例化数组  ==================================
Instance *jarray_create_by_class(Runtime *runtime, s32 count, JClass *clazz) {
    if (count < 0)return NULL;
    s32 typeIdx = clazz->mb.arr_type_index;
    s32 width = DATA_TYPE_BYTES[typeIdx];
    s32 insSize = instance_base_size() + (width * count);
    Instance *arr = jvm_calloc(insSize);
    arr->mb.heap_size = insSize;
    arr->mb.type = MEM_TYPE_ARR;
    arr->mb.clazz = clazz;
    arr->mb.arr_type_index = typeIdx;
    arr->arr_length = count;
    if (arr->arr_length)arr->arr_body = (c8 *) (&arr[1]);
    gc_obj_reg(runtime, arr);
//    jvm_printf("%s\n", utf8_cstr(clazz->name));
//    if(utf8_equals_c(clazz->name,"[Lorg/mini/util/StringFormatImpl$FmtCmpnt;")){
//        int debug = 1;
//    }
    return arr;
}

Instance *jarray_create_by_type_index(Runtime *runtime, s32 count, s32 typeIdx) {
    JClass *clazz = NULL;
    clazz = array_class_get_by_index(runtime, typeIdx);
    Instance *arr = jarray_create_by_class(runtime, count, clazz);
    return arr;
}

Instance *jarray_create_by_type_name(Runtime *runtime, s32 count, Utf8String *name) {
    JClass *clazz = NULL;
    clazz = array_class_get_by_name(runtime, name);
    Instance *arr = jarray_create_by_class(runtime, count, clazz);
    return arr;
}


s32 jarray_destory(Instance *arr) {
    if (arr && arr->mb.type == MEM_TYPE_ARR) {
        jthreadlock_destory(&arr->mb);
        arr->mb.thread_lock = NULL;
        arr->arr_length = -1;
        jvm_free(arr);
    }
    return 0;
}

/**
 * create multi array
 * @param dim arrdim
 * @param pdesc desc
 * @return ins
 */
Instance *jarray_multi_create(Runtime *runtime, s32 *dim, s32 dim_size, Utf8String *pdesc, s32 deep) {
    s32 len = dim[dim_size - 1 - deep];
    if (len == -1) {
        return NULL;
    }
    JClass *cl = array_class_create_get(runtime, pdesc);
    Instance *arr = jarray_create_by_class(runtime, len, cl);
    Utf8String *desc = utf8_create_part(pdesc, 1, pdesc->length - 1);

    c8 ch = utf8_char_at(desc, 0);
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("multi arr deep :%d  type(%c) arr[%x] size:%d\n", deep, ch, arr, len);
#endif
    if (ch == '[') {
        int i;
        s64 val;
        for (i = 0; i < len; i++) {
            Instance *elem = jarray_multi_create(runtime, dim, dim_size, desc, deep + 1);
            val = (intptr_t) elem;
            jarray_set_field(arr, i, val);
        }
    }
    utf8_destory(desc);
    return arr;
}


void jarray_set_field(Instance *arr, s32 index, s64 val) {
    s32 idx = arr->mb.arr_type_index;
    s32 bytes = DATA_TYPE_BYTES[idx];
    if (isDataReferByIndex(idx)) {
        setFieldRefer((c8 *) ((__refer *) arr->arr_body + index), (__refer) (intptr_t) val);
    } else {
        switch (bytes) {
            case 1:
                setFieldByte((c8 *) (arr->arr_body + index), (s8) val);
                break;
            case 2:
                setFieldShort((c8 *) ((s16 *) arr->arr_body + index), (s16) val);
                break;
            case 4:
                setFieldInt((c8 *) ((s32 *) arr->arr_body + index), (s32) val);
                break;
            case 8:
                setFieldLong((c8 *) ((s64 *) arr->arr_body + index), val);
                break;
        }
    }
}

s64 jarray_get_field(Instance *arr, s32 index) {
    s32 idx = arr->mb.arr_type_index;
    s32 bytes = DATA_TYPE_BYTES[idx];
    s64 val = 0;
    if (isDataReferByIndex(idx)) {
        val = (intptr_t) getFieldRefer((c8 *) ((__refer *) arr->arr_body + index));
    } else {
        switch (bytes) {
            case 1:
                val = getFieldByte(arr->arr_body + index);
                break;
            case 2:
                if (idx == DATATYPE_JCHAR) {
                    val = (u16) getFieldShort((c8 *) ((u16 *) arr->arr_body + index));
                } else
                    val = getFieldShort((c8 *) ((s16 *) arr->arr_body + index));
                break;
            case 4:
                val = getFieldInt((c8 *) ((s32 *) arr->arr_body + index));
                break;
            case 8:
                val = getFieldLong((c8 *) ((s64 *) arr->arr_body + index));
                break;
        }
    }
    return val;
}

//===============================    实例化对象  ==================================

s32 instance_base_size() {
    s32 ins_base = sizeof(Instance);
    s32 align = 8;
    ins_base = ins_base / align * align + ((ins_base % align) > 0 ? align : 0);
    return ins_base;
}

Instance *instance_create(Runtime *runtime, JClass *clazz) {
    s32 insSize = instance_base_size() + clazz->field_instance_len;
    Instance *ins = jvm_calloc(insSize);
    ins->mb.type = MEM_TYPE_INS;
    ins->mb.clazz = clazz;
    ins->mb.heap_size = insSize;

    ins->obj_fields = ((c8 *) (&ins[0])) + instance_base_size();//jvm_calloc(clazz->field_instance_len);
//    jvm_printf("%s\n", utf8_cstr(clazz->name));
//    if (utf8_equals_c(clazz->name, "java/lang/String")) {
//        s32 debug = 1;
//    }
    gc_obj_reg(runtime, ins);
    return ins;
}

void instance_init(Instance *ins, Runtime *runtime) {
    instance_init_with_para(ins, runtime, "()V", NULL);
}

void instance_init_with_para(Instance *ins, Runtime *runtime, c8 *methodtype, RuntimeStack *para) {
    if (ins) {
        Utf8String *methodName = utf8_create_c("<init>");
        Utf8String *methodType = utf8_create_c(methodtype);
        MethodInfo *mi = find_methodInfo_by_name(ins->mb.clazz->name, methodName, methodType, ins->mb.clazz->jloader, runtime);
        push_ref(runtime->stack, (__refer) ins);
        if (para) {
            s32 i;
            for (i = 0; i < stack_size(para); i++) {
                StackEntry entry;
                peek_entry(para->store + i, &entry);
                push_entry(runtime->stack, &entry);
            }
        }
        s32 ret = execute_method_impl(mi, runtime);
        if (ret != RUNTIME_STATUS_NORMAL) {
            print_exception(runtime);
        }
        utf8_destory(methodName);
        utf8_destory(methodType);
    }
}

void instance_finalize(Instance *ins, Runtime *runtime) {
    if (ins) {
        MethodInfo *mi = ins->mb.clazz->finalizeMethod;
        if (mi) {
            push_ref(runtime->stack, ins);
            s32 ret = execute_method_impl(mi, runtime);
            if (ret != RUNTIME_STATUS_NORMAL) {
                print_exception(runtime);
            }
        }
    }
}

void instance_of_reference_enqueue(Instance *ins, Runtime *runtime) {
    if (ins) {
        MethodInfo *mi = runtime->jvm->shortcut.reference_vmEnqueneReference;
        if (mi) {
            push_ref(runtime->stack, ins);
            s32 ret = execute_method_impl(mi, runtime);
            if (ret != RUNTIME_STATUS_NORMAL) {
                print_exception(runtime);
            }
        }
    }
}

void instance_clear_refer(Instance *ins) {
    s32 i;
    JClass *clazz = ins->mb.clazz;
    while (clazz) {
        FieldPool *fp = &clazz->fieldPool;
        for (i = 0; i < fp->field_used; i++) {
            FieldInfo *fi = &fp->field[i];
            if ((fi->access_flags & ACC_STATIC) == 0 && fi->isrefer) {
                c8 *ptr = getInstanceFieldPtr(ins, fi);
                if (ptr) {
                    setFieldRefer(ptr, NULL);
                }
            }
        }
        clazz = getSuperClass(clazz);
    }
}

s32 instance_destory(Instance *ins) {
    jthreadlock_destory(&ins->mb);
    jvm_free(ins);

    return 0;
}

/**
 * for java string instance copy
 * deepth copy instance
 * deepth copy array
 *
 * @param src  source instance
 * @return  instance
 */
Instance *instance_copy(Runtime *runtime, Instance *src, s32 deep_copy) {
    s32 bodySize = 0;
    if (src->mb.type == MEM_TYPE_INS) {
        bodySize = src->mb.clazz->field_instance_len;
    } else if (src->mb.type == MEM_TYPE_ARR) {
        bodySize = src->arr_length * DATA_TYPE_BYTES[src->mb.arr_type_index];
    }
    s32 insSize = instance_base_size() + bodySize;
    Instance *dst = jvm_malloc(insSize);
    memcpy(dst, src, instance_base_size());
    dst->mb.thread_lock = NULL;
    dst->mb.gcflag = src->mb.gcflag;
    GCFLAG_REG_CLEAR(dst->mb.gcflag);
    GCFLAG_FINALIZED_CLEAR(dst->mb.gcflag);
    dst->mb.garbage_mark = 0;
    dst->mb.heap_size = insSize;
    if (src->mb.type == MEM_TYPE_INS) {
        JClass *clazz = src->mb.clazz;
        s32 fileds_len = clazz->field_instance_len;
        if (fileds_len) {
            dst->obj_fields = (c8 *) dst + instance_base_size();//
            memcpy(dst->obj_fields, src->obj_fields, fileds_len);
            if (deep_copy) {
                s32 i, len;
                while (clazz) {
                    FieldPool *fp = &clazz->fieldPool;
                    for (i = 0, len = fp->field_used; i < len; i++) {
                        FieldInfo *fi = &fp->field[i];
                        if ((fi->access_flags & ACC_STATIC) == 0 && fi->isrefer) {
                            c8 *ptr = getInstanceFieldPtr(src, fi);
                            Instance *ins = (Instance *) getFieldRefer(ptr);
                            if (ins) {
                                Instance *new_ins = instance_copy(runtime, ins, deep_copy);
                                ptr = getInstanceFieldPtr(dst, fi);
                                setFieldRefer(ptr, new_ins);
                            }
                        }
                    }
                    clazz = getSuperClass(clazz);
                }
            }
        }
    } else if (src->mb.type == MEM_TYPE_ARR) {
        s32 size = src->arr_length * DATA_TYPE_BYTES[src->mb.arr_type_index];
        dst->arr_body = (c8 *) dst + instance_base_size();//
        if (isDataReferByIndex(src->mb.arr_type_index) && deep_copy) {
            s32 i;
            s64 val;
            for (i = 0; i < dst->arr_length; i++) {
                val = jarray_get_field(src, i);
                if (val) {
                    val = (intptr_t) instance_copy(runtime, (Instance *) getFieldRefer((__refer) (intptr_t) val), deep_copy);
                    jarray_set_field(dst, i, val);
                }
            }
        } else {
            memcpy(dst->arr_body, src->arr_body, size);
        }
    }
    gc_obj_reg(runtime, dst);
    return dst;
}

//===============================    实例化 java.lang.Class  ==================================
/**
 *
 * 每个java 类有一个 java.lang.Class 的实例, 用于承载对相关java类的操作
 *
 * every java Class have a instance of java.lang.Class
 *
 * @param runtime
 * @param clazz
 * @return
 */

Instance *insOfJavaLangClass_create_get(Runtime *runtime, JClass *clazz) {
    JClass *java_lang_class = classes_load_get_c(NULL, STR_CLASS_JAVA_LANG_CLASS, runtime);
    if (java_lang_class) {
        if (clazz->ins_class) {
            return clazz->ins_class;
        } else {
            Instance *ins = instance_create(runtime, java_lang_class);
            instance_init(ins, runtime);
            clazz->ins_class = ins;
            insOfJavaLangClass_set_classHandle(runtime, ins, clazz);
            insOfJavaLangClass_hold(clazz, runtime);
            return ins;
        }
    }
    return NULL;
}


JClass *insOfJavaLangClass_get_classHandle(Runtime *runtime, Instance *insOfJavaLangClass) {
    return (JClass *) (intptr_t) getFieldLong(getInstanceFieldPtr(insOfJavaLangClass, runtime->jvm->shortcut.class_classHandle));
}

void insOfJavaLangClass_set_classHandle(Runtime *runtime, Instance *insOfJavaLangClass, JClass *handle) {
    setFieldLong(getInstanceFieldPtr(insOfJavaLangClass, runtime->jvm->shortcut.class_classHandle), (s64) (intptr_t) handle);
}

void insOfJavaLangClass_hold(JClass *clazz, Runtime *runtime) {
    if (clazz) {
        Instance *loader = clazz->jloader;
        if (loader) { //if classloader exists , then hold in java classloader
            runtime->thrd_info->no_pause++;
            push_ref(runtime->stack, loader);
            push_ref(runtime->stack, clazz->ins_class ? clazz->ins_class : insOfJavaLangClass_create_get(runtime, clazz));
            s32 ret = execute_method_impl(runtime->jvm->shortcut.classloader_holdClass, runtime);
            if (ret) {
                print_exception(runtime);
            }
            runtime->thrd_info->no_pause--;
        } else { //hold in systemclassloader
            gc_obj_hold(runtime->jvm->collector, clazz->ins_class);
        }
    }
}

//===============================    实例化字符串  ==================================
Instance *jstring_create(Utf8String *src, Runtime *runtime) {
    if (!src)return NULL;
    JClass *jstr_clazz = classes_load_get_c(NULL, STR_CLASS_JAVA_LANG_STRING, runtime);
    Instance *jstring = instance_create(runtime, jstr_clazz);
    instance_hold_to_thread(jstring, runtime);//hold for no gc

    jstring->mb.clazz = jstr_clazz;
    instance_init(jstring, runtime);

    c8 *ptr = jstring_get_value_ptr(jstring, runtime);
    u16 *buf = jvm_calloc(src->length * DATA_TYPE_BYTES[DATATYPE_JCHAR]);
    s32 len = utf8_2_unicode(src, buf);
    if (len >= 0) {//可能解析出错
        Instance *arr = jarray_create_by_type_index(runtime, len, DATATYPE_JCHAR);//u16 type is 5
        setFieldRefer(ptr, (__refer) arr);//设置数组
        memcpy(arr->arr_body, buf, len * DATA_TYPE_BYTES[DATATYPE_JCHAR]);
    }
    jvm_free(buf);
    jstring_set_count(jstring, len, runtime);//设置长度
    instance_release_from_thread(jstring, runtime);
    return jstring;
}

Instance *jstring_create_cstr(c8 *cstr, Runtime *runtime) {
    if (!cstr)return NULL;
    Utf8String *ustr = utf8_create_part_c(cstr, 0, strlen(cstr));
    Instance *jstr = jstring_create(ustr, runtime);
    utf8_destory(ustr);
    return jstr;
}

s32 jstring_get_count(Instance *jstr, Runtime *runtime) {
    return getFieldInt(getInstanceFieldPtr(jstr, runtime->jvm->shortcut.string_count));
}

void jstring_set_count(Instance *jstr, s32 count, Runtime *runtime) {
    setFieldInt(getInstanceFieldPtr(jstr, runtime->jvm->shortcut.string_count), count);
}

s32 jstring_get_offset(Instance *jstr, Runtime *runtime) {
    return getFieldInt(getInstanceFieldPtr(jstr, runtime->jvm->shortcut.string_offset));
}

c8 *jstring_get_value_ptr(Instance *jstr, Runtime *runtime) {
    return getInstanceFieldPtr(jstr, runtime->jvm->shortcut.string_value);
}

Instance *jstring_get_value_array(Instance *jstr, Runtime *runtime) {
    c8 *fieldPtr = jstring_get_value_ptr(jstr, runtime);
    Instance *arr = (Instance *) getFieldRefer(fieldPtr);
    return arr;
}

u16 jstring_char_at(Instance *jstr, s32 index, Runtime *runtime) {
    Instance *ptr = jstring_get_value_array(jstr, runtime);
    s32 offset = jstring_get_offset(jstr, runtime);
    s32 count = jstring_get_count(jstr, runtime);
    if (index >= count) {
        return -1;
    }
    if (ptr && ptr->arr_body) {
        u16 *jchar_arr = (u16 *) ptr->arr_body;
        return jchar_arr[offset + index];
    }
    return -1;
}


s32 jstring_index_of(Instance *jstr, u16 ch, s32 startAt, Runtime *runtime) {
    c8 *fieldPtr = jstring_get_value_ptr(jstr, runtime);
    Instance *ptr = (Instance *) getFieldRefer(fieldPtr);//char[]数组实例
    if (ptr && ptr->arr_body && startAt >= 0) {
        u16 *jchar_arr = (u16 *) ptr->arr_body;
        s32 count = jstring_get_count(jstr, runtime);
        s32 offset = jstring_get_offset(jstr, runtime);
        s32 i;
        for (i = startAt; i < count; i++) {
            if (jchar_arr[i + offset] == ch) {
                return i;
            }
        }
    }
    return -1;
}

s32 jstring_equals(Instance *jstr1, Instance *jstr2, Runtime *runtime) {
    if (!jstr1 && !jstr2) { //两个都是null
        return 1;
    } else if (!jstr1) {
        return 0;
    } else if (!jstr2) {
        return 0;
    }
    Instance *arr1 = jstring_get_value_array(jstr1, runtime);//取得 char[] value
    Instance *arr2 = jstring_get_value_array(jstr2, runtime);//取得 char[] value
    s32 count1 = 0, offset1 = 0, count2 = 0, offset2 = 0;
    //0长度字符串可能value[] 是空值，也可能不是空值但count是0
    if (arr1) {
        count1 = jstring_get_count(jstr1, runtime);
        offset1 = jstring_get_offset(jstr1, runtime);
    }
    if (arr2) {
        count2 = jstring_get_count(jstr2, runtime);
        offset2 = jstring_get_offset(jstr2, runtime);
    }
    if (count1 != count2) {
        return 0;
    } else if (count1 == 0 && count2 == 0) {
        return 1;
    }
    if (arr1 && arr2 && arr1->arr_body && arr2->arr_body) {
        u16 *jchar_arr1 = (u16 *) arr1->arr_body;
        u16 *jchar_arr2 = (u16 *) arr2->arr_body;
        s32 i;
        for (i = 0; i < count1; i++) {
            if (jchar_arr1[i + offset1] != jchar_arr2[i + offset2]) {
                return 0;
            }
        }
        return 1;
    }
    return 0;
}

s32 jstring_2_utf8(Instance *jstr, Utf8String *utf8, Runtime *runtime) {
    if (!jstr)return 0;
    Instance *arr = jstring_get_value_array(jstr, runtime);
    if (arr) {
        s32 count = jstring_get_count(jstr, runtime);
        s32 offset = jstring_get_offset(jstr, runtime);
        u16 *arrbody = (u16 *) arr->arr_body;
        if (arr->arr_body)unicode_2_utf8(&arrbody[offset], utf8, count);
    }
    return 0;
}
//===============================    例外  ==================================

Instance *exception_create(s32 exception_type, Runtime *runtime) {
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("create exception : %s\n", STRS_CLASS_EXCEPTION[exception_type]);
#endif
    Utf8String *clsName = utf8_create_c(STRS_CLASS_EXCEPTION[exception_type]);
    JClass *clazz = classes_load_get(NULL, clsName, runtime);
    utf8_destory(clsName);

    Instance *ins = instance_create(runtime, clazz);
    instance_hold_to_thread(ins, runtime);
    instance_init(ins, runtime);
    instance_release_from_thread(ins, runtime);
    return ins;
}

Instance *exception_create_str(s32 exception_type, Runtime *runtime, c8 *errmsg) {
#if _JVM_DEBUG_LOG_LEVEL > 5
    jvm_printf("create exception : %s\n", STRS_CLASS_EXCEPTION[exception_type]);
#endif
    Utf8String *uerrmsg = utf8_create_c(errmsg);
    Instance *jstr = jstring_create(uerrmsg, runtime);
    instance_hold_to_thread(jstr, runtime);
    utf8_destory(uerrmsg);
    RuntimeStack *para = stack_create(10);
    push_ref(para, jstr);
    instance_release_from_thread(jstr, runtime);
    Utf8String *clsName = utf8_create_c(STRS_CLASS_EXCEPTION[exception_type]);
    JClass *clazz = classes_load_get(NULL, clsName, runtime);
    utf8_destory(clsName);
    Instance *ins = instance_create(runtime, clazz);
    instance_hold_to_thread(ins, runtime);
    instance_init_with_para(ins, runtime, "(Ljava/lang/String;)V", para);
    instance_release_from_thread(ins, runtime);
    stack_destory(para);
    return ins;
}
//===============================    lambda  ==================================


Instance *method_type_create(Runtime *runtime, Instance *jloader, Utf8String *desc) {
    JClass *cl = classes_load_get_c(NULL, STR_CLASS_JAVA_LANG_INVOKE_METHODTYPE, runtime);
    if (cl) {
        Instance *mt = instance_create(runtime, cl);
        instance_hold_to_thread(mt, runtime);
        Instance *jstr_desc = jstring_create(desc, runtime);

        RuntimeStack *para = stack_create(10);

        push_ref(para, jloader);
        push_ref(para, jstr_desc);
        instance_init_with_para(mt, runtime, "(Ljava/lang/ClassLoader;Ljava/lang/String;)V", para);
        stack_destory(para);
        instance_release_from_thread(mt, runtime);
        return mt;
    }
    return NULL;
}

Instance *method_handle_create(Runtime *runtime, MethodInfo *mi, s32 kind) {
    JClass *cl = classes_load_get_c(NULL, STR_CLASS_JAVA_LANG_INVOKE_METHODHANDLE, runtime);
    if (cl) {
        Instance *mh = instance_create(runtime, cl);
        instance_hold_to_thread(mh, runtime);
        RuntimeStack *para = stack_create(10);
        push_int(para, kind);
        Instance *jstr_clsName = jstring_create(mi->_this_class->name, runtime);
        instance_hold_to_thread(jstr_clsName, runtime);
        push_ref(para, jstr_clsName);
        Instance *jstr_methodName = jstring_create(mi->name, runtime);
        push_ref(para, jstr_methodName);
        instance_hold_to_thread(jstr_methodName, runtime);
        Instance *jstr_methodDesc = jstring_create(mi->descriptor, runtime);
        push_ref(para, jstr_methodDesc);
        instance_hold_to_thread(jstr_methodDesc, runtime);
        push_ref(para, mi->_this_class->jloader);
        instance_init_with_para(mh, runtime, "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V", para);
        stack_destory(para);
        instance_release_from_thread(mh, runtime);
        instance_release_from_thread(jstr_clsName, runtime);
        instance_release_from_thread(jstr_methodName, runtime);
        instance_release_from_thread(jstr_methodDesc, runtime);
        return mh;
    }
    return NULL;
}

Instance *method_handles_lookup_create(Runtime *runtime, JClass *caller) {
    JClass *cl = classes_load_get_c(NULL, STR_CLASS_JAVA_LANG_INVOKE_METHODHANDLES_LOOKUP, runtime);
    if (cl) {
        Instance *lookup = instance_create(runtime, cl);
        instance_hold_to_thread(lookup, runtime);
        RuntimeStack *para = stack_create(10);

        push_ref(para, insOfJavaLangClass_create_get(runtime, caller));
        instance_init_with_para(lookup, runtime, "(Ljava/lang/Class;)V", para);
        stack_destory(para);
        instance_release_from_thread(lookup, runtime);
        return lookup;
    }
    return NULL;
}
//===============================    实例操作  ==================================



c8 *getFieldPtr_byName_c(Instance *instance, c8 *pclassName, c8 *pfieldName, c8 *pfieldType, Runtime *runtime) {
    Utf8String *clsName = utf8_create_c(pclassName);
    //Class *clazz = classes_get(clsName);

    //set value
    Utf8String *fieldName = utf8_create_c(pfieldName);
    Utf8String *fieldType = utf8_create_c(pfieldType);
    c8 *ptr = getFieldPtr_byName(instance, clsName, fieldName, fieldType, runtime);
    utf8_destory(clsName);
    utf8_destory(fieldName);
    utf8_destory(fieldType);
    return ptr;
}


c8 *getFieldPtr_byName(Instance *instance, Utf8String *clsName, Utf8String *fieldName, Utf8String *fieldType, Runtime *runtime) {

    c8 *ptr = NULL;
    FieldInfo *fi = find_fieldInfo_by_name(clsName, fieldName, fieldType, instance->mb.clazz->jloader, runtime);

    if (fi) {
        if (fi->access_flags & ACC_STATIC) {
            ptr = getStaticFieldPtr(fi);
        } else {
            ptr = getInstanceFieldPtr(instance, fi);
        }
    }
    return ptr;
}

s32 getLineNumByIndex(CodeAttribute *ca, s32 offset) {
    s32 j;

    for (j = 0; j < ca->line_number_table_length; j++) {
        LineNumberTable *node = &(ca->line_number_table[j]);
        if (offset >= node->start_pc) {
            if (j + 1 < ca->line_number_table_length) {
                LineNumberTable *next_node = &(ca->line_number_table[j + 1]);

                if (offset < next_node->start_pc) {
                    return node->line_number;
                }
            } else {
                return node->line_number;
            }
        }
    }
    return -1;
}


void memoryblock_destory(__refer ref) {
    MemoryBlock *mb = (MemoryBlock *) ref;
    if (!mb)return;
//    if (utf8_equals_c(mb->clazz->name, "test/GuiTest$CallBack")) {
//        garbage_dump_runtime();
//        int debug = 1;
//    }
    if (mb->type == MEM_TYPE_INS) {
        instance_destory((Instance *) mb);
    } else if (mb->type == MEM_TYPE_ARR) {
        jarray_destory((Instance *) mb);
    } else if (mb->type == MEM_TYPE_CLASS) {
        class_destory((JClass *) mb);
    }
}

JavaThreadInfo *threadinfo_create() {
    JavaThreadInfo *threadInfo = jvm_calloc(sizeof(JavaThreadInfo));
    threadInfo->stacktrack = arraylist_create(16);
    threadInfo->lineNo = arraylist_create(16);
    threadInfo->jdwp_step = jvm_calloc(sizeof(JdwpStep));
    spin_init(&threadInfo->lock, 0);
    return threadInfo;
}

void threadinfo_destory(JavaThreadInfo *threadInfo) {
    arraylist_destory(threadInfo->lineNo);
    arraylist_destory(threadInfo->stacktrack);
    jvm_free(threadInfo->jdwp_step);
    spin_destroy(&threadInfo->lock);
    jvm_free(threadInfo);
}

s64 currentTimeMillis() {

    struct timespec tv;
    clock_gettime(CLOCK_REALTIME, &tv);
    return ((s64) tv.tv_sec) * MILL_2_SEC_SCALE + tv.tv_nsec / NANO_2_MILLS_SCALE;
}

s64 nanoTime() {

    struct timespec tv;
    clock_gettime(CLOCK_REALTIME, &tv);

    if (!nano_sec_start_at) {
        nano_sec_start_at = ((s64) tv.tv_sec) * NANO_2_SEC_SCALE + tv.tv_nsec;
    }
    s64 v = ((s64) tv.tv_sec) * NANO_2_SEC_SCALE + tv.tv_nsec;
    return v - nano_sec_start_at;
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

void instance_hold_to_thread(Instance *ins, Runtime *runtime) {
    if (runtime && ins) {
        ins->mb.tmp_next = runtime->thrd_info->tmp_holder;
        runtime->thrd_info->tmp_holder = (MemoryBlock *) ins;
    }
}

void instance_release_from_thread(Instance *ins, Runtime *runtime) {
    if (runtime && ins) {
        MemoryBlock *ref = (MemoryBlock *) ins;
        if (ref == runtime->thrd_info->tmp_holder) {
            runtime->thrd_info->tmp_holder = ref->tmp_next;
            return;
        }
        MemoryBlock *next, *pre;
        pre = runtime->thrd_info->tmp_holder;
        if (pre) {
            next = pre->tmp_next;

            while (next) {
                if (ref == next) {
                    pre->tmp_next = next->tmp_next;
                    return;
                }
                pre = next;
                next = next->tmp_next;
            }
        }
    }
}

CStringArr *cstringarr_create(Instance *jstr_arr) { //byte[][] to char**
    if (!jstr_arr)return NULL;
    CStringArr *cstr_arr = jvm_calloc(sizeof(CStringArr));
    cstr_arr->arr_length = jstr_arr->arr_length;
    cstr_arr->arr_body = jvm_calloc(jstr_arr->arr_length * sizeof(__refer));
    s32 i;
    for (i = 0; i < cstr_arr->arr_length; i++) {
        s64 val = jarray_get_field(jstr_arr, i);
        Instance *jbyte_arr = (__refer) (intptr_t) val;
        if (jbyte_arr) {
            cstr_arr->arr_body[i] = jbyte_arr->arr_body;
        }
    }
    return cstr_arr;
}

void cstringarr_destory(CStringArr *cstr_arr) {
    jvm_free(cstr_arr->arr_body);
    jvm_free(cstr_arr);
}

ReferArr *referarr_create(Instance *jobj_arr) {
    if (!jobj_arr)return NULL;
    CStringArr *ref_arr = jvm_calloc(sizeof(CStringArr));
    ref_arr->arr_length = jobj_arr->arr_length;
    ref_arr->arr_body = jvm_calloc(jobj_arr->arr_length * sizeof(__refer));
    s32 i;
    for (i = 0; i < ref_arr->arr_length; i++) {
        s64 val = jarray_get_field(jobj_arr, i);
        ref_arr->arr_body[i] = (__refer) (intptr_t) val;
    }
    return ref_arr;
}

void referarr_destory(CStringArr *ref_arr) {
    jvm_free(ref_arr->arr_body);
    jvm_free(ref_arr);
}

void referarr_2_jlongarr(ReferArr *ref_arr, Instance *jlong_arr) {
    s32 i;
    for (i = 0; i < ref_arr->arr_length && i < jlong_arr->arr_length; i++) {
        __refer ref = ref_arr->arr_body[i];
        jarray_set_field(jlong_arr, i, (intptr_t) ref);
    }
};


/**
 * load file less than 4G bytes
 */
s32 _loadFileContents(c8 *file, ByteBuf *buf) {

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


ByteBuf *load_file_from_classpath(PeerClassLoader *cloader, Utf8String *path) {
    ByteBuf *bytebuf = NULL;
    s32 i, iret;
    for (i = 0; i < cloader->classpath->length; i++) {
        Utf8String *pClassPath = arraylist_get_value(cloader->classpath, i);
        if (isDir(pClassPath)) { //form file
            Utf8String *filepath = utf8_create_copy(pClassPath);
            utf8_pushback(filepath, '/');
            utf8_append(filepath, path);

            bytebuf = bytebuf_create(16);
            iret = _loadFileContents(utf8_cstr(filepath), bytebuf);
            utf8_destory(filepath);
            //回收
            if (iret != 0) {
                bytebuf_destory(bytebuf);
                bytebuf = NULL;
            }
        } else { //from jar
            bytebuf = bytebuf_create(16);
            iret = zip_loadfile(utf8_cstr(pClassPath), utf8_cstr(path), bytebuf);
            //回收
            if (iret != 0) {
                bytebuf_destory(bytebuf);
                bytebuf = NULL;
            } else {
                break;
            }
        }
    }
    return bytebuf;
}

void init_jni_func_table(MiniJVM *jvm) {
    jnienv.data_type_bytes = (s32 *) &DATA_TYPE_BYTES;
    jnienv.native_reg_lib = native_reg_lib;
    jnienv.native_remove_lib = native_remove_lib;
    jnienv.push_entry = push_entry_jni;
    jnienv.push_int = push_int_jni;
    jnienv.push_long = push_long_jni;
    jnienv.push_double = push_double_jni;
    jnienv.push_float = push_float_jni;
    jnienv.push_ref = push_ref_jni;
    jnienv.pop_ref = pop_ref_jni;
    jnienv.pop_int = pop_int_jni;
    jnienv.pop_long = pop_long_jni;
    jnienv.pop_double = pop_double_jni;
    jnienv.pop_float = pop_float_jni;
    jnienv.pop_entry = pop_entry_jni;
    jnienv.pop_empty = pop_empty_jni;
    jnienv.entry_2_int = entry_2_int_jni;
    jnienv.peek_entry = peek_entry_jni;
    jnienv.entry_2_long = entry_2_long_jni;
    jnienv.entry_2_refer = entry_2_refer_jni;
    jnienv.localvar_setRefer = localvar_setRefer_jni;
    jnienv.localvar_setInt = localvar_setInt_jni;
    jnienv.localvar_getRefer = localvar_getRefer_jni;
    jnienv.localvar_getInt = localvar_getInt_jni;
    jnienv.localvar_getLong_2slot = localvar_getLong_2slot_jni;
    jnienv.localvar_setLong_2slot = localvar_setLong_2slot_jni;
    jnienv.jthread_block_enter = jthread_block_enter;
    jnienv.jthread_block_exit = jthread_block_exit;
    jnienv.utf8_create = utf8_create;
    jnienv.utf8_create_part_c = utf8_create_part_c;
    jnienv.utf8_cstr = utf8_cstr;
    jnienv.utf8_destory = utf8_destory;
    jnienv.jstring_2_utf8 = jstring_2_utf8;
    jnienv.jstring_create = jstring_create;
    jnienv.jstring_create_cstr = jstring_create_cstr;
    jnienv.cstringarr_create = cstringarr_create;
    jnienv.cstringarr_destory = cstringarr_destory;
    jnienv.referarr_create = referarr_create;
    jnienv.referarr_destory = referarr_destory;
    jnienv.referarr_2_jlongarr = referarr_2_jlongarr;
    jnienv.jvm_calloc = jvm_calloc;
    jnienv.jvm_malloc = jvm_malloc;
    jnienv.jvm_free = jvm_free;
    jnienv.jvm_realloc = jvm_realloc;
    jnienv.execute_method = execute_method;
    jnienv.find_methodInfo_by_name = find_methodInfo_by_name;
    jnienv.jarray_create_by_type_name = jarray_create_by_type_name;
    jnienv.jarray_create_by_type_index = jarray_create_by_type_index;
    jnienv.jarray_set_field = jarray_set_field;
    jnienv.jarray_get_field = jarray_get_field;
    jnienv.instance_hold_to_thread = instance_hold_to_thread;
    jnienv.instance_release_from_thread = instance_release_from_thread;
    jnienv.print_exception = print_exception;
    jnienv.garbage_refer_hold = gc_obj_hold;
    jnienv.garbage_refer_release = gc_obj_release;
    jnienv.runtime_create = runtime_create;
    jnienv.runtime_destory = runtime_destory;
    jnienv.getLastSon = getLastSon;
    jnienv.thread_boundle = thread_boundle;
    jnienv.thread_unboundle = thread_unboundle;
    jnienv.thrd_current = thrd_current;
    jnienv.pairlist_create = pairlist_create;
    jnienv.pairlist_get = pairlist_get;
    jnienv.pairlist_put = pairlist_put;
    jnienv.jthread_get_daemon_value = jthread_get_daemon_value;
    jnienv.jthread_set_daemon_value = jthread_set_daemon_value;
    jnienv.get_jvm_state = get_jvm_state;
}
