/* SPDX-License-Identifier: MIT */

package java.lang;

public class UnsatisfiedLinkError extends LinkageError {
    public UnsatisfiedLinkError() { super(); }
    public UnsatisfiedLinkError(String message) { super(message); }
}
