#include <sys/stat.h>
#include <stdio.h>
#include "garbage.h"

//#undef PRJ_DEBUG_GARBAGE_DUMP
//#define PRJ_DEBUG_GARBAGE_DUMP 01

void release_classs_static_field();

void _dump_refer();

void _garbage_clear();

void _garbage_destory_memobj(InstProp *k);

void _garbage_change_flag();

void _garbage_copy_refer();

s32 _garbage_big_search();

s32 _garbage_pause_the_world();

s32 _garbage_resume_the_world();

s32 _checkAndWaitThreadIsSuspend(JThreadRuntime *runtime);

void _garbage_mark_object(__refer ref, u8 flag_cnt);

s32 _garbage_copy_refer_thread(JThreadRuntime *pruntime);

void instance_destory(__refer ref);

/**
 * 创建垃圾收集线程，
 *
 * 收集方法如下：
 * 当对象被创建时，注册进垃圾收集器，纳入监管体系。
 *
 *
 * 注册的对象包括 Class 类，  InstProp 对象实例（包括数组对象）
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


GcCollector *garbage_collector_create() {
    GcCollector *collector = jvm_calloc(sizeof(GcCollector));
    collector->objs_holder = hashtable_create(DEFAULT_HASH_FUNC, DEFAULT_HASH_EQUALS_FUNC);

    collector->runtime_refer_copy = arraylist_create(256);
    collector->garbage_collect_period_ms = 30 * 1000;//mill sec
    collector->max_heap_size = 30 * 1000 * 1000;//byte

    collector->_garbage_thread_status = GARBAGE_THREAD_PAUSE;
    thread_lock_init(&collector->garbagelock);

    return collector;
}

void garbage_collector_destory(GcCollector *collector) {
    garbage_collection_stop();
    garbage_thread_lock();
    while (collector->_garbage_thread_status != GARBAGE_THREAD_DEAD) {
        garbage_thread_timedwait(50);
    }
    garbage_thread_unlock();

    g_jvm->collector->runtime = jthreadruntime_create();
    //
    _garbage_clear();
    //
    hashtable_destory(collector->objs_holder);
    collector->objs_holder = NULL;

    arraylist_destory(collector->runtime_refer_copy);

    jthreadruntime_destroy(collector->runtime);
    //
    thread_lock_dispose(&collector->garbagelock);
    jvm_free(collector);
    collector = NULL;
}


void garbage_start() {
    GcCollector *collector = g_jvm->collector;


    s32 rc = thrd_create(&collector->_garbage_thread, _collect_thread_run, NULL);
    if (rc != thrd_success) {
        jvm_printf("ERROR: garbage thread can't create is %d\n", rc);
    } else {
        //启动垃圾回收
        garbage_collection_resume();
    }
}


void _garbage_clear() {
#if _JVM_DEBUG_BYTECODE_DETAIL > 1
    jvm_printf("[INFO]garbage clear start\n");
    jvm_printf("[INFO]objs size :%lld\n", collector->obj_count);
#endif

    //release class static field
    release_classs_static_field();
    //
    hashtable_clear(g_jvm->collector->objs_holder);
    //解除所有引用关系后，回收全部对象
    while (_garbage_collect());//collect  InstProp

    //
#if _JVM_DEBUG_BYTECODE_DETAIL > 1
    jvm_printf("[INFO]objs size :%lld\n", collector->obj_count);
#endif
    //dump_refer();
}

void release_classs_static_field() {}

//===============================   inner  ====================================

s32 garbage_thread_trylock() {
    return mtx_trylock(&g_jvm->collector->garbagelock.mutex_lock);
}

void garbage_thread_lock() {
    mtx_lock(&g_jvm->collector->garbagelock.mutex_lock);
}

void garbage_thread_unlock() {
    mtx_unlock(&g_jvm->collector->garbagelock.mutex_lock);
}

void garbage_collection_pause() {
    garbage_thread_lock();
    g_jvm->collector->_garbage_thread_status = GARBAGE_THREAD_PAUSE;
    garbage_thread_unlock();
}

void garbage_collection_resume() {
    garbage_thread_lock();
    g_jvm->collector->_garbage_thread_status = GARBAGE_THREAD_NORMAL;
    garbage_thread_unlock();
}

void garbage_collection_stop() {
    garbage_thread_lock();
    g_jvm->collector->_garbage_thread_status = GARBAGE_THREAD_STOP;
    garbage_thread_unlock();
}

void garbage_thread_wait() {
    cnd_wait(&g_jvm->collector->garbagelock.thread_cond, &g_jvm->collector->garbagelock.mutex_lock);
}

void garbage_thread_timedwait(s64 ms) {
    struct timespec t;
    timespec_get(&t, TIME_UTC);
    t.tv_sec += ms / 1000;
    t.tv_nsec += (ms % 1000) * 1000000;
    s32 ret = cnd_timedwait(&g_jvm->collector->garbagelock.thread_cond, &g_jvm->collector->garbagelock.mutex_lock, &t);
//    if (ret == ETIMEDOUT) {
//        s32 debug = 1;
//    }
}

void garbage_thread_notify() {
    cnd_signal(&g_jvm->collector->garbagelock.thread_cond);
}

void garbage_thread_notifyall() {
    cnd_broadcast(&g_jvm->collector->garbagelock.thread_cond);
}


//=============================   debug ===================================

void _getMBName(void *memblock, Utf8String *name) {

    InstProp *mb = (InstProp *) memblock;
    if (!mb) {
        utf8_append_c(name, "NULL");
    } else {
        switch (mb->type) {
            case INS_TYPE_CLASS: {
                utf8_append_c(name, "C");
                JClass *clazz = g_jvm->collector->_garbage_thread_status == GARBAGE_THREAD_NORMAL ? (JClass *) mb : NULL;
                if (clazz)
                    utf8_append(name, clazz->name);
                break;
            }
            case INS_TYPE_OBJECT: {
                InstProp *ins = (InstProp *) mb;
                utf8_append_c(name, "L");
                JClass *clazz = g_jvm->collector->_garbage_thread_status == GARBAGE_THREAD_NORMAL ? ins->clazz : NULL;
                if (clazz)
                    utf8_append(name, clazz->name);
                utf8_append_c(name, ";");
                break;
            }
            case INS_TYPE_ARRAY: {
                InstProp *arr = (InstProp *) mb;

                utf8_append_c(name, "Array{");
                JClass *clazz = g_jvm->collector->_garbage_thread_status == GARBAGE_THREAD_NORMAL ? arr->clazz : NULL;
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

/**
 * 调试用，打印所有引用信息
 */
void _dump_refer() {
    //jvm_printf("%d\n",sizeof( _Hashset));
    InstProp *mb = g_jvm->collector->tmp_header;
    while (mb) {
        Utf8String *name = utf8_create();
        _getMBName(mb, name);
        jvm_printf("A  %s[%llx] \n", utf8_cstr(name), (s64) (intptr_t) mb);
        utf8_destory(name);
        mb = mb->next;
    }
}

void _garbage_put_in_holder(__refer ref) {
    hashtable_put(g_jvm->collector->objs_holder, ref, NULL);

#if PRJ_DEBUG_GARBAGE_DUMP
    Utf8String *sus = utf8_create();
    _getMBName((InstProp *) ref, sus);
    jvm_printf("+: [%llx]%s\n", (s64) (intptr_t) ref, utf8_cstr(sus));
    utf8_destory(sus);
#endif
}

void _garbage_remove_out_holder(__refer ref) {
    hashtable_remove(g_jvm->collector->objs_holder, ref, 0);
#if PRJ_DEBUG_GARBAGE_DUMP
    Utf8String *sus = utf8_create();
    _getMBName((InstProp *) ref, sus);
    jvm_printf("-: [%llx]%s\n", (s64) (intptr_t) ref, utf8_cstr(sus));
    utf8_destory(sus);
#endif
}

void instance_finalize(JThreadRuntime *runtime, InstProp *ins) {
    if (ins) {
        finalize_func_t finalizeFunc = ins->clazz->raw->finalize_method;
        if (finalizeFunc) {
            finalizeFunc(runtime, ins);//
            //exception_check_print(runtime);
        }
    }
}


void instance_destory(__refer ref) {
    InstProp *mb = (InstProp *) ref;
    if (!mb)return;
//    if (utf8_equals_c(mb->clazz->name, "test/GuiTest$CallBack")) {
//        garbage_dump_runtime();
//        int debug = 1;
//    }
#if PRJ_DEBUG_GARBAGE_DUMP
    jvm_printf("destroy :%llx\n", (s64) (intptr_t) ref);
#endif
    if (mb->type == INS_TYPE_OBJECT) {
        jobject_destroy((JObject *) mb);
    } else if (mb->type == INS_TYPE_ARRAY) {
        jarray_destroy((JArray *) mb);
    } else if (mb->type == INS_TYPE_CLASS) {
        jclass_destroy((JClass *) mb);
    }
}

s64 threadlist_sum_heap(GcCollector *collector) {
    s64 hsize = 0;
    spin_lock(&g_jvm->thread_list->spinlock);
    s32 i;
    for (i = 0; i < g_jvm->thread_list->length; i++) {
        JThreadRuntime *r = arraylist_get_value_unsafe(g_jvm->thread_list, i);
        hsize += r->objs_heap_of_thread;
    }
    spin_unlock(&g_jvm->thread_list->spinlock);
    return hsize;
}

s64 gc_sum_heap(GcCollector *collector) {
    s64 hsize = threadlist_sum_heap(collector);
    spin_lock(&collector->lock);
    {
        hsize += collector->obj_heap_size;
    }
    spin_unlock(&collector->lock);
    return hsize;
}

//==============================   thread_run() =====================================

s32 _collect_thread_run(void *para) {
    JThreadRuntime *runtime = jthreadruntime_create();
    jthread_bound(runtime);
    arraylist_remove(g_jvm->thread_list, runtime);//remove from threadlist

    GcCollector *collector = g_jvm->collector;
    collector->runtime = runtime;
    while (1) {
        s64 cur_mil = currentTimeMillis();

        if (collector->_garbage_thread_status == GARBAGE_THREAD_STOP) {
            break;
        }
        if (collector->_garbage_thread_status == GARBAGE_THREAD_PAUSE
            || cur_mil - collector->lastgc < 1000
            || g_jvm->thread_list == 0
                ) {// less than custom sec no gc
            threadSleep(100);
            continue;
        }

        s64 heap = gc_sum_heap(collector);
        if (cur_mil - collector->lastgc > collector->garbage_collect_period_ms || heap >= collector->max_heap_size * .8f) {
            _garbage_collect();
            collector->lastgc = cur_mil;
        } else {
            threadSleep(100);
        }
    }
    collector->_garbage_thread_status = GARBAGE_THREAD_DEAD;

    jthread_unbound(runtime);

    thrd_exit(0);
    return 0;
}


/**
 * 查找所有实例，如果发现没有被引用时 mb->garbage_mark ，
 * 去除掉此对象对其他对象的引用，并销毁对象
 *
 * @return ret
 */
s64 _garbage_collect() {
    GcCollector *collector = g_jvm->collector;
    collector->isgc = 1;
    s64 mem_total = 0, mem_free = 0;
    s64 del = 0;
    s64 time, start;

    start = time = currentTimeMillis();
    //prepar gc resource ,
    garbage_thread_lock();

    if (_garbage_pause_the_world() != 0) {
        _garbage_resume_the_world();
        return -1;
    }
#if PRJ_DEBUG_GARBAGE_DUMP
    _dump_refer();
#endif
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

    InstProp *nextmb = collector->header;
    InstProp *curmb, *prevmb = NULL;
    //finalize
    if (collector->_garbage_thread_status == GARBAGE_THREAD_NORMAL) {
        while (nextmb) {
            curmb = nextmb;
            nextmb = curmb->next;
            if (curmb->type == INS_TYPE_OBJECT) {
                JThreadRuntime *runtime = tss_get(TLS_KEY_JTHREADRUNTIME);
                ClassRaw *raw = curmb->clazz->raw;
                if (raw && raw->finalize_method && !curmb->is_finalized && curmb->garbage_mark != collector->mark_cnt) {
                    instance_finalize(runtime, curmb);
                    _garbage_mark_object(curmb, collector->mark_cnt);//mark it gc do not collect it
                    curmb->is_finalized = 1;
                }
                //process weakreference
                if (curmb->is_weakreference) {//is weakreference
                    InstProp *target = (InstProp *) weakreference_get_target(runtime, (JObject *) curmb);
                    //jvm_printf("weak reference : %llx %s, %d\n", (s64) (intptr_t) curmb, utf8_cstr(target->mb.clazz->name), curmb->garbage_mark);
                    if (target && target->garbage_mark != collector->mark_cnt) {
                        weakref_vmreferenceenqueue(runtime, (JObject *) curmb);
                        _garbage_mark_object(curmb, collector->mark_cnt);//mark it gc do not collect it
                    }
                }
                //move gc thread instance to gc
                gc_move_refer_thread_2_gc(runtime);// maybe someone new object in finalize...

            }
        }
    }



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
        if (curmb->garbage_mark != collector->mark_cnt) {
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
    collector->obj_heap_size -= mem_free;
    spin_unlock(&collector->lock);

    s64 time_gc = currentTimeMillis() - time;
#if PRJ_DEBUG_LEV > 1
    jvm_printf("[INFO]gc obj: %lld->%lld   heap : %lld -> %lld  stop_world: %lld  gc:%lld\n", iter, collector->obj_count, mem_total, collector->obj_heap_size, time_stopWorld, time_gc);
#endif

    collector->isgc = 0;
    return del;
}

void _garbage_change_flag() {
    g_jvm->collector->mark_cnt++;
    if (g_jvm->collector->mark_cnt == 0) {
        g_jvm->collector->mark_cnt = 1;
    }
}


void _garbage_destory_memobj(InstProp *mb) {
#if PRJ_DEBUG_GARBAGE_DUMP
    Utf8String *sus = utf8_create();
    _getMBName(mb, sus);
    jvm_printf("X: %s[%llx]\n", utf8_cstr(sus), (s64) (intptr_t) mb);
    utf8_destory(sus);
#endif
    instance_destory((InstProp *) mb);

}


/**
 * 各个线程把自己还需要使用的对象进行标注，表示不能回收
 */
void _list_iter_thread_pause(ArrayListValue value, void *para) {
    jthread_suspend((JThreadRuntime *) value);
}

s32 _garbage_pause_the_world() {
    s32 i;
    //jvm_printf("thread size:%d\n", thread_list->length);

    if (g_jvm->thread_list->length) {
        arraylist_iter_safe(g_jvm->thread_list, _list_iter_thread_pause, NULL);

        s32 alive;
        while (1) {
            alive = 0;
            for (i = 0; i < g_jvm->thread_list->length; i++) {
                JThreadRuntime *runtime = arraylist_get_value(g_jvm->thread_list, i);
                if (runtime->is_suspend || runtime->thread_status == THREAD_STATUS_BLOCKED) { // if a native method blocking , must set thread status is wait before enter native method
                    continue;
                }
                alive++;
            }
            if (g_jvm->collector->_garbage_thread_status != GARBAGE_THREAD_NORMAL) {
                return -1;
            }
            if (!alive) {
                break;
            } else {
                garbage_thread_notifyall();
                garbage_thread_timedwait(10);
            }
        }
        for (i = 0; i < g_jvm->thread_list->length; i++) {
            JThreadRuntime *runtime = arraylist_get_value(g_jvm->thread_list, i);
            gc_move_refer_thread_2_gc(runtime);
        }
    }

    return 0;
}

s32 _garbage_resume_the_world() {
    s32 i;
    for (i = 0; i < g_jvm->thread_list->length; i++) {
        JThreadRuntime *runtime = arraylist_get_value(g_jvm->thread_list, i);
        if (runtime) {

            jthread_resume(runtime);
            garbage_thread_notifyall();
        }
    }

    return 0;
}


s32 _checkAndWaitThreadIsSuspend(JThreadRuntime *runtime) {
    while (!(runtime->is_suspend) &&
           (runtime->thread_status != THREAD_STATUS_BLOCKED)) { // if a native method blocking , must set thread status is wait before enter native method
        garbage_thread_notifyall();
        garbage_thread_timedwait(10);
        if (g_jvm->collector->_garbage_thread_status != GARBAGE_THREAD_NORMAL) {
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
    for (i = 0, len = g_jvm->collector->runtime_refer_copy->length; i < len; i++) {
        __refer r = arraylist_get_value(g_jvm->collector->runtime_refer_copy, i);
        if (r) {
            InstProp *mb = (InstProp *) r;
            if (mb->type != INS_TYPE_OBJECT && mb->type != INS_TYPE_ARRAY && mb->type != INS_TYPE_CLASS) {
                Utf8String *name = utf8_create();
                _getMBName(mb, name);
                jvm_printf("Invalid object type in runtime_refer_copy:%llx %s, type: %d\n",
                           r, utf8_cstr(name), mb->type);
                utf8_destory(name);
                //_garbage_copy_refer();
            }
            _garbage_mark_object(r, g_jvm->collector->mark_cnt);
        } else {
            jvm_printf("Invalid object in runtime_refer_copy: %p\n", r);
        }
    }

    HashtableIterator hi;
    hashtable_iterate(g_jvm->collector->objs_holder, &hi);
    while (hashtable_iter_has_more(&hi)) {
        HashtableKey k = hashtable_iter_next_key(&hi);

        _garbage_mark_object(k, g_jvm->collector->mark_cnt);
    }

    return 0;
}

void _garbage_copy_refer() {
    arraylist_clear(g_jvm->collector->runtime_refer_copy);
    s32 i;
    //jvm_printf("thread set size:%d\n", thread_list->length);
    for (i = 0; i < g_jvm->thread_list->length; i++) {
        JThreadRuntime *runtime = arraylist_get_value(g_jvm->thread_list, i);
        _garbage_copy_refer_thread(runtime);
    }

    InstProp *mb = (InstProp *) g_jvm->collector->runtime->jthread;
    if (mb) {
        arraylist_push_back(g_jvm->collector->runtime_refer_copy, mb);
    }
//    arraylist_iter_safe(thread_list, _list_iter_iter_copy, NULL);

}

/**
 * 判定某个对象是否被所有线程的runtime引用
 * 被运行时的栈或局部变量所引用，
 * 这两种情况下，对象是不能被释放的
 *
 * @param pruntime son of runtime
 * @return how many marked
 */


s32 _garbage_copy_refer_thread(JThreadRuntime *pruntime) {
    if (pruntime->jthread) {
        InstProp *mb = (InstProp *) pruntime->jthread;
        if (mb->type == INS_TYPE_OBJECT || mb->type == INS_TYPE_ARRAY || mb->type == INS_TYPE_CLASS) {
            arraylist_push_back_unsafe(g_jvm->collector->runtime_refer_copy, pruntime->jthread);

            //jvm_printf("_garbage_copy_refer_thread:%llx name:%s\n", mb,  utf8_cstr(mb->clazz->name));
        } else {
            jvm_printf("Invalid object type in jthread: %s\n",
                       utf8_cstr(pruntime->jthread->prop.clazz->name));
        }
    }


    InstProp *next = pruntime->tmp_holder;
    for (; next;) {
        // 新增：防御性检查，避免访问非法对象的 tmp_next
        if (next->type != INS_TYPE_OBJECT &&
            next->type != INS_TYPE_ARRAY &&
            next->type != INS_TYPE_CLASS) {
            jvm_printf("[ERROR] Non-InstProp object found in tmp_holder: %p\n", next);
            next = next->tmp_next; // 跳过非法对象
            continue;
        }
        arraylist_push_back_unsafe(g_jvm->collector->runtime_refer_copy, next);
        next = next->tmp_next; // L647 原问题行
    }

    s32 i;
    for (i = 0; i < g_jvm->thread_list->length; i++) {
        JThreadRuntime *runtime = arraylist_get_value(g_jvm->thread_list, i);

        s32 j, size;
        StackFrame *frame = runtime->tail;
        while (frame) {
            const RStackItem *rstack = frame->rstack;
            MethodRaw *raw = get_methodraw_by_index(frame->methodRawIndex);
            JClass *clazz = get_class_by_nameIndex(raw->class_name);
            if (rstack) {
                s32 size = *frame->spPtr;
//                s32 size = g_methods[frame->methodRawIndex].max_stack;
                for (j = 0; j < size; j++) {
                    if (rstack[j].obj) {
                        //jvm_printf("rstack :%llx\n",(s64)(intptr_t)rstack[j].obj);
                        arraylist_push_back_unsafe(g_jvm->collector->runtime_refer_copy, rstack[j].obj);
                    }
                }
            }
            const RStackItem *rlocal = frame->rlocal;
            if (rlocal) {
                s32 max_local = raw->max_local;
                for (j = 0; j < max_local; j++) {
                    if (rlocal[j].obj) {
                        //jvm_printf("rlocal :%llx\n",(s64)(intptr_t)rlocal[j].obj);
                        arraylist_push_back_unsafe(g_jvm->collector->runtime_refer_copy, rlocal[j].obj);
                    }
                }
            }
            frame = frame->next;
        }
    }
    //jvm_printf("[%llx] notified\n", (s64) (intptr_t) pruntime->threadInfo->jthread);
    return 0;
}


static inline void _instance_mark_refer(InstProp *ins, u8 flag_cnt) {
    s32 i, len;
    JClass *clazz = ins->clazz;
    while (clazz) {
        ArrayList *fiList = clazz->fields;
        for (i = 0, len = fiList->length; i < len; i++) {
            FieldInfo *fi = arraylist_get_value_unsafe(fiList, i);
            if (fi->is_ref_target && ins->is_weakreference) {
                continue;
            }
            if (!fi->is_static && fi->is_refer) {
                __refer ref = *((__refer *) (((c8 *) ins) + fi->offset_ins));
                if (ref)_garbage_mark_object(ref, flag_cnt);
            }
        }
        clazz = getSuperClass(clazz);
    }
}


static inline void _jarray_mark_refer(InstProp *arr, u8 flag_cnt) {
//        if (utf8_equals_c(arr->mb.clazz->name, "[[D")) {
//            jvm_printf("check %llx\n", (s64) (intptr_t) arr);
//        }
    if (isDataReferByIndex(arr->arr_type)) {
        s32 i;
        for (i = 0; i < arr->arr_length; i++) {//把所有引用去除，否则不会垃圾回收
            __refer val = arr->as_obj_arr[i];
            if (val)_garbage_mark_object(val, flag_cnt);
        }
    }
    return;
}

/**
 * mark class static field is used
 * @param clazz class
 */
static inline void _class_mark_refer(InstProp *ins, u8 flag_cnt) {
    JClass *clazz = (JClass *) ins;
    s32 i, len;
    ArrayList *fiList = ((JClass *) clazz)->fields;
    for (i = 0, len = fiList->length; i < len; i++) {
        FieldInfo *fi = arraylist_get_value_unsafe(fiList, i);
        if (fi->is_static && fi->is_refer) {
            __refer ref = *((__refer *) ((c8 *) ins->members + fi->offset_ins));
            if (ref)_garbage_mark_object(ref, flag_cnt);
        }
    }
}


/**
 * 递归标注obj所有的子孙
 * @param ref addr
 */

void _garbage_mark_object(__refer ref, u8 flag_cnt) {
    if (ref) {
        InstProp *mb = (InstProp *) ref;
        if (g_jvm->collector->mark_cnt != mb->garbage_mark) {
            mb->garbage_mark = flag_cnt;
            switch (mb->type) {
                case INS_TYPE_OBJECT:
                    _instance_mark_refer(mb, flag_cnt);
                    break;
                case INS_TYPE_ARRAY:
                    _jarray_mark_refer(mb, flag_cnt);
                    break;
                case INS_TYPE_CLASS:
                    _class_mark_refer(mb, flag_cnt);
                    break;
            }
        }
    }
}
//=================================  reg unreg ==================================

InstProp *gc_is_alive(__refer ref) {
    __refer result = hashtable_get(g_jvm->collector->objs_holder, ref);
    spin_lock(&g_jvm->collector->lock);
    if (!result) {
        InstProp *mb = g_jvm->collector->header;
        while (mb) {
            if (mb == ref) {
                result = mb;
                break;
            }
            mb = mb->next;
        }
    }
    if (!result) {
        InstProp *mb = g_jvm->collector->tmp_header;
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
        for (i = 0; i < g_jvm->thread_list->length; i++) {
            JThreadRuntime *runtime = arraylist_get_value(g_jvm->thread_list, i);
            InstProp *mb = runtime->objs_header;
            while (mb) {
                if (mb == ref) {
                    result = mb;
                    break;
                }
                mb = mb->next;
            }
        }
    }

    spin_unlock(&g_jvm->collector->lock);
    return (InstProp *) result;
}

/**
 * 把对象放到线程的对象列表中
 * @param runtime
 * @param ref
 */
void gc_refer_reg(JThreadRuntime *runtime, __refer ref) {
    if (!ref) return;
    InstProp *mb = (InstProp *) ref;
    if (mb->type != INS_TYPE_OBJECT && mb->type != INS_TYPE_ARRAY && mb->type != INS_TYPE_CLASS) {
        return;  // 不是有效的Java对象，不注册
    }
    if (!mb->garbage_reg) {
        mb->garbage_reg = 1;
        mb->next = runtime->objs_header;
        runtime->objs_header = ref;
        if (!runtime->objs_tailer) {
            runtime->objs_tailer = ref;
        }
        runtime->objs_heap_of_thread += mb->heap_size;
#if PRJ_DEBUG_GARBAGE_DUMP
        Utf8String *sus = utf8_create();
        _getMBName(mb, sus);
        jthreadruntime_get_stacktrack(runtime, sus);
        jvm_printf("R: [%llx]%s\n", (s64) (intptr_t) mb, utf8_cstr(sus));
        utf8_destory(sus);
#endif
    }
}

/**
 * 把线程持有的所有对象,链接到gc的链表中去
 * move thread 's jobject to garbage
 * @param runtime
 */
void gc_move_refer_thread_2_gc(JThreadRuntime *runtime) {
    if (runtime) {

        if (runtime->objs_header) {
            //lock
            spin_lock(&g_jvm->collector->lock);

            runtime->objs_tailer->next = g_jvm->collector->tmp_header;
            if (!g_jvm->collector->tmp_tailer) {
                g_jvm->collector->tmp_tailer = runtime->objs_tailer;
            }
            g_jvm->collector->tmp_header = runtime->objs_header;
            g_jvm->collector->obj_heap_size += runtime->objs_heap_of_thread;
            runtime->objs_heap_of_thread = 0;
            spin_unlock(&g_jvm->collector->lock);

            runtime->objs_header = NULL;
            runtime->objs_tailer = NULL;
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


// 原函数：instance_hold_to_thread
void instance_hold_to_thread(JThreadRuntime *runtime, __refer ref) {
    if (!ref) return; // 空指针直接返回
    InstProp *ins = (InstProp *) ref;
    // 新增：校验对象类型是否为合法的 Java 对象
    if (ins->type != INS_TYPE_OBJECT &&
        ins->type != INS_TYPE_ARRAY &&
        ins->type != INS_TYPE_CLASS) {
        jvm_printf("[ERROR] Non-InstProp object held in thread tmp_holder: %p\n", ref);
        return;
    }
    if (runtime) {
        ins->tmp_next = runtime->tmp_holder;
        runtime->tmp_holder = ins;
    }
}

void instance_release_from_thread(JThreadRuntime *runtime, __refer ref) {
    InstProp *ins = (InstProp *) ref;
    if (ins) {
        if (ins == runtime->tmp_holder) {
            runtime->tmp_holder = ins->tmp_next;
            return;
        }
        InstProp *next, *pre;
        pre = runtime->tmp_holder;
        if (pre) {
            next = pre->tmp_next;

            while (next) {
                if (ins == next) {
                    pre->tmp_next = next->tmp_next;
                    return;
                }
                pre = next;
                next = next->tmp_next;
            }
        }
    }
}