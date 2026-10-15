package java.util.concurrent;

/** A computation that returns a result or throws an exception. */
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
