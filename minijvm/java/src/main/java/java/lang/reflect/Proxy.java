package java.lang.reflect;

import org.mini.vm.RefNative;
import org.mini.vm.VmUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;


public class Proxy implements java.io.Serializable {
    public static final String PROXY_PACKAGE = "com.sun.proxy";

    private static final Class<?>[] constructorParams = {InvocationHandler.class};


    protected InvocationHandler h;


    private Proxy() {
    }


    protected Proxy(InvocationHandler h) {
        Objects.requireNonNull(h);
        this.h = h;
    }


    private static final String proxyClassNamePrefix = "Proxy";


    private static final AtomicLong nextUniqueNumber = new AtomicLong();

    static Map<String, Class<?>> proxyClassCache = new HashMap();

    static String getKey(ClassLoader loader, Class<?>[] interfaces) {
        String proxyPkg = null;
        int accessFlags = Modifier.PUBLIC | Modifier.FINAL;


        for (Class<?> intf : interfaces) {
            int flags = intf.getModifiers();
            if (!Modifier.isPublic(flags)) {
                accessFlags = Modifier.FINAL;
                String name = intf.getName();
                int n = name.lastIndexOf('.');
                String pkg = ((n == -1) ? "" : name.substring(0, n + 1));
                if (proxyPkg == null) {
                    proxyPkg = pkg;
                } else if (!pkg.equals(proxyPkg)) {
                    throw new IllegalArgumentException(
                            "non-public interfaces from different packages");
                }
            }
        }

        if (proxyPkg == null) {

            proxyPkg = PROXY_PACKAGE + ".";
        }


        long num = nextUniqueNumber.getAndIncrement();
        String proxyName = proxyPkg + proxyClassNamePrefix + num;
        return proxyName;
    }

    public static Class<?> getProxyClass(ClassLoader loader, Class<?>... interfaces) throws IllegalArgumentException {
        String proxyName = getKey(loader, interfaces);
        Class<?> proxyClass = proxyClassCache.get(proxyName);
        if (proxyClass == null) {
            if (proxyName == null) proxyName = getKey(loader, interfaces);
            proxyClass = VmUtil.genProxyClass(loader, proxyName, interfaces);
        }
        return proxyClass;
    }


    public static Object newProxyInstance(ClassLoader loader,
                                          Class<?>[] interfaces,
                                          InvocationHandler h)
            throws IllegalArgumentException {
        Objects.requireNonNull(h);


        Class<?> cl = getProxyClass(loader, interfaces);


        try {

            final Constructor<?> cons = cl.getConstructor(constructorParams);
            final InvocationHandler ih = h;
            return cons.newInstance(new Object[]{h});
        } catch (IllegalAccessException | InstantiationException e) {
            throw new InternalError(e.toString());
        } catch (NoSuchMethodException e) {
            throw new InternalError(e.toString());
        }
    }


    public static boolean isProxyClass(Class<?> cl) {
        return Proxy.class.isAssignableFrom(cl) && proxyClassCache.containsValue(cl);
    }


    public static InvocationHandler getInvocationHandler(Object proxy)
            throws IllegalArgumentException {

        if (!isProxyClass(proxy.getClass())) {
            throw new IllegalArgumentException("not a proxy instance");
        }

        final Proxy p = (Proxy) proxy;
        final InvocationHandler ih = p.h;
        return ih;
    }

}
