package org.gridkit.nimble.execution;

import org.gridkit.nimble.driver.Activity;

//TODO maybe introduce fixed and dynamic task pools
public interface TaskPool {
    Activity exec(ExecConfig config);
    
    void setThreadsNumber(int nThreads);
    
    void setThreadPerTask();
    
    void shutdown();
}
