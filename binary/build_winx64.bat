@echo off

set GLFWDIR=win_x64
set TARGETDIR=win_x64
set LIBDIR=x86_64-w64-mingw32
set GCCHOME=D:\mingw64
rem ==============================================================


echo [INFO]Build require MinGW-w64 , setup gcc home : %GCCHOME%
if exist  %GCCHOME% (
    echo [INFO]gcc found
) else (
    echo [ERROR]gcc not found
    echo [INFO]http://mingw-w64.org/ 
    echo [INFO]Please download gcc: https://objects.githubusercontent.com/github-production-release-asset-2e65be/446033510/b5868a81-162c-42ec-ab3c-701571ad88b2?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=releaseassetproduction%2F20250201%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20250201T140119Z&X-Amz-Expires=300&X-Amz-Signature=93b5769fdafe169607384e9e75f65f7c856b1699edd52c8911c2fa7de4275895&X-Amz-SignedHeaders=host&response-content-disposition=attachment%3B%20filename%3Dx86_64-14.2.0-release-posix-seh-msvcrt-rt_v12-rev1.7z&response-content-type=application%2Foctet-stream
    echo [INFO]Change GCCHOME with installed directory
    goto :eof
)

set GCCLIBDIR=%GCCHOME%\%LIBDIR%\lib

set path=%path%;%GCCHOME%\bin

setlocal enabledelayedexpansion

echo [INFO]build mini_jvm.exe
call :jvm_compile mini_jvm.exe ..\minijvm\c %TARGETDIR%
echo [INFO]build libgui.dll
call :gui_compile libgui.dll ..\desktop\glfw_gui\c %TARGETDIR%

copy %GCCHOME%\bin\libwinpthread*.dll %TARGETDIR%
copy %GCCHOME%\bin\libgcc*.dll %TARGETDIR%

echo completed.
pause

goto :eof
rem ==============================================================
:jvm_compile
    set SRCFILES=
    @for /f "delims=" %%i in ('@dir /S /B %2\*.c ^| @find /V "sljit" ^| @find /V "cmake-"') do (@set SRCFILES=!SRCFILES! %%i)
    rem echo %SRCFILES%
    %GCCHOME%\bin\gcc  -o %1 -I%2\jvm -I%2\utils\ -I%2\utils\sljit\ -I%2\utils\https\ -I%2\utils\https\mbedtls\include\ -L%GCCLIBDIR%  %SRCFILES% %2\utils\sljit\sljitLir.c  -lpthread -lm -lws2_32 
    move %1 %3
goto :eof

:gui_compile
    set SRCFILES=
    @for /f "delims=" %%i in ('@dir /S /B %2\*.c ^| @find /V "cmake-"') do (@set SRCFILES=!SRCFILES! %%i)
    rem echo %SRCFILES%
    %GCCHOME%\bin\gcc -shared -fPIC -o %1 -I..\minijvm\c\jvm -I%2\ -I%2\deps\include -L%2\deps\lib\%GLFWDIR%  %SRCFILES% -lglfw3 -lopengl32 -mwindows
    move %1 %3
goto :eof
