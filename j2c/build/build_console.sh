
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
    ${JAVA_HOME}/bin/${JAVAC}  -cp $4 -encoding "utf-8" -d classes @source.txt
    if [ -f "$2/resource/" ]
    then 
        cp -R $2/resource/* classes/
    fi
    ${JAVA_HOME}/bin/${JAR} cf $1 -C classes ./
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
    ${JAVA_HOME}/bin/java -cp tools/translator.jar com.ebsee.Main ../../minijvm/java/src/main/java/:../../test/minijvm_test/src/main/java/ ../app/generted/classes/ ../app/generted/c/
fi

CSRC="../app"
VMLIST=`find ${CSRC}/vm  -type f  -name "*.c" `
GENLIST=`find ${CSRC}/generted/c  -type f  -name "*.c" `

${GCC} -O3  -o app -I${CSRC}/generted/c -I${CSRC}/vm -I${CSRC}/vm/https/ -I${CSRC}/vm/https/mbedtls/include/ -lpthread -lm   $VMLIST  ${GENLIST} ../app/platform/desktop/main.c



