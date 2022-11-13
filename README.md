![](/screenshot/mini_jvm_64.png)  
[![Build Status](https://travis-ci.org/digitalgust/miniJVM.svg?branch=master)](https://travis-ci.org/digitalgust/miniJVM)

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

## Features:

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

## iOS/Android Platform Extended Features:

* OpenGL ES 3.0
* Swing like gui lib, XML gui layout(html like)
* Audio Playback and Capture
* Take photo from Camera or Album
* Save and Load file from mobile device
* Api compatible with miniJVM desktop platform, app can running on desktop platform

## MiniJVM on Web:

MiniJVM on web build by Starcommander. [Source](https://github.com/Starcommander/miniJVM)
[MiniJVM Web demo](https://java-on-web.org/examples/)

<span id="linkdemo"/>

## MiniJVM gui Demo

  <div align=center><img width="224" height="398" src="/screenshot/demo.gif"/><img width="224" height="398" src="/screenshot/g3d.gif"/></div>

* Instant Message app Demo , [Source](https://github.com/digitalgust/BiBiX)
* 3D game app Demo, [Source](https://github.com/digitalgust/g3d)
* Mobile platform : First build and install AppManager for iOS /mobile/iosapp , Or build and install for Android /mobile/androidapp ,then download demo in AppManager and run these app
* Desktop computer: /binary/win_64 , /binary/macos , /binary/win32 , /binary/centos_x64 run test.sh

<span id="linkarch"/>

## Architecture:

  <div align=center><img width="540" height="350" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/arch.png"/></div>

<span id="linkchangelog"/>

## Changelog:

2021.03. Add j2c module, it 's a tool for convert minijvm java source code to c source code , then build it as native application , support desktop and mobile platform .   
2020.12. Add build script and release v2.1.   
2020.10. Refactor source and remove binary in repository.   
2020.10. Https supported.   
2020.03. Add xml layout for gui system, add 3D game demo for minijvm, fix jdwp debug for jetbrain idea.               
2019.12. Bind cross platform awtk ui system , see [awtk-minijvm](https://github.com/digitalgust/miniJVM/tree/master/desktop/awtk_gui)   
2019.12. Jit enabled, it based on sljit project   
2019.10. Jit is developing   
2018.12. Optimize performance     
2017.09. miniJVM start

<span id="linkbuildmobile"/>

## Build for iOS/Android platform:

Write java code once , running on all of iOS / Android / MacOSX / Win / Linux platforms   
There were not essential jar file pre-built, so build these jar file first   
Develop IDE:  Eclipse, Netbeans or Jetbrain Intelli Idea

1. Run script **/mobile/build_jar.sh** or **/mobile/build_jar.bat** to generted jars.     
   Or

> Build maven projects /minijvm/java copy to  **/mobile/assets/resfiles/minijvm_rt.jar**      
> Build maven projects /mobile/java/glfm_gui, copy to  **/mobile/assets/resfiles/glfm_gui.jar**       
> Build maven projects /mobile/java/ExApp, copy to  **/mobile/assets/resfiles/ExApp.jar**   
> Maybe you can change   **/mobile/java/ExApp/src/main/java/test/MyApp.java**    , Add your resource to **/mobile/java/ExApp/src/main/resource/res/** , such as audio or image etc, Configure **/mobile/java/ExApp/src/main/config.txt** for icon ,version, boot class, etc

2. XCode open **/mobile/iosapp** ,setup developer account in Signing&Capabilities , build and install app to Device , verify app before running app (Setting->General->Device Management->{Developer account}->Verify App->Trust)
3. Android Studio open **/mobile/androidapp**  build and install app to Android device
4. AppManager is running, It can start a in-app webserver for upload app, and download app from a website too

  <div align=center><img width="672" height="398"   src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/appmgr.png"/></div>

<span id="linkbuilddesktop"/>

## Build for Windows/Linux/MacOS platform:

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

## How to debug source:

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

## Project based miniJVM:

[Awtk-minijvm](https://github.com/zlgopen/awtk-minijvm)  :[AWTK](https://github.com/zlgopen/awtk) cross platform native ui bind to minijvm   
[LWJGUI-Mobile](https://github.com/orange451/LWJGUI-Mobile) : java LWJGL UI library        
[BiBiX](https://github.com/digitalgust/BiBiX) : java instantial messager        
[G3d](https://github.com/digitalgust/g3d) : java 3d game demo        
[MiniJVM web demo](https://java-on-web.org/examples/) :  miniJVM on web demo

<span id="linkref"/>

## Referenced project and technology:

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

## Development IDE usage:

C / ObjC:   JetBrains CLion ,Xcode ,Virtual studio   
Swift :    XCode    
Java :    Jetbrain Idea, Netbeans   
Android :  Android Studio

## Build GUI application, depend on OpenGL or OpenGLES

* iOS/Android system build with GLFM  (/mobile/iosapp/ , /mobile/androidapp/)
* Window system build with GLFW   (/desktop/glfw_gui/)
* GUI build with Nanovg
* Based on OpenGL 3.3 and OpenGLES 3.0 as default

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

## Screen shot   :

* Windows mini_jvm gui

  <div align=center><img width="433" height="336" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/win.png"/></div>
  * Macos mini_jvm gui    
  <div align=center><img width="433" height="336" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/mac.png"/></div> 
  * Linux mini_jvm gui    
  <div align=center><img width="433" height="336" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/centos.png"/></div>
  * Web mini_jvm gui    
  <div align=center><img width="433" height="336" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/web-glfw.png"/></div>

## Directories:

> /
>> binary/-------- minijvm binary  for build(win32/win64/mac/linux/android/ios)  
> > minijvm/
>>> c/------------ minijvm c source    
> > > java/--------- minijvm runtime library
>>
>> desktop/
>>> glfw_gui/------ desktop computer gui/audio native module
>>
>> j2c/------------ java to c translator.
>>> app/----------- native support source.    
> > > build/--------- build files for platforms.    
> > > translator/---- tool for convert minijvm java source to c source.
>>
>> mobile/
>>> c/------------- java native lib, glfm framework, gui jni, glfmapp    
> > > java/glfm_gui-- mobile platform native gui lib    
> > > java/ExApp----- java app entry point    
> > > iosapp/-------- iOS launcher     
> > > androidapp/---- Android launcher     
> > > assets/resfiles/- mobile app resource, font files, jar files ,pic ,audio etc.
>>
>> test/
>>> jni_test/------ jni example    
> > > jvm_ios/------ ios swift test project       
> > > jvm_macos/---- macosX test project      
> > > jvm_vs/------- windows virtual studio test project      
> > > minijvm_test/-- test case of **/minijvm/java**

<span id="dochome"/>    

# Documentation

[GUI Components](#docgui)

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
* [Example](#docexample)

[GUI layout](#doclayout)

* [XY layout](#doclayoutxy)
* [XML flow layout](#doclayoutflow)
* [XML table layout](#doclayouttable)

[GUI script](#docscript)

* [Syntax](#docscriptsyntax)
* [Stdlib api](#docscriptstdlib)
* [GUI Scriptlib api](#docscriptguilib)
* [Extention library](#docscriptextlib)

<span id="docgui"/>

## GUI Component

<span id="docoutline"/>

* ### Outline

<div align=center><img width="600" height="160" src="/screenshot/doc_gui_arch.png"/></div>



GApplication is a GUI application, it MUST contain a GForm. it starts by AppManager.

Visible UI component is a GObject.

GForm is an OS window or a phone screen, a top visible UI component.

GFrame is a window GContainer ,can be close, drag it move.

All UI components are son of GContainer.

GContainer can be son of other GContainer.

Set a GStyle for all GObject by GToolkit.setStyle().

There are two methods to new a GObject , one is manual new an object, the other is XML layout.

GUI XML layout like html, and there is a script like javascript for html.

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

This is a simplest app, override getForm() to export a GForm to AppManager, Device show the form ,and trans events (Keyboard/Mouse/TouchScreen/etc) to the form.
<div align=center><img width="300" height="150" src="/screenshot/doc_gui_form.jpg"/></div>


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

There is a frame show on the form. Attributes:   
**closable**: 1: the frame can be close. 0:can't close it.        
**title**: String , the frame's title.        
**onclose**: String , the script function call, this function must in the frame's script partion.        
**oninit**: String , the script function call, this function must in the frame's script partion.
<div align=center><img width="300" height="150" src="/screenshot/doc_gui_frame.jpg"/></div>


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
<div align=center><img width="300" height="150" src="/screenshot/doc_gui_menu.jpg"/></div>


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
**preicon**: utf8 char , an emoji char as icon front of the text.

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
**pic**: String , the path of image in jar.
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

GViewSlot is a multi-slot container. Each slot is a container. Only one slot can be visible at once. It can be switched to another slot by dragging and dropping. There are three slots in the above example. One is a ViewPort, the second is a Panel, and the third is a table. Each container has a "move" attribute whose value indicates which direction it can slide.    
If not specify the "w" width, the default width is parent's width.   
If not specify the "h" height, the default height is parent's height.

Attributes:   
**scroll**: h: the viewslot sliding direction is HORIZONTAL. v: VERTICAL

[< Back](#dochome)

<span id="docgtextinput"/>    

* ### GTextBox GTextField

```
<input name="WRITE_BOX" w="100%" h="20%" multiline="1" edit=1>default text</input>
```

If not specify the "w" width, the default width is image width.   
If not specify the "h" height, the default height is image height.

Attributes:   
**multiline**: int , 1: multiple row input box. 0: single row input field   
**edit**: int , 1: edit enable. 0: edit disable   
**style**: search : the search style   
**hint**: String : hint string in the input area,if text is inputed ,the hint invisible    
**union**: String : another UI component name, the input box don't lost focus when union component clicked   
**password**: int , 1: input text is hidden with *, 0: not password input

[< Back](#dochome)

<span id="doccustom"/>    

* ### Custom UI Component

Customized components can be extended to meet requests, and customized components can also be directly layout in the xml UI.

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
<tr></td><td>fly</td><td>whether or not the UI component can be drag to move</td><td>fly="0"</td></tr>
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

<div align=center><img width="224" height="398" src="/screenshot/doc_gui_example.jpg"/></div>

[< Back](#dochome)


<span id="doclayout"/>   

## GUI Layout

All UI containers support the following three layout methods, which can be mixed. In XML, flow layout and table layout are usually used at the same time. Layout is just a concept, and the code does not need to specify which layout to use. The layout manager automatically layout according to the description in XML.

[< Back](#dochome)


<span id="doclayoutxy"/>   

* ### XY layout

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

* ### XML flow layout
  The flow layout does not specify the fixed position of the UI components, and the components are arranged in rows. If the next UI component cannot be accommodated in current row, the component will be placed in a new row. The row height is the height of the highest component in this row.

  If need force a line break, can use <br/> to break row.

[< Back](#dochome)


<span id="doclayouttable"/>   

* ### XML table layout
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

<div align=center><img width="500" height="261" src="/screenshot/doc_gui_tablelayout.jpg"/></div>


[< Back](#dochome)



<span id="docscript"/>   

## GUI Script

A simple scripting system is embedded in XML, and scripts can process UI component or applications. This script is similar to what javascript for HTML.    
The script can be extended to enhance the interaction between the script and the java system.

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

On the button BT_CHANGE clicked, the script function change() would be call, the func change the label LAB_HELP text and color.   
Script onOpen() would be called when the frame is openning.

[< Back](#dochome)


<span id="docscriptsyntax"/>   

* ### Script Syntax

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

* ### Stdlib api

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

* ### GUI Scriptlib api

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

* ### Extention library

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

## License

License:    MIT

Gust , digitalgust@163.com .
