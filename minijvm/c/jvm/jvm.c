#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include "jvm.h"
#include "garbage.h"
#include "jvm_util.h"
#include "jdwp.h"


void thread_boundle(Runtime *runtime) {
    JClass *thread_clazz = classes_load_get_with_clinit_c(NULL, STR_CLASS_JAVA_LANG_THREAD, runtime);
    //create jthread for main thread
    Instance *t = instance_create(runtime, thread_clazz);
    instance_hold_to_thread(t, runtime);
    runtime->thrd_info->jthread = t; //Thread.init currentThread() need this
    //runtime->clazz = thread_clazz;
    instance_init(t, runtime);
    //destroy old runtime
    Runtime *r = jthread_get_stackframe_value(runtime->jvm, t);
    if (r) {
        gc_move_objs_thread_2_gc(r);
        runtime_destroy(r);
        jthread_set_stackframe_value(runtime->jvm, t, NULL);
    }
    //bind new runtime
    jthread_set_stackframe_value(runtime->jvm, t, runtime);
    jthread_init(runtime->jvm, t);
    instance_release_from_thread(t, runtime);

    runtime->thrd_info->thread_status = THREAD_STATUS_RUNNING;
}

void thread_unboundle(Runtime *runtime) {
    runtime->thrd_info->is_suspend = 1;
    Instance *t = runtime->thrd_info->jthread;
    //main thread object dispose
    jthread_dispose(t, runtime);
}

void print_exception(Runtime *runtime) {
#if _JVM_DEBUG_LOG_LEVEL >= 0
    if (runtime) {
        if (!runtime->thrd_info->is_stop) {
            Utf8String *stacktrack = utf8_create();
            getExceptionStack(runtime, stacktrack);
            jvm_printf("%s\n", utf8_cstr(stacktrack));
            utf8_destroy(stacktrack);

            runtime_clear_stacktrack(runtime);
        }
    }
#endif
}


#if _JVM_DEBUG_METHOD_PROFILE
spinlock_t profile_method_lock = {0};
s64 profile_last_print_time = 0;

static int profile_method_compare(ArrayListValue a, ArrayListValue b) {
    MethodInfo *m1 = (MethodInfo *) a;
    MethodInfo *m2 = (MethodInfo *) b;
    if (m2->profile_total_time > m1->profile_total_time) return 1;
    if (m2->profile_total_time < m1->profile_total_time) return -1;
    return 0;
}

void profile_method_get_all(MiniJVM *jvm, ArrayList *all) {
    s32 i, j;
    spin_lock(&profile_method_lock);
    // Accessing classloaders without lock might be unsafe if loading happens,
    // but strict locking might cause stutter. We assume stable state.
    for (i = 0; i < jvm->classloaders->length; i++) {
        PeerClassLoader *pcl = (PeerClassLoader *) arraylist_get_value(jvm->classloaders, i);
        HashtableIterator hti;
        hashtable_iterate(pcl->classes, &hti);
        while (hashtable_iter_has_more(&hti)) {
            JClass *clazz = (JClass *) hashtable_iter_next_value(&hti);
            if (clazz->status >= CLASS_STATUS_PREPARED) {
                MethodPool *p = &(clazz->methodPool);
                for (j = 0; j < p->method_used; j++) {
                    MethodInfo *mi = &(p->method[j]);
                    if (mi->profile_count > 0) {
                        arraylist_push_back(all, mi);
                    }
                }
            }
        }
    }

    arraylist_sort(all, profile_method_compare);

    spin_unlock(&profile_method_lock);
}

void profile_method_print(MiniJVM *jvm) {
    s64 now = nanoTime();
    u64 profile_print_interal = 5000000000LL;
    if (now - profile_last_print_time < profile_print_interal) {
        // 1 sec
        return;
    }

    if (spin_trylock(&profile_method_lock) != 0) {
        return;
    }
    if (now - profile_last_print_time < profile_print_interal) {
        spin_unlock(&profile_method_lock);
        return;
    }
    profile_last_print_time = now;

    ArrayList *all = arraylist_create(1024);
    profile_method_get_all(jvm, all);

    s32 i;
    jvm_printf("--- Top 10 Hot Methods (Total Time) ---\n");
    for (i = 0; i < 50 && i < all->length; i++) {
        MethodInfo *m = (MethodInfo *) arraylist_get_value(all, i);
        jvm_printf("call:%10lld|t:%10lld|max:%10lld|avg:%10lld|%s.%s%s\n",
                   m->profile_count,
                   m->profile_total_time / 1000000,
                   m->profile_max_time / 1000000,
                   (m->profile_count > 0) ? (m->profile_total_time / 1000000 / m->profile_count) : 0,
                   utf8_cstr(m->_this_class->name),
                   utf8_cstr(m->name),
                   utf8_cstr(m->descriptor));
    }

    arraylist_destroy(all);
    spin_unlock(&profile_method_lock);
}


void profile_method_reset(MiniJVM *jvm) {
    s32 i, j;
    spin_lock(&profile_method_lock);
    // Accessing classloaders without lock might be unsafe if loading happens,
    // but strict locking might cause stutter. We assume stable state.
    for (i = 0; i < jvm->classloaders->length; i++) {
        PeerClassLoader *pcl = (PeerClassLoader *) arraylist_get_value(jvm->classloaders, i);
        HashtableIterator hti;
        hashtable_iterate(pcl->classes, &hti);
        while (hashtable_iter_has_more(&hti)) {
            JClass *clazz = (JClass *) hashtable_iter_next_value(&hti);
            if (clazz->status >= CLASS_STATUS_PREPARED) {
                MethodPool *p = &(clazz->methodPool);
                for (j = 0; j < p->method_used; j++) {
                    MethodInfo *mi = &(p->method[j]);
                    mi->profile_count = 0;
                    mi->profile_total_time = 0;
                    mi->profile_max_time = 0;
                }
            }
        }
    }
    spin_unlock(&profile_method_lock);
}
#endif


#if _JVM_DEBUG_BYTECODE_PROFILE

spinlock_t pro_lock;
ProfileDetail profile_instructs[INST_COUNT] = {0};


void profile_init() {
    memset(&profile_instructs, 0, sizeof(ProfileDetail) * INST_COUNT);
}

void profile_put(u8 instruct_code, s64 cost_add, s64 count_add) {
    ProfileDetail *h_s_v = &profile_instructs[instruct_code];

    spin_lock(&pro_lock);
    h_s_v->cost += cost_add;
    h_s_v->count += count_add;
    spin_unlock(&pro_lock);
};

void profile_print() {
    s32 i;
    jvm_printf("id           total    count      avg inst  \n");
    for (i = 0; i < INST_COUNT; i++) {
        ProfileDetail *pd = &profile_instructs[i];
        jvm_printf("%2x %15lld %8d %8lld %s\n",
                   i | 0xffffff00, pd->cost, pd->count, pd->count ? (pd->cost / pd->count) : 0, INST_NAME[i]);
    }
}

#endif

#if _JVM_DEBUG_SLOW_CALL_PROFILE

enum {
    SLOW_CALL_NODE_FLAG_EXCLUDED = 1,
    SLOW_CALL_SNAPSHOT_MAGIC = 0x53435632,
    SLOW_CALL_SNAPSHOT_VERSION = 1,
    SLOW_CALL_SNAPSHOT_HEADER_SIZE = 40,
    SLOW_CALL_SNAPSHOT_NODE_SIZE = 40
};

static inline HashtableKey _slow_id_key(u32 id) {
    return (HashtableKey) (intptr_t) ((u64) id + 1);
}

typedef struct _SlowCallAggNodeRec {
    u32 node_id;
    u32 parent_node_id;
    u32 method_id;
    u32 flags;
    s64 total_ns;
    s64 self_ns;
    u32 call_count;
} SlowCallAggNodeRec;

typedef struct _SlowCallAggChildEntry {
    u32 method_id;
    u32 flags;
    u32 agg_node_id;
} SlowCallAggChildEntry;

typedef struct _SlowCallAggChildMap {
    u32 used;
    u32 cap;
    SlowCallAggChildEntry *entries;
} SlowCallAggChildMap;

static void _slow_snapshot_destroy(SlowCallSnapshot *snap) {
    if (!snap) return;
    if (snap->payload) {
        bytebuf_destroy(snap->payload);
    }
    jvm_free(snap);
}

static s32 _slow_agg_child_map_expand(SlowCallAggChildMap *map) {
    if (!map) return 0;
    u32 new_cap = map->cap > 0 ? (map->cap << 1) : 8;
    SlowCallAggChildEntry *new_entries = jvm_calloc(sizeof(SlowCallAggChildEntry) * new_cap);
    if (!new_entries) return 0;
    if (map->entries && map->used > 0) {
        memcpy(new_entries, map->entries, sizeof(SlowCallAggChildEntry) * map->used);
        jvm_free(map->entries);
    }
    map->entries = new_entries;
    map->cap = new_cap;
    return 1;
}

static s32 _slow_agg_expand_nodes(SlowCallAggNodeRec **nodes, SlowCallAggChildMap **child_maps, u32 *cap) {
    if (!nodes || !child_maps || !cap) return 0;
    u32 new_cap = *cap > 0 ? (*cap << 1) : 256;
    SlowCallAggNodeRec *new_nodes = jvm_calloc(sizeof(SlowCallAggNodeRec) * new_cap);
    SlowCallAggChildMap *new_maps = jvm_calloc(sizeof(SlowCallAggChildMap) * new_cap);
    if (!new_nodes || !new_maps) {
        if (new_nodes) jvm_free(new_nodes);
        if (new_maps) jvm_free(new_maps);
        return 0;
    }
    if (*nodes && *cap > 0) {
        memcpy(new_nodes, *nodes, sizeof(SlowCallAggNodeRec) * (*cap));
        jvm_free(*nodes);
    }
    if (*child_maps && *cap > 0) {
        memcpy(new_maps, *child_maps, sizeof(SlowCallAggChildMap) * (*cap));
        jvm_free(*child_maps);
    }
    *nodes = new_nodes;
    *child_maps = new_maps;
    *cap = new_cap;
    return 1;
}

static u32 _slow_agg_find_or_add_child(SlowCallAggNodeRec *nodes,
                                       u32 *used,
                                       SlowCallAggChildMap *lookup_map,
                                       u32 parent_agg_id,
                                       const SlowCallNodeRec *raw) {
    if (!nodes || !used || !lookup_map || !raw) return 0xFFFFFFFFu;
    u32 i;
    for (i = 0; i < lookup_map->used; i++) {
        SlowCallAggChildEntry *entry = &lookup_map->entries[i];
        if (entry->method_id == raw->method_id && entry->flags == raw->flags) {
            return entry->agg_node_id;
        }
    }
    if (lookup_map->used >= lookup_map->cap) {
        if (!_slow_agg_child_map_expand(lookup_map)) {
            return 0xFFFFFFFFu;
        }
    }
    u32 agg_id = *used;
    SlowCallAggNodeRec *agg = &nodes[agg_id];
    agg->node_id = agg_id;
    agg->parent_node_id = parent_agg_id;
    agg->method_id = raw->method_id;
    agg->flags = raw->flags;
    agg->total_ns = 0;
    agg->self_ns = 0;
    agg->call_count = 0;
    SlowCallAggChildEntry *new_entry = &lookup_map->entries[lookup_map->used++];
    new_entry->method_id = raw->method_id;
    new_entry->flags = raw->flags;
    new_entry->agg_node_id = agg_id;
    (*used)++;
    return agg_id;
}

static void _slow_agg_free_child_maps(SlowCallAggChildMap *maps, u32 count) {
    if (!maps) return;
    u32 i;
    for (i = 0; i < count; i++) {
        if (maps[i].entries) {
            jvm_free(maps[i].entries);
            maps[i].entries = NULL;
            maps[i].used = 0;
            maps[i].cap = 0;
        }
    }
}

static s32 _slow_ctx_aggregate_nodes(SlowCallTraceCtx *ctx, SlowCallAggNodeRec **out_nodes, u32 *out_count) {
    if (!ctx || !out_nodes || !out_count || !ctx->nodes || ctx->node_used == 0) return 0;
    u32 max_node_id = ctx->next_node_id > 0 ? (ctx->next_node_id - 1) : 0;
    s32 *raw_index_by_id = jvm_calloc(sizeof(s32) * (max_node_id + 1));
    u32 *raw_to_agg = jvm_calloc(sizeof(u32) * (max_node_id + 1));
    if (!raw_index_by_id || !raw_to_agg) {
        if (raw_index_by_id) jvm_free(raw_index_by_id);
        if (raw_to_agg) jvm_free(raw_to_agg);
        return 0;
    }
    u32 i;
    for (i = 0; i <= max_node_id; i++) {
        raw_index_by_id[i] = -1;
        raw_to_agg[i] = 0xFFFFFFFFu;
    }
    for (i = 0; i < ctx->node_used; i++) {
        SlowCallNodeRec *raw = &ctx->nodes[i];
        if (raw->node_id <= max_node_id) {
            raw_index_by_id[raw->node_id] = (s32) i;
        }
    }
    u32 agg_cap = ctx->node_used > 0 ? ctx->node_used : 1;
    u32 agg_used = 0;
    SlowCallAggNodeRec *agg_nodes = jvm_calloc(sizeof(SlowCallAggNodeRec) * agg_cap);
    SlowCallAggChildMap *child_maps = jvm_calloc(sizeof(SlowCallAggChildMap) * agg_cap);
    SlowCallAggChildMap root_map;
    root_map.used = 0;
    root_map.cap = 0;
    root_map.entries = NULL;
    if (!agg_nodes || !child_maps) {
        if (agg_nodes) jvm_free(agg_nodes);
        if (child_maps) jvm_free(child_maps);
        jvm_free(raw_index_by_id);
        jvm_free(raw_to_agg);
        return 0;
    }
    for (i = 0; i <= max_node_id; i++) {
        s32 raw_idx = raw_index_by_id[i];
        if (raw_idx < 0) continue;
        SlowCallNodeRec *raw = &ctx->nodes[raw_idx];
        if (agg_used >= agg_cap) {
            if (!_slow_agg_expand_nodes(&agg_nodes, &child_maps, &agg_cap)) {
                _slow_agg_free_child_maps(child_maps, agg_cap);
                if (root_map.entries) jvm_free(root_map.entries);
                jvm_free(child_maps);
                jvm_free(agg_nodes);
                jvm_free(raw_index_by_id);
                jvm_free(raw_to_agg);
                return 0;
            }
        }
        u32 parent_agg_id = 0xFFFFFFFFu;
        SlowCallAggChildMap *lookup_map = &root_map;
        if (raw->parent_node_id != 0xFFFFFFFFu && raw->parent_node_id <= max_node_id) {
            u32 mapped = raw_to_agg[raw->parent_node_id];
            if (mapped != 0xFFFFFFFFu) {
                parent_agg_id = mapped;
                lookup_map = &child_maps[mapped];
            }
        }
        u32 agg_id = _slow_agg_find_or_add_child(agg_nodes, &agg_used, lookup_map, parent_agg_id, raw);
        if (agg_id == 0xFFFFFFFFu) {
            _slow_agg_free_child_maps(child_maps, agg_cap);
            if (root_map.entries) jvm_free(root_map.entries);
            jvm_free(child_maps);
            jvm_free(agg_nodes);
            jvm_free(raw_index_by_id);
            jvm_free(raw_to_agg);
            return 0;
        }
        SlowCallAggNodeRec *agg = &agg_nodes[agg_id];
        agg->total_ns += raw->total_ns;
        agg->self_ns += raw->self_ns;
        if (agg->call_count < 0xFFFFFFFFu) {
            agg->call_count++;
        }
        raw_to_agg[i] = agg_id;
    }
    _slow_agg_free_child_maps(child_maps, agg_cap);
    if (root_map.entries) jvm_free(root_map.entries);
    jvm_free(child_maps);
    jvm_free(raw_index_by_id);
    jvm_free(raw_to_agg);
    if (agg_used == 0) {
        jvm_free(agg_nodes);
        return 0;
    }
    *out_nodes = agg_nodes;
    *out_count = agg_used;
    return 1;
}

static u16 _slow_alloc_class_id(MiniJVM *jvm) {
    u32 i;
    u16 start = jvm->slow_call_profile.next_class_id;
    for (i = 0; i <= 0xFFFFu; i++) {
        u16 cid = (u16) (start + i);
        if (!hashtable_get(jvm->slow_call_profile.class_by_id, _slow_id_key((u32) cid))) {
            jvm->slow_call_profile.next_class_id = (u16) (cid + 1);
            return cid;
        }
    }
    return 0xFFFFu;
}

static u16 _slow_get_or_alloc_class_id(MiniJVM *jvm, JClass *clazz) {
    if (!jvm || !clazz) return 0xFFFFu;
    if (clazz->slow_profile_class_id || hashtable_get(jvm->slow_call_profile.class_by_id, _slow_id_key(0))) {
        if (hashtable_get(jvm->slow_call_profile.class_by_id, _slow_id_key((u32) clazz->slow_profile_class_id)) == clazz) {
            return clazz->slow_profile_class_id;
        }
    }
    u16 cid = _slow_alloc_class_id(jvm);
    if (cid == 0xFFFFu) return cid;
    clazz->slow_profile_class_id = cid;
    hashtable_put(jvm->slow_call_profile.class_by_id, _slow_id_key((u32) cid), clazz);
    return cid;
}

u32 profile_slow_call_get_method_id(MiniJVM *jvm, MethodInfo *method) {
    if (!jvm || !method || !method->_this_class) return 0;
    if (method->slow_profile_uni_method_id > 0) {
        MethodInfo *mi = hashtable_get(jvm->slow_call_profile.method_by_id, _slow_id_key(method->slow_profile_uni_method_id));
        if (mi == method) return method->slow_profile_uni_method_id;
    }
    u16 cid = _slow_get_or_alloc_class_id(jvm, method->_this_class);
    if (cid == 0xFFFFu) return 0;
    if (method->slow_profile_method_id == 0) {
        MethodPool *pool = &method->_this_class->methodPool;
        s32 i;
        for (i = 0; i < pool->method_used; i++) {
            if (&pool->method[i] == method) {
                method->slow_profile_method_id = (u16) (i + 1);
                break;
            }
        }
    }
    if (method->slow_profile_method_id == 0) return 0;
    method->slow_profile_uni_method_id = ((u32) cid << 16) | method->slow_profile_method_id;
    hashtable_put(jvm->slow_call_profile.method_by_id, _slow_id_key(method->slow_profile_uni_method_id), method);
    return method->slow_profile_uni_method_id;
}

MethodInfo *profile_slow_call_get_method_by_id(MiniJVM *jvm, u32 method_id) {
    if (!jvm || method_id == 0) return NULL;
    return (MethodInfo *) hashtable_get(jvm->slow_call_profile.method_by_id, _slow_id_key(method_id));
}

void profile_slow_call_unregister_class(MiniJVM *jvm, JClass *clazz) {
    if (!jvm || !clazz) return;
    MethodPool *pool = &clazz->methodPool;
    s32 i;
    for (i = 0; i < pool->method_used; i++) {
        MethodInfo *m = &pool->method[i];
        if (m->slow_profile_uni_method_id > 0) {
            hashtable_remove(jvm->slow_call_profile.method_by_id, _slow_id_key(m->slow_profile_uni_method_id), 0);
            m->slow_profile_uni_method_id = 0;
        }
    }
    hashtable_remove(jvm->slow_call_profile.class_by_id, _slow_id_key((u32) clazz->slow_profile_class_id), 0);
}

static void _slow_ctx_append_node(SlowCallTraceCtx *ctx, u32 node_id, u32 parent_node_id, u32 method_id, u32 flags, s64 total_ns, s64 self_ns) {
    if (!ctx || method_id == 0 || total_ns <= 0) return;
    if (ctx->node_used >= ctx->node_cap) {
        u32 new_cap = ctx->node_cap > 0 ? (ctx->node_cap << 1) : 256;
        SlowCallNodeRec *new_nodes = jvm_calloc(sizeof(SlowCallNodeRec) * new_cap);
        if (!new_nodes) return;
        if (ctx->nodes && ctx->node_used > 0) {
            memcpy(new_nodes, ctx->nodes, sizeof(SlowCallNodeRec) * ctx->node_used);
            jvm_free(ctx->nodes);
        }
        ctx->nodes = new_nodes;
        ctx->node_cap = new_cap;
    }
    SlowCallNodeRec *n = &ctx->nodes[ctx->node_used++];
    n->node_id = node_id;
    n->parent_node_id = parent_node_id;
    n->method_id = method_id;
    n->flags = flags;
    n->total_ns = total_ns;
    n->self_ns = self_ns;
}

static SlowCallTraceCtx *_slow_ctx_create(MiniJVM *jvm, Runtime *root_runtime, MethodInfo *root_method, s64 threshold_ns, s64 start_ns) {
    SlowCallTraceCtx *ctx = jvm_calloc(sizeof(SlowCallTraceCtx));
    ctx->root_runtime = root_runtime;
    ctx->root_method = root_method;
    ctx->threshold_ns = threshold_ns;
    ctx->start_ns = start_ns;
    ctx->next_node_id = 1;
    ctx->node_cap = 256;
    ctx->nodes = jvm_calloc(sizeof(SlowCallNodeRec) * ctx->node_cap);
    return ctx;
}

static void _slow_ctx_destroy(SlowCallTraceCtx *ctx) {
    if (!ctx) return;
    if (ctx->nodes) jvm_free(ctx->nodes);
    jvm_free(ctx);
}

s32 is_valid_method_info(MiniJVM *jvm, MethodInfo *method) {
    if (!jvm || !method || !jvm->classloaders) {
        return 0;
    }

    s32 i, j;
    for (i = 0; i < jvm->classloaders->length; i++) {
        PeerClassLoader *pcl = (PeerClassLoader *) arraylist_get_value(jvm->classloaders, i);
        HashtableIterator hti;
        hashtable_iterate(pcl->classes, &hti);

        while (hashtable_iter_has_more(&hti)) {
            JClass *clazz = (JClass *) hashtable_iter_next_value(&hti);
            if (clazz->status >= CLASS_STATUS_PREPARED) {
                MethodPool *p = &(clazz->methodPool);
                for (j = 0; j < p->method_used; j++) {
                    MethodInfo *mi = &(p->method[j]);
                    if (mi == method) {
                        return 1;
                    }
                }
            }
        }
    }

    return 0;
}

void profile_slow_call_watch_method(MiniJVM *jvm, MethodInfo *method, s64 threshold_ns) {
    if (!jvm || !method) return;
    if (!is_valid_method_info(jvm, method)) return;
    if (threshold_ns <= 0) threshold_ns = 0;
    method->slow_profile_threshold_ns = threshold_ns;
}

void profile_slow_call_unwatch_method(MiniJVM *jvm, MethodInfo *method) {
    if (!jvm || !method) return;
    if (!is_valid_method_info(jvm, method)) return;
    method->slow_profile_threshold_ns = 0;
}

void profile_slow_call_clear_watch(MiniJVM *jvm) {
    if (!jvm || !jvm->classloaders) return;
    s32 i, j;
    for (i = 0; i < jvm->classloaders->length; i++) {
        PeerClassLoader *pcl = (PeerClassLoader *) arraylist_get_value(jvm->classloaders, i);
        HashtableIterator hti;
        hashtable_iterate(pcl->classes, &hti);
        while (hashtable_iter_has_more(&hti)) {
            JClass *clazz = (JClass *) hashtable_iter_next_value(&hti);
            if (clazz->status >= CLASS_STATUS_PREPARED) {
                MethodPool *p = &(clazz->methodPool);
                for (j = 0; j < p->method_used; j++) {
                    MethodInfo *mi = &(p->method[j]);
                    mi->slow_profile_threshold_ns = 0;
                }
            }
        }
    }
}

void profile_slow_call_clear_cache(MiniJVM *jvm) {
    if (!jvm || !jvm->slow_call_profile.stack_cache) return;
    spin_lock(&jvm->slow_call_profile.lock);
    while (jvm->slow_call_profile.stack_cache->length > 0) {
        SlowCallSnapshot *item = (SlowCallSnapshot *) arraylist_pop_front(jvm->slow_call_profile.stack_cache);
        _slow_snapshot_destroy(item);
    }
    spin_unlock(&jvm->slow_call_profile.lock);
}

void profile_slow_call_enter(Runtime *runtime, MethodInfo *method, s64 start_ns) {
    if (!runtime || !method) return;
    MiniJVM *jvm = runtime->jvm;
    if (!jvm || !runtime->thrd_info) return;
    if (runtime->thrd_info->slow_call_ctx) {
        SlowCallTraceCtx *ctx = runtime->thrd_info->slow_call_ctx;
        runtime->slow_profile_in_trace = 1;
        runtime->slow_profile_node_id = ctx->next_node_id++;
        return;
    }
    s64 threshold = method->slow_profile_threshold_ns;
    if (threshold <= 0) return;
    SlowCallTraceCtx *ctx = _slow_ctx_create(jvm, runtime, method, threshold, start_ns);
    runtime->thrd_info->slow_call_ctx = ctx;
    runtime->slow_profile_in_trace = 1;
    runtime->slow_profile_node_id = 0;
}

void profile_slow_call_record(Runtime *runtime, MethodInfo *method, s64 spent_ns) {
    if (!runtime || !runtime->thrd_info || !method) return;
    SlowCallTraceCtx *ctx = runtime->thrd_info->slow_call_ctx;
    if (!ctx || !runtime->slow_profile_in_trace) return;
    MiniJVM *jvm = runtime->jvm;
    if (!jvm || !jvm->slow_call_profile.stack_cache) return;
    if (spent_ns < 0) {
        spent_ns = 0;
    }

    s64 self_ns = spent_ns - runtime->slow_profile_child_spent;
    if (self_ns < 0) self_ns = 0;
    u32 method_id = profile_slow_call_get_method_id(jvm, method);
    u32 parent_node_id = 0xFFFFFFFFu;
    if (runtime->parent && runtime->parent->slow_profile_in_trace) {
        parent_node_id = runtime->parent->slow_profile_node_id;
    }
    u32 flags = method->slow_profile_excluded ? SLOW_CALL_NODE_FLAG_EXCLUDED : 0;
    _slow_ctx_append_node(ctx, runtime->slow_profile_node_id, parent_node_id, method_id, flags, spent_ns, self_ns);
    runtime->slow_profile_in_trace = 0;

    if (ctx->root_runtime != runtime) {
        return;
    }

    runtime->thrd_info->slow_call_ctx = NULL;
    if (spent_ns < ctx->threshold_ns) {
        _slow_ctx_destroy(ctx);
        return;
    }

    u64 ts_ms = currentTimeMillis();
    u32 root_method_id = profile_slow_call_get_method_id(jvm, ctx->root_method);
    SlowCallAggNodeRec *agg_nodes = NULL;
    u32 agg_count = 0;
    s32 use_agg = _slow_ctx_aggregate_nodes(ctx, &agg_nodes, &agg_count);
    u32 node_count = use_agg ? agg_count : ctx->node_used;
    ByteBuf *snapshot_buf = bytebuf_create((u32) (SLOW_CALL_SNAPSHOT_HEADER_SIZE + node_count * SLOW_CALL_SNAPSHOT_NODE_SIZE));
    bytebuf_write_int(snapshot_buf, SLOW_CALL_SNAPSHOT_MAGIC);
    bytebuf_write_short(snapshot_buf, (s16) SLOW_CALL_SNAPSHOT_VERSION);
    bytebuf_write_short(snapshot_buf, 0);
    bytebuf_write_long(snapshot_buf, (s64) ts_ms);
    bytebuf_write_long(snapshot_buf, spent_ns);
    bytebuf_write_long(snapshot_buf, ctx->threshold_ns);
    bytebuf_write_int(snapshot_buf, (s32) root_method_id);
    bytebuf_write_int(snapshot_buf, (s32) node_count);
    u32 i;
    if (use_agg) {
        for (i = 0; i < node_count; i++) {
            SlowCallAggNodeRec *n = &agg_nodes[i];
            bytebuf_write_int(snapshot_buf, (s32) n->node_id);
            bytebuf_write_int(snapshot_buf, (s32) n->parent_node_id);
            bytebuf_write_int(snapshot_buf, (s32) n->method_id);
            bytebuf_write_int(snapshot_buf, (s32) n->flags);
            bytebuf_write_long(snapshot_buf, n->total_ns);
            bytebuf_write_long(snapshot_buf, n->self_ns);
            bytebuf_write_int(snapshot_buf, (s32) n->call_count);
            bytebuf_write_int(snapshot_buf, 0);
        }
    } else {
        for (i = 0; i < ctx->node_used; i++) {
            SlowCallNodeRec *n = &ctx->nodes[i];
            bytebuf_write_int(snapshot_buf, (s32) n->node_id);
            bytebuf_write_int(snapshot_buf, (s32) n->parent_node_id);
            bytebuf_write_int(snapshot_buf, (s32) n->method_id);
            bytebuf_write_int(snapshot_buf, (s32) n->flags);
            bytebuf_write_long(snapshot_buf, n->total_ns);
            bytebuf_write_long(snapshot_buf, n->self_ns);
            bytebuf_write_int(snapshot_buf, 1);
            bytebuf_write_int(snapshot_buf, 0);
        }
    }

    SlowCallSnapshot *snapshot = jvm_calloc(sizeof(SlowCallSnapshot));
    snapshot->payload = snapshot_buf;
    snapshot->ts_ms = ts_ms;
    snapshot->spent_ns = spent_ns;
    snapshot->root_method_id = root_method_id;
    snapshot->node_count = node_count;

    spin_lock(&jvm->slow_call_profile.lock);
    while (jvm->slow_call_profile.stack_cache->length >= jvm->slow_call_profile.cache_limit) {
        SlowCallSnapshot *old = (SlowCallSnapshot *) arraylist_pop_front(jvm->slow_call_profile.stack_cache);
        _slow_snapshot_destroy(old);
    }
    arraylist_push_back(jvm->slow_call_profile.stack_cache, snapshot);
    spin_unlock(&jvm->slow_call_profile.lock);
    if (agg_nodes) {
        jvm_free(agg_nodes);
    }
    _slow_ctx_destroy(ctx);
}

void profile_slow_call_remove_class_cache(MiniJVM *jvm, Utf8String *class_name) {
    if (!jvm || !class_name) return;
}

#endif

PeerClassLoader *classloader_create(MiniJVM *jvm) {
    return classloader_create_with_path(jvm, "");
}

PeerClassLoader *classloader_create_with_path(MiniJVM *jvm, c8 *path) {
    PeerClassLoader *class_loader = jvm_calloc(sizeof(PeerClassLoader));

    class_loader->jvm = jvm;
    //split classpath
    class_loader->classpath = arraylist_create(0);
    Utf8String *g_classpath = utf8_create_c(path);
    classloader_add_jar_path(class_loader, g_classpath);
    utf8_destroy(g_classpath);
    //创建类容器
    class_loader->classes = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);

    return class_loader;
}

void classloader_destroy(PeerClassLoader *class_loader) {
    HashtableIterator hti;
    hashtable_iterate(class_loader->classes, &hti);
    for (; hashtable_iter_has_more(&hti);) {
        HashtableValue v = hashtable_iter_next_value(&hti);
        gc_obj_release(class_loader->jvm->collector, v);
    }

    hashtable_clear(class_loader->classes);
    s32 i;
    for (i = 0; i < class_loader->classpath->length; i++) {
        utf8_destroy(arraylist_get_value(class_loader->classpath, i));
    }
    arraylist_destroy(class_loader->classpath);
    hashtable_destroy(class_loader->classes);

    class_loader->classes = NULL;

    jvm_free(class_loader);
}

void classloader_remove_all_class(PeerClassLoader *class_loader) {
    hashtable_clear(class_loader->classes);
}

void classloader_release_class_static_field(PeerClassLoader *class_loader) {
    HashtableIterator hti;
    hashtable_iterate(class_loader->classes, &hti);
    for (; hashtable_iter_has_more(&hti);) {
        HashtableValue v = hashtable_iter_next_value(&hti);
        JClass *clazz = (JClass *) (v);
        class_clear_refer(class_loader, clazz);
    }
}


void classloader_add_jar_path(PeerClassLoader *class_loader, Utf8String *jar_path) {
    Utf8String *libname = utf8_create();
    s32 i;
    for (i = 0;; i++) {
        utf8_split_get_part(jar_path, PATHSEPARATOR, i, libname);
        if (libname->length) {
            arraylist_push_back(class_loader->classpath, libname);
            libname = utf8_create();
        } else {
            break;
        }
    }
    utf8_destroy(libname);
}

void classloaders_add(MiniJVM *jvm, PeerClassLoader *pcl) {
    spin_lock(&jvm->lock_cloader);
    {
        arraylist_push_back_unsafe(jvm->classloaders, pcl);
    }
    spin_unlock(&jvm->lock_cloader);
}

void classloaders_remove(MiniJVM *jvm, PeerClassLoader *pcl) {
    spin_lock(&jvm->lock_cloader);
    {
        arraylist_remove_unsafe(jvm->classloaders, pcl);
    }
    spin_unlock(&jvm->lock_cloader);
}

PeerClassLoader *classLoaders_find_by_instance(MiniJVM *jvm, Instance *jloader) {
    PeerClassLoader *r = NULL;
    spin_lock(&jvm->lock_cloader);
    {
        s32 i;
        for (i = 0; i < jvm->classloaders->length; i++) {
            PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
            if (pcl->jloader == jloader) {
                r = pcl;
            }
        }
    }
    spin_unlock(&jvm->lock_cloader);
    return r;
}

void classloaders_clear_all_static(MiniJVM *jvm) {
    spin_lock(&jvm->lock_cloader);
    {
        s32 i;
        for (i = 0; i < jvm->classloaders->length; i++) {
            PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
            //release class static field
            classloader_release_class_static_field(pcl);
        }
    }
    spin_unlock(&jvm->lock_cloader);
}

void classloaders_destroy_all(MiniJVM *jvm) {
    spin_lock(&jvm->lock_cloader);
    {
        s32 i;
        for (i = 0; i < jvm->classloaders->length; i++) {
            PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
            //release class static field
            classloader_destroy(pcl);
        }
    }
    spin_unlock(&jvm->lock_cloader);
    spin_destroy(&jvm->lock_cloader);
    arraylist_destroy(jvm->classloaders);
    jvm->boot_classloader = NULL;
    jvm->classloaders = NULL;
}

void set_jvm_state(MiniJVM *jvm, s32 state) {
    jvm->jvm_state = state;
}

s32 get_jvm_state(MiniJVM *jvm) {
    return jvm->jvm_state;
}

static MiniJVM *g_jvm = NULL;

void _on_jvm_sig_print(s32 no) {
    jvm_printf("[SIGNAL]jvm sig:%d  errno: %d , %s\n", no, errno, strerror(errno));
    if (g_jvm && g_jvm->thread_list) {
        jvm_printf("[SIGNAL]dump all threads runtime stacks:\n");
        spin_lock(&g_jvm->thread_list->spinlock);
        s32 len = g_jvm->thread_list->length;
        spin_unlock(&g_jvm->thread_list->spinlock);
        s32 i;
        for (i = 0; i < len; i++) {
            Runtime *r = threadlist_get(g_jvm, i);
            if (!r || !r->thrd_info) continue;
            Utf8String *ustr = utf8_create();
            getRuntimeStackWithOutReturn(r, ustr);
            jvm_printf("[Thread #%d status=%d suspend=%d blocking=%d] %s\n",
                       i,
                       r->thrd_info->thread_status,
                       r->thrd_info->is_suspend,
                       r->thrd_info->is_blocking,
                       utf8_cstr(ustr));
            utf8_destroy(ustr);
        }
    }
}

void _on_jvm_sig(s32 no) {
    _on_jvm_sig_print(no);
    fflush(stderr);
    fflush(stdout);
    jvm_destroy_mem_alloc();
    exit(no);
}

MiniJVM *jvm_create() {
    MiniJVM *jvm = jvm_calloc(sizeof(MiniJVM));
    if (!jvm) {
        jvm_printf("jvm create error.");
        return NULL;
    }
    jvm->env = &jnienv;
    jvm->max_heap_size = MAX_HEAP_SIZE_DEFAULT;
    jvm->heap_overload_percent = GARBAGE_OVERLOAD_DEFAULT;
    jvm->garbage_collect_period_ms = GARBAGE_PERIOD_MS_DEFAULT;
#if _JVM_DEBUG_SLOW_CALL_PROFILE
    jvm->slow_call_profile.cache_limit = 50;
    jvm->slow_call_profile.next_class_id = 0;
    spin_init(&jvm->slow_call_profile.lock, 0);
#endif
    g_jvm = jvm;
    return jvm;
}

s32 jvm_init(MiniJVM *jvm, c8 *p_bootclasspath, c8 *p_classpath) {
    if (!jvm) {
        jvm_printf("jvm not found.");
        return -1;
    }

    signal(SIGABRT, _on_jvm_sig);
    signal(SIGFPE, _on_jvm_sig);
    signal(SIGSEGV, _on_jvm_sig);
    signal(SIGTERM, _on_jvm_sig);
#ifdef SIGPIPE
    signal(SIGPIPE, _on_jvm_sig_print); //not exit when network sigpipe
#endif

#if __JVM_PRI_ALLOC__
    pri_alloc_set_max_size(jvm->max_heap_size);
#endif

    set_jvm_state(jvm, JVM_STATUS_INITING);

    if (!p_classpath) {
        p_classpath = "./";
    }
    if (!jvm->startup_dir) {
        jvm->startup_dir = utf8_create_c("./");
    }

#if _JVM_DEBUG_SLOW_CALL_PROFILE
    jvm->slow_call_profile.stack_cache = arraylist_create(0);
    jvm->slow_call_profile.class_by_id = hashtable_create(DEFAULT_HASH_FUNC, DEFAULT_HASH_EQUALS_FUNC);
    jvm->slow_call_profile.method_by_id = hashtable_create(DEFAULT_HASH_FUNC, DEFAULT_HASH_EQUALS_FUNC);
#endif

#if _JVM_DEBUG_BYTECODE_PROFILE
    profile_init();
#endif
    //
    init_jni_func_table(jvm);

    //创建线程容器
    jvm->thread_list = arraylist_create(0);
    jvm->shutdown_hook = arraylist_create(0);
    //创建垃圾收集器
    gc_create(jvm);

    //本地方法库
    jvm->native_libs = arraylist_create(0);
    reg_std_native_lib(jvm);
    reg_net_native_lib(jvm);
    reg_reflect_native_lib(jvm);

    //创建jstring 相关容器
    jvm->table_jstring_const = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);
    hashtable_register_free_functions(jvm->table_jstring_const, (HashtableKeyFreeFunc) utf8_destroy, NULL);

    spin_init(&jvm->lock_cloader, 0);
    jvm->boot_classloader = classloader_create_with_path(jvm, p_bootclasspath);
    jvm->classloaders = arraylist_create(4);
    classloaders_add(jvm, jvm->boot_classloader);

    //装入系统属性
    sys_properties_load(jvm);
    sys_properties_set_c(jvm, STR_VM_JAVA_CLASS_PATH, p_classpath);
    sys_properties_set_c(jvm, STR_VM_SUN_BOOT_CLASS_PATH, p_bootclasspath);
    sys_properties_set_c(jvm, STR_VM_JAVA_CLASS_VERSION, "52.0");
    Utf8String *tmpstr = utf8_create();
    os_get_lang(tmpstr);
    sys_properties_set_c(jvm, STR_VM_USER_LANGUAGE, utf8_cstr(tmpstr));
    os_get_uuid(jvm, tmpstr);
    sys_properties_set_c(jvm, STR_VM_UUID, utf8_cstr(tmpstr));
    utf8_destroy(tmpstr);

    //启动调试器
    jdwp_start_server(jvm);

    set_jvm_state(jvm, JVM_STATUS_RUNNING);

    //init load thread, string etc
    Runtime *runtime = runtime_create(jvm);
    runtime->thrd_info->type = THREAD_TYPE_NORMAL;
    Utf8String *clsName = utf8_create_c(STR_CLASS_JAVA_LANG_INTEGER);
    JClass *c = classes_load_get_with_clinit(NULL, clsName, runtime);
    if (!c) {
        jvm_printf("[ERROR]maybe bootstrap classpath misstake: %s \n", p_bootclasspath);
        return -1;
    }
    //load bootstrap class
    utf8_clear(clsName);
    utf8_append_c(clsName, STR_CLASS_JAVA_LANG_THREAD);
    classes_load_get_with_clinit(NULL, clsName, runtime);

    utf8_clear(clsName);
    utf8_append_c(clsName, STR_CLASS_SUN_MISC_LAUNCHER);
    classes_load_get_with_clinit(NULL, clsName, runtime);
    //for interrupted thread
    utf8_clear(clsName);
    utf8_append_c(clsName, STR_CLASS_JAVA_LANG_INTERRUPTED);
    //must load this class ,because it will be used when thread interrupt ,but it can not load when that thread is marked as interrupted
    JClass *c2;
    c2 = classes_load_get_with_clinit(NULL, clsName, runtime);
    instance_create(runtime, c2);
    utf8_clear(clsName);


    if (!jvm->collector->runtime->thrd_info->jthread) {
        Instance *inst = instance_create(runtime, classes_get_c(jvm, NULL, STR_CLASS_JAVA_LANG_THREAD));
        jvm->collector->runtime->thrd_info->jthread = inst;
        hashset_put(jvm->collector->objs_holder, inst);
    }

    utf8_destroy(clsName);
    gc_move_objs_thread_2_gc(runtime);
    runtime_destroy(runtime);
    runtime = NULL;

    //启动垃圾回收
    gc_resume(jvm->collector);

#if _JVM_DEBUG_LOG_LEVEL > 0
#ifdef __JVM_ARCH_32__
    c8 *arch = "32-bit";
#else
    c8 *arch = "64-bit";
#endif
    jvm_printf("[INFO] jvm inited in %s mode\n", arch);
#endif
    return 0;
}

void jvm_destroy(MiniJVM *jvm) {
    Runtime *parent = runtime_create(jvm);

    while (parent && jvm->shutdown_hook->length) {
        Instance *inst = arraylist_get_value(jvm->shutdown_hook, 0);
        arraylist_remove_at(jvm->shutdown_hook, 0);

        //there is an week is that the hook thread is serialized execution,
        // because the hook thread may be not inserted into the thread list,
        // then vm is destroyed
        thrd_t t = jthread_start(inst, parent);
        thrd_join(t, NULL);
    }
    runtime_destroy(parent);
    parent = NULL;
    while (threadlist_count_none_daemon(jvm) > 0 && !jvm->collector->exit_flag) {
        //wait for other thread over ,
        threadSleep(20);
    }
    set_jvm_state(jvm, JVM_STATUS_STOPED);
    //waiting for daemon thread terminate
    thread_stop_all(jvm);

#if _JVM_DEBUG_LOG_LEVEL > 0
    jvm_printf("[INFO]waitting for thread terminate\n");
#endif
    while (threadlist_count_active(jvm) > 0) {
        threadSleep(20);
    }

    jdwp_stop_server(jvm);
    //
    gc_destroy(jvm);

    hashtable_destroy(jvm->table_jstring_const);
#if _JVM_DEBUG_SLOW_CALL_PROFILE
    profile_slow_call_clear_watch(jvm);
    profile_slow_call_clear_cache(jvm);
    if (jvm->slow_call_profile.stack_cache) {
        arraylist_destroy(jvm->slow_call_profile.stack_cache);
        jvm->slow_call_profile.stack_cache = NULL;
    }
    if (jvm->slow_call_profile.class_by_id) {
        hashtable_destroy(jvm->slow_call_profile.class_by_id);
        jvm->slow_call_profile.class_by_id = NULL;
    }
    if (jvm->slow_call_profile.method_by_id) {
        hashtable_destroy(jvm->slow_call_profile.method_by_id);
        jvm->slow_call_profile.method_by_id = NULL;
    }
#endif
    //
    thread_lock_dispose(&jvm->threadlock);
    arraylist_destroy(jvm->thread_list);
    arraylist_destroy(jvm->shutdown_hook);
    native_lib_destroy(jvm);

    sys_properties_dispose(jvm);
#if _JVM_DEBUG_LOG_LEVEL > 0
    jvm_printf("[INFO]jvm destoried\n");
#endif
    set_jvm_state(jvm, JVM_STATUS_UNKNOW);
    if (jvm->startup_dir) {
        utf8_destroy(jvm->startup_dir);
    }
    jvm_free(jvm);
}

s32 call_main(MiniJVM *jvm, c8 *p_mainclass, ArrayList *java_para) {
    if (!jvm) {
        jvm_printf("jvm not found .\n");
        return 1;
    }
    Runtime *runtime = runtime_create(jvm);
    runtime->thrd_info->type = THREAD_TYPE_NORMAL;
    thread_boundle(runtime);

    //准备参数
    s32 count = java_para ? java_para->length : 0;
    Utf8String *ustr = utf8_create_c(STR_CLASS_JAVA_LANG_STRING);
    Instance *arr = jarray_create_by_type_name(runtime, count, ustr, NULL);
    instance_hold_to_thread(arr, runtime);
    utf8_destroy(ustr);
    s32 i;
    for (i = 0; i < count; i++) {
        Utf8String *utfs = utf8_create_c(arraylist_get_value(java_para, i));
        Instance *jstr = jstring_create(utfs, runtime);
        jarray_set_field(arr, i, (intptr_t) jstr);
        utf8_destroy(utfs);
    }
    push_ref(runtime->stack, arr);
    instance_release_from_thread(arr, runtime);

    c8 *p_methodname = "main";
    c8 *p_methodtype = "([Ljava/lang/String;)V";
    s32 ret = call_method(jvm, p_mainclass, p_methodname, p_methodtype, runtime);

    thread_unboundle(runtime);
    runtime_destroy(runtime);
    return ret;
}


s32 call_method(MiniJVM *jvm, c8 *p_classname, c8 *p_methodname, c8 *p_methoddesc, Runtime *p_runtime) {
    if (p_runtime && p_runtime->jvm != jvm) {
        jvm_printf("[ERROR]runtime not adapted to jvm .\n");
        return RUNTIME_STATUS_ERROR;
    }
    if (!jvm) {
        jvm_printf("[ERROR]jvm not found .\n");
        return RUNTIME_STATUS_ERROR;
    }
    //创建运行时栈
    Runtime *runtime = p_runtime;
    if (!p_runtime) {
        runtime = runtime_create(jvm);
        runtime->thrd_info->type = THREAD_TYPE_NORMAL;
        thread_boundle(runtime);
    }

    //开始装载类

    Utf8String *str_mainClsName = utf8_create_c(p_classname);
    utf8_replace_c(str_mainClsName, ".", "/");

    //find systemclassloader
    Instance *jloader = NULL;
    s32 ret = execute_method_impl(jvm->shortcut.launcher_getSystemClassLoader, runtime);
    if (!ret) {
        jloader = pop_ref(runtime->stack);
    } else {
        if (ret == RUNTIME_STATUS_EXCEPTION) {
            print_exception(runtime);
        }
    }
    //装入主类
    JClass *clazz = classes_load_get_with_clinit(jloader, str_mainClsName, runtime);


    ret = 0;
    if (clazz) {
        Utf8String *methodName = utf8_create_c(p_methodname);
        Utf8String *methodType = utf8_create_c(p_methoddesc);

        MethodInfo *m = find_methodInfo_by_name(str_mainClsName, methodName, methodType, clazz->jloader, runtime);
        if (m) {
            s64 start = currentTimeMillis();
#if _JVM_DEBUG_LOG_LEVEL > 0
            jvm_printf("\n[INFO]main thread start\n");
#endif
            //调用主方法
            if (jvm->jdwp_enable) {
#if _JVM_DEBUG_LOG_LEVEL > 0
                jvm_printf("[JDWP]jdwp listening (port:%d) ...\n", jvm->jdwp_port);
#endif
                if (jvm->jdwp_suspend_on_start) {
                    jvm_printf("[JDWP]suspend on start, waitting for connect... \n");
                    jthread_suspend(runtime);
                }
            } //jdwp 会启动调试器

            runtime->method = NULL;
            runtime->clazz = clazz;
            ret = execute_method(m, runtime);
            if (ret == RUNTIME_STATUS_EXCEPTION) {
                print_exception(runtime);
            }
#if _JVM_DEBUG_LOG_LEVEL > 0
            jvm_printf("[INFO]main thread over %llx , return %d , spent : %lld\n",
                       (s64) (intptr_t) runtime->thrd_info->jthread, ret, (currentTimeMillis() - start));
#endif

#if _JVM_DEBUG_BYTECODE_PROFILE
            profile_print();
#endif
        }
        utf8_destroy(methodName);
        utf8_destroy(methodType);
    } else {
        jvm_printf("[ERROR]main class not found: %s\n", p_classname);
        ret = RUNTIME_STATUS_ERROR;
    }
    if (!p_runtime) {
        thread_unboundle(runtime);
        runtime_destroy(runtime);
    }
    utf8_destroy(str_mainClsName);
    return ret;
}


s32 execute_method(MethodInfo *method, Runtime *runtime) {
    if (!runtime || !method) {
        return RUNTIME_STATUS_ERROR;
    }
    // if not detect the son ,may cause jthread enter fake blocking state,
    // eg: call_bc-> call_native->(reenter) call_bc->ret_bc(fake_blocking)->ret_native->ret_bc
    // only the outer thread top call the java bytecode ,need check block state
    if (runtime->thrd_info->top_runtime->son == NULL) {
        // is top call bc, not reenter bc
        jthread_block_exit(runtime);
    }
    s32 ret = execute_method_impl(method, runtime);
    if (runtime->thrd_info->top_runtime->son == NULL) {
        jthread_block_enter(runtime);
    }
    return ret;
}
