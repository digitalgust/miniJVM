

package java.lang;

import java.security.*;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.net.InetAddress;
import java.lang.reflect.Member;


public class SecurityManager {
    public static final RuntimePermission CHECK_MEMBER_ACCESS_PERMISSION = new RuntimePermission("accessDeclaredMembers");

    @Deprecated
    protected boolean inCheck;


    private boolean initialized = false;


    private boolean hasAllPermission() {
        return false;
    }


    @Deprecated
    public boolean getInCheck() {
        return inCheck;
    }


    public SecurityManager() {
        synchronized (SecurityManager.class) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {


                sm.checkPermission(new RuntimePermission
                        ("createSecurityManager"));
            }
            initialized = true;
        }
    }


    protected Class[] getClassContext() {
        List<Class> classes = new ArrayList<>();
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stackTrace.length; i++) { // 跳过前两个元素（Thread.getStackTrace 和 checkMemberAccess）
            Class callerClass = stackTrace[i].getDeclaringClass();
            classes.add(callerClass);
        }
        return classes.toArray(new Class[0]);
    }


    public Object getSecurityContext() {
        return AccessController.getContext();
    }


    public void checkPermission(Permission perm) {
        AccessController.checkPermission(perm);
    }


    public void checkPermission(Permission perm, Object context) {
        if (context instanceof AccessControlContext) {
            ((AccessControlContext) context).checkPermission(perm);
        } else {
            throw new SecurityException();
        }
    }


    public void checkCreateClassLoader() {
    }


    private static ThreadGroup rootGroup = getRootGroup();

    private static ThreadGroup getRootGroup() {
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
    }


    public void checkAccess(Thread t) {
    }

    public void checkAccess(ThreadGroup g) {
    }


    public void checkExit(int status) {
        checkPermission(new RuntimePermission("exitVM." + status));
    }


    public void checkExec(String cmd) {
    }


    public void checkLink(String lib) {
        if (lib == null) {
            throw new NullPointerException("library can't be null");
        }
        checkPermission(new RuntimePermission("loadLibrary." + lib));
    }


    public void checkRead(FileDescriptor fd) {
        if (fd == null) {
            throw new NullPointerException("file descriptor can't be null");
        }
        checkPermission(new RuntimePermission("readFileDescriptor"));
    }


    public void checkRead(String file) {
    }


    public void checkRead(String file, Object context) {
    }


    public void checkWrite(FileDescriptor fd) {
        if (fd == null) {
            throw new NullPointerException("file descriptor can't be null");
        }
        checkPermission(new RuntimePermission("writeFileDescriptor"));

    }


    public void checkWrite(String file) {
    }


    public void checkDelete(String file) {
    }


    public void checkConnect(String host, int port) {
    }


    public void checkConnect(String host, int port, Object context) {
    }


    public void checkListen(int port) {
    }


    public void checkAccept(String host, int port) {
    }


    public void checkMulticast(InetAddress maddr) {
    }


    @Deprecated
    public void checkMulticast(InetAddress maddr, byte ttl) {
    }


    public void checkPropertiesAccess() {
    }


    public void checkPropertyAccess(String key) {
    }


    public boolean checkTopLevelWindow(Object window) {
        return false;
    }


    public void checkPrintJobAccess() {
        checkPermission(new RuntimePermission("queuePrintJob"));
    }


    public void checkSystemClipboardAccess() {
    }


    public void checkAwtEventQueueAccess() {
    }


    private static boolean packageAccessValid = false;
    private static String[] packageAccess;
    private static final Object packageAccessLock = new Object();

    private static boolean packageDefinitionValid = false;
    private static String[] packageDefinition;
    private static final Object packageDefinitionLock = new Object();

    private static String[] getPackages(String p) {
        String packages[] = null;
        if (p != null && !p.equals("")) {
            java.util.StringTokenizer tok =
                    new java.util.StringTokenizer(p, ",");
            int n = tok.countTokens();
            if (n > 0) {
                packages = new String[n];
                int i = 0;
                while (tok.hasMoreElements()) {
                    String s = tok.nextToken().trim();
                    packages[i++] = s;
                }
            }
        }

        if (packages == null)
            packages = new String[0];
        return packages;
    }


    public void checkPackageAccess(String pkg) {
        if (pkg == null) {
            throw new NullPointerException("package name can't be null");
        }

        String[] pkgs;
        synchronized (packageAccessLock) {

            if (!packageAccessValid) {
                String tmpPropertyStr =
                        AccessController.doPrivileged(
                                new PrivilegedAction<String>() {
                                    public String run() {
                                        return Security.getProperty(
                                                "package.access");
                                    }
                                }
                        );
                packageAccess = getPackages(tmpPropertyStr);
                packageAccessValid = true;
            }


            pkgs = packageAccess;
        }


        for (int i = 0; i < pkgs.length; i++) {
            if (pkg.startsWith(pkgs[i]) || pkgs[i].equals(pkg + ".")) {
                checkPermission(
                        new RuntimePermission("accessClassInPackage." + pkg));
                break;
            }
        }
    }


    public void checkPackageDefinition(String pkg) {
        if (pkg == null) {
            throw new NullPointerException("package name can't be null");
        }

        String[] pkgs;
        synchronized (packageDefinitionLock) {

            if (!packageDefinitionValid) {
                String tmpPropertyStr =
                        AccessController.doPrivileged(
                                new PrivilegedAction<String>() {
                                    public String run() {
                                        return Security.getProperty(
                                                "package.definition");
                                    }
                                }
                        );
                packageDefinition = getPackages(tmpPropertyStr);
                packageDefinitionValid = true;
            }


            pkgs = packageDefinition;
        }


        for (int i = 0; i < pkgs.length; i++) {
            if (pkg.startsWith(pkgs[i]) || pkgs[i].equals(pkg + ".")) {
                checkPermission(
                        new RuntimePermission("defineClassInPackage." + pkg));
                break;
            }
        }
    }


    public void checkSetFactory() {
        checkPermission(new RuntimePermission("setFactory"));
    }


    public void checkMemberAccess(Class<?> clazz, int which) {
        if (clazz == null) {
            throw new NullPointerException("class can't be null");
        }
        if (which != Member.PUBLIC) {
            Class stack[] = getClassContext();

            if ((stack.length < 4) ||
                    (stack[3].getClassLoader() != clazz.getClassLoader())) {
                checkPermission(CHECK_MEMBER_ACCESS_PERMISSION);
            }
        }
    }


    public void checkSecurityAccess(String target) {
        checkPermission(new SecurityPermission(target));
    }


    public ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }

}
