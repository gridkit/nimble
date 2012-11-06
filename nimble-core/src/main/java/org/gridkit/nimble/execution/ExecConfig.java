package org.gridkit.nimble.execution;

import java.util.Collection;

import org.gridkit.util.concurrent.BlockingBarrier;

public interface ExecConfig {
    Collection<Task> getTasks();
    
    ExecCondition getCondition();
    
    BlockingBarrier getBarrier();
    
    int getThreads();
    
    boolean isContinuous();
}
