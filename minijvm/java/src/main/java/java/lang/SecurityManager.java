package java.lang;

import java.io.FileDescriptor;
import java.security.AccessController;
import java.security.Permission;

public class SecurityManager {

    public void checkAccept(String host, int port) {

    }

    public void checkConnect(String host, int port) {

    }

    public void checkConnect(String host, int port, Object context) {

    }

    public void checkDelete(String file) {

    }

    public void checkExec(String cmd) {

    }

    public void checkPermission(Permission perm) {
    }

    public void checkPermission(Permission perm, Object context) {
    }

    public void checkAccess(Thread t) {

    }

    public void checkAccess(ThreadGroup g) {

    }

    public void checkCreateClassLoader() {

    }

    public void checkPrintJobAccess() {
    }

    public void checkRead(FileDescriptor fd) {
    }

    public void checkRead(String file) {
    }

    public void checkRead(String file, Object context) {

    }

    public void checkSecurityAccess(String target) {

    }

    public void checkSetFactory() {

    }

    public void checkWrite(FileDescriptor fd) {

    }

    public void checkWrite(String file) {

    }

    public Object getSecurityContext() {
        return AccessController.getContext();
    }

    public ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }
}
