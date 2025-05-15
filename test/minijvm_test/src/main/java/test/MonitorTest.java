package test;

public class MonitorTest {
    Object lock = new Object();
    static MonitorTest mt;
    MonitorTest self;

    public static void main(String[] args) {
        mt = new MonitorTest();
        mt.self = mt;
        mt.t1();
    }

    public Object getLock() {
        return lock;
    }

    static void t3() {
        System.out.println("t3");
    }

    private void t2() {
        throw new RuntimeException("t2");
    }

    public void t1() {
        MonitorTest.t3();
        synchronized (self.getLock()) {
            t2();
        }
    }
}
