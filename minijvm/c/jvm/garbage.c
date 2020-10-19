
#include <errno.h>
#include "jvm.h"
#include "garbage.h"
#include "jvm_util.h"


void _dump_refer(void);

void _getMBName(void *memblock, Utf8String *name);

void _garbage_clear(void);

void _garbage_destory_memobj(MemoryBlock *k);

void _garbage_change_flag(void);

void _garbage_copy_refer(void);

s32 _garbage_big_search(void);

s32 _garbage_pause_the_world(void);

s32 _garbage_resume_the_world(void);

s32 _checkAndWaitThreadIsSuspend(Runtime *runtime);

void _garbage_mark_object(__refer ref);

s32 _garbage_copy_refer_thread(Runtime *pruntime);


/**
 * 创建垃圾收集线程，
 *
 * 收集方法如下：
 * 当对象被创建时，注册进垃圾收集器，纳入监管体系。
 *
 *
 * 注册的对象包括 Class 类， Instance 对象实例（包括数组对象）
 *
 * 在垃圾回收时，由垃圾收集线程来收集，收集 collector->header 链表中的对象，
 * 收集方法为： 当对象未被任一线程引用时，进行标记，直接销毁，释放内存
 *
 * 收集线程会暂停所有正在执行中的java线程 ，回收完之后，恢复线程执行
 *
 * jdwp调试线程中的运行时对象不可回收。
 *
 * collecting  step:
 *
 * 1. stop the world
 * 2. all reg/hold/release operation add to a temp linklist ,
 * 3. copy all runtime refer
 * 4. mark object when referenced by threads
 * 5. resume the world
 * 6. release unmarked object memory
 * 7. move temp linklist to main list
 *
 * @return errorcode
 */


s32 garbage_collector_create() {
    collector = jvm_calloc(sizeof(GcCollector));
    collector->objs_holder = hashset_create();

    collector->runtime_refer_copy = arraylist_create(256);

    collector->runtime = runtime_create(NULL);

    collector->_garbage_thread_status = GARBAGE_THREAD_PAUSE;
    thread_lock_init(&collector->garbagelock);

    s32 rc = thrd_create(&collector->_garbage_thread, _collect_thread_run, NULL);
    if (rc != thrd_success) {
        jvm_printf("ERROR: garbage thread can't create is %d\n", rc);
    } else {
        //启动垃圾回收
        garbage_collection_resume();
    }
    return 0;
}

void garbage_collector_destory() {
    garbage_collection_stop();
    garbage_thread_lock();
    while (collector->_garbage_thread_status != GARBAGE_THREAD_DEAD) {
        garbage_thread_timedwait(50);
    }
    garbage_thread_unlock();
    //
    _garbage_clear();
    //
    hashset_destory(collector->objs_holder);
    collector->objs_holder = NULL;

    arraylist_destory(collector->runtime_refer_copy);

    //
    runtime_destory(collector->runtime);
    thread_lock_dispose(&collector->garbagelock);
    jvm_free(collector);
    collector = NULL;
}


void _garbage_clear() {
#if _JVM_DEBUG_LOG_LEVEL > 1
    jvm_printf("[INFO]garbage clear start\n");
    jvm_printf("[INFO]objs size :%lld\n", collector->obj_count);
#endif
    //解除所有引用关系后，回收全部对象
    while (garbage_collect());//collect instance

    //release class static field
    classloader_release_classs_static_field(boot_classloader);
    while (garbage_collect());//collect classes

    //release classes
    classloader_destory(boot_classloader);
    boot_classloader = NULL;
    while (garbage_collect());//collect classes

    //
#if _JVM_DEBUG_LOG_LEVEL > 1
    jvm_printf("[INFO]objs size :%lld\n", collector->obj_count);
#endif
    //dump_refer();
}


//===============================   inner  ====================================

s32 garbage_thread_trylock() {
    return mtx_trylock(&collector->garbagelock.mutex_lock);
}

void garbage_thread_lock() {
    mtx_lock(&collector->garbagelock.mutex_lock);
}

void garbage_thread_unlock() {
    mtx_unlock(&collector->garbagelock.mutex_lock);
}

void garbage_collection_pause() {
    garbage_thread_lock();
    collector->_garbage_thread_status = GARBAGE_THREAD_PAUSE;
    garbage_thread_unlock();
}

void garbage_collection_resume() {
    garbage_thread_lock();
    collector->_garbage_thread_status = GARBAGE_THREAD_NORMAL;
    garbage_thread_unlock();
}

void garbage_collection_stop() {
    garbage_thread_lock();
    collector->_garbage_thread_status = GARBAGE_THREAD_STOP;
    garbage_thread_unlock();
}

void garbage_thread_wait() {
    cnd_wait(&collector->garbagelock.thread_cond, &collector->garbagelock.mutex_lock);
}

void garbage_thread_timedwait(s64 ms) {
    struct timespec t;
    clock_gettime(CLOCK_REALTIME, &t);
    t.tv_sec += ms / 1000;
    t.tv_nsec += (ms % 1000) * 1000000;
    s32 ret = cnd_timedwait(&collector->garbagelock.thread_cond, &collector->garbagelock.mutex_lock, &t);
//    if (ret == ETIMEDOUT) {
//        s32 debug = 1;
//    }
}

void garbage_thread_notify() {
    cnd_signal(&collector->garbagelock.thread_cond);
}

void garbage_thread_notifyall() {
    cnd_broadcast(&collector->garbagelock.thread_cond);
}
//=============================   debug ===================================

void _getMBName(void *memblock, Utf8String *name) {

    MemoryBlock *mb = (MemoryBlock *) memblock;
    if (!mb) {
        utf8_append_c(name, "NULL");
    } else {
        switch (mb->type) {
            case MEM_TYPE_CLASS: {
                utf8_append_c(name, "C");
                JClass *clazz = collector->_garbage_thread_status == GARBAGE_THREAD_NORMAL ? (JClass *) mb : NULL;
                if (clazz)
                    utf8_append(name, clazz->name);
                break;
            }
            case MEM_TYPE_INS: {
                Instance *ins = (Instance *) mb;
                utf8_append_c(name, "L");
                JClass *clazz = collector->_garbage_thread_status == GARBAGE_THREAD_NORMAL ? ins->mb.clazz : NULL;
                if (clazz)
                    utf8_append(name, clazz->name);
                utf8_append_c(name, ";");
                break;
            }
            case MEM_TYPE_ARR: {
                Instance *arr = (Instance *) mb;

                utf8_append_c(name, "Array{");
                JClass *clazz = collector->_garbage_thread_status == GARBAGE_THREAD_NORMAL ? arr->mb.clazz : NULL;
                if (clazz)
                    utf8_append(name, clazz->name);
                utf8_append_c(name, "}");
                break;
            }
            default:
                utf8_append_c(name, "ERROR");
        }
    }
}


s32 _getMBSize(MemoryBlock *mb) {
    return mb->heap_size;
}

/**
 * 调试用，打印所有引用信息
 */
void _dump_refer() {
    //jvm_printf("%d\n",sizeof(struct _Hashset));
    MemoryBlock *mb = collector->tmp_header;
    while (mb) {
        Utf8String *name = utf8_create();
        _getMBName(mb, name);
        jvm_printf("   %s[%llx] \n", utf8_cstr(name), (s64) (intptr_t) mb);
        utf8_destory(name);
        mb = mb->next;
    }
}

void garbage_dump_runtime() {
    s32 i;
    //jvm_printf("thread set size:%d\n", thread_list->length);
    Utf8String *name = utf8_create();
    for (i = 0; i < thread_list->length; i++) {
        Runtime *runtime = threadlist_get(i);

        jvm_printf("[INFO]============ runtime dump [%llx] ============\n", (s64) (intptr_t) runtime);
        s32 j;
        StackEntry *entry;
        RuntimeStack *stack = runtime->stack;
        for (j = 0; j < stack_size(stack); j++) {
            entry = stack->store + j;
            if (entry->rvalue) {
                __refer ref = entry->rvalue;
                if (ref) {
                    utf8_clear(name);
                    _getMBName(ref, name);
                    jvm_printf("   %s[%llx] \n", utf8_cstr(name), (s64) (intptr_t) ref);
                    utf8_destory(name);
                }
            }
        }

    }
}


void _garbage_put_in_holder(__refer ref) {
    hashset_put(collector->objs_holder, ref);

#if _JVM_DEBUG_GARBAGE_DUMP
    Utf8String *sus = utf8_create();
    _getMBName((MemoryBlock *) ref, sus);
    jvm_printf("+: %s[%llx]\n", utf8_cstr(sus), (s64) (intptr_t) ref);
    utf8_destory(sus);
#endif
}

void _garbage_remove_out_holder(__refer ref) {
    hashset_remove(collector->objs_holder, ref, 0);
#if _JVM_DEBUG_GARBAGE_DUMP
    Utf8String *sus = utf8_create();
    _getMBName((MemoryBlock *) ref, sus);
    jvm_printf("-: %s[%llx]\n", utf8_cstr(sus), (s64) (intptr_t) ref);
    utf8_destory(sus);
#endif
}

s64 _garbage_sum_heap() {
    s64 hsize = 0;
    hsize += threadlist_sum_heap();
    spin_lock(&collector->lock);
    {
        hsize += collector->obj_heap_size;
    }
    spin_unlock(&collector->lock);
    return hsize;
}
//==============================   thread_run() =====================================

s32 _collect_thread_run(void *para) {
    s64 lastgc = currentTimeMillis();
    while (1) {

        s64 cur_mil = currentTimeMillis();

        if (collector->_garbage_thread_status == GARBAGE_THREAD_STOP) {
            break;
        }
        if (collector->_garbage_thread_status == GARBAGE_THREAD_PAUSE) {
            continue;
        }
        if (cur_mil - lastgc < 1000) {// less than custom sec no gc
            continue;
        }
//        if (JDWP_DEBUG && jdwpserver.clients->length) {// less than 3 sec no gc
//            continue;
//        }

        if (cur_mil - lastgc > GARBAGE_PERIOD_MS || _garbage_sum_heap() > MAX_HEAP_SIZE * .8f) {
            garbage_collect();
            lastgc = cur_mil;
        } else {
            threadSleep(10);
        }
    }
    collector->_garbage_thread_status = GARBAGE_THREAD_DEAD;
    thrd_exit(0);
    return 0;
}


/**
 * 查找所有实例，如果发现没有被引用时 mb->garbage_mark ，
 * 去除掉此对象对其他对象的引用，并销毁对象
 *
 * @return ret
 */
s64 garbage_collect() {
    collector->isgc = 1;
    s64 mem_total = 0, mem_free = 0;
    s64 del = 0;
    s64 time, start;

    start = time = currentTimeMillis();
    //prepar gc resource ,
    garbage_thread_lock();

    if (_garbage_pause_the_world() != 0) {
        _garbage_resume_the_world();
        garbage_thread_unlock();
        jvm_printf("gc canceled ");
        return -1;
    }
//    jvm_printf("garbage_pause_the_world %lld\n", (currentTimeMillis() - time));
//    time = currentTimeMillis();
    if (collector->tmp_header) {
        collector->tmp_tailer->next = collector->header;//接起来
        collector->header = collector->tmp_header;
        collector->tmp_header = NULL;
        collector->tmp_tailer = NULL;
    }
//    jvm_printf("garbage_move_cache %lld\n", (currentTimeMillis() - time));
//    time = currentTimeMillis();
    _garbage_copy_refer();
    //
//    jvm_printf("garbage_copy_refer %lld\n", (currentTimeMillis() - time));
//    time = currentTimeMillis();
    //real GC start
    //
    _garbage_change_flag();
    _garbage_big_search();
    //
//    jvm_printf("garbage_big_search %lld\n", (currentTimeMillis() - time));
//    time = currentTimeMillis();

    _garbage_resume_the_world();
    garbage_thread_unlock();

//    jvm_printf("garbage_resume_the_world %lld\n", (currentTimeMillis() - time));

    s64 time_stopWorld = currentTimeMillis() - start;
    time = currentTimeMillis();
    //


    MemoryBlock *nextmb = collector->header;
    MemoryBlock *curmb, *prevmb = NULL;
    //finalize
    if (collector->_garbage_thread_status == GARBAGE_THREAD_NORMAL) {
        while (nextmb) {
            curmb = nextmb;
            nextmb = curmb->next;
            if (curmb->clazz->finalizeMethod) {// there is a method called finalize
                if (curmb->type == MEM_TYPE_INS && curmb->garbage_mark != collector->flag_refer) {
                    instance_finalize((Instance *) curmb, collector->runtime);
                }
            }
        }
    }
    gc_move_refer_thread_2_gc(collector->runtime);// maybe someone new object in finalize...

//    jvm_printf("garbage_finalize %lld\n", (currentTimeMillis() - time));
//    time = currentTimeMillis();
    //clear
    nextmb = collector->header;
    prevmb = NULL;
    s64 iter = 0;
    while (nextmb) {
        iter++;
        curmb = nextmb;
        nextmb = curmb->next;
        s32 size = curmb->heap_size;
        mem_total += size;
        if (curmb->garbage_mark != collector->flag_refer) {
            mem_free += size;
            //
            _garbage_destory_memobj(curmb);
            if (prevmb)prevmb->next = nextmb;
            else collector->header = nextmb;
            del++;
        } else {
            prevmb = curmb;
        }
    }
    spin_lock(&collector->lock);
    collector->obj_count = iter - del;
    heap_size = mem_total - mem_free;
    collector->obj_heap_size -= mem_free;
    spin_unlock(&collector->lock);

#if _JVM_DEBUG_LOG_LEVEL > 1
    s64 time_gc = currentTimeMillis() - time;
    jvm_printf("[INFO]gc obj: %lld->%lld   heap : %lld -> %lld  stop_world: %lld  gc:%lld\n", iter, collector->obj_count, mem_total, heap_size, time_stopWorld, time_gc);
#endif

#ifdef MEM_ALLOC_LTALLOC
    jvm_squeeze(0);
#endif
    collector->isgc = 0;
    return del;
}

void _garbage_change_flag() {
    collector->flag_refer++;
    if (collector->flag_refer == 0) {
        collector->flag_refer = 1;
    }
}


void _garbage_destory_memobj(MemoryBlock *mb) {
#if _JVM_DEBUG_GARBAGE_DUMP
    Utf8String *sus = utf8_create();
    _getMBName(mb, sus);
    jvm_printf("X: %s[%llx]\n", utf8_cstr(sus), (s64) (intptr_t) mb);
    utf8_destory(sus);
#endif
    memoryblock_destory((Instance *) mb);

}


/**
 * 各个线程把自己还需要使用的对象进行标注，表示不能回收
 */
void _list_iter_thread_pause(ArrayListValue value, void *para) {
    jthread_suspend((Runtime *) value);
}

s32 _garbage_pause_the_world(void) {
    s32 i;
    //jvm_printf("thread size:%d\n", thread_list->length);

    if (thread_list->length) {
        arraylist_iter_safe(thread_list, _list_iter_thread_pause, NULL);

        for (i = 0; i < thread_list->length; i++) {
            Runtime *runtime = arraylist_get_value(thread_list, i);
            if (_checkAndWaitThreadIsSuspend(runtime) == -1) {
                return -1;
            }
            gc_move_refer_thread_2_gc(runtime);

#if _JVM_DEBUG_GARBAGE_DUMP
            Utf8String *stack = utf8_create();
            getRuntimeStack(runtime, stack);
            jvm_printf("%s\n", utf8_cstr(stack));
            utf8_destory(stack);
#endif
        }

    }
    //调试线程
    if (jdwp_enable) {
        Runtime *runtime = jdwpserver.runtime;
        if (runtime) {
            jthread_suspend(runtime);
            if (_checkAndWaitThreadIsSuspend(runtime) == -1) {
                return -1;
            }
            gc_move_refer_thread_2_gc(runtime);
        }
    }
    return 0;
}

s32 _garbage_resume_the_world() {
    s32 i;
    for (i = 0; i < thread_list->length; i++) {
        Runtime *runtime = arraylist_get_value(thread_list, i);
        if (runtime) {
#if _JVM_DEBUG_GARBAGE_DUMP
            Utf8String *stack = utf8_create();
            getRuntimeStack(runtime, stack);
            jvm_printf("%s\n", utf8_cstr(stack));
            utf8_destory(stack);
#endif
            jthread_resume(runtime);
            garbage_thread_notifyall();
        }
    }

    //调试线程
    if (jdwp_enable) {
        Runtime *runtime = jdwpserver.runtime;
        if (runtime) {
            jthread_resume(runtime);
        }
    }
    return 0;
}


s32 _checkAndWaitThreadIsSuspend(Runtime *runtime) {
    while (!(runtime->threadInfo->is_suspend) &&
           !(runtime->threadInfo->is_blocking)) { // if a native method blocking , must set thread status is wait before enter native method
        garbage_thread_notifyall();
        garbage_thread_timedwait(1);
        if (collector->_garbage_thread_status != GARBAGE_THREAD_NORMAL) {
            return -1;
        }
    }
    return 0;
}

//=================================  big_search ==================================

/**
 * on all threads stoped ,
 * mark thread's localvar and stack refered obj and deep search
 * @return ret
 */
s32 _garbage_big_search() {
    s32 i, len;
    for (i = 0, len = collector->runtime_refer_copy->length; i < len; i++) {
        __refer r = arraylist_get_value(collector->runtime_refer_copy, i);
        _garbage_mark_object(r);
    }

    HashsetIterator hi;
    hashset_iterate(collector->objs_holder, &hi);
    while (hashset_iter_has_more(&hi)) {
        HashsetKey k = hashset_iter_next_key(&hi);

        _garbage_mark_object(k);
    }

    return 0;
}

void _garbage_copy_refer() {
    arraylist_clear(collector->runtime_refer_copy);
    s32 i;
    //jvm_printf("thread set size:%d\n", thread_list->length);
    for (i = 0; i < thread_list->length; i++) {
        Runtime *runtime = threadlist_get(i);
        _garbage_copy_refer_thread(runtime);
    }
//    arraylist_iter_safe(thread_list, _list_iter_iter_copy, NULL);
    //调试线程
    if (jdwp_enable) {
        Runtime *runtime = jdwpserver.runtime;
        if (runtime) {
            _garbage_copy_refer_thread(runtime);
        }
    }
}

/**
 * 判定某个对象是否被所有线程的runtime引用
 * 被运行时的栈或局部变量所引用，
 * 这两种情况下，对象是不能被释放的
 *
 * @param pruntime son of runtime
 * @return how many marked
 */


s32 _garbage_copy_refer_thread(Runtime *pruntime) {
    arraylist_push_back_unsafe(collector->runtime_refer_copy, pruntime->threadInfo->jthread);

    Runtime *runtime = pruntime;
    RuntimeStack *stack = runtime->stack;
    //reset free stack space
    memset(stack->sp, 0, sizeof(StackEntry) * (stack->max_size - stack_size(stack)));

    s32 i, imax;
    StackEntry *entry;
    for (i = 0, imax = stack_size(stack); i < imax; i++) {
        entry = stack->store + i;
        if (entry->rvalue) {
            arraylist_push_back_unsafe(collector->runtime_refer_copy, entry->rvalue);
        }
    }
    MemoryBlock *next = runtime->threadInfo->tmp_holder;
    for (; next;) {
        arraylist_push_back_unsafe(collector->runtime_refer_copy, next);
        next = next->tmp_next;
    }

    //jvm_printf("[%llx] notified\n", (s64) (intptr_t) pruntime->threadInfo->jthread);
    return 0;
}


static inline void _instance_mark_refer(Instance *ins) {
    s32 i, len;
    JClass *clazz = ins->mb.clazz;
    while (clazz) {
        FieldPool *fp = &clazz->fieldPool;
        ArrayList *fiList = clazz->insFieldPtrIndex;
        for (i = 0, len = fiList->length; i < len; i++) {
            FieldInfo *fi = &fp->field[(s32) (intptr_t) arraylist_get_value_unsafe(fiList, i)];
            c8 *ptr = getInstanceFieldPtr(ins, fi);
            if (ptr) {
                __refer ref = getFieldRefer(ptr);
                if (ref)_garbage_mark_object(ref);
            }
        }
        clazz = getSuperClass(clazz);
    }
}


static inline void _jarray_mark_refer(Instance *arr) {
    if (arr && arr->mb.type == MEM_TYPE_ARR) {
//        if (utf8_equals_c(arr->mb.clazz->name, "[[D")) {
//            jvm_printf("check %llx\n", (s64) (intptr_t) arr);
//        }
        if (isDataReferByIndex(arr->mb.arr_type_index)) {
            s32 i;
            for (i = 0; i < arr->arr_length; i++) {//把所有引用去除，否则不会垃圾回收
                s64 val = jarray_get_field(arr, i);
                if (val)_garbage_mark_object((__refer) (intptr_t) val);
            }
        }
    }
    return;
}

/**
 * mark class static field is used
 * @param clazz class
 */
static inline void _class_mark_refer(JClass *clazz) {
    s32 i, len;
    if (clazz->field_static) {
        FieldPool *fp = &clazz->fieldPool;
        ArrayList *fiList = clazz->staticFieldPtrIndex;
        for (i = 0, len = fiList->length; i < len; i++) {
            FieldInfo *fi = &fp->field[(s32) (intptr_t) arraylist_get_value_unsafe(fiList, i)];
            c8 *ptr = getStaticFieldPtr(fi);
            if (ptr) {
                __refer ref = getFieldRefer(ptr);
                _garbage_mark_object(ref);
            }
        }
    }
}


/**
 * 递归标注obj所有的子孙
 * @param ref addr
 */

void _garbage_mark_object(__refer ref) {
    if (ref) {
        MemoryBlock *mb = (MemoryBlock *) ref;
        if (collector->flag_refer != mb->garbage_mark) {
            mb->garbage_mark = collector->flag_refer;
            switch (mb->type) {
                case MEM_TYPE_INS:
                    _instance_mark_refer((Instance *) mb);
                    break;
                case MEM_TYPE_ARR:
                    _jarray_mark_refer((Instance *) mb);
                    break;
                case MEM_TYPE_CLASS:
                    _class_mark_refer((JClass *) mb);
                    break;
            }
        }
    }
}
//=================================  reg unreg ==================================

MemoryBlock *gc_is_alive(__refer ref) {
    __refer result = hashset_get(collector->objs_holder, ref);
    spin_lock(&collector->lock);
    if (!result) {
        MemoryBlock *mb = collector->header;
        while (mb) {
            if (mb == ref) {
                result = mb;
                break;
            }
            mb = mb->next;
        }
    }
    if (!result) {
        MemoryBlock *mb = collector->tmp_header;
        while (mb) {
            if (mb == ref) {
                result = mb;
                break;
            }
            mb = mb->next;
        }
    }
    if (!result) {
        s32 i;
        for (i = 0; i < thread_list->length; i++) {
            Runtime *runtime = threadlist_get(i);
            MemoryBlock *mb = runtime->threadInfo->objs_header;
            while (mb) {
                if (mb == ref) {
                    result = mb;
                    break;
                }
                mb = mb->next;
            }
        }
    }
    if (!result) {
        if (jdwp_enable) {
            Runtime *runtime = jdwpserver.runtime;
            if (runtime) {
                MemoryBlock *mb = runtime->threadInfo->objs_header;
                while (mb) {
                    if (mb == ref) {
                        result = mb;
                        break;
                    }
                    mb = mb->next;
                }
            }
        }
    }
    spin_unlock(&collector->lock);
    return (MemoryBlock *) result;
}

/**
 * 把对象放到线程的对象列表中
 * @param runtime
 * @param ref
 */
void gc_refer_reg(Runtime *runtime, __refer ref) {
    if (!ref)return;
    MemoryBlock *mb = (MemoryBlock *) ref;
    if (!mb->garbage_reg) {
        mb->garbage_reg = 1;
        JavaThreadInfo *ti = runtime->threadInfo;
        mb->next = ti->objs_header;
        ti->objs_header = ref;
        if (!ti->objs_tailer) {
            ti->objs_tailer = ref;
        }
        ti->objs_heap_of_thread += mb->heap_size;
#if _JVM_DEBUG_GARBAGE_DUMP
        Utf8String *sus = utf8_create();
        _getMBName(mb, sus);
        jvm_printf("R: %s[%llx]\n", utf8_cstr(sus), (s64) (intptr_t) mb);
        utf8_destory(sus);
#endif
    }
}

/**
 * 把线程持有的所有对象,链接到gc的链表中去
 * move thread 's jobject to garbage
 * @param runtime
 */
void gc_move_refer_thread_2_gc(Runtime *runtime) {
    if (runtime) {
        JavaThreadInfo *ti = runtime->threadInfo;

        if (ti->objs_header) {
            //lock
            spin_lock(&collector->lock);
            {
#if _JVM_DEBUG_GARBAGE_DUMP
                MemoryBlock *mb = ti->objs_header;
                while (mb) {
                    Utf8String *sus = utf8_create();
                    _getMBName(mb, sus);
                    jvm_printf("M: %s[%llx]\n", utf8_cstr(sus), (s64) (intptr_t) mb);
                    utf8_destory(sus);
                    mb = mb->next;
                }
#endif
                ti->objs_tailer->next = collector->tmp_header;
                if (!collector->tmp_tailer) {
                    collector->tmp_tailer = ti->objs_tailer;
                }
                collector->tmp_header = ti->objs_header;
                collector->obj_heap_size += ti->objs_heap_of_thread;
            }
            spin_unlock(&collector->lock);

            ti->objs_header = NULL;
            ti->objs_tailer = NULL;

            ti->objs_heap_of_thread = 0;
        }
    }
}


void gc_refer_hold(__refer ref) {
    if (ref) {
        _garbage_put_in_holder(ref);
    }
}

void gc_refer_release(__refer ref) {
    if (ref) {
        _garbage_remove_out_holder(ref);
    }
}

