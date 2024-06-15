
echo "Requirement: jdk1.8 jar javac "

GCC=gcc
JAVAC=javac
JAR=jar
JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home
#JAVA_HOME=


# returns the JDK version.
# 8 for 1.8.0_nn, 9 for 9-ea etc, and "no_java" for undetected
jdk_version () {
  result=""
  java_cmd=""
  if [[ -n $(type -p java) ]]
  then
    java_cmd=java
  elif [[ (-n "$JAVA_HOME") && (-x "$JAVA_HOME/bin/java") ]]
  then
    java_cmd="$JAVA_HOME/bin/java"
  fi
  IFS=$'\n'
  # remove \r for Cygwin
  lines=$("$java_cmd" -Xms32M -Xmx32M -version 2>&1 | tr '\r' '\n')
  if [[ -z $java_cmd ]]
  then
    result=no_java
  else
    for line in $lines; do
      if [[ (-z $result) && ($line = *"version \""*) ]]
      then
        ver=$(echo $line | sed -e 's/.*version "\(.*\)"\(.*\)/\1/; 1q')
        # on macOS, sed doesn't support '?'
        if [[ $ver = "1."* ]]
        then
          result=$(echo $ver | sed -e 's/1\.\([0-9]*\)\(.*\)/\1/; 1q')
        else
          result=$(echo $ver | sed -e 's/\([0-9]*\)\(.*\)/\1/; 1q')
        fi
      fi
    done
  fi
  echo "$result"
}



build_jar () {
    rm -rf $3/$1
    mkdir classes 
    find $2/java -name "*.java" >source.txt

    v="$(jdk_version)"
    v3="$((10#${v}*1))"
    if [[ ${v3} -gt 8 ]]; then
        ${JAVAC} --release 8 -cp $4:$5 -encoding "utf-8" -d classes @source.txt
    else
        ${JAVAC}  -cp $4:$5 -encoding "utf-8" -d classes @source.txt
    fi

    cp -R $2/resource/* classes/
    ${JAR} cf $1 -C classes ./
    rm -rf source.txt
    rm -rf classes
    mv $1 $3/
}


echo "build tools/translator.jar"
rm -rf ./tools
mkdir ./tools
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



