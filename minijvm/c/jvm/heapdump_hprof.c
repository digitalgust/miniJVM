#include "heapdump_hprof.h"

#include <stdio.h>
#include <string.h>

#include "jvm.h"

extern s32 _gc_pause_the_world(MiniJVM *jvm);

extern s32 _gc_resume_the_world(MiniJVM *jvm);

extern void _gc_copy_objs(MiniJVM *jvm);

extern s32 _gc_big_search(GcCollector *collector);

static void hprof_write_u32_be(FILE *fp, u32 v) {
    fputc((u8) (v >> 24), fp);
    fputc((u8) (v >> 16), fp);
    fputc((u8) (v >> 8), fp);
    fputc((u8) v, fp);
}

static void hprof_write_u64_be(FILE *fp, u64 v) {
    fputc((u8) (v >> 56), fp);
    fputc((u8) (v >> 48), fp);
    fputc((u8) (v >> 40), fp);
    fputc((u8) (v >> 32), fp);
    fputc((u8) (v >> 24), fp);
    fputc((u8) (v >> 16), fp);
    fputc((u8) (v >> 8), fp);
    fputc((u8) v, fp);
}

static void hprof_write_id_be(FILE *fp, u32 id_size, u64 id) {
    if (id_size == 4) {
        hprof_write_u32_be(fp, (u32) id);
    } else {
        hprof_write_u64_be(fp, id);
    }
}

static void hprof_bb_write_id(ByteBuf *bb, u32 id_size, u64 id) {
    if (id_size == 4) {
        bytebuf_write_int(bb, (s32) id);
    } else {
        bytebuf_write_long(bb, (s64) id);
    }
}

static int hprof_write_record(FILE *fp, u8 tag, const void *payload, u32 len) {
    fputc(tag, fp);
    hprof_write_u32_be(fp, 0);
    hprof_write_u32_be(fp, len);
    if (len) {
        if (fwrite(payload, 1, len, fp) != len) {
            return -1;
        }
    }
    return 0;
}

static u64 hprof_make_string_id(u32 id_size, u64 seq) {
    if (id_size == 4) {
        return 0x80000000u | (u32) seq;
    }
    return 0x8000000000000000ull | seq;
}

static u64 hprof_string_id(Hashtable *str2id, u32 id_size, u64 *seq, Utf8String *s) {
    if (!s) return 0;
    HashtableValue v = hashtable_get(str2id, s);
    if (v) {
        return *((u64 *) v);
    }
    Utf8String *key = utf8_create_copy(s);
    u64 *idp = (u64 *) jvm_calloc(sizeof(u64));
    *idp = hprof_make_string_id(id_size, (*seq)++);
    hashtable_put(str2id, key, idp);
    return *idp;
}

static u8 hprof_type_from_field(FieldInfo *fi) {
    if (!fi) return 2;
    if (fi->isrefer) return 2;
    switch (fi->datatype_idx) {
        case DATATYPE_BOOLEAN:
        case DATATYPE_JCHAR:
        case DATATYPE_FLOAT:
        case DATATYPE_DOUBLE:
        case DATATYPE_BYTE:
        case DATATYPE_SHORT:
        case DATATYPE_INT:
        case DATATYPE_LONG:
            return (u8) fi->datatype_idx;
        default:
            return 2;
    }
}

static u8 hprof_type_from_array_class(JClass *clazz) {
    if (!clazz || !clazz->name || clazz->name->length < 2) return DATATYPE_BYTE;
    c8 t = (c8) clazz->name->data[1];
    switch (t) {
        case 'Z': return DATATYPE_BOOLEAN;
        case 'C': return DATATYPE_JCHAR;
        case 'F': return DATATYPE_FLOAT;
        case 'D': return DATATYPE_DOUBLE;
        case 'B': return DATATYPE_BYTE;
        case 'S': return DATATYPE_SHORT;
        case 'I': return DATATYPE_INT;
        case 'J': return DATATYPE_LONG;
        default: return DATATYPE_BYTE;
    }
}

static void hprof_collect_class_closure(Hashset *classes, JClass *clazz) {
    while (clazz) {
        hashset_put(classes, clazz);
        clazz = clazz->superclass;
    }
}

static void hprof_collect_live_classes(Hashset *classes, MemoryBlock *header, u8 mark_cnt) {
    MemoryBlock *mb = header;
    while (mb) {
        if (mb->garbage_mark == mark_cnt) {
            if (mb->type == MEM_TYPE_CLASS) {
                hprof_collect_class_closure(classes, (JClass *) mb);
                if (mb->clazz) hprof_collect_class_closure(classes, mb->clazz);
            } else {
                if (mb->clazz) hprof_collect_class_closure(classes, mb->clazz);
            }
        }
        mb = mb->next;
    }
}

static void hprof_collect_strings_for_class(Hashtable *str2id, u32 id_size, u64 *seq, JClass *clazz) {
    if (!clazz) return;
    hprof_string_id(str2id, id_size, seq, clazz->name);
    FieldPool *fp = &clazz->fieldPool;
    for (s32 i = 0; i < fp->field_used; i++) {
        FieldInfo *fi = &fp->field[i];
        if (fi->name) {
            hprof_string_id(str2id, id_size, seq, fi->name);
        }
    }
}

static int hprof_write_strings(FILE *fp, Hashtable *str2id, u32 id_size) {
    HashtableIterator it;
    hashtable_iterate(str2id, &it);
    while (hashtable_iter_has_more(&it)) {
        Utf8String *k = (Utf8String *) hashtable_iter_next_key(&it);
        u64 id = *((u64 *) hashtable_get(str2id, k));
        ByteBuf *bb = bytebuf_create((u32) (id_size + k->length));
        hprof_bb_write_id(bb, id_size, id);
        bytebuf_write_batch(bb, (c8 *) k->data, k->length);
        int rc = hprof_write_record(fp, 0x01, bb->buf, bb->wp);
        bytebuf_destroy(bb);
        if (rc != 0) return rc;
    }
    return 0;
}

static int hprof_write_load_class(FILE *fp, Hashtable *str2id, u32 id_size, u32 serial, JClass *clazz) {
    if (!clazz || !clazz->name) return 0;
    u64 name_id = *((u64 *) hashtable_get(str2id, clazz->name));
    ByteBuf *bb = bytebuf_create((u32) (4 + id_size + 4 + id_size));
    bytebuf_write_int(bb, (s32) serial);
    hprof_bb_write_id(bb, id_size, (u64) (uintptr_t) clazz);
    bytebuf_write_int(bb, 0);
    hprof_bb_write_id(bb, id_size, name_id);
    int rc = hprof_write_record(fp, 0x02, bb->buf, bb->wp);
    bytebuf_destroy(bb);
    return rc;
}

static void hprof_heap_bb_write_root_unknown(ByteBuf *seg, u32 id_size, u64 obj_id) {
    bytebuf_write_byte(seg, (c8) 0xFF);
    hprof_bb_write_id(seg, id_size, obj_id);
}

// ROOT_THREAD_OBJ (0x08): thread object root
// Format: tag(1) | object ID(id_size) | thread serial(4) | stack trace serial(4)
static void hprof_heap_bb_write_root_thread_obj(ByteBuf *seg, u32 id_size, u64 obj_id, u32 thread_serial) {
    bytebuf_write_byte(seg, (c8) 0x08);
    hprof_bb_write_id(seg, id_size, obj_id);
    bytebuf_write_int(seg, (s32) thread_serial);
    bytebuf_write_int(seg, 0); // stack trace serial
}

static void hprof_heap_bb_write_class_dump(ByteBuf *seg, Hashtable *str2id, u32 id_size, JClass *clazz) {
    bytebuf_write_byte(seg, (c8) 0x20);
    hprof_bb_write_id(seg, id_size, (u64) (uintptr_t) clazz);
    bytebuf_write_int(seg, 0);
    hprof_bb_write_id(seg, id_size, (u64) (uintptr_t) (clazz ? clazz->superclass : NULL));
    hprof_bb_write_id(seg, id_size, (u64) (uintptr_t) (clazz ? clazz->jloader : NULL));
    hprof_bb_write_id(seg, id_size, 0);
    hprof_bb_write_id(seg, id_size, 0);
    hprof_bb_write_id(seg, id_size, 0);
    hprof_bb_write_id(seg, id_size, 0);
    bytebuf_write_int(seg, clazz ? clazz->field_instance_len : 0);
    bytebuf_write_short(seg, 0);

    FieldPool *fp = &clazz->fieldPool;
    s32 static_count = 0;
    s32 inst_count = 0;
    for (s32 i = 0; i < fp->field_used; i++) {
        FieldInfo *fi = &fp->field[i];
        if (fi->access_flags & ACC_STATIC) static_count++;
        else inst_count++;
    }

    bytebuf_write_short(seg, (s16) static_count);
    for (s32 i = 0; i < fp->field_used; i++) {
        FieldInfo *fi = &fp->field[i];
        if (!(fi->access_flags & ACC_STATIC)) continue;
        u64 field_name_id = fi->name ? *((u64 *) hashtable_get(str2id, fi->name)) : 0;
        hprof_bb_write_id(seg, id_size, field_name_id);
        u8 t = hprof_type_from_field(fi);
        bytebuf_write_byte(seg, (c8) t);
        c8 *ptr = getStaticFieldPtr(fi);
        if (t == 2) {
            __refer r = ptr ? getFieldRefer(ptr) : NULL;
            hprof_bb_write_id(seg, id_size, (u64) (uintptr_t) r);
        } else if (t == DATATYPE_BOOLEAN || t == DATATYPE_BYTE) {
            bytebuf_write_byte(seg, ptr ? getFieldByte(ptr) : 0);
        } else if (t == DATATYPE_JCHAR || t == DATATYPE_SHORT) {
            bytebuf_write_short(seg, ptr ? getFieldShort(ptr) : 0);
        } else if (t == DATATYPE_INT) {
            bytebuf_write_int(seg, ptr ? getFieldInt(ptr) : 0);
        } else if (t == DATATYPE_LONG) {
            bytebuf_write_long(seg, ptr ? getFieldLong(ptr) : 0);
        } else if (t == DATATYPE_FLOAT) {
            Int2Float i2f;
            i2f.f = ptr ? getFieldFloat(ptr) : 0.0f;
            bytebuf_write_int(seg, i2f.i);
        } else if (t == DATATYPE_DOUBLE) {
            Long2Double l2d;
            l2d.d = ptr ? getFieldDouble(ptr) : 0.0;
            bytebuf_write_long(seg, l2d.l);
        } else {
            hprof_bb_write_id(seg, id_size, 0);
        }
    }

    bytebuf_write_short(seg, (s16) inst_count);
    for (s32 i = 0; i < fp->field_used; i++) {
        FieldInfo *fi = &fp->field[i];
        if (fi->access_flags & ACC_STATIC) continue;
        u64 field_name_id = fi->name ? *((u64 *) hashtable_get(str2id, fi->name)) : 0;
        hprof_bb_write_id(seg, id_size, field_name_id);
        bytebuf_write_byte(seg, (c8) hprof_type_from_field(fi));
    }
}

static s32 hprof_instance_value_bytes(JClass *clazz, u32 id_size) {
    s32 total = 0;
    JClass *chain[256];
    s32 depth = 0;
    while (clazz && depth < 256) {
        chain[depth++] = clazz;
        clazz = clazz->superclass;
    }
    for (s32 i = depth - 1; i >= 0; i--) {
        FieldPool *fp = &chain[i]->fieldPool;
        for (s32 j = 0; j < fp->field_used; j++) {
            FieldInfo *fi = &fp->field[j];
            if (fi->access_flags & ACC_STATIC) continue;
            if (fi->isrefer) total += (s32) id_size;
            else total += (s32) fi->datatype_bytes;
        }
    }
    return total;
}

static void hprof_heap_bb_write_instance_dump(ByteBuf *seg, u32 id_size, Instance *ins, u8 mark_cnt) {
    bytebuf_write_byte(seg, (c8) 0x21);
    hprof_bb_write_id(seg, id_size, (u64) (uintptr_t) ins);
    bytebuf_write_int(seg, 0);
    hprof_bb_write_id(seg, id_size, (u64) (uintptr_t) (ins ? ins->mb.clazz : NULL));

    s32 bytes = hprof_instance_value_bytes(ins->mb.clazz, id_size);
    bytebuf_write_int(seg, bytes);

    // Check if this is a WeakReference instance
    s32 is_weak_ref = ins && GCFLAG_WEAKREFERENCE_GET(ins->mb.gcflag);

    JClass *chain[256];
    s32 depth = 0;
    JClass *clazz = ins->mb.clazz;
    while (clazz && depth < 256) {
        chain[depth++] = clazz;
        clazz = clazz->superclass;
    }
    for (s32 i = depth - 1; i >= 0; i--) {
        FieldPool *fp = &chain[i]->fieldPool;
        for (s32 j = 0; j < fp->field_used; j++) {
            FieldInfo *fi = &fp->field[j];
            if (fi->access_flags & ACC_STATIC) continue;
            c8 *ptr = getInstanceFieldPtr(ins, fi);
            u8 t = hprof_type_from_field(fi);
            if (t == 2) {
                __refer r = ptr ? getFieldRefer(ptr) : NULL;
                // For WeakReference.target field: if target object is not marked, write NULL
                // This is consistent with GC behavior (GC skips marking weak reference targets)
                if (is_weak_ref && fi->is_ref_target && r) {
                    MemoryBlock *target_mb = (MemoryBlock *) r;
                    if (target_mb->garbage_mark != mark_cnt) {
                        r = NULL; // Target not marked, treat as cleared
                    }
                }
                hprof_bb_write_id(seg, id_size, (u64) (uintptr_t) r);
            } else if (t == DATATYPE_BOOLEAN || t == DATATYPE_BYTE) {
                bytebuf_write_byte(seg, ptr ? getFieldByte(ptr) : 0);
            } else if (t == DATATYPE_JCHAR || t == DATATYPE_SHORT) {
                bytebuf_write_short(seg, ptr ? getFieldShort(ptr) : 0);
            } else if (t == DATATYPE_INT) {
                bytebuf_write_int(seg, ptr ? getFieldInt(ptr) : 0);
            } else if (t == DATATYPE_LONG) {
                bytebuf_write_long(seg, ptr ? getFieldLong(ptr) : 0);
            } else if (t == DATATYPE_FLOAT) {
                Int2Float i2f;
                i2f.f = ptr ? getFieldFloat(ptr) : 0.0f;
                bytebuf_write_int(seg, i2f.i);
            } else if (t == DATATYPE_DOUBLE) {
                Long2Double l2d;
                l2d.d = ptr ? getFieldDouble(ptr) : 0.0;
                bytebuf_write_long(seg, l2d.l);
            } else {
                bytebuf_write_byte(seg, 0);
            }
        }
    }
}

static void hprof_heap_bb_write_object_array_dump(ByteBuf *seg, u32 id_size, Instance *arr) {
    bytebuf_write_byte(seg, (c8) 0x22);
    hprof_bb_write_id(seg, id_size, (u64) (uintptr_t) arr);
    bytebuf_write_int(seg, 0);
    bytebuf_write_int(seg, arr ? arr->arr_length : 0);
    hprof_bb_write_id(seg, id_size, (u64) (uintptr_t) (arr ? arr->mb.clazz : NULL));
    if (!arr) return;
    for (s32 i = 0; i < arr->arr_length; i++) {
        s64 v = jarray_get_field(arr, i);
        hprof_bb_write_id(seg, id_size, (u64) (uintptr_t) (void *) (intptr_t) v);
    }
}

static void hprof_heap_bb_write_primitive_array_dump(ByteBuf *seg, u32 id_size, Instance *arr) {
    bytebuf_write_byte(seg, (c8) 0x23);
    hprof_bb_write_id(seg, id_size, (u64) (uintptr_t) arr);
    bytebuf_write_int(seg, 0);
    bytebuf_write_int(seg, arr ? arr->arr_length : 0);
    u8 t = hprof_type_from_array_class(arr ? arr->mb.clazz : NULL);
    bytebuf_write_byte(seg, (c8) t);
    if (!arr) return;
    c8 *p = arr->arr_body;
    if (t == DATATYPE_BOOLEAN || t == DATATYPE_BYTE) {
        bytebuf_write_batch(seg, p, arr->arr_length);
    } else if (t == DATATYPE_JCHAR || t == DATATYPE_SHORT) {
        for (s32 i = 0; i < arr->arr_length; i++) {
            s16 v = *((s16 *) (p + i * 2));
            bytebuf_write_short(seg, v);
        }
    } else if (t == DATATYPE_INT || t == DATATYPE_FLOAT) {
        for (s32 i = 0; i < arr->arr_length; i++) {
            s32 v = *((s32 *) (p + i * 4));
            bytebuf_write_int(seg, v);
        }
    } else if (t == DATATYPE_LONG || t == DATATYPE_DOUBLE) {
        for (s32 i = 0; i < arr->arr_length; i++) {
            s64 v = *((s64 *) (p + i * 8));
            bytebuf_write_long(seg, v);
        }
    } else {
        bytebuf_write_batch(seg, p, arr->arr_length);
    }
}

static int hprof_flush_segment(FILE *fp, ByteBuf *seg) {
    if (!seg->wp) return 0;
    int rc = hprof_write_record(fp, 0x1C, seg->buf, seg->wp);
    seg->rp = 0;
    seg->wp = 0;
    return rc;
}

int hprof_write_heap(GcCollector *collector, const char *path) {
    int flags = 0;
    if (!collector || !collector->jvm || !path || !path[0]) return -1;

    FILE *fp = fopen(path, "wb");
    if (!fp) {
        return -3;
    }

    const char hdr[] = "JAVA PROFILE 1.0.2";
    fwrite(hdr, 1, sizeof(hdr) - 1, fp);
    fputc(0, fp);

    u32 id_size = (u32) sizeof(__refer);
    hprof_write_u32_be(fp, id_size);
    hprof_write_u64_be(fp, (u64) currentTimeMillis());

    Hashtable *str2id = hashtable_create(UNICODE_STR_HASH_FUNC, UNICODE_STR_EQUALS_FUNC);
    hashtable_register_free_functions(str2id, (HashtableKeyFreeFunc) utf8_destroy, (HashtableValueFreeFunc) jvm_free);
    u64 str_seq = 1;

    Hashset *classes = hashset_create();
    hprof_collect_live_classes(classes, collector->header, collector->mark_cnt);

    HashsetIterator ci;
    hashset_iterate(classes, &ci);
    while (hashset_iter_has_more(&ci)) {
        JClass *cl = (JClass *) hashset_iter_next_key(&ci);
        hprof_collect_strings_for_class(str2id, id_size, &str_seq, cl);
    }

    int rc = hprof_write_strings(fp, str2id, id_size);
    if (rc != 0) goto cleanup;

    u32 class_serial = 1;
    hashset_iterate(classes, &ci);
    while (hashset_iter_has_more(&ci)) {
        JClass *cl = (JClass *) hashset_iter_next_key(&ci);
        rc = hprof_write_load_class(fp, str2id, id_size, class_serial++, cl);
        if (rc != 0) goto cleanup;
    }

    ByteBuf *seg = bytebuf_create(1024 * 1024);

    // Thread stack frame references as ROOT_UNKNOWN
    for (s32 i = 0; i < collector->runtime_refer_copy->length; i++) {
        __refer r = arraylist_get_value(collector->runtime_refer_copy, i);
        if (r) hprof_heap_bb_write_root_unknown(seg, id_size, (u64) (uintptr_t) r);
    }
    HashsetIterator hi;
    hashset_iterate(collector->objs_holder, &hi);
    while (hashset_iter_has_more(&hi)) {
        HashsetKey k = hashset_iter_next_key(&hi);
        if (k) hprof_heap_bb_write_root_unknown(seg, id_size, (u64) (uintptr_t) k);
    }
    
    // Boot classloader classes as ROOT
    HashtableIterator hti;
    hashtable_iterate(collector->jvm->boot_classloader->classes, &hti);
    while (hashtable_iter_has_more(&hti)) {
        HashtableValue v = hashtable_iter_next_value(&hti);
        if (v) hprof_heap_bb_write_root_unknown(seg, id_size, (u64) (uintptr_t) v);
    }
    
    // Custom classloader classes as ROOT
    MiniJVM *jvm = collector->jvm;
    for (s32 i = 0; i < jvm->classloaders->length; i++) {
        PeerClassLoader *pcl = arraylist_get_value_unsafe(jvm->classloaders, i);
        hashtable_iterate(pcl->classes, &hti);
        while (hashtable_iter_has_more(&hti)) {
            HashtableValue v = hashtable_iter_next_value(&hti);
            if (v) hprof_heap_bb_write_root_unknown(seg, id_size, (u64) (uintptr_t) v);
        }
    }
    
    // Thread objects as ROOT_THREAD_OBJ
    u32 thread_serial = 1;
    MemoryBlock *mb_scan = collector->header;
    while (mb_scan) {
        if (mb_scan->garbage_mark == collector->mark_cnt && 
            mb_scan->type == MEM_TYPE_INS && 
            GCFLAG_JTHREAD_GET(mb_scan->gcflag)) {
            hprof_heap_bb_write_root_thread_obj(seg, id_size, (u64) (uintptr_t) mb_scan, thread_serial++);
        }
        mb_scan = mb_scan->next;
    }

    hashset_iterate(classes, &ci);
    while (hashset_iter_has_more(&ci)) {
        JClass *cl = (JClass *) hashset_iter_next_key(&ci);
        hprof_heap_bb_write_class_dump(seg, str2id, id_size, cl);
        if (seg->wp >= 4 * 1024 * 1024) {
            rc = hprof_flush_segment(fp, seg);
            if (rc != 0) goto cleanup_seg;
        }
    }

    MemoryBlock *mb = collector->header;
    while (mb) {
        if (mb->garbage_mark == collector->mark_cnt) {
            if (mb->type == MEM_TYPE_INS) {
                hprof_heap_bb_write_instance_dump(seg, id_size, (Instance *) mb, collector->mark_cnt);
            } else if (mb->type == MEM_TYPE_ARR) {
                Instance *arr = (Instance *) mb;
                if (isDataReferByIndex(arr->mb.arr_type_index)) {
                    hprof_heap_bb_write_object_array_dump(seg, id_size, arr);
                } else {
                    hprof_heap_bb_write_primitive_array_dump(seg, id_size, arr);
                }
            }
            if (seg->wp >= 4 * 1024 * 1024) {
                rc = hprof_flush_segment(fp, seg);
                if (rc != 0) goto cleanup_seg;
            }
        }
        mb = mb->next;
    }

    rc = hprof_flush_segment(fp, seg);
    if (rc != 0) goto cleanup_seg;

    rc = hprof_write_record(fp, 0x2C, NULL, 0);

cleanup_seg:
    bytebuf_destroy(seg);
cleanup:
    fclose(fp);
    hashset_destroy(classes);
    hashtable_destroy(str2id);

    return rc == 0 ? 0 : -4;
}

int hprof_dump_heap(Runtime *runtime, const char *path, int flags) {
    if (!runtime || !runtime->jvm || !path || !path[0]) return -1;
    GcCollector *collector = runtime->jvm->collector;

    spin_lock(&collector->lock);
    if (collector->dump_flag != 0) {
        spin_unlock(&collector->lock);
        return -5;
    }
    if (collector->dump_path) {
        utf8_destroy(collector->dump_path);
        collector->dump_path = NULL;
    }
    collector->dump_path = utf8_create_c(path);
    collector->dump_flags = flags;
    collector->dump_rc = -6;
    collector->dump_flag = 1;
    collector->lastgc = 0;
    spin_unlock(&collector->lock);

    while (collector->dump_flag != 3) {
        if (collector->_garbage_thread_status == GARBAGE_THREAD_DEAD || collector->_garbage_thread_status == GARBAGE_THREAD_STOP) {
            break;
        }
        jthread_sleep(runtime, 100);
    }

    spin_lock(&collector->lock);
    s32 rc = collector->dump_rc;
    if (collector->dump_path) {
        utf8_destroy(collector->dump_path);
        collector->dump_path = NULL;
    }
    collector->dump_flag = 0;
    spin_unlock(&collector->lock);

    return rc;
}
