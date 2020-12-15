@echo off
echo Requirement: jdk1.8 jar javac

set JAR=jar
set JAVAC=javac

mkdir lib
mkdir libex

echo build lib\minijvm_rt.jar
call :build_jar minijvm_rt.jar ..\minijvm\java\src\main lib "." "."

echo build libex\glfw_gui.jar
call :build_jar glfw_gui.jar ..\desktop\glfw_gui\java\src\main libex "lib\minijvm_rt.jar" "."

echo build libex\minijvm_test.jar
call :build_jar minijvm_test.jar ..\test\minijvm_test\src\main libex "lib\minijvm_rt.jar" "."

echo completed.
pause
goto :eof 


:build_jar
    del /Q/S/F %3\%1
    md classes 
    dir /S /B %2\java\*.java > source.txt
    %JAVAC% -bootclasspath %4 -cp %5 -encoding "utf-8"   -d classes @source.txt
    xcopy /E %2\resource\* classes\
    %JAR% cf %1 -C classes .\
    del /Q/S source.txt
    rd /Q/S classes\
    move /Y %1 %3\
goto :eof