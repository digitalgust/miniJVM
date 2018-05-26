
![](/screenshot/mini_jvm_64.png)  
[![Build Status](https://travis-ci.org/digitalgust/miniJVM.svg?branch=master)](https://travis-ci.org/digitalgust/miniJVM)
# miniJVM

  Develop iOS Android app in java. Jvm for ios/android.
  
## Feature:  

  * Jvm Build pass: iOS / Android / mingww64 32 64bit / cygwin / MSVC 32 64bit / MacOS /  Linux  .   
  * No dependence Library .  
  * No jit but good performance, about 1/3 - 1/5 jit.   
  * Jvm runtime classlib ported from CLDC1.1 (Extended) .  
  * Support java5/6/7/8 class file version (but not support lamdba annotation ) .  
  * Thread supported .  
  * Network supported .  
  * File io supported .  
  * Java native method supported (None jni Spec) .  
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
  
  **/mini_jvm** is an independent small and fast jvm interpreter, Need **/javalib** runtime class library only, it run on Win/Mac/Linux/iOS/Android.     
  **/javalib** is the jvm foundation runtime class library, ex *java.lang* , *java.util* ,and extended classes *org.mini* for file reflect and network etc, this project generate minijvm_rt.jar , copy it into **/mobile/assets/resfiles**  .   
  **/mobile/iosapp** **/mobile/androidapp** are iOS/Android launcher program, it include minijvm source and native gui function, java call gui library with jni.       
  **/mobile/java/guilib** is a gui library ,it dependent on native gui library ,that include openGLES glad, glfm, nanovg, stb lib etc , this project generate glfm_gui.jar , copy it into **/mobile/assets/resfiles** .     
  **/mobile/java/ExApp** is an example of mobile app, it run on iOS and Android platform.  
   

## How to develop iOS/Android app in java:   
   Write java code once running both iOS and Android.  
   Open ExApp project in NetBeans , it dependent on project **/javalib** and **/mobile/java/guilib**  
   Write your code like example **/mobile/java/ExApp/src/test/App1.java**   
   Change **/mobile/java/ExApp/src/app/GlfmMain.java** App1 to your application entry class   
   Build **/mobile/java/ExApp** generate ExApp.jar ,MUST NOT change the jar name  
   Copy ExApp.jar to **/mobile/assets/resfiles/**  
   Open project **/mobile/iosapp** in Xcode, need not change anything, this project contains minijvm, glfm platform bridge, openGLES native function and jni interface, Nanovg paint module, Other include resource files like  **minijvm_rt.jar** ,**glfm_gui.jar** ,**ExApp.jar** and font files.  
   Build and run it in simulator or device, your app has launched   
   Open project **mobile/androidapp** in Android studio, need not change anything  ,same as iosapp  
   Build and run, it would be startup  
   Build ipa and apk files .  
   good luck  
  
  
## Remote debug:  
  Desktop Computer : Run mini_jvm with flag: -Xdebug for debug mode .  
  iOS/Android simulator : no attached operation.  
  iOS/Android device : check the device ip address from Setting.  
   * Intelli idea : open the java project , menu Run .> Edit Configurations , + remote , Transport : socket , Debugger mode : attach , host is your mini_jvm running at host ip and port ,ex. "localhost:8000" .  
   * Eclipse : configuration  like as idea .  
   * Netbeans : open java project ,  menu Debug .> connect to Debugger, Connector : SocketAttach , host is your mini_jvm running at the host and port, ex. "localhost:8000" , Timeout: 10000 .  
  Then you can setup breakpoint or pause mini_jvm and watch variable's value .  
  

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
import static org.mini.glfm.Glfm.GLFMDepthFormat16;
import static org.mini.glfm.Glfm.GLFMMultisampleNone;
import static org.mini.glfm.Glfm.GLFMRenderingAPIOpenGLES2;
import static org.mini.glfm.Glfm.GLFMStencilFormat8;
import org.mini.gui.GuiCallBack;
import test.App1;

/**
 *
 * This class MUST be app.GlfmMain 
 * 
 * And this jar MUST be resfiles/ExApp.jar
 * 
 * it used in c source glfmapp/main.c
 * 
 * @author gust
 */
public class GlfmMain {



    public static void main(String[] args) {
    }


    static public void glinit(long display) {

        Glfm.glfmSetDisplayConfig(display,
                GLFMRenderingAPIOpenGLES2,
                Glfm.GLFMColorFormatRGBA8888,
                GLFMDepthFormat16,
                GLFMStencilFormat8,
                GLFMMultisampleNone);
        App1 app = new App1();
        GuiCallBack ccb = new GuiCallBack(display, app);
        Glfm.glfmSetCallBack(display, ccb);

    }

}


package test;

import java.util.Random;
import org.mini.gl.warp.GLFrameBuffer;
import org.mini.gl.warp.GLFrameBufferPainter;
import org.mini.gui.GButton;
import org.mini.gui.GCanvas;
import org.mini.gui.GCheckBox;
import org.mini.gui.GColorSelector;
import org.mini.gui.GForm;
import org.mini.gui.GFrame;
import org.mini.gui.GGraphics;
import org.mini.gui.GImage;
import org.mini.gui.GTextField;
import org.mini.gui.GLabel;
import org.mini.gui.GList;
import org.mini.gui.GMenu;
import org.mini.gui.GObject;
import org.mini.gui.GPanel;
import org.mini.gui.GScrollBar;
import org.mini.gui.GTextBox;
import org.mini.gui.GuiCallBack;
import org.mini.gui.event.GActionListener;
import static org.mini.nanovg.Gutil.toUtf8;
import org.mini.nanovg.Nanovg;
import org.mini.gui.GApplication;
import org.mini.gui.GLanguage;

/**
 *
 * @author gust
 */
public class App1 implements GApplication {

    private static App1 app;

    GForm form;
    GMenu menu;

    static public App1 getInstance() {
        if (app == null) {
            app = new App1();
        }
        return app;
    }


    @Override
    public GForm createdForm(GuiCallBack ccb) {
        if (form != null) {
            return form;
        }
        GLanguage.setCurLang(GLanguage.ID_CHN);
        form = new GForm(/*"GuiTest"*/"登录 窗口", 800, 600, ccb);

        form.setFps(30f);
        long vg = form.getNvContext();
        GFrame gframe = new GFrame("demo", 50, 50, 300, 500);
        init(gframe.getPanel(), vg, ccb);
        form.add(gframe);
        gframe.align(GGraphics.HCENTER | GGraphics.VCENTER);

        int menuH = 80;
        GImage img = new GImage("./image4.png");
        menu = new GMenu(0, form.getDeviceHeight() - menuH, form.getDeviceWidth(), menuH);
        menu.addItem("Home", img);
        menu.addItem("Search", img);
        menu.addItem("New", img);
        menu.addItem("My", img);
        form.add(menu);
        return form;
    }

    public void init(GPanel parent, final long vg, final GuiCallBack ccb) {
//        light = new Light();

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
//        String conttxt = "  \n  \n ";
        String conttxt = "子窗口This is longer chunk of text.\n  \n  Would have used lorem ipsum but she    was busy jumping over the lazy dog with the fox and all the men who came to the aid of the party.";
        conttxt += "I test the program ,there are two window , one window left a button that open the other window, the other left a button for close self.\n"
                + "\n"
                + "the issue maybe related with font , if i use nuklear defult font , the bug nerver show , but i am using chinese font (google android system default font), the bug frequently occure. the app memory using about 180M with default font in macos, use chinese font it would be 460M, is that nuklear load all glyph? but it's not the cause of bug .\n"
                + "\n"
                + "i have a reference that using stb_truetype, follow code is a stbtt test case , the code using chinese font ,that var byteOffset is -64 , out of the allocated bitmap memory . but i 'm not sure there is a same issue, only a note.";
        GTextBox cont = new GTextBox(conttxt, "Contents", x, y, 280, 188);
        parent.add(cont);
        y += 195;

        GCheckBox cbox = new GCheckBox("Remember me", true, x, y, 140, 28);
        parent.add(cbox);
        GButton sig = new GButton("Sign in", x + 138, y, 140, 28);
        sig.setBgColor(0, 96, 128, 255);
        sig.setIcon(GObject.ICON_LOGIN);
        parent.add(sig);
        sig.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                Random ran = new Random();
                GFrame sub1 = new GFrame(/*"子窗口"*/"颜色选择", 40 + ran.nextInt(100), 50 + ran.nextInt(100), 300, 600);
                GPanel panel = sub1.getPanel();
                init1(panel, vg);
                sub1.setClosable(true);
                form.add(sub1);
            }
        });
        y += 35;
        GLabel lb2 = new GLabel("Diameter", x, y, 280, 20);
        parent.add(lb2);
        y += 25;
        //drawEditBoxNum(vg, "123.00", "px", x + 180, y, 100, 28);
        GScrollBar sli = new GScrollBar(0.4f, GScrollBar.HORIZONTAL, x, y, 170, 28);
        parent.add(sli);
        y += 35;
        GButton bt1 = new GButton("Delete删除", x, y, 160, 28);
        bt1.setBgColor(128, 16, 8, 255);
        bt1.setIcon(GObject.ICON_TRASH);
        parent.add(bt1);
        GButton bt2 = new GButton("Cancel", x + 170, y, 110, 28);
        bt2.setBgColor(0, 0, 0, 0);
        parent.add(bt2);

        bt1.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                System.out.println("delete something");
                menu.setPos(menu.getX(), menu.getY() - 20);
                if (menu.getY() < 0) {
                    menu.setPos(menu.getX(), form.getDeviceHeight() - menu.getH());
                }
            }
        });
        bt2.setActionListener(new GActionListener() {
            @Override
            public void action(GObject gobj) {
                System.out.println("switch app");
                ccb.setApplication(App2.getInstance());
            }
        });
    }

    public void init1(GPanel parent, long vg) {
        GImage img = new GImage("./image4.png");

        int x = 10, y = 10;
        GList list = new GList(x, y, 280, 30);
        parent.add(list);
        if (list.getImages() == null) {
            int i = Nanovg.nvgCreateImage(vg, toUtf8("./image4.png"), 0);
            list.setItems(new int[]{i, i, i},
                    new String[]{"One", "Two", "Three",});

        }
        y += 50;
        parent.add(new TestCanvas(x, y, 280, 150));
        y += 160;
        list = new GList(x, y, 280, 140);
        list.setMode(GList.MODE_MULTI_LINE);
        parent.add(list);
        if (list.getImages() == null) {
            int i = Nanovg.nvgCreateImage(vg, toUtf8("./image4.png"), 0);
            list.setItems(new int[]{i, i, i, i, i, i, i, i, i, i},
                    new String[]{"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",});
        }
        y += 150;
        GColorSelector cs = new GColorSelector(0, x, y, 130, 130);
        parent.add(cs);

    }

    class TestCanvas extends GCanvas {

        GLFrameBuffer glfb;
        GLFrameBufferPainter glfbRender;
        GImage img3D;

        public TestCanvas(int x, int y, int w, int h) {
            super(x, y, w, h);
//            glfb = new GLFrameBuffer(300, 300);
//            glfbRender = new GLFrameBufferPainter() {
//                @Override
//                public void paint() {
//                    light.setCamera();
//                    light.draw();
//                }
//            };
//            img3D = new GImage(glfb.getTexture(), glfb.getWidth(), glfb.getHeight());
        }

        int pos = 0, delta = 1;

        public void paint(GGraphics g) {
            g.setColor(0xff000000);
            g.fillRect(0, 0, (int) getW(), (int) getH());
            g.setColor(0xff0000ff);
            g.drawLine(0, 100, 100, 100);
            pos += delta;
            if (pos > 50) {
                delta = -1;
            }
            if (pos < 0) {
                delta = 1;
            }
            g.drawString("this is a canvas", pos, 50, GGraphics.TOP | GGraphics.LEFT);
//            glfb.render(glfbRender);
//            g.drawImage(img3D, 0, 0, 100, 100, GGraphics.TOP | GGraphics.LEFT);
        }
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
License:	unlicense


Gust , zhangpeng@egls.cn , Technology and production manage in EGLS ltd. EGLS is a game develop company in China .
