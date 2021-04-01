public class Test {


    static char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    static public String append(long i) {
        if(i==0)return "0";
        boolean ne = i < 0;
        if (ne) i = -i;
        String s = "";
        while (i != 0) {
            int v = (int) (i % 10);
            s = chars[v] + s;
            i /= 10;
        }
        if (ne) {
            s = '-' + s;
        }
        return s;
    }

    static public void main(String[] args) {
        System.out.println(append(0L));
    }
}
