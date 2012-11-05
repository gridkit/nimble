package org.gridkit.nimble.execution;

import java.util.concurrent.Future;

public interface Task {
    void execute() throws Exception;
    
    /**
     * Cancel task execute and cleanup resources
     * 
     * @param future - null if task was created but not executed
     */
    void cancel(Future<Void> future) throws Exception;
}
