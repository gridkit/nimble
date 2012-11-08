package org.gridkit.nimble.execution;

public interface Semaphore {
    void acquire() throws InterruptedException;
    
    void release();
}
