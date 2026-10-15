package java.lang;

public class ExceptionInInitializerError extends LinkageError {
    private Throwable exception;
    public ExceptionInInitializerError() { super(); initCause(null); }
    public ExceptionInInitializerError(String message) { super(message); initCause(null); }
    public ExceptionInInitializerError(Throwable exception) { this(); this.exception = exception; }
    public Throwable getException() { return exception; }
    public Throwable getCause() { return exception; }
}
