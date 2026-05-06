![](/doc/img/mini_jvm_64.png)
<img src="https://github.com/digitalgust/miniJVM/actions/workflows/github-actions-demo.yml/badge.svg"/>
<a href="https://discord.gg/Gy7wqkdTzz"><img src="https://img.shields.io/discord/1501437740968054875?label=discord&logo=discord&style=flat-square" alt="discord"></a>
<span id="linkhome"/>

# miniJVM

Build iOS and Android apps in Java with a cross-platform, lightweight Java Virtual Machine. Run the same Java code on mobile and desktop with small binaries and a low memory footprint.

[Features](#linkfeatures)
[Architecture](#linkarch)
[Build for iOS/Android](#linkbuildmobile)
[Build for Windows/Linux/macOS](#linkbuilddesktop)
[How to debug source](#linkdebug)
[Using miniJVM in a project](#linkembed)
[Documentation](https://github.com/digitalgust/miniJVM/tree/master/doc/api.md)
[License](#linklicense)

<span id="linkfeatures"/>

## Features

* Builds on iOS, Android, mingw-w64 (32/64-bit), MSVC (32/64-bit), macOS, and Linux
* No external dependencies
* Minimal memory footprint
* Small binaries and an embeddable JVM
* Minimal bootstrap class library (not a full JDK 8 implementation)
* Supports Java source compilation (Janino)
* JIT support
* Low-latency Java garbage collection
* Remote debugging via JDWP
* Option to translate miniJVM classes to C for faster execution

<span id="linkmobilefeatures"/>

## iOS/Android Platform Extended Features

* OpenGL ES 3.0
* Swing-like GUI library with HTML-style XML layout
* Audio/video playback and capture
* Photo capture from camera or album
* Save and load files on the device
* Desktop-compatible API so apps can run on desktop as well

## miniJVM on the Web

miniJVM on the web, built by Starcommander. [Source](https://github.com/Starcommander/miniJVM)
[Web demo](https://java-on-web.org/examples/)

<span id="linkdemo"/>

## miniJVM GUI Demos

<div align=center><img width="112" height="199" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/demo.gif"/></div>

<div align=center><img width="400" height="240" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/game.gif"/></div>

* Instant messaging app demo - [Source](https://github.com/digitalgust/BiBiX)
* 3D game app demo - [Source](https://github.com/digitalgust/g3d)

<span id="linkarch"/>

## Architecture

<div align=center><img width="540" height="350" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/arch.png"/></div>

<span id="linkchangelog"/>

## Changelog

2026.03 Added a profiling performance monitoring page to observe real-time execution time for each JVM method call.
2023.09 Reworked GLFW and GLFM graphics JNI bindings; moved GUI Java classes to the `xgui` package.
2022.11 Added documentation.
2021.03 Added the `j2c` module, which converts miniJVM Java source to C and builds native apps for desktop and mobile.
2020.12 Added build scripts and released v2.1.
2020.10 Refactored source and removed binaries from the repository.
2020.10 Added HTTPS support.
2020.03 Added XML layout for the GUI system; fixed JDWP debugging for JetBrains IDEs.
2019.12 Integrated the cross-platform AWTK UI system - see [awtk-minijvm](https://github.com/digitalgust/miniJVM/tree/master/desktop/awtk_gui).
2019.12 Enabled JIT based on the SLJIT project.
2019.10 JIT in development.
2018.12 Optimized performance.
2017.09 Project started.

<span id="linkbuildmobile"/>

## Build for iOS/Android

Write Java code once and run it on iOS, Android, macOS, Windows, and Linux.
The required JARs are not prebuilt, so build them first.
Preferred IDEs: Eclipse, NetBeans, or JetBrains IntelliJ IDEA.

1. Run **/binary/build_jar.sh** or **/binary/build_jar.bat** to generate JARs.
   Alternatively:

   > Build the Maven project in `/minijvm/java`, then copy the output to **/mobile/assets/resfiles/minijvm_rt.jar**
   > Build the Maven project in `/mobile/java/glfm_gui`, then copy the output to **/mobile/assets/resfiles/glfm_gui.jar**
   > Build the Maven project in `/mobile/java/ExApp`, then copy the output to **/mobile/assets/resfiles/ExApp.jar**
   > Optionally modify **/mobile/java/ExApp/src/main/java/test/MyApp.java**; add resources to **/mobile/java/ExApp/src/main/resource/res/** (audio, images, etc.); configure **/mobile/java/ExApp/src/main/config.txt** for icon, version, boot class, and other app settings.

2. In Xcode, open **/mobile/iosapp**; configure your developer account in Signing & Capabilities; build and install to a device; then verify the app before running it (Settings -> General -> Device Management -> {developer account} -> Verify App -> Trust).
3. In Android Studio, open **/mobile/androidapp** and build/install to an Android device.
4. AppManager starts and runs:

<div align=center><img width="340" height="250" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/appmgr.png"/></div>

<span id="linkbuilddesktop"/>

## Build for Windows/Linux/macOS

1. Run **/binary/build_jar.sh** or **/binary/build_jar.bat** to generate JARs.
   Alternatively:
   > Build Java bootstrap classes in **/minijvm/java**; build the JAR with Maven and copy it to `/binary/lib/minijvm_rt.jar`
   > Build GUI classes in **/desktop/glfw_gui/java**; build the JAR with Maven and copy it to `/binary/libex/glfw_gui.jar`
   > Build console test classes in **/test/minijvm_test**; build the JAR with Maven and copy it to `/binary/libex/minijvm_test.jar`
   > Build GUI test app classes in **/mobile/java/ExApp**; build the JAR with Maven and copy it to `/binary/{platform}/apps/ExApp.jar`

2. Run **/binary/build_mac_linux.sh**, **/binary/build_wini686.bat**, or **/binary/build_winx64.bat** to generate binaries.
   Alternatively:
   > Build the GUI JNI C dynamic library in `/desktop/glfw_gui/c` with CMake
   > Build miniJVM in `/minijvm/c` with CMake

3. Run the test script: `/binary/{platform}/test.sh` or `test.bat`.

<span id="linkdebug"/>

## How to Debug Source

Most Java IDEs support remote debugging. The IDE connects to miniJVM's debug port over TCP and starts a debugging session. miniJVM provides two options: enable debugging, and suspend on startup (wait for the IDE to attach before executing bytecode). Configure both options before initializing the VM.
These options are defined in `jvm.h`:

```
struct _MiniJVM {
    ...
    s32 jdwp_enable; // 0: disable Java debug, 1: enable Java debug and disable JIT
    s32 jdwp_suspend_on_start;
    ...
};
```

1. Enable miniJVM debug mode.
   **Desktop**: Run `mini_jvm` with `-Xdebug`, add parameter if needed `-Xjdwp:transport=dt_socket,server=y,suspend=n,address=5005`, or set it in source before building.
   `/minijvm/c/main.c`: `jvm->jdwp_enable = 1;`
   **iOS/Android**: set it in source before building.
   `/mobile/c/glfmapp/main.c`: `jvm->jdwp_enable = 1;`
   For device debugging, check the device IP address in Settings -> Wi-Fi -> (i).

2. Run the VM with JARs, for example:
   `mini_jvm.exe -Xdebug -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/glfw_gui.jar org.mini.glfw.GlfwMain`
3. Open a project in your IDE, for example: `/mobile/java/ExApp`
   **IntelliJ IDEA**: Run -> Edit Configurations -> + Remote; Transport: Socket; Debugger mode: Attach; host/port of your `mini_jvm`, for example `127.0.0.1:5005` or `192.168.0.32:5005`.
   **Eclipse**: configure similarly.
   **NetBeans**: Debug -> Attach Debugger; Connector: SocketAttach; host/port of your `mini_jvm`, for example `localhost:5005`; Timeout: `10000`.
4. Set breakpoints, pause the VM, and inspect variables.

## Profiling

Use the built-in profiling page to monitor method execution cost in real time.

1. Enable profiling in `jvm.h`:

   ```c
   #define _JVM_DEBUG_METHOD_PROFILE 1
   ```

2. Rebuild miniJVM.
3. Run miniJVM, then open `Settings -> Developer Options -> Enable LAN Web Server`.
4. Your browser opens automatically at [http://localhost:18088/](http://localhost:18088/).
5. On this web page:
   - You can upload plugin apps.
   - You can click **VM infomation** (VM Information) to open the profiling page.

### Profiling Page Features

- View the top 200 methods by execution time.
- Click a method to set a call-time threshold. If a call exceeds this threshold, miniJVM saves a call snapshot.
- Open **SlowCallTree** to inspect saved snapshots and expand the call stack.
- Dump the current heap memory, download the dump file, and open it in VisualVM to analyze memory usage.
- View runtime VM information, including garbage collector activity; data refreshes every 5 seconds.

<div align=center><img width="540" height="350" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/profile.png"/></div>

## Projects Based on miniJVM

[Awtk-minijvm](https://github.com/zlgopen/awtk-minijvm): [AWTK](https://github.com/zlgopen/awtk) cross-platform native UI bound to miniJVM.
[LWJGUI-Mobile](https://github.com/orange451/LWJGUI-Mobile): Java LWJGL UI library.
[BiBiX](https://github.com/digitalgust/BiBiX): Java instant messenger.
[miniJVM web demo](https://java-on-web.org/examples/): miniJVM web demo.

<span id="linkref"/>

## Referenced Projects and Technologies

[Oracle CLDC](http://www.oracle.com/technetwork/java/cldc-141990.html): CLDC API reference.
[OpenJDK](https://openjdk.java.net/): Java API reference.
[Miniz](https://github.com/richgel999/miniz): Reads JAR files.
[Glfm](https://github.com/brackeen/glfm): Cross-platform (Android/iOS) GUI.
[Nanovg](https://github.com/memononen/nanovg): GUI rendering.
[Stb](https://github.com/nothings/stb): GUI TrueType fonts and images.
[Glad](https://github.com/Dav1dde/glad): OpenGL/GLES header replacement.
[Glfw](https://github.com/glfw/glfw): Cross-platform desktop GUI.
[Dirent](https://github.com/tronkko/dirent): Linux-style file and directory APIs on Windows VC.
[Tinycthread](https://github.com/tinycthread/tinycthread): Cross-platform threads.
[JRegex](https://github.com/digitalgust/minijvm_third_lib/tree/master/jregex): Java string regex matching.
[Janino](http://janino-compiler.github.io/janino/): Compiles Java source files.
[MiniAudio](https://github.com/dr-soft/miniaudio): Java audio playback and capture.
[Sljit](https://github.com/zherczeg/sljit): Platform-independent low-level JIT compiler.
[Mbedtls](https://github.com/ARMmbed/mbedtls): HTTPS support via mbedtls.
[Avian](https://github.com/ReadyTalk/avian): Java API reference.

## Recommended Development IDEs

C / ObjC: JetBrains CLion, Xcode, Visual Studio
Swift: Xcode
Java: JetBrains IntelliJ IDEA, NetBeans
Android: Android Studio

<span id="linkembed"/>

## Using miniJVM in a Project

Copy C sources from **/minijvm/c** into your project, and copy the built `minijvm_rt.jar` into your project's resource folder.

```
#include "jvm/jvm.h"

int main(int argc, char **argv) {
    char *bootclasspath = "../../binary/lib/minijvm_rt.jar";
    char *classpath = "../../binary/libex/minijvm_test.jar;./";
    char *main_name = "test.Foo3";

    s32 ret = 1;
    MiniJVM *jvm = jvm_create();
    if (jvm != NULL) {
        jvm->jdwp_enable = 0; // set to 1 to enable Java remote debugging
        jvm->jdwp_suspend_on_start = 0;
        jvm->max_heap_size = 25 * 1024 * 1024;

        ret = jvm_init(jvm, bootclasspath, classpath);
        if (ret) {
            printf("[ERROR] minijvm init error.\n");
        } else {
            ret = call_main(jvm, main_name, NULL);
        }
        jvm_destroy(jvm);
    }
    return ret;
}
```

## Third-Party Libraries

### Janino Java Compiler

Project: [Janino](http://janino-compiler.github.io/janino/)
Janino is a very small and very fast Java compiler.
Janino can compile source files to class files like `javac`, and it can also compile a Java expression, a block, a class body, one `.java` file, or multiple `.java` files in memory, then load and execute the generated bytecode directly in the same JVM. Janino is not a full Java compiler; see [limitations](http://janino-compiler.github.io/janino/#limitations), for example:

```
List<String> list = new ArrayList();
list.add("abc");
String s = (String) list.get(0); // cannot omit the (String) cast.
```

Download JARs:
[janino.jar](https://github.com/digitalgust/digitalgust.github.io/blob/main/lib/janino.jar?raw=true)
[commons-compiler.jar](https://github.com/digitalgust/digitalgust.github.io/blob/main/lib/commons-compiler.jar?raw=true)

```
# compile /binary/res/BpDeepTest.java
mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/janino.jar:../libex/commons-compiler.jar org.codehaus.janino.Compiler ../res/BpDeepTest.java
```

### LuaJ

Project: [LuaJ](https://github.com/luaj/luaj)
miniJVM adaptation: [Luaj minijvm](https://github.com/digitalgust/minijvm_third_lib)
LuaJ is a lightweight, fast, Java-centric Lua interpreter for JME and JSE. It includes string, table, package, math, io, os, debug, coroutine, and luajava libraries, plus JSR-223 bindings, full metatag support, weak tables, and direct Lua-to-Java bytecode compilation.
Download JAR:
[luaj.jar](https://github.com/digitalgust/digitalgust.github.io/blob/main/lib/luaj.jar?raw=true)

```
mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/luaj.jar Sample
```

## Screenshots

<table>
<tr>
<td>Windows mini_jvm GUI
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/win.png"/></div>

</td>
<td>macOS mini_jvm GUI
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/mac.png"/></div>

</td>
<td>Linux mini_jvm GUI
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/centos.png"/></div>

</td>
<td>Web mini_jvm GUI
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/web-glfw.png"/></div>
</td>
</tr>
</table>

<span id="linkdir"/>

## Directory Structure

```
.
├── binary              miniJVM binaries for builds (win32/win64/mac/linux)
├── desktop
│   ├── awtk_gui        open-source AWTK GUI JNI bindings
│   └── glfw_gui        desktop OpenGL, GLFW, miniaudio, native module
├── j2c                 tools to translate miniJVM Java source to C
│   ├── app
│   └── translator
├── minijvm             core source
│   ├── c               miniJVM C source
│   └── java            miniJVM runtime library
├── mobile
│   ├── androidapp      Android launcher
│   ├── assets          app resources (fonts, JARs, images, audio, etc.)
│   ├── c               mobile native libs: OpenGLES, GLFM framework, GUI JNI, glfmapp
│   ├── iosapp          iOS launcher
│   └── java            mobile Java libs, GUI, AppManager, example app
├── doc
└── test                miniJVM test cases
```

[< Back](#linkhome)

<a href="https://github.com/digitalgust/miniJVM/stargazers">
        <img width="500" alt="Star History Chart" src="https://api.star-history.com/svg?repos=digitalgust/miniJVM&type=Date">
</a>

<span id="linklicense"/>

## License

License: MIT

Gust, digitalgust@163.com
