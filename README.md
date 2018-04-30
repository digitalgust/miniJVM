
![](/mini_jvm_64.png)  
# mini_jvm

  Develop application in Java for iOS,Android,Win,Linux,Mac platform. implemented in C , small mem footprint, startup fast, support native method, garbage collection ,java debug and more.
  
## Feature:  

  * Build pass: mingww64 32|64bit / cygwin / MSVC 32|64bit / MacOS / iOS / Linux / Android .   
  * No dependence Library .  
  * No jit but good performance, about 1/3 - 1/5 jit.   
  * Jvm runtime classlib ported from CLDC1.1 (Extended) .  
  * Support java5/6/7/8 class file version (but not all feature ex. lamdba) .  
  * Thread supported .  
  * Network supported .  
  * File io supported .  
  * Java native method supported (None jni Spec) .  
  * Java garbage collection supported .   
  * Java remote debug supported, JDWP Spec .  
  
## Directories: 
> /   
>> binary/-------- minijvm binary (win32/win64/mac/linux)  
>> mini_jvm/------ mini jvm c source   
>> javalib/------- jvm class lib   
>>
>> ex_lib/   
>>> glfm/--------- iOS/Android App gui module, openGLES2    
>>> jni_gui/------ native gui jni module, openGL2    
>>> luaj/--------- lua java    
>>
>> test/   
>>> javalib_test/- java class test case  
>>> jni_test/----- jni example    
>>> jvm_ios/------ ios swift test project.      
>>> jvm_macos/---- macosX test project.      
>>> jvm_vs/------- virtual studio test project.      
  
  C code:  Developed by JetBrains CLion ,Xcode ,Virtual studio .  
  Swift code:  Developed by XCode , LLVM 9 .  
  Java code:  Developed by Netbeans 8.0 ,jdk 1.8 .  
   
  The goal of mini_jvm is designed for resource limited device, iOS, Android, or other embedded device.  

  
## Deploy:  
  Download or clone github project.  
  Build jvm runtime library "/javalib" in Netbeans ,copy minijvm_rt.jar to /binary/lib/.  
  Open JetBrains Clion project (cmake) "/mini_jvm" , build and run , add args "-cp ../../binary/minijvm_rt.jar;" .  
  If create new java source file, must import the "minijvm_rt.jar" library ,because this lib diffirent with sun jdk .   
  
## Remote debug:  
  Run mini_jvm with flag: -Xdebug for debug mode .  
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



 
##   jni_gui module is a extend lib for build gui application, it depend on openGL2 or openGLES     
   * Window system build with GLFW         
   * iOS/Android system build with GLFM         
   * GUI build on nanovg          

  Screen shot   :   
    * iOS mini_jvm gui    
![iOS shot](/screenshot/ios.png)   
  * Windows mini_jvm gui    
![Windows shot](/screenshot/win.png)    
  * Macos mini_jvm gui    
![Macos shot](/screenshot/mac.png)    
  * Linux mini_jvm gui    
![Linux shot](/screenshot/centos.png)    
  
## License
License:	unlicense


Gust , zhangpeng@egls.cn , Technology and production manage in EGLS ltd. EGLS is a game develop company in China .