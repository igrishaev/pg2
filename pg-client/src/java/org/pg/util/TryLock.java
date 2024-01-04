package org.pg.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TryLock implements AutoCloseable {

    private final Lock lock = new ReentrantLock();

    public TryLock get() {
        lock.lock();
        return this;
    }

    public void close () {
        lock.unlock();
    }
}
