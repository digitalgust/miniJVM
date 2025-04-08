//
// Created by Admin on 2024/5/11.
//

#include <stdio.h>
#include <errno.h>

#include "jvm.h"
#include "jvm_util.h"

#if defined(__JVM_OS_MINGW__) || defined(__JVM_OS_CYGWIN__) || defined(__JVM_OS_VS__)
#else

#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/stat.h>
#include <dlfcn.h>


void safeClose(s32 *fd) {
    if (*fd != -1)
        close(*fd);
    *fd = -1;
}

s32 os_execute(Runtime *runtime, Instance *jstrArr, Instance *jlongArr, ArrayList *cstrList, const c8 *cmd) {

    c8 **argv = (c8 **) cstrList->data;//

    s32 in[] = {-1, -1};
    s32 out[] = {-1, -1};
    s32 err[] = {-1, -1};
    s32 msg[] = {-1, -1};

    if (pipe(in) != 0) {
        exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
        return RUNTIME_STATUS_EXCEPTION;
    }
    jarray_set_field(jlongArr, 2, in[0]);
    if (pipe(out) != 0) {
        exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
        return RUNTIME_STATUS_EXCEPTION;
    }
    jarray_set_field(jlongArr, 3, out[1]);
    if (pipe(err) != 0) {
        exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
        return RUNTIME_STATUS_EXCEPTION;
    }
    jarray_set_field(jlongArr, 4, err[0]);
    if (pipe(msg) != 0) {
        exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
        return RUNTIME_STATUS_EXCEPTION;
    }
    if (fcntl(msg[1], F_SETFD, FD_CLOEXEC) != 0) {
        exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
        return RUNTIME_STATUS_EXCEPTION;
    }

#ifdef __QNX__
    // fork(2) doesn't work in multithreaded QNX programs.  See
  // http://www.qnx.com/developers/docs/6.4.1/neutrino/getting_started/s1_procs.html
  pid_t pid = vfork();
#else
    // We might be able to just use vfork on all UNIX-style systems, but
    // the manual makes it sound dangerous due to the shared
    // parent/child address space, so we use fork if we can.
    pid_t pid = fork();
#endif
    switch (pid) {
        case -1:  // error
            exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
            safeClose(&in[0]);
            safeClose(&out[1]);
            safeClose(&err[0]);
            safeClose(&msg[0]);
            safeClose(&msg[1]);
            return RUNTIME_STATUS_EXCEPTION;
        case 0: {  // child
            // Setup stdin, stdout and stderr
            dup2(in[1], STDOUT_FILENO);
            close(in[0]);
            close(in[1]);
            dup2(out[0], STDIN_FILENO);
            close(out[0]);
            close(out[1]);
            dup2(err[1], STDERR_FILENO);
            close(err[0]);
            close(err[1]);

            close(msg[0]);

            execvp(argv[0], argv);

            // Error if here
            s32 val = errno;
            write(msg[1], &val, sizeof(val));
            _exit(127);  // 使用 _exit 而不是 exit 以避免刷新标准IO缓冲区
        }
            break;

        default: {  // parent
            jarray_set_field(jlongArr, 0, pid);

            safeClose(&in[1]);
            safeClose(&out[0]);
            safeClose(&err[1]);
            safeClose(&msg[1]);

            s32 val;
            s32 r = read(msg[0], &val, sizeof(val));
            if (r == -1) {
                exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
                return RUNTIME_STATUS_EXCEPTION;
            } else if (r) {
                errno = val;
                exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
                return RUNTIME_STATUS_EXCEPTION;
            }
        }
            break;
    }

    safeClose(&msg[0]);

    fcntl(in[0], F_SETFD, FD_CLOEXEC);
    fcntl(out[1], F_SETFD, FD_CLOEXEC);
    fcntl(err[0], F_SETFD, FD_CLOEXEC);
    return 0;
}

s32 os_kill_process(s64 pid) {
    pid_t tpid = (pid_t) pid;
    kill(tpid, SIGTERM);
    return 0;
}

s32 os_waitfor_process(Runtime *runtime, s64 pid, s64 tid, s32 *pExitCode) {
    s32 finished = 0;
    s32 status;
    s32 exitCode;
    while (!finished) {
        waitpid(pid, &status, 0);
        if (WIFEXITED(status)) {
            finished = 1;
            exitCode = WEXITSTATUS(status);
        } else if (WIFSIGNALED(status)) {
            finished = 1;
            exitCode = -1;
        }
    }
    *pExitCode = exitCode;
    return 0;
}

Utf8String *os_get_tmp_dir() {
    Utf8String *tmps = utf8_create();

#ifndef P_tmpdir
#define P_tmpdir "/tmp"
#endif
    utf8_append_c(tmps, P_tmpdir);
    return tmps;
}

void os_set_file_length(FILE *file, s64 len) {
    long current_pos = ftell(file);
    fseek(file, 0, SEEK_SET);
    ftruncate(fileno(file), (off_t) len);
    fseek(file, current_pos, SEEK_SET);
}

s32 os_mkdir(const c8 *path) {
    return mkdir(path, S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH);
}

s32 os_iswin() {
    return 0;
}

s32 os_fileno(FILE *fd) {
    return fileno(fd);
}

s32 os_append_libname(Utf8String *libname, const c8 *lib) {
    utf8_append_c(libname, "/lib");
    utf8_replace_c(libname, "//", "/");
    utf8_append_c(libname, lib);
#if defined(__JVM_OS_MAC__)
    utf8_append_c(libname, ".dylib");
#else //__JVM_OS_LINUX__
    utf8_append_c(libname, ".so");
#endif
    return 0;
}

s32 os_load_lib_and_init(const c8 *libname, Runtime *runtime) {
    __refer lib = dlopen(libname, RTLD_LAZY);
    if (!lib) {
        jvm_printf(STR_JNI_LIB_NOT_FOUND, libname, dlerror());
    } else {
        jni_fun f;
        f = dlsym(lib, STR_JNI_ON_LOAD);
        if (!f) {
            jvm_printf(STR_JNI_ONLOAD_NOT_FOUND, STR_JNI_ON_LOAD);
            return 1;
        } else {
            f(runtime->jvm);
            return 2;
        }
    }
    return 0;
}

void os_get_lang(Utf8String *buf) {
    char *lang = getenv("LC_ALL");//linux
    if (lang == NULL) {
        lang = getenv("LANG");
    }
    if (lang == NULL) {
        lang = getenv("LC_CTYPE");//mac os
    }
    if (lang != NULL) {
        //printf("language: %s\n", lang);
        utf8_append_c(buf, lang);
        utf8_replace_c(buf, "-", "_");
        s32 i = utf8_indexof_c(buf, "_");
        if (i > 0) {
            utf8_substring(buf, 0, i);
        }
    } else {
        //printf("无法获取系统语言环境。\n");
    }
}

/**
 * for posix system, implement convert utf8 path to platform encoding
 * macOS and linux use utf8 as default encoding, so copy src to dst
 */
s32 conv_utf8_2_platform_encoding(ByteBuf *dst, Utf8String *src) {
    // Check input parameters
    if (!dst || !src) {
        return -1;
    }

    // Get source string length
    s32 src_len = src->length;
    if (src_len == 0) {
        bytebuf_expand(dst, 1);  // Allocate space for null terminator
        dst->buf[0] = 0;
        return 0;
    }

    // Expand destination buffer to fit source string plus null terminator
    bytebuf_expand(dst, src_len + 1);

    // Copy source string to destination buffer
    memcpy(dst->buf, src->data, src_len);
    dst->buf[src_len] = 0;  // Add null terminator

    return src_len;
}

s32 conv_platform_encoding_2_utf8(Utf8String *dst, const c8 *src) {
    // Check input parameters
    if (!dst || !src) {
        return -1;
    }

    // Get source string length
    s32 src_len = strlen(src);
    if (src_len == 0) {
        utf8_clear(dst);
        return 0;
    }

    // Expand destination buffer to fit source string plus null terminator
    utf8_expand(dst, src_len + 1);

    // Copy source string to destination buffer
    memcpy(dst->data, src, src_len);
    dst->length = src_len;
    dst->data[src_len] = 0;  // Ensure null termination

    return src_len;
}
#endif //  end of   __JVM_OS_MINGW__ || __JVM_OS_CYGWIN__ || __JVM_OS_VS__