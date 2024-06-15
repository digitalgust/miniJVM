@echo off
echo Requirement: jdk1.8 jar javac

set JAVA_HOME=D:\Java\jdk1.8.0_361
set JAR=jar
set JAVAC=javac

set GLFWDIR=win_x64
set TARGETDIR=.\
set LIBDIR=x86_64-w64-mingw32
set GCCHOME=D:\mingw64
rem ==============================================================


for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "java_version=%%g"
)

echo %java_version%

for /f "tokens=1-3 delims=." %%a in ("%java_version%") do (
    set /a major1=%%a, minor1=%%b, build1=%%c
)

echo java version :%minor1%
rem ==============================================================

echo [INFO]Build require MinGW-w64 , setup gcc home : %GCCHOME%
if exist  %GCCHOME% (
    echo [INFO]gcc found
) else (
    echo [ERROR]gcc not found
    echo [INFO]http://mingw-w64.org/ 
    echo [INFO]Please download gcc: https://github.com/digitalgust/mingw-w64/releases/download/release/x86_64-8.1.0-release-posix-seh-rt_v6-rev0.7z
    echo [INFO]Change GCCHOME with installed directory
    goto :eof
)
set GCCLIBDIR=%GCCHOME%\%LIBDIR%\lib

set path=%path%;%GCCHOME%\bin

setlocal enabledelayedexpansion
rem ==============================================================


echo build lib\translator.jar
mkdir tools
call :build_jar translator.jar ..\translator\src\main "tools" "." "."

 %JAVA_HOME%\bin\java -cp tools/translator.jar com.ebsee.Main ../../minijvm/java/src/main/java/;../../test/minijvm_test/src/main/java/ ../app/generted/classes/ ../app/generted/c/
rem ==============================================================

echo [INFO]build app.exe
call :jvm_compile app.exe ..\app %TARGETDIR%

echo completed.
rem pause
goto :eof 

rem ==============================================================

:build_jar
    del /Q/S/F %3\%1
    md classes 
    dir /S /B %2\java\*.java > source.txt
    if %minor1% gtr 8 (
        %JAVAC% --release 8 -cp %4;%5 -encoding "utf-8" -d classes @source.txt
    ) else (
        %JAVAC%  -cp %4;%5 -encoding "utf-8"   -d classes @source.txt
    )
    
    xcopy /E %2\resource\* classes\
    %JAR% cf %1 -C classes .\
    del /Q/S source.txt
    rd /Q/S classes\
    move /Y %1 %3\
goto :eof

rem ==============================================================

:jvm_compile

    
    @del /Q/S csource.txt
    @for /f "delims=" %%i in ('@dir /S /B %2\*.c ^| @find /V "mobile"  ^| @find /V "cmake-"') do (@set SRCFILES=!SRCFILES! %%i  & set "line=%%i" & set "line=!line:\=/!" & echo !line! >>csource.txt)

    %GCCHOME%\bin\gcc  -o %1 -I%2\generted\c -I%2\platform\desktop -I%2\vm\ -I%2\vm\https\ -I%2\vm\https\mbedtls\include\ -I%2\vm\https\mbedtls\ -L%GCCLIBDIR%  @csource.txt   -lpthread -lm -lws2_32 
    move %1 %3
    del /Q/S csource.txt
goto :eof

