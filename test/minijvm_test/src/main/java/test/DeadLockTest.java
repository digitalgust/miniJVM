package test;

import org.mini.vm.RefNative;

/**
 * A thread stopped while inside a synchronized block must not leave the
 * monitor abandoned: the next user of that object would deadlock forever
 * (real-world case: NetConnection$Receiver stopped while holding a
 * Properties monitor; the GUI thread later blocked on getProperty).
 */
public class DeadLockTest {

    static final Object props = new Object();
    static volatile boolean exitLoop;
    static final Object blockedMonitor = new Object();
    static volatile boolean blockedOwnerReady;
    static volatile boolean releaseBlockedOwner;

    static class Holder extends Thread {
        public void run() {
            // hold the monitor for the whole life of the thread; only the
            // exit-cleanup / abandoned-recovery can ever free it
            synchronized (props) {
                while (!exitLoop) {
                    try {
                        props.wait(1000);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    static class Grabber extends Thread {
        public void run() {
            synchronized (props) {
                props.hashCode();
            }
        }
    }

    static class BlockingOwner extends Thread {
        public void run() {
            synchronized (blockedMonitor) {
                blockedOwnerReady = true;
                while (!releaseBlockedOwner) {
                    Thread.yield();
                }
            }
        }
    }

    static class BlockedEntrant extends Thread {
        public void run() {
            synchronized (blockedMonitor) {
                System.out.println("DEADLOCK FAIL: stopped entrant acquired monitor");
            }
        }
    }

    static void testStopWhileEnteringMonitor() throws Exception {
        BlockingOwner owner = new BlockingOwner();
        owner.start();
        while (!blockedOwnerReady) Thread.sleep(10);

        BlockedEntrant entrant = new BlockedEntrant();
        entrant.start();
        Thread.sleep(100); //entrant is now blocked inside jthread_lock
        RefNative.stopThread(entrant, null);
        entrant.join(3000);
        if (entrant.isAlive()) {
            System.out.println("DEADLOCK FAIL: monitor entrant survived stop");
            System.exit(1);
        }

        releaseBlockedOwner = true;
        owner.join(3000);
        if (owner.isAlive()) {
            System.out.println("DEADLOCK FAIL: monitor owner did not exit");
            System.exit(1);
        }
        synchronized (blockedMonitor) {
            blockedMonitor.hashCode();
        }
        System.out.println("DEADLOCK BLOCKED-ENTER OK");
    }

    public static void main(String[] args) throws Exception {
        Holder h = new Holder();
        h.start();
        Thread.sleep(300);          // let it enter and hold the monitor
        exitLoop = true;
        RefNative.stopThread(h, null);
        h.join(3000);
        if (h.isAlive()) {
            System.out.println("DEADLOCK FAIL: holder survived stop");
            System.exit(1);
        }
        // now a fresh thread wants the same monitor. Before the fix this
        // blocked forever (main's join below timed out).
        Grabber g = new Grabber();
        g.setDaemon(true);
        g.start();
        g.join(10000);
        if (g.isAlive()) {
            System.out.println("DEADLOCK FAIL: monitor abandoned by stopped thread");
            System.exit(1);
        }
        // and main itself (synchronized method entry) must not hang either
        long t0 = System.currentTimeMillis();
        synchronized (props) {
            props.hashCode();
        }
        testStopWhileEnteringMonitor();
        System.out.println("DEADLOCK OK (grabbed in " + (System.currentTimeMillis() - t0) + "ms)");
        System.exit(0);
    }
}
