//
// Created by gust on 2017/9/16.
//

#ifndef MINI_JVM_JAVA_NATIVE_JDWP_H
#define MINI_JVM_JAVA_NATIVE_JDWP_H


#include "jvm.h"


#ifdef __cplusplus
extern "C" {
#endif

static c8 *JDWP_CLASS_REFERENCE = "org/mini/reflect/ReflectClass";
static c8 *JDWP_CLASS_FIELD = "org/mini/reflect/ReflectField";
static c8 *JDWP_CLASS_METHOD = "org/mini/reflect/ReflectMethod";
static c8 *JDWP_CLASS_ARRAY = "org/mini/reflect/ReflectArray";
static c8 *JDWP_CLASS_RUNTIME = "org/mini/reflect/StackFrame";
static c8 *JDWP_CLASS_LOCALVARTABLE = "org/mini/reflect/LocalVarTable";
static c8 *JDWP_CLASS_VALUETYPE = "org/mini/reflect/vm/ValueType";


#ifdef __cplusplus
}
#endif


#endif //MINI_JVM_JAVA_NATIVE_JDWP_H
