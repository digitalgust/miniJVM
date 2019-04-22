//
// Created by gust on 2017/9/20.
//

#ifndef MINI_JVM_JDWP_H
#define MINI_JVM_JDWP_H


#include "../utils/utf8_string.h"
#include "../utils/arraylist.h"
#include "jvm.h"
#include "../utils/hashset.h"


#ifdef __cplusplus
extern "C" {
#endif

//=============================      error   ==============================================
enum {
    JDWP_ERROR_INVALID_TAG = 500, //object type id or class tag
    JDWP_ERROR_ALREADY_INVOKING = 502, //previous invoke not complete
    JDWP_ERROR_INVALID_INDEX = 503,
    JDWP_ERROR_INVALID_LENGTH = 504,
    JDWP_ERROR_INVALID_STRING = 506,
    JDWP_ERROR_INVALID_CLASS_LOADER = 507,
    JDWP_ERROR_INVALID_ARRAY = 508,
    JDWP_ERROR_TRANSPORT_LOAD = 509,
    JDWP_ERROR_TRANSPORT_INIT = 510,
    JDWP_ERROR_NATIVE_METHOD = 511,
    JDWP_ERROR_INVALID_COUNT = 512,
    JDWP_ERROR_NONE = 0,
    JDWP_ERROR_INVALID_THREAD = 10,
    JDWP_ERROR_INVALID_THREAD_GROUP = 11,
    JDWP_ERROR_INVALID_PRIORITY = 12,
    JDWP_ERROR_THREAD_NOT_SUSPENDED = 13,
    JDWP_ERROR_THREAD_SUSPENDED = 14,
    JDWP_ERROR_INVALID_OBJECT = 20,
    JDWP_ERROR_INVALID_CLASS = 21,
    JDWP_ERROR_CLASS_NOT_PREPARED = 22,
    JDWP_ERROR_INVALID_METHODID = 23,
    JDWP_ERROR_INVALID_LOCATION = 24,
    JDWP_ERROR_INVALID_FIELDID = 25,
    JDWP_ERROR_INVALID_FRAMEID = 30,
    JDWP_ERROR_NO_MORE_FRAMES = 31,
    JDWP_ERROR_OPAQUE_FRAME = 32,
    JDWP_ERROR_NOT_CURRENT_FRAME = 33,
    JDWP_ERROR_TYPE_MISMATCH = 34,
    JDWP_ERROR_INVALID_SLOT = 35,
    JDWP_ERROR_DUPLICATE = 40,
    JDWP_ERROR_NOT_FOUND = 41,
    JDWP_ERROR_INVALID_MONITOR = 50,
    JDWP_ERROR_NOT_MONITOR_OWNER = 51,
    JDWP_ERROR_INTERRUPT = 52,
    JDWP_ERROR_INVALID_CLASS_FORMAT = 60,
    JDWP_ERROR_CIRCULAR_CLASS_DEFINITION = 61,
    JDWP_ERROR_FAILS_VERIFICATION = 62,
    JDWP_ERROR_ADD_METHOD_NOT_IMPLEMENTED = 63,
    JDWP_ERROR_SCHEMA_CHANGE_NOT_IMPLEMENTED = 64,
    JDWP_ERROR_INVALID_TYPESTATE = 65,
    JDWP_ERROR_NOT_IMPLEMENTED = 99,
    JDWP_ERROR_NULL_POINTER = 100,
    JDWP_ERROR_ABSENT_INFORMATION = 101,
    JDWP_ERROR_INVALID_EVENT_TYPE = 102,
    JDWP_ERROR_ILLEGAL_ARGUMENT = 103,
    JDWP_ERROR_OUT_OF_MEMORY = 110,
    JDWP_ERROR_ACCESS_DENIED = 111,
    JDWP_ERROR_VM_DEAD = 112,
    JDWP_ERROR_INTERNAL = 113,
    JDWP_ERROR_UNATTACHED_THREAD = 115,
};
//=============================      event   ==============================================

#define JDWP_EVENTKIND_SINGLE_STEP  1
#define JDWP_EVENTKIND_BREAKPOINT  2
#define JDWP_EVENTKIND_FRAME_POP  3
#define JDWP_EVENTKIND_EXCEPTION  4
#define JDWP_EVENTKIND_USER_DEFINED  5
#define JDWP_EVENTKIND_THREAD_START  6
#define JDWP_EVENTKIND_THREAD_DEATH  7
#define JDWP_EVENTKIND_CLASS_PREPARE  8
#define JDWP_EVENTKIND_CLASS_UNLOAD  9
#define JDWP_EVENTKIND_CLASS_LOAD  10
#define JDWP_EVENTKIND_FIELD_ACCESS  20
#define JDWP_EVENTKIND_FIELD_MODIFICATION  21
#define JDWP_EVENTKIND_EXCEPTION_CATCH  30
#define JDWP_EVENTKIND_METHOD_ENTRY  40
#define JDWP_EVENTKIND_METHOD_EXIT  41
#define JDWP_EVENTKIND_METHOD_EXIT_WITH_RETURN_VALUE  42
#define JDWP_EVENTKIND_VM_START  90
#define JDWP_EVENTKIND_VM_DEATH  99
#define JDWP_EVENTKIND_VM_DISCONNECTED  100  //Never sent by across JDWP
//=============================      event   ==============================================
static u8 JDWP_STEPDEPTH_INTO = 0;
static u8 JDWP_STEPDEPTH_OVER = 1;
static u8 JDWP_STEPDEPTH_OUT = 2;


static u8 JDWP_STEPSIZE_MIN = 0;
static u8 JDWP_STEPSIZE_LINE = 1;
//=============================      class status   ==============================================

static u8 JDWP_CLASS_STATUS_VERIFIED = 1;
static u8 JDWP_CLASS_STATUS_PREPARED = 2;
static u8 JDWP_CLASS_STATUS_INITIALIZED = 4;
static u8 JDWP_CLASS_STATUS_ERROR = 8;
//=============================      typetag   ==============================================

static u8 JDWP_TYPETAG_CLASS = 1; //ReferenceType is a class.
static u8 JDWP_TYPETAG_INTERFACE = 2; //ReferenceType is an interface.
static u8 JDWP_TYPETAG_ARRAY = 3; //ReferenceType is an array.
//=============================      Thread status   ==============================================
static c8 JDWP_THREAD_ZOMBIE = 0;
static c8 JDWP_THREAD_RUNNING = 1;
static c8 JDWP_THREAD_SLEEPING = 2;
static c8 JDWP_THREAD_MONITOR = 3;
static c8 JDWP_THREAD_WAIT = 4;
//=============================      suspend   ==============================================

static c8 JDWP_SUSPEND_STATUS_SUSPENDED = 0x1;
//=============================      tag   ==============================================

#define JDWP_TAG_ARRAY  91
// '[' - an array object (objectID size).
#define JDWP_TAG_BYTE  66
// 'B' - a byte value (1 byte).
#define JDWP_TAG_CHAR  67
// 'C' - a character value (2 bytes).
#define JDWP_TAG_OBJECT  76
// 'L' - an object (objectID size).
#define JDWP_TAG_FLOAT  70
// 'F' - a float value (4 bytes).
#define JDWP_TAG_DOUBLE  68
// 'D' - a double value (8 bytes).
#define JDWP_TAG_INT  73
// 'I' - an int value (4 bytes).
#define JDWP_TAG_LONG  74
// 'J' - a long value (8 bytes).
#define JDWP_TAG_SHORT  83
// 'S' - a short value (2 bytes).
#define JDWP_TAG_VOID  86
// 'V' - a void value (no bytes).
#define JDWP_TAG_BOOLEAN  90
// 'Z' - a boolean value (1 byte).
#define JDWP_TAG_STRING  115
// 's' - a String object (objectID size).
#define JDWP_TAG_THREAD  116
// 't' - a Thread object (objectID size).
#define JDWP_TAG_THREAD_GROUP  103
// 'g' - a ThreadGroup object (objectID size).
#define JDWP_TAG_CLASS_LOADER  108
// 'l' - a ClassLoader object (objectID size).
#define JDWP_TAG_CLASS_OBJECT  99
// 'c' - a class object object (objectID size).

//=============================      cmd   ==============================================


//VirtualMachine Command Set (1)
#define JDWP_CMD_VirtualMachine_Version   0x0101
#define JDWP_CMD_VirtualMachine_ClassesBySignature   0x0102
#define JDWP_CMD_VirtualMachine_AllClasses   0x0103
#define JDWP_CMD_VirtualMachine_AllThreads   0x0104
#define JDWP_CMD_VirtualMachine_TopLevelThreadGroups   0x0105
#define JDWP_CMD_VirtualMachine_Dispose   0x0106
#define JDWP_CMD_VirtualMachine_IDSizes   0x0107
#define JDWP_CMD_VirtualMachine_Suspend   0x0108
#define JDWP_CMD_VirtualMachine_Resume   0x0109
#define JDWP_CMD_VirtualMachine_Exit   0x010a
#define JDWP_CMD_VirtualMachine_CreateString   0x010b
#define JDWP_CMD_VirtualMachine_Capabilities   0x010c
#define JDWP_CMD_VirtualMachine_ClassPaths   0x010d
#define JDWP_CMD_VirtualMachine_DisposeObjects   0x010e
#define JDWP_CMD_VirtualMachine_HoldEvents   0x010f
#define JDWP_CMD_VirtualMachine_ReleaseEvents   0x0110
#define JDWP_CMD_VirtualMachine_CapabilitiesNew   0x0111
#define JDWP_CMD_VirtualMachine_RedefineClasses   0x0112
#define JDWP_CMD_VirtualMachine_SetDefaultStratum   0x0113
#define JDWP_CMD_VirtualMachine_AllClassesWithGeneric   0x0114
//ReferenceType Command Set (2)
#define JDWP_CMD_ReferenceType_Signature   0x0201
#define JDWP_CMD_ReferenceType_ClassLoader   0x0202
#define JDWP_CMD_ReferenceType_Modifiers   0x0203
#define JDWP_CMD_ReferenceType_Fields   0x0204
#define JDWP_CMD_ReferenceType_Methods   0x0205
#define JDWP_CMD_ReferenceType_GetValues   0x0206
#define JDWP_CMD_ReferenceType_SourceFile   0x0207
#define JDWP_CMD_ReferenceType_NestedTypes   0x0208
#define JDWP_CMD_ReferenceType_Status   0x0209
#define JDWP_CMD_ReferenceType_Interfaces   0x020a
#define JDWP_CMD_ReferenceType_ClassObject   0x020b
#define JDWP_CMD_ReferenceType_SourceDebugExtension   0x020c
#define JDWP_CMD_ReferenceType_SignatureWithGeneric   0x020d
#define JDWP_CMD_ReferenceType_FieldsWithGeneric   0x020e
#define JDWP_CMD_ReferenceType_MethodsWithGeneric   0x020f
//ClassType Command Set (3)
#define JDWP_CMD_ClassType_Superclass   0x0301
#define JDWP_CMD_ClassType_SetValues   0x0302
#define JDWP_CMD_ClassType_InvokeMethod   0x0303
#define JDWP_CMD_ClassType_NewInstance   0x0304
//ArrayType Command Set (4)
#define JDWP_CMD_ArrayType_NewInstance   0x0401
//InterfaceType Command Set (5)
//Method Command Set (6)
#define JDWP_CMD_Method_LineTable   0x0601
#define JDWP_CMD_Method_VariableTable   0x0602
#define JDWP_CMD_Method_Bytecodes   0x0603
#define JDWP_CMD_Method_IsObsolete   0x0604
#define JDWP_CMD_Method_VariableTableWithGeneric   0x0605
//Field Command Set (8)
//ObjectReference Command Set (9)
#define JDWP_CMD_ObjectReference_ReferenceType   0x0901
#define JDWP_CMD_ObjectReference_GetValues   0x0902
#define JDWP_CMD_ObjectReference_SetValues   0x0903
#define JDWP_CMD_ObjectReference_MonitorInfo   0x0905
#define JDWP_CMD_ObjectReference_InvokeMethod   0x0906
#define JDWP_CMD_ObjectReference_DisableCollection   0x0907
#define JDWP_CMD_ObjectReference_EnableCollection   0x0908
#define JDWP_CMD_ObjectReference_IsCollected   0x0909
//StringReference Command Set (10)
#define JDWP_CMD_StringReference_Value   0x0a01
//ThreadReference Command Set (11)
#define JDWP_CMD_ThreadReference_Name   0x0b01
#define JDWP_CMD_ThreadReference_Suspend   0x0b02
#define JDWP_CMD_ThreadReference_Resume   0x0b03
#define JDWP_CMD_ThreadReference_Status   0x0b04
#define JDWP_CMD_ThreadReference_ThreadGroup   0x0b05
#define JDWP_CMD_ThreadReference_Frames   0x0b06
#define JDWP_CMD_ThreadReference_FrameCount   0x0b07
#define JDWP_CMD_ThreadReference_OwnedMonitors   0x0b08
#define JDWP_CMD_ThreadReference_CurrentContendedMonitor   0x0b09
#define JDWP_CMD_ThreadReference_Stop   0x0b0a
#define JDWP_CMD_ThreadReference_Interrupt   0x0b0b
#define JDWP_CMD_ThreadReference_SuspendCount   0x0b0c
//ThreadGroupReference Command Set (12)
#define JDWP_CMD_ThreadGroupReference_Name   0x0c01
#define JDWP_CMD_ThreadGroupReference_Parent   0x0c02
#define JDWP_CMD_ThreadGroupReference_Children   0x0c03
//ArrayReference Command Set (13)
#define JDWP_CMD_ArrayReference_Length   0x0d01
#define JDWP_CMD_ArrayReference_GetValues   0x0d02
#define JDWP_CMD_ArrayReference_SetValues   0x0d03
//ClassLoaderReference Command Set (14)
#define JDWP_CMD_ClassLoaderReference_VisibleClasses   0x0e01
//EventRequest Command Set (15)
#define JDWP_CMD_EventRequest_Set   0x0f01
#define JDWP_CMD_EventRequest_Clear   0x0f02
#define JDWP_CMD_EventRequest_ClearAllBreakpoints   0x0f03
//StackFrame Command Set (16)
#define JDWP_CMD_StackFrame_GetValues   0x1001
#define JDWP_CMD_StackFrame_SetValues   0x1002
#define JDWP_CMD_StackFrame_ThisObject   0x1003
#define JDWP_CMD_StackFrame_PopFrames   0x1004
//ClassObjectReference Command Set (17)
#define JDWP_CMD_ClassObjectReference_ReflectedType   0x1101
//Event Command Set (64)
#define JDWP_CMD_Event_Composite   0x4064


//=============================      string   ==============================================


//=============================      my define   ==============================================

static u16 JDWP_TCP_PORT = 8000;

static c8 *JDWP_HANDSHAKE = "JDWP-Handshake";

static c8 JDWP_EVENTSET_SET = 1;
static c8 JDWP_EVENTSET_CLEAR = 0;

static u16 JDWP_PACKET_REQUEST = 0;
static u16 JDWP_PACKET_RESPONSE = 0x80;

//=============================      typedef   ==============================================

typedef struct _JdwpPacket {
    c8 *data;
    s32 alloc;
    s32 readPos;
    s32 writePos;
    //inner receive for nonblock rceive
    s32 _rcv_len;
    s32 _req_len;
    u8 _4len;
} JdwpPacket;

enum {
    JDWP_MODE_LISTEN = 0x01,
    JDWP_MODE_DISPATCH = 0x02,
};
typedef struct _JdwpServer {
    Utf8String *ip;
    thrd_t pt_listener;
    thrd_t pt_dispacher;
    s32 srvsock;
    ArrayList *clients;
    ArrayList *events;
    Hashtable *event_sets;
    Runtime *runtime;
    u16 port;
    u8 exit;
    u8 mode;
} JdwpServer;

typedef struct _JdwpClient {
    s32 sockfd;
    u8 closed;
    u8 conn_first;
    JdwpPacket *rcvp; //用于非阻塞接收，多次接收往同一个包内写入字节
    Hashset *temp_obj_holder;
} JdwpClient;

typedef struct _JdwpConn {
    s32 sockfd;
    u8 closed;
} JdwpConn;

typedef struct _Location {
    c8 typeTag;
    __refer classID;
    __refer methodID;
    s64 execIndex;
} Location;

typedef struct _ValueType {
    c8 type;
    union {
        s64 value;
        __refer ptr;
    };
} ValueType;

typedef struct _EventSetMod {
    u8 mod_type;
    //
    Utf8String *classPattern;
    //
    __refer clazz;
    //
    s32 exprID;
    //
    s32 count;
    //
    __refer exceptionOrNull;
    c8 caught;
    c8 uncaught;
    //
    __refer declaring;
    __refer fieldID;
    //
    __refer instance;
    //
    Location loc;
    //
    Utf8String *sourceNamePattern;
    //
    __refer thread;
    s32 size;
    s32 depth;
} EventSetMod;

typedef struct _EventSet {
    s32 requestId;
    c8 eventKind;
    c8 suspendPolicy;
    c8 kindMod;
    s32 modifiers;
    EventSetMod *mods;
} EventSet;

typedef struct _EventInfo {
    s32 requestId;

    u8 eventKind;

    //VM_START
    __refer thread;

    //SINGLE_STEP
    //__refer thread;
    Location loc;

    //BREAKPOINT
    //__refer thread;
    //Location loc;
    //
    //METHOD_ENTRY
    //__refer thread;
    //Location loc;
    //
    //METHOD_EXIT
    //__refer thread;
    //Location loc;
    //
    //METHOD_EXIT_WITH_RETURN_VALUE
    //__refer thread;
    //Location loc;
    ValueType vt;

    //MONITOR_CONTENDED_ENTER
    //__refer thread;
    __refer object;
    //Location loc;

    //MONITOR_CONTENDED_ENTERED
    //__refer thread;
    //__refer object;
    //Location loc;
    //
    //MONITOR_WAIT
    //__refer thread;
    //__refer object;
    //Location loc;
    s64 timeout;

    //MONITOR_WAITED
    //__refer thread;
    //__refer object;
    //Location loc;
    c8 timed_out;

    //EXCEPTION
    //__refer thread;
    //__refer object;
    //Location loc;
    __refer exception;
    Location catchLoc;

    //THREAD_START
    //__refer thread;
    //
    //THREAD_DEATH
    //__refer thread;
    //
    //CLASS_PREPARE
    //__refer thread;
    u8 refTypeTag;
    __refer typeID;
    Utf8String *signature;
    s32 status;

    //CLASS_UNLOAD
    // Utf8String * signature;
    //
    //FIELD_ACCESS
    //__refer thread;
    //Location loc;
    //u8 refTypeTag;
    //__refer typeID;
    __refer fieldID;
    //__refer object;
    //
    //FIELD_MODIFICATION
    //__refer thread;
    //Location loc;
    //u8 refTypeTag;
    //__refer typeID;
    //__refer fieldID;
    //__refer object;
    //ValueType vt;
    //
    //VM_DEATH
} EventInfo;
enum {
    NEXT_TYPE_INTO,
    NEXT_TYPE_OUT,
    NEXT_TYPE_OVER,
    NEXT_TYPE_SINGLE,
};
typedef struct _JdwpStep {
    u8 active;
    u8 next_type;
    s32 next_stop_runtime_depth;
    union {
        s32 next_stop_bytecode_count;
        s32 next_stop_bytecode_index;
    };
    s32 bytecode_count;
} JdwpStep;


static s32 jdwp_eventset_requestid = 0;
static s32 jdwp_eventset_commandid = 0;

extern JdwpServer jdwpserver;

s32 jdwp_client_process(JdwpClient *client, Runtime *runtime);

s32 jdwp_start_server(void);

s32 jdwp_stop_server(void);

s32 jdwp_set_breakpoint(s32 setOrClear, JClass *clazz, MethodInfo *methodInfo, s64 execIndex);


void event_on_breakpoint(Runtime *breakpoint_runtime);

void event_on_class_prepar(Runtime *runtime, JClass *clazz);

void event_on_thread_death(Instance *jthread);

void event_on_thread_start(Instance *jthread);

void jdwp_check_breakpoint(Runtime *runtime);

void jdwp_check_debug_step(Runtime *runtime);

#ifdef __cplusplus
}
#endif

#endif //MINI_JVM_JDWP_H

