package java.lang;

/** A class definition changed incompatibly after a caller was compiled. */
public class IncompatibleClassChangeError extends LinkageError {
    public IncompatibleClassChangeError() {
        super();
    }

    public IncompatibleClassChangeError(String message) {
        super(message);
    }
}
