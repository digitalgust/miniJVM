# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

project(androidapp C)

set(CMAKE_C_STANDARD 99)

include_directories(../../c/glfm ../../c/gui/ ../../c/gui/stb ../../c/gui/other ../../../minijvm/c/jvm ../../../minijvm/c/utils ../../../minijvm/c/utils/sljit)

set(GLFMAPP_SOURCE_FILES
../../c/glfmapp/main.c
)
set(GUI_SOURCE_FILES
	../../c/gui/stb/stb_image_write.h
	../../c/gui/stb/stb_image.h
	../../c/gui/stb/stb_truetype.h
	../../c/gui/nanovg/fontstash.h
	../../c/gui/nanovg/nanovg_gl.h
	../../c/gui/nanovg/nanovg_gl_utils.h
	../../c/gui/nanovg/nanovg.h
	../../c/gui/nanovg/nanovg.c
    ../../c/gui/glad/glad.c
	../../c/gui/glad/glad.h
	../../c/gui/KHR/khrplatform.h
	../../c/gui/other/linmath.h
	../../c/gui/media.h
	../../c/gui/jni_nanovg_assist.h
	../../c/gui/jni_stb_assist.h
	../../c/gui/jni_nanovg.c
	../../c/gui/jni_minial.m
	../../c/gui/media.c
	../../c/gui/jni_glfm.c
	../../c/gui/jni_gl.c
	../../c/glfm/glfm.h
)

set(MINI_JVM_SOURCE_FILES
        ../../../minijvm/c/utils/ltalloc.h
        ../../../minijvm/c/utils/ltalloc.c
        ../../../minijvm/c/utils/d_type.h
        ../../../minijvm/c/utils/d_type.c
        ../../../minijvm/c/utils/arraylist.c
        ../../../minijvm/c/utils/arraylist.h
        ../../../minijvm/c/utils/hashtable.c
        ../../../minijvm/c/utils/hashtable.h
        ../../../minijvm/c/utils/utf8_string.c
        ../../../minijvm/c/utils/utf8_string.h
        ../../../minijvm/c/utils/pairlist.h
        ../../../minijvm/c/utils/hashset.c
        ../../../minijvm/c/utils/hashset.h
        ../../../minijvm/c/jvm/jvm.h
        ../../../minijvm/c/jvm/interpreter.c
        ../../../minijvm/c/jvm/jvm.c
        ../../../minijvm/c/jvm/class.c
        ../../../minijvm/c/jvm/runtime.c
        ../../../minijvm/c/jvm/garbage.c
        ../../../minijvm/c/jvm/jvm_util.c
        ../../../minijvm/c/jvm/jvm_util.h
        ../../../minijvm/c/jvm/global.c
        ../../../minijvm/c/jvm/java_native_std.h
        ../../../minijvm/c/jvm/java_native_io.c
        ../../../minijvm/c/jvm/java_native_std.c
        ../../../minijvm/c/jvm/java_native_reflect.c
        ../../../minijvm/c/jvm/java_native_reflect.h
        ../../../minijvm/c/jvm/jdwp.c
        ../../../minijvm/c/jvm/jdwp.h
        ../../../minijvm/c/jvm/java_native_io.h
        ../../../minijvm/c/jvm/class_load.c
        ../../../minijvm/c/jvm/garbage.h
        ../../../minijvm/c/jvm/jit.h
        ../../../minijvm/c/jvm/jit.c
        ../../../minijvm/c/utils/tinycthread.h
        ../../../minijvm/c/utils/tinycthread.c
        ../../../minijvm/c/utils/linkedlist.c
        ../../../minijvm/c/utils/linkedlist.h
        ../../../minijvm/c/utils/spinlock.h
        ../../../minijvm/c/utils/bytebuf.c
        ../../../minijvm/c/utils/bytebuf.h
	    ../../../minijvm/c/utils/miniz.c
        ../../../minijvm/c/utils/miniz.h
        ../../../minijvm/c/utils/miniz_wrapper.c
        ../../../minijvm/c/utils/miniz_wrapper.h
        ../../../minijvm/c/utils/dirent_win.h

        ../../../minijvm/c/utils/sljit/sljitLir.c
        ../../../minijvm/c/utils/sljit/sljitLir.h
        ../../../minijvm/c/utils/sljit/sljitConfig.h

        )


if (CMAKE_SYSTEM_NAME MATCHES "Emscripten")
    set(GLFM_SOURCE_FILES ../../c/glfm/glfm_platform.h ../../c/glfm/glfm_platform_emscripten.c)
elseif (CMAKE_SYSTEM_NAME MATCHES "Android")
    set(GLFM_SOURCE_FILES ../../c/glfm/glfm.h ../../c/glfm/glfm_platform.h ../../c/glfm/glfm_platform_android.c ${ANDROID_NDK}/sources/android/native_app_glue/android_native_app_glue.c)
    # Set NDEBUG for android_native_app_glue to remove some superfluous logging
    set_source_files_properties(${ANDROID_NDK}/sources/android/native_app_glue/android_native_app_glue.c PROPERTIES COMPILE_FLAGS "-DNDEBUG")
else() # Assume iOS
    set(IOS TRUE)
    set(GLFM_SOURCE_FILES src/glfm_platform.h src/glfm_platform_ios.m)
    set(CMAKE_OSX_SYSROOT "iphoneos")
    set(GLFM_COMPILE_FLAGS "-Wno-objc-interface-ivars -Wno-objc-missing-property-synthesis -Wno-direct-ivar-access")
endif()

add_library(minijvm  SHARED  ${GUI_SOURCE_FILES} ${MINI_JVM_SOURCE_FILES} ${GLFM_SOURCE_FILES} ${GLFMAPP_SOURCE_FILES})
target_include_directories(minijvm PUBLIC gui)
target_include_directories(minijvm PRIVATE mini_jvm)
target_include_directories(minijvm PRIVATE glfm)
target_include_directories(minijvm PRIVATE glfmapp)

source_group(gui FILES ${GUI_SOURCE_FILES})
source_group(mini_jvm FILES ${MINI_JVM_SOURCE_FILES})
source_group(glfm FILES ${GLFM_SOURCE_FILES})
source_group(glfmapp FILES ${GLFMAPP_SOURCE_FILES})



if (CMAKE_C_COMPILER_ID MATCHES "Clang")
    set_target_properties(minijvm PROPERTIES COMPILE_FLAGS "-Weverything -Wwrite-strings -Wno-padded -Wno-covered-switch-default ${GLFM_COMPILE_FLAGS}")
elseif (CMAKE_C_COMPILER_ID MATCHES "GNU")
    set_target_properties(minijvm PROPERTIES COMPILE_FLAGS "-Wall -Wextra -Wwrite-strings ${GLFM_COMPILE_FLAGS}")
elseif (CMAKE_C_COMPILER_ID MATCHES "MSVC")
    set_target_properties(minijvm PROPERTIES COMPILE_FLAGS "/Wall ${GLFM_COMPILE_FLAGS}")
endif()

if (CMAKE_SYSTEM_NAME MATCHES "Android")
    find_library(log-lib log)
    find_library(android-lib android)
    find_library(EGL-lib EGL)
    find_library(GLESv2-lib GLESv2)
    find_library(SLES-lib OpenSLES)
    target_link_libraries(minijvm ${log-lib} ${android-lib} ${EGL-lib} ${GLESv2-lib} ${SLES-lib})
    target_include_directories(minijvm PRIVATE ${ANDROID_NDK}/sources/android/native_app_glue/)
    set_target_properties(minijvm PROPERTIES  C_STANDARD 99)
elseif (IOS)
    target_link_libraries(minijvm "-framework Foundation -framework CoreGraphics -framework UIKit -framework OpenGLES -framework QuartzCore")
    set_target_properties(minijvm PROPERTIES
        ARCHIVE_OUTPUT_DIRECTORY ${PROJECT_BINARY_DIR}/GLFM.build/lib # For Archiving
        XCODE_ATTRIBUTE_SUPPORTED_PLATFORMS "iphoneos iphonesimulator appletvos appletvsimulator"
        XCODE_ATTRIBUTE_IPHONEOS_DEPLOYMENT_TARGET 8.0
        XCODE_ATTRIBUTE_TVOS_DEPLOYMENT_TARGET 9.0
        XCODE_ATTRIBUTE_CLANG_ENABLE_OBJC_ARC YES
    )
endif()

set(MINIJVM_APP_ORGANIZATION_IDENTIFIER "com.eglsgame")
set(MINIJVM_APP_VERSION "1.0")
set(MINIJVM_APP_VERSION_ITERATION 1)
set(MINIJVM_APP_ASSETS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/assets)
set_property(TARGET minijvm PROPERTY C_STANDARD 99)


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#add_library( # Sets the name of the library.
#             native-lib
#
#             # Sets the library as a shared library.
#             SHARED
#
#             # Provides a relative path to your source file(s).
#             src/main/cpp/native-lib.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

#target_link_libraries( # Specifies the target library.
#                       native-lib
#
#                       # Links the target library to the log library
#                       # included in the NDK.
#                       ${log-lib} )
