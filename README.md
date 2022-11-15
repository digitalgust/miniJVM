![](/screenshot/mini_jvm_64.png)  
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
[Documentation](#dochome)    

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

  <div align=center><img width="112" height="199" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/demo.gif"/><img width="112" height="199" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/g3d.gif"/></div>

* Instant Message app Demo , [Source](https://github.com/digitalgust/BiBiX)
* 3D game app Demo, [Source](https://github.com/digitalgust/g3d)

<span id="linkarch"/>

## Architecture:

  <div align=center><img width="540" height="350" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/arch.png"/></div>

<span id="linkchangelog"/>

## Changelog:

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

  <div align=center><img width="340" height="250"   src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/appmgr.png"/></div>

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
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/win.png"/></div>

</td>
<td>  Macos mini_jvm gui    
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/mac.png"/></div> 

</td>
<td>  Linux mini_jvm gui    
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/centos.png"/></div>

</td>
<td>  Web mini_jvm gui    
  <div align=center><img width="210" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/web-glfw.png"/></div>
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
├── screenshot
└── test                miniJVM test case 
```

<span id="dochome"/>    

# Documentation

[GUI Framework (java)](#docgui)

* [Outline](#docoutline)
* [GApplication](#docgapplication)
* [GForm](#docgform)
* [GFrame](#docgframe)
* [GMenu](#docgmenu)
* [GButton](#docgbutton)
* [GLabel](#docglabel)
* [GCheckBox](#docgcheckbox)
* [GSwitch](#docgswitch)
* [GScrollBar](#docgscrollbar)
* [GImageItem](#docgimageitem)
* [GList](#docglist)
* [GPanel](#docgpanel)
* [GViewPort](#docgviewport)
* [GViewSlot](#docgviewslot)
* [GTextBox GTextField](#docgtextinput)
* [Custom UI Component](#doccustom)   
* [XML Component common attributes](#docattrib)   
* [Events](#docevents)   
* [Template](#doctemplate)   
* [MultiLanguage](#docmultilang)   
* [Style](#docstyle)   
* [Layout](#doclayout)
  * [XY layout](#doclayoutxy)   
  * [XML flow layout](#doclayoutflow)   
  * [XML table layout](#doclayouttable)
* [Script](#docscript)   
  * [Syntax](#docscriptsyntax)   
  * [Stdlib api](#docscriptstdlib)   
  * [GUI Scriptlib api](#docscriptguilib)   
  * [Extention library](#docscriptextlib)   
* [Example](#docexample)
* [Example Game](#docexamplegame)

[Audio](#docaudio)

<span id="docgui"/>

## GUI Framework

<span id="docoutline"/>

* ### Outline

   <div align=center><img width="600" height="160" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/doc_gui_arch.png"/></div>

   * Visible UI components are inherited from GObject.
   * All GUI application are extends class GApplication, GApplication contains a GForm, and starts by AppManager.
   * GForm is a panels of OS window or phone screen, a bottom visible UI container.
   * GFrame is an internal closable and movable window container.
   * All UI components are placed in a GContainer.
   * GContainer can be a child of another GContainer.
   * Set a GStyle for all components to change appearance.    
   * There are two methods to new a GObject , one is manual create an object, the other is added a node to XML file.   
   * GUI XML layout like HTML, and there is a simple script like javascript for HTML.    
   * iOS/Android system based on GLFM  (/mobile/iosapp/ , /mobile/androidapp/)    
   * Desktop window system based on GLFW   (/desktop/glfw_gui/)
   * GUI rendering by Nanovg
   * OpenGL 3.3 for desktop and OpenGLES 3.0 for mobile
   * The UI consists of two parts, one is compiled into a dynamic link library by the jni native function, and the native function is compiled together with the JVM on the mobile phone, and the other part is the java api.    


[< Back](#dochome)

<span id="docgapplication"/>    

* ### GApplication

```
package test;
import org.mini.apploader.GApplication;
import org.mini.gui.GForm;

public class MyApp0 extends GApplication {
    GForm form;

    @Override
    public GForm getForm() {
        if (form != null) {
            return form;
        }
        form = new GForm(null);

        return form;
    }
}
``` 
This is a simplest app, override getForm() to export a GForm to AppManager, Device show the form ,and post events (Keyboard/Mouse/TouchScreen/etc) to the form.

<!--
miniJVM 本身并不包含图形系统，为便于开发，依附于VM建立了一套基于OpenGL/GLES 的图形框架，
这套系统会在启动VM后，进入AppManager的图形界面，这个应用管理器负责管理更多的java开发的应用，
包括安装应用，删除应用等功能，其安装应用的方式有下载jar的方式安装，或上传jar的方式，
这些jar应用，在jar中必须包含一个config.txt的文件，用于描述此应用的一些属性，比如启动类，图标，更新地址等。   
-->

miniJVM core does not contain a graphics system. For desktop and mobile phone, a graphics framework based on OpenGL/GLES is established attached to the JVM.
This system will enter the graphical interface of AppManager after starting the VM. This application manager is managing more applications developed by java.
It can be installing and deleting applications, etc. The way of installing applications include downloading jars, or uploading jars.
These jars applications must contain a config.txt file in the jar, which is used to describe some properties of this application, such as startup class, icon, update address, etc.

config.txt
```
name=ExApp
app=test.MyApp
icon=/res/hello.png
desc=  Mobile app develop example.\n  you can upgrade it.
version=1.0.4
upgradeurl=https://github.com/digitalgust/miniJVM/raw/master/mobile/assets/resfiles/ExApp.jar
```


<div align=center><img width="300" height="150" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/doc_gui_form.jpg"/></div>


[< Back](#dochome)


<span id="docgform"/>    

* ### GForm

Save the xml to a file .

```
<form name="FORM_MAIN" w="100%" h="100%">
</form>
```

Load the xml file and build form . there 3 attributes descript the form.    
Attributes:   
**name** : String , the form name.    
**w** : form width, a percentage of OS window or a abstract int value for pixels. "100%" indicate the form width is window width, or phone screen width.    
**h** : like w, form height.

```
package test;
import org.mini.apploader.GApplication;
import org.mini.gui.*;
import org.mini.layout.*;

public class MyApp2 extends GApplication {
    GForm form;

    @Override
    public GForm getForm() {
        if (form != null) {
            return form;
        }

        //load xml
        String xmlStr = GToolkit.readFileFromJarAsString("/res/myapp2-form.xml", "utf-8");
        UITemplate uit = new UITemplate(xmlStr);
        XContainer xc = (XContainer) XContainer.parseXml(uit.parse(), new XmlExtAssist(null));
        int screenW = GCallBack.getInstance().getDeviceWidth();
        int screenH = GCallBack.getInstance().getDeviceHeight();

        //build gui with event handler
        xc.build(screenW, screenH, new XEventHandler() {
            @Override
            public void action(GObject gobj) {
            }

            public void onStateChange(GObject gobj, String cmd) {
            }
        });
        form = xc.getGui();
        return form;
    }
}

```

The MyApp2 same as MyApp0. Show a form but there is nothing in the form.    

[< Back](#dochome)


<span id="docgframe"/>    

* ### GFrame

```
<form name="FORM_MAIN" w="100%" h="100%" closable="1">
    <frame name="FRAME_TEST" w="80%" h="80%" title="WINDOW" onclose="onTestClose()" oninit="onTestOpen()">
    </frame>
</form>
```

There is a frame show on the form.     
Attributes:   
**closable**: 1: the frame can be closed. 0:can't close it.        
**title**: String , the frame's title.        
**onclose**: String , the script function call, this function must in the frame's script partion.        
**oninit**: String , the script function call, this function must in the frame's script partion.
<div align=center><img width="300" height="150" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/doc_gui_frame.jpg"/></div>


[< Back](#dochome)


<span id="docgmenu"/>    

* ### GMenu

```
<form name="FORM_MAIN" w="100%" h="100%">
    <frame name="FRAME_TEST" w="80%" h="70%" title="WINDOW">
    </frame>

    <menu name="MENU_MAIN" x="0" y="90%" w="100%" h="10%" fixed="1" contextmenu="0">
        <mi name="MI_OPENFRAME" pic="/res/hello.png">Test</mi>
        <mi name="MI_EXIT" pic="/res/appmgr.png">Exit</mi>
    </menu>
</form>
```

The menu have two menu item, menu item have an icon, and a text. The menu location specify x=0 and y=90%, show on the buttom of form.   
Attributes:   
**fixed**:    1:menu location is fixed. 0:menu location is not fixed.    
**contextmenu**:  1:the menu is contextmenu ,like "copy/paste"

How to active the menu item?

```
        //build gui with event handler
        xc.build(screenW, screenH, new XEventHandler() {
            @Override
            public void action(GObject gobj) {
                String name = gobj.getName();
                switch (name) {
                    case "MI_OPENFRAME":
                        // do something
                        break;
                    case "MI_EXIT":
                        closeApp();
                        break;
                }
            }

            public void onStateChange(GObject gobj, String cmd) {
            }
        });
```

in the XEventHandler, on Test Button touched or clicked ,then do something.
<div align=center><img width="300" height="150" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/doc_gui_menu.jpg"/></div>


[< Back](#dochome)


<span id="docgbutton"/>    

* ### GButton

```
<button name="BT_SCRIPT" h="30" onclick="change()" addon="20" preicon="✉">Change</button>
```

If not specify the "w" width, the default width is Text width add addon.   
If not specify the "h" height, the default height is Text width add 30pix.   

Attributes:   
**onclick**: String , the script function call, this function must in the frame's script partion.    
**addon**: int , specify an int value for increase button's width, the default value is 30pix, in pixels.     
**preicon**: utf8 emoji char , as icon front of the text.     

[< Back](#dochome)


<span id="docglabel"/>    

* ### GLabel

```
<label h="30" align="left,vcenter" onclick="change()" addon="20" multiline="0">TEXT NOW</label>
```

If not specify the "w" width, the default width is Text width add addon.   
If not specify the "h" height, the default height is Text width add 30pix.

Attributes:   
**align**: String , value: left, hcenter, right, top, hcenter, bottom, bottom, center. "center" is "hcenter,hcenter"      
**onclick**: String , the script function call, this function must in the frame's script partion.     
**addon**: int , specify an int value for increase button's width, the default value is 30pix, in pixels.    
**multiline**: int , 1: show multiple row text, if h is ignore ,the lable height is multiple line text total height. 0: single line text.    

[< Back](#dochome)


<span id="docgcheckbox"/>    

* ### GCheckBox

```
<checkbox name="SET_AUDIO_ENABLE" w="50%" selected="1" onclick="setAudioEnable()">Audio</checkbox>
```

If not specify the "w" width, the default width is Text width add 30pix.   
If not specify the "h" height, the default height is Text width add 30pix.

Attributes:   
**selected**: int , 1: the checkbox is selected. 0: not selected    
**onclick**: String , the script function call, this function must in the frame's script partion.    

[< Back](#dochome)

<span id="docgscrollbar"/>    

* ### GScrollBar

```
<scrollbar name="SET_AUDIO_EFFECT" w="50%" h="25" value="0.0" scroll="h" onchange="seteffect()" onclick="seteffectAndSave()"></scrollbar>
```

If not specify the "w" width, the default width is Text width add 30pix.   
If not specify the "h" height, the default height is Text width add 30pix.   

Attributes:   
**value**: float , 0.0f ~ 1.0f, percent of slider position    
**scroll**: h: HORIZONTAL layout the scrollbar; v: VERTICAL layout the scrollbar.    

[< Back](#dochome)

<span id="docgimageitem"/>    

* ### GImageItem

```
<img w="32" h="32" fly="1" border="1" fontsize="10" pic="{SKILL_IMG_2}" onclick="onSkillClick({SKILL_INDEX_2})" cmd="{SKILL_CMD_2}" name="SKILL_{SKILL_INDEX_2}">{SKILL_LEV_2}</img>
```

If not specify the "w" width, the default width is image width.   
If not specify the "h" height, the default height is image height.  

Attributes:   
**fly**: int , 1: the image item can be drag. 0: fixed and can not be drag    
**pic**: String , the path of image in jar.   
**border**: int , 1: draw a round rectanger out of the image. 0: no border   
**alpha**: float , 0.0f - 1.0f: draw image with alpha   

[< Back](#dochome)

<span id="docglist"/>    

* ### GList

```
<list name="RECEIVERS" w="40%" h="30" scroll="1" multiline="0" multiselect="0">
    <li name ="LISTITEM1" pic="/res/ui/men.png" selected="1" preicon="✉">Jack</li>
    <li name ="LISTITEM1">>Tom</li>
</list>
```

GList is a multiple rows or single row with popup list.    
If not specify the "w" width, the default width is parent's width .   
If not specify the "h" height, the default height : multiline=1 that is items count multiply 40pix; multiline=0 that's 40pix.   

Attributes of GList:   
**scroll**: int , 1: the scrollbar is visible. 0: invisible   
**multiline**: int , 1: the list is multiple row mod. 0: single row mod, when click the list would popup pulldown list   
**multiselect**: int , 1: the select mod is multiple select mode . 0: select one menu item at once   

Attributes of GListItem:   
**pic**: String , the path of image in jar    
**selected**: int , 1: the menu item is selected. 0: not selected    
**preicon**: utf8 char , an emoji char as icon front of the text. if pic is not null, then the preicon invisible    

[< Back](#dochome)

<span id="docgpanel"/>    

* ### GPanel

```
<panel w="100%" h="100%"></panel>
```

If not specify the "w" width, the default width is parent's width.   
If not specify the "h" height, the default height is parent's height.

Attributes:

[< Back](#dochome)

<span id="docgviewport"/>    

* ### GViewPort

```
<viewport w="100%" h="100%" name="MAINVIEW">
</viewport>
```

GViewPort is a scalable panel, the width and height will be automatic scale ，if child is add or remove    
If not specify the "w" width, the default width is parent's width.   
If not specify the "h" height, the default height is parent's height.  

Attributes:

[< Back](#docgviewslot)

<span id="docgscrollbar"/>    

* ### GViewSlot

```
<viewslot w="100%" h="100%" y="0" scroll="h" name="SHOWBOARD">
    <viewport w="100%" h="100%" name="INTRO" move="right">
        <label>Label in viewport</label>
    </viewport>
    
    <panel w="100%" h="100%" move="left,right">
        <label>Label in panel</label>
    </panel>
    
    <table move="left">
        <tr><td><label>A</label></td><td><label>1</label></td></tr>
        <tr><td><label>B</label></td><td><label>2</label></td></tr>
    </table>
</viewslot>
```

GViewSlot is a multi-slot container. Each slot is a fullsize container. Only one slot is visible at once. It can be switched to another slot by dragging and dropping. There are three slots in the above example. One is a ViewPort, the second is a Panel, and the third is a table. Each container has a "move" attribute whose value indicates which direction it can slide.    
If not specify the "w" width, the default width is parent's width.   
If not specify the "h" height, the default height is parent's height.

Attributes:   
**scroll**: h: the viewslot sliding direction is HORIZONTAL. v: VERTICAL   

[< Back](#dochome)

<span id="docgtextinput"/>    

* ### GTextBox GTextField

```
<input name="WRITE_BOX" w="100%" h="20%" multiline="1" edit="1">default text</input>
```

If not specify the "w" width, the default width is image width.   
If not specify the "h" height, the default height is image height.

Attributes:   
**multiline**: int , 1: multiple row input box. 0: single row input field   
**edit**: int , 1: edit enable. 0: edit disable   
**style**: search : the search style, ignored in multiline mode    
**hint**: String : hint string in the input area,if text is inputed ,the hint invisible    
**union**: String : another UI component name, the input box doesn't lost focus when union component clicked, this is important on phone device, if the input component's focus losted then the keyboard would hide .    
**password**: int , 1: input text is hidden with *, 0: not password input   

[< Back](#dochome)

<span id="doccustom"/>    

* ### Custom UI Component

Customized components can be extended to meet requirements, and customized components can also be directly layout in the xml UI.

```
<test.ext.XCustomList name="CUSTLIST" w="100%" h="100%">
</test.ext.XCustomList>
```

Register customized UI components when parsing xml.

```
    XmlExtAssist assist = new XmlExtAssist(form);
    assist.registerGUI("test.ext.XCustomList");   
    String xmlStr = GToolkit.readFileFromJarAsString("/res/Frame1.xml", "utf-8");
    UITemplate uit = new UITemplate(xmlStr);
    XContainer xc = (XContainer) XContainer.parseXml(uit.parse(), assist);
    xc.build((int) form.getW(), (int) form.getH(), this);
    GFrame f1 = xc.getGui();
                        
```

customized parser:

```
package test.ext;

import org.mini.gui.GObject;
import org.mini.layout.XContainer;
import org.mini.layout.XList;

public class XCustomList extends XList {
    static public final String XML_NAME = "test.ext.XCustomList";


    public XCustomList(XContainer xc) {
        super(xc);
    }

    @Override
    protected String getXmlTag() {
        return XML_NAME;
    }


    protected GObject createGuiImpl() {
        return new GCustomList(getAssist().getForm(), x, y, width, height);
    }

}

```

customized GUI component:

```
package test.ext;

import org.mini.gui.GForm;
import org.mini.gui.GList;

public class GCustomList extends GList {

    public GCustomList(GForm form, float left, float top, float width, float height) {
        super(form, left, top, width, height);
    }

}

```

[< Back](#dochome)

<span id="docattrib"/>    

* ### XML Component common attributes

Many of UI component are supports these attribute, excepted GMenuItem and GListItem
<table border="1">
<tr><td>Attribute</td><td>Value</td><td>Example</td></tr>
<tr><td>name</td><td>the UI component name. it used for find the component on coding usually </td><td>name="BT_SUBMIT"</td></tr>
<tr></td><td>attachment</td><td>attach a String to the UI component, it's invisible</td><td>attachment="this component is used for memo"</td></tr>
<tr></td><td>cmd</td><td>same as attachment, attach a String to the UI component, it's invisible</td><td>attachment="more info here"</td></tr>
<tr></td><td>onclick</td><td>call script function when the UI component is clicked or touched, the function must in parents of this component</td><td>onclick="submit()"</td></tr>
<tr></td><td>onchange</td><td>call script function when the UI component state is changed, the function must in parents of this component</td><td>onchange="onChange()"</td></tr>
<tr></td><td>fly</td><td>whether or not the UI component can be drag to move, Implemente flyBegin(), flying(), flyEnd() events handler in XEventHandler.</td><td>fly="0"</td></tr>
<tr></td><td>hidden</td><td>whether or not hidden the UI component </td><td>hidden="0"</td></tr>
<tr></td><td>enable</td><td>enable or disable the UI component </td><td>enable="1"</td></tr>
<tr></td><td>move</td><td>the UI component as child of GViewSlot, the value describe sliding direction<br/>value: left/right/up/down</td><td>move="left,right"<br/>move="up,down"</td></tr>
<tr></td><td>w</td><td>the UI component width<br/>value type: int,percent,float</td><td>w="150"<br/>w="100%"<br/>w="float"</td></tr>
<tr></td><td>h</td><td>the UI component height<br/>value type: int,percent,float</td><td>h="150"<br/>h="100%"<br/>h="float"</td></tr>
<tr></td><td>x</td><td>the UI component x location<br/>value type: int,percent</td><td>h="10"<br/>h="30%"</td></tr>
<tr></td><td>y</td><td>the UI component y location<br/>value type: int,percent</td><td>h="10"<br/>h="30%"</td></tr>
<tr></td><td>bgcolor</td><td>set the UI component backgroud color<br/>value type: RGBA</td><td>h="10"<br/>bgcolor="00ff0080"</td></tr>
<tr></td><td>color</td><td>set the UI component text color<br/>value type: RGBA</td><td>h="10"<br/>color="00ff00ff"</td></tr>
<tr></td><td>fontsize</td><td>set the UI component text font size<br/>value range: 0-1000</td><td>h="10"<br/>fontsize="18"</td></tr>
<tr></td><td>preicon</td><td>set the UI component preview icon char<br/>value type: emoji char</td><td>h="10"<br/>preicon="✉"</td></tr>
</table>

[< Back](#dochome)

<span id="docevents"/>    

* ### Events
An event example:
```
GButton bt = new GButton(form, "exit", 0, 0, 100, 20);
frame.add(bt);
bt.setActionListener(new GActionListener() {
    @Override
    public void action(GObject gObject) {
        closeApp();
    }
});
```

**GActionListener**    
When mouse clicked or touched a UI component, the event be call, para gobj is the clicked component.   
void action(GObject gobj);    
Responses by:   
All components.

**GStateChangeListener**    
void onStateChange(GObject gobj);    
When a UI component state changed, the event be cal.    
Responses by:    
GFrame closed   
GList selected an item   
ScrollBar slider moved   
GTextBox/GTextField text changed   
GViewSlot slot changed   

**GFlyListener**    
When draging a UI component to move, the event be call    
public void flyBegin(GObject gObject, float x, float y);   
public void flying(GObject gObject, float x, float y);   
public void flyEnd(GObject gObject, float x, float y);   
Responses by:   
All components.   

**GChildrenListener**    
When a UI component add to or remove from it's parents, the event be call     
void onChildAdd(GObject child);   
void onChildRemove(GObject child);   
Responses by:   
All GContainer components.

**GFocusChangeListener**    
When a UI component gain focus or lost focus, the event be call     
void focusGot(GObject oldgo);    
void focusLost(GObject newgo);     
Responses by:   
All components.

**GAppActiveListener**    
When app has actived, the event be call     
void focusGot(GObject oldgo);    
void focusLost(GObject newgo);     
Responses by:   
When GApplication has actived.

**GKeyboardShowListener**    
When keyboard popup, the event be call     
void keyboardShow(boolean show, float x, float y, float w, float h);       
Responses by:   
GForm

**GNotifyListener**    
An native async notify, like that iOS notify the application device UUID     
void onNotify(String key, String val);      
Responses by:   
GForm

**GPhotoPickedListener**    
When iOS and Android pick an image or video finished     
void onPicked(int uid, String url, byte[] data);      
Responses by:   
GForm

**GSizeChangeListener**    
When OS changed the window size or mobile phone screen changes horizontal and vertical      
void onPicked(int uid, String url, byte[] data);      
Responses by:   
GForm

[< Back](#dochome)

<span id="doctemplate"/>    

* ### Template

Each XML UI file is a template, and some keywords in these templates can be replaced, which is useful in multi-language development.   
The keywords in the curly braces in the example below will be replaced during parsing.   
```
<frame name="FRAME_TEST" w="80%" h="80%" bgcolor="{FRAME_BGCOLOR}" title="{FRAME_TITLE}">
</frame>
```
Parse code:    
```
String xmlStr = GToolkit.readFileFromJarAsString("/res/MyForm.xml", "utf-8");
UITemplate uit = new UITemplate(xmlStr);
UITemplate.getVarMap().put("FRAME_TITLE", "Detail"); //replace keywork in xml
UITemplate.getVarMap().put("FRAME_BGCOLOR", "80303030"); //replace keywork in xml
XContainer xc = (XContainer) XContainer.parseXml(uit.parse());
```

[< Back](#dochome)

<span id="docmultilang"/>    

* ### MultiLanguage
```
//Add multilanguage words to the system
GLanguage.addString("Start", new String[]{"Start", "启动", "啟動"});
GLanguage.addString("Stop", new String[]{"Stop", "停止", "停止"});

//set the default language
GLanguage.setCurLang(GLanguage.ID_CHN); //more options : ID_ENG , ID_CHT

//If you need to add more languages, you can add more elements to the array, such as
GLanguage.addString("More", new String[]{"More", "更多", "更多", "もっと"});

GLanguage.setCurLang(3); //that 3 is array index
```


[< Back](#dochome)

<span id="docstyle"/>    

* ### Style
The GUI system can set a variety of color appearance. By default, two appearance of light and dark are provided. Users can customize other color schemes.

```
GToolkit.setStyle(new GStyleBright());
GToolkit.setStyle(new GStyleDark());  // Or set to customize style
```
Customize appearance

```
import org.mini.gui.GStyle;
import static org.mini.gui.GToolkit.nvgRGBA;

public class GStyleGolden extends GStyle {

    @Override
    public float getTextFontSize() {
        return 16f;
    }

    @Override
    public float getTitleFontSize() {
        return 18f;
    }

    @Override
    public float getIconFontSize() {
        return 35f;
    }

    float[] textFontColor = nvgRGBA(237, 217, 158, 0xc0);

    @Override
    public float[] getTextFontColor() {
        return textFontColor;
    }

    float[] textShadowColor = nvgRGBA(0, 0, 0, 0xb0);

    @Override
    public float[] getTextShadowColor() {
        return textShadowColor;
    }

    float[] disabledTextFontColor = nvgRGBA(0x60, 0x60, 0x60, 0x80);

    @Override
    public float[] getDisabledTextFontColor() {
        return disabledTextFontColor;
    }

    float[] frameBackground = nvgRGBA(0x20, 0x20, 0x20, 0xff);

    @Override
    public float[] getFrameBackground() {
        return frameBackground;
    }

    float[] frameTitleColor = nvgRGBA(0xd0, 0xd0, 0xd0, 0xff);

    @Override
    public float[] getFrameTitleColor() {
        return frameTitleColor;
    }

    float[] hintFontColor = nvgRGBA(0xff, 0xff, 0xff, 64);

    @Override
    public float[] getHintFontColor() {
        return hintFontColor;
    }

    float[] editBackground = nvgRGBA(0x00, 0x00, 0x00, 0x20);

    @Override
    public float[] getEditBackground() {
        return editBackground;
    }

    @Override
    public float getIconFontWidth() {
        return 18;
    }

    float[] selectedColor = nvgRGBA(0x80, 0xff, 0x80, 0x40);

    @Override
    public float[] getSelectedColor() {
        return selectedColor;
    }

    float[] unselectedColor = nvgRGBA(0x80, 0x80, 0x80, 0x10);

    @Override
    public float[] getUnselectedColor() {
        return unselectedColor;
    }

    float[] backgroundColor = nvgRGBA(0x30, 0x20, 0x15, 0xff);

    @Override
    public float[] getBackgroundColor() {
        return backgroundColor;
    }

    float[] listBackgroundColor = nvgRGBA(0x30, 0x20, 0x15, 0x30);

    @Override
    public float[] getListBackgroundColor() {
        return listBackgroundColor;
    }

    float[] popBackgroundColor = nvgRGBA(0x10, 0x10, 0x10, 0xc0);

    @Override
    public float[] getPopBackgroundColor() {
        return popBackgroundColor;
    }

    float[] highColor = nvgRGBA(0xff, 0xff, 0xff, 0x80);//

    public float[] getHighColor() {
        return highColor;
    }

    float[] lowColor = nvgRGBA(0x80, 0x80, 0x80, 0x80);//0x006080ff

    public float[] getLowColor() {
        return lowColor;
    }

}

```

[< Back](#dochome)


<span id="doclayout"/>   

* ### Layout

  All UI containers support the following three layouts. In XML, flow layout and table layout are usually simultaneously exist. Layout is just a concept, we do not need to specify which layout to use anywhere. The layout manager automatically layout according to the description in XML.

  [< Back](#dochome)


   <span id="doclayoutxy"/>   

* #### XY layout

  Manual init a GUI component, and specify fixed position. Example:

  ```
  GFrame frame=new GFrame(form);
  GLabel lab = new GLabel(form, "This is a label", 20, 20, 100, 20);
  frame.getView().add(lab);
  GButton but = new GButton(form, "Submit", 20, 100, 100, 20);
  frame.getView().add(but);
  ```

  [< Back](#dochome)


<span id="doclayoutflow"/>   

* #### XML flow layout
  The flow layout does not specify the fixed position of the UI components, and the components are arranged in rows. If the next UI component cannot be accommodated in current row, the component will be placed in a new row. The row height is the height of the highest component in this row.

  If need force a line break, can use &lt;br/&gt; to break row.

  [< Back](#dochome)


<span id="doclayouttable"/>   

* #### XML table layout
  The table layout is the same as the HTML table, TABLE represents an area, TR represents a row, TD represents a column in a row, and another TABLE can be nested in TD. If the table has multiple rows, the height of one TR can be scalable, and the height attribute is h="float", that is, the table height is subtracted from the height of other rows, and the remaining height is the height of the scalable TR. If there are multiple columns in a row of the table, the width of one TD can be scalable. The width attribute is w="float". The width of this TR is subtracted from the sum of the widths of other TD, and the remaining width is the width of the scalable TD.

```
        <table w="100%" h="100%">
            <tr h="100%">
                <td w="50%" h="100%">
                    <table h="100%">
                        <tr h="30">
                            <td h="100%">
                                <label w="100%" align="center">Member List</label>
                            </td>
                        </tr>
                        <tr h="float">
                            <td>
                                <list name="NPCCLAN_LIST" w="100%" h="100%" multiline="1" itemh="25">
                                    <li>Jack</li>
                                    <li>Tom</li>
                                </list>
                            </td>
                        </tr>
                        <tr h="30">
                            <td>
                                <button name="NPCCLAN_PREV" w="30%">Preview</button>
                                <label name="NPCCLAN_PAGE" w="40%" align="center"></label>
                                <button name="NPCCLAN_NEXT" w="30%">Next</button>
                            </td>
                        </tr>
                    </table>
                </td>
                <td w="50%">
                    <label w="100%" align="center">Detail</label>
                    <label w="30%" align="left,vcenter">Name:</label>
                    <label w="70%" align="left,vcenter" name="NPCCLAN_NAME">NPCCLAN_NAME</label>
                    <label w="100%" h="80" multiline="1" name="NPCCLAN_DESC">NPCCLAN_DESC</label>
                    <br/>
                    <button name="NPCCLAN_APPL" w="100%">Application</button>
                </td>
            </tr>
        </table>

```

<div align=center><img width="500" height="261" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/doc_gui_tablelayout.jpg"/></div>


  [< Back](#dochome)



<span id="docscript"/>   

* ### Script

A simple scripting system is embedded in XML, and scripts can modify UI component or services for applications. This script is similar to what javascript for HTML.    
The script can be extended as a bridge between the script and the java system.

XML node &lt;script&gt; can be place in any GContainer components, if a button has "onclick" attribute, the button find it's parent's script, if it's parent no script exists, then find the grandparent, until found .

```
    <frame name="FRAME_TEST" w="80%" h="70%" title="WINDOW" oninit="onOpen()">
        <script>
            <![CDATA[

            sub onOpen()
                print("FRAME_TEST is open")
            ret

            sub change()
                red=mod(random(),255)
                green=mod(random(),255)
                blue=mod(random(),255)
                setColor("LAB_HELP",red,green,blue,255)
                setText("LAB_HELP","Only a test")
            ret

            ]]>
        </script>

        <label name="LAB_HELP" w="50%" align="left,vcenter">TEXT NOW</label>
        <button name="BT_CHANGE" w="50%" onclick="change()">Change Color</button>
    </frame>    

```

When the button BT_CHANGE clicked, the script function "change()" would be call, the func change the label "LAB_HELP" text and color.   
Script onOpen() would be called when the frame is openning.

[< Back](#dochome)


<span id="docscriptsyntax"/>   

* #### Script Syntax

```
' this is comment 
' Variable names are NOT case sensitive
a = 3        'define variable a as Int and set value to 3 , this Int is 64bit Int
b = "ABC"    'define variable b as Str and set value to "ABC"
c = true     'define variable c as Bool and set value to true

arr[2][10]      'define variable arr as 2 dim Array and set array length [2][10]
arr[0][0] = 5
arr[0][1] = "CDE"
arr[0][2] = false

'operator ex
a = (a + 7) * min(a, 5) - 15 / 3 
b = "Hellow" + " world" + 100 + true
c = (7 > 6) | (a = 3) & (!c)

'branch statment
if(a > 5)
    b="great than five"
else
    b="less than or equals five" 
eif

' loop statment
i = 0
while(i < 3)
    print(i+" ")
loop

' function call
a = sum(5, 7)
p(a)

'function define
'function names are NOT case sensitive
sub sum(x, y)
ret x+y           ' return value x+y 

sub p(v)
    println(v)
ret               ' No value return

```

[< Back](#dochome)


<span id="docscriptstdlib"/>   

* #### Stdlib api

<table>
<tr><td>Return Type</td><td>call</td><td>function</td></tr>

<tr><td>Int</td><td>arrlen(arr)</td><td>return array arr length </td></tr>
<tr><td>Int</td><td>abs(a)</td><td>return a&lt;0?-a:a</td></tr>
<tr><td>Str</td><td>base64enc("ABCD")</td><td>return base64 encoded of "ABCD"</td></tr>
<tr><td>Str</td><td>base64dec("QUJDRA==")</td><td>return base64 decoded of "QUJDRA=="</td></tr>
<tr><td>void</td><td>def("varname",2)</td><td>Define a globe variable varname, and set as Int 2 </td></tr>
<tr><td>Bool</td><td>equals(str1,str2)</td><td>if str1 equals str2 return true, else false</td></tr>
<tr><td>Int</td><td>getbit(1000,1)</td><td>return 0 or 1, get bit index 1 of number 1000</td></tr>
<tr><td>Bool/Str/Int/Obj</td><td>getobjfield(sound,"effectVolume")</td><td>return java Object sound field "effectVolume" value</td></tr>
<tr><td>Bool</td><td>isDef("varname")</td><td>Whether or not defined a globe variable varname </td></tr>
<tr><td>Int</td><td>idxof("ABCD", "CD")</td><td>return index of "CD" in "ABCD", this case is 2</td></tr>
<tr><td>Bool</td><td>isnull(var)</td><td>determine var is null, var type limited: Str or Obj</td></tr>
<tr><td>Bool/Str/Int/Obj/void</td><td>invokejava(sound,"setSoundOpen(Z)V",true)</td><td>call java Object sound method "setSoundOpen(Z)V" , 1 parameter, no return<br/>invokejava(smgr,"addUser(Lcom.lba.user.Admin;I)Z",user1,30) ,2 para,the firstis  user1 , an object of class com.lba.user.Admin, the second para 30 is int type.</td></tr>
<tr><td>Bool/Str/Int/Obj/void</td><td>invokeStatic("com.lba.user.Admin","check()V")</td><td>call class com.lba.user.Admin method "check()V" , 0 parameter, no return</td></tr>
<tr><td>Int</td><td>min(a,b)</td><td>return a&lt;b?a:b </td></tr>
<tr><td>Int</td><td>max(a,b)</td><td>return a&gt;b?a:b </td></tr>
<tr><td>Int</td><td>mod(a, b)</td><td>return a % b</td></tr>
<tr><td>void</td><td>print("ABC")</td><td>print a string</td></tr>
<tr><td>void</td><td>println()<br/>println("ABC"+3)</td><td>print a newline ; print a string with newline</td></tr>
<tr><td>Int</td><td>random()</td><td>return Int value may be negative</td></tr>
<tr><td>Int</td><td>strlen(str)</td><td>return string length</td></tr>
<tr><td>Str</td><td>substr("ABCD", 0, 3)</td><td>return sub string of "ABCD", start at 0, end at 3, this case return "ABC"</td></tr>
<tr><td>Array</td><td>split("A-BC-D", "-")</td><td>return splited string array , this case return ["A","BC","D"]</td></tr>
<tr><td>void</td><td>setObjField(sound,"effectVolume", 10)</td><td>set java Object sound field "effectVolume" value</td></tr>
<tr><td>Int</td><td>str2int("7")</td><td>Convert String to Int ,same as valueOf(Str)</td></tr>
<tr><td>Int</td><td>setbit(1000,5,0)</td><td>the number 1000, set bit index 5 to 0</td></tr>
<tr><td>Str</td><td>trim(" ABC ")</td><td>return trim String , this case is "ABC"</td></tr>
<tr><td>Int</td><td>valueOf("7")</td><td>Convert String to Int </td></tr>
</table>

[< Back](#dochome)


<span id="docscriptguilib"/>   

* #### GUI Scriptlib api

<table>
<tr><td>Return Type</td><td>call</td><td>function</td></tr>
<tr><td>void</td><td>setBgColor("COMP1",r,g,b,a)</td><td>set component background color</td></tr>
<tr><td>void</td><td>setColor("COMP1",r,g,b,a)</td><td>set component text color</td></tr>
<tr><td>void</td><td>setBgColorHexStr("COMP1","00ff00ff")</td><td>set component background color, color format is RGBA</td></tr>
<tr><td>void</td><td>setColorHexStr("COMP1","00ff00ff")</td><td>set component text color, color format is RGBA</td></tr>
<tr><td>void</td><td>setText("COMP1","new string")</td><td>set component text String</td></tr>
<tr><td>Str</td><td>getText("COMP1")</td><td>return component text</td></tr>
<tr><td>void</td><td>setXY("COMP1",x,y)</td><td>set component position to x,y</td></tr>
<tr><td>void</td><td>setWH("COMP1",w,h)</td><td>set component Width,Height to w,h</td></tr>
<tr><td>Int</td><td>getX("COMP1")<br/>getX()</td><td>get component position x, if no component specified, return form x</td></tr>
<tr><td>Int</td><td>getY("COMP1")<br/>getY()</td><td>get component position y, if no component specified, return form y</td></tr>
<tr><td>Int</td><td>getW("COMP1")<br/>getW()</td><td>get component position w, if no component specified, return form w</td></tr>
<tr><td>Int</td><td>getH("COMP1")<br/>getH()</td><td>get component position h, if no component specified, return form h</td></tr>
<tr><td>Str</td><td>getCmd("COMP1")</td><td>get component command string</td></tr>
<tr><td>void</td><td>setCmd("COMP1","command str")</td><td>set component command to the second parameter</td></tr>
<tr><td>void</td><td>close("FRAME1")</td><td>close GFrame that specified name</td></tr>
<tr><td>Int</td><td>getCurSlot("VIEWSLOT1")</td><td>get current slot of GViewSlot that specified name</td></tr>
<tr><td>void</td><td>showSlot("VIEWSLOT1",2,100)</td><td>set current slot of GViewSlot to 2, move animation 200ms</td></tr>
<tr><td>void</td><td>setImgPath("COMP1","/res/test.png")</td><td>set GImageItem image to "/res/test.png"</td></tr>
<tr><td>void</td><td>setImg("COMP1",gimageObj)</td><td>set GImageItem image to java GImage object</td></tr>
<tr><td>Obj</td><td>getImg("COMP1",gimageObj)</td><td>get GImageItem image to Obj type</td></tr>
<tr><td>void</td><td>setAttachStr("COMP1","a string")</td><td>attach the String to specify component</td></tr>
<tr><td>Str</td><td>getAttachStr("COMP1")</td><td>return the attachment as String from specify component</td></tr>
<tr><td>void</td><td>setAttachInt("COMP1","a string")</td><td>attach the Int to specify component</td></tr>
<tr><td>Int</td><td>getAttachInt("COMP1")</td><td>return the attachment as Int from specify component</td></tr>
<tr><td>Int</td><td>getListIdx("List1")</td><td>return the current selected index of specify GList </td></tr>
<tr><td>Str</td><td>getListText("List1")</td><td>return the selected item text of specify GList </td></tr>
<tr><td>void</td><td>setListIdx("List1",2)</td><td>set the specify GList selected item to 2</td></tr>
<tr><td>void</td><td>setImgAlphaStr("List1","0.5")</td><td>set the specify GImageItem image alpha to 0.5, NOTICE the second parameter is String</td></tr>
<tr><td>void</td><td>setEnable("COMP1",true)</td><td>set the specify component active or not</td></tr>
<tr><td>void</td><td>setCheckBox("CHKBOX1",true)</td><td>set the specify GCheckBox status to true or false</td></tr>
<tr><td>Bool</td><td>getCheckBox("CHKBOX1")</td><td>return the specify GCheckBox status</td></tr>
<tr><td>void</td><td>setScrollBar("SCRBAR1",floatObj)</td><td>set the specify GScrollBar to Float obj</td></tr>
<tr><td>Obj</td><td>getScrollBar("SCRBAR1")</td><td>return the specify GScrollBar value</td></tr>
<tr><td>void</td><td>setSwitch("SWH1",true)</td><td>set the specify GSwitch status to true or false</td></tr>
<tr><td>Bool</td><td>getSwitch("SWH1")</td><td>return the specify GSwitch status</td></tr>
<tr><td>void</td><td>loadXmlUI("/res/accounts.xml",getXmlAssist(),getEventHandler())</td><td>load xml with path String,XmlExtAssist(option),XEventHandler(option)</td></tr>
<tr><td>Bool</td><td>uiExist("COMP1")</td><td>Determine specify component is exists</td></tr>
<tr><td>void</td><td>showBar("a message")</td><td>show top bar message</td></tr>
<tr><td>void</td><td>showMsg("a message")</td><td>show a GFrame with message</td></tr>
</table>

[< Back](#dochome)

<span id="docscriptextlib"/>   

* #### Extention library

The extension function library inherit from Lib. register the extension function library before parsing the XML UI.

```
    XmlExtAssist assist = new XmlExtAssist(form);
    assist.registerGUI("test.ext.XCustomList");
    assist.addExtScriptLib(new ExScriptLib());
    String xmlStr = GToolkit.readFileFromJarAsString("/res/Frame1.xml", "utf-8");
    UITemplate uit = new UITemplate(xmlStr);
    XContainer xc = (XContainer) XContainer.parseXml(uit.parse(), assist);
    xc.build((int) form.getW(), (int) form.getH(), this);
    GFrame f1 = xc.getGui();

```

The extention library.

```
package test.ext;

import org.mini.gui.gscript.DataType;
import org.mini.gui.gscript.Interpreter;
import org.mini.gui.gscript.Lib;

import java.util.ArrayList;

public class ExScriptLib extends Lib {

    {
        methodNames.put("func1".toLowerCase(), 0);//
        methodNames.put("func2".toLowerCase(), 1);//
    }

    public DataType call(Interpreter inp, ArrayList<DataType> para, int methodID) {
        switch (methodID) {
            case 0:
                return func1(para);
            case 1:
                return func2(para);
        }
        return null;
    }

    public DataType func1(ArrayList<DataType> para) {
        String str1 = Interpreter.popBackStr(para);
        String str2 = Interpreter.popBackStr(para);
        System.out.println(str1);
        System.out.println(str2);
        return null;
    }

    public DataType func2(ArrayList<DataType> para) {
        int a = Interpreter.popBackInt(para);
        int b = Interpreter.popBackInt(para);
        return Interpreter.getCachedInt(a + b);
    }
}
```


[< Back](#dochome)

<span id="docexample"/>    

* ### Example

The example demonstrate how develop java app for iOS and Android UI layout xml file MyForm.xml These files located in /mobile/java/ExApp .

```
<form name="FORM_MAIN" w="100%" h="100%">
    <script>
        <![CDATA[
        sub change()
            red=mod(random(),255)
            green=mod(random(),255)
            blue=mod(random(),255)
            setColor("LAB_HELP",red,green,blue,255)
            setText("LAB_HELP","Only a test")
        ret
        ]]>
    </script>

    <frame name="FRAME_TEST" w="80%" h="500" align="top,hcenter" title="WINDOW">
        <label name="LAB_HELP" w="100%" h="30" align="hcenter,vcenter">Help text:</label>
        <input w="100%" h="395" multiline="1" edit="0"><![CDATA[
            This app is an example of mini_jvm, Threre are a menu and a frame .
            Touch the 'Exit to AppManager' , you will enter the AppManager, AppManager manage all app, it can upload ,download , delete app.
            1. DOWNLOAD : Put your jar in a website , then input the url of jar in AppManager, Touch 'Download' ,it would download the jar ,then update the app list.
            2. UPLOAD : The first you touch the 'Start' to open the inapp webserver, then open browser in your Desktop Computer, open 'http://phone_ip_addr:8088' , and pickup a jar in the page, upload it. NOTE: That computer and the phone must be same LAN.
            3. RUN : Touch the App name in the list, Touch 'Run' can start the app.
            4. SET AS BOOT APP : The boot app will startup when MiniPack opend.
            5. UPGRADE : AppManager will download the new jar ,url that get from config.txt in jar.
            6. DELETE : The app would be deleteted.;
            ]]>
        </input>
        <br/>
        <button name="BT_SCRIPT" h="40" onclick="change()">{Change}</button>
        <button name="BT_CANCEL" h="40">{Cancel}</button>
        <br/>
    </frame>

    <menu name="MENU_MAIN" x="0" y="90%" w="100%" h="10%" fixed="1">
        <mi name="MI_OPENFRAME" pic="/res/hello.png">{Test}</mi>
        <mi name="MI_EXIT" pic="/res/appmgr.png">{Exit}</mi>
    </menu>
</form>
```

Java source file MyApp.java

```
package test;

import org.mini.apploader.AppManager;
import org.mini.apploader.GApplication;
import org.mini.gui.*;
import org.mini.layout.UITemplate;
import org.mini.layout.XContainer;
import org.mini.layout.XEventHandler;

/**
 * @author gust
 */
public class MyApp extends GApplication {

    GForm form;
    GMenu menu;
    GFrame gframe;

    @Override
    public GForm getForm() {
        if (form != null) {
            return form;
        }
        //set the default language
        GLanguage.setCurLang(GLanguage.ID_CHN);

        //load xml
        String xmlStr = GToolkit.readFileFromJarAsString("/res/MyForm.xml", "utf-8");

        UITemplate uit = new UITemplate(xmlStr);
        UITemplate.getVarMap().put("Cancel", "CANCEL"); //replace keywork in xml
        UITemplate.getVarMap().put("Change", "Change");
        UITemplate.getVarMap().put("Test", "Test");
        UITemplate.getVarMap().put("Exit", "QUIT");
        XContainer xc = (XContainer) XContainer.parseXml(uit.parse());
        int screenW = GCallBack.getInstance().getDeviceWidth();
        int screenH = GCallBack.getInstance().getDeviceHeight();

        //build gui with event handler
        xc.build(screenW, screenH, new XEventHandler() {
            @Override
            public void action(GObject gobj) {
                String name = gobj.getName();
                switch (name) {
                    case "MI_OPENFRAME":
                        if (form.findByName("FRAME_TEST") == null) {
                            form.add(gframe);
                        }
                        break;
                    case "MI_EXIT":
                        close();
                        break;
                    case "BT_CANCEL":
                        gframe.close();
                        break;
                }
            }

            public void onStateChange(GObject gobj, String cmd) {
            }
        });
        form = (GForm) xc.getGui();
        gframe = (GFrame) form.findByName("FRAME_TEST");
        if (gframe != null) gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
        menu = (GMenu) form.findByName("MENU_MAIN");

        //process Hori screen or Vert screen
        //if screen size changed ,then ui will resized relative
        form.setSizeChangeListener((width, height) -> {
            if (gframe != null && gframe.getLayout() != null) {
                form.getLayout().reSize(width, height);
                gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
            }
        });
        return form;
    }
}

```

<div align=center><img width="112" height="199" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/doc_gui_example.jpg"/></div>


[< Back](#dochome)

<span id="docexamplegame"/>    

## Example Game

```
public class SimplePanel extends GOpenGLPanel {

    public void gl_paint(){
    }

    public void gl_init(){
    }

    public void gl_destroy(){
    }

}
```
Game Example is placed /mobile/java/ExGame   
<div align=center><img width="400" height="203" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/doc_gui_opengl.jpg"/></div>


[< Back](#dochome)

<span id="docaudio"/>    

## Audio

Audio is based on open source soft miniaudio.

```
    //init a miniaudio engine
    MaEngine maEngine = new MaEngine();

    public void startBgm(String pathInJar) {
        byte[] audio = GToolkit.readFileFromJar(pathInJar);
        if (audio != null) {
            stopBgm();
            MaSound bgm;

            MaDecoder decoder = new MaDecoder(audio);
            bgm = new MaSound(maEngine, decoder, MiniAudio.MA_SOUND_FLAG_STREAM | MiniAudio.MA_SOUND_FLAG_ASYNC);
            bgm.setVolume(bgmVolume);
            bgm.setSpatialization(false);
            bgm.setLooping(true);
            bgm.setFadeIn(1000, bgmVolume);
            bgm.start();
        }
    }
    
    //like openAL
    public void play(String audioPath, float x, float y, float z) {
        if (!soundOpen) {
            return;
        }
        MaSound snd = getMaSound(audioPath);
        if (snd != null) {
            snd.setSpatialization(true);// support 3d audio
            snd.setAttenuationModel(MiniAudio.ma_attenuation_model_linear);
            snd.setMinDistance(minDistance);
            snd.setMaxDistance(maxDistance);
            snd.setPosition(x, y, z);
            snd.start();
        }
    }
```


[< Back](#linkhome)

<span id="linklicense"/>    

## License

License:    MIT

Gust , digitalgust@163.com .   
