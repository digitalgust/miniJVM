package test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NioBufferTest {


    static void printerr(boolean asset, String s) {
        if (!asset) {
            return;
        }
        System.out.println("test error :" + s);
        try {
            throw new Exception();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    void t1() {
        try {
            ByteBuffer b0 = ByteBuffer.allocate(110);
            b0.position(10);
            ByteBuffer bb = b0.slice();
            bb.position(10);
            bb.put(0, (byte) 1);
            printerr(bb.get(0) != 1, "put(i,v)");
            printerr(bb.position() != 10, "put position");
            printerr(bb.limit() != 100, "put limit");
            bb.put(99, (byte) 1);
            printerr(bb.get(99) != 1, "put");
            bb.put((byte) 1);
            printerr(bb.get(10) != 1 || bb.position() != 11 || bb.limit() != 100, "put(v)");
            bb.putFloat(1.f);
            printerr(bb.getFloat(11) != 1.f || bb.position() != 15 || bb.limit() != 100, "putFloat(v)");
            bb.putLong(8);
            printerr(bb.getLong(15) != 8 || bb.position() != 23 || bb.limit() != 100, "putLong(v)");
            bb.putInt(5);
            printerr(bb.getInt(23) != 5 || bb.position() != 27 || bb.limit() != 100, "putInt(v)");
            bb.putDouble(2.d);
            printerr(bb.getDouble(27) != 2.d || bb.position() != 35 || bb.limit() != 100, "putDouble(v)");
            bb.putChar('a');
            printerr(bb.getChar(35) != 'a' || bb.position() != 37 || bb.limit() != 100, "putChar(v)");
            bb.putShort((short) -25);
            printerr(bb.getShort(37) != -25 || bb.position() != 39 || bb.limit() != 100, "putShort(v)");

            int debug = 1;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void show(List<Map<Integer, List<String>>> list, List<String> a) {
        for (Method m : this.getClass().getMethods()) {
            System.out.println(m.toString());
            System.out.println(m.toGenericString());
            for (Type t : m.getGenericParameterTypes())
                System.out.println(m.getName() + ":" + t.getTypeName());
        }
        System.out.println(list.getClass().getName());
    }

    public static void main(String[] args) {
        try {
            NioBufferTest tf = new NioBufferTest();
            tf.t1();
            tf.show(new ArrayList<>(), null);
            System.out.println(args.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
