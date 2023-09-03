cmake_minimum_required(VERSION 3.6)
project(mini_jvm C)

#enable_language(CXX)
enable_language(C)

#set(CMAKE_CXX_STANDARD 11)
#set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD 99)

#option(MINI_JVM_BUILD_LIB "Build the mini_jvm library" OFF)

IF (MSVC)
    add_compile_options("/utf-8")
ENDIF ()

if (CMAKE_BUILD_TYPE MATCHES "Debug")
    ADD_DEFINITIONS(-D_DEBUG -D__JVM_DEBUG__)
    message("this is a debug compile")
endif ()


include_directories(jvm/ utils/ utils/sljit/ utils/https/ utils/https/mbedtls/include)

aux_source_directory(./jvm SOURCE_JVM)
aux_source_directory(./utils SOURCE_UTILS)
aux_source_directory(./utils/https SOURCE_HTTPS)
aux_source_directory(./utils/https/mbedtls/library SOURCE_TLS)
set(SOURCE_SLJIT ./utils/sljit/sljitLir.c)

SET(SOURCE_FILES ${SOURCE_JVM} ${SOURCE_UTILS} ${SOURCE_HTTPS} ${SOURCE_TLS} ${SOURCE_SLJIT})

MESSAGE(STATUS "platform: ${CMAKE_SYSTEM_NAME}")


#if (MINI_JVM_BUILD_LIB)
add_library(mini_jvm_lib SHARED ${SOURCE_FILES})

IF (MSVC)
ELSE ()
TARGET_LINK_LIBRARIES(mini_jvm_lib pthread m)
ENDIF ()

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

IF (MSVC)
ELSE ()
TARGET_LINK_LIBRARIES(mini_jvm pthread m)
ENDIF ()

#Set link library.
IF (APPLE)
    SET(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../../binary/mac_x64)
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




