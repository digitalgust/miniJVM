package test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class MultiThread {

    Object lock = new Object();
    int v = 0;

    class Ta extends Thread {

        public void run() {
            for (int i = 0; i < 6; i++) {
                synchronized (lock) {
                    v = i;
                    lock.notify();
                    try {
                        lock.wait(1000);
                    } catch (InterruptedException ex) {
                    }
                }
            }

        }
    }

    class Tb extends Thread {

        public void run() {
            while (v < 5) {
                synchronized (lock) {
                    System.out.println("v=" + v);
                    lock.notify();
                    try {
                        lock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
    }

    void test() {
        Ta a = new Ta();
        Tb b = new Tb();
        a.start();
        b.start();
    }


    class TBody extends Thread {
        ConcurrentHashMap map = new ConcurrentHashMap();
        boolean exit = false;

        public void run() {
            int i = 0;
            while (!exit) {
                map.put(i % 10000, "OK");
                i++;
            }
        }
    }

    ;

    void test2() {
        System.out.println("test 2 start");
        TBody t1 = new TBody();
        t1.start();
        Map<Integer, String> map = t1.map;
        for (int i = 0; i >= 0; i++) {
            for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
                it.next();
                map.put(Integer.valueOf(0), "OK");//no exception thrown
            }
            if (map.size() > 1000) {
                t1.exit = true;
                System.out.println("iteratored count " + i);
                break;
            }
        }

    }

    public static void main(String args[]) {
        MultiThread mt = new MultiThread();
        mt.test();
        mt.test2();

        hugeThreadTest();
    }

    static class HugeThread extends Thread {
        public void run() {
            List<Float> list = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                list.add((float) Math.random());
            }
            threadList.remove(this);
        }
    }

    static Vector threadList = new Vector();

    private static void hugeThreadTest() {
        int i = 0;
        while (i < 50000) {
            synchronized (threadList) {
                while (threadList.size() < 15) {
                    HugeThread ht = new HugeThread();
                    ht.start();
                    threadList.add(ht);
                    i++;
                    if (i % 100 == 0) {
                        System.out.println("thread created:" + i);
                    }
                }
            }
        }

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
