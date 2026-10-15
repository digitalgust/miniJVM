package org.mini.reflect;

import java.io.*;
import java.util.*;

/** Class membership attributes, also retained for classes defined from in-memory byte arrays. */
public final class ClassMetadata {
    public String declaring;
    public int modifiers = -1;
    public boolean nested;
    public String simpleName;
    public String enclosing;
    public String enclosingMethodName;
    public String enclosingMethodDescriptor;
    public final List<String> members = new ArrayList<String>();

    public static ClassMetadata read(Class<?> type) {
        if (type.isPrimitive() || type.isArray()) return new ClassMetadata();
        InputStream in = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class");
        if (in == null) return new ClassMetadata();
        try { return read(in); }
        finally { try { in.close(); } catch (IOException ignored) {} }
    }

    public static ClassMetadata read(InputStream stream) {
        ClassMetadata result = new ClassMetadata();
        try {
            DataInputStream in = new DataInputStream(stream);
            if (in.readInt() != 0xcafebabe) throw new ClassFormatError();
            in.readInt();
            Object[] pool = new Object[in.readUnsignedShort()];
            for (int i = 1; i < pool.length; i++) {
                switch (in.readUnsignedByte()) {
                    case 1: pool[i] = in.readUTF(); break;
                    case 7: case 8: case 16: case 19: case 20: pool[i] = Integer.valueOf(in.readUnsignedShort()); break;
                    case 12: pool[i] = new int[] { in.readUnsignedShort(), in.readUnsignedShort() }; break;
                    case 3: case 4: case 9: case 10: case 11: case 17: case 18: in.readInt(); break;
                    case 5: case 6: in.readLong(); i++; break;
                    case 15: in.readUnsignedByte(); in.readUnsignedShort(); break;
                    default: throw new ClassFormatError();
                }
            }
            in.readUnsignedShort();
            String self = name(pool, in.readUnsignedShort());
            in.readUnsignedShort();
            int count = in.readUnsignedShort();
            for (int i = 0; i < count; i++) in.readUnsignedShort();
            for (int table = 0; table < 2; table++) {
                count = in.readUnsignedShort();
                for (int i = 0; i < count; i++) {
                    in.readUnsignedShort(); in.readUnsignedShort(); in.readUnsignedShort();
                    int attrs = in.readUnsignedShort();
                    for (int j = 0; j < attrs; j++) { in.readUnsignedShort(); skip(in, in.readInt()); }
                }
            }
            count = in.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                String attr = (String) pool[in.readUnsignedShort()];
                int length = in.readInt();
                if ("InnerClasses".equals(attr)) {
                    int rows = in.readUnsignedShort();
                    for (int j = 0; j < rows; j++) {
                        int inner = in.readUnsignedShort(), outer = in.readUnsignedShort(), simple = in.readUnsignedShort();
                        int flags = in.readUnsignedShort();
                        if (inner != 0 && self.equals(name(pool, inner))) {
                            result.modifiers = flags;
                            result.nested = true;
                            result.simpleName = simple == 0 ? null : (String) pool[simple];
                        }
                        if (inner != 0 && outer != 0 && simple != 0) {
                            String innerName = name(pool, inner), outerName = name(pool, outer);
                            if (self.equals(innerName)) result.declaring = outerName;
                            if (self.equals(outerName)) result.members.add(innerName);
                        }
                    }
                } else if ("EnclosingMethod".equals(attr)) {
                    result.enclosing = name(pool, in.readUnsignedShort());
                    int method = in.readUnsignedShort();
                    if (method != 0) {
                        int[] pair = (int[]) pool[method];
                        result.enclosingMethodName = (String) pool[pair[0]];
                        result.enclosingMethodDescriptor = (String) pool[pair[1]];
                    }
                } else skip(in, length);
            }
            return result;
        } catch (IOException e) { throw new ClassFormatError(e.toString()); }
    }

    private static String name(Object[] pool, int index) { return ((String) pool[((Integer) pool[index]).intValue()]).replace('/', '.'); }
    private static void skip(DataInputStream in, int length) throws IOException {
        if (length < 0) throw new ClassFormatError();
        while (length > 0) { int skipped = in.skipBytes(length); if (skipped == 0) { in.readByte(); skipped = 1; } length -= skipped; }
    }
}
