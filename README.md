
![](/screenshot/mini_jvm_64.png)  
[![Build Status](https://travis-ci.org/digitalgust/miniJVM.svg?branch=master)](https://travis-ci.org/digitalgust/miniJVM)
# miniJVM

  Develop iOS Android app in java. Jvm for ios/android or embed system. the fastest ios java interpreter . 
  
## Features:  

  * Jvm Build pass: iOS / Android / mingww64 32 64bit / cygwin / MSVC 32 64bit / MacOS /  Linux  .   
  * No dependence Library .  
  * Low memory footprint .  
  * Minimal runtime classlib .   
  * Support java5/6/7/8 class file version .  
  * Support embedded java source compiler(janino compiler) .  
  * Thread supported .  
  * Network supported .  
  * File io supported .  
  * Java native method supported .  
  * Java garbage collection supported .   
  * Java remote debug supported, JDWP Spec .  
  
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
   Write java code once running both iOS and Android.  
   * Open ExApp project in NetBeans , it dependent on project **/javalib** and **/mobile/java/guilib**  
   * Write your code like example **/mobile/java/ExApp/src/test/App1.java**   
   * Change **/mobile/java/ExApp/src/app/GlfmMain.java** App1 to your application entry class   
   * Build **/mobile/java/ExApp** generate ExApp.jar ,MUST NOT change the jar name  
   * Copy ExApp.jar to **/mobile/assets/resfiles/**  
   * Open project **/mobile/iosapp** in Xcode, need not change anything, this project contains minijvm, glfm platform bridge, openGLES native function and jni interface, Nanovg paint module, Other include resource files like  **minijvm_rt.jar** ,**glfm_gui.jar** ,**ExApp.jar** and font files.  
   * Build and run it in simulator or device, your app has launched   
   * Open project **mobile/androidapp** in Android studio, need not change anything  ,same as iosapp  
   * Build and run, it would be startup  
   * Build ipa and apk files .  
    
   good luck  
  
  
## Remote debug:  
  Desktop Computer : Run mini_jvm with flag: -Xdebug for debug mode .  
  iOS/Android simulator : no attached operation.  
  iOS/Android device : check the device ip address from Setting.  
   * Intelli idea : open the java project , menu Run .> Edit Configurations , + remote , Transport : socket , Debugger mode : attach , host is your mini_jvm running at host ip and port ,ex. "localhost:8000" .  
   * Eclipse : configuration  like as idea .  
   * Netbeans : open java project ,  menu Debug .> connect to Debugger, Connector : SocketAttach , host is your mini_jvm running at the host and port, ex. "localhost:8000" , Timeout: 10000 .  
  Then you can setup breakpoint or pause mini_jvm and watch variable's value .  
  


## Compile java source:  
   To compile java source file ,there are 2 resolution:
   * Oracle JDK javac to compile.
   * Janino the third compiler.
   using Janino jar lib,  can see example in binary folder.   
   the compile command like :
```
win:
mini_jvm -cp ../lib/minijvm_rt.jar;../lib/janino.jar;../lib/commons-compiler.jar org.codehaus.janino.Compiler  ../res/BpDeepTest.java
posix:
./mini_jvm -cp ../lib/minijvm_rt.jar:../lib/janino.jar:../lib/commons-compiler.jar org.codehaus.janino.Compiler  ../res/BpDeepTest.java
```

Janion compiler [limitation](http://janino-compiler.github.io/janino/#limitations) ,example :
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

## Development IDE:  
  C code:   JetBrains CLion ,Xcode ,Virtual studio .  
  Swift code/Object c:    XCode , LLVM 9 .  
  Java code:    Netbeans 8.0 ,jdk 1.8 .  
  android project:  Android Studio ,Android SDK 

 
## Build GUI application, depend on openGL2 or openGLES     
   * iOS/Android system build with GLFM  (/mobile/iosapp/  ,  /mobile/androidapp/)       
   * Window system build with GLFW   (/ex_lib/gui_jni/)      
   * GUI build on Nanovg          

  Screen shot   :   
    * iOS mini_jvm gui    
  
<div align=center><img  src="/screenshot/ios.png"/></div>

There are two class demo how develop java app for iOS and Android, one is App main class, the other is an GuiApp
```

package app;

import org.mini.glfm.Glfm;
import org.mini.gui.GApplication;
import org.mini.gui.impl.GuiCallBack;
import test.MyApp;

/**
 *
 * This class MUST be app.GlfmMain
 *
 * And this jar MUST be resfiles/ExApp.jar
 * 
 * Or you can change the file name and class name in source glfmapp/main.c 
 *
 * @author gust
 */
public class GlfmMain {

    public static void main(String[] args) {
    }

    static public void glinit(long display) {

        Glfm.glfmSetDisplayConfig(display,
                Glfm.GLFMRenderingAPIOpenGLES3,
                Glfm.GLFMColorFormatRGBA8888,
                Glfm.GLFMDepthFormat16,
                Glfm.GLFMStencilFormat8,
                Glfm.GLFMMultisampleNone);
        GuiCallBack.getInstance().setDisplay(display);
        Glfm.glfmSetCallBack(display, GuiCallBack.getInstance());

        GApplication app = new MyApp();
        GuiCallBack.getInstance().setApplication(app);
    }

}


package test;

import java.io.File;
import org.mini.glfm.Glfm;
import org.mini.gui.*;
import org.mini.gui.event.*;
import org.mini.gui.impl.GuiCallBack;

/**
 *
 * @author gust
 */
public class MyApp implements GApplication {

    private static MyApp app;

    GForm form;
    GMenu menu;

    static public MyApp getInstance() {
        if (app == null) {
            app = new MyApp();
        }
        return app;
    }

    @Override
    public GForm createdForm(GuiCallBack ccb) {
        if (form != null) {
            return form;
        }
        GLanguage.setCurLang(GLanguage.ID_CHN);
        form = new GForm(ccb);

        form.setFps(30f);
        long vg = form.getNvContext();

        int menuH = 80;
        GImage img = GImage.createImageFromJar(form.getNvContext(), "/res/mini_jvm_64.png");
        menu = new GMenu(0, form.getDeviceHeight() - menuH, form.getDeviceWidth(), menuH);
        menu.setFixed(true);
        GMenuItem item = menu.addItem("Login", img);
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GFrame gframe = getFrame1();
                form.add(gframe);
                gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);
            }
        });
        item = menu.addItem("Select", img);
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GFrame gframe = getFrame2();
                form.add(gframe);
                gframe.align(GGraphics.VCENTER | GGraphics.HCENTER);
            }
        });
        item = menu.addItem("File", img);
        item.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GFrame gframe = getFrame3();
                form.add(gframe);
                gframe.align(GGraphics.VCENTER | GGraphics.HCENTER);
            }
        });

        form.add(menu);
        return form;
    }

    public GFrame getFrame1() {
        GFrame gframe = new GFrame("Login", 50, 50, 300, 500);
        GContainer parent = gframe.getView();
        int x = 8, y = 10;
        GTextField gif = new GTextField("", "search", x, y, 280, 25);
        gif.setBoxStyle(GTextField.BOX_STYLE_SEARCH);
        parent.add(gif);
        y += 30;
        GLabel lb1 = new GLabel("Login", x, y, 280, 20);
        parent.add(lb1);
        y += 25;
        GTextField mail = new GTextField("", "Email", x, y, 280, 28);
        parent.add(mail);
        y += 35;
        GTextField pwd = new GTextField("", "Password", x, y, 280, 28);
        parent.add(pwd);
        y += 35;

        String conttxt = "Features:\n"
                + "Jvm Build pass: iOS / Android / mingww64 32 64bit / cygwin / MSVC 32 64bit / MacOS / Linux .\n"
                + "No dependence Library .\n"
                + "Low memory footprint .\n"
                + "Minimal runtime classlib .\n"
                + "Support java5/6/7/8 class file version .\n"
                + "Support embedded java source compiler(janino compiler) .\n"
                + "Thread supported .\n"
                + "Network supported .\n"
                + "File io supported .\n"
                + "Java native method supported .\n"
                + "Java garbage collection supported .\n"
                + "Java remote debug supported, JDWP Spec .";
        GTextBox cont = new GTextBox(conttxt, "Contents", x, y, 280, 188);
        parent.add(cont);
        y += 195;

        GCheckBox cbox = new GCheckBox("Remember me", true, x, y, 140, 28);
        parent.add(cbox);
        GButton sig = new GButton("Sign in", x + 138, y, 140, 28);
        sig.setBgColor(0, 96, 128, 255);
        sig.setIcon(GObject.ICON_LOGIN);
        parent.add(sig);

        y += 35;
        GLabel lb2 = new GLabel("Diameter", x, y, 280, 20);
        parent.add(lb2);
        y += 25;
        GScrollBar sli = new GScrollBar(0.4f, GScrollBar.HORIZONTAL, x, y, 170, 28);
        parent.add(sli);
        y += 35;
        GButton bt1 = new GButton("Delete删除", x, y, 160, 28);
        bt1.setBgColor(128, 16, 8, 255);
        bt1.setIcon(GObject.ICON_TRASH);
        parent.add(bt1);
        GButton bt2 = new GButton("Cancel", x + 170, y, 110, 28);
        bt2.setBgColor(0, 0, 0, 0);
        bt2.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                gobj.getForm().remove(gframe);
            }
        });
        parent.add(bt2);

        bt1.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                System.out.println("delete something");
            }
        });
        return gframe;
    }

    public GFrame getFrame2() {
        GFrame gframe = new GFrame("Select", 0, 0, 300, 550);
        GContainer parent = gframe.getView();
        GImage img = GImage.createImageFromJar(form.getNvContext(), "/res/logo128.png");

        int x = 10, y = 10;
        GList list = new GList(x, y, 280, 30);
        parent.add(list);
        list.setItems(new GImage[]{img, img, img, img, img, img, img, img, img, img},
                new String[]{"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",});

        y += 50;
        parent.add(new TestCanvas(x, y, 280, 150));
        y += 160;
        list = new GList(x, y, 280, 140);
        list.setShowMode(GList.MODE_MULTI_SHOW);
        parent.add(list);
        list.setItems(new GImage[]{img, img, img, img, img, img, img, img, img, img},
                new String[]{"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",});

        y += 150;
        GColorSelector cs = new GColorSelector(0, x, y, 130, 130);
        parent.add(cs);
        return gframe;
    }

    class TestCanvas extends GCanvas {

        GImage img;

        public TestCanvas(int x, int y, int w, int h) {
            super(x, y, w, h);
        }

        int pos = 0, delta = 1;

        public void paint(GGraphics g) {
            g.setColor(0xff000000);
            g.fillRect(0, 0, (int) getW(), (int) getH());
            g.setColor(0xff0000ff);
            g.drawLine(20, 100, 100, 100);
            pos += delta;
            if (pos > 50) {
                delta = -1;
            }
            if (pos < 0) {
                delta = 1;
            }

            g.setColor(0xffff00ff);
            g.drawString("this is a canvas", pos, 50, GGraphics.TOP | GGraphics.LEFT);

            g.setColor(0xff00ff00);
            g.drawLine(20, 50, 100, 50);

            if (img == null) {
                img = GImage.createImageFromJar(g.getNvContext(), "/res/logo128.png");
            }
            g.drawImage(img, 130, 30, 100, 100, GGraphics.TOP | GGraphics.LEFT);
            form.flush();
        }
    }

    public GFrame getFrame3() {
        GFrame gframe = new GFrame("File", 0, 0, form.getDeviceWidth() - 40, (form.getDeviceHeight() - menu.getH() - 150));

        GList list = new GList(0, 0, (int) gframe.getView().getW(), (int) (gframe.getView().getH()));
        list.setShowMode(GList.MODE_MULTI_SHOW);
        list.setSelectMode(GList.MODE_MULTI_SHOW);
        gframe.getView().add(list);

        String resRoot = Glfm.glfmGetResRoot();
        File f = new File(resRoot);
        if (f.exists()) {
            String[] files = f.list();
            GImage[] imgs = new GImage[files.length];
            list.setItems(imgs, files);
        }
        list.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                GList glist = (GList) gobj;
                System.out.println(glist.getSelectedIndex());
            }
        });
        return gframe;
    }
}


```

  * Windows mini_jvm gui    
![Windows shot](/screenshot/win.png)    
  * Macos mini_jvm gui    
![Macos shot](/screenshot/mac.png)    
  * Linux mini_jvm gui    
![Linux shot](/screenshot/centos.png)    
  
  
  
  
  
  
## License
License:	Public domain


Gust , zhangpeng@egls.cn , Technology and production manage in EGLS ltd. EGLS is a game develop company in China .
