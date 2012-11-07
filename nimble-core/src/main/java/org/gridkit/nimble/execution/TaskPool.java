package org.gridkit.nimble.execution;

import org.gridkit.nimble.driver.Activity;

public interface TaskPool {
    Activity exec(ExecConfig config);
    
    void setThreadsNumber(int nThreads);
    
    void setThreadPerTask();
    
    void shutdown();
}
