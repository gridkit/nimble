package org.gridkit.nimble.execution;

public interface ExecutionDriver {
    /**
     * @return thread per task pool
     */
    public ExecutionPool newExecutionPool(String name);
    
    /**
     * @return fixed number of threads pool
     */
    public ExecutionPool newExecutionPool(String name, int nTasks);
}
