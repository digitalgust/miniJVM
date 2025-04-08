package org.mini.crypt;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Random;

/**
 * RSA like asymmetric encryption
 * <pre>
 *     public static void test() {
 *         int NUMBER_OF_BIT = 512;
 *
 *         String MY_TEXT = "This is my text";
 *         AsynCrypt ac = new AsynCrypt(NUMBER_OF_BIT);
 *         System.out.println("plain text: " + MY_TEXT);
 *         byte[] encryptedMessage = ac.encryptMessageStr(MY_TEXT);
 *         BigInteger enc = new BigInteger(encryptedMessage);
 *         System.out.println("encrptyed text: " + enc);
 *         System.out.println("decrypt text: " + ac.decryptMessageStr(encryptedMessage));
 *
 *         byte[] xorkey = XorCrypt.genKey(8);
 *         System.out.println("plain bytes: " + bytesToHex(xorkey));
 *         byte[] enbytes = ac.encryptMessage(xorkey);
 *         System.out.println("encrptyed bytes: " + bytesToHex(enbytes));
 *         System.out.println("decrypt bytes: " + bytesToHex(ac.decryptMessage(enbytes)));
 *     }
 * </pre>
 *
 * @author Gust
 */
public class AsynCrypt {

    static Random rand = new Random(System.currentTimeMillis());

    //证书
    BigInteger certificate;
    byte[] certificateBytes;
    //公钥
    BigInteger publicKey;
    byte[] publicKeyBytes;
    //私钥
    private BigInteger privateKey;

    public AsynCrypt(BigInteger privateKey, BigInteger publicKey, BigInteger certificate) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        publicKeyBytes = publicKey.toByteArray();
        this.certificate = certificate;
        certificateBytes = certificate.toByteArray();
    }

    public AsynCrypt(int bitLen) {

        BigInteger p = generateBigPrime(bitLen);
        BigInteger q = generateBigPrime(bitLen);

        BigInteger n = calculateModulus(p, q);

        certificate = n;
        certificateBytes = n.toByteArray();
        BigInteger phi = calculateEulerFunction(p, q);

        BigInteger e = null;
        while (e == null) {
            try {
                e = calculateCoprime(phi, n, bitLen);
            } catch (Exception exc) {
                System.err.println("Coprime cannot be found. find again ...\n");
                exc.printStackTrace();
                return;
            }
        }
        publicKey = e;
        publicKeyBytes = e.toByteArray();
        BigInteger d = calculateInverseMod(e, phi);
        privateKey = d;

    }

    private BigInteger generateBigPrime(int numberOfBit) {
        return BigInteger.probablePrime(numberOfBit, rand);
    }

    private BigInteger calculateModulus(BigInteger number1, BigInteger number2) {
        return number1.multiply(number2);
    }

    private BigInteger calculateEulerFunction(BigInteger number1, BigInteger number2) {
        BigInteger a1 = number1.subtract(BigInteger.valueOf(1));
        BigInteger a2 = number2.subtract(BigInteger.valueOf(1));
        return a1.multiply(a2);
    }

    private BigInteger calculateCoprime(BigInteger phi, BigInteger modulus, int numberOfBytes) throws RuntimeException {
        BigInteger e = BigInteger.probablePrime(numberOfBytes, rand);

        while (phi.gcd(e).compareTo(BigInteger.ONE) != 0 && e.compareTo(modulus) < 0) {
            e.add(BigInteger.ONE);
            if (e.compareTo(modulus) >= 0) {
                throw new RuntimeException("Coprime cannot be found");
            }
        }
        return e;
    }

    private BigInteger calculateInverseMod(BigInteger number1, BigInteger number2) {
        return number1.modInverse(number2);
    }

    //=============================================================================
    //                              public method
    //=============================================================================
    //
    public byte[] encryptMessage(byte[] message) {
        /**
         * if the first bit of message is 1 , then message means negate, it
         * would error when decrypt, so add a byte , ensure it's not negate
         *
         */
        byte[] nb = new byte[message.length + 1];
        nb[0] = 0x07f;
        System.arraycopy(message, 0, nb, 1, message.length);
        //String s = bytesToHex(message);
        return new BigInteger(nb).modPow(publicKey, certificate).toByteArray();
    }

    public byte[] encryptMessageStr(String message) {
        try {
            byte[] b = message.getBytes("utf-8");
            return encryptMessage(b);
        } catch (UnsupportedEncodingException ex) {
        }
        throw new RuntimeException("get utf-8 bytes error");
    }

    public byte[] decryptMessage(byte[] encryptedMessage) {
        byte[] nb = new BigInteger(encryptedMessage).modPow(privateKey, certificate).toByteArray();
        byte[] b = new byte[nb.length - 1];
        System.arraycopy(nb, 1, b, 0, b.length);
        return b;
    }

    public String decryptMessageStr(byte[] encryptedMessage) {
        byte[] b = decryptMessage(encryptedMessage);
        try {
            String s = new String(b, "utf-8");
            return s;
        } catch (UnsupportedEncodingException ex) {
        }
        throw new RuntimeException("get utf-8 bytes error");
    }

    public byte[] getPublicKey() {
        return publicKeyBytes;
    }

    public byte[] getCertificate() {
        return certificateBytes;
    }

    public BigInteger getPublicKeyBig() {
        return publicKey;
    }

    public BigInteger getCertificateBig() {
        return certificate;
    }

    public BigInteger getPrivateKeyBig() {
        return privateKey;
    }

    public static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, imax = b.length; i < imax; i++) {
            String s = Integer.toHexString(b[i] & 0xff);
            sb.append(s.length() > 1 ? "" : '0').append(s);
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0, imax = s.length(); i < imax; i += 2) {
            int v1 = s.charAt(i);
            if (v1 >= 'a') {
                v1 -= 'a';
                v1 += 10;
            } else {
                v1 -= '0';
            }

            int v2 = s.charAt(i + 1);
            if (v2 >= 'a') {
                v2 -= 'a';
                v2 += 10;
            } else {
                v2 -= '0';
            }
            int v = v1 * 16 + v2;
            b[i >> 1] = (byte) v;

        }
        return b;
    }

//    public static void test() {
//        int NUMBER_OF_BIT = 512;
//
//        String MY_TEXT = "This is my text";
//        AsynCrypt ac = new AsynCrypt(NUMBER_OF_BIT);
//        System.out.println("plain text: " + MY_TEXT);
//        byte[] encryptedMessage = ac.encryptMessageStr(MY_TEXT);
//        BigInteger enc = new BigInteger(encryptedMessage);
//        System.out.println("encrptyed text: " + enc);
//        System.out.println("decrypt text: " + ac.decryptMessageStr(encryptedMessage));
//
//        byte[] xorkey = XorCrypt.genKey(8);
//        System.out.println("plain bytes: " + bytesToHex(xorkey));
//        byte[] enbytes = ac.encryptMessage(xorkey);
//        System.out.println("encrptyed bytes: " + bytesToHex(enbytes));
//        System.out.println("decrypt bytes: " + bytesToHex(ac.decryptMessage(enbytes)));
//    }

}
