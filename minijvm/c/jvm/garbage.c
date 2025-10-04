#include <errno.h>
#include "jvm.h"
#include "garbage.h"
#include "jvm_util.h"

#if __JVM_LTALLOC__

#include "ltalloc.h"

#endif

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

void _gc_print_obj_list(GcCollector *pType);

/**
 * Creates a garbage collection thread.
 *
 * The collection method works as follows:
 * When an object is created, it is registered with the garbage collector and included in its monitoring system.
 *
 * The objects registered include:
 * - Class objects
 * - Instance objects (including array objects)
 *
 * During garbage collection, the garbage collection thread collects objects from the `collector->header` linked list.
 * The collection process involves marking objects that are not referenced by any thread, directly destroying them,
 * and freeing their memory.
 *
 * The garbage collection thread will pause all currently executing Java threads. Once collection is complete,
 * the paused threads are resumed.
 *
 * Runtime objects in the JDWP debugging thread are not eligible for garbage collection.
 *
 * Collection steps:
 *
 * 1. Stop the world (pause all Java threads).
 * 2. Add all register/hold/release operations to a temporary linked list.
 * 3. Copy all runtime references.
 * 4. Mark objects referenced by threads.
 * 5. Resume the world (resume all Java threads).
 * 6. Release the memory of unmarked objects.
 * 7. Move the temporary linked list back to the main list.
 *
 * @return Error code
 */



s32 gc_create(MiniJVM *jvm) {
    GcCollector *collector = jvm_calloc(sizeof(GcCollector));
    jvm->collector = collector;
    collector->jvm = jvm;
    collector->objs_holder = hashset_create();
    collector->objs_2_count = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);

    collector->runtime_refer_copy = arraylist_create(256);

    collector->runtime = runtime_create(jvm);
    collector->runtime->thrd_info->type = THREAD_TYPE_GC;
    collector->_garbage_thread_status = GARBAGE_THREAD_PAUSE;
    thread_lock_init(&jvm->threadlock);

    collector->lastgc = currentTimeMillis();

    s32 rc = thrd_create(&collector->garbage_thread, _gc_thread_run, collector);
    if (rc != thrd_success) {
        jvm_printf("ERROR: garbage thread can't create is %d\n", rc);
    }
    return 0;
}

void gc_destroy(MiniJVM *jvm) {
    GcCollector *collector = jvm->collector;
    gc_stop(collector);
    vm_share_lock(jvm);
    while (collector->_garbage_thread_status != GARBAGE_THREAD_DEAD) {
        vm_share_timedwait(jvm, 100);
    }
    vm_share_unlock(jvm);
    //
    _garbage_clear(collector);
    //
    hashset_destroy(collector->objs_holder);
    collector->objs_holder = NULL;

    arraylist_destroy(collector->runtime_refer_copy);
    hashtable_destroy(collector->objs_2_count);

    //
    runtime_destroy(collector->runtime);
    jvm_free(collector);
    jvm->collector = NULL;
}


void _garbage_clear(GcCollector *collector) {
#if _JVM_DEBUG_LOG_LEVEL > 1
    jvm_printf("[INFO]garbage clear start\n");
    jvm_printf("[INFO]objs size :%lld\n", collector->obj_count);
#endif
    MiniJVM *jvm = collector->jvm;

    //After all references are removed, all objects are collected
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
    while (collector->isgc) {
        vm_share_timedwait(collector->jvm, 100);
    }
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
void _gc_add_obj_count(GcCollector *collector, MemoryBlock *mb) {
    if (mb->type == MEM_TYPE_CLASS) {
        return;
    }
    if (collector->_garbage_thread_status != GARBAGE_THREAD_NORMAL) {
        return;
    }
    if (utf8_equals_c(mb->clazz->name, "javax/microedition/lcdui/Display$SerialCallTimerTask")) {
        s32 debug = 1;
    }
    HashtableValue v = hashtable_get(collector->objs_2_count, mb->clazz->name);
    if (!v) {
        s64 count = 1;
        if (sizeof(void *) == 8) {
            count = count << 32;
            count = count | mb->heap_size;
        }
        hashtable_put(collector->objs_2_count, mb->clazz->name, (HashtableValue) (intptr_t) count);
    } else {
        s64 val = (s64) (intptr_t) v;
        if (sizeof(void *) == 8) {
            s32 pc = (s32) (val >> 32);
            s32 msize = (s32) val;
            pc++;
            val = ((s64) pc) << 32;
            msize += mb->heap_size;
            val = val | msize;
        } else {
            val++;
        }
        hashtable_put(collector->objs_2_count, mb->clazz->name, (HashtableValue) (intptr_t) val);
    }
}


void _gc_print_obj_list(GcCollector *collector) {
    jvm_printf("[INFO]--------------------------types: %d\n", collector->objs_2_count->entries);
    jvm_printf("[INFO] -----count----- ---memo size---  ----name---\n");
    HashtableIterator hti;
    hashtable_iterate(collector->objs_2_count, &hti);
    for (; hashtable_iter_has_more(&hti);) {
        Utf8String *clsName = hashtable_iter_next_key(&hti);
        if (utf8_equals_c(clsName, "javax/microedition/lcdui/Display$SerialCallTimerTask")) {
            s32 debug = 1;
        }
        s64 val = (s64) (intptr_t) hashtable_get(collector->objs_2_count, clsName);
        s32 pc = 0, msize = 0;
        if (sizeof(void *) == 8) {
            pc = (s32) (val >> 32);
            msize = (s32) val;
        } else {
            pc = (s32) val;
        }
        jvm_printf("[INFO] %15d %15d  %s\n", pc, msize, utf8_cstr(clsName));
        //utf8_destroy(clsName);
    }
    hashtable_clear(collector->objs_2_count);
    jvm_printf("[INFO]--------------------------\n");
}
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
 * For debugging: Print all reference information
 */
void _dump_refer(GcCollector *collector) {
    //jvm_printf("%d\n",sizeof(struct _Hashset));
    MemoryBlock *mb = collector->tmp_header;
    while (mb) {
        Utf8String *name = utf8_create();
        _gc_get_obj_name(collector, mb, name);
        jvm_printf("   %s[%llx] \n", utf8_cstr(name), (s64) (intptr_t) mb);
        utf8_destroy(name);
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
                    utf8_destroy(name);
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
/**
 * Garbage Collection Thread Function.
 *
 * @param para A pointer to the GcCollector instance which provides garbage collection context.
 * @return 0 on successful termination of the thread.

 * This function implements a dedicated garbage collection thread that performs the following tasks:
 * 1. Continuously monitors the JVM's heap status and garbage collection parameters.
   *   It uses `currentTimeMillis()` to track time and `gc_sum_heap` to get current heap usage.
 * 2. Checks the garbage collector thread's status (`collector->_garbage_thread_status`) and pauses or terminates as needed.
 * 3. If the specified garbage collection period has elapsed (determined by `jvm->garbage_collect_period_ms`) or the heap usage exceeds a pre-defined threshold (`jvm->max_heap_size * jvm->heap_overload_percent / 100`),
   *   it initiates a garbage collection cycle with `_garbage_collect`.
 * 4. Updates the last garbage collection timestamp (`collector->lastgc`) after successfully completing a GC cycle.
 * 5. Sleeps for 1 second if no garbage collection is required.
 * 6. When signaled to stop, changes the garbage collector thread status to `GARBAGE_THREAD_DEAD` and exits gracefully using `thrd_exit(0)`.
 */

s32 _gc_thread_run(void *para) {
    GcCollector *collector = (GcCollector *) para;
    MiniJVM *jvm = collector->jvm;

    while (1) {

        s64 cur_mil = currentTimeMillis();

        if (collector->_garbage_thread_status == GARBAGE_THREAD_STOP) {
            break;
        }
        if (collector->_garbage_thread_status == GARBAGE_THREAD_PAUSE) {
            threadSleep(1000);
            continue;
        }
        if (cur_mil - collector->lastgc < 2000) {
            threadSleep(1000);
            continue;
        };

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
 * Executes a full garbage collection cycle.
 *
 * @param collector A pointer to the GcCollector instance that manages garbage collection for the JVM.
 * @return The number of objects deleted during this GC cycle.

 * This function performs a complete garbage collection process, including:
 * 1. Initiates garbage collection by setting the `isgc` flag and recording the start time.
 * 2. Pauses the world by stopping all threads and preparing resources for GC using `_gc_pause_the_world`.
   *   If pausing fails, resumes the world and returns -1.
 * 3. Merges temporary object lists into the main list and copies references from runtime stacks.
 * 4. Begins the mark phase with `_gc_big_search`, incrementing the mark counter and marking reachable objects.
 * 5. Processes finalize methods for eligible objects and enqueues weak references for objects no longer reachable.
   *   Re-marks these objects for collection on the next cycle.
 * 6. Clears unreachable objects, updates total memory usage stats, and destroys them.
 * 7. Updates the count of live objects and heap size after clearing dead objects.
 * 8. Logs performance metrics related to GC timing.
 * 9. Resumes the world and unlocks shared resources.
 * 10. Optionally calls `jvm_squeeze` if MEM_ALLOC_LTALLOC is defined.
 * 11. Resets the `isgc` flag and returns the number of deleted objects.
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
            jvm_printf("[WARN] GC canceled - failed to pause the world\n");
            return -1;
        }
        collector->isworldstoped = 1;
#if _JVM_DEBUG_GARBAGE
        jvm_printf("garbage_move_cache %lld\n", (currentTimeMillis() - time));
        time = currentTimeMillis();
#endif
        if (collector->tmp_header) {
            collector->tmp_tailer->next = collector->header;//接起来
            collector->header = collector->tmp_header;
            collector->tmp_header = NULL;
            collector->tmp_tailer = NULL;
        }
#if _JVM_DEBUG_GARBAGE
        jvm_printf("garbage_move_cache %lld\n", (currentTimeMillis() - time));
        time = currentTimeMillis();
#endif
        _gc_copy_objs(jvm);
        //
#if _JVM_DEBUG_GARBAGE
        jvm_printf("garbage_copy_refer %lld\n", (currentTimeMillis() - time));
        time = currentTimeMillis();
#endif
        //real GC start
        //
        collector->mark_cnt++;
        if (collector->mark_cnt == 0) {
            collector->mark_cnt = 1;
        }
        _gc_big_search(collector);
        //
#if _JVM_DEBUG_GARBAGE
        jvm_printf("garbage_big_search %lld\n", (currentTimeMillis() - time));
        time = currentTimeMillis();
#endif

#if _JVM_DEBUG_GARBAGE_DUMP > 0
        _gc_print_obj_list(collector);
#endif

        collector->isworldstoped = 0;
        _gc_resume_the_world(jvm);
    }
    vm_share_unlock(jvm);

#if _JVM_DEBUG_GARBAGE
    jvm_printf("garbage_resume_the_world %lld\n", (currentTimeMillis() - time));
#endif

#if _JVM_DEBUG_LOG_LEVEL > 1
    s64 time_stopWorld = currentTimeMillis() - start;
#endif
    time = currentTimeMillis();
    //


    MemoryBlock *head = NULL;//find all finalize obj and weak obj

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
                            head = curmb;
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
                            head = curmb;
                            curmb->tmp_next = NULL;
                        } else {
                            curmb->tmp_next = head;
                            head = curmb;
                        }
                    } else {
                        s32 debug = 1;
                    }
                }
            }
        }

        //remark this obj
        nextmb = head;
        while (nextmb) {
            curmb = nextmb;
            nextmb = curmb->tmp_next;
            _gc_mark_object(collector, curmb, collector->mark_cnt);//mark it collect on next time
        }
    }

#if _JVM_DEBUG_GARBAGE
    jvm_printf("garbage_finalize %lld\n", (currentTimeMillis() - time));
    time = currentTimeMillis();
#endif
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
#if _JVM_DEBUG_GARBAGE_DUMP > 1
            Utf8String *sus = utf8_create();
            _gc_get_obj_name(collector, curmb, sus);
            jvm_printf("X: %s[%llx]\n", utf8_cstr(sus), (s64) (intptr_t) curmb);
            utf8_destroy(sus);
#endif
            if (curmb->type == MEM_TYPE_CLASS) {
                classes_remove(collector->jvm, (JClass *) curmb);
            } else if (GCFLAG_JLOADER_GET(curmb->gcflag)) {// curmb->class might be destroyed, when gc_destroy() called
#if _JVM_DEBUG_GARBAGE_DUMP > 1
                jvm_printf("X: [%llx] classloader\n", (s64) (intptr_t) curmb);
#endif
                PeerClassLoader *pcl = classLoaders_find_by_instance(jvm, (Instance *) curmb);
#if _JVM_DEBUG_LOG_LEVEL > 1
                jvm_printf("[INFO] [%llx] classloader destroied class:%s\n", (s64) (intptr_t) curmb, utf8_cstr(pcl->jloader->mb.clazz->name));
#endif
                if (pcl) {
                    classloaders_remove(jvm, pcl);
                    classloader_destroy(pcl);
                }
            } else if (GCFLAG_JTHREAD_GET(curmb->gcflag)) {//process thread if it created but not started
                Runtime *ort = jthread_get_stackframe_value(jvm, (Instance *) curmb);
                jthread_run_finalize(ort);
            }
            memoryblock_destroy(curmb);
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
#if __JVM_LTALLOC__
    ltsqueeze(0);
#endif
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
 * Each thread marks the objects it still needs to use, indicating they cannot be collected.
 */
static void _list_iter_thread_pause(ArrayListValue value, void *para) {
#if _JVM_DEBUG_LOG_LEVEL > 1
    print_runtime_stack((Runtime *) value);
#endif
    jthread_suspend((Runtime *) value);
}

s32 _gc_pause_the_world(MiniJVM *jvm) {
    GcCollector *collector = jvm->collector;
    ArrayList *thread_list = jvm->thread_list;
    s32 i;
    //jvm_printf("thread size:%d\n", thread_list->length);
    if (thread_list->length) {
        arraylist_iter_safe(thread_list, _list_iter_thread_pause, NULL);

        //此处可能存在多线程交互，比如某个线程结束等情况，导致for错误
        spin_lock(&thread_list->spinlock);
        {
            for (i = 0; i < thread_list->length; i++) {
                Runtime *runtime = arraylist_get_value(thread_list, i);
                if (_gc_wait_thread_suspend(jvm, runtime) == -1) {
                    return -1;
                }
                gc_move_objs_thread_2_gc(runtime);

#if _JVM_DEBUG_GARBAGE_DUMP > 1
                Utf8String *stack = utf8_create();
                getRuntimeStack(runtime, stack);
                jvm_printf("%s\n", utf8_cstr(stack));
                utf8_destroy(stack);
#endif
            }
        }
        spin_unlock(&thread_list->spinlock);
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
#if _JVM_DEBUG_GARBAGE_DUMP > 1
            Utf8String *stack = utf8_create();
            getRuntimeStack(runtime, stack);
            jvm_printf("%s\n", utf8_cstr(stack));
            utf8_destroy(stack);
#endif
            jthread_resume(runtime);
            vm_share_notifyall(jvm);
        }
    }

    return 0;
}


s32 _gc_wait_thread_suspend(MiniJVM *jvm, Runtime *runtime) {
    if (runtime->thrd_info->thread_status == THREAD_STATUS_NEW || runtime->thrd_info->thread_status == THREAD_STATUS_ZOMBIE) {
        return 0;
    }
    while (!(runtime->thrd_info->is_suspend ||  /// While executing bytecode, if suspend_count is not 0, pause bytecode execution and set is_suspend = 1
             runtime->thrd_info->is_blocking)  // During certain IO waits, JNI sets is_blocking = 1
            ) { //
        vm_share_notifyall(jvm);
        vm_share_timedwait(jvm, 100);
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
        if (runtime->thrd_info->thread_status != THREAD_STATUS_ZOMBIE) {//zombie thread is not need to mark
            _gc_copy_objs_from_thread(runtime);
        } else {
            s32 debug = 1;
        }
    }
//    arraylist_iter_safe(thread_list, _list_iter_iter_copy, NULL);
    // Debug thread
    if (jvm->jdwp_enable && jvm->jdwpserver) {
        Runtime *runtime = jdwp_get_runtime(jvm->jdwpserver);
        if (runtime) {
            _gc_copy_objs_from_thread(runtime);
        }
    }
}

/**
 * Copies object references from a thread's runtime stack and temporary holder for garbage collection.
 *
 * @param pruntime A pointer to the Runtime instance representing the current thread.
 * @return 0 on successful completion.

 * This function performs the following actions:
 * 1. Retrieves the GcCollector instance associated with the JVM.
 * 2. Adds the thread's JavaThreadInfo (`pruntime->thrd_info->jthread`) to the collector's runtime reference copy list (`collector->runtime_refer_copy`).
 * 3. Resets the free stack space by marking the current stack pointer as the clean point, and clears unused stack entries.
 * 4. Iterates over the stack of the given thread's Runtime and adds each non-null reference value found in the StackEntry to the reference copy list.
 * 5. Traverses the temporary holder list (`runtime->thrd_info->tmp_holder`) for the thread and adds each MemoryBlock to the reference copy list.
 * 6. Upon completion, it signals that the references have been copied and are ready for garbage collection.
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
            for (i = 0; i < arr->arr_length; i++) {// Remove all references; otherwise, garbage collection will not occur.
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
 * Recursively mark all descendants of the object.
 * @param ref Address of the object
 */

void _gc_mark_object(GcCollector *collector, __refer ref, u8 flag_cnt) {
    if (ref) {
        MemoryBlock *mb = (MemoryBlock *) ref;
        if (flag_cnt != mb->garbage_mark) {

#if _JVM_DEBUG_GARBAGE_DUMP > 0
            _gc_add_obj_count(collector, mb);
#endif
//            if (utf8_equals_c(mb->clazz->name, "com/ebsee/shl/main/GamePanel")) {
//                s32 debug = 1;
//            }

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
 * Add the object to the thread's object list.
 * @param runtime Runtime environment
 * @param ref Reference to the object
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

#if _JVM_DEBUG_GARBAGE_DUMP > 1
        Utf8String *sus = utf8_create();
        _gc_get_obj_name(runtime->jvm->collector, mb, sus);
        getRuntimeStackWithOutReturn(runtime, sus);
        jvm_printf("R: [%llx]%s\n", (s64) (intptr_t) mb, utf8_cstr(sus));
        utf8_destroy(sus);
#endif
    } else {
        s32 debug = 1;
    }
}

/**
 * Link all objects held by the thread to the GC's linked list.
 * Move the thread's jobject to garbage collection.
 * @param runtime Runtime environment
 */
void gc_move_objs_thread_2_gc(Runtime *runtime) {
    if (runtime) {
        JavaThreadInfo *ti = runtime->thrd_info;
        GcCollector *collector = runtime->jvm->collector;
        //lock
        spin_lock(&collector->lock);
        {
            if (ti->objs_header) {
#if _JVM_DEBUG_GARBAGE_DUMP > 1
                MemoryBlock *mb = ti->objs_header;
                while (mb) {
                    Utf8String *sus = utf8_create();
                    _gc_get_obj_name(runtime->jvm->collector, mb, sus);
                    jvm_printf("M: %s[%llx]\n", utf8_cstr(sus), (s64) (intptr_t) mb);
                    utf8_destroy(sus);
                    mb = mb->next;
                }
#endif
                ti->objs_tailer->next = collector->tmp_header;
                if (!collector->tmp_tailer) {
                    collector->tmp_tailer = ti->objs_tailer;
                }
                collector->tmp_header = ti->objs_header;
                collector->obj_heap_size += ti->objs_heap_of_thread;

                ti->objs_header = NULL;
                ti->objs_tailer = NULL;

                ti->objs_heap_of_thread = 0;
            }
        }
        spin_unlock(&collector->lock);
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

