/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.crypt;

import java.util.Random;

/**
 *
 * @author Gust
 */
public class XorCrypt {

    static Random rand = new Random();

    /**
     *
     * generate a key with length byte
     *
     * @param len
     * @return
     */
    public static byte[] genKey(int len) {
        if (len < 0) {
            return null;
        }
        byte[] key = new byte[len];
        for (int i = 0; i < len; i++) {
            key[i] = (byte) rand.nextInt(0xff);
        }
        return key;
    }

    /**
     *
     * encrypt or decrypt msg with key
     *
     * when msg encrypted , must decrypt it with same key
     *
     * @param msg
     * @param key
     * @return
     */
    public static byte[] xor_encrypt(byte[] msg, byte[] key) {
        return encrypt(msg, key);
    }
//    public static byte[] xor_encrypt(byte[] msg, byte[] key) {
//        byte[] r = new byte[msg.length];
//
//        for (int i = 0, imax = msg.length; i < imax; i++) {
//            int v = msg[i] & 0xff;
//            for (int j = 0, jmax = key.length; j < jmax; j++) {
//                int k = key[j] & 0xff;
//                int bitshift = k % 8;
//
//                int v1 = (v << bitshift);
//                int v2 = (v >>> (8 - bitshift));
//                v = (v1 | v2);
//
//                v = (v ^ k) & 0xff;
//            }
//            r[i] = (byte) v;
//        }
//        return r;
//    }

    /**
     * decrypt xor info
     *
     * @param msg
     * @param key
     * @return
     */
    public static byte[] xor_decrypt(byte[] msg, byte[] key) {
        return decrypt(msg, key);
    }
//        public static byte[] xor_decrypt(byte[] msg, byte[] key) {
//        byte[] r = new byte[msg.length];
//
//        for (int i = 0, imax = msg.length; i < imax; i++) {
//            int v = msg[i] & 0xff;
//            for (int j = key.length - 1; j >= 0; j--) {
//                int k = key[j] & 0xff;
//                v = (v ^ k) & 0xff;
//
//                int bitshift = k % 8;
//
//                int v1 = (v >>> bitshift);
//                int v2 = (v << (8 - bitshift));
//                v = (v1 | v2);
//
//            }
//            r[i] = (byte) v;
//        }
//        return r;
//    }

    static native byte[] encrypt(byte[] msg, byte[] key);

    static native byte[] decrypt(byte[] msg, byte[] key);
}
