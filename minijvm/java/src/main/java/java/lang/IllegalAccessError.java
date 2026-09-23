package java.lang;

/** A linked field or method is not accessible to the caller. */
public class IllegalAccessError extends IncompatibleClassChangeError {
    public IllegalAccessError() {
        super();
    }

    public IllegalAccessError(String message) {
        super(message);
    }
}
