package test;

import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * Targeted regression tests for the 2026-09-02 JVM semantics fixes:
 *  - NegativeArraySizeException for newarray / anewarray / multianewarray (interpreter + old JIT)
 *  - ArrayStoreException for aastore with covariant arrays (interpreter + old JIT)
 *  - System.arraycopy full JDK semantics (bounds / NPE / type mismatch / partial copy)
 *  - instanceof / aastore array covariance (String[] vs Object[], CharSequence[], int[] vs Object)
 *  - f2i / f2l / d2i / d2l saturating conversions (NaN->0, +-Inf -> MIN/MAX)
 *  - partially-dimensioned arrays: new int[5][] must yield null elements
 *  - java.lang.reflect.Array set/get/getComponentType/newInstance semantics
 *  - GC stress over array classes (component_class marking)
 *
 * Expected values for float/double conversions are hardcoded from the JLS,
 * NOT computed by a cast (which would be circular on miniJVM).
 *
 * Run in BOTH modes:
 *   default                (interpreter + old JIT via hot loops)
 *   -Xdebug                (pure interpreter; no debugger needs to attach)
 */
public class JvmSemanticsFixTest {

    static int passed = 0;
    static int failed = 0;

    static void ok(String name) {
        passed++;
        System.out.println("[PASS] " + name);
    }

    static void bad(String name, String detail) {
        failed++;
        System.out.println("[FAIL] " + name + " : " + detail);
    }

    static void check(String name, boolean cond) {
        if (cond) ok(name); else bad(name, "expected true");
    }

    static void checkEq(String name, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) ok(name);
        else bad(name, "expected=" + expected + " actual=" + actual);
    }

    // ================= helpers used by hot loops (old-JIT compiles after ~2500 calls) =========

    static int newIntArrHot(int n) {
        try {
            return new int[n].length;
        } catch (NegativeArraySizeException e) {
            return -1;
        }
    }

    static int newRefArrHot(int n) {
        try {
            return new String[n].length;
        } catch (NegativeArraySizeException e) {
            return -1;
        }
    }

    static int multiArrHot(int a, int b) {
        try {
            int[][] r = new int[a][b];
            return r.length * 100 + r[0].length;
        } catch (NegativeArraySizeException e) {
            return -1;
        }
    }

    static int aastoreHot(Object[] arr, Object v) {
        try {
            arr[0] = v;
            return 1;
        } catch (ArrayStoreException e) {
            return 0;
        }
    }

    static int f2iHot(float f) {
        return (int) f;
    }

    static long d2lHot(double d) {
        return (long) d;
    }

    // ================= 1. NegativeArraySizeException =================

    static void testNegativeArraySize() {
        try {
            int[] a = new int[-1];
            bad("new int[-1]", "no exception, len=" + a.length);
        } catch (NegativeArraySizeException e) {
            ok("new int[-1]");
        }
        try {
            Object[] a = new Object[-1];
            bad("new Object[-1]", "no exception");
        } catch (NegativeArraySizeException e) {
            ok("new Object[-1]");
        }
        try {
            String[][] a = new String[2][-1];
            bad("new String[2][-1]", "no exception");
        } catch (NegativeArraySizeException e) {
            ok("new String[2][-1]");
        }
        try {
            int[][] a = new int[-1][2];
            bad("new int[-1][2]", "no exception");
        } catch (NegativeArraySizeException e) {
            ok("new int[-1][2]");
        }
        try {
            int[][][] a = new int[1][2][-3];
            bad("new int[1][2][-3]", "no exception");
        } catch (NegativeArraySizeException e) {
            ok("new int[1][2][-3]");
        }
        try {
            int[][] a = new int[1 - 2][3 - 4];
            bad("new int[neg][neg]", "no exception");
        } catch (NegativeArraySizeException e) {
            ok("new int[neg][neg]");
        }
    }

    // ================= 2. AASTORE covariance + ArrayStoreException =================

    static void testAastore() {
        Object[] o = new String[3];
        o[0] = "x";                       // covariant store must be ALLOWED
        ok("covariant store String->Object[]");
        o[2] = "y";
        checkEq("covariant store slot", "x", o[0]);
        try {
            o[1] = Integer.valueOf(1);
            bad("store Integer->String[]", "no exception");
        } catch (ArrayStoreException e) {
            ok("store Integer->String[]");
        }
        check("rejected slot untouched", o[1] == null);

        Number[] nums = new Number[2];
        nums[0] = Integer.valueOf(1);     // subclass store allowed
        nums[1] = Long.valueOf(2L);
        ok("subclass store Integer/Long->Number[]");
        try {
            Object[] n2 = nums;           // heap pollution route: Object[] view of Number[]
            n2[0] = "nope";
            bad("store String->Number[]", "no exception");
        } catch (ArrayStoreException e) {
            ok("store String->Number[]");
        }

        CharSequence[] cs = new String[2];
        cs[0] = "s";                      // String -> CharSequence[] (interface) allowed
        ok("covariant store String->CharSequence[]");
        try {
            Object[] c2 = cs;             // Object[] view of String[]
            c2[1] = Integer.valueOf(3);
            bad("store Integer->String[](iface)", "no exception");
        } catch (ArrayStoreException e) {
            ok("store Integer->String[](iface)");
        }

        Object[] general = new Object[2];
        general[0] = new int[1];          // primitive array IS an Object
        general[1] = new String[1];
        ok("store int[]/String[]->Object[]");

        Object[] nested = new Object[1];
        nested[0] = new String[1][1];     // String[][] as Object
        ok("store String[][]->Object[]");

        String[][] sa2 = new String[1][1];
        sa2[0][0] = "z";
        checkEq("nested aastore", "z", sa2[0][0]);

        Object[][] oo = new String[1][2]; // Object[][] holding String[] rows
        oo[0][0] = "row";                 // String fits the String component
        ok("store String->String[](as Object[] row)");
        try {
            oo[0][1] = new Integer[1];
            bad("store Integer[]->String[] row", "no exception");
        } catch (ArrayStoreException e) {
            ok("store Integer[]->String[] row");
        }
        try {
            Object[] row = oo[0];         // Object[] view, runtime row is String[]
            row[0] = new String[1];       // String[] does not fit String component (JDK throws)
            bad("store String[]->String[] row", "no exception");
        } catch (ArrayStoreException e) {
            ok("store String[]->String[] row");
        }

        String[] nullStore = new String[2];
        nullStore[0] = null;              // null always allowed
        ok("store null");
    }

    // ================= 3. System.arraycopy =================

    static void testArraycopy() {
        int[] a = {1, 2, 3, 4, 5};
        int[] b = new int[5];
        System.arraycopy(a, 0, b, 0, 5);
        check("arraycopy int[] bulk", java.util.Arrays.equals(a, b));

        // self overlap forward
        int[] c = {1, 2, 3, 4, 5, 6, 7, 8};
        System.arraycopy(c, 0, c, 2, 4);   // expect {1,2,1,2,3,4,7,8}
        checkEq("arraycopy self overlap fwd", "[1, 2, 1, 2, 3, 4, 7, 8]", java.util.Arrays.toString(c));
        // self overlap backward
        int[] d = {1, 2, 3, 4, 5, 6, 7, 8};
        System.arraycopy(d, 2, d, 0, 4);   // expect {3,4,5,6,5,6,7,8}
        checkEq("arraycopy self overlap bwd", "[3, 4, 5, 6, 5, 6, 7, 8]", java.util.Arrays.toString(d));

        // refer bulk: String[] -> Object[] allowed
        String[] src = {"a", "b", "c"};
        Object[] dst = new Object[3];
        System.arraycopy(src, 0, dst, 0, 3);
        checkEq("arraycopy String[]->Object[]", "b", dst[1]);

        // same exact type overlap (String[] onto itself)
        String[] ov = {"1", "2", "3", "4"};
        System.arraycopy(ov, 0, ov, 1, 3);
        checkEq("arraycopy ref self overlap", "1", ov[1]);

        // static incompatibility: Integer[] dest, String[] src -> ASE, dest unmodified
        String[] s2 = {"x"};
        Integer[] i2 = new Integer[1];
        try {
            System.arraycopy(s2, 0, i2, 0, 1);
            bad("arraycopy String[]->Integer[]", "no exception");
        } catch (ArrayStoreException e) {
            ok("arraycopy String[]->Integer[]");
        }
        check("ASE dest untouched", i2[0] == null);

        // dynamic incompatibility with PARTIAL copy: Object[] {ok, bad} -> String[2]
        Object[] mixed = {"good", Integer.valueOf(1)};
        String[] sd = new String[2];
        try {
            System.arraycopy(mixed, 0, sd, 0, 2);
            bad("arraycopy Object[]mixed->String[]", "no exception");
        } catch (ArrayStoreException e) {
            ok("arraycopy Object[]mixed->String[]");
        }
        checkEq("partial copy before failure", "good", sd[0]); // K=1: element 0 copied

        // primitive type mismatch
        try {
            System.arraycopy(new int[2], 0, new long[2], 0, 2);
            bad("arraycopy int[]->long[]", "no exception");
        } catch (ArrayStoreException e) {
            ok("arraycopy int[]->long[]");
        }
        try {
            System.arraycopy(new int[2], 0, new Integer[2], 0, 2);
            bad("arraycopy int[]->Integer[]", "no exception");
        } catch (ArrayStoreException e) {
            ok("arraycopy int[]->Integer[]");
        }
        // boolean[] vs byte[] are distinct primitive arrays
        try {
            System.arraycopy(new boolean[2], 0, new byte[2], 0, 2);
            bad("arraycopy boolean[]->byte[]", "no exception");
        } catch (ArrayStoreException e) {
            ok("arraycopy boolean[]->byte[]");
        }
        try {
            System.arraycopy(new int[1], -1, new long[1], 0, 0);
            bad("arraycopy type check before bounds", "no exception");
        } catch (ArrayStoreException e) {
            ok("arraycopy type check before bounds");
        } catch (IndexOutOfBoundsException e) {
            bad("arraycopy type check before bounds", "wrong exception " + e);
        }

        // bounds
        try {
            System.arraycopy(a, 3, b, 0, 3);
            bad("arraycopy src overflow", "no exception");
        } catch (IndexOutOfBoundsException e) {
            ok("arraycopy src overflow");
        }
        try {
            System.arraycopy(a, 0, b, 4, 2);
            bad("arraycopy dest overflow", "no exception");
        } catch (IndexOutOfBoundsException e) {
            ok("arraycopy dest overflow");
        }
        try {
            System.arraycopy(a, -1, b, 0, 1);
            bad("arraycopy neg srcPos", "no exception");
        } catch (IndexOutOfBoundsException e) {
            ok("arraycopy neg srcPos");
        }
        try {
            System.arraycopy(a, 0, b, 0, -1);
            bad("arraycopy neg length", "no exception");
        } catch (IndexOutOfBoundsException e) {
            ok("arraycopy neg length");
        }
        try {
            System.arraycopy(null, 0, b, 0, 1);
            bad("arraycopy null src", "no exception");
        } catch (NullPointerException e) {
            ok("arraycopy null src");
        }
        try {
            System.arraycopy(a, 0, null, 0, 1);
            bad("arraycopy null dest", "no exception");
        } catch (NullPointerException e) {
            ok("arraycopy null dest");
        }
        try {
            System.arraycopy(new Object(), 0, b, 0, 1);
            bad("arraycopy non-array src", "no exception");
        } catch (ArrayStoreException e) {
            ok("arraycopy non-array src");
        }
        // zero length at boundary is legal
        System.arraycopy(a, 5, b, 5, 0);
        ok("arraycopy zero-length at end");
        // 2D array copy: int[][] rows are references
        int[][] m1 = {{1, 2}, {3, 4}};
        int[][] m2 = new int[2][2];
        System.arraycopy(m1, 0, m2, 0, 2);
        check("arraycopy int[][] rows", m2[1][0] == 3);
    }

    // ================= 4. instanceof array covariance =================

    static void testInstanceof() {
        String[] sa = new String[1];
        check("String[] instanceof Object[]", sa instanceof Object[]);
        check("String[] instanceof Object", sa instanceof Object);
        check("String[] instanceof Cloneable", sa instanceof Cloneable);
        check("String[] instanceof Serializable", sa instanceof Serializable);
        check("String[] instanceof CharSequence[]", sa instanceof CharSequence[]);
        Object saO = sa;
        check("!(String[] instanceof Integer[])", !(saO instanceof Integer[]));
        check("!(String[] instanceof Number[])", !(saO instanceof Number[]));

        int[] ia = new int[1];
        Object iaO = ia;
        check("int[] instanceof Object", ia instanceof Object);
        check("int[] instanceof Cloneable", ia instanceof Cloneable);
        check("int[] instanceof Serializable", ia instanceof Serializable);
        check("!(int[] instanceof Object[])", !(iaO instanceof Object[]));

        int[][] iaa = new int[2][];
        Object iaaO = iaa;
        check("int[][] instanceof Object[]", iaa instanceof Object[]);
        check("int[][] instanceof Object", iaa instanceof Object);
        check("!(int[][] instanceof int[])", !(iaaO instanceof int[]));

        CharSequence[] csa = new CharSequence[2];
        check("CharSequence[] instanceof Object[]", csa instanceof Object[]);

        Object held = sa;
        check("held String[] instanceof String[]", held instanceof String[]);
        check("held String[] instanceof Object[]", held instanceof Object[]);

        // subclass arrays
        Integer[] na = new Integer[1];
        Object naO = na;
        check("Integer[] instanceof Number[]", na instanceof Number[]);
        check("!(Integer[] instanceof String[])", !(naO instanceof String[]));
    }

    // ================= 5. checkcast behavior (documents known gaps) =================

    static void testCheckcast() {
        Object held = new String[1];
        try {
            String[] back = (String[]) held;
            ok("checkcast String[]<-String[]");
        } catch (ClassCastException e) {
            bad("checkcast String[]<-String[]", e.toString());
        }
        try {
            Object[] up = (Object[]) held;
            ok("checkcast Object[]<-String[] (covariant)");
        } catch (ClassCastException e) {
            bad("checkcast Object[]<-String[] (covariant)", e.toString());
        }
        try {
            Integer[] bad = (Integer[]) held;
            bad("checkcast Integer[]<-String[] must CCE", "cast SUCCEEDED (heap pollution risk)");
        } catch (ClassCastException e) {
            ok("checkcast Integer[]<-String[] must CCE");
        }
        Object pint = new int[1];
        try {
            Cloneable c = (Cloneable) pint;
            ok("checkcast Cloneable<-int[]");
        } catch (ClassCastException e) {
            bad("checkcast Cloneable<-int[]", "threw (JDK allows)");
        }
        try {
            Serializable s = (Serializable) pint;
            ok("checkcast Serializable<-int[]");
        } catch (ClassCastException e) {
            bad("checkcast Serializable<-int[]", "threw (JDK allows)");
        }
        try {
            int[] back = (int[]) pint;
            ok("checkcast int[]<-int[]");
        } catch (ClassCastException e) {
            bad("checkcast int[]<-int[]", e.toString());
        }
        try {
            long[] wrong = (long[]) pint;
            bad("checkcast long[]<-int[] must CCE", "cast SUCCEEDED");
        } catch (ClassCastException e) {
            ok("checkcast long[]<-int[] must CCE");
        }
    }

    // ================= 6. float/double conversions (JLS saturated) =================

    static void testConversions() {
        // (int) float — expected values per JLS 5.1.3
        checkEq("f2i NaN", 0, (int) Float.NaN);
        checkEq("f2i +Inf", Integer.MAX_VALUE, (int) Float.POSITIVE_INFINITY);
        checkEq("f2i -Inf", Integer.MIN_VALUE, (int) Float.NEGATIVE_INFINITY);
        checkEq("f2i 1.5", 1, (int) 1.5f);
        checkEq("f2i -1.5", -1, (int) -1.5f);
        checkEq("f2i 2^31f", Integer.MAX_VALUE, (int) 2.14748365E9f);
        checkEq("f2i just under 2^31", 2147483520, (int) 2.14748352E9f);
        checkEq("f2i -2^31f", Integer.MIN_VALUE, (int) -2.14748365E9f);
        checkEq("f2i huge", Integer.MAX_VALUE, (int) 3.4e38f);
        checkEq("f2i -huge", Integer.MIN_VALUE, (int) -3.4e38f);
        checkEq("f2i 0.9", 0, (int) 0.9f);
        checkEq("f2i -0.9", 0, (int) -0.9f);

        // (int) double
        checkEq("d2i NaN", 0, (int) Double.NaN);
        checkEq("d2i +Inf", Integer.MAX_VALUE, (int) Double.POSITIVE_INFINITY);
        checkEq("d2i -Inf", Integer.MIN_VALUE, (int) Double.NEGATIVE_INFINITY);
        checkEq("d2i MAX-0.5", 2147483647, (int) 2147483647.5d);
        checkEq("d2i 2^31", Integer.MAX_VALUE, (int) 2147483648.0d);
        checkEq("d2i -2^31-1", Integer.MIN_VALUE, (int) -2147483649.0d);
        checkEq("d2i 1e300", Integer.MAX_VALUE, (int) 1e300);

        // (long) float
        checkEq("f2l NaN", 0L, (long) Float.NaN);
        checkEq("f2l +Inf", Long.MAX_VALUE, (long) Float.POSITIVE_INFINITY);
        checkEq("f2l -Inf", Long.MIN_VALUE, (long) Float.NEGATIVE_INFINITY);
        checkEq("f2l 2^62f", 4611686018427387904L, (long) 4.611686E18f);
        checkEq("f2l 2^63f", Long.MAX_VALUE, (long) 9.223372E18f);
        checkEq("f2l 1e18f", 999999984306749440L, (long) 1.0e18f);
        checkEq("f2l 123.9", 123L, (long) 123.9f);

        // (long) double
        checkEq("d2l NaN", 0L, (long) Double.NaN);
        checkEq("d2l +Inf", Long.MAX_VALUE, (long) Double.POSITIVE_INFINITY);
        checkEq("d2l -Inf", Long.MIN_VALUE, (long) Double.NEGATIVE_INFINITY);
        checkEq("d2l 2^62", 4611686018427387904L, (long) 4.611686018427387904E18);
        checkEq("d2l 2^63", Long.MAX_VALUE, (long) 9.223372036854775808E18);
        checkEq("d2l 1e300", Long.MAX_VALUE, (long) 1e300);
        checkEq("d2l -1e300", Long.MIN_VALUE, (long) -1e300);
    }

    // ================= 7. partially dimensioned arrays =================

    static void testPartialDims() {
        int[][] a = new int[5][];
        checkEq("new int[5][].length", 5, a.length);
        check("new int[5][] elems null", a[0] == null && a[4] == null);
        a[0] = new int[]{7};
        checkEq("assign subarray", 7, a[0][0]);

        String[][] s = new String[3][];
        check("new String[3][] elems null", s[1] == null);
        s[2] = new String[]{"q"};
        checkEq("assign ref subarray", "q", s[2][0]);

        int[][] full = new int[2][3];
        checkEq("new int[2][3] outer", 2, full.length);
        checkEq("new int[2][3] inner", 3, full[0].length);
        checkEq("new int[2][3] zeroed", 0, full[1][2]);

        int[][][] tri = new int[2][][  ];
        checkEq("new int[2][][]", 2, tri.length);
        check("new int[2][][] elems null", tri[0] == null);
        int[][][] tri2 = new int[2][3][];
        checkEq("new int[2][3][] outer", 2, tri2.length);
        checkEq("new int[2][3][] mid", 3, tri2[1].length);
        check("new int[2][3][] inner null", tri2[0][0] == null);
        // this class's own array type
        JvmSemanticsFixTest[][] me = new JvmSemanticsFixTest[2][];
        check("new Self[2][] null", me[0] == null);
        me[1] = new JvmSemanticsFixTest[1];
        check("new Self[1] store", me[1][0] == null);
    }

    // ================= 8. java.lang.reflect.Array =================

    static void testReflectArray() {
        Object a = Array.newInstance(String.class, 2);
        check("newInstance(String,2) type", a instanceof String[]);
        checkEq("newInstance(String,2) len", 2, ((String[]) a).length);
        Array.set(a, 1, "v");
        checkEq("Array.set/get", "v", Array.get(a, 1));

        Object m = Array.newInstance(String.class, 2, 3);
        String[][] mm = (String[][]) m;
        checkEq("newInstance dims order outer", 2, mm.length);   // transposed-dims bug gate
        checkEq("newInstance dims order inner", 3, mm[0].length);
        Array.set(mm[0], 2, "deep");
        checkEq("nested reflect set", "deep", mm[0][2]);

        Object p = Array.newInstance(int.class, 2, 4);
        int[][] pp = (int[][]) p;
        checkEq("newInstance(int,2,4) outer", 2, pp.length);
        checkEq("newInstance(int,2,4) inner", 4, pp[0].length);
        Array.set(pp[1], 0, 42);
        checkEq("primitive reflect set", 42, Array.get(pp[1], 0));

        try {
            Array.newInstance(String.class, -1);
            bad("newInstance neg", "no exception");
        } catch (NegativeArraySizeException e) {
            ok("newInstance neg");
        }
        try {
            Array.newInstance(String.class);
            bad("newInstance zero dims", "no exception");
        } catch (IllegalArgumentException e) {
            ok("newInstance zero dims");
        } catch (NegativeArraySizeException e) {
            bad("newInstance zero dims", "wrong exception " + e);
        }
        try {
            Array.newInstance(null, 1);
            bad("newInstance null type", "no exception");
        } catch (NullPointerException e) {
            ok("newInstance null type");
        }
        try {
            Array.newInstance(Void.TYPE, 0);
            bad("newInstance void type", "no exception");
        } catch (IllegalArgumentException e) {
            ok("newInstance void type");
        }
        try {
            Array.newInstance(Void.TYPE, 1, 1);
            bad("multiNewInstance void type", "no exception");
        } catch (IllegalArgumentException e) {
            ok("multiNewInstance void type");
        }
        try {
            Array.newInstance(String[].class, new int[255]);
            bad("newInstance total dimensions > 255", "no exception");
        } catch (IllegalArgumentException e) {
            ok("newInstance total dimensions > 255");
        }

        String[] sa = {"a"};
        try {
            Array.set(sa, 0, Integer.valueOf(1));
            bad("Array.set type check", "no exception");
        } catch (IllegalArgumentException e) {
            ok("Array.set type check");
        }
        Array.set(sa, 0, null);
        check("Array.set null ok", Array.get(sa, 0) == null);
        try {
            Array.get(sa, 1);
            bad("Array.get at length", "no exception");
        } catch (IndexOutOfBoundsException e) {
            ok("Array.get at length");
        }
        try {
            Array.set(sa, -1, "x");
            bad("Array.set neg idx", "no exception");
        } catch (IndexOutOfBoundsException e) {
            ok("Array.set neg idx");
        }

        // getComponentType
        check("getComponentType String[]", sa.getClass().getComponentType() == String.class);
        check("getComponentType int[]", int[].class.getComponentType() == int.class);
        check("getComponentType int[][]", int[][].class.getComponentType() == int[].class);
        check("getComponentType String[][]", String[][].class.getComponentType() == String[].class);
        check("getComponentType non-array", String.class.getComponentType() == null);
        check("getComponentType reflect-made", mm.getClass().getComponentType() == String[].class);

        // getLength
        checkEq("Array.getLength", 3, Array.getLength(new int[3]));
    }

    // ================= 9. GC stress over array classes =================

    static void testGcArrays() {
        long sink = 0;
        for (int i = 0; i < 30000; i++) {
            Object[] o = new String[3];
            o[0] = "x" + (i & 15);
            int[][] x = new int[4][4];
            x[0][0] = i;
            CharSequence[][] cs = new String[2][2];
            cs[0][0] = "y";
            sink += x[0][0] + ((String) o[0]).length();
            if ((i & 2047) == 0) System.gc();
        }
        check("gc array stress", sink > 0);
        // component classes still usable after GCs
        check("post-gc covariance", new String[1] instanceof Object[]);
        Object[] again = new String[2];
        try {
            again[0] = Integer.valueOf(9);
            bad("post-gc ASE", "no exception");
        } catch (ArrayStoreException e) {
            ok("post-gc ASE");
        }
        check("post-gc getComponentType", again.getClass().getComponentType() == String.class);
    }

    // ================= 10. hot loops -> force old JIT compile of fixed paths =================

    static void testHotLoops() {
        int r1 = 0, r2 = 0, r3 = 0, r4 = 0, r5 = 0;
        long r6 = 0;
        for (int i = 0; i < 50000; i++) {
            r1 = newIntArrHot(-3);
            r2 = newRefArrHot(-3);
            r3 = multiArrHot(2, -5);
            r4 = aastoreHot(new String[1], "ok") + aastoreHot(new String[1], Integer.valueOf(1));
            r5 = f2iHot(2.14748365E9f) + f2iHot(Float.NaN) + f2iHot(3.4e38f);
            r6 = d2lHot(1e300) + d2lHot(Double.NaN) + d2lHot(-1e300);
        }
        checkEq("jit newarray neg", -1, r1);
        checkEq("jit anewarray neg", -1, r2);
        checkEq("jit multiarray neg", -1, r3);
        checkEq("jit aastore ok+ASE", 1 + 0, r4);
        checkEq("jit f2i saturate", Integer.MAX_VALUE + 0 + Integer.MAX_VALUE, r5);
        checkEq("jit d2l saturate", (Long.MAX_VALUE + 0 + Long.MIN_VALUE), r6);
        // positive shapes still work in compiled code
        int good = 0;
        for (int i = 0; i < 50000; i++) {
            good = multiArrHot(3, 4) + newIntArrHot(6);
        }
        checkEq("jit multiarray good", 3 * 100 + 4 + 6, good);
    }

    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();
        testNegativeArraySize();
        testAastore();
        testArraycopy();
        testInstanceof();
        testCheckcast();
        testConversions();
        testPartialDims();
        testReflectArray();
        testGcArrays();
        testHotLoops();
        long t1 = System.currentTimeMillis();
        System.out.println("======================================");
        System.out.println("JvmSemanticsFixTest: passed=" + passed + " failed=" + failed
                + "  (" + (t1 - t0) + " ms)");
        System.out.println("======================================");
        System.exit(failed == 0 ? 0 : 1);
    }
}
