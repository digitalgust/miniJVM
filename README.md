# mini_jvm

  Mini jvm is a java vm (java virtual machine) for iOS, Android. implemented in C , small mem footprint, startup quikly, support thread ,native method, garbage collection ,java debug and more.
  
## Feature:  

  Java runtime classlib ported from CLDC1.1 .  
  Support java5/6/7/8 class file version .  
  Little.endian and big.endian supported.   
  Java garbage collection supported .   
  Java remote debug supported, transport by JDWP .  
  Java Thread supported ,need ld flag -lpthread .  
  Java native method supported.  
  Java network supported ,Socket/ServerSocket/Http etc .  
  Java file supported.  
  
## Directories:  
  mini_jvm/javalib/------------------ java foundation class  
  mini_jvm/javalib_test/------------- java foundation class for test ,few class java.lang.*  
  mini_jvm/mini_jvm/jvm/------------- mini jvm c source ,jvm   
  mini_jvm/mini_jvm/utils/----------- c ,type def and containers.    
  mini_jvm/mini_jvm/utils/cmem/------ c ,memory leak detect, change: utils/d_type.h: #define __MEM_LEAK_DETECT     
  mini_jvm/mini_jvm/iostest/--------- ios swift test project.      
  
  Compiled and tested in 32bit mingw / 64bit cygwin / MacOS /iOS/Linux CentOS64 .   
  C code: (mini_jvm/jvm; mini_jvm/utils) develop by JetBrains CLion, MinGW 5.0 or Cygwin 2.8.2.  ,c99 evn.
  Swift code: (mini_jvm/iostests) develop by XCode , LLVM 9 .  
  Java code: (javalib; javalib_test) develop by Netbeans 8.0 ,jdk 1.8 , User class compile must be with these foundation classes.  
  The mini_jvm designed for resource limited device, iOS, Android, or other embedded device.  
  for this work reference : sun cldc, ntu.android/simple_vm ,zhangkari/jvm ,CppArchMasters/lightweight.java.vm and more in github.   
  
  
## Deploy:  
  Download github project.  
  Compile java classes to  mini_jvm/javalib/build/classes/ , or you can open the project mini_jvm/javalib/  in Netbeans,then build .  
  Open JetBrains Clion project (cmake) mini_jvm/mini_jvm/ ,setup mingw /cygwin /linux /mac xcode env, build and run .  
  
  
## Remote debug:  
  Ensure that mini_jvm is running .  
  Open intelli idea ,open the java project , menu Run .> Edit Configurations , + remote , Transport : socket , Debugger mode : attach , host is your mini_jvm running at host ip, port : 8000 .  
  Eclipse's configuration  like as idea .  
  If you are using Netbeans , open java project ,  menu Debug .> connect to Debugger, Connector : SocketAttach , host is your mini_jvm running at the host, port : 8000 , Timeout: 10000 .  
  Then you can set breakpoint or pause mini_jvm and watch variable value .  
  
  
  
## License
License:	BSD


Gust , zhangpeng@egls.cn , Technology and production manage in EGLS ltd. EGLS is a game develop company in China .