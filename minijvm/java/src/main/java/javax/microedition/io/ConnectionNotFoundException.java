/* SPDX-License-Identifier: MIT */
package javax.microedition.io;

import java.io.IOException;

public class ConnectionNotFoundException extends IOException {
    public ConnectionNotFoundException() {
    }

    public ConnectionNotFoundException(String message) {
        super(message);
    }
}
