package test;

/**
 * System.exit() while a GC cycle is mid-pause must not deadlock:
 * thread_stop_all() marks every thread no_pause=1, so the GC's
 * stop-the-world wait would spin forever unless it abandons the cycle
 * when exit_flag is set.
 */
public class ExitDeadlockTest {

    static final Object a = new Object();
    static final Object b = new Object();

    public static void main(String[] args) throws Exception {
        // thread 1: parks holding monitor a (GC counts it as paused)
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    synchronized (a) {
                        try {
                            a.wait(500);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        });
        // thread 2: permanently contends monitor a from the outside
        Thread t2 = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    synchronized (a) {
                        b.hashCode();
                    }
                }
            }
        });
        t1.start();
        t2.start();
        // churn objects so GC cycles overlap with the exit window
        for (int i = 0; i < 60; i++) {
            byte[] junk = new byte[200 * 1024];
            junk[0] = 1;
            Thread.sleep(5);
        }
        System.out.println("EXIT-DEADLOCK OK (exiting)");
        System.exit(0);
    }
}
