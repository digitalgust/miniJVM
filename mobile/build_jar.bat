@echo off
echo Requirement: jdk1.8 jar javac

set JAR=jar
set JAVAC=javac

mkdir assets
mkdir assets\resfiles

echo build assets\resfiles\minijvm_rt.jar
call :build_jar minijvm_rt.jar ..\minijvm\java\src\main assets\resfiles  "" ""

echo build assets\resfiles\glfm_gui.jar
call :build_jar glfm_gui.jar .\java\glfm_gui\src\main assets\resfiles "assets\resfiles\minijvm_rt.jar" ""

echo build assets\resfiles\ExApp.jar
call :build_jar ExApp.jar .\java\ExApp\src\main assets\resfiles "assets\resfiles\minijvm_rt.jar" "assets\resfiles\glfm_gui.jar"

echo completed.
pause
goto :eof 


:build_jar
    del /Q/S/F %3\%1
    md classes 
    dir /S /B %2\java\*.java > source.txt
    @echo %4 %5
    %JAVAC% -bootclasspath %4 -cp %5 -encoding "utf-8"   -d classes @source.txt
    xcopy /E %2\resource\* classes\
    %JAR% cf %1 -C classes .\
    del /Q/S source.txt
    rd /Q/S classes\
    move /Y %1 %3\
goto :eof