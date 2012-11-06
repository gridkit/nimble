package org.gridkit.nimble.execution;

import java.util.concurrent.Future;

public abstract class AbstractTask implements Task {
    private final boolean mayInterruptIfRunning;
    
    public AbstractTask(boolean mayInterruptIfRunning) {
        this.mayInterruptIfRunning = mayInterruptIfRunning;
    }
    
    public AbstractTask() {
        this(true);
    }
    
    @Override
    public void cancel(Future<Void> future) throws Exception {
        if (future != null) {
            future.cancel(mayInterruptIfRunning);
        }
    }
}
