package org.mini.vm;

import org.mini.reflect.ReflectClass;
import org.mini.reflect.ReflectMethod;
import org.mini.zip.Zip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;

import static org.mini.vm.ByteCodeAssembler.*;

public class VmUtil {
    /**
     * find a file bytes from classes paths
     *
     * @param name
     * @param paths
     * @return
     * @throws ClassNotFoundException
     */
    static public byte[] getFileData(String name, String[] paths) throws ClassNotFoundException {
        // 加载D盘根目录下指定类名的class
        byte[] classData = null;
        for (String s : paths) {
            if (s != null && s.length() > 0) {
                File f = new File(s);
                if (f.exists()) {
                    if (f.isFile()) {
                        classData = Zip.getEntry(s, name);
                        if (classData != null) {
                            break;
                        }
                    } else {
                        File cf = new File(s + "/" + name);
                        // System.out.println("cf=" + cf.getAbsolutePath() + cf.exists());
                        if (cf.exists()) {
                            RandomAccessFile fis = null;
                            try {
                                classData = new byte[(int) cf.length()];
                                fis = new RandomAccessFile(cf, "r");
                                fis.read(classData, 0, classData.length);

                                if (classData != null) {
                                    break;
                                }
                            } catch (Exception e) {
                                classData = null;
                            } finally {
                                try {
                                    fis.close();
                                } catch (Exception e) {
                                }
                            }
                        } else { // it's a lib directory ,maybe contain jar files
                            File[] jars = f.listFiles(fj -> fj.getName().endsWith(".jar"));
                            for (File jar : jars) {
                                // System.out.println("jar file:" + jar.getAbsolutePath() + " -> " + name);
                                classData = Zip.getEntry(jar.getAbsolutePath(), name);
                                if (classData != null) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return classData;
    }

    /**
     * find a file url from paths ,the paths may contains jar and directory
     *
     * @param sourceName
     * @param paths
     * @return
     */
    static public URL getFileUrl(String sourceName, String[] paths) {

        while (sourceName.startsWith("/"))
            sourceName = sourceName.substring(1);
        for (String s : paths) {
            if (s != null && s.length() > 0) {
                File f = new File(s);
                if (f.isFile()) {
                    try {
                        boolean exist = Zip.isEntryExist(f.getAbsolutePath(), sourceName);
                        if (exist) {
                            String us = "jar:file:///" + f.getAbsolutePath() + "!/" + sourceName;
                            URL url = new URL(us);
                            return url;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    File cf = new File(s + "/" + sourceName);
                    if (cf.exists()) {
                        try {
                            String us = "file:///" + cf.getAbsolutePath();
                            // System.out.println(us);
                            return new URL(us);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * thread create handler
     * 用于把线程创建事件通知给应用
     */
    static class ThreadHandler implements ThreadLifeHandler {
        ThreadLifeHandler[] handlers;

        void addHandler(ThreadLifeHandler handler) {
            if (handlers == null) {
                handlers = new ThreadLifeHandler[]{handler};
            } else {
                ThreadLifeHandler[] nhandlers = new ThreadLifeHandler[handlers.length + 1];
                System.arraycopy(handlers, 0, nhandlers, 0, handlers.length);
                nhandlers[handlers.length] = handler;
                handlers = nhandlers;
            }
        }

        void removeHandler(ThreadLifeHandler handler) {
            for (int i = 0; i < handlers.length; i++) {
                if (handlers[i] == handler) {
                    ThreadLifeHandler[] nhandlers = new ThreadLifeHandler[handlers.length - 1];
                    System.arraycopy(handlers, 0, nhandlers, 0, i);
                    System.arraycopy(handlers, i + 1, nhandlers, i, handlers.length - i - 1);
                    handlers = nhandlers;
                    break;
                }
            }
        }

        @Override
        public synchronized void threadCreated(Thread t) {
            // System.out.println("threadCreated:" + t.getName());
            for (ThreadLifeHandler handler : handlers) {
                try {
                    handler.threadCreated(t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public synchronized void threadDestroy(Thread t) {
            // System.out.println("threadCreated:" + t.getName());
            for (ThreadLifeHandler handler : handlers) {
                try {
                    handler.threadDestroy(t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static ThreadHandler threadHandler = new ThreadHandler();

    public synchronized static void addThreadLifeHandler(ThreadLifeHandler r) {
        threadHandler.addHandler(r);
        Thread.setThreadLifeHandler(threadHandler);
    }

    public synchronized static void removeThreadLifeHandler(ThreadLifeHandler r) {
        threadHandler.removeHandler(r);
    }


    /**
     * ===============================================================================
     */
    /**
     * 生成代理类
     *
     * @param loader
     * @param proxyName
     * @param interfaces
     * @return
     */

    public static Class<?> genProxyClass(ClassLoader loader, String proxyName, Class<?>[] interfaces) {
        //生成class的字节码，并加载为class
        //这个class 命名为proxyName
        //这个class 有一个构造函数，接受InvocationHandler参数，并在实例里面保存这个参数
        //这个class 实现所有interfaces的接口方法，并在这些接口方法中会调用InvocationHandler的接口Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable;

        try {
            return generateProxyClassUsingBytecode(loader, proxyName, interfaces);
        } catch (Exception e) {
            throw new RuntimeException("无法生成代理类: " + proxyName, e);
        }
    }

    /**
     * 使用ByteCode相关类生成代理类
     */
    private static Class<?> generateProxyClassUsingBytecode(ClassLoader loader, String proxyName, Class<?>[] interfaces) {
        try {
            // 1. 创建常量池
            java.util.List<org.mini.vm.ByteCodeConstantPool.PoolEntry> pool = new java.util.ArrayList<>();

            // 2. 添加类名和父类
            String internalProxyName = proxyName.replace('.', '/');
            int classNameIndex = org.mini.vm.ByteCodeConstantPool.addClass(pool, internalProxyName);
            int superClassIndex = org.mini.vm.ByteCodeConstantPool.addClass(pool, "java/lang/reflect/Proxy");

            // 3. 添加实现的接口
            int[] interfaceIndices = new int[interfaces.length];
            for (int i = 0; i < interfaces.length; i++) {
                String interfaceName = interfaces[i].getName().replace('.', '/');
                interfaceIndices[i] = org.mini.vm.ByteCodeConstantPool.addClass(pool, interfaceName);
            }

            // 4. 添加InvocationHandler字段 (继承自Proxy类，不需要额外字段)
            org.mini.vm.ByteCodeAssembler.FieldData[] fields = new org.mini.vm.ByteCodeAssembler.FieldData[0];

            // 5. 生成方法
            java.util.List<org.mini.vm.ByteCodeAssembler.MethodData> methodsList = new java.util.ArrayList<>();

            // 5.1 生成构造函数
            int constructorNameIndex = org.mini.vm.ByteCodeConstantPool.addUtf8(pool, "<init>");
            int constructorDescIndex = org.mini.vm.ByteCodeConstantPool.addUtf8(pool, "(Ljava/lang/reflect/InvocationHandler;)V");
            byte[] constructorCode = generateConstructorBytecode(pool);

            methodsList.add(new org.mini.vm.ByteCodeAssembler.MethodData(
                    org.mini.vm.ByteCodeAssembler.ACC_PUBLIC,
                    constructorNameIndex,
                    constructorDescIndex,
                    constructorCode));

            // 5.2 为每个接口方法生成代理方法
            for (Class<?> intf : interfaces) {
                Method[] methods = intf.getMethods();
                for (Method method : methods) {
                    // 跳过Object类的方法
                    if (method.getDeclaringClass() == Object.class) {
                        continue;
                    }

                    int methodNameIndex = org.mini.vm.ByteCodeConstantPool.addUtf8(pool, method.getName());
                    int methodDescIndex = org.mini.vm.ByteCodeConstantPool.addUtf8(pool, getMethodDescriptor(method));
                    byte[] methodCode = generateProxyMethodBytecode(pool, method, intf);

                    methodsList.add(new org.mini.vm.ByteCodeAssembler.MethodData(
                            org.mini.vm.ByteCodeAssembler.ACC_PUBLIC,
                            methodNameIndex,
                            methodDescIndex,
                            methodCode));
                }

                // 如果是注解接口，需要额外生成annotationType()方法
                if (intf.isAnnotation()) {
                    // 检查是否已经有annotationType方法
                    boolean hasAnnotationTypeMethod = false;
                    for (Method method : methods) {
                        if ("annotationType".equals(method.getName()) &&
                                method.getParameterTypes().length == 0 &&
                                method.getReturnType() == Class.class) {
                            hasAnnotationTypeMethod = true;
                            break;
                        }
                    }

                    // 只有当接口中没有声明annotationType方法时才生成
                    if (!hasAnnotationTypeMethod) {
                        int annotationTypeNameIndex = org.mini.vm.ByteCodeConstantPool.addUtf8(pool, "annotationType");
                        int annotationTypeDescIndex = org.mini.vm.ByteCodeConstantPool.addUtf8(pool, "()Ljava/lang/Class;");
                        byte[] annotationTypeCode = generateAnnotationTypeMethodBytecode(pool, intf);

                        methodsList.add(new org.mini.vm.ByteCodeAssembler.MethodData(
                                org.mini.vm.ByteCodeAssembler.ACC_PUBLIC,
                                annotationTypeNameIndex,
                                annotationTypeDescIndex,
                                annotationTypeCode));
                    }
                }
            }

            org.mini.vm.ByteCodeAssembler.MethodData[] methods =
                    methodsList.toArray(new org.mini.vm.ByteCodeAssembler.MethodData[methodsList.size()]);

            // 6. 生成类文件到字节数组
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            org.mini.vm.ByteCodeAssembler.writeClass(baos, pool, classNameIndex, superClassIndex,
                    interfaceIndices, fields, methods);

            byte[] classBytes = baos.toByteArray();
//            try {
//                FileOutputStream fos = new FileOutputStream(proxyName + ".class");
//                fos.write(classBytes);
//                fos.close();
//            } catch (IOException iOException) {
//            }
            // 7. 使用反射定义类 (这是miniJVM需要支持的部分)
            return RefNative.defineClass(loader, proxyName, classBytes, 0, classBytes.length);

        } catch (Exception e) {
            throw new RuntimeException("字节码生成失败", e);
        }
    }

    /**
     * 生成构造函数字节码
     */
    private static byte[] generateConstructorBytecode(java.util.List<org.mini.vm.ByteCodeConstantPool.PoolEntry> pool) throws Exception {
        // 构造函数应该调用super(InvocationHandler)
        // aload_0      // 加载this
        // aload_1      // 加载InvocationHandler参数
        // invokespecial java/lang/reflect/Proxy.<init>(Ljava/lang/reflect/InvocationHandler;)V
        // return

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        // 写入Code属性头部
        org.mini.vm.ByteCodeStream.write2(out, 3); // max stack
        org.mini.vm.ByteCodeStream.write2(out, 2); // max locals  
        org.mini.vm.ByteCodeStream.write4(out, 0); // code length (稍后设置)

        // 生成字节码指令
        org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.aload_0);     // aload_0
        org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.aload_1);     // aload_1

        // invokespecial java/lang/reflect/Proxy.<init>(Ljava/lang/reflect/InvocationHandler;)V
        org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.invokespecial);
        int superConstructorRef = org.mini.vm.ByteCodeConstantPool.addMethodRef(pool,
                "java/lang/reflect/Proxy", "<init>", "(Ljava/lang/reflect/InvocationHandler;)V");
        org.mini.vm.ByteCodeStream.write2(out, superConstructorRef + 1);

        org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.return_);      // return

        // 异常表和属性
        org.mini.vm.ByteCodeStream.write2(out, 0); // exception handler table length
        org.mini.vm.ByteCodeStream.write2(out, 0); // attribute count

        byte[] result = out.toByteArray();

        // 设置实际的code length
        org.mini.vm.ByteCodeStream.set4(result, 4, result.length - 12);

        return result;
    }

    /**
     * 生成代理方法字节码
     */
    private static byte[] generateProxyMethodBytecode(java.util.List<org.mini.vm.ByteCodeConstantPool.PoolEntry> pool, Method method, Class<?> interfaceClass) throws Exception {
        // 参考字节码模式：
        // 0 aload_0
        // 1 getfield h
        // 4 aload_0  
        // 5 ldc 接口Class
        // 7 ldc 方法名
        // 9 ldc 方法描述符
        // 11 创建参数数组
        // 26 invokestatic VmUtil.wrapInvoke
        // 29 checkcast 返回类型
        // 32 astore_3 (如果需要)
        // 33 aload_3 (如果需要)
        // 34 return指令

        Class<?>[] paramTypes = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();
        String methodDescriptor = getMethodDescriptor(method);

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        // 计算栈和局部变量大小
        int maxStack = Math.max(paramTypes.length + 8, 10); // 足够的栈空间
        int maxLocals = paramTypes.length + 2; // this + 参数 + 返回值变量

        // 写入Code属性头部
        org.mini.vm.ByteCodeStream.write2(out, maxStack); // max stack
        org.mini.vm.ByteCodeStream.write2(out, maxLocals); // max locals  
        org.mini.vm.ByteCodeStream.write4(out, 0); // code length (稍后设置)

        // 1. aload_0 - 加载this
        org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.aload_0);

        // 2. getfield h - 获取InvocationHandler字段
        org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.getfield);
        int handlerFieldRef = org.mini.vm.ByteCodeConstantPool.addFieldRef(pool,
                "java/lang/reflect/Proxy", "h", "Ljava/lang/reflect/InvocationHandler;");
        org.mini.vm.ByteCodeStream.write2(out, handlerFieldRef + 1);

        // 3. aload_0 - 加载this作为proxy参数
        org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.aload_0);

        // 4. ldc 接口Class - 加载接口Class对象
        int interfaceClassRef = org.mini.vm.ByteCodeConstantPool.addClass(pool, interfaceClass.getName().replace('.', '/'));
        if (interfaceClassRef + 1 <= 255) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.ldc);
            org.mini.vm.ByteCodeStream.write1(out, interfaceClassRef + 1);
        } else {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.ldc_w);
            org.mini.vm.ByteCodeStream.write2(out, interfaceClassRef + 1);
        }

        // 5. ldc 方法名 - 加载方法名字符串
        int methodNameRef = org.mini.vm.ByteCodeConstantPool.addString(pool, method.getName());
        if (methodNameRef + 1 <= 255) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.ldc);
            org.mini.vm.ByteCodeStream.write1(out, methodNameRef + 1);
        } else {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.ldc_w);
            org.mini.vm.ByteCodeStream.write2(out, methodNameRef + 1);
        }

        // 6. ldc 方法描述符 - 加载方法描述符字符串
        int methodDescRef = org.mini.vm.ByteCodeConstantPool.addString(pool, methodDescriptor);
        if (methodDescRef + 1 <= 255) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.ldc);
            org.mini.vm.ByteCodeStream.write1(out, methodDescRef + 1);
        } else {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.ldc_w);
            org.mini.vm.ByteCodeStream.write2(out, methodDescRef + 1);
        }

        // 7. 创建参数数组
        if (paramTypes.length == 0) {
            // 没有参数，加载null
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.aconst_null); // aconst_null
        } else {
            // 创建Object[]数组
            writeLoadInt(out, paramTypes.length);
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.anewarray);
            int objectClassRef = org.mini.vm.ByteCodeConstantPool.addClass(pool, "java/lang/Object");
            org.mini.vm.ByteCodeStream.write2(out, objectClassRef + 1);

            // 填充数组
            for (int i = 0; i < paramTypes.length; i++) {
                org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.dup); // 复制数组引用
                writeLoadInt(out, i); // 数组索引

                // 加载参数并装箱
                Class<?> paramType = paramTypes[i];
                loadParameter(out, pool, paramType, i + 1);
                boxIfNeeded(out, pool, paramType);

                org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.aastore); // 存储到数组
            }
        }

        // 8. invokestatic VmUtil.wrapInvoke
        org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.invokestatic);
        int wrapInvokeMethodRef = org.mini.vm.ByteCodeConstantPool.addMethodRef(pool,
                "org/mini/vm/VmUtil", "wrapInvoke",
                "(Ljava/lang/reflect/InvocationHandler;Ljava/lang/reflect/Proxy;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
        org.mini.vm.ByteCodeStream.write2(out, wrapInvokeMethodRef + 1);

        // 9. 处理返回值
        if (returnType == void.class) {
            // void方法，弹出返回值并返回
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.pop);
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.return_);
        } else if (returnType.isPrimitive()) {
            // 基本类型，需要拆箱
            unboxToPrimitive(out, pool, returnType);
            writeReturnInstruction(out, returnType);
        } else {
            // 引用类型，需要checkcast
            org.mini.vm.ByteCodeStream.write1(out, checkcast); // checkcast
            int returnTypeRef = org.mini.vm.ByteCodeConstantPool.addClass(pool, returnType.getName().replace('.', '/'));
            org.mini.vm.ByteCodeStream.write2(out, returnTypeRef + 1);

            // 可选：存储到局部变量再返回（按照参考字节码的模式）
            int localVarIndex = paramTypes.length + 1;
            if (localVarIndex <= 3) {
                // 使用astore_0, astore_1, astore_2, astore_3
                org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.astore_0 + localVarIndex);
                org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.aload_0 + localVarIndex);
            } else {
                // 使用astore和aload指令
                org.mini.vm.ByteCodeStream.write1(out, astore); // astore
                org.mini.vm.ByteCodeStream.write1(out, localVarIndex);
                org.mini.vm.ByteCodeStream.write1(out, aload); // aload  
                org.mini.vm.ByteCodeStream.write1(out, localVarIndex);
            }
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.areturn);
        }

        // 异常表和属性
        org.mini.vm.ByteCodeStream.write2(out, 0); // exception handler table length
        org.mini.vm.ByteCodeStream.write2(out, 0); // attribute count

        byte[] result = out.toByteArray();
        // 设置实际的code length
        org.mini.vm.ByteCodeStream.set4(result, 4, result.length - 12);

        return result;
    }

    /**
     * 拆箱为基本类型（用于VmUtil.wrapInvoke返回的Object）
     */
    private static void unboxToPrimitive(java.io.ByteArrayOutputStream out,
                                         java.util.List<org.mini.vm.ByteCodeConstantPool.PoolEntry> pool,
                                         Class<?> primitiveType) throws Exception {

        String wrapperClass;
        String valueMethodName;
        String methodDescriptor;

        if (primitiveType == boolean.class) {
            wrapperClass = "java/lang/Boolean";
            valueMethodName = "booleanValue";
            methodDescriptor = "()Z";
        } else if (primitiveType == byte.class) {
            wrapperClass = "java/lang/Byte";
            valueMethodName = "byteValue";
            methodDescriptor = "()B";
        } else if (primitiveType == char.class) {
            wrapperClass = "java/lang/Character";
            valueMethodName = "charValue";
            methodDescriptor = "()C";
        } else if (primitiveType == short.class) {
            wrapperClass = "java/lang/Short";
            valueMethodName = "shortValue";
            methodDescriptor = "()S";
        } else if (primitiveType == int.class) {
            wrapperClass = "java/lang/Integer";
            valueMethodName = "intValue";
            methodDescriptor = "()I";
        } else if (primitiveType == long.class) {
            wrapperClass = "java/lang/Long";
            valueMethodName = "longValue";
            methodDescriptor = "()J";
        } else if (primitiveType == float.class) {
            wrapperClass = "java/lang/Float";
            valueMethodName = "floatValue";
            methodDescriptor = "()F";
        } else if (primitiveType == double.class) {
            wrapperClass = "java/lang/Double";
            valueMethodName = "doubleValue";
            methodDescriptor = "()D";
        } else {
            return; // 未知类型
        }

        // checkcast 到包装类型
        org.mini.vm.ByteCodeStream.write1(out, 0xC0); // checkcast
        int wrapperClassRef = org.mini.vm.ByteCodeConstantPool.addClass(pool, wrapperClass);
        org.mini.vm.ByteCodeStream.write2(out, wrapperClassRef + 1);

        // 调用拆箱方法
        org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.invokevirtual);
        int methodRef = org.mini.vm.ByteCodeConstantPool.addMethodRef(pool, wrapperClass, valueMethodName, methodDescriptor);
        org.mini.vm.ByteCodeStream.write2(out, methodRef + 1);
    }

    /**
     * 写入加载null的字节码
     */
    private static void writeLoadNull(java.io.ByteArrayOutputStream out) throws Exception {
        org.mini.vm.ByteCodeStream.write1(out, 0x01); // aconst_null
    }

    /**
     * 写入加载整数的字节码
     */
    private static void writeLoadInt(java.io.ByteArrayOutputStream out, int value) throws Exception {
        if (value >= 0 && value <= 5) {
            org.mini.vm.ByteCodeStream.write1(out, iconst_0 + value); // iconst_0 到 iconst_5
        } else if (value >= -128 && value <= 127) {
            org.mini.vm.ByteCodeStream.write1(out, bipush); // bipush
            org.mini.vm.ByteCodeStream.write1(out, value);
        } else {
            org.mini.vm.ByteCodeStream.write1(out, sipush); // sipush
            org.mini.vm.ByteCodeStream.write2(out, value);
        }
    }

    /**
     * 加载方法参数
     */
    private static void loadParameter(java.io.ByteArrayOutputStream out,
                                      java.util.List<org.mini.vm.ByteCodeConstantPool.PoolEntry> pool,
                                      Class<?> paramType, int localIndex) throws Exception {
        if (paramType == boolean.class || paramType == byte.class ||
                paramType == char.class || paramType == short.class || paramType == int.class) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.iload);
            org.mini.vm.ByteCodeStream.write1(out, localIndex);
        } else if (paramType == long.class) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.lload);
            org.mini.vm.ByteCodeStream.write1(out, localIndex);
        } else if (paramType == float.class) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.fload);
            org.mini.vm.ByteCodeStream.write1(out, localIndex);
        } else if (paramType == double.class) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.dload);
            org.mini.vm.ByteCodeStream.write1(out, localIndex);
        } else {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.aload);
            org.mini.vm.ByteCodeStream.write1(out, localIndex);
        }
    }

    /**
     * 装箱基本类型
     */
    private static void boxIfNeeded(java.io.ByteArrayOutputStream out,
                                    java.util.List<org.mini.vm.ByteCodeConstantPool.PoolEntry> pool,
                                    Class<?> type) throws Exception {
        if (!type.isPrimitive()) {
            return; // 不需要装箱
        }

        String wrapperClass;
        String valueMethodName;
        String methodDescriptor;

        if (type == boolean.class) {
            wrapperClass = "java/lang/Boolean";
            valueMethodName = "valueOf";
            methodDescriptor = "(Z)Ljava/lang/Boolean;";
        } else if (type == byte.class) {
            wrapperClass = "java/lang/Byte";
            valueMethodName = "valueOf";
            methodDescriptor = "(B)Ljava/lang/Byte;";
        } else if (type == char.class) {
            wrapperClass = "java/lang/Character";
            valueMethodName = "valueOf";
            methodDescriptor = "(C)Ljava/lang/Character;";
        } else if (type == short.class) {
            wrapperClass = "java/lang/Short";
            valueMethodName = "valueOf";
            methodDescriptor = "(S)Ljava/lang/Short;";
        } else if (type == int.class) {
            wrapperClass = "java/lang/Integer";
            valueMethodName = "valueOf";
            methodDescriptor = "(I)Ljava/lang/Integer;";
        } else if (type == long.class) {
            wrapperClass = "java/lang/Long";
            valueMethodName = "valueOf";
            methodDescriptor = "(J)Ljava/lang/Long;";
        } else if (type == float.class) {
            wrapperClass = "java/lang/Float";
            valueMethodName = "valueOf";
            methodDescriptor = "(F)Ljava/lang/Float;";
        } else if (type == double.class) {
            wrapperClass = "java/lang/Double";
            valueMethodName = "valueOf";
            methodDescriptor = "(D)Ljava/lang/Double;";
        } else {
            return; // 未知类型
        }

        org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.invokestatic);
        int methodRef = org.mini.vm.ByteCodeConstantPool.addMethodRef(pool, wrapperClass, valueMethodName, methodDescriptor);
        org.mini.vm.ByteCodeStream.write2(out, methodRef + 1);
    }

    /**
     * 写入适当的返回指令
     */
    private static void writeReturnInstruction(java.io.ByteArrayOutputStream out, Class<?> returnType) throws Exception {
        if (returnType == void.class) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.return_);
        } else if (returnType == boolean.class || returnType == byte.class ||
                returnType == char.class || returnType == short.class || returnType == int.class) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.ireturn);
        } else if (returnType == long.class) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.lreturn);
        } else if (returnType == float.class) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.freturn);
        } else if (returnType == double.class) {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.dreturn);
        } else {
            org.mini.vm.ByteCodeStream.write1(out, org.mini.vm.ByteCodeAssembler.areturn);
        }
    }

    /**
     * 获取方法描述符
     */
    private static String getMethodDescriptor(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');

        // 参数类型
        Class<?>[] paramTypes = method.getParameterTypes();
        for (Class<?> paramType : paramTypes) {
            sb.append(getTypeDescriptor(paramType));
        }

        sb.append(')');

        // 返回类型
        sb.append(getTypeDescriptor(method.getReturnType()));

        return sb.toString();
    }

    /**
     * 获取类型描述符
     */
    private static String getTypeDescriptor(Class<?> type) {
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        if (type == double.class) return "D";
        if (type.isArray()) {
            return "[" + getTypeDescriptor(type.getComponentType());
        } else {
            return "L" + type.getName().replace('.', '/') + ";";
        }
    }

    public static Object wrapInvoke(InvocationHandler handler, Proxy proxy, Class<?> annotationClass, String methodName, String methodDescriptor, Object[] args) {
        try {
            Method method = annotationClass.getMethod(methodName, ReflectMethod.getMethodPara(annotationClass.getClassLoader(), methodDescriptor));
            Object o = handler.invoke(proxy, method, args);
            return o;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成annotationType()方法的字节码
     * 字节码模式：
     * 0 ldc #3 <注解类>
     * 2 areturn
     */
    private static byte[] generateAnnotationTypeMethodBytecode(java.util.List<org.mini.vm.ByteCodeConstantPool.PoolEntry> pool, Class<?> annotationClass) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        // 写入Code属性头部
        org.mini.vm.ByteCodeStream.write2(out, 2); // max stack - 只需要栈深度1
        org.mini.vm.ByteCodeStream.write2(out, 1); // max locals - 只有this
        org.mini.vm.ByteCodeStream.write4(out, 0); // code length (稍后设置)

        // 生成字节码指令
        // ldc 注解类
        int annotationClassRef = org.mini.vm.ByteCodeConstantPool.addClass(pool, annotationClass.getName().replace('.', '/'));
        if (annotationClassRef + 1 <= 255) {
            org.mini.vm.ByteCodeStream.write1(out, ldc);
            org.mini.vm.ByteCodeStream.write1(out, annotationClassRef + 1);
        } else {
            org.mini.vm.ByteCodeStream.write1(out, ldc_w);
            org.mini.vm.ByteCodeStream.write2(out, annotationClassRef + 1);
        }

        // areturn
        org.mini.vm.ByteCodeStream.write1(out, areturn);

        // 异常表和属性
        org.mini.vm.ByteCodeStream.write2(out, 0); // exception handler table length
        org.mini.vm.ByteCodeStream.write2(out, 0); // attribute count

        byte[] result = out.toByteArray();
        // 设置实际的code length
        org.mini.vm.ByteCodeStream.set4(result, 4, result.length - 12);

        return result;
    }
}
