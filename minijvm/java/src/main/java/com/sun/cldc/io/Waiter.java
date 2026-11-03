/* SPDX-License-Identifier: MIT */
package com.sun.cldc.io;

/** Optional native hook retained for binary compatibility. */
public final class Waiter {
    private Waiter() {
    }

    public static native void waitForIO();
}
