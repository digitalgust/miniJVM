//
// Created by gust on 2017/9/25.
//

#include "stdlib.h"
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include "../utils/bytebuf.h"
#include "../utils/miniz_wrapper.h"
#include "jvm.h"
#include "jvm_util.h"
#include "garbage.h"
#include "jit.h"

/* parse UTF-8 String */
void *_parseCPString(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantUTF8 *ptr = jvm_calloc(sizeof(ConstantUTF8));

    ptr->item.tag = CONSTANT_UTF8;
    ptr->item.index = index;

    //fread(short_tmp, 2, 1, fp);
    Short2Char s2c;
    s2c.c1 = (c8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (c8) bytebuf_read(buf);//short_tmp[1];
    ptr->string_size = s2c.s;

    ptr->utfstr = utf8_create();
    s32 i = 0;
    for (; i < ptr->string_size; i++) {
        u8 ch = (u8) bytebuf_read(buf);//0;
        //fread(&ch, 1, 1, fp);
        utf8_append_part_c(ptr->utfstr, &ch, 0, 1);
    }
    arraylist_push_back(_this->constantPool.utf8CP, ptr);
    return ptr;
}

/* parse Integer */
void *_parseCPInteger(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantInteger *ptr = jvm_calloc(sizeof(ConstantInteger));

    ptr->item.tag = CONSTANT_INTEGER;
    ptr->item.index = index;

    //fread(tmp, 4, 1, fp);
    Int2Float i2c;
    i2c.c3 = (u8) bytebuf_read(buf);//tmp[0];
    i2c.c2 = (u8) bytebuf_read(buf);//tmp[1];
    i2c.c1 = (u8) bytebuf_read(buf);//tmp[2];
    i2c.c0 = (u8) bytebuf_read(buf);//tmp[3];
    ptr->value = i2c.i;

    return ptr;
}

/* parse Float */
void *_parseCPFloat(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantFloat *ptr = jvm_calloc(sizeof(ConstantFloat));

    ptr->item.tag = CONSTANT_FLOAT;
    ptr->item.index = index;

    //fread(tmp, 4, 1, fp);
    Int2Float i2c;
    i2c.c3 = (u8) bytebuf_read(buf);//tmp[0];
    i2c.c2 = (u8) bytebuf_read(buf);//tmp[1];
    i2c.c1 = (u8) bytebuf_read(buf);//tmp[2];
    i2c.c0 = (u8) bytebuf_read(buf);//tmp[3];

    ptr->value = i2c.f;

    return ptr;
}

/* parse LONG */
void *_parseCPLong(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantLong *ptr = jvm_calloc(sizeof(ConstantLong));

    ptr->item.tag = CONSTANT_LONG;
    ptr->item.index = index;

    //fread(tmp, 8, 1, fp);
    Long2Double l2d;
    l2d.c7 = (u8) bytebuf_read(buf);//tmp[0];
    l2d.c6 = (u8) bytebuf_read(buf);//tmp[1];
    l2d.c5 = (u8) bytebuf_read(buf);//tmp[2];
    l2d.c4 = (u8) bytebuf_read(buf);//tmp[3];
    l2d.c3 = (u8) bytebuf_read(buf);//tmp[4];
    l2d.c2 = (u8) bytebuf_read(buf);//tmp[5];
    l2d.c1 = (u8) bytebuf_read(buf);//tmp[6];
    l2d.c0 = (u8) bytebuf_read(buf);//tmp[7];
    ptr->value = l2d.l;

    return ptr;
}

/* parse Double */
void *_parseCPDouble(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantDouble *ptr = jvm_calloc(sizeof(ConstantDouble));

    ptr->item.tag = CONSTANT_DOUBLE;
    ptr->item.index = index;

    //fread(tmp, 8, 1, fp);
    Long2Double l2d;
    l2d.c7 = (u8) bytebuf_read(buf);//tmp[0];
    l2d.c6 = (u8) bytebuf_read(buf);//tmp[1];
    l2d.c5 = (u8) bytebuf_read(buf);//tmp[2];
    l2d.c4 = (u8) bytebuf_read(buf);//tmp[3];
    l2d.c3 = (u8) bytebuf_read(buf);//tmp[4];
    l2d.c2 = (u8) bytebuf_read(buf);//tmp[5];
    l2d.c1 = (u8) bytebuf_read(buf);//tmp[6];
    l2d.c0 = (u8) bytebuf_read(buf);//tmp[7];
    ptr->value = l2d.d;

    return ptr;
}

/* parse Constant Pool Class */
void *_parseCPClass(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantClassRef *ptr = jvm_calloc(sizeof(ConstantClassRef));

    ptr->item.tag = CONSTANT_CLASS;
    ptr->item.index = index;

    //fread(short_tmp, 2, 1, fp);
    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->stringIndex = s2c.s;

    arraylist_push_back(_this->constantPool.classRef, ptr);
    return ptr;
}

/* parse Constant Pool String Ref */
void *_parseCPStringRef(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantStringRef *ptr = jvm_calloc(sizeof(ConstantStringRef));

    ptr->item.tag = CONSTANT_STRING_REF;
    ptr->item.index = index;

    //fread(short_tmp, 2, 1, fp);
    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->stringIndex = s2c.s;

    arraylist_push_back(_this->constantPool.stringRef, ptr);
    return ptr;
}

/* parse Constant Pool Field */
void *_parseCPField(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantFieldRef *ptr = jvm_calloc(sizeof(ConstantFieldRef));

    ptr->item.tag = CONSTANT_FIELD_REF;
    ptr->item.index = index;

    //fread(short_tmp, 2, 1, fp);
    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->classIndex = s2c.s;

    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->nameAndTypeIndex = s2c.s;

    arraylist_push_back(_this->constantPool.fieldRef, ptr);
    return ptr;
}

/* parse Constant Pool Method */
void *_parseCPMethod(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantMethodRef *ptr = jvm_calloc(sizeof(ConstantMethodRef));
    ptr->para_slots = -1;

    ptr->item.tag = CONSTANT_METHOD_REF;
    ptr->item.index = index;

    //fread(short_tmp, 2, 1, fp);
    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->classIndex = s2c.s;

    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->nameAndTypeIndex = s2c.s;

    arraylist_push_back(_this->constantPool.methodRef, ptr);
    return ptr;
}

void *_parseCPInterfaceMethod(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantInterfaceMethodRef *ptr = jvm_calloc(sizeof(ConstantInterfaceMethodRef));

    ptr->item.tag = CONSTANT_INTERFACE_METHOD_REF;
    ptr->item.index = index;

    //fread(short_tmp, 2, 1, fp);
    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->classIndex = s2c.s;

    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->nameAndTypeIndex = s2c.s;

    arraylist_push_back(_this->constantPool.interfaceMethodRef, ptr);
    return ptr;
}

void *_parseCPNameAndType(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantNameAndType *ptr = jvm_calloc(sizeof(ConstantNameAndType));

    ptr->item.tag = CONSTANT_NAME_AND_TYPE;
    ptr->item.index = index;

    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->nameIndex = s2c.s;

    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->typeIndex = s2c.s;

    return ptr;
}


void *_parseCPMethodType(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantMethodType *ptr = jvm_calloc(sizeof(ConstantNameAndType));

    ptr->item.tag = CONSTANT_METHOD_TYPE;
    ptr->item.index = index;

    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->descriptor_index = s2c.s;


    return ptr;
}


void *_parseCPMethodHandle(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantMethodHandle *ptr = jvm_calloc(sizeof(ConstantNameAndType));

    ptr->item.tag = CONSTANT_METHOD_HANDLE;
    ptr->item.index = index;

    ptr->reference_kind = (u8) bytebuf_read(buf);

    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->reference_index = s2c.s;

    return ptr;
}


void *_parseCPInvokeDynamic(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantInvokeDynamic *ptr = jvm_calloc(sizeof(ConstantNameAndType));

    ptr->item.tag = CONSTANT_INVOKE_DYNAMIC;
    ptr->item.index = index;

    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->bootstrap_method_attr_index = s2c.s;

    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->nameAndTypeIndex = s2c.s;

    return ptr;
}

s32 _class_constant_pool_destory(JClass *clazz) {
    int i;
    for (i = 0; i < clazz->constant_item_count; i++) {
        ConstantItem *cptr = clazz->constant_item_ptr[i];
        if (cptr) {
            switch (cptr->tag) {
                case CONSTANT_UTF8: {
                    ConstantUTF8 *ptr = (ConstantUTF8 *) cptr;
                    if (ptr->utfstr) {
                        utf8_destory(ptr->utfstr);
                        ptr->utfstr = NULL;
                    }
                    break;
                }
                case CONSTANT_METHOD_REF: {
                    ConstantMethodRef *ptr = (ConstantMethodRef *) cptr;
                    if (ptr->virtual_methods) {
                        pairlist_destory(ptr->virtual_methods);
                        ptr->virtual_methods = NULL;
                    }
                    break;
                }
            }
        }
        jvm_free(cptr);
    }

    return 0;
}

s32 _parseAttr(FieldInfo *ptr, ByteBuf *buf) {
    s32 i;
    AttributeInfo *tmp = 0;

#if 0
    jvm_printf("fieldRef attributes_count = %d\n", arr_body->attributes_count);
#endif
    for (i = 0; i < ptr->attributes_count; i++) {
        tmp = &(ptr->attributes[i]);
        //fread(short_tmp, 2, 1, fp);
        Short2Char s2c;
        s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
        s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
        tmp->attribute_name_index = s2c.s;

        //fread(integer_tmp, 4, 1, fp);
        Int2Float i2c;
        i2c.c3 = (u8) bytebuf_read(buf);//integer_tmp[0];
        i2c.c2 = (u8) bytebuf_read(buf);//integer_tmp[1];
        i2c.c1 = (u8) bytebuf_read(buf);//integer_tmp[2];
        i2c.c0 = (u8) bytebuf_read(buf);//integer_tmp[3];
        tmp->attribute_length = i2c.i;

        if (tmp->attribute_length > 0) {
            tmp->info = (u8 *) jvm_calloc(sizeof(u8) * tmp->attribute_length);
            //fread(tmp->info, tmp->attribute_length, 1, fp);
            bytebuf_read_batch(buf, (c8 *) tmp->info, tmp->attribute_length);
        } else {
            tmp->info = NULL;
        }
    }
    return 0;
}

/* parse Field Pool */
s32 _parseFP(JClass *_this, ByteBuf *buf) {

    FieldInfo *ptr = &(_this->fieldPool.field[_this->fieldPool.field_used]);

    /* access flag */
    //fread(short_tmp, 2, 1, fp);
    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->access_flags = s2c.s;

    /* name index */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->name_index = s2c.s;


    /* descriptor index */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->descriptor_index = s2c.s;

    /* attributes count */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->attributes_count = s2c.s;

    if (ptr->attributes_count > 0) {
        ptr->attributes = (AttributeInfo *) jvm_calloc(sizeof(AttributeInfo) * ptr->attributes_count);
    } else {
        ptr->attributes = NULL;
    }
    /* parse attributes */
    _parseAttr(ptr, buf);
    _this->fieldPool.field_used++;
    return 0;
}


s32 _parse_field_pool(JClass *_this, ByteBuf *buf, s32 count) {
    s32 size = sizeof(FieldInfo) * count;
    _this->fieldPool.field = jvm_calloc(size);

    s32 i;
    for (i = 0; i < count; i++)
        _parseFP(_this, buf);
    return 0;
}


s32 _class_field_info_destory(JClass *clazz) {
    s32 i, j;
    for (i = 0; i < clazz->fieldPool.field_used; i++) {
        FieldInfo *fi = &clazz->fieldPool.field[i];
        for (j = 0; j < fi->attributes_count; j++) {
            AttributeInfo *attr = &fi->attributes[j];
            jvm_free(attr->info);
            attr->info = NULL;
        }
        if (fi->attributes)jvm_free(fi->attributes);
        fi->attributes = NULL;
    }
    if (clazz->fieldPool.field)jvm_free(clazz->fieldPool.field);
    clazz->fieldPool.field = NULL;
    return 0;
}


/* parse Interface Pool Class */
s32 _parseIPClass(JClass *_this, ByteBuf *buf, s32 index) {

    ConstantClassRef *ptr = &_this->interfacePool.clasz[_this->interfacePool.clasz_used];

    ptr->item.tag = CONSTANT_CLASS;
    ptr->item.index = index;

    //fread(short_tmp, 2, 1, fp);
    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->stringIndex = s2c.s;

    _this->interfacePool.clasz_used++;
    return 0;
}


s32 _parse_interface_pool(JClass *_this, ByteBuf *buf, s32 count) {

    s32 size = sizeof(ConstantClassRef) * count;
    _this->interfacePool.clasz = jvm_calloc(size);
    s32 i;
    for (i = 0; i < count; i++)
        _parseIPClass(_this, buf, i);
    return 0;
}

s32 _class_interface_pool_destory(JClass *clazz) {
    if (clazz->interfacePool.clasz)jvm_free(clazz->interfacePool.clasz);
    clazz->interfacePool.clasz = NULL;
    return 0;
}


//=================================     load         ======================================


s32 _parseMethodAttr(MethodInfo *ptr, ByteBuf *buf) {
    s32 i;
    AttributeInfo *tmp = 0;

    for (i = 0; i < ptr->attributes_count; i++) {
        tmp = &ptr->attributes[i];
        //fread(short_tmp, 2, 1, fp);
        Short2Char s2c;
        s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
        s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
        tmp->attribute_name_index = s2c.s;
        //fread(integer_tmp, 4, 1, fp);
        Int2Float i2c;
        i2c.c3 = (u8) bytebuf_read(buf);//integer_tmp[0];
        i2c.c2 = (u8) bytebuf_read(buf);//integer_tmp[1];
        i2c.c1 = (u8) bytebuf_read(buf);//integer_tmp[2];
        i2c.c0 = (u8) bytebuf_read(buf);//integer_tmp[3];
        tmp->attribute_length = i2c.i;

        tmp->info = (u8 *) jvm_calloc(sizeof(u8) * tmp->attribute_length);
        //fread(tmp->info, tmp->attribute_length, 1, fp);
        bytebuf_read_batch(buf, (c8 *) tmp->info, tmp->attribute_length);
    }
    return 0;
}


/* parse Method Pool */
s32 _parseMP(JClass *_this, ByteBuf *buf) {

    MethodInfo *ptr = &(_this->methodPool.method[_this->methodPool.method_used]);

    /* access flag */
    //fread(short_tmp, 2, 1, fp);
    Short2Char s2c;
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->access_flags = s2c.s;
    ptr->is_native = (s2c.s & ACC_NATIVE) != 0;
    ptr->is_sync = (s2c.s & ACC_SYNCHRONIZED) != 0;
    ptr->is_static = (s2c.s & ACC_STATIC) != 0;

    /* name index */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->name_index = s2c.s;

    /* descriptor index */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->descriptor_index = s2c.s;

    /* attributes count */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
    s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
    ptr->attributes_count = s2c.s;

    if (ptr->attributes_count) {
        ptr->attributes = (AttributeInfo *) jvm_calloc(sizeof(AttributeInfo) * ptr->attributes_count);
    }
    /* parse methodRef attributes */
    _parseMethodAttr(ptr, buf);

    _this->methodPool.method_used++;
    return 0;
}

s32 _parse_method_pool(JClass *_this, ByteBuf *buf, s32 count) {

    s32 size = sizeof(MethodInfo) * count;
    _this->methodPool.method = jvm_calloc(size);
    s32 i;
    for (i = 0; i < count; i++)
        _parseMP(_this, buf);
    return 0;
}


s32 _class_method_info_destory(JClass *clazz) {
    s32 i, j;
    for (i = 0; i < clazz->methodPool.method_used; i++) {
        MethodInfo *mi = &clazz->methodPool.method[i];
        for (j = 0; j < mi->attributes_count; j++) {
            AttributeInfo *attr = &mi->attributes[j];
            if (attr->info)jvm_free(attr->info);//某些没有转
            attr->info = NULL;

        }
        if (mi->converted_code) {
            CodeAttribute *ca = (CodeAttribute *) mi->converted_code;
            jvm_free(ca->code);//info已被转换为converted_attribute
            ca->code = NULL;
            jvm_free(ca->bytecode_for_jit);//
            ca->bytecode_for_jit = NULL;
            jvm_free(ca->exception_table);//info已被转换为converted_attribute
            ca->exception_table = NULL;
            jvm_free(ca->line_number_table);
            ca->line_number_table = NULL;
            if (ca->local_var_table)jvm_free(ca->local_var_table);
            ca->local_var_table = NULL;
            jit_destory(&ca->jit);
            //
            jvm_free(mi->converted_code);
            mi->converted_code = NULL;
        }
        if (mi->attributes)jvm_free(mi->attributes);
        mi->attributes = NULL;
        if (mi->jump_2_pos)pairlist_destory(mi->jump_2_pos);
        mi->jump_2_pos = NULL;
        if (mi->pos_2_label)pairlist_destory(mi->pos_2_label);
        mi->pos_2_label = NULL;
        utf8_destory(mi->paraType);
        utf8_destory(mi->returnType);
        if (mi->breakpoint)jvm_free(mi->breakpoint);
        mi->breakpoint = NULL;
    }
    if (clazz->methodPool.method)jvm_free(clazz->methodPool.method);
    clazz->methodPool.method = NULL;
    return 0;
}

s32 _parse_attribute_pool(JClass *_this, ByteBuf *buf, s32 count) {

    s32 size = sizeof(AttributeInfo) * count;
    _this->attributePool.attribute = jvm_calloc(size);
    _this->attributePool.attribute_used = count;
    s32 i;

    for (i = 0; i < count; i++) {
        AttributeInfo *ptr = &(_this->attributePool.attribute[i]);

        /* access flag */
        //fread(short_tmp, 2, 1, fp);
        Short2Char s2c;
        s2c.c1 = (u8) bytebuf_read(buf);//short_tmp[0];
        s2c.c0 = (u8) bytebuf_read(buf);//short_tmp[1];
        ptr->attribute_name_index = s2c.s;

        //fread(integer_tmp, 4, 1, fp);
        Int2Float i2c;
        i2c.c3 = (u8) bytebuf_read(buf);//integer_tmp[0];
        i2c.c2 = (u8) bytebuf_read(buf);//integer_tmp[1];
        i2c.c1 = (u8) bytebuf_read(buf);//integer_tmp[2];
        i2c.c0 = (u8) bytebuf_read(buf);//integer_tmp[3];
        ptr->attribute_length = i2c.i;

        //
        ptr->info = jvm_calloc(ptr->attribute_length);
        //fread(ptr->info, ptr->attribute_length, 1, fp);
        bytebuf_read_batch(buf, (c8 *) ptr->info, ptr->attribute_length);
    }
    return 0;
}

s32 _class_attribute_info_destory(JClass *clazz) {
    s32 i;
    for (i = 0; i < clazz->attributePool.attribute_used; i++) {
        AttributeInfo *ptr = &(clazz->attributePool.attribute[i]);
        if (ptr->info) {
            jvm_free(ptr->info);
            ptr->info = NULL;
        }
    }
    if (clazz->attributePool.attribute)jvm_free(clazz->attributePool.attribute);
    return 0;
}

s32 _parse_constant_pool(JClass *_this, ByteBuf *buf, s32 count) {
    u8 tag = 0;
    s32 i = 0;
    u64 offset_start = 0;
    u64 offset_end = 0;
    _this->constant_item_ptr = jvm_calloc(count * sizeof(void *));
    _this->constant_item_count = count;
    for (i = 1; i < count; i++) {
        //fread(&tag, 1, 1, fp);
        tag = (u8) bytebuf_read(buf);
        offset_start = buf->rp;
        //jvm_printf("!!!read tag = %02x!!!\n", tag);
        __refer ptr = NULL;
        s32 idx = i;
        switch (tag) {
            case CONSTANT_UTF8:
                ptr = _parseCPString(_this, buf, i);
                break;
            case CONSTANT_INTEGER:
                ptr = _parseCPInteger(_this, buf, i);
                break;
            case CONSTANT_FLOAT:
                ptr = _parseCPFloat(_this, buf, i);
                break;
            case CONSTANT_LONG:
                ptr = _parseCPLong(_this, buf, i);
                i++;
                break;
            case CONSTANT_DOUBLE:
                ptr = _parseCPDouble(_this, buf, i);
                i++;
                break;
            case CONSTANT_STRING_REF:
                ptr = _parseCPStringRef(_this, buf, i);
                break;
            case CONSTANT_CLASS:
                ptr = _parseCPClass(_this, buf, i);
                break;
            case CONSTANT_FIELD_REF:
                ptr = _parseCPField(_this, buf, i);
                break;
            case CONSTANT_METHOD_REF:
                ptr = _parseCPMethod(_this, buf, i);
                break;
            case CONSTANT_INTERFACE_METHOD_REF:
                ptr = _parseCPMethod(_this, buf, i);//parseCPInterface(_this, fp, i);
                break;
            case CONSTANT_NAME_AND_TYPE:
                ptr = _parseCPNameAndType(_this, buf, i);
                break;
            case CONSTANT_METHOD_TYPE:
                ptr = _parseCPMethodType(_this, buf, i);
                break;
            case CONSTANT_METHOD_HANDLE:
                ptr = _parseCPMethodHandle(_this, buf, i);
                break;
            case CONSTANT_INVOKE_DYNAMIC:
                ptr = _parseCPInvokeDynamic(_this, buf, i);
                break;
            default:
                jvm_printf("\n!!!unknow constant item tag = %02x!!!\n\n", tag);
                //fseek(fp, -1, SEEK_CUR);
                break;
        };
        offset_end = buf->rp;
        _this->constant_item_ptr[idx] = ptr;
    }

    return 0;
}

/**
 *  change bytes order in  method bytecode to cpu bytes order
 * @param method
 */
void _changeBytesOrder(MethodInfo *method) {

    method->pos_2_label = pairlist_create(4);
    method->jump_2_pos = pairlist_create(4);
    CodeAttribute *ca = method->converted_code;
    spin_init(&ca->compile_lock, 0);

    u8 *ip = ca->code;
    u8 *end = ca->code_length + ip;
    //jvm_printf("adapte method %s.%s()\n", method->_this_class->name->data, method->name->data);
    while (ip < end) {
        u8 cur_inst = *ip;
        s32 code_idx = (s32) (ip - ca->code);
        if (cur_inst < op_breakpoint) {
//            if (utf8_equals_c(method->name, "test_typecast"))
//                jvm_printf("%8d, %s\n", pc, inst_name[cur_inst]);
        } else {
            int debug = 1;
        }
        switch (cur_inst) {
            case op_nop:
            case op_aconst_null:
            case op_iconst_m1:
            case op_iconst_0:
            case op_iconst_1:
            case op_iconst_2:
            case op_iconst_3:
            case op_iconst_4:
            case op_iconst_5:
            case op_lconst_0:
            case op_lconst_1:
            case op_fconst_0:
            case op_fconst_1:
            case op_fconst_2:
            case op_dconst_0:
            case op_dconst_1: {
                ip++;
                break;
            }
            case op_bipush: {
                ip += 2;
                break;
            }
            case op_sipush: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];
                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;
                ip += 3;
                break;
            }


            case op_ldc: {

                ip += 2;
                break;
            }

            case op_ldc_w: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];
                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;
                ip += 3;
                break;
            }

            case op_ldc2_w: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];

                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;
                ip += 3;

                break;
            }


            case op_iload:
            case op_fload:
            case op_aload: {
                ip += 2;
                break;
            }
            case op_lload:
            case op_dload: {
                ip += 2;
                break;
            }

            case op_iload_0:
            case op_iload_1:
            case op_iload_2:
            case op_iload_3:
            case op_lload_0:
            case op_lload_1:
            case op_lload_2:
            case op_lload_3:
            case op_fload_0:
            case op_fload_1:
            case op_fload_2:
            case op_fload_3:
            case op_dload_0:
            case op_dload_1:
            case op_dload_2:
            case op_dload_3:
            case op_aload_0:
            case op_aload_1:
            case op_aload_2:
            case op_aload_3:
            case op_iaload:
            case op_faload:
            case op_laload:
            case op_daload:
            case op_aaload:
            case op_baload:
            case op_caload:
            case op_saload: {
                ip++;
                break;
            }
            case op_istore:
            case op_fstore:
            case op_astore:
            case op_lstore:
            case op_dstore: {
                ip += 2;
                break;
            }

            case op_istore_0:
            case op_istore_1:
            case op_istore_2:
            case op_istore_3:
            case op_lstore_0:
            case op_lstore_1:
            case op_lstore_2:
            case op_lstore_3:
            case op_fstore_0:
            case op_fstore_1:
            case op_fstore_2:
            case op_fstore_3:
            case op_dstore_0:
            case op_dstore_1:
            case op_dstore_2:
            case op_dstore_3:
            case op_astore_0:
            case op_astore_1:
            case op_astore_2:
            case op_astore_3:
            case op_fastore:
            case op_iastore:
            case op_dastore:
            case op_lastore:
            case op_aastore:
            case op_bastore:
            case op_castore:
            case op_sastore:
            case op_pop:
            case op_pop2:
            case op_dup:
            case op_dup_x1:
            case op_dup_x2:
            case op_dup2:
            case op_dup2_x1:
            case op_dup2_x2:
            case op_swap:
            case op_iadd:
            case op_ladd:
            case op_fadd:
            case op_dadd:
            case op_isub:
            case op_lsub:
            case op_fsub:
            case op_dsub:
            case op_imul:
            case op_lmul:
            case op_fmul:
            case op_dmul:
            case op_idiv:
            case op_ldiv:
            case op_fdiv:
            case op_ddiv:
            case op_irem:
            case op_lrem:
            case op_frem:
            case op_drem:
            case op_ineg:
            case op_lneg:
            case op_fneg:
            case op_dneg:
            case op_ishl:
            case op_lshl:
            case op_ishr:
            case op_lshr:
            case op_iushr:
            case op_lushr:
            case op_iand:
            case op_land:
            case op_ior:
            case op_lor:
            case op_ixor:
            case op_lxor: {
                ip++;
                break;
            }

            case op_iinc: {
                ip += 3;
                break;
            }

            case op_i2l:
            case op_i2f:
            case op_i2d:
            case op_l2i:
            case op_l2f:
            case op_l2d:
            case op_f2i:
            case op_f2l:
            case op_f2d:
            case op_d2i:
            case op_d2l:
            case op_d2f:
            case op_i2b:
            case op_i2c:
            case op_i2s:
            case op_lcmp:
            case op_fcmpl:
            case op_fcmpg:
            case op_dcmpl:
            case op_dcmpg: {
                ip++;
                break;
            }


            case op_ifeq:
            case op_ifne:
            case op_iflt:
            case op_ifge:
            case op_ifgt:
            case op_ifle:
            case op_if_icmpeq:
            case op_if_icmpne:
            case op_if_icmplt:
            case op_if_icmpge:
            case op_if_icmpgt:
            case op_if_icmple:
            case op_if_acmpeq:
            case op_if_acmpne:
            case op_goto:
            case op_jsr: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];
                u8 *addr = ip + 1;
                *((s16 *) addr) = s2c.s;
                s32 jumpto = code_idx + s2c.s;
                pairlist_putl(method->pos_2_label, jumpto, -1);// save label pos in list
                pairlist_putl(method->pos_2_label, code_idx + 3, -1);// save label pos in list
                ip += 3;
                break;
            }

            case op_ret: {
                ip += 2;
                break;
            }


            case op_tableswitch: {
                s32 pos = 0;
                pos = 4 - ((((u64) (intptr_t) ip) - (u64) (intptr_t) (ca->code)) % 4);//4 byte对齐

                u8 *addr = ip + pos;
                Int2Float i2c;
                i2c.c3 = ip[pos++];
                i2c.c2 = ip[pos++];
                i2c.c1 = ip[pos++];
                i2c.c0 = ip[pos++];
                s32 default_offset = i2c.i;
                pairlist_putl(method->pos_2_label, code_idx + i2c.i, -1);
                *((s32 *) addr) = i2c.i;
                addr += 4;
                i2c.c3 = ip[pos++];
                i2c.c2 = ip[pos++];
                i2c.c1 = ip[pos++];
                i2c.c0 = ip[pos++];
                s32 low = i2c.i;
                *((s32 *) addr) = i2c.i;
                addr += 4;
                i2c.c3 = ip[pos++];
                i2c.c2 = ip[pos++];
                i2c.c1 = ip[pos++];
                i2c.c0 = ip[pos++];
                s32 high = i2c.i;
                *((s32 *) addr) = i2c.i;
                addr += 4;
                //
                s32 i = low;
                for (; i <= high; i++) {

                    i2c.c3 = ip[pos++];
                    i2c.c2 = ip[pos++];
                    i2c.c1 = ip[pos++];
                    i2c.c0 = ip[pos++];
                    *((s32 *) addr) = i2c.i;
                    pairlist_putl(method->pos_2_label, code_idx + i2c.i, -1);// save label pos in list
                    addr += 4;
                }
                ip += pos;
                break;
            }

            case op_lookupswitch: {
                s32 pos = 0;
                pos = 4 - ((((u64) (intptr_t) ip) - (u64) (intptr_t) (ca->code)) % 4);//4 byte对齐

                u8 *addr = ip + pos;

                Int2Float i2c;
                i2c.c3 = ip[pos++];
                i2c.c2 = ip[pos++];
                i2c.c1 = ip[pos++];
                i2c.c0 = ip[pos++];
                s32 default_offset = i2c.i;
                pairlist_putl(method->pos_2_label, code_idx + i2c.i, -1);
                *((s32 *) addr) = i2c.i;
                i2c.c3 = ip[pos++];
                i2c.c2 = ip[pos++];
                i2c.c1 = ip[pos++];
                i2c.c0 = ip[pos++];
                s32 n = i2c.i;
                addr += 4;
                *((s32 *) addr) = i2c.i;
                s32 i, key;

                int offset = default_offset;
                for (i = 0; i < n; i++) {
                    i2c.c3 = ip[pos++];
                    i2c.c2 = ip[pos++];
                    i2c.c1 = ip[pos++];
                    i2c.c0 = ip[pos++];
                    key = i2c.i;
                    addr += 4;
                    *((s32 *) addr) = i2c.i;
                    i2c.c3 = ip[pos++];
                    i2c.c2 = ip[pos++];
                    i2c.c1 = ip[pos++];
                    i2c.c0 = ip[pos++];
                    offset = i2c.i;
                    pairlist_putl(method->pos_2_label, code_idx + i2c.i, -1);// save label pos in list
                    addr += 4;
                    *((s32 *) addr) = i2c.i;
                }
                ip += pos;
                break;
            }

            case op_lreturn:
            case op_dreturn:
            case op_ireturn:
            case op_freturn:
            case op_areturn:
            case op_return: {
                ip++;
                break;
            }

            case op_getstatic:
            case op_putstatic:
            case op_getfield:
            case op_putfield: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];

                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;
                ip += 3;
                break;
            }

            case op_invokevirtual: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];

                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;

                ip += 3;

                break;
            }


            case op_invokespecial: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];

                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;
                ip += 3;

                break;
            }


            case op_invokestatic: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];
                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;
                ip += 3;
                break;
            }


            case op_invokeinterface: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];

                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;

                ip += 5;
                break;
            }

            case op_invokedynamic: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];

                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;
                ip += 5;
                break;
            }


            case op_new: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];

                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;
                ip += 3;

                break;
            }


            case op_newarray: {
                ip += 2;
                break;
            }

            case op_anewarray: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];
                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;
                ip += 3;

                break;
            }

            case op_arraylength: {
                ip++;
                break;
            }


            case op_athrow: {
                ip++;
                break;
            }

            case op_checkcast: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];

                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;
                ip += 3;

                break;
            }


            case op_instanceof: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];

                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;

                ip += 3;

                break;
            }

            case op_monitorenter:
            case op_monitorexit: {
                ip++;

                break;
            }

            case op_wide: {
                ip++;

                cur_inst = *ip;
                switch (cur_inst) {
                    case op_iload:
                    case op_fload:
                    case op_aload:
                    case op_lload:
                    case op_dload:
                    case op_istore:
                    case op_fstore:
                    case op_astore:
                    case op_lstore:
                    case op_dstore:
                    case op_ret: {
                        Short2Char s2c;
                        s2c.c1 = ip[1];
                        s2c.c0 = ip[2];
                        u8 *addr = ip + 1;
                        *((u16 *) addr) = s2c.us;
                        ip += 3;
                        break;
                    }
                    case op_iinc    : {
                        Short2Char s2c1, s2c2;

                        s2c1.c1 = ip[1];
                        s2c1.c0 = ip[2];
                        s2c2.c1 = ip[3];
                        s2c2.c0 = ip[4];

                        u8 *addr = ip + 1;
                        *((u16 *) addr) = s2c1.us;
                        addr += 2;
                        *((u16 *) addr) = s2c2.us;
                        ip += 5;
                        break;
                    }
                    default:
                        jvm_printf("instruct wide %x not found\n", cur_inst);
                }
                break;
            }

            case op_multianewarray: {
                //data type index
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];

                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;
                ip += 4;
                break;
            }


            case op_ifnull:
            case op_ifnonnull: {
                Short2Char s2c;
                s2c.c1 = ip[1];
                s2c.c0 = ip[2];
                u8 *addr = ip + 1;
                *((u16 *) addr) = s2c.us;

                s32 jumpto = code_idx + s2c.s;
                pairlist_putl(method->pos_2_label, jumpto, -1);// save label pos in list

                ip += 3;
                break;
            }

            case op_breakpoint: {
                ip += 1;
                break;
            }


            case op_goto_w: {
                Int2Float i2f;
                i2f.c3 = ip[1];
                i2f.c2 = ip[2];
                i2f.c1 = ip[3];
                i2f.c0 = ip[4];

                u8 *addr = ip + 1;
                *((s32 *) addr) = i2f.i;

                s32 jumpto = code_idx + i2f.i;
                pairlist_putl(method->pos_2_label, jumpto, -1);// save label pos in list

                ip += 5;


                break;
            }

            case op_jsr_w: {
                Int2Float i2f;
                i2f.c3 = ip[1];
                i2f.c2 = ip[2];
                i2f.c1 = ip[3];
                i2f.c0 = ip[4];

                u8 *addr = ip + 1;
                *((s32 *) addr) = i2f.i;

                s32 jumpto = code_idx + i2f.i;
                pairlist_putl(method->pos_2_label, jumpto, -1);// save label pos in list
                pairlist_putl(method->pos_2_label, code_idx + 5, -1);// save label pos in list

                ip += 5;
                break;
            }
            default:
                jvm_printf("adapte instruct %x not found\n", cur_inst);
        }
    }

    memcpy(ca->bytecode_for_jit, ca->code, ca->code_length);

    u8 *mc = ca->code;
    if (ca->code_length == 5) {//optimize setter eg: void setSize(int size){this.size=size;}
        u8 mc4 = mc[4];
        if (mc[1] == op_getfield
            && mc[0] == op_aload_0
            && (mc4 >= op_ireturn && mc4 <= op_areturn)) {
            method->is_getter = 1;
            //jvm_printf(" getter %s.%s  %d \n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), method->_this_class->status);
        }
    } else if (ca->code_length == 6) {//optimize setter eg: void setSize(int size){this.size=size;}
        u8 mc1 = mc[1];
        if (mc[5] == op_return
            && mc[0] == op_aload_0
            && mc[2] == op_putfield
            && (mc1 == op_aload_1 || mc1 == op_dload_1 || mc1 == op_lload_1 || mc1 == op_iload_1 || mc1 == op_fload_1)) {
            method->is_setter = 1;
            //jvm_printf(" setter %s.%s  %d \n", utf8_cstr(method->_this_class->name), utf8_cstr(method->name), method->_this_class->status);
        }
    }
}

s32 _convert_to_code_attribute(CodeAttribute *ca, AttributeInfo *attr, JClass *clazz) {
    s32 info_p = 0;

    ca->attribute_name_index = attr->attribute_name_index;
    ca->attribute_length = attr->attribute_length;
    Short2Char s2c;
    s2c.c1 = attr->info[info_p++];
    s2c.c0 = attr->info[info_p++];
    ca->max_stack = s2c.s;
    s2c.c1 = attr->info[info_p++];
    s2c.c0 = attr->info[info_p++];
    ca->max_locals = s2c.s;
    Int2Float i2c;
    i2c.c3 = attr->info[info_p++];
    i2c.c2 = attr->info[info_p++];
    i2c.c1 = attr->info[info_p++];
    i2c.c0 = attr->info[info_p++];
    ca->code_length = i2c.i;
    ca->code = (u8 *) jvm_calloc(sizeof(u8) * ca->code_length);
    ca->bytecode_for_jit = (u8 *) jvm_calloc(sizeof(u8) * ca->code_length);
    memcpy(ca->code, attr->info + info_p, ca->code_length);
    info_p += ca->code_length;
    s2c.c1 = attr->info[info_p++];
    s2c.c0 = attr->info[info_p++];
    ca->exception_table_length = s2c.s;
    s32 bytelen = sizeof(ExceptionTable) * ca->exception_table_length;
    ca->exception_table = jvm_calloc(bytelen);
    int i;
    for (i = 0; i < 4 * ca->exception_table_length; i++) {
        s2c.c1 = attr->info[info_p++];
        s2c.c0 = attr->info[info_p++];
        ((u16 *) ca->exception_table)[i] = s2c.s;
    }
    //line number
    s2c.c1 = attr->info[info_p++];
    s2c.c0 = attr->info[info_p++];
    s32 attr_count = (u16) s2c.s;
    for (i = 0; i < attr_count; i++) {
        s2c.c1 = attr->info[info_p++];
        s2c.c0 = attr->info[info_p++];
        s32 attribute_name_index = (u16) s2c.s;
        i2c.c3 = attr->info[info_p++];
        i2c.c2 = attr->info[info_p++];
        i2c.c1 = attr->info[info_p++];
        i2c.c0 = attr->info[info_p++];
        s32 attribute_lenth = (u32) i2c.i;
        //转行号表
        if (utf8_equals_c(class_get_utf8_string(clazz, attribute_name_index), "LineNumberTable")) {
            s2c.c1 = attr->info[info_p++];
            s2c.c0 = attr->info[info_p++];
            ca->line_number_table_length = (u16) s2c.s;
            ca->line_number_table = jvm_calloc(sizeof(u32) * ca->line_number_table_length);
            s32 j;
            for (j = 0; j < ca->line_number_table_length; j++) {
                s2c.c1 = attr->info[info_p++];
                s2c.c0 = attr->info[info_p++];
                //setFieldShort(ca->line_number_table[j].start_pc, s2c.s);
                ca->line_number_table[j].start_pc = s2c.s;
                s2c.c1 = attr->info[info_p++];
                s2c.c0 = attr->info[info_p++];
                //setFieldShort(ca->line_number_table[j].line_number, s2c.s);
                ca->line_number_table[j].line_number = s2c.s;
            }
        } else if (utf8_equals_c(class_get_utf8_string(clazz, attribute_name_index), "LocalVariableTable")) {
            s2c.c1 = attr->info[info_p++];
            s2c.c0 = attr->info[info_p++];
            ca->local_var_table_length = (u16) s2c.s;
            ca->local_var_table = jvm_calloc(sizeof(LocalVarTable) * ca->local_var_table_length);
            s32 j;
            for (j = 0; j < ca->local_var_table_length; j++) {
                s2c.c1 = attr->info[info_p++];
                s2c.c0 = attr->info[info_p++];
                ca->local_var_table[j].start_pc = s2c.s;
                s2c.c1 = attr->info[info_p++];
                s2c.c0 = attr->info[info_p++];
                ca->local_var_table[j].length = s2c.s;
                s2c.c1 = attr->info[info_p++];
                s2c.c0 = attr->info[info_p++];
                ca->local_var_table[j].name_index = s2c.s;
                s2c.c1 = attr->info[info_p++];
                s2c.c0 = attr->info[info_p++];
                ca->local_var_table[j].descriptor_index = s2c.s;
                s2c.c1 = attr->info[info_p++];
                s2c.c0 = attr->info[info_p++];
                ca->local_var_table[j].index = s2c.s;
            }
        } else {
            info_p += attribute_lenth;
        }
    }
    return 0;
}

void _convert_2_bootstrap_methods(AttributeInfo *attr, JClass *clazz) {
    BootstrapMethodsAttr *bms = jvm_calloc(sizeof(BootstrapMethodsAttr));
    s32 ptr = 0;
    Short2Char s2c;
    s2c.c1 = attr->info[ptr++];
    s2c.c0 = attr->info[ptr++];
    bms->num_bootstrap_methods = s2c.s;
    bms->bootstrap_methods = jvm_calloc(sizeof(BootstrapMethod) * bms->num_bootstrap_methods);

    s32 i;
    for (i = 0; i < bms->num_bootstrap_methods; i++) {
        BootstrapMethod *bm = &bms->bootstrap_methods[i];
        s2c.c1 = attr->info[ptr++];
        s2c.c0 = attr->info[ptr++];
        bm->bootstrap_method_ref = s2c.s;

        s2c.c1 = attr->info[ptr++];
        s2c.c0 = attr->info[ptr++];
        bm->num_bootstrap_arguments = s2c.s;
        bm->bootstrap_arguments = jvm_calloc(sizeof(u16) * bm->num_bootstrap_arguments);
        s32 j;
        for (j = 0; j < bm->num_bootstrap_arguments; j++) {
            s2c.c1 = attr->info[ptr++];
            s2c.c0 = attr->info[ptr++];
            bm->bootstrap_arguments[j] = s2c.s;
        }

    }
    jvm_free(attr->info);
    attr->info = NULL;
    clazz->bootstrapMethodAttr = bms;
}

void _class_bootstrap_methods_destory(JClass *clazz) {
    BootstrapMethodsAttr *bms = clazz->bootstrapMethodAttr;
    if (!bms)return;

    s32 i;
    for (i = 0; i < bms->num_bootstrap_methods; i++) {
        BootstrapMethod *bm = &bms->bootstrap_methods[i];

        if (bm->bootstrap_arguments) {
            jvm_free(bm->bootstrap_arguments);
        }
    }
    jvm_free(bms->bootstrap_methods);
    jvm_free(bms);
    clazz->bootstrapMethodAttr = NULL;
}


s32 parseMethodPara(Utf8String *methodType, Utf8String *out) {
    s32 count = 0;
    Utf8String *para = utf8_create_copy(methodType);
    utf8_substring(para, utf8_indexof_c(para, "(") + 1, utf8_last_indexof_c(para, ")"));
    //从后往前拆分方法参数，从栈中弹出放入本地变量
    int i = 0;
    while (para->length > 0) {
        c8 ch = utf8_char_at(para, 0);
        switch (ch) {
            case 'S':
            case 'C':
            case 'B':
            case 'I':
            case 'F':
            case 'Z':
                utf8_substring(para, 1, para->length);
                utf8_append_c(out, "4");
                count++;
                break;
            case 'D':
            case 'J': {
                utf8_substring(para, 1, para->length);
                utf8_append_c(out, "8");
                count += 2;
                break;
            }
            case 'L':
                utf8_substring(para, utf8_indexof_c(para, ";") + 1, para->length);
                utf8_append_c(out, "R");
                count += 1;
                break;
            case '[':
                while (utf8_char_at(para, 1) == '[') {
                    utf8_substring(para, 1, para->length);//去掉多维中的 [[[[LObject; 中的 [符
                }
                if (utf8_char_at(para, 1) == 'L') {
                    utf8_substring(para, utf8_indexof_c(para, ";") + 1, para->length);
                } else {
                    utf8_substring(para, 2, para->length);
                }
                utf8_append_c(out, "R");
                count += 1;
                break;
        }
        i++;
    }
    utf8_destory(para);
    return count;
}


/**
 * 把各个索引转为直接地址引用，加快处理速度
 * @param clazz class
 */
void _class_optimize(JClass *clazz) {
    Utf8String *ustr = class_get_utf8_string(clazz,
                                             class_get_constant_classref(clazz, clazz->cff.this_class)->stringIndex);
    clazz->name = utf8_create_copy(ustr);
//    if (utf8_equals_c(clazz->name, "com/meslewis/simplegltf2/simpleviewer/SimpleViewer$1")) {
//        int debug = 1;
//    }
    s32 i;
    for (i = 0; i < clazz->interfacePool.clasz_used; i++) {
        ConstantClassRef *ptr = &clazz->interfacePool.clasz[i];
        ptr->name = class_get_utf8_string(clazz, class_get_constant_classref(clazz, ptr->stringIndex)->stringIndex);
    }

    for (i = 0; i < clazz->fieldPool.field_used; i++) {
        FieldInfo *fi = &clazz->fieldPool.field[i];
        fi->name = class_get_utf8_string(clazz, fi->name_index);
        fi->descriptor = class_get_utf8_string(clazz, fi->descriptor_index);
        fi->datatype_idx = getDataTypeIndex(utf8_char_at(fi->descriptor, 0));
        fi->isrefer = isDataReferByIndex(fi->datatype_idx);
        fi->datatype_bytes = DATA_TYPE_BYTES[fi->datatype_idx];
        fi->isvolatile = fi->access_flags & ACC_VOLATILE;

        //for gc iterator fast
        if (isDataReferByIndex(fi->datatype_idx)) {
            if (fi->access_flags & ACC_STATIC) {
                arraylist_push_back_unsafe(clazz->staticFieldPtrIndex, (__refer) (intptr_t) i);
            } else {
                arraylist_push_back_unsafe(clazz->insFieldPtrIndex, (__refer) (intptr_t) i);
            }
        }

        //for static final
        s32 j;
        for (j = 0; j < fi->attributes_count; j++) {
            AttributeInfo *att = &fi->attributes[j];
            Utf8String *attName = class_get_constant_utf8(clazz, att->attribute_name_index)->utfstr;
            if (utf8_equals_c(attName, "ConstantValue")) {
                Short2Char s2c;
                s2c.c1 = att->info[0];
                s2c.c0 = att->info[1];
                fi->const_value_item = class_get_constant_item(clazz, s2c.us);
            } else if (utf8_equals_c(attName, "Signature")) {
                Short2Char s2c;
                s2c.c1 = att->info[0];
                s2c.c0 = att->info[1];
                fi->signature = class_get_utf8_string(clazz, s2c.us);
            }
        }

    }
    for (i = 0; i < clazz->methodPool.method_used; i++) {
        MethodInfo *ptr = &clazz->methodPool.method[i];
        ptr->name = class_get_utf8_string(clazz, ptr->name_index);
        ptr->descriptor = class_get_utf8_string(clazz, ptr->descriptor_index);
        ptr->_this_class = clazz;
        if (!ptr->paraType) {//首次执行
            // eg:  (Ljava/lang/Object;IBLjava/lang/String;[[[ILjava/lang/Object;)Ljava/lang/String;Z
            ptr->paraType = utf8_create();
            //parse method description return slots
            ptr->para_slots = parseMethodPara(ptr->descriptor, ptr->paraType);
            ptr->para_count_with_this = ptr->paraType->length;
            if (!(ptr->is_static)) {
                ptr->para_slots++;//add this pointer
                ptr->para_count_with_this++;
            }
            s32 pos = utf8_indexof_c(ptr->descriptor, ")") + 1;
            ptr->returnType = utf8_create_part(ptr->descriptor, pos, ptr->descriptor->length - pos);
            c8 ch = utf8_char_at(ptr->returnType, 0);
            if (ch == 'J' || ch == 'D') {
                ptr->return_slots = 2;
            } else if (ch == 'V') {
                ptr->return_slots = 0;
            } else {
                ptr->return_slots = 1;
            }
        }
        s32 j;

        //转attribute为CdoeAttribute
        for (j = 0; j < ptr->attributes_count; j++) {
            Utf8String *attname = class_get_utf8_string(clazz, ptr->attributes[j].attribute_name_index);
            if (utf8_equals_c(attname, "Code") == 1) {
//                if (utf8_equals_c(clazz->name, "espresso/syntaxtree/ExpressionNode") && utf8_equals_c(ptr->name, "evaluateExp")) {
//                    int debug = 1;
//                }

                CodeAttribute *ca = jvm_calloc(sizeof(CodeAttribute));
                _convert_to_code_attribute(ca, &ptr->attributes[j], clazz);
                jvm_free(ptr->attributes[j].info);//无用删除
                ptr->attributes[j].info = NULL;
                ptr->converted_code = ca;
                _changeBytesOrder(ptr);
                jit_init(ca);
            } else if (utf8_equals_c(attname, "Signature") == 1) {
                Short2Char s2c;
                s2c.c1 = ptr->attributes[j].info[0];
                s2c.c0 = ptr->attributes[j].info[1];
                ptr->signature = class_get_utf8_string(clazz, s2c.us);
            }
        }
    }
    for (i = 0; i < clazz->attributePool.attribute_used; i++) {
        AttributeInfo *ptr = &clazz->attributePool.attribute[i];
        Utf8String *name = class_get_utf8_string(clazz, ptr->attribute_name_index);
        if (utf8_equals_c(name, "SourceFile")) {
            Short2Char s2c;
            s2c.c1 = ptr->info[0];
            s2c.c0 = ptr->info[1];
            clazz->source = class_get_utf8_string(clazz, s2c.us);
        } else if (utf8_equals_c(name, "BootstrapMethods")) {
            _convert_2_bootstrap_methods(ptr, clazz);

        } else if (utf8_equals_c(name, "Signature")) {
            Short2Char s2c;
            s2c.c1 = ptr->info[0];
            s2c.c0 = ptr->info[1];
            clazz->signature = class_get_utf8_string(clazz, s2c.us);
        }
    }

    for (i = 0; i < clazz->constantPool.classRef->length; i++) {
        ConstantClassRef *ccr = (ConstantClassRef *) arraylist_get_value(clazz->constantPool.classRef, i);
        ccr->name = class_get_utf8_string(clazz, ccr->stringIndex);
    }

    for (i = 0; i < clazz->constantPool.fieldRef->length; i++) {
        ConstantFieldRef *cfr = (ConstantFieldRef *) arraylist_get_value(clazz->constantPool.fieldRef, i);
        cfr->nameAndType = class_get_constant_name_and_type(clazz, cfr->nameAndTypeIndex);
        cfr->name = class_get_utf8_string(clazz, cfr->nameAndType->nameIndex);
        cfr->descriptor = class_get_utf8_string(clazz, cfr->nameAndType->typeIndex);
        cfr->clsName = class_get_constant_classref(clazz, cfr->classIndex)->name;
    }
    for (i = 0; i < clazz->constantPool.methodRef->length; i++) {
        ConstantMethodRef *cmr = (ConstantMethodRef *) arraylist_get_value(clazz->constantPool.methodRef, i);
        cmr->nameAndType = class_get_constant_name_and_type(clazz, cmr->nameAndTypeIndex);
        cmr->name = class_get_utf8_string(clazz, cmr->nameAndType->nameIndex);
        cmr->descriptor = class_get_utf8_string(clazz, cmr->nameAndType->typeIndex);
        cmr->clsName = class_get_constant_classref(clazz, cmr->classIndex)->name;
//        if (utf8_equals_c(clazz->name, "java/lang/String")) {
//            printf("%s,%s\n", utf8_cstr(cmr->name), utf8_cstr(cmr->clsName));
//            int debug = 1;
//        }
        if (cmr->para_slots == -1) {
            Utf8String *tmps = utf8_create();
            cmr->para_slots = parseMethodPara(cmr->descriptor, tmps);
            utf8_destory(tmps);
        }
    }
    for (i = 0; i < clazz->constantPool.interfaceMethodRef->length; i++) {
        ConstantMethodRef *cmr = (ConstantMethodRef *) arraylist_get_value(clazz->constantPool.methodRef, i);
        cmr->nameAndType = class_get_constant_name_and_type(clazz, cmr->nameAndTypeIndex);
        cmr->name = class_get_utf8_string(clazz, cmr->nameAndType->nameIndex);
        cmr->descriptor = class_get_utf8_string(clazz, cmr->nameAndType->typeIndex);
        cmr->clsName = class_get_constant_classref(clazz, cmr->classIndex)->name;
        if (cmr->para_slots == -1) {
            Utf8String *tmps = utf8_create();
            cmr->para_slots = parseMethodPara(cmr->descriptor, tmps);
            utf8_destory(tmps);
        }
    }

}


/* Parse Class File */
s32 _LOAD_CLASS_FROM_BYTES(JClass *_this, ByteBuf *buf) {
    ClassFileFormat *cff = &(_this->cff);

    /* magic number */
    bytebuf_read_batch(buf, (c8 *) &cff->magic_number, 4);
//    fread(cff->magic_number, 4, 1, fp);

    /* minor_version */
    //fread(short_tmp, 2, 1, fp);
    Short2Char s2c;
    s2c.c1 = (c8) bytebuf_read(buf);
    s2c.c0 = (c8) bytebuf_read(buf);
    cff->minor_version = s2c.s;

    /* major_version */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (c8) bytebuf_read(buf);
    s2c.c0 = (c8) bytebuf_read(buf);
    cff->major_version = s2c.s;

    /* constant pool */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (c8) bytebuf_read(buf);
    s2c.c0 = (c8) bytebuf_read(buf);
    cff->constant_pool_count = s2c.s;
    /* constant pool table */
    _parse_constant_pool(_this, buf, cff->constant_pool_count);
    /* access flag */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (c8) bytebuf_read(buf);
    s2c.c0 = (c8) bytebuf_read(buf);
    cff->access_flags = s2c.s;

    /* this class */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (c8) bytebuf_read(buf);
    s2c.c0 = (c8) bytebuf_read(buf);
    cff->this_class = s2c.s;

    /* super class */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (c8) bytebuf_read(buf);
    s2c.c0 = (c8) bytebuf_read(buf);
    cff->super_class = s2c.s;

    /* interfaceRef count */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (c8) bytebuf_read(buf);
    s2c.c0 = (c8) bytebuf_read(buf);
    cff->interface_count = s2c.s;
    /* interfaceRef pool table */
    _parse_interface_pool(_this, buf, cff->interface_count);

    /* fieldRef count */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (c8) bytebuf_read(buf);
    s2c.c0 = (c8) bytebuf_read(buf);
    cff->fields_count = s2c.s;

    /* fieldRef pool table */
    _parse_field_pool(_this, buf, cff->fields_count);
    /* methodRef count */
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (c8) bytebuf_read(buf);
    s2c.c0 = (c8) bytebuf_read(buf);
    cff->methods_count = s2c.s;

    /* methodRef pool table */
    _parse_method_pool(_this, buf, cff->methods_count);

    //attribute
    //fread(short_tmp, 2, 1, fp);
    s2c.c1 = (c8) bytebuf_read(buf);
    s2c.c0 = (c8) bytebuf_read(buf);
    cff->attributes_count = s2c.s;
    _parse_attribute_pool(_this, buf, cff->attributes_count);

    //fclose(fp);

    _class_optimize(_this);

    _this->status = CLASS_STATUS_LOADED;
    return 0;
}

JClass *class_parse(Instance *loader, ByteBuf *bytebuf, Runtime *runtime) {
    JClass *tmpclazz = NULL;
    if (bytebuf != NULL) {
        tmpclazz = class_create(runtime);
        tmpclazz->jloader = loader;

        s32 iret = tmpclazz->_load_class_from_bytes(tmpclazz, bytebuf);//load file

        if (iret == 0) {
            classes_put(runtime->jvm, tmpclazz);

            class_prepar(loader, tmpclazz, runtime);
            gc_obj_hold(runtime->jvm->collector, tmpclazz);

#if _JVM_DEBUG_LOG_LEVEL > 2
            jvm_printf("load class (%016llx load %016llx):  %s \n", (s64) (intptr_t) loader, (s64) (intptr_t) tmpclazz, utf8_cstr(tmpclazz->name));
#endif
        } else {
            class_destory(tmpclazz);
            tmpclazz = NULL;
        }
    }
    return tmpclazz;
}

JClass *load_class(Instance *jloader, Utf8String *pClassName, Runtime *runtime) {
    if (!pClassName)return NULL;
    MiniJVM *jvm = runtime->jvm;
    //
    Utf8String *clsName = utf8_create_copy(pClassName);
    utf8_replace_c(clsName, ".", "/");

    JClass *tmpclazz = classes_get(jvm, jloader, clsName);

    if (utf8_indexof_c(clsName, "[") == 0) {
        tmpclazz = array_class_create_get(runtime, clsName);
    }
    if (!tmpclazz) {
        ByteBuf *bytebuf = NULL;
        PeerClassLoader *pcl = jloader ? classLoaders_find_by_instance(jvm, jloader) : jvm->boot_classloader;


        utf8_append_c(clsName, ".class");
        bytebuf = load_file_from_classpath(pcl, clsName);//load class from classloader
        if (bytebuf != NULL) {
            tmpclazz = class_parse(jloader, bytebuf, runtime);
            bytebuf_destory(bytebuf);
        } else if (jloader) { //using appclassloader load
            if (jvm->shortcut.launcher_loadClass) {
//                if (utf8_equals_c(pClassName, "test/AppManagerTest")) {
//                    int debug = 1;
//                }

                runtime->thrd_info->no_pause++;
                utf8_replace_c(clsName, "/", ".");
                Instance *jstr = jstring_create(pClassName, runtime);
                push_ref(runtime->stack, jstr);
                push_ref(runtime->stack, jloader);

                s32 ret = execute_method_impl(jvm->shortcut.launcher_loadClass, runtime);
                if (!ret) {
                    Instance *ins_of_clazz = pop_ref(runtime->stack);
                    if (ins_of_clazz) {
                        tmpclazz = insOfJavaLangClass_get_classHandle(runtime, ins_of_clazz);
                    }
                } else {
                    print_exception(runtime);
                    Instance *ins = pop_ref(runtime->stack);
                    //jvm_printf("load class exception:%s\n", utf8_cstr(ins->mb.clazz->name));
                }
                runtime->thrd_info->no_pause--;
            }
        }
        if (jvm->jdwp_enable && jvm->jdwpserver && tmpclazz)event_on_class_prepare(jvm->jdwpserver, runtime, tmpclazz);
    }
#if _JVM_DEBUG_LOG_LEVEL > 2
    if (!tmpclazz) {
        //jvm_printf("class not found in bootstrap classpath:  %s \n", utf8_cstr(clsName));
    }
#endif
    utf8_destory(clsName);
    return tmpclazz;
}


s32 _DESTORY_CLASS(JClass *clazz) {
#if _JVM_DEBUG_LOG_LEVEL > 2
    jvm_printf("destroy class (%016llx load %016llx):  %s \n", (s64) (intptr_t) clazz->jloader, (s64) (intptr_t) clazz, utf8_cstr(clazz->name));
#endif
    //
    _class_method_info_destory(clazz);
    _class_bootstrap_methods_destory(clazz);
    _class_interface_pool_destory(clazz);
    _class_field_info_destory(clazz);
    _class_constant_pool_destory(clazz);
    _class_attribute_info_destory(clazz);
    clazz->field_static = NULL;
    if (clazz->constant_item_ptr)jvm_free(clazz->constant_item_ptr);
    clazz->constant_item_ptr = NULL;
    jthreadlock_destory(&clazz->mb);
    constant_list_destory(clazz);
    utf8_destory(clazz->name);
    return 0;
}
