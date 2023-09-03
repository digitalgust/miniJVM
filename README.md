![](/doc/img/mini_jvm_64.png)  
[![Build Status](https://travis-ci.org/digitalgust/miniJVM.svg?branch=master)](https://travis-ci.org/digitalgust/miniJVM)

<span id="linkhome"/>   

# miniJVM

Develop iOS Android app in java, Cross platform java virtual machine , the minimal jvm .

[Features](#linkfeatures)    
[Architecture](#linkarch)    
[Build for iOS/Android platform](#linkbuildmobile)     
[Build for Windows/Linux/MacOS platform](#linkbuilddesktop)     
[How to debug source](#linkdebug)    
[Using miniJVM in project](#linkembed)    
[Documentation](https://github.com/digitalgust/miniJVM/tree/master/doc/api.md)    
[License](#linklicense)    

<span id="linkfeatures"/>   

## Features

* Jvm Build pass: iOS / Android / mingw-w64 32|64bit / MSVC 32|64bit / MacOS / Linux
* No dependence Library
* Minimal memory footprint
* Minimal binary, embedded jvm
* Minimal bootstrap classlib
* Support java source compiler(janino compiler)
* Jit support
* Low latency java garbage collection
* Java remote debug supported, JDWP Spec
* Translate minijvm classes to C source code to improve execute speed.

<span id="linkmobilefeatures"/>

## iOS/Android Platform Extended Features

* OpenGL ES 3.0
* Swing like gui lib, HTML like XML layout
* Audio/Video Playback and Capture
* Take photo from Camera or Album
* Save and Load file from mobile device
* Api compatible with miniJVM desktop platform, app can running on desktop platform

## MiniJVM on Web

MiniJVM on web build by Starcommander. [Source](https://github.com/Starcommander/miniJVM)
[Web demo](https://java-on-web.org/examples/)

<span id="linkdemo"/>

## MiniJVM gui Demo

  <div align=center><img width="112" height="199" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/demo.gif"/><img width="112" height="199" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/g3d.gif"/></div>

* Instant Message app Demo , [Source](https://github.com/digitalgust/BiBiX)
* 3D game app Demo, [Source](https://github.com/digitalgust/g3d)

<span id="linkarch"/>

## Architecture:

  <div align=center><img width="540" height="350" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/arch.png"/></div>

<span id="linkchangelog"/>

## Changelog:

2023.09. Rebound the glfw and glfm graphics jni and separated the GUI java class into the xgui package .    
2022.11. Add documentation.    
2021.03. Add j2c module, it 's a tool for convert minijvm java source code to c source code , then build it as native application , support desktop and mobile platform .   
2020.12. Add build script and release v2.1.   
2020.10. Refactor source and remove binary in repository.   
2020.10. Https supported.   
2020.03. Add xml layout for gui system, add 3D game demo for minijvm, fix jdwp debug for jetbrain idea.               
2019.12. Bind cross-platform awtk ui system , see [awtk-minijvm](https://github.com/digitalgust/miniJVM/tree/master/desktop/awtk_gui)   
2019.12. Jit enabled, it based on sljit project   
2019.10. Jit is developing   
2018.12. Optimize performance     
2017.09. miniJVM start

<span id="linkbuildmobile"/>

## Build for iOS/Android platform:

Write java code once , running on all of iOS / Android / MacOSX / Win / Linux platforms   
There were not essential jar file pre-built, so build these jar file first   
Develop IDE:  Eclipse, Netbeans or JetBrains Intelli Idea

1. Run script **/binary/build_jar.sh** or **/binary/build_jar.bat** to generted jars.     
   Or

   > Build maven projects /minijvm/java copy to  **/mobile/assets/resfiles/minijvm_rt.jar**      
   > Build maven projects /mobile/java/glfm_gui, copy to  **/mobile/assets/resfiles/glfm_gui.jar**       
   > Build maven projects /mobile/java/ExApp, copy to  **/mobile/assets/resfiles/ExApp.jar**   
   > Maybe you can change   **/mobile/java/ExApp/src/main/java/test/MyApp.java**    , Add your resource to **/mobile/java/ExApp/src/main/resource/res/** , such as audio or image etc, Configure **/mobile/java/ExApp/src/main/config.txt** for icon ,version, boot class, etc

2. XCode open **/mobile/iosapp** ,setup developer account in Signing&Capabilities , build and install app to Device , verify app before running app (Setting->General->Device Management->{Developer account}->Verify App->Trust)
3. Android Studio open **/mobile/androidapp**  build and install app to Android device
4. The AppManager is running   

  <div align=center><img width="340" height="250"   src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/appmgr.png"/></div>

<span id="linkbuilddesktop"/>

## Build for Windows/Linux/MacOS platform

1. Run script **/binary/build_jar.sh** or **/binary/build_jar.bat** to generted jars     
   Or   
   > Build java bootstrap classes  **/minijvm/java**  , Maven build jar and copy to /binary/lib/minijvm_rt.jar    
   > Build gui classes **/desktop/glfw_gui/java** , Maven build jar and copy to /binary/libex/glfw_gui.jar     
   > Build console test case classes **/test/minijvm_test** , Maven build jar and copy to /binary/libex/minijvm_test.jar     
   > Build gui test app classes **/mobile/java/ExApp** , Maven built jar and copy to /binary/{platform}/apps/ExApp.jar

2. Run **/binary/build_mac_linux.sh** or **/binary/build_wini686.bat** or  **/binary/build_winx64.bat** to generted binaries    
   Or   
   > Build gui jni c dynamic library /desktop/glfw_gui/c by cmake    
   > Build minijvm /minijvm/c by cmake

3. Run test script /binary/{platform}/test.sh | test.bat

<span id="linkdebug"/>

## How to debug source

Almost all Java IDEs support remote debugging. The IDE connects to the debug port of the miniJVM through TCP, and then enters the debugging state. The miniJVM has two options, one is to enable the debugging function, and the other is to wait for the IDE to connect to the debug port after the VM is started, and execute the bytecode only after that. These two options need to be setup before initializing the VM.    
These two options defined in jvm.h

```
struct _MiniJVM {
    ...
    s32 jdwp_enable;// 0:disable java debug , 1:enable java debug and disable jit
    s32 jdwp_suspend_on_start;
    ...
};
```

1. Open miniJVM debug mode, mini_jvm jdwp listen on port 8000    
   **Desktop platform** : Run mini_jvm with flag: -Xdebug for debug mode ,Or change source before build.   
   /minijvm/c/main.c 134: jvm->jdwp_enable = 1; //change "jdwp" to 1    
   **iOS/Android** : change source before build      
   /mobile/c/glfmapp/main.c 50: refers.jvm->jdwp_enable = 1; //change 0 to 1    
   If debug on phone device, check the device ip address from General Setting -> wifi ->(i)

2. Run the VM with jars eg:   
   mini_jvm.exe -Xdebug -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/glfw_gui.jar org.mini.glfw.GlfwMain
3. IDE open project , eg: /mobile/java/ExApp    
   **Intelli idea** :  menu Run -> Edit Configurations , + remote , Transport : socket , Debugger mode : attach , host is your mini_jvm running on host ip and port ,ex "127.0.0.1:8000" or "192.168.0.32:8000"   
   **Eclipse** : configuration like as idea    
   **Netbeans** :  menu Debug -> connect to Debugger, Connector : SocketAttach , host is your mini_jvm running on the host and port, ex. "localhost:8000" , Timeout: 10000
4. Then you can set breakpoints and pause vm and watch variables


## Project based miniJVM

[Awtk-minijvm](https://github.com/zlgopen/awtk-minijvm)  :[AWTK](https://github.com/zlgopen/awtk) cross platform native ui bind to minijvm   
[LWJGUI-Mobile](https://github.com/orange451/LWJGUI-Mobile) : java LWJGL UI library        
[BiBiX](https://github.com/digitalgust/BiBiX) : java instantial messager        
[G3d](https://github.com/digitalgust/g3d) : java 3d game demo        
[MiniJVM web demo](https://java-on-web.org/examples/) :  miniJVM on web demo

<span id="linkref"/>

## miniJVM referenced project and technology

[Oracle CLDC](http://www.oracle.com/technetwork/java/cldc-141990.html)  :referenced cldc api     
[OpenJDK](https://openjdk.java.net/) : referenced java api    
[Miniz](https://github.com/richgel999/miniz) :for read jar files    
[Glfm](https://github.com/brackeen/glfm) :for cross platform (android/ios) GUI     
[Nanovg](https://github.com/memononen/nanovg)  :for GUI paint function     
[Stb](https://github.com/nothings/stb) :for GUI truetype font and image    
[Glad](https://github.com/Dav1dde/glad)  :for replace openGL/GLES head file   
[Glfw](https://github.com/glfw/glfw)  :for pc cross platform GUI   
[Dirent](https://github.com/tronkko/dirent)  :for linux style on win vc file and directory access    
[Tinycthread](https://github.com/tinycthread/tinycthread)  :for cross platform thread    
[JRegex](https://github.com/digitalgust/minijvm_third_lib/tree/master/jregex)  :for java String regex match        
[Janino](http://janino-compiler.github.io/janino/)  :for compile java source file     
[MiniAudio](https://github.com/dr-soft/miniaudio)  :for java audio playback and capture    
[Sljit](https://github.com/zherczeg/sljit)  :Platform independent low-level JIT compiler      
[Mbedtls](https://github.com/ARMmbed/mbedtls)  :Https support by mbedtls    
[Avian](https://github.com/ReadyTalk/avian)  :referenced java api

## Development IDE usage

C / ObjC:   JetBrains CLion, Xcode, Virtual studio   
Swift :    XCode    
Java :     Jetbrain Idea, Netbeans   
Android :  Android Studio


<span id="linkembed"/>

## Using miniJVM in project

Copy C source **/minijvm/c**  to your project source folder, copy built jar minijvm_rt.jar to you projec resource folder

```
#include "jvm/jvm.h"

int main(int argc, char **argv) {
    char *bootclasspath = "../../binary/lib/minijvm_rt.jar";
    char *classpath = "../../binary/libex/minijvm_test.jar;./";
    char *main_name = "test.Foo3";

    s32 ret = 1;
    MiniJVM *jvm = jvm_create();
    if (jvm != NULL) {
        jvm->jdwp_enable = 0; //value 1 for java remote debug enable
        jvm->jdwp_suspend_on_start = 0;
        jvm->max_heap_size = 25 * 1024 * 1024; //

        ret = jvm_init(jvm, bootclasspath, classpath);
        if (ret) {
            printf("[ERROR]minijvm init error.\n");
        } else {
            ret = call_main(jvm, main_name, NULL);
        }
        jvm_destroy(jvm);
    }
    return ret;
}
```

## Third liberies   

* ### Janino java compiler

Project:   [Janino](http://janino-compiler.github.io/janino/)       
Janino is a super-small, super-fast Java compiler.   
Janino can not only compile a set of source files to a set of class files like JAVAC, but also compile a Java expression, a block, a class body, one .java file or a set of .java files in memory, load the bytecode and execute it directly in the same JVM. Janino compiler is not the full java compiler, see [limitation](http://janino-compiler.github.io/janino/#limitations) , like :

```
List<String> list=new ArrayList();
list.add("abc");
String s=(String)list.get(0);//can't ignore (String) cast qualifier.
```

Download jars :    
[janino.jar](https://github.com/digitalgust/digitalgust.github.io/blob/main/lib/janino.jar?raw=true)    
[commons-compiler.jar](https://github.com/digitalgust/digitalgust.github.io/blob/main/lib/commons-compiler.jar?raw=true)

```
#compile /binary/res/BpDeepTest.java
mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/janino.jar:../libex/commons-compiler.jar   org.codehaus.janino.Compiler  ../res/BpDeepTest.java
```

* ### Luaj

Project:   [Luaj](https://github.com/luaj/luaj)   
miniJVM adapted : [Luaj minijvm]  https://github.com/digitalgust/minijvm_third_lib
Lightweight, fast, Java-centric Lua interpreter written for JME and JSE, with string, table, package, math, io, os, debug, coroutine & luajava libraries, JSR-223 bindings, all metatags, weak tables and unique direct lua-to-java-bytecode compiling. Download jars :    
[luaj.jar](https://github.com/digitalgust/digitalgust.github.io/blob/main/lib/luaj.jar?raw=true)

```
mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/luaj.jar Sample
```

## Screen shot   

<table>
<tr>
<td>  Windows mini_jvm gui
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/win.png"/></div>

</td>
<td>  Macos mini_jvm gui    
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/mac.png"/></div> 

</td>
<td>  Linux mini_jvm gui    
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/centos.png"/></div>

</td>
<td>  Web mini_jvm gui    
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/doc/img/web-glfw.png"/></div>
</td>
</tr>
</table>

<span id="linkdir"/>    

## Directorie struct   

```
.
├── binary              miniJVM binary  for build(win32/win64/mac/linux/
├── desktop
│   ├── awtk_gui        open source gui frame awtk jni
│   └── glfw_gui        desktop openGL, glfw, miniaudio, native module
├── j2c                 miniJVM java source translate to c tools
│   ├── app
│   └── translator
├── minijvm             Core source
│   ├── c               miniJVM c source 
│   └── java            miniJVM runtime library
├── mobile
│   ├── androidapp      Android launcher
│   ├── assets          mobile app resource, font files, jar files ,pic ,audio etc.
│   ├── c               mobile native lib, openGLES, glfm framework, gui jni, glfmapp
│   ├── iosapp          iOS launcher
│   └── java            mobile java lib, GUI, AppManager, app example
├── doc
└── test                miniJVM test case 
```


[< Back](#linkhome)

<span id="linklicense"/>    

## License

License:    MIT

Gust , digitalgust@163.com .   
