package test;

public class HotArr {
    static int newIntArrHot(int n) {
        try {
            return new int[n].length;
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
    public static void main(String[] args) {
        // warm up both
        for (int i = 0; i < 60000; i++) {
            newIntArrHot(i & 7);
            multiArrHot(2, -5);
        }
        System.out.println("warm ok");
        // now the failing pattern: positive after hot
        int good = 0;
        for (int i = 0; i < 50000; i++) {
            good = multiArrHot(3, 4) + newIntArrHot(6);
        }
        System.out.println("good=" + good);
        if (good != 307 + 6) System.out.println("[FAIL] want=" + (307 + 6));
        else System.out.println("[PASS]");
    }
}
