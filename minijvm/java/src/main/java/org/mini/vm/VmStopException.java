package org.mini.vm;

public class VmStopException extends RuntimeException {

    public VmStopException() {
        super();
    }

    public VmStopException(String s) {
        super(s);
    }
}
