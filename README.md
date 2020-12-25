
![](/screenshot/mini_jvm_64.png)  
[![Build Status](https://travis-ci.org/digitalgust/miniJVM.svg?branch=master)](https://travis-ci.org/digitalgust/miniJVM)
# miniJVM

  Develop iOS Android app in java, Cross platform java virtual machine , the minimal jvm . 
  
## Features:  

  * Jvm Build pass: iOS / Android / mingw-w64 32|64bit / cygwin / MSVC 32|64bit / MacOS /  Linux     
  * No dependence Library   
  * Minimal memory footprint   
  * Minimal binary, embedded jvm  
  * Minimal bootstrap classlib    
  * Support embedded java source compiler(janino compiler)   
  * Jit support     
  *  Low latency  java garbage collection    
  * Java remote debug supported, JDWP Spec   
  
## iOS/Android Platform Extended Features:  
  * OpenGL ES 2.0 / 3.0   
  * Swing like gui lib, XML gui html like layout    
  * Audio Playback and Capture   
  * Take photo from Camera or Album   
  * Save and Load file from mobile storage    
  * Api compatible with miniJVM desktop platform,  app can running on desktop platform   
  * Compile minijvm java source code to C boost performance.  Source on [java2c](https://github.com/digitalgust/java2c) .  

## MiniJVM gui Demo

  <div align=center><img width="224" height="398" src="/screenshot/demo.gif"/><img width="224" height="398" src="/screenshot/g3d.gif"/></div>

  * Instant Message app  Demo , source on :https://github.com/digitalgust/BiBiX     
  * BiBiX binary can be download from : http://bb.egls.cn:8080/down/BiBiX.jar     
  * 3D game app Demo, source on :https://github.com/digitalgust/g3d     
  * G3D binary can be download from : http://bb.egls.cn:8080/down/g3d.jar     
  * Mobile platform : First build and install AppManager for iOS /mobile/iosapp , Or build and install for Android /mobile/androidapp ,then download demo in AppManager and run these app     
  * Desktop computer: /binary/win_64  , /binary/macos , /binary/win32 , /binary/centos_x64   run test.sh 


## Architecture:  

  <div align=center><img width="540" height="350" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/arch.png"/></div>


## Changelog:
   2020.12.  Add build script and release v2.1.   
   2020.10.  Refactor source and remove binary in repository.   
   2020.10.  Https supported.   
   2020.03.  Add xml layout for gui system, add 3D game demo for minijvm, fix jdwp debug for jetbrain idea.               
   2019.12.  Bind cross platform awtk ui system , see [awtk-minijvm](https://github.com/digitalgust/miniJVM/tree/master/desktop/awtk_gui)   
   2019.12.  Jit enabled, it based on sljit project   
   2019.10.  Jit is developing   
   2018.12.  Optimize performance     
   2017.09.  miniJVM start   


## Build for iOS/Android platform:   
   Write java code once , running on all of iOS / Android / MacOSX / Win / Linux platforms   
   There were not essential jar file pre-built, so build these jar file first   
   Develop IDE:  Eclipse, Netbeans or Jetbrain Intelli Idea     
   * Run script **/mobile/build_jar.sh** or **/mobile/build_jar.bat** to generted jars.     
    Or
>>    Build maven projects /minijvm/java copy to  **/mobile/assets/resfiles/minijvm_rt.jar**      
>>    Build maven projects /mobile/java/glfm_gui, copy to  **/mobile/assets/resfiles/glfm_gui.jar**       
>>    Build maven projects /mobile/java/ExApp, copy to  **/mobile/assets/resfiles/ExApp.jar**   
>>    Maybe you can change   **/mobile/java/ExApp/src/main/java/test/MyApp.java**    , Add your resource to **/mobile/java/ExApp/src/main/resource/res/** , such as audio or image etc,  Configure **/mobile/java/ExApp/src/main/config.txt** for icon ,version, boot class, etc   
   * XCode open **/mobile/iosapp** ,setup developer account in Signing&Capabilities , build and install app to Device , verify app before running app (Setting->General->Device Management->{Developer account}->Verify App->Trust)      
   * Android Studio open **/mobile/androidapp**  build and install app to Android device     
   * AppManager is running, It can start a in-app webserver for upload app, and download app from a website too    
    <div align=center><img width="672" height="398"   src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/appmgr.png"/></div>   
  
  
## Build for Windows/Linux/MacOS platform:     
  There were not essential jar file pre-built, so build these jar file first 
  * Run script **/binary/build_jar.sh** or **/binary/build_jar.bat** to generted jars     
   Or   
>>   Build java bootstrap classes  **/minijvm/java**  , Maven build jar and copy to /binary/lib/minijvm_rt.jar    
>>   Build gui classes **/desktop/glfw_gui/java** , Maven build jar and copy to /binary/libex/glfw_gui.jar     
>>   Build console test case classes **/test/minijvm_test** , Maven build jar and copy to /binary/libex/minijvm_test.jar     
>>   Build gui test app classes **/mobile/java/ExApp** , Maven built jar and copy to /binary/{platform}/apps/ExApp.jar    

  * Run **/binary/build_mac_linux.sh** or **/binary/build_wini686.bat** or  **/binary/build_winx64.bat** to generted binaries    
    Or 
>>    Build gui jni c dynamic library /desktop/glfw_gui/c by cmake    
>>    Build minijvm /minijvm/c by cmake      
  * Run test script /binary/{platform}/test.sh | test.bat    
 
   
## How to Remote debug:  
  Desktop platform : Run mini_jvm with flag: -Xdebug for debug mode 
  iOS/Android simulator : change /mobile/c/glfmapp/main.c , set **jvm->jdp_enable = 1** after **jvm_create()**   
  iOS/Android device : check the device ip address from General Setting -> wifi ->(i)  
  mini_jvm jdwp listen port is 8000   
   * Intelli idea : open the java project , menu Run -> Edit Configurations , + remote , Transport : socket , Debugger mode : attach , host is your mini_jvm running at host ip and port ,ex "localhost:8000"   
   * Eclipse : configuration  like as idea   
   * Netbeans : open java project ,  menu Debug -> connect to Debugger, Connector : SocketAttach , host is your mini_jvm running at the host and port, ex. "localhost:8000" , Timeout: 10000   
  Then you can setup breakpoint or pause mini_jvm and watch variable's value   
  


## How to use Embed java compiler in mini_jvm:  
   The third compiler [Janino](http://janino-compiler.github.io/janino/)     
   Download and build it to  /binary/libex/janino.jar  and   /binary/libex/commons-compiler.jar    
   Type compile command :  
```
#win:
mini_jvm -bootclasspath ../lib/minijvm_rt.jar  -cp ../libex/janino.jar;../libex/commons-compiler.jar org.codehaus.janino.Compiler  ../res/BpDeepTest.java
#posix:
./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/janino.jar:../libex/commons-compiler.jar org.codehaus.janino.Compiler  ../res/BpDeepTest.java
```

Janino compiler is not the full java compiler, see [limitation](http://janino-compiler.github.io/janino/#limitations) , like :
```
List<String> list=new ArrayList(); 
list.add("abc");
String s=(String)list.get(0);//can't ignore (String) cast qualifier.   
```   
   
## Project based miniJVM:   
   [Awtk-minijvm](https://github.com/zlgopen/awtk-minijvm)  :[AWTK](https://github.com/zlgopen/awtk) cross platform native ui bind to minijvm   
   [LWJGUI-Mobile](https://github.com/orange451/LWJGUI-Mobile) : java LWJGL UI library        
   [BiBiX](https://github.com/digitalgust/BiBiX) : java instantial messager        
   [G3d](https://github.com/digitalgust/g3d) : java 3d game demo        



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
  C / ObjC:   JetBrains CLion ,Xcode ,Virtual studio .  
  Swift :    XCode  .  
  Java :    Jetbrain Idea, Netbeans  ,jdk  .  
  Android :  Android Studio .

 
## Build GUI application, depend on OpenGL or OpenGLES     
   * iOS/Android system build with GLFM  (/mobile/iosapp/  ,  /mobile/androidapp/)       
   * Window system build with GLFW   (/desktop/glfw_gui/)      
   * GUI build with Nanovg          
   * Based on OpenGL 3.3 and OpenGLES 3.0 as default
  
  
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
  
  
## Example of mobile gui application    

The example demonstrate how develop java app for iOS and Android 
UI layout xml file MyForm.xml

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
        UITemplate.getVarMap().put("Test", "test");
        UITemplate.getVarMap().put("Exit", "QUIT");
        XContainer xc = (XContainer) XContainer.parseXml(uit.parse());
        int screenW = GCallBack.getInstance().getDeviceWidth();
        int screenH = GCallBack.getInstance().getDeviceHeight();

        //build gui with event handler
        xc.build(screenW, screenH, new XEventHandler() {
            public void action(GObject gobj, String cmd) {
                String name = gobj.getName();
                switch (name) {
                    case "MI_OPENFRAME":
                        if (form.findByName("FRAME_TEST") == null) {
                            form.add(gframe);
                        }
                        break;
                    case "MI_EXIT":
                        AppManager.getInstance().active();
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
            if (gframe != null && gframe.getXmlAgent() != null) {
                ((XContainer) form.getXmlAgent()).reSize(width, height);
                gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
            }
        });
        return form;
    }
}

```
<div align=center><img width="224" height="398" src="/screenshot/myapp.jpg"/></div>

## Third liberies   

* ### Janino java compiler
Project:   [Janino](http://janino-compiler.github.io/janino/)       
Janino is a super-small, super-fast Java compiler.   
Janino can not only compile a set of source files to a set of class files like JAVAC, but also compile a Java expression, a block, a class body, one .java file or a set of .java files in memory, load the bytecode and execute it directly in the same JVM.   
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
Lightweight, fast, Java-centric Lua interpreter written for JME and JSE, with string, table, package, math, io, os, debug, coroutine & luajava libraries, JSR-223 bindings, all metatags, weak tables and unique direct lua-to-java-bytecode compiling.   
Download jars :    
[luaj.jar](https://github.com/digitalgust/digitalgust.github.io/blob/main/lib/luaj.jar?raw=true)    
```
    mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/luaj.jar Sample
```


##   Screen shot   :   
  * Windows mini_jvm gui    
  <div align=center><img width="433" height="336" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/win.png"/></div>
  * Macos mini_jvm gui    
  <div align=center><img width="433" height="336" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/mac.png"/></div> 
  * Linux mini_jvm gui    
  <div align=center><img width="433" height="336" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/centos.png"/></div>


## Directories: 
> /   
>> binary/-------- minijvm binary  for build(win32/win64/mac/linux/android/ios)  
>> minijvm/    
>>> c/------------ minijvm c source    
>>> java/--------- minijvm runtime library     
>>
>> desktop/   
>>> glfw_gui/------ desktop computer gui/audio native module   
>>
>> mobile/        
>>> c/------------- java native lib, glfm framework, gui jni, glfmapp    
>>> java/glfm_gui-- mobile platform native gui lib    
>>> java/ExApp----- java app entry point    
>>> iosapp/-------- iOS launcher     
>>> androidapp/---- Android launcher     
>>> assets/resfiles/- mobile app resource, font files, jar files ,pic ,audio etc.  
>>
>> test/   
>>> jni_test/------ jni example    
>>> jvm_ios/------ ios swift test project       
>>> jvm_macos/---- macosX test project      
>>> jvm_vs/------- windows virtual studio test project      
>>> minijvm_test/-- test case of **/minijvm/java**      
  
     
  
## License
License:	MIT


Gust , digitalgust@163.com , works at EGLS ltd. EGLS is a game development and distribution company .
