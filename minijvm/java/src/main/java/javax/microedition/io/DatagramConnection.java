/* SPDX-License-Identifier: MIT */
package javax.microedition.io;

import java.io.IOException;

public interface DatagramConnection extends Connection {
    int getMaximumLength() throws IOException;
    int getNominalLength() throws IOException;
    void send(Datagram datagram) throws IOException;
    void receive(Datagram datagram) throws IOException;
    Datagram newDatagram(int size) throws IOException;
    Datagram newDatagram(int size, String address) throws IOException;
    Datagram newDatagram(byte[] buffer, int size) throws IOException;
    Datagram newDatagram(byte[] buffer, int size, String address) throws IOException;
}
