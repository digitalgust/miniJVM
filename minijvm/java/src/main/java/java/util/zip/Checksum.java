/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools and Templates, and edit the template in the editor.
 */
package java.util.zip;

public
interface Checksum {

    void update(int b);

    void update(byte[] b, int off, int len);

    long getValue();

    void reset();
}
