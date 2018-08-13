package test;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

class Hello {

    static Timer timer = new Timer(true);

    static TimerTask task = new TimerTask() {

        @Override
        public void run() {
            try {
                System.out.println(".");
            } catch (Exception e) {
            }
        }
    };

    public static void main(String[] args) {

        timer.schedule(task, 0, 1000);
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
    }

}
