//
// Created by Gust on 2020/5/7.
//

#ifndef JAVA2LLVM_JNI_H
#define JAVA2LLVM_JNI_H


typedef int jint;
typedef signed char jbyte;
typedef long long jlong;
typedef unsigned char jboolean;
typedef unsigned short jchar;
typedef short jshort;
typedef float jfloat;
typedef double jdouble;

typedef jint jsize;

//#define JNIEXPORT __declspec(dllexport)
#define JNIEXPORT
//#define JNIIMPORT __declspec(dllimport)
//#define JNICALL __stdcall
#define JNICALL

#ifdef __cplusplus

class _jobject {};
class _jclass : public _jobject {};
class _jthrowable : public _jobject {};
class _jstring : public _jobject {};
class _jarray : public _jobject {};
class _jbooleanArray : public _jarray {};
class _jbyteArray : public _jarray {};
class _jcharArray : public _jarray {};
class _jshortArray : public _jarray {};
class _jintArray : public _jarray {};
class _jlongArray : public _jarray {};
class _jfloatArray : public _jarray {};
class _jdoubleArray : public _jarray {};
class _jobjectArray : public _jarray {};

typedef _jobject *jobject;
typedef _jclass *jclass;
typedef _jthrowable *jthrowable;
typedef _jstring *jstring;
typedef _jarray *jarray;
typedef _jbooleanArray *jbooleanArray;
typedef _jbyteArray *jbyteArray;
typedef _jcharArray *jcharArray;
typedef _jshortArray *jshortArray;
typedef _jintArray *jintArray;
typedef _jlongArray *jlongArray;
typedef _jfloatArray *jfloatArray;
typedef _jdoubleArray *jdoubleArray;
typedef _jobjectArray *jobjectArray;

#else

struct _jobject;

typedef struct _jobject *jobject;
typedef jobject jclass;
typedef jobject jthrowable;
typedef jobject jstring;
typedef jobject jarray;
typedef jarray jbooleanArray;
typedef jarray jbyteArray;
typedef jarray jcharArray;
typedef jarray jshortArray;
typedef jarray jintArray;
typedef jarray jlongArray;
typedef jarray jfloatArray;
typedef jarray jdoubleArray;
typedef jarray jobjectArray;

#endif


typedef jobject jweak;

typedef union jvalue {
    jboolean z;
    jbyte b;
    jchar c;
    jshort s;
    jint i;
    jlong j;
    jfloat f;
    jdouble d;
    jobject l;
} jvalue;

struct _jfieldID;
typedef struct _jfieldID *jfieldID;

struct _jmethodID;
typedef struct _jmethodID *jmethodID;


typedef struct _JNIEnv JNIEnv;

struct _JNIEnv {
    jint (*Throw)(JNIEnv *env, jthrowable obj);

    jint (*ThrowNew)(JNIEnv *env, jclass clazz, const char *message);

    jstring (*NewString)(JNIEnv *env, const jchar *unicodeChars, jsize len);

    jstring (*NewStringUTF)(JNIEnv *env, const char *bytes);

    jclass (*FindClass)(JNIEnv *env, const char *name);

    jmethodID (*GetMethodID)(JNIEnv *env, jclass clazz, const char *name, const char *sig);
};


#endif //JAVA2LLVM_JNI_H
