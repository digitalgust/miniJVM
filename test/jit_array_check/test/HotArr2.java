package test;

public class HotArr2 {
    static int multiArrHot(int a, int b) {
        try {
            int[][] r = new int[a][b];
            return r.length * 100 + r[0].length;
        } catch (NegativeArraySizeException e) {
            return -1;
        }
    }
    static int newIntArrHot(int n) {
        try {
            return new int[n].length;
        } catch (NegativeArraySizeException e) {
            return -1;
        }
    }
    public static void main(String[] args) throws Exception {
        int which = Integer.parseInt(args[0]);
        if (which == 0) {
            for (int i = 0; i < 60000; i++) multiArrHot(2, -5);
            System.out.println("multi warm ok");
            System.out.println("multi(3,4)=" + multiArrHot(3, 4));
        } else if (which == 1) {
            for (int i = 0; i < 60000; i++) newIntArrHot(i & 7);
            System.out.println("newInt warm ok");
            System.out.println("newInt(6)=" + newIntArrHot(6));
        } else {
            for (int i = 0; i < 60000; i++) { multiArrHot(2, -5); newIntArrHot(i & 7); }
            System.out.println("both warm ok");
            System.out.println("multi(3,4)=" + multiArrHot(3, 4));
            System.out.println("newInt(6)=" + newIntArrHot(6));
        }
    }
}
