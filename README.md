# mini_jvm

  Mini jvm for iOS, Android. A java virtual machine implementation by C , small footprint, startup quikly, support amost all java instruct , support thread ,native method, garbage collection ,debug and more.
  
## Feature:  

  Java foundation classlib ported from CLDC1.1 ,Tested success.  
  support java8 and lower.  
  Little-endian and big-endian were supported.   
  java garbage collection supported , spreat thread collect.   
  java remote debug supported, transport by JDWP .  
  jvm instructions supported.  
  java Thread supported ,need ld flag -lpthread .  
  java native method supported.  
  java network supported ,Socket/ServerSocket/Http etc .  
  java file supported.  
  Compiled and tested in 32bit mingw / 64bit cygwin / MacOS /iOS .   
  c source code: mini_jvm/jvm mini_jvm/utils mini_jvm/cmem develop ide that JetBrains CLion, MinGW 5.0 or Cygwin 2.8.2.  
  swift code: mini_jvm/iostests develop ide that XCode , LLVM 9 .
  java code: mini_jvm/java develop ide that Netbeans 8.0 ,jdk 1.8 , User class compile *must be with this foundation classlib*.  
  The mini_jvm designed for resource limited device, iOS, Android, or other arm device.  
  for this work reference : sun cldc, ntu-android/simple_vm ,zhangkari/jvm ,CppArchMasters/lightweight-java-vm and more in github.   

## License
License:	LGPL


Gust , zhangpeng@egls.cn , Technology and production manage in EGLS ltd. EGLS is a game develop company in China .