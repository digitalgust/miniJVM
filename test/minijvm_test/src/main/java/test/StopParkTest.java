package test;

import org.mini.vm.RefNative;

/**
 * Thread.stop() must kill threads parked in Object.wait()/Thread.sleep(),
 * including the "catch-everything-and-loop" pattern real games use
 * (see the shl UpdaterWorker / PathFindingThread leak). is_stop is only
 * observed at method entry and between park slices, so a stop that cannot
 * wake a parked thread leaks its classloader forever.
 */
public class StopParkTest {

    static final Object panel = new Object();
    static volatile boolean exit = false;
    static volatile int wokeByNotify = -1;
    static volatile boolean notifyReady = false;
    static boolean notifyReleased = false; // guarded by panel
    static final int TIMED_WAIT_MS = 300;

    // exact shape of the leaked UpdaterWorker: wait(0) nobody notifies,
    // outer catch swallows interrupt, loop restarts
    static class WaitLoop implements Runnable {
        public void run() {
            while (!exit) {
                try {
                    synchronized (panel) {
                        panel.wait();
                    }
                } catch (Exception e) {
                    // swallow: must NOT save the thread
                }
            }
        }
    }

    // exact shape of PathFindingThread: sleep loop + catch everything
    static class SleepLoop implements Runnable {
        public void run() {
            while (!exit) {
                try {
                    Thread.sleep(300);
                } catch (Exception e) {
                }
            }
        }
    }

    // controls: notify must still wake a waiter; timed wait must still time out
    static class NotifiedWaiter implements Runnable {
        public void run() {
            synchronized (panel) {
                try {
                    notifyReady = true;
                    // Object.wait may return spuriously; only the condition permits progress.
                    while (!notifyReleased) panel.wait();
                    wokeByNotify = 1;
                } catch (Exception e) {
                    wokeByNotify = 0;
                }
            }
        }
    }

    static class TimedWaiter implements Runnable {
        public void run() {
            synchronized (panel) {
                try {
                    long t0 = System.nanoTime();
                    long remaining;
                    // A wait call does not guarantee a minimum blocking duration.
                    // Retry after spurious wakeups, using a monotonic elapsed-time budget.
                    while ((remaining = TIMED_WAIT_MS * 1000000L - (System.nanoTime() - t0)) > 0) {
                        panel.wait((remaining + 999999L) / 1000000L);
                    }
                    wokeByNotify = (int) ((System.nanoTime() - t0) / 1000000L);
                } catch (Exception e) {
                    wokeByNotify = -2;
                }
            }
        }
    }

    interface AliveProbe {
        boolean isAlive();
    }

    static boolean waitFor(AliveProbe probe, boolean want, int ms) throws InterruptedException {
        // probe re-evaluated every poll: a boolean[] snapshot would freeze
        // the first observation and fake "thread survived stop"
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            if (probe.isAlive() == want) return true;
            Thread.sleep(20);
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        // 1. stop kills a wait(0)-parked catch-everything thread
        Thread t1 = new Thread(new WaitLoop());
        t1.start();
        Thread.sleep(300);
        RefNative.stopThread(t1, null);
        if (!waitFor(new AliveProbe() { public boolean isAlive() { return t1.isAlive(); } }, false, 15000)) {
            System.out.println("STOP FAIL: wait(0) loop thread survived stop");
            System.exit(1);
        }
        System.out.println("stop-wait(0) ok, dead after " + "stop");

        // 2. stop kills a sleep-looping catch-everything thread
        exit = false;
        long ts0 = System.currentTimeMillis();
        Thread t2 = new Thread(new SleepLoop());
        t2.start();
        Thread.sleep(300);
        System.out.println("trace stop at +" + (System.currentTimeMillis() - ts0));
        RefNative.stopThread(t2, null);
        if (!waitFor(new AliveProbe() { public boolean isAlive() { return t2.isAlive(); } }, false, 15000)) {
            for (int k = 0; k < 2; k++) {
                System.out.println("observe alive=" + t2.isAlive() + " t=" + k);
                Thread.sleep(500);
            }
            try {
                t2.join(3000);
                System.out.println("join returned, alive=" + t2.isAlive());
            } catch (Exception e) {
                System.out.println("join threw " + e);
            }
            System.out.println("STOP FAIL: sleep loop thread survived stop");
            System.exit(1);
        }
        System.out.println("stop-sleep ok");

        // 3. notify still wakes a waiter (semantics preserved)
        wokeByNotify = -1;
        synchronized (panel) {
            notifyReady = false;
            notifyReleased = false;
        }
        Thread t3 = new Thread(new NotifiedWaiter());
        t3.start();
        long readyDeadline = System.nanoTime() + 2000000000L;
        while (!notifyReady && System.nanoTime() - readyDeadline < 0) Thread.sleep(10);
        if (!notifyReady) {
            System.out.println("STOP FAIL: notify waiter did not start");
            System.exit(1);
        }
        // Even a real notification must not bypass the still-false condition.
        synchronized (panel) {
            panel.notifyAll();
        }
        Thread.sleep(150);
        if (wokeByNotify != -1 || !t3.isAlive()) {
            System.out.println("STOP FAIL: waiter proceeded before notification condition");
            System.exit(1);
        }
        synchronized (panel) {
            notifyReleased = true;
            panel.notify();
        }
        t3.join(2000);
        if (t3.isAlive() || wokeByNotify != 1) {
            System.out.println("STOP FAIL: notify did not wake waiter: " + wokeByNotify);
            System.exit(1);
        }
        System.out.println("notify ok");

        // 4. timed wait still returns by timeout
        wokeByNotify = -1;
        Thread t4 = new Thread(new TimedWaiter());
        t4.start();
        t4.join(2000);
        if (t4.isAlive() || wokeByNotify < TIMED_WAIT_MS) {
            System.out.println("STOP FAIL: timed wait returned at " + wokeByNotify + "ms");
            System.exit(1);
        }
        System.out.println("timed-wait ok (~" + wokeByNotify + "ms)");

        // 5. the game scenario: two stubborn threads + join pattern from closeThreads
        exit = false;
        Thread a = new Thread(new WaitLoop());
        Thread b = new Thread(new SleepLoop());
        a.start();
        b.start();
        Thread.sleep(300);
        a.stop();
        b.stop();
        a.join(2000);
        b.join(2000);
        if (a.isAlive() || b.isAlive()) {
            System.out.println("STOP FAIL: game-pattern threads survived (a=" + a.isAlive() + " b=" + b.isAlive() + ")");
            System.exit(1);
        }
        System.out.println("STOP-PARK ALL OK");
    }
}
