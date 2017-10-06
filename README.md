# mini_jvm

  Mini jvm for iOS, Android. A java virtual machine implementation by C , small footprint, startup quikly, support thread ,native method, garbage collection ,debug and more.
  
## Feature:  

  Java foundation classlib ported from CLDC1.1 ,Tested success.  
  Support java8 and lower.  
  Little-endian and big-endian were supported.   
  Java garbage collection supported , spreat thread collect.   
  Java remote debug supported, transport by JDWP .  
  Java Thread supported ,need ld flag -lpthread .  
  Java native method supported.  
  Java network supported ,Socket/ServerSocket/Http etc .  
  Java file supported.  
  Compiled and tested in 32bit mingw / 64bit cygwin / MacOS /iOS .   
  C code: (mini_jvm/jvm; mini_jvm/utils; mini_jvm/cmem) develop by JetBrains CLion, MinGW 5.0 or Cygwin 2.8.2.  
  Swift code: (mini_jvm/iostests) develop by XCode , LLVM 9 .  
  Java code: (mini_jvm/java) develop by Netbeans 8.0 ,jdk 1.8 , User class compile *must be with this foundation classlib*.  
  The mini_jvm designed for resource limited device, iOS, Android, or other arm device.  
  for this work reference : sun cldc, ntu-android/simple_vm ,zhangkari/jvm ,CppArchMasters/lightweight-java-vm and more in github.   

## License
License:	LGPL


Gust , zhangpeng@egls.cn , Technology and production manage in EGLS ltd. EGLS is a game develop company in China .