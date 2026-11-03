/* SPDX-License-Identifier: MIT */
package javax.microedition.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface Datagram extends DataInput, DataOutput {
    String getAddress();
    byte[] getData();
    int getLength();
    int getOffset();
    void setAddress(String address) throws IOException;
    void setAddress(Datagram reference);
    void setLength(int length);
    void setData(byte[] buffer, int offset, int length);
    void reset();
}
