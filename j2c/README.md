

# miniJVM j2c

  Convert minijvm JAVA source to C cource ,then compile to native application.

  /j2c/translator is a tool for convert java source code to c source.
  
###  Generate console app
   Run /j2c/build/build_console.sh in terminal ,generate binary file "app" .    
   Translator translate minijvm bootstrap classes (/minijvm/java/src/main/java/) and test case classes (/test/minijvm_test/src/main/java/) to C source to /j2c/app/generted/c.
   Then gcc compile source (/j2c/app/vm  /j2c/app/generted/c  /j2c/app/platform/desktop ) to binary, app file generated.
   Run binary in terminal :

   ```
   ./app test.HelloWorld
   ```   

###  Generate iOS/Android app
   Run /j2c/build/build_ios_android.sh convert java source code to c source .    
   Open project /j2c/build/ccios in Xcode, Or open /j2c/build/android in Android Studio, build mobile app.    

   It translate minijvm bootstrap classes (/minijvm/java/src/main/java/) and gui classes (/mobile/java/glfm_gui/src/main/java/) and gui testcase (/mobile/java/ExApp/src/main/java/) to C source , files generated in  /j2c/app/generted/c .    
   Then open ios project build app or build android app.     