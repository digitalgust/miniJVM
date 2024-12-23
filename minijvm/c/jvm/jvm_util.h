//
// Created by gust on 2017/8/8.
//

#ifndef MINI_JVM_UTIL_H
#define MINI_JVM_UTIL_H

#ifdef __cplusplus
extern "C" {
#endif

#include "jvm.h"
#include "jdwp.h"


#define NANO_2_SEC_SCALE 1000000000
#define NANO_2_MILLS_SCALE 1000000
#define MILL_2_SEC_SCALE 1000

//======================= utils =============================
s32 isDir(Utf8String *path);

s32 utf8_2_unicode(Utf8String *ustr, u16 *arr);

s32 unicode_2_utf8(u16 *jchar_arr, Utf8String *ustr, s32 totalSize);

void swap_endian_little_big(u8 *ptr, s32 size);

s32 instance_base_size();

s32 getDataTypeIndex(c8 ch);

c8 *getDataTypeFullName(c8 ch);

u8 getDataTypeTagByName(Utf8String *name);

s32 isDataReferByTag(c8 c);

s32 isData8ByteByTag(c8 c);

s32 isDataReferByIndex(s32 index);

u8 getDataTypeTag(s32 index);

s64 currentTimeMillis(void);

s64 nanoTime(void);

s64 threadSleep(s64 ms);

s32 sys_properties_load(MiniJVM *jvm);

void sys_properties_dispose(MiniJVM *jvm);

void sys_properties_set_c(MiniJVM *jvm, c8 const *key, c8 const *val);

void instance_release_from_thread(Instance *ref, Runtime *runtime);

void instance_hold_to_thread(Instance *ins, Runtime *runtime);

s32 jvm_printf(const c8 *, ...);

void invoke_deepth(Runtime *runtime);

void printDumpOfClasses(void);


Instance *exception_create(s32 exception_type, Runtime *runtime);

Instance *exception_create_str(s32 exception_type, Runtime *runtime, c8 const *errmsg);

void exception_throw(s32 exception_type, Runtime *runtime, c8 const *errmsg);

Instance *method_type_create(Runtime *runtime, Instance *jloader, Utf8String *desc);

Instance *method_handle_create(Runtime *runtime, MethodInfo *mi, s32 kind);

Instance *method_handles_lookup_create(Runtime *runtime, JClass *caller);


/**
 * get instance field value address
 * @param ins ins
 * @param fi fi
 * @return addr
 */
static inline c8 *getInstanceFieldPtr(Instance *ins, FieldInfo *fi) {
    return &(ins->obj_fields[fi->offset_instance]);
}

static inline c8 *getInstanceFieldPtrByOffset(Instance *ins, u16 offset) {
    return &(ins->obj_fields[offset]);
}

static inline c8 *getStaticFieldPtr(FieldInfo *fi) {
    return &(fi->_this_class->field_static[fi->offset]);
}


static inline void setFieldInt(c8 *ptr, s32 v) {
    *((s32 *) ptr) = v;
}

static inline void setFieldRefer(c8 *ptr, __refer v) {
    *((__refer *) ptr) = v;
}

static inline void setFieldLong(c8 *ptr, s64 v) {
    *((s64 *) ptr) = v;
}

static inline void setFieldShort(c8 *ptr, s16 v) {
    *((s16 *) ptr) = v;
}

static inline void setFieldByte(c8 *ptr, s8 v) {
    *((s8 *) ptr) = v;
}

static inline void setFieldDouble(c8 *ptr, f64 v) {
    *((f64 *) ptr) = v;
}

static inline void setFieldFloat(c8 *ptr, f32 v) {
    *((f32 *) ptr) = v;
}

static inline s32 getFieldInt(c8 *ptr) {
    return *((s32 *) ptr);
}

static inline __refer getFieldRefer(c8 *ptr) {
    return *((__refer *) ptr);
}

static inline s16 getFieldShort(c8 *ptr) {
    return *((s16 *) ptr);
}

static inline u16 getFieldChar(c8 *ptr) {
    return *((u16 *) ptr);
}

static inline s8 getFieldByte(c8 *ptr) {
    return *((s8 *) ptr);
}

static inline s64 getFieldLong(c8 *ptr) {
    return *((s64 *) ptr);
}

static inline f32 getFieldFloat(c8 *ptr) {
    return *((f32 *) ptr);
}


static inline f64 getFieldDouble(c8 *ptr) {
    return *((f64 *) ptr);
}

s32 getLineNumByIndex(CodeAttribute *ca, s32 offset);

s32 _loadFileContents(c8 const *file, ByteBuf *buf);

ByteBuf *load_file_from_classpath(PeerClassLoader *cloader, Utf8String *path);


//===============================    实例化 java.lang.Class  ==================================

Instance *insOfJavaLangClass_create_get(Runtime *runtime, JClass *clazz);

void insOfJavaLangClass_set_classHandle(Runtime *runtime, Instance *insOfJavaLangClass, JClass *handle);

JClass *insOfJavaLangClass_get_classHandle(Runtime *runtime, Instance *insOfJavaLangClass);


////======================= jstring =============================

Instance *jstring_create(Utf8String *src, Runtime *runtime);

Instance *jstring_create_cstr(c8 const *cstr, Runtime *runtime);

void jstring_set_count(Instance *jstr, s32 count, Runtime *runtime);

s32 jstring_get_count(Instance *jstr, Runtime *runtime);

s32 jstring_get_offset(Instance *jstr, Runtime *runtime);

c8 *jstring_get_value_ptr(Instance *jstr, Runtime *runtime);

Instance *jstring_get_value_array(Instance *jstr, Runtime *runtime);

u16 jstring_char_at(Instance *jstr, s32 index, Runtime *runtime);

s32 jstring_index_of(Instance *jstr, u16 ch, s32 startAt, Runtime *runtime);

s32 jstring_equals(Instance *jstr1, Instance *jstr2, Runtime *runtime);

s32 jstring_2_utf8(Instance *jstr, Utf8String *utf8, Runtime *runtime);

CStringArr *cstringarr_create(Instance *jstr_arr);

void cstringarr_destory(CStringArr *);

void referarr_destory(CStringArr *ref_arr);

ReferArr *referarr_create(Instance *jobj_arr);

void referarr_2_jlongarr(ReferArr *ref_arr, Instance *jlong_arr);

////======================= thread =============================


void threadlist_add(Runtime *r);

void threadlist_remove(Runtime *r);

Runtime *threadlist_get(MiniJVM *jvm, s32 i);

s32 threadlist_count_none_daemon(MiniJVM *jvm);

s64 threadlist_sum_heap(MiniJVM *jvm);

void thread_stop_all(MiniJVM *jvm);


s32 vm_share_trylock(MiniJVM *jvm);

void vm_share_lock(MiniJVM *jvm);

void vm_share_unlock(MiniJVM *jvm);

void vm_share_wait(MiniJVM *jvm);

void vm_share_timedwait(MiniJVM *jvm, s64 ms);

void vm_share_notify(MiniJVM *jvm);

void vm_share_notifyall(MiniJVM *jvm);


JavaThreadInfo *threadinfo_create(void);

void threadinfo_destory(JavaThreadInfo *threadInfo);

s32 jthread_init(MiniJVM *jvm, Instance *jthread);

s32 jthread_dispose(Instance *jthread, Runtime *runtime);

s32 jthread_run(void *para);

thrd_t jthread_start(Instance *ins, Runtime *parent);

s32 jthread_get_daemon_value(Instance *ins, Runtime *runtime);

__refer jthread_get_stackframe_value(MiniJVM *jvm, Instance *ins);

void jthread_set_stackframe_value(MiniJVM *jvm, Instance *ins, void *val);

__refer jthread_get_name_value(MiniJVM *jvm, Instance *ins);

void jthreadlock_create(Runtime *runtime, MemoryBlock *mb);

void jthreadlock_destory(MemoryBlock *mb);

s32 jthread_lock(MemoryBlock *mb, Runtime *runtime);

s32 jthread_unlock(MemoryBlock *mb, Runtime *runtime);

s32 jthread_notify(MemoryBlock *mb, Runtime *runtime);

s32 jthread_notifyAll(MemoryBlock *mb, Runtime *runtime);

s32 jthread_waitTime(MemoryBlock *mb, Runtime *runtime, s64 waitms);

s32 jthread_wakeup(Runtime *runtime);

s32 jthread_sleep(Runtime *runtime, s64 ms);

s32 jthread_yield(Runtime *runtime);

s32 jthread_resume(Runtime *runtime);

s32 jthread_suspend(Runtime *runtime);

void jthread_block_exit(Runtime *runtime);

void jthread_block_enter(Runtime *runtime);

s32 check_throw_interruptexception(Runtime *runtime);

s32 check_suspend_and_pause(Runtime *runtime);

void thread_lock_dispose(ThreadLock *lock);

void thread_lock_init(ThreadLock *lock);


static inline Runtime *_runtime_alloc() {
    Runtime *runtime = (Runtime *) jvm_calloc(sizeof(Runtime));
    runtime->jnienv = &jnienv;
    return runtime;
}

/**
 * runtime 的创建和销毁会极大影响性能，因此对其进行缓存
 * @param parent runtime of parent
 * @return runtime
 */
static inline Runtime *runtime_create_inl(Runtime *parent) {

    Runtime *runtime;

    if (!parent) {
        runtime = _runtime_alloc();
        runtime->stack = stack_create(MAX_STACK_SIZE_DEFAULT);
        runtime->thrd_info = threadinfo_create();
        runtime->thrd_info->top_runtime = runtime;
    } else {
        Runtime *top_runtime = parent->thrd_info->top_runtime;
        runtime = top_runtime->runtime_pool_header;
        if (runtime) {
            top_runtime->runtime_pool_header = runtime->next;
        } else {
            runtime = _runtime_alloc();
            runtime->jvm = parent->jvm;
            runtime->stack = parent->stack;
            runtime->thrd_info = parent->thrd_info;
        }
        runtime->parent = parent;
        parent->son = runtime;
    }
    return runtime;
}


static inline void runtime_destory_inl(Runtime *runtime) {
    Runtime *top_runtime = runtime->thrd_info->top_runtime;
    if (top_runtime != runtime) {
        runtime->next = top_runtime->runtime_pool_header;
        top_runtime->runtime_pool_header = runtime;
    } else {
        stack_destory(runtime->stack);
        threadinfo_destory(runtime->thrd_info);

        Runtime *next = top_runtime->runtime_pool_header;
        while (next) {
            Runtime *r = next;
            next = r->next;
            jvm_free(r);
        }
        runtime->runtime_pool_header = NULL;
        jvm_free(runtime);
    }
}

static inline void runtime_clear_stacktrack(Runtime *runtime) {
    arraylist_clear(runtime->thrd_info->stacktrack);
    arraylist_clear(runtime->thrd_info->lineNo);
}

////======================= array =============================

Instance *jarray_create_by_class(Runtime *runtime, s32 count, JClass *clazz);

Instance *jarray_create_by_type_name(Runtime *runtime, s32 count, Utf8String *name, Instance *jloader);

Instance *jarray_create_by_type_index(Runtime *runtime, s32 count, s32 typeIdx);

JClass *array_class_get_by_name(Runtime *runtime, Instance *jloader, Utf8String *name);

JClass *array_class_get_by_typetag(Runtime *runtime, Utf8String *tag);

JClass *array_class_create_get(Runtime *runtime, Instance *jloader, Utf8String *desc);

JClass *array_class_get_by_index(Runtime *runtime, s32 typeIdx);

s32 jarray_destory(Instance *arr);

Instance *jarray_multi_create(Runtime *runtime, s32 *dim, s32 dim_size, Utf8String *desc, s32 deep);

void jarray_set_field(Instance *arr, s32 index, s64 val);

s64 jarray_get_field(Instance *arr, s32 index);

c8 *getFieldPtr_byName_c(Instance *instance, c8 const *pclassName, c8 const *pfieldName, c8 const *pfieldType, Runtime *runtime);

c8 *getFieldPtr_byName(Instance *instance, Utf8String *clsName, Utf8String *fieldName, Utf8String *fieldType, Runtime *runtime);

JClass *classes_get_c(MiniJVM *jvm, Instance *jloader, c8 const *clsName);

JClass *classes_get(MiniJVM *jvm, Instance *jloader, Utf8String *clsName);

JClass *classes_load_get_without_resolve(Instance *jloader, Utf8String *ustr, Runtime *runtime);

JClass *classes_load_get_c(Instance *jloader, c8 const *pclassName, Runtime *runtime);

s32 classes_put(MiniJVM *jvm, JClass *clazz);

JClass *classes_load_get(Instance *jloader, Utf8String *pclassName, Runtime *runtime);

JClass *primitive_class_create_get(Runtime *runtime, Utf8String *ustr);

s32 classes_loaded_count_unsafe(MiniJVM *jvm);

s32 classes_remove(MiniJVM *jvm, JClass *clazz);


#ifdef __cplusplus
}
#endif

#endif //MINI_JVM_UTIL_H
