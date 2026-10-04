package test;

import java.lang.ref.WeakReference;

/**
 * An object locked by synchronized() must become collectable once no Java
 * reference remains - regardless of how the synchronized region exited
 * (normal / exception / thread-stop). A leaked monitor entry would pin the
 * object through the GC's active-monitor root scan, and through it the
 * whole defining classloader.
 */
public class MonitorLeakTest {

    static WeakReference<Object> wr;
    static volatile boolean exitLoop;

    static class Worker extends Thread {
        public void run() {
            // park holding nothing, synchronizing on the target object
            while (!exitLoop) {
                Object target = wr.get();
                if (target == null) return;
                synchronized (target) {
                    try {
                        target.wait(50);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    static void gcRounds() throws InterruptedException {
        for (int i = 0; i < 6; i++) {
            System.gc();
            Thread.sleep(80);
        }
    }

    public static void main(String[] args) throws Exception {
        // 1) normal exit
        {
            Object o = new Object();
            synchronized (o) {
                wr = new WeakReference<Object>(o);
            }
            o = null;
            gcRounds();
            System.out.println("normal-exit: " + (wr.get() == null ? "collected OK" : "LEAK"));
            if (wr.get() != null) fail("normal exit leaked");
        }
        // 2) exception exit from synchronized block
        {
            Object o = new Object();
            wr = new WeakReference<Object>(o);
            try {
                synchronized (o) {
                    throw new RuntimeException("boom");
                }
            } catch (RuntimeException e) {
                // javac's any-handler should have run monitorexit
            }
            o = null;
            gcRounds();
            System.out.println("exception-exit: " + (wr.get() == null ? "collected OK" : "LEAK"));
            if (wr.get() != null) fail("exception exit leaked");
        }
        // 3) main thread still HOLDS a lock on the object, then drops the ref:
        //    the object must stay alive (active monitor root), then after
        //    unlock + gc it must die
        {
            Object o = new Object();
            wr = new WeakReference<Object>(o);
            synchronized (o) {
                o = null;          // ref dropped while locked
                gcRounds();        // multiple GCs while held
                if (wr.get() == null) fail("locked object collected while held!");
            }
            gcRounds();            // released now
            System.out.println("held-then-release: " + (wr.get() == null ? "collected OK" : "LEAK"));
            if (wr.get() != null) fail("lock released but object leaked");
        }
        // 4) stopped thread inside a synchronized region: after the thread
        //    dies its exit cleanup must release the monitor
        {
            Object o = new Object();
            wr = new WeakReference<Object>(o);
            exitLoop = false;
            Worker w = new Worker();
            w.start();
            Thread.sleep(300);     // let it enter synchronized(target)
            exitLoop = true;
            o = null;
            org.mini.vm.RefNative.stopThread(w, null);
            w.join(3000);
            if (w.isAlive()) fail("worker survived stop");
            gcRounds();
            System.out.println("stop-exit: " + (wr.get() == null ? "collected OK" : "LEAK"));
            if (wr.get() != null) fail("stopped thread's monitor leaked");
        }
        System.out.println("MONITOR-LEAK ALL OK");
        System.exit(0);
    }

    static void fail(String msg) {
        System.out.println("MONITOR-LEAK FAIL: " + msg);
        System.exit(1);
    }
}
