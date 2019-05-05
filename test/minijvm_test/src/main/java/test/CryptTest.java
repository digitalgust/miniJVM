/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import org.mini.crypt.AsynCrypt;
import org.mini.crypt.XorCrypt;

/**
 *
 * @author Gust
 */
public class CryptTest {

    void t1() {
        AsynCrypt.test();
    }

    void t2() {
        byte[] key = XorCrypt.genKey(5);
        System.out.println("key=" + AsynCrypt.bytesToHex(key));
        byte[] src = {'a', 'b'};
        XorCrypt.xor_encrypt(key, key);
        System.out.println("src=" + AsynCrypt.bytesToHex(src));
        byte[] cr = XorCrypt.xor_encrypt(src, key);
        System.out.println("crypt= " + AsynCrypt.bytesToHex(cr));
        byte[] dcr = XorCrypt.xor_decrypt(cr, key);
        System.out.println("dcrypt= " + AsynCrypt.bytesToHex(dcr));

        byte fake = (byte) (src[0] ^ cr[0]);
        System.out.println("fake=" + AsynCrypt.bytesToHex(new byte[]{fake}));

        byte[] src1 = {'3', '4'};
        System.out.println("src1=" + AsynCrypt.bytesToHex(src1));
        byte[] cr1 = XorCrypt.xor_encrypt(src1, key);
        System.out.println("crypt1= " + AsynCrypt.bytesToHex(cr1));
        byte[] dcr1 = new byte[src1.length];
        dcr1[0] = (byte) (cr1[0] ^ fake);
        dcr1[1] = (byte) (cr1[1] ^ fake);
        System.out.println("dcrypt1= " + AsynCrypt.bytesToHex(dcr1));
    }

    public static void main(String args[]) {
        CryptTest obj = new CryptTest();
        obj.t1();
        obj.t2();

    }
}
