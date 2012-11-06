package org.gridkit.nimble.execution;

import org.gridkit.util.concurrent.BlockingBarrier;

public interface ExecConfig {
    TaskProvider getProvider();
    
    ExecCondition getCondition();
    
    BlockingBarrier getBarrier();
    
    int getThreads();
    
    boolean isContinuous();
}
