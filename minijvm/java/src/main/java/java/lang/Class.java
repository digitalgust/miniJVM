/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package java.lang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.mini.reflect.ReflectClass;
import org.mini.reflect.ReflectField;
import org.mini.reflect.ReflectMethod;
import org.mini.reflect.vm.RConst;
import org.mini.reflect.vm.RefNative;

/**
 * Instances of the class <code>Class</code> represent classes and interfaces in
 * a running Java application. Every array also belongs to a class that is
 * reflected as a <code>Class</code> object that is shared by all arrays with
 * the same element type and number of dimensions.
 *
 * <p>
 * <code>Class</code> has no public constructor. Instead <code>Class</code>
 * objects are constructed automatically by the Java Virtual Machine as classes
 * are loaded.
 *
 * <p>
 * The following example uses a <code>Class</code> object to print the class
 * name of an object:
 *
 * <p>
 * <blockquote><pre>
 *     void printClassName(Object obj) {
 *         System.out.println("The class of " + obj +
 *                            " is " + obj.getClass().getName());
 *     }
 * </pre></blockquote>
 *
 * @author unascribed
 * @version 12/17/01 (CLDC 1.1)
 * @since JDK1.0, CLDC 1.0
 */
public final class Class<T> {

    private static final int ANNOTATION = 0x00002000;
    private static final int ENUM = 0x00004000;
    private static final int SYNTHETIC = 0x00001000;

    /*
     * Constructor. Only the Java Virtual Machine creates Class
     * objects.
     */
    private Class() {
    }

    /**
     * Converts the object to a string. The string representation is the string
     * "class" or "interface", followed by a space, and then by the fully
     * qualified name of the class in the format returned by
     * <code>getName</code>. If this <code>Class</code> object represents a
     * primitive type, this method returns the name of the primitive type. If
     * this <code>Class</code> object represents void this method returns
     * "void".
     *
     * @return a string representation of this class object.
     */
    public String toString() {
        return (isInterface() ? "interface " : "class ") + getName();
    }

    /**
     * Returns the <code>Class</code> object associated with the class with the
     * given string name. Given the fully-qualified name for a class or
     * interface, this method attempts to locate, load and link the class.
     * <p>
     * For example, the following code fragment returns the runtime
     * <code>Class</code> descriptor for the class named
     * <code>java.lang.Thread</code>:
     * <ul><code>
     *   Class&nbsp;t&nbsp;= Class.forName("java.lang.Thread")
     * </code></ul>
     *
     * @param className the fully qualified name of the desired class.
     * @return the <code>Class</code> object for the class with the specified
     * name.
     * @exception ClassNotFoundException if the class could not be found.
     * @exception Error if the function fails for any other reason.
     * @since JDK1.0
     */
    public static native Class<?> forName(String className)
            throws ClassNotFoundException;

    /**
     * Creates a new instance of a class.
     *
     * @return a newly allocated instance of the class represented by this
     * object. This is done exactly as if by a <code>new</code> expression with
     * an empty argument list.
     * @exception IllegalAccessException if the class or initializer is not
     * accessible.
     * @exception InstantiationException if an application tries to instantiate
     * an abstract class or an interface, or if the instantiation fails for some
     * other reason.
     * @since JDK1.0
     */
    public native T newInstance()
            throws InstantiationException, IllegalAccessException;

    /**
     * Determines if the specified <code>Object</code> is assignment-compatible
     * with the object represented by this <code>Class</code>. This method is
     * the dynamic equivalent of the Java language <code>instanceof</code>
     * operator. The method returns <code>true</code> if the specified
     * <code>Object</code> argument is non-null and can be cast to the reference
     * type represented by this <code>Class</code> object without raising a
     * <code>ClassCastException.</code> It returns <code>false</code> otherwise.
     *
     * <p>
     * Specifically, if this <code>Class</code> object represents a declared
     * class, this method returns <code>true</code> if the specified
     * <code>Object</code> argument is an instance of the represented class (or
     * of any of its subclasses); it returns <code>false</code> otherwise. If
     * this <code>Class</code> object represents an array class, this method
     * returns <code>true</code> if the specified <code>Object</code> argument
     * can be converted to an object of the array class by an identity
     * conversion or by a widening reference conversion; it returns
     * <code>false</code> otherwise. If this <code>Class</code> object
     * represents an interface, this method returns <code>true</code> if the
     * class or any superclass of the specified <code>Object</code> argument
     * implements this interface; it returns <code>false</code> otherwise. If
     * this <code>Class</code> object represents a primitive type, this method
     * returns <code>false</code>.
     *
     * @param obj the object to check
     * @return true if <code>obj</code> is an instance of this class
     *
     * @since JDK1.1
     */
    public native boolean isInstance(Object obj);

    /**
     * Determines if the class or interface represented by this
     * <code>Class</code> object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * <code>Class</code> parameter. It returns <code>true</code> if so;
     * otherwise it returns <code>false</code>. If this <code>Class</code>
     * object represents a primitive type, this method returns <code>true</code>
     * if the specified <code>Class</code> parameter is exactly this
     * <code>Class</code> object; otherwise it returns <code>false</code>.
     *
     * <p>
     * Specifically, this method tests whether the type represented by the
     * specified <code>Class</code> parameter can be converted to the type
     * represented by this <code>Class</code> object via an identity conversion
     * or via a widening reference conversion. See <em>The Java Language
     * Specification</em>, sections 5.1.1 and 5.1.4 , for details.
     *
     * @param cls the <code>Class</code> object to be checked
     * @return the <code>boolean</code> value indicating whether objects of the
     * type <code>cls</code> can be assigned to objects of this class
     * @exception NullPointerException if the specified Class parameter is null.
     * @since JDK1.1
     */
    public native boolean isAssignableFrom(Class cls);

    /**
     * Determines if the specified <code>Class</code> object represents an
     * interface type.
     *
     * @return  <code>true</code> if this object represents an interface;
     * <code>false</code> otherwise.
     */
    public native boolean isInterface();

    /**
     * Determines if this <code>Class</code> object represents an array class.
     *
     * @return  <code>true</code> if this object represents an array class;
     * <code>false</code> otherwise.
     * @since JDK1.1
     */
    public native boolean isArray();

    /**
     * Returns the fully-qualified name of the entity (class, interface, array
     * class, primitive type, or void) represented by this <code>Class</code>
     * object, as a <code>String</code>.
     *
     * <p>
     * If this <code>Class</code> object represents a class of arrays, then the
     * internal form of the name consists of the name of the element type in
     * Java signature format, preceded by one or more "<tt>[</tt>" characters
     * representing the depth of array nesting. Thus:
     *
     * <blockquote><pre>
     * (new Object[3]).getClass().getName()
     * </pre></blockquote>
     *
     * returns "<code>[Ljava.lang.Object;</code>" and:
     *
     * <blockquote><pre>
     * (new int[3][4][5][6][7][8][9]).getClass().getName()
     * </pre></blockquote>
     *
     * returns "<code>[[[[[[[I</code>". The encoding of element type names is as
     * follows:
     *
     * <blockquote><pre>
     * B            byte
     * C            char
     * D            double
     * F            float
     * I            int
     * J            long
     * L<i>classname;</i>  class or interface
     * S            short
     * Z            boolean
     * </pre></blockquote>
     *
     * The class or interface name <tt><i>classname</i></tt> is given in fully
     * qualified form as shown in the example above.
     *
     * @return the fully qualified name of the class or interface represented by
     * this object.
     */
    String name;

    public String getName() {
        if (name == null) {
            name = getName0();
        }
        return name;
    }

    public native String getName0();

    /**
     * Finds a resource with a given name in the application's JAR file. This
     * method returns <code>null</code> if no resource with this name is found
     * in the application's JAR file.
     * <p>
     * The resource names can be represented in two different formats: absolute
     * or relative.
     * <p>
     * Absolute format:
     * <ul><code>/packagePathName/resourceName</code></ul>
     * <p>
     * Relative format:
     * <ul><code>resourceName</code></ul>
     * <p>
     * In the absolute format, the programmer provides a fully qualified name
     * that includes both the full path and the name of the resource inside the
     * JAR file. In the path names, the character "/" is used as the separator.
     * <p>
     * In the relative format, the programmer provides only the name of the
     * actual resource. Relative names are converted to absolute names by the
     * system by prepending the resource name with the fully qualified package
     * name of class upon which the <code>getResourceAsStream</code> method was
     * called.
     *
     * @param name name of the desired resource
     * @return a <code>java.io.InputStream</code> object.
     */
    public java.io.InputStream getResourceAsStream(String name) {
        try {
            if (name.length() > 0 && name.charAt(0) == '/') {
                /* Absolute format */
                name = name.substring(1);
            } else {
                /* Relative format */
                String className = this.getName();
                int dotIndex = className.lastIndexOf('.');
                if (dotIndex >= 0) {
                    name = className.substring(0, dotIndex + 1).replace('.', '/')
                            + name;
                }
            }
            return new com.sun.cldc.io.ResourceInputStream(name);
        } catch (java.io.IOException x) {
            return null;
        }
    }

    /**
     * Returns the {@code Class} representing the component type of an array. If
     * this class does not represent an array class this method returns null.
     *
     * @return the {@code Class} representing the component type of this class
     * if this class is an array
     * @see java.lang.reflect.Array
     * @since JDK1.1
     */
//    public native Class<?> getComponentType();
    /*
     * This private function is used during virtual machine initialization.
     * The user does not normally see this function.
     */
    private static void runCustomCode() {
    }

    /**
     * Returns the class loader for the class. Some implementations may use null
     * to represent the bootstrap class loader. This method will return null in
     * such implementations if this class was loaded by the bootstrap class
     * loader.
     *
     * <p>
     * If a security manager is present, and the caller's class loader is not
     * null and the caller's class loader is not the same as or an ancestor of
     * the class loader for the class whose class loader is requested, then this
     * method calls the security manager's {@code checkPermission} method with a
     * {@code RuntimePermission("getClassLoader")} permission to ensure it's ok
     * to access the class loader for the class.
     *
     * <p>
     * If this object represents a primitive type or void, null is returned.
     *
     * @return the class loader that loaded the class or interface represented
     * by this object.
     * @throws SecurityException if a security manager exists and its
     * {@code checkPermission} method denies access to the class loader for the
     * class.
     * @see java.lang.ClassLoader
     * @see SecurityManager#checkPermission
     * @see java.lang.RuntimePermission
     */
    public ClassLoader getClassLoader() {
        ClassLoader cl = getClassLoader0();
        if (cl == null) {
            return null;
        }
        return cl;
    }

    // Package-private to allow ClassLoader access
    native ClassLoader getClassLoader0();

    /*
     * Return the Virtual Machine's Class object for the named
     * primitive type.
     */
    static native Class<?> getPrimitiveClass(String name);

    public String getCanonicalName() {
        if (isPrimitive()) {
            return getName();
        } else if (isArray()) {
            return getComponentType().getCanonicalName() + "[]";
        } else {
            return getName().replace('$', '.');
        }
    }

    public String getSimpleName() {
        if (isPrimitive()) {
            return getName();
        } else if (isArray()) {
            return getComponentType().getSimpleName() + "[]";
        } else {
            String name = getCanonicalName();
            int index = name.lastIndexOf('.');
            if (index >= 0) {
                return name.substring(index + 1);
            } else {
                return name;
            }
        }
    }

    public native boolean isPrimitive();

    public native Class<? super T> getSuperclass();

    public boolean isEnum() {
        // An enum must both directly extend java.lang.Enum and have
        // the ENUM bit set; classes for specialized enum constants
        // don't do the former.
        return (this.getModifiers() & ENUM) != 0
                && this.getSuperclass() == java.lang.Enum.class;
    }

    /**
     * Returns the elements of this enum class or null if this Class object does
     * not represent an enum type.
     *
     * @return an array containing the values comprising the enum class
     * represented by this Class object in the order they're declared, or null
     * if this Class object does not represent an enum type
     * @since 1.5
     */
    public T[] getEnumConstants() {
        T[] values = getEnumConstantsShared();
        return (values != null) ? values.clone() : null;
    }

    /**
     * Returns the elements of this enum class or null if this Class object does
     * not represent an enum type; identical to getEnumConstants except that the
     * result is uncloned, cached, and shared by all callers.
     */
    T[] getEnumConstantsShared() {
        if (enumConstants == null) {
            if (!isEnum()) {
                return null;
            }
            try {
                final Method values = getMethod("values");
                T[] temporaryConstants = (T[]) values.invoke(null);
                enumConstants = temporaryConstants;
            } // These can happen when users concoct enum-like classes
            // that don't comply with the enum spec.
            catch (Exception ex) {
                return null;
            }
        }
        return enumConstants;
    }
    private volatile transient T[] enumConstants = null;

    /**
     * Returns a map from simple name to enum constant. This package-private
     * method is used internally by Enum to implement
     * {@code public static <T extends Enum<T>> T valueOf(Class<T>, String)}
     * efficiently. Note that the map is returned by this method is created
     * lazily on first use. Typically it won't ever get created.
     */
    Map<String, T> enumConstantDirectory() {
        if (enumConstantDirectory == null) {
            T[] universe = getEnumConstantsShared();
            if (universe == null) {
                throw new IllegalArgumentException(
                        getName() + " is not an enum type");
            }
            Map<String, T> m = new HashMap<>(2 * universe.length);
            for (T constant : universe) {
                m.put(((Enum<?>) constant).name(), constant);
            }
            enumConstantDirectory = m;
        }
        return enumConstantDirectory;
    }
    private volatile transient Map<String, T> enumConstantDirectory = null;

    /**
     *
     * --------------------------- reflect ----------------------------------
     *
     *
     */
    long classHandle;//save JClass ptr ,MUST NOT delete it and rename it, set it when JClass->ins_class init

    ReflectClass refClass;

    public long getClassHandler() {
        return classHandle;
    }

    public int getModifiers() {
        if (refClass == null) {
            refClass = new ReflectClass(classHandle);
        }
        return refClass.accessFlags;
    }

    public Method getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method m = getDeclaredMethod(name, parameterTypes);
        if ((m.getModifiers() & RConst.ACC_PUBLIC) != 0) {
            return m;
        }
        throw new NoSuchMethodException(name);
    }

    public Method getDeclaredMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        if (refClass == null) {
            refClass = new ReflectClass(classHandle);
        }
        ReflectMethod rm = refClass.getMethod(name, parameterTypes);
        if (rm != null) {
            return new Method(this, rm);
        }
        throw new NoSuchMethodException(name);
    }

    public Method[] getMethods() {
        if (refClass == null) {
            refClass = new ReflectClass(classHandle);
        }
        ReflectMethod[] rms = refClass.getMethods();
        int mcount = 0;
        for (int i = 0, imax = rms.length; i < imax; i++) {
            if (rms[i].methodName.charAt(0) != '<'
                    && (rms[i].accessFlags & (RConst.ACC_PUBLIC)) != 0) {
                mcount++;
            }
        }
        Method[] ms = new Method[mcount];
        for (int i = 0; i < mcount; i++) {
            if (rms[i].methodName.charAt(0) != '<'
                    && (rms[i].accessFlags & (RConst.ACC_PUBLIC)) != 0) {
                ms[i] = new Method(this, rms[i]);
            }
        }
        return ms;
    }

    public Method[] getDeclaredMethods() {
        if (refClass == null) {
            refClass = new ReflectClass(classHandle);
        }
        ReflectMethod[] rms = refClass.getMethods();
        int mcount = 0;
        for (int i = 0, imax = rms.length; i < imax; i++) {
            if (rms[i].methodName.charAt(0) != '<') {
                mcount++;
            }
        }
        Method[] ms = new Method[mcount];
        for (int i = 0; i < mcount; i++) {
            if (rms[i].methodName.charAt(0) != '<') {
                ms[i] = new Method(this, rms[i]);
            }
        }
        return ms;
    }

    public Constructor<T> getConstructor(Class<?>... parameterTypes) throws NoSuchMethodException {
        Constructor<T> c = getDeclaredConstructor(parameterTypes);
        if ((c.getModifiers() & RConst.ACC_PUBLIC) != 0) {
            return c;
        }
        throw new NoSuchMethodException();
    }

    public Constructor<?>[] getConstructors() {
        if (refClass == null) {
            refClass = new ReflectClass(classHandle);
        }
        ReflectMethod[] rms = refClass.getMethods();
        int mcount = 0;
        for (int i = 0, imax = rms.length; i < imax; i++) {
            if (rms[i].methodName.equals("<init>")) {
                mcount++;
            }
        }
        Constructor[] cs = new Constructor[mcount];
        for (int i = 0; i < mcount; i++) {
            if (rms[i].methodName.equals("<init>")) {
                cs[i] = new Constructor<>(this, rms[i]);
            }
        }
        return cs;
    }

    public Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes) throws NoSuchMethodException {
        if (refClass == null) {
            refClass = new ReflectClass(classHandle);
        }
        ReflectMethod rm = refClass.getMethod("<init>", parameterTypes);
        if (rm != null) {
            return new Constructor<>(this, rm);
        }
        throw new NoSuchMethodException();
    }

    public Field getField(String name) throws NoSuchFieldException {
        Field f = getDeclaredField(name);
        if ((f.getModifiers() & RConst.ACC_PUBLIC) != 0) {
            return f;
        }
        throw new NoSuchFieldException();
    }

    public Field getDeclaredField(String name) throws NoSuchFieldException {
        if (refClass == null) {
            refClass = new ReflectClass(classHandle);
        }
        ReflectField rf = refClass.getField(name);
        if (rf != null) {
            return new Field(this, rf);
        }
        throw new NoSuchFieldException();
    }

    public Field[] getFields() {
        if (refClass == null) {
            refClass = new ReflectClass(classHandle);
        }
        ReflectField[] rfs = refClass.getFields();
        int fcount = 0;
        for (ReflectField rf : rfs) {
            if ((rf.accessFlags & RConst.ACC_PUBLIC) != 0) {
                fcount++;
            }
        }

        Field[] fs = new Field[fcount];
        for (int i = 0; i < fcount; i++) {
            if ((rfs[i].accessFlags & RConst.ACC_PUBLIC) != 0) {
                fs[i] = new Field(this, rfs[i]);
            }
        }
        return fs;
    }

    public Field[] getDeclaredFields() {
        if (refClass == null) {
            refClass = new ReflectClass(classHandle);
        }
        ReflectField[] rfs = refClass.getFields();
        int fcount = rfs.length;

        Field[] fs = new Field[fcount];
        for (int i = 0; i < fcount; i++) {
            fs[i] = new Field(this, rfs[i]);
        }
        return fs;
    }

    public Class getComponentType() {
        if (isArray()) {
            String n = getName();
            if ("[Z".equals(n)) {
                return boolean.class;
            } else if ("[B".equals(n)) {
                return byte.class;
            } else if ("[S".equals(n)) {
                return short.class;
            } else if ("[C".equals(n)) {
                return char.class;
            } else if ("[I".equals(n)) {
                return int.class;
            } else if ("[F".equals(n)) {
                return float.class;
            } else if ("[J".equals(n)) {
                return long.class;
            } else if ("[D".equals(n)) {
                return double.class;
            }
            String objname = n.substring(n.indexOf("[L") + 2, n.length() - 1);
            return RefNative.getClassByName(objname);
        } else {
            return null;
        }
    }

    public Class getDeclaringClass() {
        if (isPrimitive() || isArray()) {
            return null;
        } else {
            return this;//todo
        }
    }

    public boolean desiredAssertionStatus() {
        return false;
    }

    public T cast(Object obj) {
        if (obj != null && !isInstance(obj)) {
            throw new ClassCastException();
        }
        return (T) obj;
    }

}
