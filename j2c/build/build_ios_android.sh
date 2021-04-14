
echo "Requirement: jdk1.8 jar javac "

GCC=gcc
JAVAC=javac
JAR=jar
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk/Contents/Home/
#JAVA_HOME=

function build_jar(){
    rm -rf $3/$1
    mkdir classes 
    find $2/java -name "*.java" >source.txt
    ${JAVAC}  -cp $4 -encoding "utf-8" -d classes @source.txt
    if [ -f "$2/resource/" ]
    then 
        cp -R $2/resource/* classes/
    fi
    ${JAR} cf $1 -C classes ./
    rm -rf source.txt
    rm -rf classes
    mkdir $3
    mv $1 $3/
}


echo "build tools/translator.jar"
$(build_jar translator.jar ../translator/src/main "tools" "." ".")

if [ ! ${JAVA_HOME} ] ;then
    echo "JDK and JAVA_HOME env var set required"
else
    echo "JAVA_HOME=${JAVA_HOME}"
    ${JAVA_HOME}/bin/java -cp tools/translator.jar com.ebsee.Main ../../minijvm/java/src/main/java/:../../mobile/java/glfm_gui/src/main/java:../../mobile/java/ExApp/src/main/java ../app/generted/classes/ ../app/generted/c/
fi

echo "[INFO]Generted c source , open /j2c/build/ccios/ccios.xcodeproj in Xcode or /j2c/build/ccandroid in Android Studio."


