
$(dirname $0)/mini_jvm -Xdebug -bootclasspath $(dirname $0)/../lib/minijvm_rt.jar -cp $(dirname $0)/../libex/glfw_gui.jar:$(dirname $0)/../libex/xgui.jar org.mini.glfw.GlfwMain

$(dirname $0)/mini_jvm -bootclasspath $(dirname $0)/../lib/minijvm_rt.jar -cp $(dirname $0)/../libex/minijvm_test.jar test.Foo3

#./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/janino.jar:../libex/commons-compiler.jar #org.codehaus.janino.Compiler  ../res/BpDeepTest.java

#echo execute BpDeepTest
#./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../res/ BpDeepTest
#./mini_jvm -bootclasspath ../lib/minijvm_rt.jar -cp ../libex/luaj.jar Sample
