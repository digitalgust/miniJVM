@echo off
echo Requirement: jdk1.8 jar javac

set JAR=jar
set JAVAC=javac


for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "java_version=%%g"
)

echo %java_version%

for /f "tokens=1-3 delims=." %%a in ("%java_version%") do (
    set /a major1=%%a, minor1=%%b, build1=%%c
)

echo java version :%major1%




mkdir lib
mkdir libex

echo build lib\minijvm_rt.jar
call :build_jar minijvm_rt.jar ..\minijvm\java\src\main lib "." "."

echo build libex\glfw_gui.jar
call :build_jar glfw_gui.jar ..\desktop\glfw_gui\java\src\main libex "lib\minijvm_rt.jar" "."

echo build libex\xgui.jar
call :build_jar xgui.jar ..\extlib\xgui\src\main libex "lib\minijvm_rt.jar" "libex\glfw_gui.jar"

echo build libex\minijvm_test.jar
call :build_jar minijvm_test.jar ..\test\minijvm_test\src\main libex "lib\minijvm_rt.jar" "."

echo Build MOBILE jars
mkdir ..\mobile\assets
mkdir ..\mobile\assets\resfiles

echo build assets\resfiles\minijvm_rt.jar
call :build_jar minijvm_rt.jar ..\minijvm\java\src\main ..\mobile\assets\resfiles  "" ""

echo build assets\resfiles\glfm_gui.jar
call :build_jar glfm_gui.jar ..\mobile\java\glfm_gui\src\main ..\mobile\assets\resfiles "..\mobile\assets\resfiles\minijvm_rt.jar" ""

echo build assets\resfiles\xgui.jar
call :build_jar xgui.jar ..\extlib\xgui\src\main ..\mobile\assets\resfiles "..\mobile\assets\resfiles\minijvm_rt.jar" "..\mobile\assets\resfiles\glfm_gui.jar"

echo build assets\resfiles\ExApp.jar
call :build_jar ExApp.jar ..\mobile\java\ExApp\src\main ..\mobile\assets\resfiles "..\mobile\assets\resfiles\minijvm_rt.jar" "..\mobile\assets\resfiles\glfm_gui.jar;..\mobile\assets\resfiles\xgui.jar"



echo completed.
pause
goto :eof 


:build_jar
    del /Q/S/F %3\%1
    md classes 
    dir /S /B %2\java\*.java > source.txt
    if %major1% gtr 8 (
        %JAVAC% --release 8 -cp %4;%5 -encoding "utf-8" -d classes @source.txt
    ) else (
        %JAVAC% -bootclasspath %4 -cp %5 -encoding "utf-8"   -d classes @source.txt
    )
    
    xcopy /E %2\resource\* classes\
    %JAR% cf %1 -C classes .\
    del /Q/S source.txt
    rd /Q/S classes\
    move /Y %1 %3\
goto :eof