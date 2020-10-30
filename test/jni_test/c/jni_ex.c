//
// Created by gust on 2017/12/28.
//

#include <stdio.h>
#include "jvm.h"

int test_JniTest_getValue(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    s64 t = env->localvar_getLong_2slot(runtime->localvar, 0);//long used 2 slots
    s32 v = env->localvar_getInt(runtime->localvar, 2);
    Instance *jstr = env->localvar_getRefer(runtime->localvar, 3);
    //convert jstring to utf8 string
    void *ustr = env->utf8_create();
    env->jstring_2_utf8(jstr, ustr, runtime);

    printf("native print time = %lld , v = %d ,s = %s\n", t, v, env->utf8_cstr(ustr));
    env->push_int(runtime->stack, v + 1);

    env->utf8_destory(ustr);
    return RUNTIME_STATUS_NORMAL;
}

int test_JniTest_print(Runtime *runtime, JClass *clazz) {
    JniEnv *env = runtime->jnienv;
    int v = env->localvar_getInt(runtime->localvar, 0);
    printf("%d", v);
    return RUNTIME_STATUS_NORMAL;
}

static java_native_method method_test2_table[] = {
        {"test/JniTest", "getValue", "(JILjava/lang/String;)I", test_JniTest_getValue},
        {"test/JniTest", "print",     "(I)V",                    test_JniTest_print},
};

void JNI_OnLoad(MiniJVM *jvm) {
    jvm->env->native_reg_lib(jvm, &(method_test2_table[0]), sizeof(method_test2_table) / sizeof(java_native_method));
}

void JNI_OnUnload(MiniJVM *jvm) {
}

