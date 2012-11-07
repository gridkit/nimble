package org.gridkit.nimble.execution;

import org.gridkit.nimble.driver.Activity;

public interface ExecutionPool {
    Activity exec(ExecConfig config);
    
    void concurrentTasks(int nTasks);
    
    void unlimitConcurrentTasks();
    
    void shutdown();
}
