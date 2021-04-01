#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

#include "jni.h"
#include "jvm.h"
#include "metadata.h"

struct _jobject {
    JObject obj;
};


static inline JThreadRuntime *get_runtime() {
    return tss_get(TLS_KEY_JTHREADRUNTIME);
}


jint Throw(JNIEnv *env, jthrowable obj) {
    JThreadRuntime *runtime = tss_get(TLS_KEY_JTHREADRUNTIME);
    StackFrame *cur = runtime->tail;
    runtime->exception = (JObject *) obj;
    return 0;
}

jint ThrowNew(JNIEnv *env, jclass clazz, const char *message) {
    jstring str = env->NewStringUTF(env, message);
    jmethodID method = env->GetMethodID(env, clazz, "<init>", "(Ljava/lang/String;)V");
    return 0;
}

jstring NewStringUTF(JNIEnv *env, const char *bytes) {
    JObject *jstr = construct_string_with_cstr(get_runtime(), (c8 *) bytes);
    return (jstring) jstr;
}

jstring NewString(JNIEnv *env, const jchar *unicodeChars, jsize len) {

    return NULL;
}

jobject NewObject(JNIEnv *env, jclass clazz, jmethodID methodID, ...) {
    JClass *c = (JClass *) clazz;
    JObject *obj = new_instance_with_classraw(get_runtime(), c->raw);
    //todo init with para
    return (jobject) obj;
}

jclass FindClass(JNIEnv *env, const char *name) {
    s32 index = find_global_string_index((c8 *) name);
    JClass *clazz = get_class_by_nameIndex(index);
    return (jclass) clazz;
}

jobject NewObjectA(JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args) {

    return NULL;
}

jobject NewObjectV(JNIEnv *env, jclass clazz, jmethodID methodID, va_list args) {

    return NULL;
}

jmethodID GetStaticMethodID(JNIEnv *env, jclass clazz, const char *name, const char *sig) {

    return NULL;
}

jmethodID GetMethodID(JNIEnv *env, jclass clazz, const char *name, const char *sig) {
    JClass *c = (JClass *) clazz;
    MethodInfo *method = find_methodInfo_by_name(utf8_cstr(c->name), (c8 *) name, (c8 *) sig);
    return (jmethodID) method;
}
