/* SPDX-License-Identifier: MIT */
package com.sun.cldc.io;

import java.io.IOException;
import javax.microedition.io.Connection;

/** Protocol factory contract used by {@code Connector}. */
public interface ConnectionBaseInterface {
    Connection openPrim(String name, int mode, boolean timeouts) throws IOException;
}
