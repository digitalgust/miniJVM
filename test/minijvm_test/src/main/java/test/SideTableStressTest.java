package test;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrent side-table stress: many mutators creating WeakReference and
 * finalizable objects at once, then GC. Guards the GC-side-list registration
 * race (entries lost / list corrupted when pushes are not serialized).
 * Uses only volatile+AtomicInteger barriers: miniJVM has no CountDownLatch.
 */
public class SideTableStressTest {

    static int THREADS = 8;
    static int PER_THREAD = 20000;
    // every 16th iteration allocates a Fin -> expected finalize() count
    static final int FIN_STEP = 16;
    static final int WEAK_STEP = 64;

    static final AtomicInteger finalized = new AtomicInteger();
    static final AtomicInteger nextId = new AtomicInteger();
    static final java.util.ArrayList<Integer> finalizedIds = new java.util.ArrayList<Integer>();
    static AtomicInteger remaining = new AtomicInteger(8);
    static volatile boolean go = false;

    static class Fin {
        static int createdCount = 0;
        final int id;

        Fin() {
            id = nextId.incrementAndGet();
            synchronized (SideTableStressTest.class) {
                createdCount++;
            }
        }

        public void finalize() {
            synchronized (finalizedIds) {
                finalizedIds.add(Integer.valueOf(id));
            }
            finalized.incrementAndGet();
        }
    }

    // isolated so the instance is provably unreachable on return
    static void dropFinalizable() {
        Fin f = new Fin();
        if (f.hashCode() == 0) System.out.print(""); // keep alloc alive
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) THREADS = Integer.parseInt(args[0]);
        remaining.set(THREADS);
        if (args.length > 1) PER_THREAD = Integer.parseInt(args[1]);
        final int weakPerThread = (PER_THREAD + WEAK_STEP - 1) / WEAK_STEP;
        final WeakReference<?>[] refs = new WeakReference<?>[THREADS * weakPerThread];

        for (int t = 0; t < THREADS; t++) {
            final int tid = t;
            Thread th = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (!go) Thread.sleep(1);
                        for (int i = 0; i < PER_THREAD; i++) {
                            if ((i % WEAK_STEP) == 0) {
                                refs[tid * weakPerThread + (i / WEAK_STEP)] =
                                        new WeakReference<Object>(new byte[1024]);
                            }
                            if ((i % FIN_STEP) == 0) {
                                dropFinalizable();
                            }
                            if ((i % 3) == 0) {
                                byte[] plain = new byte[32]; // never side-registered
                                if (plain.length < 0) System.out.print("");
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        remaining.decrementAndGet();
                    }
                }
            });
            th.start();
        }

        go = true;
        while (remaining.get() > 0) Thread.sleep(5);

        int expectFin = THREADS * ((PER_THREAD + FIN_STEP - 1) / FIN_STEP);
        int expectWeak = THREADS * weakPerThread;

        // finalize() and weak clearing run after each cycle; a few rounds
        // with a pause make the counts deterministic
        for (int g = 0; g < 20; g++) {
            System.gc();
            Thread.sleep(100);
            if (g == 0) {
                try {
                    int rc = org.mini.vm.RefNative.dumpHeap("sidetable_round1.hprof", 0);
                    System.out.println("dumped round1 rc=" + rc
                            + " finafterround1=" + finalized.get());
                } catch (Throwable t) {
                    System.out.println("round1 dump failed: " + t);
                }
            }
            if (finalized.get() >= expectFin) {
                System.out.println("converged after " + (g + 1) + " rounds");
                break;
            }
        }

        int cleared = 0;
        int live = 0;
        for (int i = 0; i < refs.length; i++) {
            if (refs[i] != null) {
                if (refs[i].get() == null) cleared++;
                else live++;
            }
        }

        int fin = finalized.get();
        System.out.println("SideTableStress: created=" + Fin.createdCount
                + " finalize=" + fin
                + "/" + expectFin + " weakCleared=" + cleared
                + " weakLive=" + live + "/" + expectWeak);

        // which ids never finalized? (created ids are exactly 1..expectFin)
        if (fin < expectFin) {
            try {
                int rc = org.mini.vm.RefNative.dumpHeap("sidetable_missing.hprof", 0);
                System.out.println("dumped sidetable_missing.hprof rc=" + rc);
            } catch (Throwable t) {
                System.out.println("dump failed: " + t);
            }
            synchronized (finalizedIds) {
                java.util.Collections.sort(finalizedIds);
                StringBuilder sb = new StringBuilder("missing ids:");
                int prev = 0;
                int groups = 0;
                for (int i = 0; i < finalizedIds.size() && groups < 40; i++) {
                    int id = finalizedIds.get(i).intValue();
                    for (int miss = prev + 1; miss < id && groups < 40; miss++) {
                        sb.append(' ').append(miss);
                        groups++;
                    }
                    prev = id;
                }
                for (int miss = prev + 1; miss <= expectFin && groups < 40; miss++) {
                    sb.append(' ').append(miss);
                    groups++;
                }
                System.out.println(sb.toString());
            }
        }

        if (fin < expectFin) {
            System.out.println("SIDE FAIL: lost finalizable side entries: "
                    + fin + " < " + expectFin);
            System.exit(1);
        }
        if (fin > expectFin) {
            System.out.println("SIDE FAIL: finalize ran more than once per object: "
                    + fin + " > " + expectFin);
            System.exit(1);
        }
        if (cleared + live != expectWeak) {
            System.out.println("SIDE FAIL: lost weak side entries: "
                    + (cleared + live) + " != " + expectWeak);
            System.exit(1);
        }
        if (live > expectWeak / 10) {
            System.out.println("SIDE FAIL: weak targets not reclaimed: " + live);
            System.exit(1);
        }
        System.out.println("SIDE OK");
    }
}
