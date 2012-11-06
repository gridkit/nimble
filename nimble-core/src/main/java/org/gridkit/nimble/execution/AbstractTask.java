package org.gridkit.nimble.execution;

public abstract class AbstractTask implements Task {
    @Override
    public void cancel(Interruptible thread) throws Exception {
        thread.interrupt();
    }
}
