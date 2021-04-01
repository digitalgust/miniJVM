package com.ebsee.j2c;

import com.ebsee.classparser.ClassFile;
import com.ebsee.classparser.ClassHelper;
import com.ebsee.classparser.Field;
import com.ebsee.classparser.Method;

import java.util.*;

public class ClassManger {
    static public ClassHelper helper;
    static private Map<String, List<Method>> class2vmt = new HashMap();
    //    static private Map<String, List<List<Method>>> class2vmt = new HashMap();
    static private Map<String, Map<String, List<Method>>> class2tree = new HashMap();
    static private Map<String, List<Field>> class2ft = new HashMap();

    public static void init(List<String> files) {
        helper = new ClassHelper("", files.toArray(new String[files.size()]));
        helper.openClasses();
    }

    public static void addClassFile(String file) {
        helper.openClass(file);
    }

    public static ClassFile getClassFile(String className) {
        return helper.getClassFile(className);
    }

    public static int getFieldIndexInFieldTable(String className, String field, String desc) {
        List<Field> fieldsWithSuperClass = getFieldTable(className);
        int i = 0;
        for (Field f : fieldsWithSuperClass) {
            if (f.getFieldName().equals(field) && desc.equals(f.getDescriptor())) {
                return i;
            }
            i++;
        }
        throw new RuntimeException("field not found : " + className + "." + field + " " + desc);
    }

    public static int getFieldIndexInFieldTable(Field field) {
        List<Field> fieldsWithSuperClass = getFieldTable(field.getClassFile().getThisClassName());
        int i = 0;
        for (Field f : fieldsWithSuperClass) {
            if (f == field) {
                return i;
            }
            i++;
        }
        throw new RuntimeException("field not found : " + field.getClassFile().getThisClassName() + "." + field.getFieldName() + " " + field.getDescriptor());
    }

    public static Field findField(String className, String fieldName, String descript) {
        Field f = null;

        ClassFile cf = helper.getClassFile(className);
        if (className.startsWith("[")) cf = helper.getClassFile(Util.CLASS_JAVA_LANG_OBJECT);
        if (cf == null) {
            System.out.println("class not found :" + className);
        }
        for (int i = 0, imax = cf.getFields().length; i < imax; i++) {
            f = cf.getFields()[i];
            if (f.getFieldName().equals(fieldName) && f.getDescriptor().equals(descript)) {
                return f;
            }
        }
        String superName = cf.getSuperClassName();
        if (superName != null) f = findField(superName, fieldName, descript);
        if (f != null) {
            return f;
        }
        for (String iname : cf.getInterfaceClasses()) {
            f = findField(iname, fieldName, descript);
            if (f != null) {
                return f;
            }
        }

        return f;
    }

    public static List<Field> getFieldTable(String className) {
        List<Field> fieldsContainSuperClass = class2ft.get(className);
        if (fieldsContainSuperClass != null) return fieldsContainSuperClass;

        ClassFile c = ClassManger.getClassFile(className);
        fieldsContainSuperClass = new ArrayList<>();
        ClassFile sc = c;
        while (sc != null) {
            fieldsContainSuperClass.addAll(0, Arrays.asList(sc.getFields()));
            sc = ClassManger.getClassFile(sc.getSuperClassName());
        }
        class2ft.put(className, fieldsContainSuperClass);
        return fieldsContainSuperClass;
    }


    public static List<ClassFile> getClasses() {
        return helper.getClassFileList();
    }


    public static Map<String, List<Method>> getMethodTree(String className) {
        Map<String, List<Method>> map = class2tree.get(className);
        if (map != null) return map;
        if (className.startsWith("[")) {
            map = getMethodTree(Util.CLASS_JAVA_LANG_OBJECT);
        } else {
            map = new LinkedHashMap<>();
            getMethodTree(className, map);
            List<Method> impls = getImplementMethodTable(className);
            for (List<Method> list : map.values()) {
                for (int i = 0, imax = list.size(); i < imax; i++) {
                    Method m = list.get(i);
                    for (Method impl : impls) {
//                        if (m.getMethodName().equals("hashCode") && impl.getMethodName().equals("hashCode") && className.equals("java/lang/String")) {
//                            int debug = 1;
//                        }
                        // if overwrite
                        if (m.getMethodName().equals(impl.getMethodName()) && m.getDescriptor().equals(impl.getDescriptor())) {
                            list.set(i, impl);
                            break;
                        }
                    }
                }
            }
        }
        class2tree.put(className, map);
        return map;
    }


    private static void getMethodTree(String className, Map<String, List<Method>> map) {
        if (className == null) return;
        ClassFile c = helper.getClassFile(className);
        if (c == null) {
            int debug = 1;
            System.out.println("Class not found:" + className);
        }
        List<Method> list = new ArrayList<>();
        list.addAll(Arrays.asList(c.getVirtualMethods()));
        map.put(className, list);
        //
        ClassFile sc = helper.getClassFile(c.getSuperClassName());
        if (sc != null) getMethodTree(c.getSuperClassName(), map);
        for (String i : c.getInterfaceClasses()) {
            getMethodTree(i, map);
        }

    }

    public static List<Method> getImplementMethodTable(String className) {
        List<Method> list = class2vmt.get(className);
        if (list != null) return list;
        if (className.startsWith("[")) {
            list = getImplementMethodTable(Util.CLASS_JAVA_LANG_OBJECT);
        } else {
            if (className.equals(("java/util/ArrayList"))) {
                int debug = 1;
            }
            list = new ArrayList<>();
            getInterfaceMethods(className, list);
            getClassMethods(className, list);
        }
        class2vmt.put(className, list);
        return list;
    }


    private static void getInterfaceMethods(String className, List<Method> list) {
        if (className == null) return;
        ClassFile c = helper.getClassFile(className);
        if (c == null) {
            int debug = 1;
        }

        Method[] myVirMthd = c.getVirtualMethods();
        if (c.getInterfaceClasses().length == 0) {
            list.addAll(Arrays.asList(myVirMthd));
        } else {
            for (String iname : c.getInterfaceClasses()) {
                ClassFile cf = getClassFile(iname);
                if (cf == null) {
                    int debug = 1;
                }
                getInterfaceMethods(cf.getThisClassName(), list);
                for (Method impl : myVirMthd) {
                    // if overwrite
                    boolean override = false;
                    for (int i = 0, imax = list.size(); i < imax; i++) {
                        Method m = list.get(i);
                        if (m.getMethodName().equals(impl.getMethodName()) && m.getDescriptor().equals(impl.getDescriptor())) {
                            override = true;
                            list.set(i, impl);
                            break;
                        }
                    }
                    if (!override && !list.contains(impl)) {
                        list.add(impl);
                    }
                }
            }
        }
    }

    private static void getClassMethods(String className, List<Method> list) {
        if (className == null) return;
        ClassFile c = helper.getClassFile(className);
        if (c == null) {
            int debug = 1;
            System.out.println("Class not found:" + className);
        }

        ClassFile sc = getClassFile(c.getSuperClassName());
        if (sc != null) {
            getClassMethods(sc.getThisClassName(), list);
        }
        //overwrite
        Method[] myVirMthd = c.getVirtualMethods();
        for (Method impl : myVirMthd) {
            // if overwrite
            boolean override = false;
            for (int i = 0, imax = list.size(); i < imax; i++) {
                Method m = list.get(i);
                if (m.getMethodName().equals(impl.getMethodName()) && m.getDescriptor().equals(impl.getDescriptor())) {
                    override = true;
                    list.set(i, impl);
                    break;
                }
            }
            if (!override && !list.contains(impl)) {
                list.add(impl);
            }
        }
    }

    public static class FindResult {
        public int index;
        public String className;
        public List<Method> methods;

        public FindResult(int i, String cn, List<Method> l) {
            index = i;
            className = cn;
            methods = l;
        }

        public Method getMethod() {
            return methods.get(index);
        }
    }

    public static FindResult findVirtualMethod(String className, String methodName, String descript) {
//        if (methodName.equals("ensureOpen")) {
//            int debug = 1;
//        }
        if (className.startsWith("[")) {
            return findVirtualMethod(Util.CLASS_JAVA_LANG_OBJECT, methodName, descript);
        }
        String cname = className;
        Map<String, List<Method>> map = getMethodTree(cname);
        while (cname != null) {
            int mhdidx, imax;
            ClassFile cf = getClassFile(cname);
            if (cf == null) {
                int debug = 1;
                System.out.println("Class not found:" + className);
            }
            List<Method> methods = map.get(cname);
            if (methods != null) {
                for (mhdidx = 0, imax = methods.size(); mhdidx < imax; mhdidx++) {
                    Method m = methods.get(mhdidx);
                    if (m.getMethodName().equals(methodName) && m.getDescriptor().equals(descript)) {
                        return new FindResult(mhdidx, cname, methods);
                    }
                }
            }
//            if (cname.equals("java/util/List") && methodName.equals("forEach")) {
//                int debug = 1;
//            }
            for (String iname : cf.getInterfaceClasses()) {
                FindResult r = findVirtualMethod(iname, methodName, descript);
                if (r != null) return r;
            }


            if (cf != null) cname = cf.getSuperClassName();
            else cname = null;
        }
        return null;
        //throw new RuntimeException("method not found:" + cname + "." + methodName + descript);
    }

    public static Method findMethod(String className, String methodName, String descript) {

        ClassFile cf = helper.getClassFile(className);
        if (className.startsWith("[")) cf = helper.getClassFile(Util.CLASS_JAVA_LANG_OBJECT);
        if (cf == null) {
            System.out.println("class not found :" + className);
        }
        for (int i = 0, imax = cf.getMethods().length; i < imax; i++) {
            Method m = cf.getMethods()[i];
            if (m.getMethodName().equals(methodName) && m.getDescriptor().equals(descript)) {
                return m;
            }
        }
        String superName = cf.getSuperClassName();
        if (superName != null) {
            Method m = findMethod(superName, methodName, descript);
            if (m != null) return m;
        }
        for (String interfaceName : cf.getInterfaceClasses()) {
            Method m = findMethod(interfaceName, methodName, descript);
            if (m != null) {
                return m;
            }
        }
        return null;
    }


    public static Method findFinalizeMethod(String className) {
        List<Method> lists = getImplementMethodTable(className);
        for (Method m : lists) {
            if ("finalize".equals(m.getMethodName()) && "()V".equals(m.getDescriptor())) {
                if (!Util.CLASS_JAVA_LANG_OBJECT.equals(m.getClassFile().getThisClassName())) {
                    return m;
                }
            }
        }
        return null;
    }


}
