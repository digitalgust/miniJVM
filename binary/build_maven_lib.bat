
d:

cd ..\minijvm\java\
call mvn clean install deploy -P release


cd ..\..\binary
cd ..\desktop\glfw_gui\java\
call mvn clean install deploy -P release



cd ..\..\..\binary
cd ..\mobile\java\glfm_gui\
call mvn clean install deploy -P release


cd ..\..\..\binary
cd ..\extlib\xgui\
call mvn clean install deploy -P release

cd ..\..\binary