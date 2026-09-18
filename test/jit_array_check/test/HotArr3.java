package test;

public class HotArr3 {
    // split: warm a no-catch version to isolate
    static int plainMulti(int a, int b) {
        int[][] r = new int[a][b];
        return r.length * 100 + r[0].length;
    }
    public static void main(String[] args) {
        for (int i = 0; i < 60000; i++) {
            plainMulti(3, 4);
        }
        System.out.println("plain=" + plainMulti(3, 4));
    }
}
