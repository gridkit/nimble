package org.gridkit.nimble.execution;

public abstract class AbstractTask implements Task {
    private final boolean interrupt;

    public AbstractTask(boolean interrupt) {
        this.interrupt = interrupt;
    }

    @Override
    public void cancel(Interruptible thread) throws Exception {
        if (interrupt) {
            thread.interrupt();
        }
    }
}
