
echo "Requirement: gcc "

#${GCCHOME} setup as: /usr/bin/gcc
GCC=gcc
OSNAME="Darwin"
UNAME=`uname -a`

if [[ $UNAME == *$OSNAME* ]] 
then
    echo "Mac"
    BINDIR="macos"
    LIBDIR="mac"
    LIBFILE="libgui.dylib"
else
    echo "Linux"
    BINDIR="centos_x64"
    LIBDIR="centos_x64"
    LIBFILE="libgui.so"
fi

echo "compile mini_jvm"
CSRC="../minijvm/c"
#echo ${CSRC}
#
#

SRCLIST=`find ${CSRC}  -type f  -name "*.c" -not -path "${CSRC}/utils/sljit/*"  -not -path "${CSRC}/cmake-*" -not -path "${CSRC}/.*"`
#echo ${SRCLIST}
${GCC}  -o mini_jvm -I${CSRC}/jvm -I${CSRC}/utils/ -I${CSRC}/utils/sljit/ -I${CSRC}/utils/https/ -I${CSRC}/utils/https/mbedtls/include/ -lpthread -lm -ldl  $SRCLIST ${CSRC}/utils/sljit/sljitLir.c


echo "compile glfw_gui"

CSRC="../desktop/glfw_gui/c"
SRCLIST=`find ${CSRC} -type f -name "*.c"  -not -path "${CSRC}/cmake-*" -not -path "${CSRC}/.*"`
#
if [[ $UNAME == *$OSNAME* ]] 
then
    ${GCC} -shared -fPIC -o ${LIBFILE} -I../minijvm/c/jvm -I${CSRC}/ -I${CSRC}/deps/include -L${CSRC}/deps/lib/${LIBDIR} -lpthread -lglfw3 -framework Cocoa -framework IOKit -framework OpenGL -framework CoreFoundation -framework CoreVideo $SRCLIST
else
    ${GCC} -shared -fPIC -o ${LIBFILE} -I../minijvm/c/jvm -I${CSRC}/ -I${CSRC}/deps/include -L${CSRC}/deps/lib/${LIBDIR}   $SRCLIST -lglfw3 -lX11 -lXi -lpthread -lXcursor -lXrandr -lGL -lXinerama
fi
mv mini_jvm ${LIBFILE} ${BINDIR}/

