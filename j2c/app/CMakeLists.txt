cmake_minimum_required(VERSION 3.23)
project(app C)

set(CMAKE_C_STANDARD 11)

include_directories(generted/c)
include_directories(vm)
include_directories(vm/https)
include_directories(vm/https/mbedtls/include)
include_directories(vm/https/mbedtls/include/mbedtls)

file(GLOB_RECURSE SOURCES 
    "generted/c/*.c"
    "vm/*.c"
    "vm/https/mbedtls/library/*.c"
    "platform/desktop/*.c"
)

add_executable(app ${SOURCES})


IF (MSVC)
ELSE ()
    TARGET_LINK_LIBRARIES(app pthread m)
ENDIF ()

IF (APPLE)
ELSEIF (UNIX)
    TARGET_LINK_LIBRARIES(app dl)
ELSEIF (MINGW)
    TARGET_LINK_LIBRARIES(app ws2_32)
    IF (CMAKE_SIZEOF_VOID_P EQUAL 4)
    elseif (CMAKE_SIZEOF_VOID_P EQUAL 8)
    endif ()
ENDIF ()