

cd ../minijvm/java && mvn clean install deploy -P release
pwd

cd ../../desktop/glfw_gui/java/ && mvn clean install deploy -P release
pwd


cd ../../../mobile/java/glfm_gui/ && mvn clean install deploy -P release
pwd

cd ../../../extlib/xgui/ && mvn clean install deploy -P release
pwd