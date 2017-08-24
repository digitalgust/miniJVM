# mini_jvm

  A java virtual machine implemention by c , small footprint, startup quikly, support amost all java instruct , support thread ,native method, garbage collection and more.
  
## Feature:  

  Java foundation classlib implemention is CLDC1.1 ,Tested success.  
  No verify class file version ,support java8 and lower.  
  Compiled and tested in 32bit mingw and 64bit cygwin.   
  Little-endian and big-endian are designed, but not test yet.   
  java garbage collection supported ,simple implemention.  
  java instruct supported.  
  java Thread supported ,need ld flag -lpthread .  
  java native method supported.  
  java file/network io not supported yet, need native method.  
  mini_jvm/jvm develop ide that Clion, MinGW 5.0 or Cygwin 2.8.2.  
  mini_jvm/java develop ide that Netbeans 8.0 ,jdk 1.8 , User class compile *must be with this foundation classlib*.  
  The mini_jvm designed for resource limited device, iOS, Android, or other arm device.  
  for this work reference : ntu-android/simple_vm ,zhangkari/jvm ,CppArchMasters/lightweight-java-vm and more in github.   

## License
License:	GPL 3.0


Gust , zhangpeng@egls.cn , Technology and product manage in EGLS ltd. EGLS is a game develop Chinese company.