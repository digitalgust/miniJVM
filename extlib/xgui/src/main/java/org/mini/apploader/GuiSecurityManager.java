package org.mini.apploader;

import org.mini.gui.callback.GCallBack;

import java.io.File;
import java.security.Permission;
import java.util.HashSet;
import java.util.Set;

/**
 * 自定义安全管理器
 * 不允许跨Application的访问
 *
 * @author Gust
 */
public class GuiSecurityManager extends SecurityManager {
    static RuntimePermission PERMISSION_TOKEN = new RuntimePermission("accessPassword");
    // 允许访问的文件路径
    // 允许访问的线程名称
    private final Set<String> allowedThreadNames = new HashSet<>();

    // 允许访问的类名
    private final Set<String> allowedCaller = new HashSet<>();
    private final Set<String> declinedCaller = new HashSet<>();

    public GuiSecurityManager() {
    }

    // 添加允许访问的线程名称
    public void addAllowedThread(String threadName) {
        allowedThreadNames.add(threadName);
    }

    // 添加允许访问的类名
    public void addAllowedCaller(String className, String methodName) {
        allowedCaller.add(className + "." + methodName);
    }

    public void addDeclinedCaller(String className, String methodName) {
        declinedCaller.add(className + "." + methodName);
    }

    @Override
    public void checkPermission(Permission perm) {
        // 默认允许所有权限
        switch (perm.getName()) {
            case "accessPassword":
                if (Thread.currentThread().getContextClassLoader() instanceof StandalongGuiAppClassLoader) {
                    throw new SecurityException("Access declined: " + perm);
                }
                if (isAccessAllowed() || isAppManager()) {

                } else {
                    if (isAccessDeclined()) {
                        throw new SecurityException("Access declined: " + perm);
                    }
                }
                break;
            case "setSecurityManager":
                throw new SecurityException("Can't setSecurityManager");

        }
        super.checkPermission(perm);
    }

    @Override
    public void checkDelete(String filePath) {
        checkRestrictedFile(filePath);
        super.checkDelete(filePath);
    }

    @Override
    public void checkRead(String filePath) {
        checkRestrictedFile(filePath);
        super.checkRead(filePath);
    }

    @Override
    public void checkWrite(String filePath) {
        checkRestrictedFile(filePath);
        super.checkWrite(filePath);
    }

    private void checkRestrictedFile(String filePath) {
        if (isAccessAllowed()) {
            return;
        }
        //当前处于应用程序中
        if (Thread.currentThread().getContextClassLoader() instanceof StandalongGuiAppClassLoader) {

            // 检查是否是受限文件
            String saveRootFilePath = GCallBack.getInstance().getAppSaveRoot();
            File saveRootFile = new File(saveRootFilePath);
            String absSaveRoot = saveRootFile.getAbsolutePath();
            if (GCallBack.getInstance().getApplication() != null) {
                String appSaveRoot = GCallBack.getInstance().getApplication().getSaveRoot();
                File appSaveRootFile = new File(appSaveRoot);
                String absAppSaveRoot = appSaveRootFile.getAbsolutePath();
                File file = new File(filePath);
                String absolutePath = file.getAbsolutePath();

                if (absolutePath.startsWith(absSaveRoot)) {//如果是访问保存目录下的文件
                    if (!absolutePath.startsWith(absAppSaveRoot)) { // 如果不是访问当前Application的保存目录下的文件
//                        System.out.println("absSaveRoot= " + absSaveRoot);
//                        System.out.println("absAppSaveRoot= " + absAppSaveRoot);
//                        System.out.println("absolutePath= " + absolutePath);
                        throw new SecurityException("Access declined: " + file);
                    }
                }
            }
        }
    }

    private boolean isAppManager() {
        return GCallBack.getInstance().getApplication() instanceof AppManager;
    }

    // 判断当前访问是否被允许
    private boolean isAccessAllowed() {
        // 检查当前线程名称
        String currentThreadName = Thread.currentThread().getName();
        if (allowedThreadNames.contains(currentThreadName)) {
            return true;
        }

        // 检查调用栈中的类
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            String methodName = element.getMethodName();
            if (allowedCaller.contains(className + "." + methodName)) {
                return true;
            }
        }

        return false;
    }

    private boolean isAccessDeclined() {
        // 检查调用栈中的类
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            String methodName = element.getMethodName();
            if (declinedCaller.contains(className + "." + methodName)) {
                return true;
            }
        }

        return false;
    }

    private void checkClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != ClassLoader.getSystemClassLoader()) {
            throw new SecurityException("Access declined: " + classLoader);
        }
    }

}