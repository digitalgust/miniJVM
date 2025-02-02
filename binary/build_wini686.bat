@echo off

set GLFWDIR=win_i686
set TARGETDIR=win32
set LIBDIR=i686-w64-mingw32
set GCCHOME=D:\mingw32
rem ==============================================================


echo [INFO]Build require MinGW-w64 , setup gcc home : %GCCHOME%
if exist  %GCCHOME% (
    echo [INFO]gcc found
) else (
    echo [ERROR]gcc not found
    echo [INFO]http://mingw-w64.org/ 
    echo [INFO]Please download gcc: https://objects.githubusercontent.com/github-production-release-asset-2e65be/446033510/d954d73e-f001-44c9-abcf-95fdda6e08c5?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=releaseassetproduction%2F20250201%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20250201T140135Z&X-Amz-Expires=300&X-Amz-Signature=393a2cf0bafc765a57a8142c166638f121d2ab39c38c536a1b5d0e9820e28ec7&X-Amz-SignedHeaders=host&response-content-disposition=attachment%3B%20filename%3Di686-14.2.0-release-posix-dwarf-msvcrt-rt_v12-rev1.7z&response-content-type=application%2Foctet-stream
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
copy %GCCHOME%\bin\libstdc*.dll %TARGETDIR%

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
