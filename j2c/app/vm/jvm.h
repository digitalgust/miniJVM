#ifndef RUNNER_H
#define RUNNER_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stddef.h>

#include "jvmtype.h"
#include "utf8string.h"
#include "bytebuf.h"
//=====================================================================

#define PRJ_DEBUG_LEV 2
#define PRJ_DEBUG_GARBAGE_DUMP 0


#define NANO_2_SEC_SCALE 1000000000
#define NANO_2_MILLS_SCALE 1000000
#define MILL_2_SEC_SCALE 1000


enum {
    INS_TYPE_UNINIT = 0,
    INS_TYPE_CLASS,
    INS_TYPE_OBJECT,
    INS_TYPE_ARRAY,
};
//acces flag
enum {
    ACC_PUBLIC = 0x0001,
    ACC_PRIVATE = 0x0002,
    ACC_PROTECTED = 0x0004,
    ACC_STATIC = 0x0008,
    ACC_FINAL = 0x0010,
    ACC_SYNCHRONIZED = 0x0020,
    ACC_VOLATILE = 0x0040,
    ACC_TRANSIENT = 0x0080,
    ACC_NATIVE = 0x0100,
    ACC_INTERFACE = 0x0200,
    ACC_ABSTRACT = 0x0400,
    ACC_STRICT = 0x0800,
    ACC_SYNTHETIC = 0x1000,
};

enum {
    THREAD_STATUS_NEW,
    THREAD_STATUS_RUNNABLE,
    THREAD_STATUS_RUNNING,
    THREAD_STATUS_DEAD,
    THREAD_STATUS_BLOCKED,
};

enum {
    CLASS_STATUS_RAW = 0,
    CLASS_STATUS_LOADED,
    CLASS_STATUS_PREPARING,
    CLASS_STATUS_PREPARED,
    CLASS_STATUS_CLINITING,
    CLASS_STATUS_CLINITED,
};


enum {
    DATATYPE_BOOLEAN = 4,
    DATATYPE_JCHAR = 5,
    DATATYPE_FLOAT = 6,
    DATATYPE_DOUBLE = 7,
    DATATYPE_BYTE = 8,
    DATATYPE_SHORT = 9,
    DATATYPE_INT = 10,
    DATATYPE_LONG = 11,
    DATATYPE_REFERENCE = 12,
    DATATYPE_ARRAY = 13,
    DATATYPE_RETURNADDRESS = 14,
    DATATYPE_COUNT,
};

typedef union _Int2Float {
    s32 i;
    f32 f;
    struct {
        c8 c0;
        c8 c1;
        c8 c2;
        c8 c3;
    };
} Int2Float;

extern tss_t TLS_KEY_JTHREADRUNTIME;
extern tss_t TLS_KEY_UTF8STR_CACHE;


typedef struct _JThreadRuntime JThreadRuntime;
typedef struct _VMTable VMTable;
typedef struct _LabelTable LabelTable;
typedef struct _ExceptionItem ExceptionItem;
typedef struct _ExceptionTable ExceptionTable;
typedef struct _UtfRaw UtfRaw;
typedef struct _FieldRaw FieldRaw;
typedef struct _MethodRaw MethodRaw;
typedef struct _ClassRaw ClassRaw;
typedef union _StackItem StackItem;
typedef union _ParaItem ParaItem;
typedef union _RStackItem RStackItem;
typedef struct _ThreadLock ThreadLock;
typedef struct _InstProp InstProp;
typedef struct _MethodInfo MethodInfo;
typedef struct _FieldInfo FieldInfo;
typedef struct _JClass JClass;
typedef struct _JObject JObject;
typedef struct _JObject JArray;
typedef struct _StackFrame StackFrame;
typedef struct _GcCollectorType GcCollector;
typedef struct _PeerClassLoader PeerClassLoader;
typedef struct _Jvm Jvm;
typedef struct _ProCache ProCache;

//
extern c8 const *STR_JAVA_LANG_OBJECT;
extern c8 const *STR_JAVA_LANG_CLASS;
extern c8 const *STR_JAVA_LANG_STRING;
extern c8 const *STR_JAVA_LANG_THREAD;
extern c8 const *STR_JAVA_LANG_INTEGER;
extern c8 const *STR_JAVA_IO_EOF_EXCEPTION;
extern c8 const *STR_JAVA_IO_IO_EXCEPTION;
extern c8 const *STR_JAVA_IO_FILE_NOT_FOUND_EXCEPTION;
extern c8 const *STR_JAVA_LANG_OUT_OF_MEMORY_ERROR;
extern c8 const *STR_JAVA_LANG_VIRTUAL_MACHINE_ERROR;
extern c8 const *STR_JAVA_LANG_NO_CLASS_DEF_FOUND_ERROR;
extern c8 const *STR_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION;
extern c8 const *STR_JAVA_LANG_ARITHMETIC_EXCEPTION;
extern c8 const *STR_JAVA_LANG_NULL_POINTER_EXCEPTION;
extern c8 const *STR_JAVA_LANG_NO_SUCH_METHOD_EXCEPTION;
extern c8 const *STR_JAVA_LANG_NO_SUCH_FIELD_EXCEPTION;
extern c8 const *STR_JAVA_LANG_ILLEGAL_ARGUMENT_EXCEPTION;
extern c8 const *STR_JAVA_LANG_CLASS_CAST_EXCEPTION;
extern c8 const *STR_JAVA_LANG_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION;
extern c8 const *STR_JAVA_LANG_INSTANTIATION_EXCEPTION;
extern c8 const *STR_JAVA_LANG_STACKTRACEELEMENT;
extern c8 const *STR_ORG_MINI_REFLECT_LAUNCHER;
extern c8 const *STR_CLASS_JAVA_LANG_REF_WEAKREFERENCE;

//=================================  raw ====================================

struct _VMTable {
    s32 class_index;
    s32 size;
    __refer *func_ptr;
};

struct _LabelTable {
    s32 size;
    __refer *labels;
};

struct _ExceptionItem {
    s32 startIdxOfBC;
    s32 endIdxOfBC;
    s32 exceptionClassName;
};

struct _ExceptionTable {
    s32 size;
    ExceptionItem *exception;
};


struct _UtfRaw {
    s32 utf8_size;
    s32 utf16_size;
    c8 *str;
    Utf8String *ustr;
    JObject *jstr;
};

struct _FieldRaw {//name, signature_name, class_name, access, offset_ins, static_ptr
    s32 name;
    s32 desc_name;
    s32 signature_name;
    s32 class_name;
    u16 access;
    u16 offset_ins;
};

typedef void (*func_bridge)(JThreadRuntime *runtime, __refer ins, ParaItem *para, ParaItem *ret);

struct _MethodRaw {
    s32 index;
    s32 name;
    s32 desc_name;
    s32 signature_name;
    s32 class_name;
    s32 bytecode;
    u16 access;
    s16 max_stack;
    s16 max_local;
    __refer func_ptr;
    func_bridge bridge_ptr;
    ExceptionTable *extable;
};

typedef void (*finalize_func_t)(JThreadRuntime *runtime, __refer jobj);


struct _ClassRaw {
    s32 index;
    JClass *clazz;
    s32 name; //g_strings[] index ,to get "java/lang/String"
    s32 super_name;//g_strings[] index ,to get "java/lang/String"
    s32 source_name;//g_strings[] index ,to get "String.java"
    s32 signature_name;//g_strings[] index ,to get "<E:Ljava/lang/Object;>Ljava/lang/Object;"
    u16 acc_flag;//
    s32 interface_name_arr;//g_strings[] index to get "3,5,6,7" ,  parsed element as g_strings[] index
    s32 method_arr;//g_strings[] index "6,7,8" , parsed element as g_methods[]
    s32 field_arr;//g_strings[] index "6,7,8" , parsed element as g_fields[] index
    s32 depd_arr;//g_strings[] index "6,7,8" , parsed element as dependent class name index
    s32 ins_size;
    s32 vmtable_size;
    VMTable *vmtable;
    __refer finalize_method;
    __refer static_fields;
};

typedef void (*class_clinit_func_t)(__refer);


//=================================  java instance ====================================


struct _ThreadLock {
    cnd_t thread_cond;
    mtx_t mutex_lock; //互斥锁
};

struct _InstProp {
    InstProp *next;
    InstProp *tmp_next;
    JClass *clazz;
    ThreadLock *thread_lock;
    union {
        __refer members;
        __refer *as_obj_arr;
        s64 *as_s64_arr;
        s32 *as_s32_arr;
        s16 *as_s16_arr;
        u16 *as_u16_arr;
        c8 *as_c8_arr;
        c8 *as_s8_arr;
        f32 *as_f32_arr;
        f64 *as_f64_arr;
    };
    s32 heap_size;
    s32 arr_length;
    u8 arr_type;
    u8 garbage_mark;
    u8 garbage_reg;
    u8 type;
    u8 is_weakreference;
    u8 is_finalized;
};


struct _MethodInfo {
    MethodRaw *raw;
    JClass *clazz;
    Utf8String *name;
    Utf8String *desc;
    Utf8String *signature;
    Utf8String *paratype;
    Utf8String *returntype;
    __refer func_ptr;
};

struct _FieldInfo {
    FieldRaw *raw;
    JClass *clazz;
    Utf8String *name;
    Utf8String *desc;
    Utf8String *signature;
    u16 offset_ins;
    u8 is_static;
    u8 is_private;
    u8 is_refer;
    u8 is_ref_target;
};

struct _JClass {
    InstProp prop;
    ClassRaw *raw;
    Utf8String *name;
    Utf8String *source_name;
    Utf8String *signature;
    ArrayList *methods;
    ArrayList *fields;
    ArrayList *interfaces;
    ArrayList *dependent_classes;
    JClass *superclass;
    JClass *array_cell_class;
    JObject *ins_of_Class;
    JObject *jclass_loader;
    s32 status;
    u8 array_cell_type;
    u8 primitive;
    u8 is_weakref;
};

struct _JObject {
    InstProp prop;
    VMTable *vm_table;
};


union _StackItem {
    s32 i;
    u32 u;
    s64 j;
    f32 f;
    f64 d;
};
union _RStackItem {
    JObject *ins;
    __refer obj;
};


struct _StackFrame {
//    JThreadRuntime *runtime;
    //MethodInfo *methodInfo;
    StackFrame *next;
    const RStackItem *rstack;
    const s32 *spPtr;
    const RStackItem *rlocal;

    //
    s32 bytecodeIndex;
    s32 lineNo;

    s32 methodRawIndex;
};

typedef void (*jthread_run_t)(__refer, __refer);

struct _JThreadRuntime {
    JObject *jthread;
    JObject *context_classloader;
    StackFrame *tail;
    StackFrame *cache;//cache the stackframe object
    MethodRaw *exec;
    thrd_t thread;

    //gc
    InstProp *tmp_holder;//for jni hold java object
    InstProp *objs_header;//link to new instance, until garbage accept
    InstProp *objs_tailer;//link to last instance, until garbage accept
    s64 objs_heap_of_thread;// heap use for objs_header, if translate to gc ,the var need clear to 0

    u8 volatile suspend_count;//for jdwp suspend ,>0 suspend, ==0 resume
    u8 volatile no_pause;  //can't pause when clinit
    u8 volatile thread_status;
    u8 volatile is_suspend;
    u8 volatile is_interrupt;


    spinlock_t lock;
    JObject *pack;
    u8 volatile is_unparked;

    //exception
    JObject *exception;
    ArrayList *stacktrack;  //save methodrawindex, the pos 0 is the throw point
    ArrayList *lineNo;  //save methodrawindex, the pos 0 is the throw point
};

//每个线程一个回收站，线程多了就是灾难
struct _GcCollectorType {
    //
    Hashtable *objs_holder; //法外之地，防回收的持有器，放入其中的对象及其引用的其他对象不会被回收
    InstProp *header, *tmp_header, *tmp_tailer;
    s64 obj_count;
    //
    s64 garbage_collect_period_ms;
    s64 max_heap_size;
    //
    thrd_t _garbage_thread;//垃圾回收线程
    ThreadLock garbagelock;

    spinlock_t lock;
    //
    ArrayList *runtime_refer_copy;
    //
    s64 obj_heap_size;
    s64 lastgc;//last gc at mills

    s64 _garbage_count;
    u8 _garbage_thread_status;
    u8 mark_cnt;
    u8 isgc;
    s16 exit_flag;
    s16 exit_code;
};

struct _PeerClassLoader {
    Jvm *jvm;
    JObject *jloader;
    JObject *parent;
    ArrayList *classpath;
    //
};

struct _Jvm {
    Hashtable *classes;
    ArrayList *thread_list;
    GcCollector *collector;
    Hashtable *table_jstring_const;
    Hashtable *sys_prop;
    ArrayList *classloaders;
    JObject *shutdown_hook;
    Utf8String *startup_dir;
};

struct _ProCache {
    ClassRaw *java_lang_object_raw;
    ClassRaw *java_lang_string_raw;
    ClassRaw *java_lang_thread_raw;
    ClassRaw *java_lang_class_raw;
    MethodRaw *java_lang_string_init_C_raw;
};

union _ParaItem {
    s32 i;
    u32 u;
    s64 j;
    f32 f;
    f64 d;
    JObject *ins;
    __refer obj;
};

extern Jvm *g_jvm;
extern ProCache g_procache;
//=================================  extern ====================================

extern UtfRaw g_strings[];
extern FieldRaw g_fields[];
extern MethodRaw g_methods[];
extern ClassRaw g_classes[];

extern const s32 g_strings_count;
extern const s32 g_fields_count;
extern const s32 g_methods_count;
extern const s32 g_classes_count;
extern s32 data_type_bytes[];

//=================================  inline ====================================

static inline __refer find_method(VMTable *table, s32 class_idx, s32 method_idx) {
    while (1) {
        if (table->class_index == class_idx)return table->func_ptr[method_idx];
        table++;
    }
}
//=====================================================================

Jvm *jvm_create(c8 *bootclasspath, c8 *classpath);

void jvm_destroy(Jvm *jvm);

s32 jvm_run_main(Utf8String *mainClass, Utf8String *args);

PeerClassLoader *classloader_create_with_path(Jvm *jvm, c8 *path);

JThreadRuntime *jthreadruntime_create();

void jthreadruntime_destroy(__refer jthreadruntime);

void jthreadruntime_get_stacktrack(JThreadRuntime *runtime, Utf8String *ustr);

JObject *new_jthread(JThreadRuntime *runtime);

s32 jthread_prepar(JThreadRuntime *runtime);

s32 jthread_run(__refer p);

void jthreadlock_destory(InstProp *mb);

s32 jthread_suspend(JThreadRuntime *runtime);

s32 jthread_resume(JThreadRuntime *runtime);

int jvm_printf(const char *format, ...);

ClassRaw *find_classraw(c8 const *className);

MethodRaw *get_methodraw_by_index(s32 index);

MethodRaw *find_methodraw(c8 const *className, c8 const *methodName, c8 const *signature);

MethodInfo *get_methodinfo_by_rawindex(s32 methodRawIndex);

MethodInfo *find_methodInfo_by_name(c8 const *clsName, c8 const *methodName, c8 const *methodType);

Utf8String *get_utf8str(UtfRaw *utfraw);

Utf8String *get_utf8str_by_utfraw_index(s32 index);

s32 find_global_string_index(c8 const *str);

void classes_put(JClass *clazz);

JClass *classes_get(Utf8String *className);

JClass *classes_get_c(c8 const *className);

JClass *get_class_by_name(Utf8String *name);

JClass *get_class_by_name_c(c8 const *name);

JClass *get_class_by_nameIndex(s32 index);

void class_prepar(JClass *clazz);

void class_clinit(JThreadRuntime *runtime, Utf8String *className);

void class_load(Utf8String *className);

StackFrame *stackframe_create();

void stackframe_destroy(StackFrame *stackframe);

s32 utf8_2_unicode(Utf8String *ustr, u16 *arr);

int unicode_2_utf8(u16 *jchar_arr, Utf8String *ustr, s32 u16arr_len);

JThreadRuntime *jthread_bound(JThreadRuntime *runtime);

void jthread_unbound(JThreadRuntime *runtime);

void jthread_lock(JThreadRuntime *runtime, JObject *jobj);

void jthread_unlock(JThreadRuntime *runtime, JObject *jobj);

JThreadRuntime *jthread_start(JObject *jthread);

void thread_lock_init(ThreadLock *lock);

s32 jthread_sleep(JThreadRuntime *runtime, s64 ms);

s32 jthread_yield();

s32 jthread_notify(InstProp *mb);

s32 jthread_notifyAll(InstProp *mb);

s32 jthread_waitTime(InstProp *mb, JThreadRuntime *runtime, s64 waitms);

u8 jthread_block_enter(JThreadRuntime *runtime);

void jthread_block_exit(JThreadRuntime *runtime, u8 state);

void thread_lock_dispose(ThreadLock *lock);

s64 threadSleep(s64 ms);

s64 currentTimeMillis();

s64 nanoTime();

JClass *getSuperClass(JClass *clazz);

s32 instance_of_class_name(InstProp *ins, s32 classNameIndex);

s32 instance_of(InstProp *ins, JClass *clazz);

s32 assignable_from(JClass *clazzSuper, JClass *clazzSon);

s32 getDataTypeIndex(c8 ch);

c8 getDataTypeTag(s32 index);

c8 getDataTypeTagByName(Utf8String *name);

c8 *getDataTypeFullName(c8 ch);

s32 isDataReferByTag(c8 c);

s32 isDataReferByIndex(s32 index);

s32 isData8ByteByTag(c8 c);

s32 parseMethodPara(Utf8String *methodType, Utf8String *out);

JClass *array_class_create_get(Utf8String *name);

JClass *primitive_class_create_get(JThreadRuntime *runtime, Utf8String *ustr);

void jclass_destroy(JClass *clazz);

JObject *ins_of_Class_create_get(JThreadRuntime *runtime, JClass *clazz);

JObject *jclassloader_get_with_init(JThreadRuntime *runtime);

ByteBuf *load_file_from_classpath(Utf8String *path);

JArray *multi_array_create(JThreadRuntime *runtime, s32 *dimm, s32 dimm_count, JClass *clazz);  // create array  [[[Ljava/lang/Object;  para: [3,2,1],3,8

JArray *multi_array_create_by_typename(JThreadRuntime *runtime, s32 *dimm, s32 dimm_count, c8 const *type_name);

s32 jarray_destroy(JArray *arr);

s32 instance_of_classname_index(JObject *jobj, s32 classNameIdx);

s32 checkcast(JObject *jobj, s32 classNameIdx);

s32 find_exception_handler_index(JThreadRuntime *runtime);

void throw_exception(JThreadRuntime *runtime, JObject *jobj);

JObject *construct_and_throw_exception(JThreadRuntime *runtime, s32 classrawIndex, s32 bytecodeIndex, s32 lineNo);

//StackFrame *method_enter(JThreadRuntime *runtime, s32 methodRawIndex, LabelTable *labtable, RStackItem *stack, RStackItem *local, s32 *spPtr);
//
//void method_exit(JThreadRuntime *runtime);

s32 exception_check_print(JThreadRuntime *runtime);

JObject *new_instance_with_classraw_index(JThreadRuntime *runtime, s32 classIndex);

JObject *new_instance_with_classraw(JThreadRuntime *runtime, ClassRaw *raw);

JObject *new_instance_with_nameindex(JThreadRuntime *runtime, s32 classNameIndex);

JObject *new_instance_with_name(JThreadRuntime *runtime, c8 const *className);

JObject *new_instance_with_class(JThreadRuntime *runtime, JClass *clazz);

JObject *new_instance_with_classraw_index_and_init(JThreadRuntime *runtime, s32 classIndex);

void instance_init(JThreadRuntime *runtime, JObject *ins);

InstProp *instance_copy(JThreadRuntime *runtime, InstProp *src, s32 deep_copy);

s32 jobject_destroy(JObject *ins);

JObject *construct_string_with_utfraw_index(JThreadRuntime *runtime, s32 utfIndex);

JObject *construct_string_with_cstr(JThreadRuntime *runtime, c8 const *str);

JObject *construct_string_with_ustr(JThreadRuntime *runtime, Utf8String *str);

JObject *construct_string_with_cstr_and_size(JThreadRuntime *runtime, c8 const *str, s32 size);

s32 check_suspend_and_pause(JThreadRuntime *runtime);

void sys_properties_set_c(c8 *key, c8 *val);

s32 sys_properties_load();
//=====================================================================
//require jni implementation

void jthread_set_stackFrame(JObject *jobj, JThreadRuntime *runtime);

JThreadRuntime *jthread_get_stackFrame(JObject *jobj);

void jclass_set_classHandle(JObject *jobj, JClass *clazz);

void jclass_set_classLoader(JObject *jobj, JObject *jloader);

void jclass_init_insOfClass(JThreadRuntime *runtime, JObject *jobj);

void jstring_debug_print(JObject *jobj, c8 *appendix);

JObject *weakreference_get_target(JThreadRuntime *runtime, JObject *jobj);

void weakref_vmreferenceenqueue(JThreadRuntime *runtime, JObject *jobj);

JObject *launcher_get_systemClassLoader(JThreadRuntime *runtime);

s32 thread_is_daemon(JObject *jobj);
//=====================================================================

static inline StackFrame *method_enter(JThreadRuntime *runtime, s32 methodRawIndex, const RStackItem *stack, const RStackItem *local, const s32 *spPtr) {

    StackFrame *cur;
    if (runtime->cache) {
        cur = runtime->cache;
        runtime->cache = cur->next;
    } else {
        cur = stackframe_create();
    }
    cur->next = runtime->tail;
    runtime->tail = cur;
    cur->methodRawIndex = methodRawIndex;
    cur->rstack = stack;
    cur->rlocal = local;
    cur->spPtr = spPtr;

#if PRJ_DEBUG_LEV > 6
    //debug print
     StackFrame *next = cur;
    while (next) {
        next = next->next;
        printf(" ");
    }
    printf("enter %d %s.%s\n", cur->methodRawIndex,
           g_strings[g_methods[cur->methodRawIndex].class_name].str,
           g_strings[g_methods[cur->methodRawIndex].name].str);
#endif
    return cur;
}

static inline void method_exit(JThreadRuntime *runtime) {

    StackFrame *cur = runtime->tail;
    //native no stackframe, so non native method exit to next
#if PRJ_DEBUG_LEV > 6
    //debug print
     StackFrame *next = cur;
    while (next) {
        next = next->next;
        printf(" ");
    }
    printf("exit %d %s.%s\n", cur->methodRawIndex,
           g_strings[g_methods[cur->methodRawIndex].class_name].str,
           g_strings[g_methods[cur->methodRawIndex].name].str);
#endif

    //
    runtime->tail = cur->next;
    cur->next = runtime->cache;
    runtime->cache = cur;
}

#ifdef __cplusplus
}
#endif

#endif //RUNNER_H