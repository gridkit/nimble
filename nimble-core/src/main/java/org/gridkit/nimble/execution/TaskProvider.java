package org.gridkit.nimble.execution;

public interface TaskProvider {
    /**
     * Called in concurrent environment
     * 
     * @return null if no more tasks to execute
     */
    Task nextTask();
}
