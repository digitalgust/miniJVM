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


#if __JVM_OS_MINGW__

#ifndef AI_ALL
#define    AI_ALL        0x00000100
#endif

/*--------------------------------------------------------------------------------------

    By Marco Ladino - mladinox.. jan/2016


    MinGW 3.45 thru 4.5 versions, don't have the socket functions:

    --> inet_ntop(..)
    --> inet_pton(..)

    But with this adapted code using the original functions from FreeBSD,
    one can to use it in the C/C++ Applications, without problem..!

    This implementation, include tests for IPV4 and IPV6 addresses,
    and is full C/C++ compatible..

--------------------------------------------------------------------------------------*/


/*
 * Copy src to string dst of size siz.  At most siz-1 characters
 * will be copied.  Always NUL terminates (unless siz == 0).
 * Returns strlen(src); if retval >= siz, truncation occurred.
 */
size_t strlcpy(c8 *dst, const c8 *src, size_t siz) {
    c8 *d = dst;
    const c8 *s = src;
    size_t n = siz;

    /* Copy as many bytes as will fit */
    if (n != 0) {
        while (--n != 0) {
            if ((*d++ = *s++) == '\0')
                break;
        }
    }

    /* Not enough room in dst, add NUL and traverse rest of src */
    if (n == 0) {
        if (siz != 0)
            *d = '\0';        /* NUL-terminate dst */
        while (*s++);
    }

    return (s - src - 1);    /* count does not include NUL */
}


#include <Ws2tcpip.h>
#include <stdio.h>


//------------------------------------------------------------------------------------
//                              Network
//------------------------------------------------------------------------------------

#ifndef InetNtopA

/*%
 * WARNING: Don't even consider trying to compile this on a system where
 * sizeof(int) < 4.  sizeof(int) > 4 is fine; all the world's not a VAX.
 */

static c8 *inet_ntop4(const u8 *src, c8 *dst, socklen_t size);

static c8 *inet_ntop6(const u8 *src, c8 *dst, socklen_t size);

/* char *
 * inet_ntop(af, src, dst, size)
 *	convert a network format address to presentation format.
 * return:
 *	pointer to presentation format address (`dst'), or NULL (see errno).
 * author:
 *	Paul Vixie, 1996.
 */
c8 *inet_ntop(s32 af, const void *src, c8 *dst, socklen_t size) {
    switch (af) {
        case AF_INET:
            return (inet_ntop4((const u8 *) src, dst, size));
        case AF_INET6:
            return (inet_ntop6((const u8 *) src, dst, size));
        default:
            return (NULL);
    }
    /* NOTREACHED */
}

/* const char *
 * inet_ntop4(src, dst, size)
 *	format an IPv4 address
 * return:
 *	`dst' (as a const)
 * notes:
 *	(1) uses no statics
 *	(2) takes a u_char* not an in_addr as input
 * author:
 *	Paul Vixie, 1996.
 */
static c8 *inet_ntop4(const u8 *src, c8 *dst, socklen_t size) {
    static const c8 fmt[] = "%u.%u.%u.%u";
    c8 tmp[sizeof "255.255.255.255"];
    s32 l;

    l = snprintf(tmp, sizeof(tmp), fmt, src[0], src[1], src[2], src[3]);
    if (l <= 0 || (socklen_t) l >= size) {
        return (NULL);
    }
    strlcpy(dst, tmp, size);
    return (dst);
}

/* const char *
 * inet_ntop6(src, dst, size)
 *	convert IPv6 binary address into presentation (printable) format
 * author:
 *	Paul Vixie, 1996.
 */
static c8 *inet_ntop6(const u8 *src, c8 *dst, socklen_t size) {
    /*
     * Note that int32_t and int16_t need only be "at least" large enough
     * to contain a value of the specified size.  On some systems, like
     * Crays, there is no such thing as an integer variable with 16 bits.
     * Keep this in mind if you think this function should have been coded
     * to use pointer overlays.  All the world's not a VAX.
     */
    c8 tmp[sizeof "ffff:ffff:ffff:ffff:ffff:ffff:255.255.255.255"], *tp;
    struct {
        s32 base, len;
    } best, cur;
#define NS_IN6ADDRSZ    16
#define NS_INT16SZ    2
    u_int words[NS_IN6ADDRSZ / NS_INT16SZ];
    s32 i;

    /*
     * Preprocess:
     *	Copy the input (bytewise) array into a wordwise array.
     *	Find the longest run of 0x00's in src[] for :: shorthanding.
     */
    memset(words, '\0', sizeof words);
    for (i = 0; i < NS_IN6ADDRSZ; i++)
        words[i / 2] |= (src[i] << ((1 - (i % 2)) << 3));
    best.base = -1;
    best.len = 0;
    cur.base = -1;
    cur.len = 0;
    for (i = 0; i < (NS_IN6ADDRSZ / NS_INT16SZ); i++) {
        if (words[i] == 0) {
            if (cur.base == -1)
                cur.base = i, cur.len = 1;
            else
                cur.len++;
        } else {
            if (cur.base != -1) {
                if (best.base == -1 || cur.len > best.len)
                    best = cur;
                cur.base = -1;
            }
        }
    }
    if (cur.base != -1) {
        if (best.base == -1 || cur.len > best.len)
            best = cur;
    }
    if (best.base != -1 && best.len < 2)
        best.base = -1;

    /*
     * Format the result.
     */
    tp = tmp;
    for (i = 0; i < (NS_IN6ADDRSZ / NS_INT16SZ); i++) {
        /* Are we inside the best run of 0x00's? */
        if (best.base != -1 && i >= best.base &&
            i < (best.base + best.len)) {
            if (i == best.base)
                *tp++ = ':';
            continue;
        }
        /* Are we following an initial run of 0x00s or any real hex? */
        if (i != 0)
            *tp++ = ':';
        /* Is this address an encapsulated IPv4? */
        if (i == 6 && best.base == 0 && (best.len == 6 ||
                                         (best.len == 7 && words[7] != 0x0001) ||
                                         (best.len == 5 && words[5] == 0xffff))) {
            if (!inet_ntop4(src + 12, tp, sizeof tmp - (tp - tmp)))
                return (NULL);
            tp += strlen(tp);
            break;
        }
        tp += sprintf(tp, "%x", words[i]);
    }
    /* Was it a trailing run of 0x00's? */
    if (best.base != -1 && (best.base + best.len) ==
                           (NS_IN6ADDRSZ / NS_INT16SZ))
        *tp++ = ':';
    *tp++ = '\0';

    /*
     * Check for overflow, copy, and we're done.
     */
    if ((socklen_t) (tp - tmp) > size) {
        return (NULL);
    }
    strcpy(dst, tmp);
    return (dst);
}

#endif

#ifndef InetPtonA

/*%
 * WARNING: Don't even consider trying to compile this on a system where
 * sizeof(int) < 4.  sizeof(int) > 4 is fine; all the world's not a VAX.
 */

static s32 inet_pton4(const c8 *src, u_char *dst);

static s32 inet_pton6(const c8 *src, u_char *dst);

/* int
 * inet_pton(af, src, dst)
 *	convert from presentation format (which usually means ASCII printable)
 *	to network format (which is usually some kind of binary format).
 * return:
 *	1 if the address was valid for the specified address family
 *	0 if the address wasn't valid (`dst' is untouched in this case)
 *	-1 if some other error occurred (`dst' is untouched in this case, too)
 * author:
 *	Paul Vixie, 1996.
 */
s32 inet_pton(s32 af, const c8 *src, void *dst) {
    switch (af) {
        case AF_INET:
            return (inet_pton4(src, (u8 *) dst));
        case AF_INET6:
            return (inet_pton6(src, (u8 *) dst));
        default:
            return (-1);
    }
    /* NOTREACHED */
}

/* int
 * inet_pton4(src, dst)
 *	like inet_aton() but without all the hexadecimal and shorthand.
 * return:
 *	1 if `src' is a valid dotted quad, else 0.
 * notice:
 *	does not touch `dst' unless it's returning 1.
 * author:
 *	Paul Vixie, 1996.
 */
static s32 inet_pton4(const c8 *src, u_char *dst) {
    static const c8 digits[] = "0123456789";
    s32 saw_digit, octets, ch;
#define NS_INADDRSZ    4
    u_char tmp[NS_INADDRSZ], *tp;

    saw_digit = 0;
    octets = 0;
    *(tp = tmp) = 0;
    while ((ch = *src++) != '\0') {
        const c8 *pch;

        if ((pch = strchr(digits, ch)) != NULL) {
            u_int uiNew = *tp * 10 + (pch - digits);

            if (saw_digit && *tp == 0)
                return (0);
            if (uiNew > 255)
                return (0);
            *tp = uiNew;
            if (!saw_digit) {
                if (++octets > 4)
                    return (0);
                saw_digit = 1;
            }
        } else if (ch == '.' && saw_digit) {
            if (octets == 4)
                return (0);
            *++tp = 0;
            saw_digit = 0;
        } else
            return (0);
    }
    if (octets < 4)
        return (0);
    memcpy(dst, tmp, NS_INADDRSZ);
    return (1);
}

/* int
 * inet_pton6(src, dst)
 *	convert presentation level address to network order binary form.
 * return:
 *	1 if `src' is a valid [RFC1884 2.2] address, else 0.
 * notice:
 *	(1) does not touch `dst' unless it's returning 1.
 *	(2) :: in a full address is silently ignored.
 * credit:
 *	inspired by Mark Andrews.
 * author:
 *	Paul Vixie, 1996.
 */
static s32 inet_pton6(const c8 *src, u_char *dst) {
    static const c8 xdigits_l[] = "0123456789abcdef",
            xdigits_u[] = "0123456789ABCDEF";
#define NS_IN6ADDRSZ    16
#define NS_INT16SZ    2
    u_char tmp[NS_IN6ADDRSZ], *tp, *endp, *colonp;
    const c8 *xdigits, *curtok;
    s32 ch, seen_xdigits;
    u_int val;

    memset((tp = tmp), '\0', NS_IN6ADDRSZ);
    endp = tp + NS_IN6ADDRSZ;
    colonp = NULL;
    /* Leading :: requires some special handling. */
    if (*src == ':')
        if (*++src != ':')
            return (0);
    curtok = src;
    seen_xdigits = 0;
    val = 0;
    while ((ch = *src++) != '\0') {
        const c8 *pch;

        if ((pch = strchr((xdigits = xdigits_l), ch)) == NULL)
            pch = strchr((xdigits = xdigits_u), ch);
        if (pch != NULL) {
            val <<= 4;
            val |= (pch - xdigits);
            if (++seen_xdigits > 4)
                return (0);
            continue;
        }
        if (ch == ':') {
            curtok = src;
            if (!seen_xdigits) {
                if (colonp)
                    return (0);
                colonp = tp;
                continue;
            } else if (*src == '\0') {
                return (0);
            }
            if (tp + NS_INT16SZ > endp)
                return (0);
            *tp++ = (u_char) (val >> 8) & 0xff;
            *tp++ = (u_char) val & 0xff;
            seen_xdigits = 0;
            val = 0;
            continue;
        }
        if (ch == '.' && ((tp + NS_INADDRSZ) <= endp) &&
            inet_pton4(curtok, tp) > 0) {
            tp += NS_INADDRSZ;
            seen_xdigits = 0;
            break;    /*%< '\\0' was seen by inet_pton4(). */
        }
        return (0);
    }
    if (seen_xdigits) {
        if (tp + NS_INT16SZ > endp)
            return (0);
        *tp++ = (u_char) (val >> 8) & 0xff;
        *tp++ = (u_char) val & 0xff;
    }
    if (colonp != NULL) {
        /*
         * Since some memmove()'s erroneously fail to handle
         * overlapping regions, we'll do the shift by hand.
         */
        const s32 n = tp - colonp;
        s32 i;

        if (tp == endp)
            return (0);
        for (i = 1; i <= n; i++) {
            endp[-i] = colonp[n - i];
            colonp[n - i] = 0;
        }
        tp = endp;
    }
    if (tp != endp)
        return (0);
    memcpy(dst, tmp, NS_IN6ADDRSZ);
    return (1);
}

#endif

/*------------------------------------------
    MAIN  -  tester
--------------------------------------------
    by Marco Ladino - mladinox
------------------------------------------*/
//int main()
//{
//    in_addr ipv4_address;
//    in_addr6 ipv6_address;
//    char strIP[128];
//    char *pBytes=0;
//    int i=0;
//
//    //ipv4 addresses..
//    pBytes = (char *)&ipv4_address;
//    inet_pton_2( AF_INET, "127.0.0.1", &ipv4_address); //31.175.162.251
//    printf("inet_pton=(ipv4) -> %02x, %02x, %02x, %02x\n", pBytes[0],pBytes[1],pBytes[2],pBytes[3]);
//    inet_ntop_2(AF_INET,&ipv4_address, strIP, INET_ADDRSTRLEN);
//    printf("inet_ntop=(ipv4) -> %s\n", strIP);
//
//    //ipv6 addresses..
//    pBytes = (char *)&ipv6_address;
//    inet_pton_2( AF_INET6, "2001:DB8:CAFE:0:beef:800:200C:417A", &ipv6_address); //2001:DB8:0:0:8:800:200C:417A  fe80:cafe:250:8dff:fecb:e3dc:beef
//    printf("inet_pton=(ipv6) -> ");
//    for (i=0;i<16;i++)
//    {
//        printf("%02x ",pBytes[i]);
//    }
//    printf("\n");
//    inet_ntop_2(AF_INET6,&ipv6_address, strIP, INET6_ADDRSTRLEN);
//    printf("inet_ntop=(ipv6) -> %s\n", strIP);
//
//
//
//    return 0;
//
//}

#endif


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
