package java.lang;

/** The selected instance method has no concrete implementation. */
public class AbstractMethodError extends IncompatibleClassChangeError {
    public AbstractMethodError() {
        super();
    }

    public AbstractMethodError(String message) {
        super(message);
    }
}
