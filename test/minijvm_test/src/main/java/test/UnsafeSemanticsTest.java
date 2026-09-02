package test;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Targeted audit of sun.misc.Unsafe (Java + native) semantics, 2026-09-02.
 * Cases marked [BUG-...] document current wrong behavior; they are EXPECTED to
 * print FAIL-style evidence lines, not to pass silently. The suite exits 0 only
 * if all "works" cases pass; evidence cases print EVID and count separately.
 */
public class UnsafeSemanticsTest {

    static int pass = 0, fail = 0, evid = 0;

    static void ok(String n) { pass++; System.out.println("[PASS] " + n); }
    static void ok(String n, boolean cond) { if (cond) ok(n); else bad(n, "expected true"); }
    static void bad(String n, String d) { fail++; System.out.println("[FAIL] " + n + " : " + d); }
    static void ev(String n, String d) { evid++; System.out.println("[EVID] " + n + " : " + d); }

    static Unsafe U = Unsafe.getUnsafe();

    // INSTANCE fields used for offset tests (static fields have no instance offset)
    int xInt;
    long xLong;
    Object xRef;
    boolean fBool = true;
    short fShort = 0x7788;
    char fChar = 0xBEEF;
    float fFloat = 1.5f;
    double fDouble = 2.25;

    static long offInt, offLong, offRef, offBool, offByte, offShort, offChar, offFloat, offDouble;

    static UnsafeSemanticsTest SELF = new UnsafeSemanticsTest();

    static long fieldOff(String name) throws Exception {
        Field f = UnsafeSemanticsTest.class.getDeclaredField(name);
        return U.objectFieldOffset(f);
    }

    // ================= 1. instance field access via offsets =================

    static void testFieldAccess() throws Exception {
        offInt = fieldOff("xInt");
        offLong = fieldOff("xLong");
        offRef = fieldOff("xRef");

        U.putInt(SELF, offInt, 123456789);
        ok("putInt/getInt field", SELF.xInt == 123456789 && U.getInt(SELF, offInt) == 123456789);

        U.putLong(SELF, offLong, 0x7FEDCBA987654321L);
        ok("putLong/getLong field", SELF.xLong == 0x7FEDCBA987654321L && U.getLong(SELF, offLong) == 0x7FEDCBA987654321L);

        Object marker = "marker";
        U.putObject(SELF, offRef, marker);
        ok("putObject/getObject field", SELF.xRef == marker && U.getObject(SELF, offRef) == marker);

        long ob = fieldOff("fBool");
        U.putBooleanVolatile(SELF, ob, false);
        ok("putBooleanVolatile", SELF.fBool == false && U.getBooleanVolatile(SELF, ob) == false);

        long os = fieldOff("fShort");
        U.putShort(SELF, os, (short) 0x1234);
        ok("putShort/getShort field", SELF.fShort == 0x1234);

        long oc = fieldOff("fChar");
        U.putChar(SELF, oc, '\uCAFE');
        ok("putChar/getChar field", SELF.fChar == 0xCAFE);

        long of = fieldOff("fFloat");
        U.putFloat(SELF, of, 9.75f);
        ok("putFloat/getFloat field", SELF.fFloat == 9.75f);

        long od = fieldOff("fDouble");
        U.putDouble(SELF, od, 8.5d);
        ok("putDouble/getDouble field", SELF.fDouble == 8.5d);

        U.putOrderedLong(SELF, offLong, 42L);
        ok("putOrderedLong", SELF.xLong == 42L);
    }

    // ================= 2. direct memory =================

    static void testDirectMemory() throws Exception {
        long buf = U.allocateMemory(64);
        for (int i = 0; i < 64; i++) U.putByte(buf + i, (byte) i);
        boolean okb = true;
        for (int i = 0; i < 64; i++) okb &= U.getByte(buf + i) == (byte) i;
        ok("allocateMemory/putByte/getByte", okb);

        U.putLong(buf, 0x0102030405060708L);
        ok("putLong/getLong direct", U.getLong(buf) == 0x0102030405060708L);
        ok("putInt/getInt direct", U.getInt(buf) == 0x05060708);
        U.putDouble(buf, 0.5d);
        ok("putDouble/getDouble direct", U.getDouble(buf) == 0.5d);

        // setMemory with (Object)null base, offset = absolute address.
        // Probe at buf+16 INSIDE a 64B allocation so an off-by-one write
        // cannot hit allocator metadata (which crashed GC later before).
        long buf2 = U.allocateMemory(64);
        U.setMemory(null, buf2 + 16, 8, (byte) 0x7A);
        int first = U.getByte(buf2 + 16) & 0xff;
        int last = U.getByte(buf2 + 23) & 0xff;
        int before = U.getByte(buf2 + 15) & 0xff;
        ok("setMemory(null,addr,..) exact", first == 0x7A && last == 0x7A && before == 0);
        U.freeMemory(buf2);

        // copyMemory 3-arg direct form
        long src = U.allocateMemory(8);
        long dst = U.allocateMemory(8);
        for (int i = 0; i < 8; i++) U.putByte(src + i, (byte) (10 + i));
        U.copyMemory(src, dst, 8);
        boolean exact = true, shifted = true;
        for (int i = 0; i < 8; i++) {
            exact &= (U.getByte(dst + i) & 0xff) == 10 + i;
            shifted &= i > 0 && (U.getByte(dst + i) & 0xff) == 10 + i - 1;
        }
        ok("copyMemory(long,long,long) exact", exact);
        U.freeMemory(src);
        U.freeMemory(dst);
        U.freeMemory(buf);
    }

    // ================= 3. array element access via base+offset model =================

    static void testArrays() throws Exception {
        int[] a = new int[4];
        int scale = U.arrayIndexScale(int[].class);
        int base = U.arrayBaseOffset(int[].class);
        for (int i = 0; i < 4; i++) U.putInt(a, (long) base + (long) i * scale, 100 + i);
        boolean ok = a[0] == 100 && a[1] == 101 && a[2] == 102 && a[3] == 103;
        ok("array putInt base+index*scale (scale=" + scale + ")", ok);

        long[] la = new long[2];
        int lscale = U.arrayIndexScale(long[].class);
        U.putLong(la, (long) lscale, 0x100000000L);
        ok("array putLong (scale=" + lscale + ")", la[1] == 0x100000000L);

        String[] sa = new String[2];
        int rscale = U.arrayIndexScale(String[].class);
        U.putObject(sa, (long) rscale, "x");
        ok("array putObject (scale=" + rscale + ")", "x".equals(sa[1]));

        int[][] na = new int[3][];
        int nscale = U.arrayIndexScale(int[][].class);
        int[] inner = {7};
        U.putObject(na, (long) 2 * nscale, inner);
        ok("nested array putObject (scale=" + nscale + ")", na[2] == inner);
    }

    // ================= 4. CAS =================

    static void testCas() {
        U.putInt(SELF, offInt, 5);
        boolean r1 = U.compareAndSwapInt(SELF, offInt, 5, 6);
        boolean r2 = U.compareAndSwapInt(SELF, offInt, 5, 7);
        ok("CAS int hit/miss", r1 && !r2 && SELF.xInt == 6);

        U.putLong(SELF, offLong, 0x100000000L); // needs real 64-bit CAS
        boolean r3 = U.compareAndSwapLong(SELF, offLong, 0x100000000L, 0x200000000L);
        ok("CAS long 64-bit value", r3 && SELF.xLong == 0x200000000L);
        U.putLong(SELF, offLong, 1L);
        ok("CAS long small value", U.compareAndSwapLong(SELF, offLong, 1L, 2L) && SELF.xLong == 2L);
        U.putLong(SELF, offLong, 0x200000000L);

        boolean r4 = U.compareAndSwapLong(SELF, offLong, 0x100000000L, 1L);
        ok("CAS long miss on high bits", !r4 && SELF.xLong == 0x200000000L);

        Object a = "a", b = "b";
        U.putObject(SELF, offRef, a);
        boolean r5 = U.compareAndSwapObject(SELF, offRef, a, b);
        ok("CAS object", r5 && SELF.xRef == b);

        // 64-bit-only bit patterns must round-trip through CAS success
        U.putLong(SELF, offLong, 0x1234567890ABCDEFL);
        boolean r6 = U.compareAndSwapLong(SELF, offLong, 0x1234567890ABCDEFL, 0x0FEDCBA098765432L);
        ok("CAS long full-width bits", r6 && SELF.xLong == 0x0FEDCBA098765432L);
    }

    static void testCasConcurrency() throws Exception {
        final AtomicInteger ai = new AtomicInteger(0);
        final AtomicLong al = new AtomicLong(0);
        final AtomicReference<Integer> ar = new AtomicReference<Integer>(0);
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 50000; i++) {
                    ai.incrementAndGet();
                    al.incrementAndGet();
                    ar.set(ar.get() + 1);
                }
            }
        });
        Thread t2 = new Thread(t1);
        t1.start(); t2.start();
        t1.join(); t2.join();
        ok("AtomicInteger concurrent", ai.get() == 100000);
        ok("AtomicLong concurrent", al.get() == 100000);
        // AtomicReference get/set is not CAS-atomic; just record
        ev("AtomicReference set/get race", "final=" + ar.get() + " (non-atomic by design here)");
    }

    // ================= 5. park/unpark =================

    static volatile String parkState = "";
    static volatile long parkElapsed = -1;

    static void testPark() throws Exception {
        // (a) park(0) with NO unpark: JDK = block indefinitely; measure how long it actually stayed
        final Thread main = Thread.currentThread();
        Thread observer = new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(200); } catch (InterruptedException e) { }
                parkState = "still-parked-after-200ms";
            }
        });
        long t0 = System.currentTimeMillis();
        final Thread pt = new Thread(new Runnable() {
            public void run() {
                long s = System.currentTimeMillis();
                LockSupport.park(); // park(false, 0)
                parkElapsed = System.currentTimeMillis() - s;
                parkState = "returned";
            }
        });
        observer.start();
        pt.start();
        Thread.sleep(400);
        if ("still-parked-after-200ms".equals(parkState) && pt.isAlive()) {
            ok("park() blocks until unpark");
            LockSupport.unpark(pt);
            pt.join(2000);
            ok("unpark wakes park", parkElapsed >= 150);
        } else {
            pt.join(2000);
            bad("park() blocks until unpark", "park returned after ~" + parkElapsed + "ms without unpark (JDK: block indefinitely)");
        }
        observer.join(2000);

        // (b) permit pre-grant: unpark before park must make park return immediately
        Thread pt2 = new Thread(new Runnable() {
            public void run() {
                long s = System.currentTimeMillis();
                LockSupport.park();
                parkElapsed = System.currentTimeMillis() - s;
            }
        });
        pt2.start();
        Thread.sleep(50);
        LockSupport.unpark(pt2); // park may or may not be inside yet; permit is remembered
        pt2.join(2000);
        ok("unpark-then-park permit", parkElapsed >= 0 && parkElapsed < 1500);

        // (c) parkNanos timing
        Thread pt3 = new Thread(new Runnable() {
            public void run() {
                long s = System.currentTimeMillis();
                LockSupport.parkNanos(200000000L); // 200ms
                parkElapsed = System.currentTimeMillis() - s;
            }
        });
        pt3.start(); pt3.join(3000);
        if (parkElapsed >= 150) ok("parkNanos(200ms) waits ~" + parkElapsed + "ms", true);
        else bad("parkNanos(200ms)", "returned after " + parkElapsed + "ms");
    }

    // ================= 6. missing native: getAddress/putAddress =================

    static void testGetAddress() {
        try {
            long buf = U.allocateMemory(32);
            U.putLong(buf, 0x0011223344556677L); // 8 bytes == pointer width on 64-bit
            long v = U.getAddress(buf);
            ok("getAddress", v == 0x0011223344556677L);
            U.putAddress(buf, 0x00AABBCCDDEEFF00L);
            ok("putAddress", U.getLong(buf) == 0x00AABBCCDDEEFF00L);
            U.freeMemory(buf);
        } catch (Throwable t) {
            bad("getAddress/putAddress", t.toString());
        }
    }

    // ================= 7. static field offset model =================

    static long staticVal = 0x0BADF00DCAFEBABEL;

    static void testStaticField() throws Exception {
        Field f = UnsafeSemanticsTest.class.getDeclaredField("staticVal");
        long addr = U.staticFieldOffset(f); // absolute address of the slot
        Object base = U.staticFieldBase(f); // null by design
        long got = U.getLong(base, addr);
        ok("staticField read", got == 0x0BADF00DCAFEBABEL);
        U.putLong(base, addr, 0x1234567890ABCDEFL);
        ok("staticField write", staticVal == 0x1234567890ABCDEFL);
        staticVal = 0x0BADF00DCAFEBABEL;
        // instance/static rejection
        try {
            U.objectFieldOffset(f);
            bad("objectFieldOffset(static) rejected", "no exception");
        } catch (IllegalArgumentException e) {
            ok("objectFieldOffset(static) rejected");
        }
        try {
            U.staticFieldOffset(UnsafeSemanticsTest.class.getDeclaredField("xInt"));
            bad("staticFieldOffset(instance) rejected", "no exception");
        } catch (IllegalArgumentException e) {
            ok("staticFieldOffset(instance) rejected");
        }
    }

    // ================= 8. interrupt during park =================

    static volatile String interruptResult = "";
    static volatile boolean flagAfterPark = false;

    static void testParkInterrupt() throws Exception {
        Thread pt = new Thread(new Runnable() {
            public void run() {
                try {
                    LockSupport.park();
                    interruptResult = "returned-normally";
                    flagAfterPark = Thread.currentThread().isInterrupted();
                } catch (Throwable t) {
                    interruptResult = "THREW:" + t;
                }
            }
        });
        pt.start();
        Thread.sleep(300); // thread must be blocked in park now (no 1ms spin)
        pt.interrupt();
        pt.join(3000);
        if (interruptResult.startsWith("THREW")) {
            bad("park interrupt returns silently", interruptResult + " (JDK: park never throws)");
        } else {
            ok("park interrupt returns silently", "returned-normally".equals(interruptResult));
            // check while alive: the native flag is cleared by thread teardown after join
            ok("interrupt flag survives park", flagAfterPark);
        }
    }

    // ================= 9. unpark robustness + argument guards =================

    static void testUnparkRobust() {
        try { U.unpark(null); ok("unpark(null) no-op", true); }
        catch (Throwable t) { bad("unpark(null) no-op", t.toString()); }
        try { LockSupport.unpark(new Thread(new Runnable(){ public void run(){} })); ok("unpark(not-started) no-op", true); }
        catch (Throwable t) { bad("unpark(not-started) no-op", t.toString()); }
        Thread dead = new Thread(new Runnable(){ public void run(){} });
        dead.start();
        try { dead.join(); Thread.sleep(100); } catch (InterruptedException e) { }
        try { LockSupport.unpark(dead); ok("unpark(finished) no-op", true); }
        catch (Throwable t) { bad("unpark(finished) no-op", t.toString()); }
        // isInterrupted on a not-started thread must not crash
        try {
            boolean b = new Thread(new Runnable(){ public void run(){} }).isInterrupted();
            ok("isInterrupted(not-started)", !b);
        } catch (Throwable t) { bad("isInterrupted(not-started)", t.toString()); }
        // allocateMemory / copyMemory guards
        try { U.allocateMemory(-1); bad("allocateMemory(-1) rejected", "no exception"); }
        catch (IllegalArgumentException e) { ok("allocateMemory(-1) rejected"); }
        try { U.allocateMemory(1L << 40); bad("allocateMemory(huge) rejected", "no exception"); }
        catch (IllegalArgumentException e) { ok("allocateMemory(huge) rejected"); }
        try {
            U.copyMemory(null, 0, null, 0, 1L << 40);
            bad("copyMemory(huge) rejected", "no exception");
        } catch (IllegalArgumentException e) { ok("copyMemory(huge) rejected"); }
    }

    // ================= 10. park wakes on interrupt; Thread.interrupted() clears =================

    static void testInterruptFlag() throws Exception {
        final Thread pt = Thread.currentThread();
        Thread setter = new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(100); } catch (InterruptedException e) { }
                pt.interrupt();
            }
        });
        setter.start();
        long t0 = System.currentTimeMillis();
        LockSupport.park(); // must wake on interrupt, silently
        long woke = System.currentTimeMillis() - t0;
        ok("park wakes on interrupt ~" + woke + "ms", woke >= 50 && woke < 2500);
        boolean first = Thread.interrupted();
        boolean second = Thread.interrupted();
        ok("Thread.interrupted() clears", first && !second);
        setter.join(2000);
    }

    public static void main(String[] args) throws Exception {
        long t0 = System.currentTimeMillis();
        testFieldAccess();
        testDirectMemory();
        testArrays();
        testCas();
        testCasConcurrency();
        testPark();
        testGetAddress();
        testStaticField();
        testParkInterrupt();
        testUnparkRobust();
        testInterruptFlag();
        long t1 = System.currentTimeMillis();
        System.out.println("======================================");
        System.out.println("UnsafeSemanticsTest: pass=" + pass + " fail=" + fail
                + " evidence=" + evid + "  (" + (t1 - t0) + " ms)");
        System.out.println("======================================");
        System.exit(fail == 0 ? 0 : 1);
    }
}
