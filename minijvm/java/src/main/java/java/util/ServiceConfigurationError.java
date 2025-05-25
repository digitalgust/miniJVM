package java.util;


public class ServiceConfigurationError
        extends Error {


    public ServiceConfigurationError(String msg) {
        super(msg);
    }

    public ServiceConfigurationError(String msg, Throwable cause) {
        super(msg, cause);
    }

}
