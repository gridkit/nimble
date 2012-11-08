package org.gridkit.nimble.execution;

// TODO create abstraction to limit concurrent tasks number from master node
public interface ExecutionDriver {
    /**
     * @return task execution pool
     */
    public ExecutionPool newExecutionPool(String name);
}
