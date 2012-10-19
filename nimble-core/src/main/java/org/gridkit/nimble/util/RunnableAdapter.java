package org.gridkit.nimble.util;

import java.util.concurrent.Callable;

public class RunnableAdapter implements Runnable {
    private final Callable<?> delegate;

    public RunnableAdapter(Callable<?> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void run() {
        try {
            delegate.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
