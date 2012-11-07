package org.gridkit.nimble.execution;

public interface ExecutionDriver {
    /**
     * @return thread per task pool
     */
    public TaskPool newTaskPool(String name);
    
    /**
     * @return fixed number of threads pool
     */
    public TaskPool newTaskPool(String name, int threads);
}
