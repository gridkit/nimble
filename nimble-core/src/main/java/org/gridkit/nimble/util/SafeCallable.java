package org.gridkit.nimble.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeCallable<V> implements Callable<V>, Runnable {
    private static final Logger log = LoggerFactory.getLogger(SafeCallable.class);

    private final Callable<V> delegate;

    public SafeCallable(Callable<V> delegate) {
        this.delegate = delegate;
    }
    
    public SafeCallable(Runnable runnable) {
        this(Executors.<V>callable(runnable, null));
    }
    
    @Override
    public V call() throws InterruptedException {
        try {
            return delegate.call();
        } catch (InterruptedException e) {
            log.warn("Callable delegate '" + delegate + "' was interrupted", e);
            throw e;
        } catch (Error e) {
            log.warn("Error in callable delegate '" + delegate + "'", e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to invoke callable delegate '" + delegate + "'", e);
            return null;
        }
    }

    @Override
    public void run() {
        try {
            call();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
