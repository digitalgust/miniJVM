//
// Created by Gust on 2020/5/9.
//

#include <string.h>
#include "jvm.h"
#include "garbage.h"

/**
 * all method would call by llvm method
 */



void print_debug(s32 v) {
    printf("lldebug: %d \n", v);
}

void print_ptr(s64 v) {
    printf("ll_ptr: %llx (%lld)\n", v, v);
}

void print_mem(void *ptr, s32 size) {
    c8 *start = (c8 *) ptr;
    c8 *end = start + size;
    s32 i = 0;
    while (start < end) {
        if ((i++ % 16) == 0)printf("\n[%8llx]", (s64) (intptr_t) start);
        printf("%2x ", *start);
        start++;
    }
    printf("\n");
}

s32 func_ptr_size() {
    return sizeof(__refer);
}


JObject *new_instance_with_classraw(JThreadRuntime *runtime, ClassRaw *raw) {
    s32 insSize = raw->ins_size;
    //printf("ins size :%d\n", insSize);
    JObject *ins = (JObject *) jvm_calloc(insSize);
    ins->prop.heap_size = insSize;
    if (!raw->clazz) {
        class_clinit(runtime, get_utf8str_by_utfraw_index(raw->name));
    }
    ins->prop.clazz = raw->clazz;
//    if (!ins->prop.clazz) {
//        JClass *clazz = classes_get(get_utf8str_by_utfraw_index(raw->name));
//        s32 debug = 1;
//    }
    ins->prop.members = &ins[1];
    ins->prop.type = INS_TYPE_OBJECT;
    ins->vm_table = raw->vmtable;
    gc_refer_reg(runtime, ins);
    return ins;
}

JObject *new_instance_with_classraw_index(JThreadRuntime *runtime, s32 classIndex) {
    ClassRaw *raw = &g_classes[classIndex];
    return new_instance_with_classraw(runtime, raw);
}

JObject *new_instance_with_nameindex(JThreadRuntime *runtime, s32 classNameIndex) {
    ClassRaw *raw = get_class_by_nameIndex(classNameIndex)->raw;
    return new_instance_with_classraw(runtime, raw);
}

JObject *new_instance_with_name(JThreadRuntime *runtime, c8 const *className) {
    ClassRaw *raw = find_classraw(className);
    return new_instance_with_classraw(runtime, raw);
}

JObject *new_instance_with_class(JThreadRuntime *runtime, JClass *clazz) {
    return new_instance_with_classraw(runtime, clazz->raw);
}

JObject *new_instance_with_classraw_index_and_init(JThreadRuntime *runtime, s32 classIndex) {
    JObject *jobj = new_instance_with_classraw_index(runtime, classIndex);
    instance_init(runtime, jobj);
    return jobj;
}


s32 jobject_destroy(JObject *ins) {
    jthreadlock_destory(&ins->prop);
    jvm_free(ins);

    return 0;
}

JObject *construct_string_with_utfraw_index(JThreadRuntime *runtime, s32 utfIndex) {
    if (utfIndex < 0)return NULL;
    UtfRaw *utfRaw = &g_strings[utfIndex];
    if (utfRaw->jstr) {
        return utfRaw->jstr;
    }
    ClassRaw *classraw = g_procache.java_lang_string_raw;
    if (!classraw) {
        return NULL;
    }
    JObject *ins = construct_string_with_cstr_and_size(runtime, utfRaw->str, utfRaw->utf8_size);
    utfRaw->jstr = ins;
    gc_refer_hold(ins);
    return ins;
}

JObject *construct_string_with_cstr(JThreadRuntime *runtime, c8 const *str) {
    if (!str)return NULL;
    s32 c8len = strlen(str);
    return construct_string_with_cstr_and_size(runtime, str, c8len);
}

JObject *construct_string_with_cstr_and_size(JThreadRuntime *runtime, c8 const *str, s32 size) {
    if (!str)return NULL;

    JClass *clazz = g_procache.java_lang_string_raw->clazz;
    if (!clazz) {
        return NULL;
    }
    JObject *ins = new_instance_with_class(runtime, clazz);

    u16 *buf = (u16 *) jvm_calloc(size * data_type_bytes[DATATYPE_JCHAR]);
    s32 u16len = utf8_2_unicode(str, buf, size);
    JArray *carr = multi_array_create_by_typename(runtime, &u16len, 1, "[C");
    carr->prop.arr_length = u16len;
    memcpy(carr->prop.as_s8_arr, buf, u16len * data_type_bytes[DATATYPE_JCHAR]);
    jvm_free(buf);
    MethodRaw *methodRaw = g_procache.java_lang_string_init_C_raw;
    gc_refer_hold(ins);
    ((void (*)(__refer, __refer, __refer)) methodRaw->func_ptr)(runtime, ins, carr);
    exception_check_print(runtime);
    gc_refer_release(ins);
    return ins;
}

JObject *construct_string_with_ustr(JThreadRuntime *runtime, Utf8String *str) {
    if (!str)return NULL;
    return construct_string_with_cstr(runtime, utf8_cstr(str));
}


static JArray *multi_array_create_impl(JThreadRuntime *runtime, s32 *dimm, s32 dimm_count, s32 deep, JClass *clazz) {  // create array  [[[Ljava/lang/Object;  para: [3,2,1],3,8
    s32 arr_len = *(dimm + deep);
    //printf("multiarray :%d %d %d %d %d %d\n", dimm[0], dimm[1], dimm_count, index, cellBytes, arr_len);
    JClass *cell_class = clazz->array_cell_class;
    c8 typetag = utf8_char_at(clazz->name, 1);
    s32 cellBytes = data_type_bytes[getDataTypeIndex(typetag)];
    if (cellBytes != 0 && deep == dimm_count - 1) {// none object array
        s32 totalBytes = arr_len * cellBytes + sizeof(JArray);
        JArray *arr = (JArray *) jvm_calloc(totalBytes);
        arr->prop.heap_size = totalBytes;
        arr->prop.clazz = clazz;
        arr->vm_table = g_procache.java_lang_object_raw->vmtable;
        arr->prop.type = INS_TYPE_ARRAY;
        arr->prop.arr_length = arr_len;
        arr->prop.arr_type = getDataTypeIndex(typetag);
        arr->prop.members = &arr[1];
        gc_refer_reg(runtime, arr);
        return arr;
    } else {
        deep++;
        s32 totalBytes = arr_len * sizeof(char *) + sizeof(JArray);
        JArray *arr = (JArray *) jvm_calloc(totalBytes);
        arr->prop.heap_size = totalBytes;
        arr->prop.clazz = cell_class;
        arr->vm_table = g_procache.java_lang_object_raw->vmtable;
        arr->prop.type = INS_TYPE_ARRAY;
        arr->prop.arr_length = arr_len;
        arr->prop.arr_type = getDataTypeIndex(typetag);
        arr->prop.members = &arr[1];
        s32 i = 0;
        for (i = 0; i < arr_len; i++) {
            //printf("multiarray sub :%d %d\n", i, index);
            arr->prop.as_obj_arr[i] = (JObject *) multi_array_create_impl(runtime, dimm, dimm_count, deep, cell_class);
        }
        gc_refer_reg(runtime, arr);
        return arr;
    }
}

JArray *multi_array_create_by_typename(JThreadRuntime *runtime, s32 *dimm, s32 dimm_count, c8 const *type_name) {  // create array  [[[Ljava/lang/Object;  para: [3,2,1],3,[Ljava/lang/String;
    Utf8String *cache = (Utf8String *) tss_get(TLS_KEY_UTF8STR_CACHE);
    utf8_clear(cache);
    utf8_append_c(cache, type_name);
    return multi_array_create_impl(runtime, dimm, dimm_count, 0, array_class_create_get(cache));
}

JArray *multi_array_create(JThreadRuntime *runtime, s32 *dimm, s32 dimm_count, JClass *clazz) {  // create array  [[[Ljava/lang/Object;  para: [3,2,1],3,8
//    if (type_name_idx == 223) {
//        int debug = 1;
//    }
//    Utf8String *signature = get_utf8str(&g_strings[type_name_idx]);
//    Utf8String *cache = tss_get(TLS_KEY_UTF8STR_CACHE);
//    utf8_clear(cache);
//    utf8_append(cache, signature);
    return multi_array_create_impl(runtime, dimm, dimm_count, 0, clazz);
}

s32 jarray_destroy(JArray *arr) {
    if (arr && arr->prop.type == INS_TYPE_ARRAY) {
        jthreadlock_destory(&arr->prop);
        arr->prop.thread_lock = NULL;
        arr->prop.arr_length = -1;
        jvm_free(arr);
    }
    return 0;
}

s32 instance_of_classname_index(JObject *jobj, s32 classNameIdx) {
    JClass *cinfo = get_class_by_nameIndex(classNameIdx);
    return instance_of(&jobj->prop, cinfo);
}


void throw_exception(JThreadRuntime *runtime, JObject *jobj) {
    // StackFrame *cur = runtime->tail;
    runtime->exception = jobj;
}

JObject *construct_and_throw_exception(JThreadRuntime *runtime, s32 classrawIndex, s32 bytecodeIndex, s32 lineNo) {
    JObject *jobj = new_instance_with_classraw_index_and_init(runtime, classrawIndex);
    runtime->tail->bytecodeIndex = bytecodeIndex;
    runtime->tail->lineNo = lineNo;
    runtime->exception = jobj;
    return jobj;
}

s32 find_exception_handler_index(JThreadRuntime *runtime) {
    StackFrame *cur = runtime->tail;
    MethodRaw *methodRaw = &g_methods[cur->methodRawIndex];
    ExceptionTable *extable = methodRaw->extable;
    if (!extable)return -1;//no handler

    s32 rise = cur->bytecodeIndex;
    s32 i;
//    for (i = 0; i < cur->labtable->size; i++) {
//        printf("%d %llx\n", i, (intptr_t) labels[i]);
//    }

    for (i = 0; i < extable->size; i++) {
        s32 start = extable->exception[i].startIdxOfBC;
        s32 end = extable->exception[i].endIdxOfBC;
        s32 classNameIdx = extable->exception[i].exceptionClassName;
        JClass *catchClass = classNameIdx < 0 ? NULL : get_class_by_nameIndex(classNameIdx);
        //printf("%s %d  %d %d %s\n", g_strings[methodRaw->name].str, start, rise, end, utf8_cstr(catchClass->name));
        if (rise >= start && rise < end
            && (instance_of((InstProp *) runtime->exception, catchClass) || !catchClass)) {//catchClass==NULL that's finally
            arraylist_clear(runtime->stacktrack);
            arraylist_clear(runtime->lineNo);
            runtime->exception = NULL;
            cur->bytecodeIndex = 0;
            cur->lineNo = -1;
            return i;
        }
    }
    arraylist_push_back(runtime->stacktrack, (__refer) (intptr_t) cur->methodRawIndex);
    arraylist_push_back(runtime->lineNo, (__refer) (intptr_t) cur->lineNo);
    //the last item is the default handler
    //printf("why not found handler\n");
    return -1;
}

s32 exception_check_print(JThreadRuntime *runtime) {
    //
    if (runtime->exception) {
        jvm_printf("Exception in thread [%llx] %s\n", (s64) (intptr_t) runtime->jthread, utf8_cstr(runtime->exception->prop.clazz->name));
        s32 i, imax;
        for (i = 0, imax = runtime->stacktrack->length; i < imax; i++) {
            MethodRaw methodRaw = g_methods[(s32) (intptr_t) arraylist_get_value(runtime->stacktrack, i)];
            c8 *className = g_strings[methodRaw.class_name].str;
            c8 *methodName = g_strings[methodRaw.name].str;
            ClassRaw *classRaw = find_classraw(className);
            jvm_printf("    at %s.%s(%s:%d)\n",
                       className,
                       methodName,
                       g_strings[classRaw->source_name].str, (s32) (intptr_t) arraylist_get_value(runtime->lineNo, i));
        }
        return 1;
    }
    return 0;
}


void check_suspend_and_pause(JThreadRuntime *runtime) {
    if (runtime->suspend_count && !runtime->no_pause) {
        runtime->is_suspend = 1;
        garbage_thread_lock();
        while (runtime->suspend_count) {
            garbage_thread_notifyall();
            garbage_thread_timedwait(5);
        }
        runtime->is_suspend = 0;
        //jvm_printf(".");
        garbage_thread_unlock();
    }
}


void instance_init(JThreadRuntime *runtime, JObject *ins) {
    if (ins) {
        MethodInfo *mi = find_methodInfo_by_name(utf8_cstr(ins->prop.clazz->name), "<init>", "()V");
        void (*func_ptr)(__refer, __refer) =mi->func_ptr;
        func_ptr(runtime, ins);
    }
}

InstProp *instance_copy(JThreadRuntime *runtime, InstProp *src, s32 deep_copy) {
    s32 ins_len = 0;
    if (src->type == INS_TYPE_OBJECT) {
        ins_len = src->clazz->raw->ins_size;
    } else if (src->type == INS_TYPE_ARRAY) {
        ins_len = src->arr_length * data_type_bytes[src->arr_type] + sizeof(JArray);
    }
    InstProp *dst = (InstProp *) jvm_malloc(ins_len);
    //memcpy(dst, src, sizeof(ins_len));
    dst->type = src->type;
    dst->thread_lock = NULL;
    dst->garbage_reg = 0;
    dst->garbage_mark = 0;
    dst->clazz = src->clazz;
    if (src->type == INS_TYPE_OBJECT) {
        JObject *jobj = (JObject *) dst;
        JClass *clazz = src->clazz;
        s32 fileds_len = ins_len - sizeof(JObject);
        if (fileds_len) {
            dst->members = &jobj[1];//jvm_malloc(fileds_len);
            memcpy(dst->members, src->members, fileds_len);
            if (deep_copy) {
                s32 i, len;
                while (clazz) {
                    ArrayList *fiList = clazz->fields;
                    for (i = 0, len = fiList->length; i < len; i++) {
                        FieldInfo *fi = (FieldInfo *) arraylist_get_value_unsafe(fiList, i);
                        if (!fi->is_static && fi->is_refer) {
                            __refer ref = *((__refer *) (((c8 *) src) + fi->offset_ins));
                            if (ref) {
                                InstProp *new_ins = (InstProp *) instance_copy(runtime, (InstProp *) (ref), deep_copy);
                                *((__refer *) (((c8 *) dst) + fi->offset_ins)) = new_ins;
                            }
                        }
                    }
                    clazz = getSuperClass(clazz);
                }
            }
        }
    } else if (src->type == INS_TYPE_ARRAY) {
        JArray *jarr = (JArray *) dst;
        dst->arr_length = src->arr_length;
        dst->arr_type = src->arr_type;
        s32 size = src->arr_length * data_type_bytes[src->arr_type];
        dst->members = &jarr[1];//
        if (isDataReferByIndex(src->arr_type) && deep_copy) {
            s32 i;
            __refer ref;
            for (i = 0; i < dst->arr_length; i++) {
                ref = src->as_obj_arr[i];
                if (ref) {
                    InstProp *new_ins = (InstProp *) instance_copy(runtime, (InstProp *) (ref), deep_copy);
                    dst->as_obj_arr[i] = new_ins;
                }
            }
        } else {
            memcpy(dst->as_s8_arr, src->as_s8_arr, size);
        }
    }
    gc_refer_reg(runtime, dst);
    return dst;
}
