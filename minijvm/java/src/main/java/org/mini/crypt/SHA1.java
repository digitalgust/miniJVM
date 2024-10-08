package org.mini.crypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * This is my custom implementation of the cryptographic hash function SHA-1 on a String.
 * <p>
 * The ideas in this program are modified from a lecture on youtube given by Christof Paar,
 * as well as the ideas found in the FIPS publication.
 *
 * @author Joe Peacock
 * @Version 1.2
 * @Date 11/22/14
 */
public class SHA1 extends MessageDigest {
    //the message to be hashed
    private static String message = "Default Message Value";
    //the iniial input into the algorithm
    private static int A = 0x67452301;
    private static int B = 0xEFCDAB89;
    private static int C = 0x98BADCFE;
    private static int D = 0x10325476;
    private static int E = 0xC3D2E1F0;

    int[] init = new int[5]; //initial inputs.  Stored in an array for convenience


    byte[] buf;
    int bufOffset;
    long messageBits;

    byte[] oneByte = new byte[]{0};


    public SHA1() {
        super("SHA-1");
        reset();
    }

    @Override
    public void reset() {
        state = INITIAL;

        init = new int[5]; //iniial inputs.  Stored in an array for convenience
        buf = new byte[64];
        bufOffset = 0;
        messageBits = 0;

        init[0] = A;
        init[1] = B;
        init[2] = C;
        init[3] = D;
        init[4] = E;
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
                    init = sha_2(buf, init);
                    bufOffset = 0;
                    srcOffset += copyLength;
                    messageBits += copyLength * 8;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
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
            init = sha_2(buf, init);
            bufOffset = 0;
            Arrays.fill(buf, (byte) 0);
        }
        for (int i = 0; i < 8; i++) {
            buf[64 - 1 - i] = (byte) ((messageBits >>> (8 * i)) & 0xFF);
        }

        init = sha_2(buf, init);

        ByteBuffer bb = ByteBuffer.allocate(20);
        bb.put((byte) (init[0] >>> 24));
        bb.put((byte) (init[0] >>> 16));
        bb.put((byte) (init[0] >>> 8));
        bb.put((byte) (init[0] >>> 0));
        bb.put((byte) (init[1] >>> 24));
        bb.put((byte) (init[1] >>> 16));
        bb.put((byte) (init[1] >>> 8));
        bb.put((byte) (init[1] >>> 0));
        bb.put((byte) (init[2] >>> 24));
        bb.put((byte) (init[2] >>> 16));
        bb.put((byte) (init[2] >>> 8));
        bb.put((byte) (init[2] >>> 0));
        bb.put((byte) (init[3] >>> 24));
        bb.put((byte) (init[3] >>> 16));
        bb.put((byte) (init[3] >>> 8));
        bb.put((byte) (init[3] >>> 0));
        bb.put((byte) (init[4] >>> 24));
        bb.put((byte) (init[4] >>> 16));
        bb.put((byte) (init[4] >>> 8));
        bb.put((byte) (init[4] >>> 0));
        //clear buf
        reset();

        return bb.array();
    }

    @Override
    public int digest(byte[] buf, int offset, int len) throws DigestException {
        return 0;
    }

    @Override
    public byte[] digest(byte[] input) {
        return new byte[0];
    }


    private static int[] sha_2(byte[] block, int[] input) {

        int[] W = new int[80];

        for (int j = 0; j < 16; j++) {
            W[j] = ((block[(j << 2)] & 0xFF) << 24)
                    | ((block[(j << 2) + 1] & 0xFF) << 16)
                    | ((block[(j << 2) + 2] & 0xFF) << 8)
                    | (block[(j << 2) + 3] & 0xFF);
        }

        for (int j = 16; j < 80; j++) {
            W[j] = leftRotate(W[j - 3] ^ W[j - 8] ^ W[j - 14] ^ W[j - 16], 1);
        }

        int a = input[0];
        int b = input[1];
        int c = input[2];
        int d = input[3];
        int e = input[4];

        for (int j = 0; j < 80; j++) {
            int f, k;
            if (j < 20) {
                f = (b & c) | ((~b) & d);
                k = 0x5A827999;
            } else if (j < 40) {
                f = b ^ c ^ d;
                k = 0x6ED9EBA1;
            } else if (j < 60) {
                f = (b & c) | (b & d) | (c & d);
                k = 0x8F1BBCDC;
            } else {
                f = b ^ c ^ d;
                k = 0xCA62C1D6;
            }

            int temp = leftRotate(a, 5) + f + e + k + W[j];
            e = d;
            d = c;
            c = leftRotate(b, 30);
            b = a;
            a = temp;
        }

        input[0] += a;
        input[1] += b;
        input[2] += c;
        input[3] += d;
        input[4] += e;

        return input;
    }

    private static int leftRotate(int value, int bits) {
        return (value << bits) | (value >>> (32 - bits));
    }


//    public static void main(String[] args) {
//        SHA1 md5 = new SHA1();//aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d
//        md5.update("01234567890123456789012345678901234567890123456789012345".getBytes());
////        md5.update("hello".getBytes());
//        System.out.println(byteArrayToHex(md5.digest()));
//
//        SHA1 md52 = new SHA1();//6fccfe04eeabef6526f5317f9480ddfe6a1a871b
//        File f = new File("D:\\GitHub\\miniJVM\\binary\\win_x64\\apps\\shl.jar");
//        byte[] b = new byte[123];
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
