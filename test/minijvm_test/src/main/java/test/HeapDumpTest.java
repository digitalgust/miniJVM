package test;

import org.mini.vm.RefNative;
import java.io.FileWriter;
import java.io.IOException;

public class HeapDumpTest {
    static class Node {
        Object next;
        byte[] data;

        Node(Object next) {
            this.next = next;
            this.data = new byte[1024];
        }
    }

    public static void main(String[] args) {
        System.out.println("HeapDumpTest start");
        Object head = null;
        for (int i = 0; i < 200; i++) {
            head = new Node(head);
        }
        try {
            FileWriter fw0 = new FileWriter("dump_start.txt");
            fw0.write("start");
            fw0.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            int rc = RefNative.dumpHeap("minijvm_test_dump.hprof", 0);
            FileWriter fw = new FileWriter("dump_rc.txt");
            fw.write("dumpHeap rc=" + rc);
            fw.close();
        } catch (Throwable t) {
            try {
                FileWriter fe = new FileWriter("dump_err.txt");
                fe.write(String.valueOf(t));
                fe.close();
            } catch (IOException ignored) {
            }
        }
    }
}
