package org.mini.vm;

public interface ThreadLifeHandler {
    void threadCreated(Thread t);

    void threadDestroy(Thread t);
}
