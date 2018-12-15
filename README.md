
![](/screenshot/mini_jvm_64.png)  
[![Build Status](https://travis-ci.org/digitalgust/miniJVM.svg?branch=master)](https://travis-ci.org/digitalgust/miniJVM)
# miniJVM

  Develop iOS Android app in java, Cross platform java virtual machine , the minimal jvm . 
  
## Features:  

  * Jvm Build pass: iOS / Android / mingww64 32-64bit / cygwin / MSVC 32-64bit / MacOS /  Linux  .   
  * No dependence Library .  
  * Minimal memory footprint .  
  * Minimal binary ,300 - 800 KB jvm.  
  * Minimal runtime classlib .   
  * Support java5+ class file .  
  * Support embedded java source compiler(janino compiler) .  
  * Java native method  .  
  * Java garbage collection .   
  * Java remote debug supported, JDWP Spec .  
  
## iOS/Android Platform Extended Features:  
  * OpenGL ES 3.0 .  
  * GUI similar with swing .  
  * Audio Playback and Capture .  
  * Take photo from Camera or Album .  
  * Save and Load file from mobile storage .  
  These java api of features can be found in mobile/java/guilib/ , api platform independent .  

## iOS app IM Demo :
  <div align=center><img width="224" height="398" src="/screenshot/demo.gif"/></div>
   This demo can be download from : http://bb.egls.cn:8080/down/BiBiX.jar , First install AppManager from :
   /binary/ios/MiniPack.ipa or /binary/android/MiniPack.apk 
   then download demo in MiniPack and run the app. 

## Architecture:  

  <div align=center><img width="480" height="386" src="/screenshot/arch.png"/></div>


## Directories: 
> /   
>> binary/-------- minijvm binary for pc (win32/win64/mac/linux)  
>> mini_jvm/------ minijvm c source   
>> javalib/------- minijvm runtime class library   
>>
>> ex_lib/   
>>> jni_gui/------ desktop computer native gui jni module   
>>> luaj/--------- lua java    
>>
>> mobile/        
>>> c/------------- java native lib, glfm framework, gui jni, glfmapp    
>>> java/guilib---- java jni for above **/mobile/c** native gui lib    
>>> java/ExApp----- java app entry point    
>>> iosapp/-------- iOS launcher     
>>> androidapp/---- Android launcher     
>>> assets/resfiles/- java app resource, font files, jar files ,pic etc.  
>>
>> test/   
>>> javalib_test/- test **/javalib** classes.   
>>> jni_test/----- jni example     
>>> jvm_ios/------ ios swift test project ,only test the jvm.      
>>> jvm_macos/---- macosX test project, only test jvm.      
>>> jvm_vs/------- virtual studio test project, only test jvm.      
  
 * **/mini_jvm** is an independent small and fast jvm interpreter, Need **/javalib** runtime class library only, it run on Win/Mac/Linux/iOS/Android.     
 * **/javalib** is the jvm foundation runtime class library, ex *java.lang* , *java.util* ,and extended classes *org.mini* for file reflect and network etc, this project generate minijvm_rt.jar , copy it into **/mobile/assets/resfiles**  .   
 * **/mobile/iosapp** **/mobile/androidapp** are iOS/Android launcher program, it include minijvm source and native gui function, java call gui library with jni.       
 * **/mobile/java/guilib** is a gui library ,it dependent on native gui library ,that include openGLES glad, glfm, nanovg, stb lib etc , this project generate glfm_gui.jar , copy it into **/mobile/assets/resfiles** .     
 * **/mobile/java/ExApp** is an example of mobile app, it run on iOS and Android platform.  
   

## How to develop iOS/Android app in java:   
   Write java code once , running both iOS and Android.   
   1. Can develop app by Eclipse, Netbeans or Intelli Idea , or any ide .   
   2. Add  **/mobile/assets/resfiles/minijvm_rt.jar** and **/mobile/assets/resfiles/glfm_gui.jar**  as library   
   * Open ExApp project in NetBeans    
   * Write your code like example **/mobile/java/ExApp/src/test/MyApp.java**    
   * Add your resource to **/mobile/java/ExApp/resource/res/** , such as audio/image etc.     
   * Ensure **/mobile/java/ExApp/src/config.txt** configure right.     
   * Build **/mobile/java/ExApp** generate jar file    
   * Install **/binary/ios/MiniPack.ipa** for iPhone device , (Enterprise distrbute version, need Verify app, Setting->General->Device Management->EGLS Technology ltd->Verify App), or **/binary/android/MiniPack.apk** for Android device , These two binary built from **/mobile/iosapp/**  and **/mobile/java/androidapp**, you can build it yourself.    
   * Touch the app icon to open MiniPack app, start inapp webserver , in the same lan, open browser of desktop computer, input the url of your phone , http://phone_ip:8088/   
   * In browser, pickup the jar ,and upload , just it would in the manager list   
   * you can run it now . 
    <div align=center><img width="672" height="398"   src="/screenshot/appmgr.png"/></div>
  
  
## How to Remote debug:  
  Desktop Computer : Run mini_jvm with flag: -Xdebug for debug mode .  
  iOS/Android simulator : no attached operation.  
  iOS/Android device : check the device ip address from Setting.  
  mini_jvm jdwp listen port is 8000.   
   * Intelli idea : open the java project , menu Run .> Edit Configurations , + remote , Transport : socket , Debugger mode : attach , host is your mini_jvm running at host ip and port ,ex. "localhost:8000" .  
   * Eclipse : configuration  like as idea .  
   * Netbeans : open java project ,  menu Debug .> connect to Debugger, Connector : SocketAttach , host is your mini_jvm running at the host and port, ex. "localhost:8000" , Timeout: 10000 .  
  Then you can setup breakpoint or pause mini_jvm and watch variable's value .  
  


## How to embed Java Compiler to mini_jvm:  
   Copy /binary/lib/janino.jar to mini_jvm lib directory, and add the jar to classpath.   
   using Janino compiler,  can find in example in binary folder.   
   the compile command like :
```
win:
mini_jvm -cp ../lib/minijvm_rt.jar;../lib/janino.jar;../lib/commons-compiler.jar org.codehaus.janino.Compiler  ../res/BpDeepTest.java
posix:
./mini_jvm -cp ../lib/minijvm_rt.jar:../lib/janino.jar:../lib/commons-compiler.jar org.codehaus.janino.Compiler  ../res/BpDeepTest.java
```

Janion compiler is not the full java compiler, see [limitation](http://janino-compiler.github.io/janino/#limitations) , like :
```
List<String> list=new ArrayList(); 
list.add("abc");
String s=(String)list.get(0);//can't ignore (String) cast qualifier.   
```   
   


## Referenced project and technology:   
   [Sun CLDC](http://www.oracle.com/technetwork/java/cldc-141990.html)  :reference    
   [Miniz](https://github.com/richgel999/miniz) :for read jar files    
   [GLFM](https://github.com/brackeen/glfm) :for cross platform (android/ios) GUI   
   [Nanovg](https://github.com/memononen/nanovg)  :for GUI paint function   
   [Stb](https://github.com/nothings/stb) :for GUI truetype font and image  
   [Glad](https://github.com/Dav1dde/glad)  :for replace openGL/GLES head file   
   [GLFW](https://github.com/glfw/glfw)  :for pc cross platform GUI   
   [Dirent](https://github.com/tronkko/dirent)  :for win vc file and directory access    
   [Tinycthread](https://github.com/tinycthread/tinycthread)  :for cross platform thread   
   [JRegex](https://github.com/digitalgust/jregex)  :for java String regex match     
   [Janino](http://janino-compiler.github.io/janino/)  :for compile java source file     
   [Mini_al](https://github.com/dr-soft/mini_al)  :for java audio playback and capture     


## Development IDE:  
  C code:   JetBrains CLion ,Xcode ,Virtual studio .  
  Swift code/Object c:    XCode , LLVM 9 .  
  Java code:    Netbeans 8.0 ,jdk 1.8 .  
  android project:  Android Studio ,Android SDK 

 
## Build GUI application, depend on openGL or openGLES     
   * iOS/Android system build with GLFM  (/mobile/iosapp/  ,  /mobile/androidapp/)       
   * Window system build with GLFW   (/ex_lib/gui_jni/)      
   * GUI build on Nanovg          

  
## Example of mobile application

There are two class demo how develop java app for iOS and Android, one is App main class, the other is an GuiApp
```
package test;

import org.mini.apploader.AppManager;
import org.mini.gui.*;
import org.mini.gui.event.*;
import org.mini.gui.impl.GuiCallBack;

/**
 *
 * @author gust
 */
public class MyApp extends GApplication {

    GForm form;
    GMenu menu;
    GFrame gframe;

    @Override
    public GForm getForm(GApplication appins) {
        if (form != null) {
            return form;
        }
        GuiCallBack ccb = GuiCallBack.getInstance();
        GLanguage.setCurLang(GLanguage.ID_CHN);
        form = new GForm(ccb);

        form.setFps(30f);
        long vg = form.getNvContext();

        int menuH = 80;
        GImage img = GImage.createImageFromJar("/res/hello.png");
        menu = new GMenu(0, form.getDeviceHeight() - menuH, form.getDeviceWidth(), menuH);
        menu.setFixed(true);
        GMenuItem item = menu.addItem("Hello World", img);
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                if (gframe != null) {
                    gframe.close();
                }
                gframe = getFrame1();
                form.add(gframe);
                gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
            }
        });

        img = GImage.createImageFromJar("/res/appmgr.png");
        item = menu.addItem("Exit to AppManager", img);
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                AppManager.getInstance().active();
            }
        });

        form.add(menu);
        return form;
    }

    public GFrame getFrame1() {

        GFrame gframe = new GFrame("Hello World", 50, 50, form.getDeviceWidth() * .8f, (form.getDeviceHeight() - menu.getH()) * .7f);
        GViewPort parent = gframe.getView();
        float pad = 8;
        float x = pad, y = 10;
        float btnH = 28;

        String conttxt = "  This app is an example of mini_jvm, Threre are a menu and a frame .\n"
                + "  Touch the 'Exit to AppManager' , you will enter the AppManager, AppManager manage all app, it can upload ,download , delete app.\n"
                + "  1. DOWNLOAD : Put your jar in a website , then input the url of jar in AppManager, Touch 'Download' ,it would download the jar ,then update the app list.\n"
                + "  2. UPLOAD : The first you touch the 'Start' to open the inapp webserver, then open browser in your Desktop Computer, open 'http://phone_ip_addr:8088' , and pickup a jar in the page, upload it.  NOTE: That computer and the phone must be same LAN.\n"
                + "  3. RUN : Touch the App name in the list, Touch 'Run' can start the app.\n "
                + "  4. SET AS BOOT APP : The boot app will startup when MiniPack opend. \n"
                + "  5. UPGRADE : AppManager will download the new jar ,url that get from config.txt in jar.\n"
                + "  6. DELETE : The app would be deleteted.\n";
        GTextBox cont = new GTextBox(conttxt, "Contents", x, y, parent.getW() - x * 2, parent.getH() - pad * 2 - btnH - y);
        cont.setEditable(false);
        parent.add(cont);
        y += cont.getH() + pad;

        GButton bt2 = new GButton("Cancel", x + 170, y, 110, btnH);
        bt2.setBgColor(0, 0, 0, 0);
        bt2.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                gobj.getForm().remove(gframe);
            }
        });
        parent.add(bt2);

        return gframe;
    }

}


```
##   Screen shot   :   
  * Windows mini_jvm gui    
    <div align=center><img width="433" height="336" src="/screenshot/win.png"/></div>
  * Macos mini_jvm gui    
  <div align=center><img width="433" height="336" src="/screenshot/mac.png"/></div> 
  * Linux mini_jvm gui    
  <div align=center><img width="433" height="336" src="/screenshot/centos.png"/></div>

  
  
## License
License:	Public domain


Gust , zhangpeng@egls.cn , Technology and production manage in EGLS ltd. EGLS is a game develop company in China .
