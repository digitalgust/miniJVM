chcp 65001

mini_jvm.exe -Xdebug -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/glfw_gui.jar;../libex/xgui.jar org.mini.glfw.GlfwMain

@echo test
mini_jvm.exe -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/minijvm_test.jar test.Foo3

pause