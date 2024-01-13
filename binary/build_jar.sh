#!/bin/bash

echo "Requirement: jdk1.8+ jar javac "

JAVAC=javac
JAR=jar


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
        ${JAVAC} -bootclasspath $4 -cp $5 -encoding "utf-8" -d classes @source.txt
    fi

    cp -R $2/resource/* classes/
    ${JAR} cf $1 -C classes ./
    rm -rf source.txt
    rm -rf classes
    mv $1 $3/
}


v="$(jdk_version)"
v3="$((10#${v}*1))"
echo "java version: ${v3}"


mkdir lib
mkdir libex

echo "build lib/minijvm_rt.jar"
$(build_jar minijvm_rt.jar ../minijvm/java/src/main lib "." ".")

# echo "build libex/glfw_gui.jar"
# $(build_jar glfw_gui.jar ../desktop/glfw_gui/java/src/main libex "lib/minijvm_rt.jar" ".")
#
# echo "build libex/xgui.jar"
# $(build_jar xgui.jar ../extlib/xgui/src/main libex "lib/minijvm_rt.jar" "libex/glfw_gui.jar")
#
# echo "build libex/minijvm_test.jar"
# $(build_jar minijvm_test.jar ../test/minijvm_test/src/main libex "lib/minijvm_rt.jar" ".")
#
# echo "Build MOBILE jars"
# mkdir ../mobile/assets
# mkdir ../mobile/assets/resfiles
#
# echo "build ../mobile/assets/resfiles/minijvm_rt.jar"
# $(build_jar minijvm_rt.jar ../minijvm/java/src/main ../mobile/assets/resfiles "." ".")
#
# echo "build ../mobile/assets/resfiles/glfm_gui.jar"
# $(build_jar glfm_gui.jar ../mobile/java/glfm_gui/src/main ../mobile/assets/resfiles "../mobile/assets/resfiles/minijvm_rt.jar" ".")
#
# echo "build ../mobile/assets/resfiles/xgui.jar"
# $(build_jar xgui.jar ../extlib/xgui/src/main ../mobile/assets/resfiles "../mobile/assets/resfiles/minijvm_rt.jar" "../mobile/assets/resfiles/glfm_gui.jar:")
#
# echo "build ../mobile/assets/resfiles/ExApp.jar"
# $(build_jar ExApp.jar ../mobile/java/ExApp/src/main ../mobile/assets/resfiles "../mobile/assets/resfiles/minijvm_rt.jar" "../mobile/assets/resfiles/glfm_gui.jar:../mobile/assets/resfiles/xgui.jar")


