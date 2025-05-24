package java.security;

public class PrivilegedActionException extends Exception {

    public PrivilegedActionException() {
        super();
    }

    public PrivilegedActionException(String msg) {
        super(msg);
    }

    public PrivilegedActionException(String message, Throwable cause) {
        super(message, cause);
    }


    /**
     * @serial
     */
    private Exception exception;

    public PrivilegedActionException(Exception exception) {
        super((Throwable) null);  // Disallow initCause
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    public Throwable getCause() {
        return exception;
    }

    public String toString() {
        String s = getClass().getName();
        return (exception != null) ? (s + ": " + exception.toString()) : s;
    }
}
