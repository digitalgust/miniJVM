package java.lang;

public class TypeNotPresentException extends RuntimeException {
    private final String typeName;
    public TypeNotPresentException(String typeName, Throwable cause) {
        super("Type " + typeName + " not present", cause);
        this.typeName = typeName;
    }
    public String typeName() { return typeName; }
}
