#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>


#include "jvm.h"
#include "bytebuf.h"

void _on_jvm_sig(int no) {

    printf("[ERROR]jvm signo:%d  errno: %d , %s\n", no, errno, strerror(errno));
    exit(no);
}

s32 main(int argc, const char *argv[]) {
//    signal(SIGABRT, _on_jvm_sig);
//    signal(SIGFPE, _on_jvm_sig);
//    signal(SIGSEGV, _on_jvm_sig);
//    signal(SIGTERM, _on_jvm_sig);
//#ifdef SIGPIPE
//    signal(SIGPIPE, _on_jvm_sig);
//#endif

    Utf8String *mainClassName = utf8_create();
    if (argc > 1) {
        utf8_append_c(mainClassName, (c8 *) argv[1]);
    } else {
        utf8_clear(mainClassName);
//        utf8_append_c(mainClassName, "test.HelloWorld");
//        utf8_append_c(mainClassName, "test.Foo1");
//        utf8_append_c(mainClassName, "test.Foo2");
        utf8_append_c(mainClassName, "test.Foo3");
//        utf8_append_c(mainClassName, "test.ThreadDaemon");
//        utf8_append_c(mainClassName, "test.SpecTest");
//        utf8_append_c(mainClassName, "test.MultiThread");
//        utf8_append_c(mainClassName, "test.ExecuteSpeed");
//        utf8_append_c(mainClassName, "test.TestFile");
//        utf8_append_c(mainClassName, "test.HttpServer");
//        utf8_append_c(mainClassName, "test.BpDeepTest");
//        utf8_append_c(mainClassName, "test.ReflectTest");
//        utf8_append_c(mainClassName, "test.LambdaTest");
//        utf8_append_c(mainClassName, "test.NioBufferTest");
        jvm_printf("[INFO]ccjvm test.Test\n");
    }
    s32 ret = jvm_run_main(mainClassName);
    utf8_destory(mainClassName);
    return ret;

}
