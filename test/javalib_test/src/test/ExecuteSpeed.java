/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

/**
 *
 * @author Gust
 */
public class ExecuteSpeed {

    public static final void main(String[] args) throws Exception {
        String str = "abcdefgh" + "efghefgh";
        int imax = 1024 / str.length() * 1024 * 4;

        long time = System.currentTimeMillis();
        System.out.println("exec.tm.sec\tstr.length\tallocated memory:free memory:memory used");
        Runtime runtime = Runtime.getRuntime();
        System.out.println("0\t\t0\t\t" + runtime.totalMemory() / 1024 + ":" + runtime.freeMemory() / 1024 + ":" + (runtime.totalMemory() - runtime.freeMemory()) / 1024);

        String gstr = "";
        int i = 0;
        int lngth;

        while (i++ < imax + 1000) {
//            System.out.println("gstr=" + gstr + "  str=" + str);
            gstr += str;
//            System.out.println("gstr=" + gstr + "  str=" + str);
            gstr = gstr.replace("efgh", "____");
//            System.out.println("gstr=" + gstr + "  str=" + str);
            lngth = str.length() * i;
            if ((lngth % (1024 * 2)) == 0) {
                System.out.println(((System.currentTimeMillis() - time) / 1000) + "sec\t\t" + lngth / 1024 + "kb\t\t" + runtime.totalMemory() / 1024 + ":" + runtime.freeMemory() / 1024 + ":" + (runtime.totalMemory() - runtime.freeMemory()) / 1024);
            }
        }
    }
}
