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

    //get startup dir
    Utf8String *startup_dir = utf8_create_c(argv[0]);
#if __JVM_OS_VS__ || __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__
    utf8_replace_c(startup_dir, "\\", "/");
#endif
    s32 dpos = utf8_last_indexof_c(startup_dir, "/");
    if (dpos > 0)utf8_substring(startup_dir, 0, dpos);
    else if (dpos < 0) {
        utf8_clear(startup_dir);
        utf8_append_c(startup_dir, "./");
    }
    if (utf8_char_at(startup_dir, startup_dir->length - 1) != '/')utf8_append_c(startup_dir, "/");
#if _JVM_DEBUG_LOG_LEVEL > 0
    jvm_printf("App dir:%s\n", utf8_cstr(startup_dir));
#endif


    Utf8String *mainClassName = utf8_create();
    if (argc > 1) {
        utf8_append_c(mainClassName, (c8 *) argv[1]);
    } else {
        utf8_clear(mainClassName);
//        utf8_append_c(mainClassName, "test.HelloWorld");
//        utf8_append_c(mainClassName, "test.Foo1");
//        utf8_append_c(mainClassName, "test.Foo2");
//        utf8_append_c(mainClassName, "test.Foo3");
//        utf8_append_c(mainClassName, "test.ThreadDaemon");
        utf8_append_c(mainClassName, "test.SpecTest");
//        utf8_append_c(mainClassName, "test.MultiThread");
//        utf8_append_c(mainClassName, "test.ExecuteSpeed");
//        utf8_append_c(mainClassName, "test.TestFile");
//        utf8_append_c(mainClassName, "test.HttpServer");
//        utf8_append_c(mainClassName, "test.BpDeepTest");
//        utf8_append_c(mainClassName, "test.ReflectTest");
//        utf8_append_c(mainClassName, "test.LambdaTest");
//        utf8_append_c(mainClassName, "test.NioBufferTest");
        jvm_printf("[INFO]ccjvm %s\n", utf8_cstr(mainClassName));
    }
    s32 ret = jvm_run_main(mainClassName, startup_dir);
    utf8_destory(mainClassName);
    utf8_destory(startup_dir);
    return ret;

}
