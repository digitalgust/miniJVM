cmake_minimum_required(VERSION 3.6)
project(mini_jvm C)

#enable_language(CXX)
enable_language(C)

#set(CMAKE_CXX_STANDARD 11)
#set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD 99)

#option(MINI_JVM_BUILD_LIB "Build the mini_jvm library" OFF)

if (CMAKE_BUILD_TYPE MATCHES "Debug")
    ADD_DEFINITIONS(-D_DEBUG -D__JVM_DEBUG__)
    message("this is a debug compile")
endif ()


include_directories(jvm/ utils/ utils/sljit/)

set(SOURCE_FILES
        utils/d_type.h
        utils/d_type.c
        utils/arraylist.c
        utils/arraylist.h
        utils/hashtable.c
        utils/hashtable.h
        utils/utf8_string.c
        utils/utf8_string.h
        utils/pairlist.h
        utils/hashset.c
        utils/hashset.h
        jvm/jvm.h
        jvm/interpreter.c
        jvm/jvm.c
        jvm/class.c
        jvm/runtime.c
        jvm/garbage.c
        jvm/jvm_util.c
        jvm/jvm_util.h
        jvm/global.c
        jvm/java_native_std.h
        jvm/java_native_io.c
        jvm/java_native_std.c
        jvm/java_native_reflect.c
        jvm/java_native_reflect.h
        jvm/jdwp.c
        jvm/jdwp.h
        jvm/java_native_io.h
        jvm/class_load.c
        jvm/garbage.h
        jvm/jit.c
        jvm/jit.h
        utils/tinycthread.h
        utils/tinycthread.c
        utils/linkedlist.c
        utils/linkedlist.h
        utils/spinlock.h
        utils/bytebuf.c
        utils/bytebuf.h
        utils/miniz_wrapper.c
        utils/miniz_wrapper.h
        utils/dirent_win.h
        utils/ltalloc.c
        utils/ltalloc.h
        utils/miniz.c
        utils/miniz.h

        utils/sljit/sljitLir.c
        )


MESSAGE(STATUS "platform: ${CMAKE_SYSTEM_NAME}")


#if (MINI_JVM_BUILD_LIB)
add_library(mini_jvm_lib SHARED ${SOURCE_FILES})
TARGET_LINK_LIBRARIES(mini_jvm_lib pthread m)
#Set link library.
IF (APPLE)
ELSEIF (UNIX)
    TARGET_LINK_LIBRARIES(mini_jvm_lib dl)
ELSEIF (MINGW)
    TARGET_LINK_LIBRARIES(mini_jvm_lib ws2_32)
    IF (CMAKE_SIZEOF_VOID_P EQUAL 4)
    elseif (CMAKE_SIZEOF_VOID_P EQUAL 8)
    endif ()
ENDIF ()
#else ()

add_executable(mini_jvm ${SOURCE_FILES} main.c)
TARGET_LINK_LIBRARIES(mini_jvm pthread m)
#Set link library.
IF (APPLE)
    SET(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../../binary/macos)
ELSEIF (UNIX)
    TARGET_LINK_LIBRARIES(mini_jvm dl)
    SET(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../../binary/centos_x64)
ELSEIF (MINGW)
    TARGET_LINK_LIBRARIES(mini_jvm ws2_32)
    IF (CMAKE_SIZEOF_VOID_P EQUAL 4)
        SET(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../../binary/win32)
    elseif (CMAKE_SIZEOF_VOID_P EQUAL 8)
        SET(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../../binary/win_x64)
    endif ()
ENDIF ()

#endif ()


function(copyfile pathsrc pathtgt)
    if (EXISTS "${pathsrc}")
        #message("copy src:${pathsrc}   to :${pathtgt}")
        configure_file(${pathsrc} ${pathtgt} COPYONLY)
    endif ()
endfunction()

copyfile("${PROJECT_SOURCE_DIR}/../../javalib/dist/minijvm_rt.jar" "${PROJECT_SOURCE_DIR}/../../binary/lib/minijvm_rt.jar")
copyfile("${PROJECT_SOURCE_DIR}/../../javalib/dist/minijvm_rt.jar" "${PROJECT_SOURCE_DIR}/../../mobile/assets/resfiles/minijvm_rt.jar")
copyfile("${PROJECT_SOURCE_DIR}/../../ex_lib/jni_gui/java/dist/glfw_gui.jar" "${PROJECT_SOURCE_DIR}/../../binary/lib/glfw_gui.jar")
copyfile("${PROJECT_SOURCE_DIR}/../../ex_lib/luaj/dist/luaj.jar" "${PROJECT_SOURCE_DIR}/../../binary/lib/luaj.jar")
copyfile("${PROJECT_SOURCE_DIR}/../../test/javalib_test/dist/minijvm_test.jar" "${PROJECT_SOURCE_DIR}/../../binary/lib/minijvm_test.jar")
#copyfile("${PROJECT_SOURCE_DIR}/../../../../javaSrc/g3d/dist/g3d.jar" "${PROJECT_SOURCE_DIR}/../../binary/lib/g3d.jar")




