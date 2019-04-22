cmake_minimum_required(VERSION 3.8)
project(jni_gui)


#set(CMAKE_CXX_STANDARD 11)
set(CMAKE_C_STANDARD 99)

include_directories(deps/include/ ../../../minijvm/c/jvm)

set(SOURCE_FILES
        deps/include/glad/glad.c
        deps/include/nanovg/nanovg.c
        media.h
        jni_glfw.c
        jni_gl.c
        jni_minial.c
        media.c
        jni_nanovg.c
        )
add_executable(test_gui ${SOURCE_FILES})

add_library(gui SHARED ${SOURCE_FILES})


#find_package(OpenGL REQUIRED)
MESSAGE(STATUS "CMAKE_SYSTEM :${CMAKE_SYSTEM}")
MESSAGE(STATUS "CMAKE_SYSTEM_NAME :${CMAKE_SYSTEM_NAME}")
MESSAGE(STATUS "CMAKE_SYSTEM_PROCESSOR :${CMAKE_SYSTEM_PROCESSOR}")
MESSAGE(STATUS "WIN32 :${WIN32}")
MESSAGE(STATUS "CMAKE_GENERATOR :${CMAKE_GENERATOR}")
MESSAGE(STATUS "CMAKE_SIZEOF_VOID_P  :${CMAKE_SIZEOF_VOID_P}")
MESSAGE(STATUS "CMAKE_SOURCE_DIR  :${CMAKE_SOURCE_DIR}")


IF (APPLE)
    MESSAGE("apple os here.")
    find_library(MY_LIB libglfw3.a deps/lib/mac/)
    find_library(COCOA_LIB Cocoa)
    find_library(IOKIT_LIB IOKit)
    find_library(OPENGL_LIB OpenGL)
    find_library(COREFOUNDATION_LIB CoreFoundation)
    find_library(COREVIDEO_LIB CoreVideo)
    MARK_AS_ADVANCED(MY_LIB COCOA_LIB IOKIT_LIB OPENGL_LIB COREFOUNDATION_LIB COREVIDEO_LIB)
    SET(EXTRA_LIBS ${MY_LIB} ${COCOA_LIB} ${IOKIT_LIB} ${OPENGL_LIB} ${COREFOUNDATION_LIB} ${COREVIDEO_LIB})

    TARGET_LINK_LIBRARIES(gui ${EXTRA_LIBS})
    TARGET_LINK_LIBRARIES(test_gui ${EXTRA_LIBS})
    SET(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../../../binary/macos)
ELSEIF (UNIX)
    MESSAGE("linux os here.")
    find_library(MY_LIB libglfw3.a "${CMAKE_SOURCE_DIR}/deps/lib/centos_x64/")
    MARK_AS_ADVANCED(MY_LIB)
    TARGET_LINK_LIBRARIES(gui ${MY_LIB})
    TARGET_LINK_LIBRARIES(gui X11 Xi pthread Xcursor Xrandr GL Xinerama)
    SET(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../../../binary/centos_x64)
ELSEIF (MINGW)
    IF (CMAKE_SIZEOF_VOID_P EQUAL 4)
        message("32bit")
        find_library(MY_LIB libglfw3.a "${CMAKE_SOURCE_DIR}/deps/lib/win_i686/")
        SET(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../../../binary/win32)
    elseif (CMAKE_SIZEOF_VOID_P EQUAL 8)
        message("64bit")
        find_library(MY_LIB libglfw3.a "${CMAKE_SOURCE_DIR}/deps/lib/win_x64/")
        SET(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../../../binary/win_x64)
    endif ()
    message(${MY_LIB})
    MARK_AS_ADVANCED(MY_LIB)
    TARGET_LINK_LIBRARIES(gui ${MY_LIB})
    TARGET_LINK_LIBRARIES(gui opengl32)
    TARGET_LINK_LIBRARIES(test_gui opengl32)

    TARGET_LINK_LIBRARIES(test_gui ${MY_LIB})
    set(CMAKE_EXE_LINKER_FLAGS "-mwindows")
ENDIF ()

message(${LIBRARY_OUTPUT_PATH})


