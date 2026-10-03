#!/bin/bash

# 进入脚本所在的当前目录（确保相对路径正确）
cd "$(dirname "$0")" || exit

# 执行你的命令（可执行文件在当前目录）
./mini_jvm -Xdebug \
-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 \
-bootclasspath ../lib/minijvm_rt.jar \
-cp ../libex/glfw_gui.jar:../libex/xgui.jar \
org.mini.glfw.GlfwMain

#./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/minijvm_test.jar test.Foo3

#./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/janino.jar:../libex/commons-compiler.jar #org.codehaus.janino.Compiler  ../res/BpDeepTest.java

echo execute BpDeepTest
#./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../res/ BpDeepTest
#./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/luaj.jar Sample
