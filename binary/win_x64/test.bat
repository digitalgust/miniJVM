chcp 65001

mini_jvm.exe -Xdebug  -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/glfw_gui.jar;../libex/xgui.jar org.mini.glfw.GlfwMain

@echo test
rem mini_jvm.exe -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/minijvm_test.jar test.HeapDumpTest
rem mini_jvm.exe -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/minijvm_test.jar test.Foo3
rem mini_jvm.exe -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/minijvm_test.jar test.SpecTest
rem mini_jvm.exe -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/minijvm_test.jar test.InvokeTest

if "%NO_PAUSE%"=="" pause
