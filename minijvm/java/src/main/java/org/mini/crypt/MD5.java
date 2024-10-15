package org.mini.crypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.Arrays;

public class MD5 extends MessageDigest {
    int[] init;

    byte[] buf;
    int bufOffset;
    long messageBits;

    byte[] oneByte = new byte[]{0};

    public MD5() {
        super("MD5");
        reset();
    }

    @Override
    public void reset() {
        state = INITIAL;

        init = new int[]{0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476};
        buf = new byte[64];
        bufOffset = 0;
        messageBits = 0;
    }

    @Override
    public void update(byte[] src, int off, int len) {
        if (src == null) {
            return;
        }
        if (src.length == 0) {
            return;
        }
        if (off < 0 || off >= src.length || len < 0 || len > src.length) {
            throw new IllegalArgumentException("src offset or length error");
        }
        if (off == 0 && len == buf.length) {
            update(src);
        } else {
            byte[] b = new byte[len];
            System.arraycopy(src, off, b, 0, len);
            update(b);
        }
    }

    @Override
    public void update(byte input) {
        oneByte[0] = input;
        update(oneByte);
    }

    @Override
    public void update(byte[] src) {
        if (src == null) {
            return;
        }
        state = IN_PROGRESS;

        int srcOffset = 0;
        int srcRemaining = 0;
        int copyLength = 0;
        try {
            while (true) {
                copyLength = buf.length - bufOffset;
                srcRemaining = src.length - srcOffset;
                if (srcRemaining < copyLength) {
                    copyLength = srcRemaining;
                    System.arraycopy(src, srcOffset, buf, bufOffset, copyLength);
                    bufOffset += copyLength;
                    messageBits += copyLength * 8;
                    return;
                } else {
                    System.arraycopy(src, srcOffset, buf, bufOffset, copyLength);
                    init = md5_2(buf, init);
                    bufOffset = 0;
                    srcOffset += copyLength;
                    messageBits += copyLength * 8;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected int getDigestLengthImpl() {
        return init.length * Integer.BYTES;
    }

    @Override
    public byte[] digest() {
        for (int i = bufOffset; i < buf.length; i++) {
            buf[i] = 0;
        }
        if (bufOffset < 56) {
            buf[bufOffset] = -128;
        } else {
            buf[bufOffset] = -128;
            init = md5_2(buf, init);
            bufOffset = 0;
            Arrays.fill(buf, (byte) 0);
        }
        for (int i = 0; i < 8; i++) {
            buf[56 + i] = new Long(messageBits >>> i * 8).byteValue();
        }

        init = md5_2(buf, init);

        ByteBuffer bb = ByteBuffer.allocate(getDigestLengthImpl());
        bb.put((byte) (init[0] >>> 0));
        bb.put((byte) (init[0] >>> 8));
        bb.put((byte) (init[0] >>> 16));
        bb.put((byte) (init[0] >>> 24));
        bb.put((byte) (init[1] >>> 0));
        bb.put((byte) (init[1] >>> 8));
        bb.put((byte) (init[1] >>> 16));
        bb.put((byte) (init[1] >>> 24));
        bb.put((byte) (init[2] >>> 0));
        bb.put((byte) (init[2] >>> 8));
        bb.put((byte) (init[2] >>> 16));
        bb.put((byte) (init[2] >>> 24));
        bb.put((byte) (init[3] >>> 0));
        bb.put((byte) (init[3] >>> 8));
        bb.put((byte) (init[3] >>> 16));
        bb.put((byte) (init[3] >>> 24));
        //clear buf
        reset();

        return bb.array();
    }

    @Override
    public int digest(byte[] buf, int offset, int len) throws DigestException {
        byte[] md5 = digest();
        if (len >= md5.length) {
            System.arraycopy(md5, 0, buf, offset, md5.length);
            return md5.length;
        } else {
            System.arraycopy(md5, 0, buf, offset, len);
            return len;
        }
    }

    @Override
    public byte[] digest(byte[] input) {
        update(input);
        return digest();
    }

    int s[] = {7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
            5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 4, 11,
            16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 6, 10, 15,
            21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21};
    int[] k = {0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee, 0xf57c0faf,
            0x4787c62a, 0xa8304613, 0xfd469501, 0x698098d8, 0x8b44f7af,
            0xffff5bb1, 0x895cd7be, 0x6b901122, 0xfd987193, 0xa679438e,
            0x49b40821, 0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
            0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8, 0x21e1cde6,
            0xc33707d6, 0xf4d50d87, 0x455a14ed, 0xa9e3e905, 0xfcefa3f8,
            0x676f02d9, 0x8d2a4c8a, 0xfffa3942, 0x8771f681, 0x6d9d6122,
            0xfde5380c, 0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
            0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05, 0xd9d4d039,
            0xe6db99e5, 0x1fa27cf8, 0xc4ac5665, 0xf4292244, 0x432aff97,
            0xab9423a7, 0xfc93a039, 0x655b59c3, 0x8f0ccc92, 0xffeff47d,
            0x85845dd1, 0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
            0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391};

    private int[] md5_2(byte[] bytes, int[] input) {
        int A = input[0], B = input[1], C = input[2], D = input[3];
        int a = A, b = B, c = C, d = D;


        for (int i = 0; i < 64; i++) {
            int f, g;
            if (i < 16) {
                f = (b & c) | (~b & d);
                g = i;
            } else if (i < 32) {
                f = (b & d) | (~d & c);
                g = (5 * i + 1) % 16;
            } else if (i < 48) {
                f = b ^ c ^ d;
                g = (3 * i + 5) % 16;
            } else {
                f = c ^ (~d | b);
                g = 7 * i % 16;
            }

            int m = ((bytes[4 * g + 3] & 0xff) << 24)
                    | ((bytes[4 * g + 2] & 0xff) << 16)
                    | ((bytes[4 * g + 1] & 0xff) << 8)
                    | ((bytes[4 * g + 0] & 0xff) << 0);//
            int b_temp = b;
            b = b + Integer.rotateLeft(a + f + m + k[i], s[i]);
            a = d;
            d = c;
            c = b_temp;
        }
        A += a;
        B += b;
        C += c;
        D += d;

        input[0] = A;
        input[1] = B;
        input[2] = C;
        input[3] = D;

        return input;
    }


//    public static void main(String[] args) {
//        MD5 md5 = new MD5();
//        md5.update("hello".getBytes());//5d41402abc4b2a76b9719d911017c592
//        System.out.println(byteArrayToHex(md5.digest()));
//
//        MD5 md52 = new MD5();//3f863ccb5fcb828e4eaaed663b28763a
//        File f = new File("D:\\GitHub\\miniJVM\\binary\\win_x64\\apps\\shl.jar");
//        byte[] b = new byte[1024];
//        try {
//            FileInputStream fis = new FileInputStream(f);
//            while (fis.available() > 0) {
//                int len = fis.read(b);
//                md52.update(b, 0, len);
//            }
//            fis.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println(byteArrayToHex(md52.digest()));
//        System.out.println(byteArrayToHex(md52.digest()));
//        System.out.println(byteArrayToHex(md52.digest()));
//    }
//
//    public static String byteArrayToHex(byte[] a) {
//        StringBuilder sb = new StringBuilder(a.length * 2);
//        for (byte b : a)
//            sb.append(String.format("%02x", b & 0xff));
//        return sb.toString();
//    }
}
