/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

import org.mini.reflect.vm.RConst;
import org.mini.reflect.vm.RefNative;

import java.lang.reflect.Type;

/**
 * 类成员的反射，初始化时传进mini jvm 中的 FieldIndo 实例地址，mapField会为此实例 赋值
 *
 * @author gust
 */
public class ReflectField {
    ReflectClass refClass;

    //不可随意改动字段类型及名字，要和native一起改
    public long fieldId;
    public String fieldName;
    public String descriptor;
    public String signature;
    public short accessFlags;
    public byte type;
    char bytesTag; //'1','2','4','8','R'
    public long fieldOffset;

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
                + descriptor + "|access:"
                + Integer.toHexString(accessFlags) + "|";
    }

    static class TypeFieldImpl implements Type {
        String name;

        public String getTypeName() {
            return name;
        }
    }

    public Type getGenericType() {
        String s = "";
        if (signature != null) signature.substring(signature.indexOf(')') + 1);
        else s = descriptor.substring(descriptor.indexOf(')') + 1);
        TypeFieldImpl t = new TypeFieldImpl();
        t.name = s;
        return t;
    }

    public void setValObj(Object object, Object val) {
        switch (bytesTag) {
            case '1': {
                if (type == RConst.TAG_BOOLEAN) {
                    setFieldVal(object, fieldId, ((Boolean) val) ? 1 : 0);

                } else {
                    setFieldVal(object, fieldId, (Byte) val);
                }
                break;
            }
            case '2': {
                if (type == RConst.TAG_CHAR) {
                    setFieldVal(object, fieldId, (Character) val);

                } else {
                    setFieldVal(object, fieldId, (Byte) val);
                }
                break;
            }
            case '4': {
                if (type == RConst.TAG_FLOAT) {
                    float fv = ((Float) val).floatValue();
                    setFieldVal(object, fieldId, Float.floatToIntBits(fv));
                } else {
                    setFieldVal(object, fieldId, (Integer) val);
                }
                break;
            }
            case '8': {
                if (type == RConst.TAG_FLOAT) {
                    double dv = ((Double) val).doubleValue();
                    setFieldVal(object, fieldId, Double.doubleToLongBits(dv));
                } else {
                    setFieldVal(object, fieldId, (Long) val);
                }
                break;
            }
            case 'R': {
                setFieldVal(object, fieldId, RefNative.obj2id(val));
                break;
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    public Object getValObj(Object object) {

        switch (bytesTag) {
            case '1':
                if (type == RConst.TAG_BOOLEAN) {
                    return getFieldVal(object, fieldId) != 0;
                }
                return ((byte) getFieldVal(object, fieldId));
            case '2':
                if (type == RConst.TAG_CHAR) {
                    return ((char) getFieldVal(object, fieldId));
                }
                return ((short) getFieldVal(object, fieldId));
            case '4':
                int iv = ((int) getFieldVal(object, fieldId));
                if (type == RConst.TAG_FLOAT) {
                    return Float.intBitsToFloat(iv);
                }
                return iv;
            case '8':
                long lv = getFieldVal(object, fieldId);
                if (type == RConst.TAG_DOUBLE) {
                    return Double.longBitsToDouble(lv);
                }
                return lv;
            case 'R': {
                return RefNative.id2obj(getFieldVal(object, fieldId));
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    native void mapField(long fid);

    static native long getFieldVal(Object obj, long fieldId);

    static native void setFieldVal(Object obj, long fieldId, long val);
}
