public class InterpLoopBench {
    // called 4900 times (< 5000 entry threshold): stays interpreted forever;
    // each call runs a 4000-iteration loop -> ~19.6M interpreted backedges,
    // isolating the per-backedge probe cost of pure interpretation
    static int smallLoop(int n, int seed) {
        int s = seed;
        for (int i = 0; i < n; i++) {
            s = (s * 31 + i) & 0x7fffffff;
        }
        return s;
    }

    public static void main(String[] a) {
        int r = 0;
        for (int k = 0; k < 4900; k++) {
            r += smallLoop(4000, k);
        }
        System.out.println("r=" + r);
        long t = System.currentTimeMillis();
        r = 0;
        for (int k = 0; k < 4900; k++) {
            r += smallLoop(4000, k);
        }
        System.out.println("interp-loop: " + (System.currentTimeMillis() - t) + " ms r=" + r);
    }
}
