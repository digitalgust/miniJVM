
#include <errno.h>
#include "jvm.h"
#include "garbage.h"
#include "jvm_util.h"

s32 _gc_thread_run(void *para);

void _dump_refer(GcCollector *collector);

void _gc_get_obj_name(GcCollector *collector, void *memblock, Utf8String *name);

void _garbage_clear(GcCollector *collector);

void _gc_copy_objs(MiniJVM *jvm);

s32 _gc_big_search(GcCollector *collector);

s32 _gc_pause_the_world(MiniJVM *jvm);

s32 _gc_resume_the_world(MiniJVM *jvm);

s32 _gc_wait_thread_suspend(MiniJVM *jvm, Runtime *runtime);

void _gc_mark_object(GcCollector *collector, __refer ref, u8 flag_cnt);

s32 _gc_copy_objs_from_thread(Runtime *pruntime);

s64 _garbage_collect(GcCollector *collector);

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


s32 gc_create(MiniJVM *jvm) {
    GcCollector *collector = jvm_calloc(sizeof(GcCollector));
    jvm->collector = collector;
    collector->jvm = jvm;
    collector->objs_holder = hashset_create();

    collector->runtime_refer_copy = arraylist_create(256);

    collector->runtime = runtime_create(jvm);

    collector->_garbage_thread_status = GARBAGE_THREAD_PAUSE;
    thread_lock_init(&jvm->threadlock);

    collector->lastgc = currentTimeMillis();

    s32 rc = thrd_create(&collector->garbage_thread, _gc_thread_run, collector);
    if (rc != thrd_success) {
        jvm_printf("ERROR: garbage thread can't create is %d\n", rc);
    } else {
        //启动垃圾回收
        gc_resume(collector);
    }
    return 0;
}

void gc_destory(MiniJVM *jvm) {
    GcCollector *collector = jvm->collector;
    gc_stop(collector);
    vm_share_lock(jvm);
    while (collector->_garbage_thread_status != GARBAGE_THREAD_DEAD) {
        vm_share_timedwait(jvm, 50);
    }
    vm_share_unlock(jvm);
    //
    _garbage_clear(collector);
    //
    hashset_destory(collector->objs_holder);
    collector->objs_holder = NULL;

    arraylist_destory(collector->runtime_refer_copy);

    //
    runtime_destory(collector->runtime);
    jvm_free(collector);
    jvm->collector = NULL;
}


void _garbage_clear(GcCollector *collector) {
#if _JVM_DEBUG_LOG_LEVEL > 1
    jvm_printf("[INFO]garbage clear start\n");
    jvm_printf("[INFO]objs size :%lld\n", collector->obj_count);
#endif
    MiniJVM *jvm = collector->jvm;

    //解除所有引用关系后，回收全部对象
    while (_garbage_collect(collector));//collect instance

#if _JVM_DEBUG_LOG_LEVEL > 1
    jvm_printf("[INFO]objs size :%lld\n", collector->obj_count);
    jvm_printf("[INFO]clear class static field \n");
#endif
    hashset_clear(collector->objs_holder);
    classloaders_clear_all_static(jvm);
    while (_garbage_collect(collector));//collect classes

#if _JVM_DEBUG_LOG_LEVEL > 1
    jvm_printf("[INFO]objs size :%lld\n", collector->obj_count);
    jvm_printf("[INFO]clear boot classes \n");
#endif
    classloader_remove_all_class(jvm->boot_classloader);
    while (_garbage_collect(collector));//collect classes
    classloaders_destroy_all(jvm);

    //
#if _JVM_DEBUG_LOG_LEVEL > 1
    jvm_printf("[INFO]objs size :%lld\n", collector->obj_count);
#endif
    //dump_refer();
}


void gc_pause(GcCollector *collector) {
    vm_share_lock(collector->jvm);
    collector->_garbage_thread_status = GARBAGE_THREAD_PAUSE;
    vm_share_unlock(collector->jvm);
}

void gc_resume(GcCollector *collector) {
    vm_share_lock(collector->jvm);
    collector->_garbage_thread_status = GARBAGE_THREAD_NORMAL;
    vm_share_unlock(collector->jvm);
}

void gc_stop(GcCollector *collector) {
    vm_share_lock(collector->jvm);
    collector->_garbage_thread_status = GARBAGE_THREAD_STOP;
    vm_share_unlock(collector->jvm);
}

//===============================   inner  ====================================

//=============================   debug ===================================

void _gc_get_obj_name(GcCollector *collector, void *memblock, Utf8String *name) {

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
void _dump_refer(GcCollector *collector) {
    //jvm_printf("%d\n",sizeof(struct _Hashset));
    MemoryBlock *mb = collector->tmp_header;
    while (mb) {
        Utf8String *name = utf8_create();
        _gc_get_obj_name(collector, mb, name);
        jvm_printf("   %s[%llx] \n", utf8_cstr(name), (s64) (intptr_t) mb);
        utf8_destory(name);
        mb = mb->next;
    }
}

void gc_dump_runtime(GcCollector *collector) {
    s32 i;
    //jvm_printf("thread set size:%d\n", thread_list->length);
    Utf8String *name = utf8_create();
    for (i = 0; i < collector->jvm->thread_list->length; i++) {
        Runtime *runtime = threadlist_get(collector->jvm, i);

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
                    _gc_get_obj_name(collector, ref, name);
                    jvm_printf("   %s[%llx] \n", utf8_cstr(name), (s64) (intptr_t) ref);
                    utf8_destory(name);
                }
            }
        }

    }
}


s64 gc_sum_heap(GcCollector *collector) {
    s64 hsize = threadlist_sum_heap(collector->jvm);
    spin_lock(&collector->lock);
    {
        hsize += collector->obj_heap_size;
    }
    spin_unlock(&collector->lock);
    return hsize;
}
//==============================   thread_run() =====================================

s32 _gc_thread_run(void *para) {
    GcCollector *collector = (GcCollector *) para;
    MiniJVM *jvm = collector->jvm;

    while (1) {

        s64 cur_mil = currentTimeMillis();

        if (collector->_garbage_thread_status == GARBAGE_THREAD_STOP) {
            break;
        }
        if (collector->_garbage_thread_status == GARBAGE_THREAD_PAUSE) {
            continue;
        }

        s64 heap = gc_sum_heap(collector);
        if (cur_mil - collector->lastgc > jvm->garbage_collect_period_ms || heap >= jvm->max_heap_size * jvm->heap_overload_percent / 100) {
            _garbage_collect(collector);
            collector->lastgc = cur_mil;
        } else {
            threadSleep(1000);
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
s64 _garbage_collect(GcCollector *collector) {
    collector->isgc = 1;
    s64 mem_total = 0, mem_free = 0;
    s64 del = 0;
    s64 time, start;
    MiniJVM *jvm = collector->jvm;

    start = time = currentTimeMillis();

    //prepar gc resource ,
    vm_share_lock(jvm);
    {
        if (_gc_pause_the_world(jvm) != 0) {
            _gc_resume_the_world(jvm);
            vm_share_unlock(jvm);
            jvm_printf("gc canceled ");
            return -1;
        }
        collector->isworldstoped = 1;
        //jvm_printf("garbage_pause_the_world %lld\n", (currentTimeMillis() - time));
        //time = currentTimeMillis();
        if (collector->tmp_header) {
            collector->tmp_tailer->next = collector->header;//接起来
            collector->header = collector->tmp_header;
            collector->tmp_header = NULL;
            collector->tmp_tailer = NULL;
        }
        //jvm_printf("garbage_move_cache %lld\n", (currentTimeMillis() - time));
        //time = currentTimeMillis();
        _gc_copy_objs(jvm);
        //
        //jvm_printf("garbage_copy_refer %lld\n", (currentTimeMillis() - time));
        //time = currentTimeMillis();
        //real GC start
        //
        collector->mark_cnt++;
        if (collector->mark_cnt == 0) {
            collector->mark_cnt = 1;
        }
        _gc_big_search(collector);
        //
        //jvm_printf("garbage_big_search %lld\n", (currentTimeMillis() - time));
        //time = currentTimeMillis();

        collector->isworldstoped = 0;
        _gc_resume_the_world(jvm);
    }
    vm_share_unlock(jvm);

//    jvm_printf("garbage_resume_the_world %lld\n", (currentTimeMillis() - time));

    s64 time_stopWorld = currentTimeMillis() - start;
    time = currentTimeMillis();
    //


    MemoryBlock *head = NULL;//find all finalize obj and weak obj
    MemoryBlock *tail = NULL;

    MemoryBlock *nextmb = collector->header;
    MemoryBlock *curmb, *prevmb = NULL;
    //finalize
    if (collector->_garbage_thread_status == GARBAGE_THREAD_NORMAL) {
        while (nextmb) {
            curmb = nextmb;
            nextmb = curmb->next;
            if (curmb->type == MEM_TYPE_INS) {
                //execute finalize() method
                if (curmb->clazz->finalizeMethod) {// there is a method called finalize
                    if (curmb->garbage_mark != collector->mark_cnt && !GCFLAG_FINALIZED_GET(curmb->gcflag)) {
                        instance_finalize((Instance *) curmb, collector->runtime);
                        if (!head) {
                            head = tail = curmb;
//                            if (curmb->tmp_next) {
//                                s32 debug = 1;
//                            }
                            curmb->tmp_next = NULL;
                        } else {
                            curmb->tmp_next = head;
                            head = curmb;
                        }
                        GCFLAG_FINALIZED_SET(curmb->gcflag);
                    }
                }
                //process weakreference
                if (GCFLAG_WEAKREFERENCE_GET(curmb->gcflag)) {//is weakreference
                    Instance *target = getFieldRefer(getInstanceFieldPtr((Instance *) curmb, jvm->shortcut.reference_target));
                    //jvm_printf("weak reference : %llx %s, %d\n", (s64) (intptr_t) curmb, utf8_cstr(target->mb.clazz->name), curmb->garbage_mark);
                    if (target && target->mb.garbage_mark != collector->mark_cnt) {
                        instance_of_reference_enqueue((Instance *) curmb, collector->runtime);
                        if (!head) {
                            head = tail = curmb;
//                            if (curmb->tmp_next) {
//                                s32 debug = 1;
//                            }
                            curmb->tmp_next = NULL;
                        } else {
                            curmb->tmp_next = head;
                            head = curmb;
                        }
                    }
                }
            }
        }

        //re mark these obj
        nextmb = head;
        while (nextmb) {
            curmb = nextmb;
            nextmb = curmb->tmp_next;
            _gc_mark_object(collector, curmb, collector->mark_cnt);//mark it collect on next time
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
#if _JVM_DEBUG_GARBAGE_DUMP
            Utf8String *sus = utf8_create();
            _gc_get_obj_name(collector, curmb, sus);
            jvm_printf("X: %s[%llx]\n", utf8_cstr(sus), (s64) (intptr_t) curmb);
            utf8_destory(sus);
#endif
            if (curmb->type == MEM_TYPE_CLASS) {
                classes_remove(collector->jvm, (JClass *) curmb);
            }
            if (curmb->clazz->is_jcloader) {
                PeerClassLoader *pcl = classLoaders_find_by_instance(jvm, (Instance *) curmb);
                if (pcl) {
                    classloaders_remove(jvm, pcl);
                    classloader_destory(pcl);
                }
            }
            memoryblock_destory(curmb);
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

#if _JVM_DEBUG_LOG_LEVEL > 1
    s64 time_gc = currentTimeMillis() - time;
    jvm_printf("[INFO]gc obj: %lld->%lld   heap : %lld -> %lld  stop_world: %lld  gc:%lld\n", iter, collector->obj_count, mem_total, collector->obj_heap_size, time_stopWorld, time_gc);
#endif

#ifdef MEM_ALLOC_LTALLOC
    jvm_squeeze(0);
#endif
    collector->isgc = 0;
    return del;
}


/**
 * 各个线程把自己还需要使用的对象进行标注，表示不能回收
 */
static void _list_iter_thread_pause(ArrayListValue value, void *para) {
    jthread_suspend((Runtime *) value);
}

s32 _gc_pause_the_world(MiniJVM *jvm) {
    GcCollector *collector = jvm->collector;
    ArrayList *thread_list = jvm->thread_list;
    s32 i;
    //jvm_printf("thread size:%d\n", thread_list->length);
    if (thread_list->length) {
        arraylist_iter_safe(thread_list, _list_iter_thread_pause, NULL);

        for (i = 0; i < thread_list->length; i++) {
            Runtime *runtime = arraylist_get_value(thread_list, i);
            if (_gc_wait_thread_suspend(jvm, runtime) == -1) {
                return -1;
            }
            gc_move_objs_thread_2_gc(runtime);

#if _JVM_DEBUG_GARBAGE_DUMP
            Utf8String *stack = utf8_create();
            getRuntimeStack(runtime, stack);
            jvm_printf("%s\n", utf8_cstr(stack));
            utf8_destory(stack);
#endif
        }

    }
    gc_move_objs_thread_2_gc(collector->runtime);// maybe someone new object in finalize...

    return 0;
}

s32 _gc_resume_the_world(MiniJVM *jvm) {
    GcCollector *collector = jvm->collector;
    ArrayList *thread_list = jvm->thread_list;
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
            vm_share_notifyall(jvm);
        }
    }

    return 0;
}


s32 _gc_wait_thread_suspend(MiniJVM *jvm, Runtime *runtime) {
#if _JVM_DEBUG_LOG_LEVEL > 1
    if (runtime->thrd_info->is_blocking) {
        s32 debug = 1;
        Runtime *r = getLastSon(runtime);
        jvm_printf("blocking on: %s.%s\n", utf8_cstr(r->method->_this_class->name), utf8_cstr(r->method->name));
        if (!(utf8_equals_c(r->method->name, "wait") || utf8_equals_c(r->method->name, "sleep"))) {
            s32 debug = 1;
        }
    }
#endif
    while (!(runtime->thrd_info->is_suspend) &&
           !(runtime->thrd_info->is_blocking)) { //
        vm_share_notifyall(jvm);
        vm_share_timedwait(jvm, 1);
        if (jvm->collector->_garbage_thread_status != GARBAGE_THREAD_NORMAL) {
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
s32 _gc_big_search(GcCollector *collector) {
    //thread stack frame mark
    s32 i, len;
    for (i = 0, len = collector->runtime_refer_copy->length; i < len; i++) {
        __refer r = arraylist_get_value(collector->runtime_refer_copy, i);
        _gc_mark_object(collector, r, collector->mark_cnt);
    }

    //holder mark
    HashsetIterator hi;
    hashset_iterate(collector->objs_holder, &hi);
    while (hashset_iter_has_more(&hi)) {
        HashsetKey k = hashset_iter_next_key(&hi);
        _gc_mark_object(collector, k, collector->mark_cnt);
    }
    //bootclassloader mark
    HashtableIterator hti;
    hashtable_iterate(collector->jvm->boot_classloader->classes, &hti);
    while (hashtable_iter_has_more(&hti)) {
        HashtableValue v = hashtable_iter_next_value(&hti);
        _gc_mark_object(collector, v, collector->mark_cnt);
    }

    return 0;
}

void _gc_copy_objs(MiniJVM *jvm) {
    arraylist_clear(jvm->collector->runtime_refer_copy);
    s32 i;
    //jvm_printf("thread set size:%d\n", thread_list->length);
    for (i = 0; i < jvm->thread_list->length; i++) {
        Runtime *runtime = threadlist_get(jvm, i);
        _gc_copy_objs_from_thread(runtime);
    }
//    arraylist_iter_safe(thread_list, _list_iter_iter_copy, NULL);
    //调试线程
    if (jvm->jdwp_enable && jvm->jdwpserver) {
        Runtime *runtime = jdwp_get_runtime(jvm->jdwpserver);
        if (runtime) {
            _gc_copy_objs_from_thread(runtime);
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


s32 _gc_copy_objs_from_thread(Runtime *pruntime) {
    GcCollector *collector = pruntime->jvm->collector;
    arraylist_push_back_unsafe(collector->runtime_refer_copy, pruntime->thrd_info->jthread);

    Runtime *runtime = pruntime;
    RuntimeStack *stack = runtime->stack;

//    jvm_printf("stack:%llx:", (s64) (intptr_t) stack);
//    s32 j;
//    for (j = stack_size(stack); j < stack->max_size; j++) {
//        if ((stack->store + j)->rvalue) {
//            jvm_printf("%d->%llx,", j, (s64) (intptr_t) (stack->store + j)->rvalue);
//        }
//    }
//    jvm_printf("\n");

    //reset free stack space
    stack->gc_clean = stack->sp;
    memset(stack->sp, 0, sizeof(StackEntry) * (stack->max_size - stack_size(stack)));

    s32 i, imax;
    StackEntry *entry;
    for (i = 0, imax = stack_size(stack); i < imax; i++) {
        entry = stack->store + i;
        if (entry->rvalue) {
            arraylist_push_back_unsafe(collector->runtime_refer_copy, entry->rvalue);
        }
    }
    MemoryBlock *next = runtime->thrd_info->tmp_holder;
    for (; next;) {
        arraylist_push_back_unsafe(collector->runtime_refer_copy, next);
        next = next->hold_next;
    }

    //jvm_printf("[%llx] notified\n", (s64) (intptr_t) pruntime->threadInfo->jthread);
    return 0;
}


static inline void _gc_instance_mark(GcCollector *collector, Instance *ins, u8 flag_cnt) {
    s32 i, len;
    JClass *clazz = ins->mb.clazz;
    while (clazz) {
        FieldPool *fp = &clazz->fieldPool;
        ArrayList *fiList = clazz->insFieldPtrIndex;
        for (i = 0, len = fiList->length; i < len; i++) {
            FieldInfo *fi = arraylist_get_value_unsafe(fiList, i);

            if (fi->is_ref_target && GCFLAG_WEAKREFERENCE_GET(ins->mb.gcflag)) continue;//skip weakreference target mark, but others mark need
            c8 *ptr = getInstanceFieldPtr(ins, fi);
            if (ptr) {
                __refer ref = getFieldRefer(ptr);
                if (ref)_gc_mark_object(collector, ref, flag_cnt);
            }
            _gc_mark_object(collector, fi->_this_class, flag_cnt);
        }
        _gc_mark_object(collector, clazz, flag_cnt);//keep class and classloader alive
        clazz = getSuperClass(clazz);
    }

    if (GCFLAG_JLOADER_GET(ins->mb.gcflag)) {//if the instance is created from java.lang.ClassLoader
        PeerClassLoader *pcl = classLoaders_find_by_instance(collector->jvm, ins);
        if (pcl) {// pcl maybe finalized
            HashtableIterator hi;
            hashtable_iterate(pcl->classes, &hi);
            while (hashtable_iter_has_more(&hi)) {
                HashtableValue v = hashtable_iter_next_value(&hi);
                _gc_mark_object(collector, v, flag_cnt);
            }
        }
    }
}


static inline void _gc_jarray_mark(GcCollector *collector, Instance *arr, u8 flag_cnt) {
    if (arr && arr->mb.type == MEM_TYPE_ARR) {
//        if (utf8_equals_c(arr->mb.clazz->name, "[[D")) {
//            jvm_printf("check %llx\n", (s64) (intptr_t) arr);
//        }
        if (isDataReferByIndex(arr->mb.arr_type_index)) {
            s32 i;
            for (i = 0; i < arr->arr_length; i++) {//把所有引用去除，否则不会垃圾回收
                s64 val = jarray_get_field(arr, i);
                if (val)_gc_mark_object(collector, (__refer) (intptr_t) val, flag_cnt);
            }
        }
    }
    return;
}

/**
 * mark class static field is used
 * @param clazz class
 */
static inline void _gc_class_mark(GcCollector *collector, JClass *clazz, u8 flag_cnt) {
    s32 i, len;
    if (clazz->field_static) {
        FieldPool *fp = &clazz->fieldPool;
        ArrayList *fiList = clazz->staticFieldPtrIndex;
        for (i = 0, len = fiList->length; i < len; i++) {
            FieldInfo *fi = arraylist_get_value_unsafe(fiList, i);
            c8 *ptr = getStaticFieldPtr(fi);
            if (ptr) {
                __refer ref = getFieldRefer(ptr);
                _gc_mark_object(collector, ref, flag_cnt);
            }
        }
    }
    if (clazz->ins_class) {
        _gc_mark_object(collector, clazz->ins_class, flag_cnt);
    }
    if (clazz->jloader) {
        _gc_mark_object(collector, clazz->jloader, flag_cnt);
    }
}


/**
 * 递归标注obj所有的子孙
 * @param ref addr
 */

void _gc_mark_object(GcCollector *collector, __refer ref, u8 flag_cnt) {
    if (ref) {
        MemoryBlock *mb = (MemoryBlock *) ref;
        if (flag_cnt != mb->garbage_mark) {
            mb->garbage_mark = flag_cnt;
            switch (mb->type) {
                case MEM_TYPE_INS:
                    _gc_instance_mark(collector, (Instance *) mb, flag_cnt);
                    break;
                case MEM_TYPE_ARR:
                    _gc_jarray_mark(collector, (Instance *) mb, flag_cnt);
                    break;
                case MEM_TYPE_CLASS:
                    _gc_class_mark(collector, (JClass *) mb, flag_cnt);
                    break;
            }
        }
    }
}
//=================================  reg unreg ==================================

s32 _gc_is_alive_in_link(MemoryBlock *header, MemoryBlock *target) {
    MemoryBlock *mb = header;
    while (mb) {
        if (mb == target) {
            return 1;
        }
        mb = mb->next;
    }
    return 0;
}

MemoryBlock *gc_is_alive(GcCollector *collector, __refer ref) {
    __refer result = hashset_get(collector->objs_holder, ref);
    if (result)return ref;

    MiniJVM *jvm = collector->jvm;
    spin_lock(&collector->lock);
    if (!result) {
        MemoryBlock *mb = collector->header;
        if (_gc_is_alive_in_link(mb, ref)) {
            result = ref; //don't return ,there is a lock need unlock
        }
    }
    if (!result) {
        MemoryBlock *mb = jvm->collector->tmp_header;
        if (_gc_is_alive_in_link(mb, ref)) {
            result = ref;//don't return ,there is a lock need unlock
        }
    }
    if (!result) {
        s32 i;
        for (i = 0; i < jvm->thread_list->length; i++) {
            Runtime *runtime = threadlist_get(jvm, i);
            MemoryBlock *mb = runtime->thrd_info->objs_header;
            if (_gc_is_alive_in_link(mb, ref)) {
                result = ref;//don't return ,there is a lock need unlock
            }
        }
    }
    if (!result) {
        if (jvm->jdwp_enable) {
            Runtime *runtime = jdwp_get_runtime(jvm->jdwpserver);
            if (runtime) {
                MemoryBlock *mb = runtime->thrd_info->objs_header;
                if (_gc_is_alive_in_link(mb, ref)) {
                    result = ref;//don't return ,there is a lock need unlock
                }
            }
        }
    }
    spin_unlock(&jvm->collector->lock);
    return (MemoryBlock *) result;
}

/**
 * 把对象放到线程的对象列表中
 * @param runtime
 * @param ref
 */
void gc_obj_reg(Runtime *runtime, __refer ref) {
    if (!ref)return;
    MemoryBlock *mb = (MemoryBlock *) ref;
    if (!GCFLAG_REG_GET(mb->gcflag)) {
        GCFLAG_REG_SET(mb->gcflag);
        JavaThreadInfo *ti = runtime->thrd_info;
        mb->next = ti->objs_header;
        ti->objs_header = ref;
        if (!ti->objs_tailer) {
            ti->objs_tailer = ref;
        }
        ti->objs_heap_of_thread += mb->heap_size;

#ifdef HARD_LIMIT
        //HARD_LIMIT define will limit heap use alaways less than MAX_HEAP_SIZE
        //but the performance down
        while (MAX_HEAP_SIZE * GARBAGE_OVERLOAD / 100 - collector->obj_heap_size < 0) {
            jthread_block_enter(runtime);
            jthread_block_exit(runtime);//for jthread pause waiting for gc
        }
#endif

#if _JVM_DEBUG_GARBAGE_DUMP
        Utf8String *sus = utf8_create();
        _gc_get_obj_name(runtime->jvm->collector, mb, sus);
        jvm_printf("R: %s[%llx]\n", utf8_cstr(sus), (s64) (intptr_t) mb);
        utf8_destory(sus);
#endif
    } else {
        s32 debug = 1;
    }
}

/**
 * 把线程持有的所有对象,链接到gc的链表中去
 * move thread 's jobject to garbage
 * @param runtime
 */
void gc_move_objs_thread_2_gc(Runtime *runtime) {
    if (runtime) {
        JavaThreadInfo *ti = runtime->thrd_info;
        GcCollector *collector = runtime->jvm->collector;
        if (ti->objs_header) {
            //lock
            spin_lock(&collector->lock);
            {
#if _JVM_DEBUG_GARBAGE_DUMP
                MemoryBlock *mb = ti->objs_header;
                while (mb) {
                    Utf8String *sus = utf8_create();
                    _gc_get_obj_name(runtime->jvm->collector, mb, sus);
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

void gc_obj_hold(GcCollector *collector, __refer ref) {
    if (ref) {
        hashset_put(collector->objs_holder, ref);
    }
}

void gc_obj_release(GcCollector *collector, __refer ref) {
    if (ref) {
        hashset_remove(collector->objs_holder, ref, 0);
    }
}

