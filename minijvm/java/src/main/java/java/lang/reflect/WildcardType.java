package java.lang.reflect;

public interface WildcardType extends Type {

    Type[] getUpperBounds();

    Type[] getLowerBounds();
}
