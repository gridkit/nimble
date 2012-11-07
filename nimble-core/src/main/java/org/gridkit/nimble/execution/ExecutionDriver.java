package org.gridkit.nimble.execution;

public interface ExecutionDriver {
    /**
     * @return unlimited concurrent tasks execution pool
     */
    public ExecutionPool newExecutionPool(String name);
    
    /**
     * @return fixed number of concurrent tasks execution pool
     */
    public ExecutionPool newExecutionPool(String name, int nTasks);
}
