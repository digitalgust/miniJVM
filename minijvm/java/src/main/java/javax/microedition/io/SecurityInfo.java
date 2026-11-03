/* SPDX-License-Identifier: MIT */
package javax.microedition.io;

public interface SecurityInfo {
    String getProtocolVersion();
    String getProtocolName();
    String getCipherSuite();
}
