/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
#include "jvm.h"
#include "jvm_util.h"
#include "garbage.h"


//===============================    创建及加载  ==================================



JClass *class_create(Runtime *runtime) {
    s32 jcsize = sizeof(JClass);
    JClass *clazz = jvm_calloc(jcsize);
    clazz->mb.heap_size = jcsize;
    clazz->mb.clazz = clazz;
    clazz->mb.type = MEM_TYPE_CLASS;
    clazz->field_instance_len = 0;
    clazz->field_static = NULL;
    clazz->status = CLASS_STATUS_RAW;
    clazz->_load_class_from_bytes = _LOAD_CLASS_FROM_BYTES;
    //
    jthreadlock_create(runtime, &clazz->mb);
    constant_list_create(clazz);
    clazz->arr_class_type = pairlist_create(16);
    clazz->insFieldPtrIndex = arraylist_create(8);
    clazz->staticFieldPtrIndex = arraylist_create(4);
    clazz->supers = arraylist_create(4);
    gc_obj_reg(runtime, clazz);
    return clazz;
}

s32 class_destory(JClass *clazz) {
    //jvm_printf("class_destory   : %s\n", utf8_cstr(clazz->name));
    _DESTORY_CLASS(clazz);
    pairlist_destory(clazz->arr_class_type);
    arraylist_destory(clazz->insFieldPtrIndex);
    arraylist_destory(clazz->staticFieldPtrIndex);
    arraylist_destory(clazz->supers);
    jvm_free(clazz);
    return 0;
}

void constant_list_create(JClass *clazz) {
    clazz->constantPool.utf8CP = arraylist_create(0);
    clazz->constantPool.classRef = arraylist_create(0);
    clazz->constantPool.stringRef = arraylist_create(0);
    clazz->constantPool.fieldRef = arraylist_create(0);
    clazz->constantPool.methodRef = arraylist_create(0);
    clazz->constantPool.interfaceMethodRef = arraylist_create(0);
}

void constant_list_destory(JClass *clazz) {
    arraylist_destory(clazz->constantPool.utf8CP);
    arraylist_destory(clazz->constantPool.classRef);
    arraylist_destory(clazz->constantPool.stringRef);
    arraylist_destory(clazz->constantPool.fieldRef);
    arraylist_destory(clazz->constantPool.methodRef);
    arraylist_destory(clazz->constantPool.interfaceMethodRef);
}

void class_clear_refer(PeerClassLoader *cloader, JClass *clazz) {
    s32 i, len;
    if (clazz->field_static) {
        FieldPool *fp = &clazz->fieldPool;
        for (i = 0; i < fp->field_used; i++) {
            FieldInfo *fi = &fp->field[i];
//        if (utf8_equals_c(fi->name, "zones")) {
//            s32 debug = 1;
//        }
            if ((fi->access_flags & ACC_STATIC) != 0 && fi->isrefer) {
                c8 *ptr = getStaticFieldPtr(fi);
                if (ptr) {
                    setFieldRefer(ptr, NULL);
                }
            }
        }
        if (clazz->field_static)jvm_free(clazz->field_static);
        clazz->field_static = NULL;
    }
    ArrayList *utf8list = clazz->constantPool.utf8CP;
    for (i = 0, len = utf8list->length; i < len; i++) {
        ConstantUTF8 *cutf = arraylist_get_value(utf8list, i);
        gc_obj_release(cloader->jvm->collector, cutf->jstr);
    }
    gc_obj_release(cloader->jvm->collector, clazz->ins_class);
    clazz->ins_class = NULL;
}
//===============================    初始化相关  ==================================

/**
 * 需要在所有类加载入系统之后
 * 初始化静态变量区，及生成实例模板
 * @param clazz class
 * @return ret
 */
s32 class_prepar(Instance *loader, JClass *clazz, Runtime *runtime) {
    if (clazz->status >= CLASS_STATUS_PREPARING)return 0;
    clazz->status = CLASS_STATUS_PREPARING;

    s32 superid = clazz->cff.super_class;
    if (superid && !clazz->superclass) {
        ConstantClassRef *ccf = class_get_constant_classref(clazz, superid);
        if (ccf) {
            Utf8String *clsName_u = class_get_utf8_string(clazz, ccf->stringIndex);
            JClass *other = classes_load_get_without_resolve(loader, clsName_u, runtime);
            clazz->superclass = other;
        } else {
            jvm_printf("error get superclass , class: %s\n", utf8_cstr(clazz->name));
        }
    }

    find_supers(clazz, runtime);

    int i;

//    if (utf8_equals_c(clazz->name, "espresso/parser/JavaParser")) {
//        int debug = 1;
//    }

    FieldInfo *f = clazz->fieldPool.field;
    //计算不同种类变量长度
    s32 static_len = 0;
    s32 instance_len = 0;
    s32 field_count = clazz->fieldPool.field_used;
    s32 *mem_align_order = jvm_calloc(field_count * sizeof(s32));//fieldwidth order 8,4,2,1
    s32 order_idx = 0;
    s32 datawidth = 8;//
    //memory align begin
    //先排8字节成员,紧跟4字节成员,再跟2字节成员,最后排1字节成员
    while (datawidth > 0) {//first align width=8B ,then width=4B, then width=2,then 1
        for (i = 0; i < field_count; i++) {
            s32 width = DATA_TYPE_BYTES[f[i].datatype_idx];
            if (width == datawidth) {
                mem_align_order[order_idx] = i;
                order_idx++;
            }
        }
        datawidth /= 2;
    }
    for (i = 0; i < field_count; i++) {
        FieldInfo *fi = &f[mem_align_order[i]];
        s32 width = DATA_TYPE_BYTES[fi->datatype_idx];
        if (fi->access_flags & ACC_STATIC) {//静态变量
            fi->offset = static_len;
            static_len += width;
        } else {//实例变量
            fi->offset = instance_len;
            instance_len += width;
        }
        fi->_this_class = clazz;
    }

    jvm_free(mem_align_order);
    s32 align = 8;
    static_len = static_len / align * align + ((static_len % align) > 0 ? align : 0); // 8 byte align
    instance_len = instance_len / align * align + ((instance_len % align) > 0 ? align : 0); // 8 byte align
    //memory align end

    //静态变量分配
    clazz->field_static_len = static_len;
    clazz->field_static = jvm_calloc(clazz->field_static_len);


    //生成实例变量模板
    if (clazz->superclass) {
        clazz->field_instance_start = clazz->superclass->field_instance_len;
        clazz->field_instance_len = clazz->field_instance_start + instance_len;
        //实例变量区前面是继承的父类变量，后面是自己的变量
        //memcpy((clazz->field_instance_template), (superclass->field_instance_template), clazz->field_instance_start);
    } else {
        clazz->field_instance_start = 0;
        //实例变量区前面是继承的父类变量，后面是自己的变量
        clazz->field_instance_len = clazz->field_instance_start + instance_len;
    }


    //提前计算类成员的偏移量，提高执行速度
    for (i = 0; i < clazz->fieldPool.field_used; i++) {
        FieldInfo *fi = &clazz->fieldPool.field[i];
        fi->offset_instance = fi->_this_class->field_instance_start + fi->offset;
    }

    ShortCut *jvm_runtime_cache = &runtime->jvm->shortcut;
    //预计算字段在实例内存中的偏移，节约运行时时间
    if (utf8_equals_c(clazz->name, STR_CLASS_JAVA_LANG_CLASS)) {
        FieldInfo *fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_CLASS, STR_FIELD_CLASSHANDLE, "J", NULL, runtime);
        jvm_runtime_cache->class_classHandle = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_CLASS, STR_FIELD_CLASSLOADER, "Ljava/lang/ClassLoader;", NULL, runtime);
        jvm_runtime_cache->class_classLoader = fi;
    } else if (utf8_equals_c(clazz->name, STR_CLASS_JAVA_LANG_STRING)) {
        FieldInfo *fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_STRING, STR_FIELD_COUNT, "I", NULL, runtime);
        jvm_runtime_cache->string_count = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_STRING, STR_FIELD_OFFSET, "I", NULL, runtime);
        jvm_runtime_cache->string_offset = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_STRING, STR_FIELD_VALUE, "[C", NULL, runtime);
        jvm_runtime_cache->string_value = fi;
    } else if (utf8_equals_c(clazz->name, STR_CLASS_JAVA_LANG_STRINGBUILDER)) {
        FieldInfo *fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_STRINGBUILDER, STR_FIELD_COUNT, "I", NULL, runtime);
        jvm_runtime_cache->stringbuilder_count = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_STRINGBUILDER, STR_FIELD_VALUE, "[C", NULL, runtime);
        jvm_runtime_cache->stringbuilder_value = fi;
    } else if (utf8_equals_c(clazz->name, STR_CLASS_JAVA_LANG_THREAD)) {
        FieldInfo *fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_THREAD, STR_FIELD_NAME, "[C", NULL, runtime);
        jvm_runtime_cache->thread_name = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_THREAD, STR_FIELD_STACKFRAME, "J", NULL, runtime);
        jvm_runtime_cache->thread_stackFrame = fi;
    } else if (utf8_equals_c(clazz->name, STR_CLASS_JAVA_LANG_STACKTRACE)) {
        FieldInfo *fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_STACKTRACE, "declaringClass", STR_INS_JAVA_LANG_STRING, NULL, runtime);
        jvm_runtime_cache->stacktrace_declaringClass = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_STACKTRACE, "methodName", STR_INS_JAVA_LANG_STRING, NULL, runtime);
        jvm_runtime_cache->stacktrace_methodName = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_STACKTRACE, "fileName", STR_INS_JAVA_LANG_STRING, NULL, runtime);
        jvm_runtime_cache->stacktrace_fileName = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_STACKTRACE, "lineNumber", "I", NULL, runtime);
        jvm_runtime_cache->stacktrace_lineNumber = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_STACKTRACE, "parent", STR_INS_JAVA_LANG_STACKTRACEELEMENT, NULL, runtime);
        jvm_runtime_cache->stacktrace_parent = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_STACKTRACE, "declaringClazz", STR_INS_JAVA_LANG_CLASS, NULL, runtime);
        jvm_runtime_cache->stacktrace_declaringClazz = fi;
    } else if (utf8_equals_c(clazz->name, STR_CLASS_ORG_MINI_REFLECT_DIRECTMEMOBJ)) {
        FieldInfo *fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_ORG_MINI_REFLECT_DIRECTMEMOBJ, "memAddr", "J", NULL, runtime);
        jvm_runtime_cache->dmo_memAddr = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_ORG_MINI_REFLECT_DIRECTMEMOBJ, "length", "I", NULL, runtime);
        jvm_runtime_cache->dmo_length = fi;
        fi = find_fieldInfo_by_name_c(STR_CLASS_ORG_MINI_REFLECT_DIRECTMEMOBJ, "typeDesc", "C", NULL, runtime);
        jvm_runtime_cache->dmo_desc = fi;
    } else if (utf8_equals_c(clazz->name, STR_CLASS_ORG_MINI_REFLECT_LAUNCHER)) {
        jvm_runtime_cache->launcher_loadClass = find_methodInfo_by_name_c(STR_CLASS_ORG_MINI_REFLECT_LAUNCHER, "loadClass", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/Class;", NULL, runtime);
        jvm_runtime_cache->launcher_getSystemClassLoader = find_methodInfo_by_name_c(STR_CLASS_ORG_MINI_REFLECT_LAUNCHER, "getSystemClassLoader", "()Ljava/lang/ClassLoader;", NULL, runtime);
    } else if (utf8_equals_c(clazz->name, STR_CLASS_JAVA_LANG_REF_REFERENCE)) {
        jvm_runtime_cache->reference = clazz;
        jvm_runtime_cache->reference_target = find_fieldInfo_by_name_c(STR_CLASS_JAVA_LANG_REF_REFERENCE, "target", STR_INS_JAVA_LANG_OBJECT, NULL, runtime);
        jvm_runtime_cache->reference_target->is_weak_target = 1;//mark as weakreference.target field
        jvm_runtime_cache->reference_vmEnqueneReference = find_methodInfo_by_name_c(STR_CLASS_JAVA_LANG_REF_REFERENCE, "vmEnqueneReference", "(Ljava/lang/ref/Reference;)V", NULL, runtime);
    } else if (utf8_equals_c(clazz->name, STR_CLASS_JAVA_LANG_CLASSLOADER)) {
        jvm_runtime_cache->classloader_holdClass = find_methodInfo_by_name_c(STR_CLASS_JAVA_LANG_CLASSLOADER, "holdClass", "(Ljava/lang/Class;)V", NULL, runtime);
    }
//    jvm_printf("prepared: %s\n", utf8_cstr(clazz->name));

    clazz->status = CLASS_STATUS_PREPARED;
    return 0;
}

/**
 * 执行静态代码，需要在类装入字节码，并初始化好静态变量区之后执行
 * @param clazz class
 * @param runtime  runtime
 */
void class_clinit(JClass *clazz, Runtime *runtime) {
    vm_share_lock(runtime->jvm);
    runtime->thrd_info->no_pause++;
    if (clazz->status < CLASS_STATUS_PREPARED) {
        class_prepar(clazz->jloader, clazz, runtime);
    }
    if (clazz->status < CLASS_STATUS_CLINITING) {
        clazz->status = CLASS_STATUS_CLINITING;

        s32 i, len;

        /**
         * 把一些索引引用，转为内存对象引用，以此加快字节码执行速度
         * 把ConstantMethodRef.index 指向具体的 MethodInfo ，可能在本类，可能在父类
         * 把ConstantFieldRef.index 指向具体的 FieldInfo 内存
         * @param clazz
         */
        for (i = 0; i < clazz->constantPool.methodRef->length; i++) {
            ConstantMethodRef *cmr = (ConstantMethodRef *) arraylist_get_value(clazz->constantPool.methodRef, i);
            cmr->methodInfo = find_methodInfo_by_methodref(clazz, cmr->item.index, runtime);
            cmr->virtual_methods = pairlist_create(0);
            //jvm_printf("%s.%s %llx\n", utf8_cstr(clazz->name), utf8_cstr(cmr->name), (s64) (intptr_t) cmr->virtual_methods);
        }

        for (i = 0; i < clazz->constantPool.interfaceMethodRef->length; i++) {
            ConstantMethodRef *cmr = (ConstantMethodRef *) arraylist_get_value(clazz->constantPool.interfaceMethodRef, i);
            cmr->methodInfo = find_methodInfo_by_methodref(clazz, cmr->item.index, runtime);
            cmr->virtual_methods = pairlist_create(0);
        }

        for (i = 0; i < clazz->constantPool.fieldRef->length; i++) {
            ConstantFieldRef *cfr = (ConstantFieldRef *) arraylist_get_value(clazz->constantPool.fieldRef, i);
            FieldInfo *fi = find_fieldInfo_by_fieldref(clazz, cfr->item.index, runtime);
            cfr->fieldInfo = fi;
            if (!fi) {
                jvm_printf("field not found %s.%s \n", utf8_cstr(class_get_constant_classref(clazz, cfr->classIndex)->name), utf8_cstr(class_get_constant_utf8(clazz, class_get_constant_name_and_type(clazz, cfr->nameAndTypeIndex)->nameIndex)->utfstr));
                fi = find_fieldInfo_by_fieldref(clazz, cfr->item.index, runtime);
            }
            if (fi->_this_class->status < CLASS_STATUS_CLINITED) {
                //class_clinit(fi->_this_class, runtime); //init here would too early
            }
        }
        //find finalize method, but not process java.lang.Object.finalize()
        clazz->finalizeMethod = find_methodInfo_by_name_c(utf8_cstr(clazz->name), STR_METHOD_FINALIZE, "()V", clazz->jloader, runtime);
        if (clazz->finalizeMethod && utf8_equals_c(clazz->finalizeMethod->_this_class->name, STR_CLASS_JAVA_LANG_OBJECT)) {
            clazz->finalizeMethod = NULL;
        } else {
            int debug = 1;
        }

        // init javastring
        ArrayList *strlist = clazz->constantPool.stringRef;
        for (i = 0, len = strlist->length; i < len; i++) {
            ConstantStringRef *strRef = arraylist_get_value_unsafe(strlist, i);
            ConstantUTF8 *cutf = class_get_constant_utf8(clazz, strRef->stringIndex);
            Instance *jstr = hashtable_get(runtime->jvm->table_jstring_const, cutf->utfstr);
            if (!jstr) {
                jstr = jstring_create(cutf->utfstr, runtime);
                Utf8String *ustr = utf8_create_copy(cutf->utfstr);
                hashtable_put(runtime->jvm->table_jstring_const, ustr, jstr);
                gc_obj_hold(runtime->jvm->collector, jstr);
            }
            cutf->jstr = jstr;
        }

        for (i = 0; i < clazz->fieldPool.field_used; i++) {

            FieldInfo *fi = &clazz->fieldPool.field[i];
            if (fi->const_value_item && (fi->access_flags & ACC_STATIC)) { //except no static field of class: final int a=3;
                c8 *ptr = getStaticFieldPtr(fi);
                // check variable type to determain long/s32/f64/f32
                s32 datatype = fi->datatype_idx;
                //非引用类型
                switch (datatype) {
                    case DATATYPE_BOOLEAN:
                    case DATATYPE_BYTE: {
                        setFieldByte(ptr, (s8) ((ConstantInteger *) fi->const_value_item)->value);
                        break;
                    }
                    case DATATYPE_SHORT:
                    case DATATYPE_JCHAR: {
                        setFieldShort(ptr, (s16) ((ConstantInteger *) fi->const_value_item)->value);
                        break;
                    }
                    case DATATYPE_INT: {
                        setFieldInt(ptr, ((ConstantInteger *) fi->const_value_item)->value);
                        break;
                    }
                    case DATATYPE_FLOAT: {
                        setFieldFloat(ptr, ((ConstantFloat *) fi->const_value_item)->value);
                        break;
                    }
                    case DATATYPE_LONG: {
                        setFieldLong(ptr, ((ConstantLong *) fi->const_value_item)->value);
                        break;
                    }
                    case DATATYPE_DOUBLE: {
                        setFieldDouble(ptr, ((ConstantDouble *) fi->const_value_item)->value);
                        break;
                    }
                    default: {
                        if (utf8_equals_c(fi->descriptor, STR_INS_JAVA_LANG_STRING)) {//垃圾回收标识
                            setFieldRefer(ptr, class_get_constant_utf8(fi->_this_class, ((ConstantStringRef *) fi->const_value_item)->stringIndex)->jstr);
                        } else {
                        }
                    }

                }
            }
        }

        //优先初始化基类
        JClass *superclass = getSuperClass(clazz);
        if (superclass && superclass->status < CLASS_STATUS_CLINITED) {
            class_clinit(superclass, runtime);
        }

        MethodPool *p = &(clazz->methodPool);
        for (i = 0; i < p->method_used; i++) {
            //jvm_printf("%s,%s\n", utf8_cstr(p->methodRef[i].name), utf8_cstr(p->methodRef[i].descriptor));
            MethodInfo *mi = &(p->method[i]);
            if (utf8_equals_c(mi->name, STR_METHOD_CLINIT)) {

                s32 ret = execute_method_impl(mi, runtime);
                if (ret != RUNTIME_STATUS_NORMAL) {
                    print_exception(runtime);
                }
                //jvm_printf("clinit %s.%s\n", utf8_cstr(clazz->name), utf8_cstr(mi->name));
                break;
            }
        }
#if _JVM_DEBUG_LOG_LEVEL > 2
        jvm_printf("hold class %s\n", utf8_cstr(clazz->name));
#endif
        clazz->status = CLASS_STATUS_CLINITED;
    }
    runtime->thrd_info->no_pause--;
    vm_share_unlock(runtime->jvm);
}
//===============================    实例化相关  ==================================

u8 instance_of(Instance *ins, JClass *other) {
    JClass *clazz = ins->mb.clazz;
    s32 i, imax;
    if (clazz) {
        for (i = 0, imax = clazz->supers->length; i < imax; i++) {
            JClass *cl = clazz->supers->data[i];
            if (other == cl) {
                return 1;
            }
        }
    }
    return 0;
}

u8 isSonOfInterface(JClass *clazz, JClass *son, Runtime *runtime) {
    s32 i;
    for (i = 0; i < son->interfacePool.clasz_used; i++) {
        ConstantClassRef *ccr = (son->interfacePool.clasz + i);
        JClass *other = classes_load_get(son->jloader, class_get_constant_utf8(son, ccr->stringIndex)->utfstr, runtime);
        if (clazz == other) {
            return 1;
        } else {
            u8 sure = isSonOfInterface(clazz, other, runtime);
            if (sure)return 1;
        }
    }
    return 0;
}

u8 assignable_from(JClass *clazzSon, JClass *clazzSuper) {

    while (clazzSuper) {
        if (clazzSon == clazzSuper) {
            return 1;
        }
        clazzSuper = getSuperClass(clazzSuper);
    }
    return 0;
}

JClass *getSuperClass(JClass *clazz) {
    return clazz->superclass;
}

//===============================    类数据访问  ==================================


JClass *getClassByConstantClassRef(JClass *clazz, s32 index, Runtime *runtime) {
    ConstantClassRef *ccf = class_get_constant_classref(clazz, index);
    if (!ccf->clazz) {
        Utf8String *clsName = class_get_utf8_string(clazz, ccf->stringIndex);
        ccf->clazz = classes_load_get(clazz->jloader, clsName, runtime);
    }
    return ccf->clazz;
}


/**
 * 取得类成员信息，成员信息存在以下几种情况
 * ConstantFieldRef中：
 * 父类的静态和实例成员 Fathar.x ，都会描述为  Son.x ,类名描述为本类
 * 而调用其他类（非父类）的静态变量比如：  System.out ，会被描述为 System.out ，类名描述为其他类
 *
 * @param clazz class
 * @param field_ref ref
 * @return fi
 */
FieldInfo *find_fieldInfo_by_fieldref(JClass *clazz, s32 field_ref, Runtime *runtime) {
    ConstantFieldRef *cfr = class_get_constant_fieldref(clazz, field_ref);
    ConstantNameAndType *nat = class_get_constant_name_and_type(clazz, cfr->nameAndTypeIndex);
    Utf8String *clsName = class_get_utf8_string(clazz, class_get_constant_classref(clazz, cfr->classIndex)->stringIndex);
    Utf8String *fieldName = class_get_utf8_string(clazz, nat->nameIndex);
    Utf8String *type = class_get_utf8_string(clazz, nat->typeIndex);
    return find_fieldInfo_by_name(clsName, fieldName, type, clazz->jloader, runtime);
}

FieldInfo *find_fieldInfo_by_name_c(c8 *pclsName, c8 *pfieldName, c8 *pfieldType, Instance *jloader, Runtime *runtime) {
    Utf8String *clsName = utf8_create_c(pclsName);
    Utf8String *fieldName = utf8_create_c(pfieldName);
    Utf8String *fieldType = utf8_create_c(pfieldType);
    FieldInfo *fi = find_fieldInfo_by_name(clsName, fieldName, fieldType, jloader, runtime);
    utf8_destory(clsName);
    utf8_destory(fieldName);
    utf8_destory(fieldType);
    return fi;
}

FieldInfo *find_fieldInfo_by_name(Utf8String *clsName, Utf8String *fieldName, Utf8String *fieldType, Instance *jloader, Runtime *runtime) {
    FieldInfo *fi = NULL;
    JClass *other = classes_load_get_without_resolve(jloader, clsName, runtime);
//    if (utf8_equals_c(clsName, "espresso/parser/JavaParser")&&utf8_equals_c(fieldName, "methodNode_d")) {
//        int debug = 1;
//    }
    if (!other) {
        jvm_printf("field not exist :%s.%s%s\n", utf8_cstr(clsName), utf8_cstr(fieldName), utf8_cstr(fieldType));
        return NULL;
    }

    while (fi == NULL && other) {
        FieldPool *fp = &(other->fieldPool);
        s32 i = 0;
        for (; i < fp->field_used; i++) {
            FieldInfo *tmp = &fp->field[i];
            if (utf8_equals(fieldName, tmp->name) == 1
                && utf8_equals(fieldType, tmp->descriptor) == 1
                    ) {
                fi = tmp;
                break;
            }
        }
        //find interface default field
        if (fi == NULL) {
            for (i = 0; i < other->interfacePool.clasz_used; i++) {
                ConstantClassRef *ccr = (other->interfacePool.clasz + i);
                Utf8String *icl_name = class_get_constant_utf8(other, ccr->stringIndex)->utfstr;
//                if (utf8_equals_c(icl_name, "java/util/List")&&utf8_equals_c(methodName, "size")) {
//                    int debug = 1;
//                }
                FieldInfo *ifi = find_fieldInfo_by_name(icl_name, fieldName, fieldType, jloader, runtime);
                if (ifi != NULL) {
                    fi = ifi;
                    break;
                }
            }
        }
        other = getSuperClass(other);
    }

    return fi;
}


/**
 * 查找实例的方法， invokevirtual
 * @param ins ins
 * @param methodName name
 * @param methodType type
 * @return mi
 */
MethodInfo *find_instance_methodInfo_by_name(Instance *ins, Utf8String *methodName, Utf8String *methodType, Runtime *runtime) {
    if (!ins)return NULL;
    return find_methodInfo_by_name(ins->mb.clazz->name, methodName, methodType, ins->mb.clazz->jloader, runtime);
}

MethodInfo *find_methodInfo_by_methodref(JClass *clazz, s32 method_ref, Runtime *runtime) {
    ConstantMethodRef *cmr = class_get_constant_method_ref(clazz, method_ref);
    Utf8String *clsName = cmr->clsName;
    Utf8String *methodName = cmr->name;
    Utf8String *methodType = cmr->descriptor;
    return find_methodInfo_by_name(clsName, methodName, methodType, clazz->jloader, runtime);
}

MethodInfo *find_methodInfo_by_name_c(c8 *pclsName, c8 *pmethodName, c8 *pmethodType, Instance *jloader, Runtime *runtime) {
    Utf8String *clsName = utf8_create_c(pclsName);
    Utf8String *methodName = utf8_create_c(pmethodName);
    Utf8String *methodType = utf8_create_c(pmethodType);
    MethodInfo *mi = find_methodInfo_by_name(clsName, methodName, methodType, jloader, runtime);
    utf8_destory(clsName);
    utf8_destory(methodName);
    utf8_destory(methodType);
    return mi;
}

MethodInfo *find_methodInfo_by_name(Utf8String *clsName, Utf8String *methodName, Utf8String *methodType, Instance *jloader, Runtime *runtime) {
    MethodInfo *mi = NULL;
    JClass *other = classes_load_get_without_resolve(jloader, clsName, runtime);
    if (!other) {
        jvm_printf("method not exist :%s.%s%s\n", utf8_cstr(clsName), utf8_cstr(methodName), utf8_cstr(methodType));
        return NULL;
    }

    while (mi == NULL && other) {
        MethodPool *fp = &(other->methodPool);
        s32 i;
        for (i = 0; i < fp->method_used; i++) {
            MethodInfo *tmp = &fp->method[i];
            if (utf8_equals(methodName, tmp->name) == 1
                && utf8_equals(methodType, tmp->descriptor) == 1) {
                mi = tmp;
                if (!mi->_this_class) {
                    mi->_this_class = other;
                }
                break;
            }
        }
        //find interface default method implementation JDK8
        if (mi == NULL) {
            for (i = 0; i < other->interfacePool.clasz_used; i++) {
                ConstantClassRef *ccr = (other->interfacePool.clasz + i);
                Utf8String *icl_name = class_get_constant_utf8(other, ccr->stringIndex)->utfstr;
                classes_load_get_without_resolve(jloader, icl_name, runtime);
                MethodInfo *imi = find_methodInfo_by_name(icl_name, methodName, methodType, jloader, runtime);
                if (imi != NULL && imi->converted_code != NULL) {
                    mi = imi;
                    break;
                }
            }
        }
        //find superclass
        other = getSuperClass(other);
    }

    return mi;
}


/**
 * find all superclass and interfaces ,put they in supers,
 * @param clazz
 * @param runtime
 * @return
 */

static void find_supers_impl(JClass *clazz, Runtime *runtime, ArrayList *list) {
    JClass *other = clazz;
    s32 i;
    while (other) {
        if (arraylist_index_of(list, DEFAULT_ARRAYLIST_EQUALS_FUNC, other) < 0) {
            arraylist_push_back(list, other);
        }

        //find interface default method implementation JDK8
        for (i = 0; i < other->interfacePool.clasz_used; i++) {
            ConstantClassRef *ccr = (other->interfacePool.clasz + i);
            Utf8String *icl_name = class_get_constant_utf8(other, ccr->stringIndex)->utfstr;
            JClass *icl = classes_load_get_without_resolve(other->jloader, icl_name, runtime);
            find_supers_impl(icl, runtime, list);//find interface's interfaces
        }
        //find superclass
        other = getSuperClass(other);
    }
}

void find_supers(JClass *clazz, Runtime *runtime) {
    find_supers_impl(clazz, runtime, clazz->supers);
}

/* Get Major Version String */
c8 *getMajorVersionString(u16 major_number) {
    if (major_number == 0x33)
        return "J2SE 7";
    if (major_number == 0x32)
        return "J2SE 6.0";
    return "NONE";
}
