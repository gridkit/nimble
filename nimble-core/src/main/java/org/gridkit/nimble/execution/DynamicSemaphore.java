package org.gridkit.nimble.execution;

public interface DynamicSemaphore extends Semaphore {    
    void permits(int nPermits);
    
    void disable();
}
