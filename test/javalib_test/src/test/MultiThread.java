package test;

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

    public static void main(String args[]) {
        MultiThread mt = new MultiThread();
        mt.test();
    }
}
