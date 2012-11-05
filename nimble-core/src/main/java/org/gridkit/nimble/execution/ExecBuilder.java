package org.gridkit.nimble.execution;

import org.gridkit.util.concurrent.BlockingBarrier;

public interface ExecBuilder {
    ExecBuilder provider(TaskProvider provider);
    
    ExecBuilder condition(ExecCondition condition);
    
    ExecBuilder barrier(BlockingBarrier barrier);
    
    ExecBuilder threads(int threads);
    
    ExecBuilder continuous(boolean continuous);
    
    ExecHandle build();
}
