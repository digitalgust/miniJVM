cmake_minimum_required(VERSION 3.8)
project(jnitest)

set(CMAKE_CXX_STANDARD 11)

set(SOURCE_FILES
        jni_ex.c)

#add_executable(test1 ${SOURCE_FILES})

add_library(jnitest SHARED  ${SOURCE_FILES})



IF (APPLE)
    MESSAGE("apple os here.")
    SET(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../binary/macos)
ELSEIF (UNIX)
    MESSAGE("linux os here.")
    SET(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../binary/centos_x64)
ELSEIF (MINGW)
    IF (CMAKE_SIZEOF_VOID_P EQUAL 4)
        message("win 32bit")
        SET(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../binary/win32)
    elseif(CMAKE_SIZEOF_VOID_P EQUAL 8)
        message("win 64bit")
        SET(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../binary/win_x64)
    endif()

ENDIF ()

message(${LIBRARY_OUTPUT_PATH})

