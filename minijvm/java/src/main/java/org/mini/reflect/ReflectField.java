/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

import org.mini.reflect.vm.RConst;
import org.mini.reflect.vm.RefNative;

/**
 *
 * 类成员的反射，初始化时传进mini jvm 中的 FieldIndo 实例地址，mapField会为此实例 赋值
 *
 * @author gust
 */
public class ReflectField {
    ReflectClass refClass;
    
    //不可随意改动字段类型及名字，要和native一起改
    public long fieldId;
    public String fieldName;
    public String signature;
    public short accessFlags;
    public byte type;
    char bytesTag; //'1','2','4','8','R'

    public ReflectField(ReflectClass c, long fid) {
        if (fid == 0) {
            throw new IllegalArgumentException();
        }
        refClass = c;
        this.fieldId = fid;
        mapField(fieldId);
        bytesTag = RConst.getBytes(type);
    }

    public String toString() {
        return Long.toString(fieldId, 16) + "|"
                + fieldName + "|"
                + signature + "|access:"
                + Integer.toHexString(accessFlags) + "|";
    }

    public void setValObj(Object object, Object val) {
        long arrayId = RefNative.obj2id(object);
        switch (bytesTag) {
            case '1': {
                if (type == RConst.TAG_BOOLEAN) {
                    setFieldVal(arrayId, fieldId, ((Boolean) val) ? 1 : 0);

                } else {
                    setFieldVal(arrayId, fieldId, (Byte) val);
                }
                break;
            }
            case '2': {
                if (type == RConst.TAG_CHAR) {
                    setFieldVal(arrayId, fieldId, (Character) val);

                } else {
                    setFieldVal(arrayId, fieldId, (Byte) val);
                }
                break;
            }
            case '4': {
                setFieldVal(arrayId, fieldId, (Integer) val);
                break;
            }
            case '8': {
                setFieldVal(arrayId, fieldId, (Long) val);
                break;
            }
            case 'R': {
                setFieldVal(arrayId, fieldId, RefNative.obj2id(val));
                break;
            }
        }
        throw new IllegalArgumentException();
    }

    public Object getValObj(Object object) {
        long arrayId = RefNative.obj2id(object);

        switch (bytesTag) {
            case '1':
                if (type == RConst.TAG_BOOLEAN) {
                    return getFieldVal(arrayId, fieldId) != 0;
                }
                return ((byte) getFieldVal(arrayId, fieldId));

            case '2':
                if (type == RConst.TAG_CHAR) {
                    return ((char) getFieldVal(arrayId, fieldId));
                }
                return ((short) getFieldVal(arrayId, fieldId));
            case '4':
                return ((int) getFieldVal(arrayId, fieldId));
            case '8':
                return getFieldVal(arrayId, fieldId);
            case 'R': {
                return RefNative.id2obj(getFieldVal(arrayId, fieldId));
            }
        }
        throw new IllegalArgumentException();
    }

    native void mapField(long fid);

    static native long getFieldVal(long objId, long fieldId);

    static native void setFieldVal(long objId, long fieldId, long val);
}
