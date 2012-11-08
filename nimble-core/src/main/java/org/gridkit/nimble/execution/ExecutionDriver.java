package org.gridkit.nimble.execution;

public interface ExecutionDriver {
    public ExecutionPool newExecutionPool(String name);
        
    public DynamicSemaphore newDynamicSemaphore(int nPermits);
}
