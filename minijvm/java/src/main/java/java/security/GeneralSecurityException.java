package java.security;


public class GeneralSecurityException extends Exception {


    public GeneralSecurityException() {
        super();
    }

    public GeneralSecurityException(String msg) {
        super(msg);
    }

    public GeneralSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeneralSecurityException(Throwable cause) {
        super(cause);
    }
}

