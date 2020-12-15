
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

mkdir assets
mkdir assets/resfiles

echo "build assets/resfiles/minijvm_rt.jar"
$(build_jar minijvm_rt.jar ../minijvm/java/src/main assets/resfiles "." ".")

echo "build assets/resfiles/glfm_gui.jar"
$(build_jar glfm_gui.jar ./java/glfm_gui/src/main assets/resfiles "assets/resfiles/minijvm_rt.jar" ".")

echo "build assets/resfiles/ExApp.jar"
$(build_jar ExApp.jar ./java/ExApp/src/main assets/resfiles "assets/resfiles/minijvm_rt.jar" "assets/resfiles/glfm_gui.jar")

echo "completed"
