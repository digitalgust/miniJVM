# mini_jvm

  Mini jvm is a java virtual machine for iOS, Android. implemented in C , small mem footprint, startup quikly, support thread ,native method, garbage collection ,java debug and more.
  
## Feature:  

  * Compiled pass: mingw(tdm) 32bit /mingww64 32/64bit / cygwin / MSVC 32/64bit / MacOS / iOS / Linux /Android .   
  * No dependence .  
  * No jit but good performance .   
  * Java runtime classlib ported from CLDC1.1 (Enhanced) .  
  * Support java5/6/7/8 class file version (but not all feature ex. lamdba) .  
  * Java garbage collection supported .   
  * Java remote debug supported, transport by JDWP .  
  * Thread supported .  
  * Java native method supported (none jni standard) .  
  * Network supported ,Socket/ServerSocket/Http etc .  
  * File io supported .  
  
## Directories: 
> /   
>> binary/-------- minijvm binary (win32/win64/mac/linux)  
>> mini_jvm/------ mini jvm c source   
>> javalib/------- jvm class lib   

>> ex_lib/   
>>> jni_gui/------ native gui jni module, openGL2    
>>> luaj/--------- lua java    

>> test/   
>>> javalib_test/- java class test case  
>>> jni_test/----- jni example    
>>> jvm_ios/------ ios swift test project.      
>>> jvm_macos/---- macosX test project.      
>>> jvm_vs/------- virtual studio test project.      
  
  C code:  develop by JetBrains CLion, mingww64 or Cygwin 2.8.2.  ,c99 evn.
  Swift code:  develop by XCode , LLVM 9 .  
  Java code:  develop by Netbeans 8.0 ,jdk 1.8 , User class compile must be with these foundation classes.  
  The mini_jvm designed for resource limited device, iOS, Android, or other embedded device.  
  for this work , referenced : sun cldc, ntu.android/simple_vm ,zhangkari/jvm ,CppArchMasters/lightweight.java.vm and more in github.   
  
  
## Deploy:  
  Download or clone github project.  
  Compile java classes and package it to  javalib/dist/ , or you can open the project mini_jvm/javalib/  in Netbeans,then build .  
  Open JetBrains Clion project (cmake) mini_jvm/ ,setup mingw /cygwin /linux /mac xcode /vs env, build and run .  
  
  
## Remote debug:  
  Ensure that mini_jvm is running , and opened with flag: -Xdebug.  
  Open intelli idea ,open the java project , menu Run .> Edit Configurations , + remote , Transport : socket , Debugger mode : attach , host is your mini_jvm running at host ip and port ,ex. "localhost:8000" .  
  Eclipse's configuration  like as idea .  
  If you are using Netbeans , open java project ,  menu Debug .> connect to Debugger, Connector : SocketAttach , host is your mini_jvm running at the host and port, ex. "localhost:8000" , Timeout: 10000 .  
  Then you can setup breakpoint or pause mini_jvm and watch variable's value .  
  
  
## Screen shot    
  MINI_JVM gui module depend on openGL2 ,    
Window system build with  [GLFW](https://github.com/glfw/glfw),     
GUI build on [nanovg](https://github.com/memononen/nanovg).       


  * Windows mini_jvm gui    
![Windows shot](https://github.com/digitalgust/mini_jvm/raw/master/screenshot/win.png)    
  * Macos mini_jvm gui    
![Macos shot](https://github.com/digitalgust/mini_jvm/raw/master/screenshot/mac.png)    
  * Linux mini_jvm gui    
![Linux shot](https://github.com/digitalgust/mini_jvm/raw/master/screenshot/centos.png)    
  
## License
License:	FREE


Gust , zhangpeng@egls.cn , Technology and production manage in EGLS ltd. EGLS is a game develop company in China .