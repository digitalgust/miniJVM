
echo "Requirement: jdk1.8 jar javac "

JAVAC=javac
JAR=jar


function build_jar(){
    rm -rf $3/$1
    mkdir classes 
    find $2/java -name "*.java" >source.txt
    ${JAVAC} -bootclasspath $4 -cp $5 -encoding "utf-8" -d classes @source.txt
    cp -R $2/resource/* classes/
    ${JAR} cf $1 -C classes ./
    rm -rf source.txt
    rm -rf classes
    mv $1 $3/
}

mkdir lib
mkdir libex

echo "build lib/minijvm_rt.jar"
$(build_jar minijvm_rt.jar ../minijvm/java/src/main lib "." ".")

echo "build libex/glfw_gui.jar"
$(build_jar glfw_gui.jar ../desktop/glfw_gui/java/src/main libex "lib/minijvm_rt.jar" ".")

echo "build libex/minijvm_test.jar"
$(build_jar minijvm_test.jar ../test/minijvm_test/src/main libex "lib/minijvm_rt.jar" ".")


