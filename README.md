# mini_jvm

  A java virtual machine implementation by c , small footprint, startup quikly, support amost all java instruct , support thread ,native method, garbage collection and more.
  
## Feature:  

  Java foundation classlib ported from CLDC1.1 ,Tested success.  
  No verify class file version ,support java8 and lower.  
  Little-endian and big-endian are designed, but big-endian not test yet.   
  java garbage collection supported , spreat thread collect.  
  jvm instructions supported.  
  java Thread supported ,need ld flag -lpthread .  
  java native method supported.  
  java network supported ,Socket/ServerSocket/Http etc .  
  java file supported.  
  Compiled and tested in 32bit mingw and 64bit cygwin.   
  mini_jvm/jvm develop ide that JetBrains CLion, MinGW 5.0 or Cygwin 2.8.2.  
  mini_jvm/java develop ide that Netbeans 8.0 ,jdk 1.8 , User class compile *must be with this foundation classlib*.  
  The mini_jvm designed for resource limited device, iOS, Android, or other arm device.  
  for this work reference : sun cldc, ntu-android/simple_vm ,zhangkari/jvm ,CppArchMasters/lightweight-java-vm and more in github.   

## License
License:	LGPL


Gust , zhangpeng@egls.cn , Technology and production manage in EGLS ltd. EGLS is a game develop company in China .