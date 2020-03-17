
![](/screenshot/mini_jvm_64.png)  
[![Build Status](https://travis-ci.org/digitalgust/miniJVM.svg?branch=master)](https://travis-ci.org/digitalgust/miniJVM)
# miniJVM

  Develop iOS Android app in java, Cross platform java virtual machine , the minimal jvm . 
  
## Features:  

  * Jvm Build pass: iOS / Android / mingww64 32-64bit / cygwin / MSVC 32-64bit / MacOS /  Linux  .   
  * No dependence Library .  
  * Minimal memory footprint .  
  * Minimal binary, embedded jvm.  
  * Minimal runtime classlib .   
  * Support java5-8 class file .
  * Support embedded java source compiler(janino compiler) .  
  * Jit supported  .
  * Java garbage collection .   
  * Java remote debug supported, JDWP Spec .  
  
## iOS/Android Platform Extended Features:  
  * OpenGL ES 3.0 .  
  * Swing like gui .  
  * Audio Playback and Capture .  
  * Take photo from Camera or Album .  
  * Save and Load file from mobile storage .   
  * Api compatible with miniJVM desktop platform, can running on pc .  

## mobile app IM Demo BiBiX , source at :https://github.com/digitalgust/BiBiX 
  <div align=center><img width="224" height="398" src="/screenshot/demo.gif"/></div>    
   This demo can be download from : http://bb.egls.cn:8080/down/BiBiX.jar ,    
  * Mobile platform : First install AppManager from iOS for binary/ios/MiniPack.ipa , Android for /binary/android/MiniPack.apk ,then download demo in AppManager and run the app.     
  * Desktop computer: /binary/win_64  , /binary/macos , /binary/win32 , /binary/centos_x64   run test.sh 


## Architecture:  

  <div align=center><img width="540" height="350" src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/arch.png"/></div>


## Changelog
   2019.12.  Bind cross platform awtk ui system , see [awtk-minijvm](https://github.com/digitalgust/miniJVM/tree/master/desktop/awtk_gui)   
   2019.12.  Jit enabled, it based on sljit project   
   2019.10.  Jit is developing   
   2018.12.  Optimize performance     
   2017.09.  miniJVM start   


## How to develop iOS/Android app in java:   
   Write java code once , running on all of iOS / Android / MacOSX / Win / Linux platforms.   
   * Can develop app by Eclipse, Netbeans or Intelli Idea , or any IDE .   
   * Build maven projects /minijvm/java and /mobile/java/glfm_gui, it world copy generated jar to  **/mobile/assets/resfiles/minijvm_rt.jar** and **/mobile/assets/resfiles/glfm_gui.jar**     
   * Open maven project ExApp in IDE ,or create new project same as ExApp   
   * Write your code like example **/mobile/java/ExApp/src/main/java/test/MyApp.java**    
   * Add your resource to **/mobile/java/ExApp/src/main/resource/res/** , such as audio or image etc.     
   * Configure **/mobile/java/ExApp/src/main/config.txt** for icon ,version, boot class, etc .     
   * Build ExApp project , it would copy ExApp.jar to **/mobile/assets/resfiles/ExApp.jar**   
   * Install **/binary/ios/MiniPack.ipa** for iPhone device , (Enterprise distrbute version, need Verify app, Setting->General->Device Management->EGLS Technology ltd->Verify App), or **/binary/android/MiniPack.apk** for Android device , These two binary built from **/mobile/iosapp/**  and **/mobile/java/androidapp**, you can build it yourself.    
   * Touch the app icon to open MiniPack app, you would see the ExApp is running (the left of picture).   
     You can touch "exit to AppManager" in ExApp, AppManager is a App maintaince tool (the middle and right of picture), It can start a in-app webserver for upload app, it can download app from a website also .   
    <div align=center><img width="672" height="398"   src="https://raw.githubusercontent.com/digitalgust/miniJVM/master/screenshot/appmgr.png"/></div>   
  
  
## How to Remote debug:  
  Prepare:
  Rebuild /minijvm/c ,change  /minijvm/c/jvm/jvm.h "#define JDWP_DEBUG 0" as "#define JDWP_DEBUG 1"
  Desktop Computer : Run mini_jvm with flag: -Xdebug for debug mode .  
  iOS/Android simulator : nothing to do .  
  iOS/Android device : check the device ip address from General Setting -> wifi ->(i).  
  mini_jvm jdwp listen port is 8000.   
   * Intelli idea : open the java project , menu Run .> Edit Configurations , + remote , Transport : socket , Debugger mode : attach , host is your mini_jvm running at host ip and port ,ex. "localhost:8000" .  
   * Eclipse : configuration  like as idea .  
   * Netbeans : open java project ,  menu Debug .> connect to Debugger, Connector : SocketAttach , host is your mini_jvm running at the host and port, ex. "localhost:8000" , Timeout: 10000 .  
  Then you can setup breakpoint or pause mini_jvm and watch variable's value .  
  


## How to use Embed java compiler in mini_jvm:  
   Java Compiler : /binary/libex/janino.jar    
   Usage of compiler can be found in /binary folder   
   the compile command :  
```
win:
mini_jvm -cp ../lib/minijvm_rt.jar;../libex/janino.jar;../libex/commons-compiler.jar org.codehaus.janino.Compiler  ../res/BpDeepTest.java
posix:
./mini_jvm -cp ../lib/minijvm_rt.jar:../libex/janino.jar:../libex/commons-compiler.jar org.codehaus.janino.Compiler  ../res/BpDeepTest.java
```

Janino compiler is not the full java compiler, see [limitation](http://janino-compiler.github.io/janino/#limitations) , like :
```
List<String> list=new ArrayList(); 
list.add("abc");
String s=(String)list.get(0);//can't ignore (String) cast qualifier.   
```   
   


## Referenced project and technology:   
   [Sun CLDC](http://www.oracle.com/technetwork/java/cldc-141990.html)  :referenced cldc    
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
   [Awtk-minijvm](https://github.com/zlgopen/awtk-minijvm)  :[AWTK](https://github.com/zlgopen/awtk) cross platform ui bind to minijvm   


## Development IDE using:  
  C code:   JetBrains CLion ,Xcode ,Virtual studio .  
  Swift code/Object c:    XCode , LLVM  .  
  Java code:    Netbeans  ,jdk  .  
  android project:  Android Studio , Android SDK 

 
## Build GUI application, depend on openGL or openGLES     
   * iOS/Android system build with GLFM  (/mobile/iosapp/  ,  /mobile/androidapp/)       
   * Window system build with GLFW   (/desktop/glfw_gui/)      
   * GUI build with Nanovg          

  
## Example of mobile application

the example demonstrate how develop java app for iOS and Android 
UI layout xml file MyForm.xml

```
<form name="FORM_MAIN" w="100%" h="100%">
    <script>
        sub change()
            red=mod(random(),255)
            green=mod(random(),255)
            blue=mod(random(),255)
            setColor("LAB_HELP",red,green,blue,255)
            setText("LAB_HELP","Any a test")
        ret
    </script>

    <frame name="FRAME_TEST" w="80%" h="400" align="top,hcenter" title="WINDOW">
        <label name="LAB_HELP" w="100%">Help text:</label>
        <input w="100%" h="300" multiline="1"><![CDATA[
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
        <button name="BT_SCRIPT" h="30" onclick="change()">{Change}</button>
        <button name="BT_CANCEL" h="30">{Cancel}</button>
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
import org.mini.gui.event.GSizeChangeListener;
import org.mini.layout.*;

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
        GLanguage.setCurLang(GLanguage.ID_CHN);

        String xmlStr = GToolkit.readFileFromJarAsString("/res/MyForm.xml", "utf-8");

        UITemplate uit = new UITemplate(xmlStr);
        for (String key : uit.getVariable()) {
            uit.setVar(key, GLanguage.getString(key));
        }
        XContainer xc = new XForm(null);
        xc.parseXml(uit.parse());
        xc.build((int) GCallBack.getInstance().getDeviceWidth(), (int) GCallBack.getInstance().getDeviceHeight(), new XEventHandler() {
            public void action(GObject gobj, String cmd) {
                String name = gobj.getName();
                if ("MI_OPENFRAME".equals(name)) {
                    if (form.findByName("FRAME_TEST") == null) {
                        form.add(gframe);
                    }
                } else if ("MI_EXIT".equals(name)) {
                    AppManager.getInstance().active();
                } else if ("BT_CANCEL".equals(name)) {
                    gframe.close();
                }
            }
            public void onStateChange(GObject gobj, String cmd) {
            }
        });
        form = (GForm) xc.getGui();
        gframe = (GFrame) form.findByName("FRAME_TEST");
        if (gframe != null) gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
        menu = (GMenu) form.findByName("MENU_MAIN");
        form.setSizeChangeListener(new GSizeChangeListener() {
            @Override
            public void onSizeChange(int width, int height) {
                if (gframe != null && gframe.getAttachment() != null
                        && (gframe.getAttachment() instanceof XFrame)) {
                    ((XContainer) form.getAttachment()).reSize(width, height);
                    if (gframe != null) gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
                }
            }
        });
        return form;
    }
}




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
>> binary/-------- minijvm binary (win32/win64/mac/linux/android/ios)  
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
>>> jvm_ios/------ ios swift test project .      
>>> jvm_macos/---- macosX test project.      
>>> jvm_vs/------- windows virtual studio test project.      
>>> jni_test/------ jni example    
>>> javalib_test/-- test case of **/minijvm/java** .   
  
 * **/minijvm/c** is an independent small and fast jvm interpreter, it run on Win/Mac/Linux/iOS/Android.     
 * **/minijvm/java** is the jvm foundation runtime library, ex *java.lang* , *java.util* ,and extended classes *org.mini* for file,reflect and network etc, this project generate minijvm_rt.jar .   
 * **/mobile/iosapp** **/mobile/androidapp** are iOS/Android launcher program, it include minijvm source and native function, java call gui library with jni.       
 * **/mobile/java/glfm_gui** is a gui library ,it dependent on native gui library ,that include openGLES glad, glfm, nanovg, stb lib etc , this project generate glfm_gui.jar .     
 * **/mobile/java/ExApp** is an example of mobile app, it run on iOS and Android platform.  
     
  
## License
License:	MIT


Gust , zhangpeng@egls.cn , work in EGLS ltd. EGLS is a game development and distribution company in China .
