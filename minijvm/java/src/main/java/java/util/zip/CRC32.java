/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates, and edit the template in the editor.
 */
package java.util.zip;

/**
 * Table-driven CRC-32 (IEEE 802.3, reflected), API-compatible with the JDK subset
 * needed by zip writing.
 */
public class CRC32 implements Checksum {

    private static final int[] TABLE = new int[256];

    static {
        for (int n = 0; n < 256; n++) {
            int c = n;
            for (int k = 0; k < 8; k++) c = ((c & 1) != 0) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
            TABLE[n] = c;
        }
    }

    private int crc = 0xFFFFFFFF;

    @Override
    public void update(int b) {
        crc = TABLE[(crc ^ b) & 0xFF] ^ (crc >>> 8);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        if (b == null) throw new NullPointerException();
        if (off < 0 || len < 0 || off > b.length - len) throw new ArrayIndexOutOfBoundsException();
        int c = crc;
        for (int i = off; i < off + len; i++) c = TABLE[(c ^ b[i]) & 0xFF] ^ (c >>> 8);
        crc = c;
    }

    public void update(byte[] b) { update(b, 0, b.length); }

    @Override
    public long getValue() { return (long) (crc ^ 0xFFFFFFFF) & 0xFFFFFFFFL; }

    @Override
    public void reset() { crc = 0xFFFFFFFF; }
}
