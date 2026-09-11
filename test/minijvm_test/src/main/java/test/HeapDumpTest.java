package test;

import org.mini.vm.RefNative;
import java.io.FileWriter;
import java.io.IOException;

public class HeapDumpTest {
    static class HprofBaseValue {
    }

    static class HprofDerivedValue {
    }

    static class HprofBase {
        HprofBaseValue baseRef = new HprofBaseValue();
        int baseInt = 0x12345678;
    }

    static class HprofDerived extends HprofBase {
        HprofDerivedValue derivedRef = new HprofDerivedValue();
        long derivedLong = 0x1122334455667788L;
    }

    /* Kept alive through the dump.  HPROF INSTANCE_DUMP must encode the
     * derived fields before the inherited fields, independent of miniJVM's
     * physical field packing order. */
    static HprofDerived fieldOrderSentinel = new HprofDerived();

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
        HprofDerived sentinel = fieldOrderSentinel;
        System.out.println("HPROF field-order sentinel=" + sentinel.derivedLong + "/" + sentinel.baseInt);
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
