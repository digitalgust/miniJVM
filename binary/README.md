
###1. Install tools   

*   UBUNTU install :    
```
sudo apt-get install openjdk-8-jdk gcc libxi-dev libxcursor-dev libxrandr-dev libgl1-mesa-dev libxinerama-dev
```
*   CentOS install :    
```
yum -y install java-1.8.0-openjdk gcc mesa-libGL-devel libXi-devel libXcursor-devel libXrandr-devel libXinerama-devel
```
*   MacOS install :    
   Install Xcode and cli tools.

*   Windows install :
   Download gcc: https://sourceforge.net/projects/mingw-w64/files/Toolchains%20targetting%20Win64/Personal%20Builds/mingw-builds/8.1.0/threads-posix/sjlj/x86_64-8.1.0-release-posix-sjlj-rt_v6-rev0.7z
   Change GCCHOME in build_winXXX.bat with install gcc path.
   Download and install jdk 1.8 or later
    

###2. Build jar

*Posix:
```
build_jar.sh
```
*Windows:
```
build_jar.bat
```

###3. Build binary 

*Posix:
```
./build_mac_linux.sh
```
*Win x64:
```
./build_winx64.bat
```
*Win i686:
```
./build_wini686.bat
```

###4. Run test

Enter macos or centos_x64 or winxxx

Run test.sh or test.bat



