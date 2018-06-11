# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

project(androidapp C)

set(CMAKE_C_STANDARD 99)

include_directories(../../c/glfm ../../c/gui/ ../../c/gui/stb ../../c/gui/other ../../../mini_jvm/jvm)

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
	../../c/gui/jni_gui.h
	../../c/gui/jni_nanovg_assist.h
	../../c/gui/jni_stb_assist.h
	../../c/gui/jni_nanovg.c
	../../c/gui/jni_gui.c
	../../c/gui/jni_glfm.c
	../../c/gui/jni_gl.c
)

set(MINI_JVM_SOURCE_FILES
        ../../../mini_jvm/utils/ltalloc.h
        ../../../mini_jvm/utils/ltalloc.c
        ../../../mini_jvm/utils/d_type.h
        ../../../mini_jvm/utils/d_utils.c
        ../../../mini_jvm/utils/arraylist.c
        ../../../mini_jvm/utils/arraylist.h
        ../../../mini_jvm/utils/hashtable.c
        ../../../mini_jvm/utils/hashtable.h
        ../../../mini_jvm/utils/utf8_string.c
        ../../../mini_jvm/utils/utf8_string.h
        ../../../mini_jvm/utils/pairlist.h
        ../../../mini_jvm/utils/pairlist.c
        ../../../mini_jvm/utils/hashset.c
        ../../../mini_jvm/utils/hashset.h
        ../../../mini_jvm/jvm/jvm.h
        ../../../mini_jvm/jvm/interpreter.c
        ../../../mini_jvm/jvm/jvm.c
        ../../../mini_jvm/jvm/class.c
        ../../../mini_jvm/jvm/runtime.c
        ../../../mini_jvm/jvm/garbage.c
        ../../../mini_jvm/jvm/jvm_util.c
        ../../../mini_jvm/jvm/jvm_util.h
        ../../../mini_jvm/jvm/global.c
        ../../../mini_jvm/jvm/java_native_std.h
        ../../../mini_jvm/jvm/java_native_io.c
        ../../../mini_jvm/jvm/java_native_std.c
        ../../../mini_jvm/jvm/java_native_reflect.c
        ../../../mini_jvm/jvm/java_native_reflect.h
        ../../../mini_jvm/jvm/jdwp.c
        ../../../mini_jvm/jvm/jdwp.h
        ../../../mini_jvm/jvm/java_native_io.h
        ../../../mini_jvm/jvm/class_load.c
        ../../../mini_jvm/jvm/garbage.h
        ../../../mini_jvm/utils/tinycthread.h
        ../../../mini_jvm/utils/tinycthread.c
        ../../../mini_jvm/utils/linkedlist.c
        ../../../mini_jvm/utils/linkedlist.h
        ../../../mini_jvm/utils/spinlock.c
        ../../../mini_jvm/utils/spinlock.h
        ../../../mini_jvm/utils/bytebuf.c
        ../../../mini_jvm/utils/bytebuf.h
        ../../../mini_jvm/utils/miniz/miniz_wrapper.c
        ../../../mini_jvm/utils/miniz/miniz_wrapper.h
        ../../../mini_jvm/utils/dirent_win.h)


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
    target_link_libraries(minijvm ${log-lib} ${android-lib} ${EGL-lib} ${GLESv2-lib})
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