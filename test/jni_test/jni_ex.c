//
// Created by gust on 2017/12/28.
//

#include <stdio.h>
#include "jvm.h"

int test_JniTest_getValue(Runtime *runtime, Class *clazz) {
    JniEnv *env = runtime->jnienv;
    int v = env->localvar_getInt(runtime, 0);
    printf("native test_JniTest_getValue(I)I invoked: v = %d\n", v);
    env->push_int(runtime->stack, v + 1);
    return 0;
}
int test_JniTest_test(Runtime *runtime, Class *clazz) {

    printf("native test_JniTest_test()V invoked: \n");

    return 0;
}

static java_native_method method_test2_table[] = {
        {"test/JniTest", "getValue", "(I)I", test_JniTest_getValue},
        {"test/JniTest", "test", "()V", test_JniTest_test},
};

void JNI_OnLoad(JniEnv *env) {
    env->native_reg_lib(&(method_test2_table[0]), sizeof(method_test2_table) / sizeof(java_native_method));
}
void JNI_OnUnload(JniEnv *env) {
}

int main(int argc, char **argv) {
    return 0;
}