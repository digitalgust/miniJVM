
./mini_jvm -Xdebug -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/glfw_gui.jar:../libex/xgui.jar org.mini.glfw.GlfwMain

./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/minijvm_test.jar test.Foo3

#./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/janino.jar:../libex/commons-compiler.jar #org.codehaus.janino.Compiler  ../res/BpDeepTest.java

echo execute BpDeepTest
#./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../res/ BpDeepTest
#./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/luaj.jar Sample
