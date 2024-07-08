//
// Created by Admin on 2024/5/11.
//
#include "jvm.h"
#include "jvm_util.h"

#if defined(__JVM_OS_MINGW__) || defined(__JVM_OS_CYGWIN__) || defined(__JVM_OS_VS__)

#include <WinSock2.h>
#include <Ws2tcpip.h>
#include <wspiapi.h>
#include <io.h>

#if __JVM_OS_VS__
#include "../utils/dirent_win.h"
#include "../utils/tinycthread.h"
#include <direct.h>
#include <io.h>
#endif

#pragma comment(lib, "Ws2_32.lib")

#include <windows.h>
#include <stdio.h>

void get_last_error(Utf8String *error_msg) {
    DWORD errorCode = GetLastError(); // 假设之前调用了某个API并发生了错误
    LPSTR errorMessage = NULL;
    DWORD flags = FORMAT_MESSAGE_ALLOCATE_BUFFER |
                  FORMAT_MESSAGE_FROM_SYSTEM |
                  FORMAT_MESSAGE_IGNORE_INSERTS;

    if (FormatMessageA(flags, NULL, errorCode, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                       (LPSTR) &errorMessage, 0, NULL) != 0) {
        printf("Error code %d: %s\n", (int)errorCode, errorMessage);
        if (error_msg)utf8_append_c(error_msg, errorMessage);
        LocalFree(errorMessage); // 使用完毕后释放由FormatMessage分配的内存
    } else {
        printf("Failed to get error message for error code %d\n", (int)errorCode);
    }
}

/**
 * implement clock_gettime windows version same as linux
 * @param tv
 * @return
 */
//s32 clock_gettime(s32, struct timespec *tv) {
//    static int initialized = 0;
//    static LARGE_INTEGER freq, startCount;
//    static struct timespec tv_start;
//    LARGE_INTEGER curCount;
//    time_t sec_part;
//    s64 nsec_part;
//
//    if (!initialized) {
//        QueryPerformanceFrequency(&freq);
//        QueryPerformanceCounter(&startCount);
//        timespec_get(&tv_start, TIME_UTC);
//        initialized = 1;
//    }
//
//    QueryPerformanceCounter(&curCount);
//
//    curCount.QuadPart -= startCount.QuadPart;
//    sec_part = curCount.QuadPart / freq.QuadPart;
//    nsec_part = (s64) ((curCount.QuadPart - (sec_part * freq.QuadPart))
//                        * 1000000000UL / freq.QuadPart);
//
//    tv->tv_sec = tv_start.tv_sec + sec_part;
//    tv->tv_nsec = tv_start.tv_nsec + nsec_part;
//    if (tv->tv_nsec >= 1000000000UL) {
//        tv->tv_sec += 1;
//        tv->tv_nsec -= 1000000000UL;
//    }
//    return 0;
//}

void makePipe(HANDLE p[2], Runtime *runtime) {
    SECURITY_ATTRIBUTES sa;
    sa.nLength = sizeof(sa);
    sa.bInheritHandle = 1;
    sa.lpSecurityDescriptor = 0;

    s32 success = CreatePipe(p, p + 1, &sa, 0);
    if (!success) {
        exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
    }
}

s32 descriptor(HANDLE h, Runtime *runtime) {
    s32 fd = _open_osfhandle((intptr_t) (h), 0);

    return fd;
}


s32 os_execute(Runtime *runtime, Instance *jstrArr, Instance *jlongArr, ArrayList *cstrList, const c8 *cmd) {

    HANDLE in[] = {0, 0};
    HANDLE out[] = {0, 0};
    HANDLE err[] = {0, 0};

    makePipe(in, runtime);
    SetHandleInformation(in[0], HANDLE_FLAG_INHERIT, 0);
    s32 inDescriptor = descriptor(in[0], runtime);
    if (inDescriptor < 0) {
        exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
        return RUNTIME_STATUS_EXCEPTION;
    }
    jarray_set_field(jlongArr, 2, inDescriptor);
    makePipe(out, runtime);
    SetHandleInformation(out[1], HANDLE_FLAG_INHERIT, 0);
    s32 outDescriptor = descriptor(out[1], runtime);
    if (inDescriptor < 0) {
        exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
        return RUNTIME_STATUS_EXCEPTION;
    }
    jarray_set_field(jlongArr, 3, inDescriptor);
    makePipe(err, runtime);
    SetHandleInformation(err[0], HANDLE_FLAG_INHERIT, 0);
    s32 errDescriptor = descriptor(err[0], runtime);
    if (inDescriptor < 0) {
        exception_throw(JVM_EXCEPTION_IO, runtime, NULL);
        return RUNTIME_STATUS_EXCEPTION;
    }
    jarray_set_field(jlongArr, 4, inDescriptor);

    PROCESS_INFORMATION pi;
    memset(&pi, 0, sizeof(pi));

    STARTUPINFO si;
    memset(&si, 0, sizeof(si));
    si.cb = sizeof(si);
    si.dwFlags = STARTF_USESTDHANDLES;
    si.hStdOutput = in[1];
    si.hStdInput = out[0];
    si.hStdError = err[1];

    BOOL success = CreateProcess(0,
                                 (LPSTR) (cmd),
                                 0,
                                 0,
                                 1,
                                 CREATE_NO_WINDOW | CREATE_UNICODE_ENVIRONMENT,
                                 0,
                                 0,
                                 &si,
                                 &pi);


    CloseHandle(in[1]);
    CloseHandle(out[0]);
    CloseHandle(err[1]);

    if (!success) {
        Utf8String *cstr = utf8_create();
        //get_last_error(cstr);
        exception_throw(JVM_EXCEPTION_IO, runtime, utf8_cstr(cstr));
        return RUNTIME_STATUS_EXCEPTION;
    }

    s64 pid = (s64) (intptr_t) (pi.hProcess);
    jarray_set_field(jlongArr, 0, pid);
    s64 tid = (s64) (intptr_t) (pi.hThread);
    jarray_set_field(jlongArr, 1, tid);
    return 0;
}

s32 os_kill_process(s64 pid) {
    TerminateProcess((HANDLE) (intptr_t) pid, 1);
    return 0;
}

s32 os_waitfor_process(Runtime *runtime, s64 pid, s64 tid, s32 *pExitCode) {
    DWORD exitCode;
    WaitForSingleObject((__refer) (intptr_t) (pid), INFINITE);
    BOOL success = GetExitCodeProcess((HANDLE) (intptr_t) (pid), &exitCode);
    if (!success) {
        exception_throw(JVM_EXCEPTION_ILLEGALARGUMENT, runtime, NULL);
        return RUNTIME_STATUS_EXCEPTION;
    }

    CloseHandle((HANDLE) (intptr_t) (pid));
    CloseHandle((HANDLE) (intptr_t) (tid));

    *pExitCode = exitCode;
    return 0;
}

Utf8String *os_get_tmp_dir() {
    Utf8String *tmps = utf8_create();
    c8 buf[1024];
    s32 len = GetTempPath(1024, buf);
    utf8_append_data(tmps, buf, len);
    return tmps;
}


void os_set_file_length(FILE *file, s64 len) {
    long current_pos = ftell(file);
    fseek(file, 0, SEEK_SET);
    _chsize_s(_fileno(file), len);
    fseek(file, current_pos, SEEK_SET);
}

s32 os_mkdir(const c8 *path) {
    return mkdir(path);
}

s32 os_iswin() {
    return 1;
}

s32 os_fileno(FILE *fd) {
    return _fileno(fd);
}

s32 os_append_libname(Utf8String *libname, const c8 *lib) {
    utf8_append_c(libname, "/lib");
    utf8_append_c(libname, lib);
    utf8_append_c(libname, ".dll");
    utf8_replace_c(libname, "//", "/");
    return 0;
}

s32 os_load_lib_and_init(const c8 *libname, Runtime *runtime) {
    HINSTANCE hInstLibrary = LoadLibrary(libname);
    if (!hInstLibrary) {
        jvm_printf(STR_JNI_LIB_NOT_FOUND, libname);
    } else {
        jni_fun f;
        FARPROC fp = GetProcAddress(hInstLibrary, STR_JNI_ON_LOAD);
        if (!fp) {
            jvm_printf(STR_JNI_ONLOAD_NOT_FOUND, STR_JNI_ON_LOAD);
            return 1;
        } else {
            f = (jni_fun) fp;
            f(runtime->jvm);
            return 2;
        }
    }
    return 0;
}

#endif
