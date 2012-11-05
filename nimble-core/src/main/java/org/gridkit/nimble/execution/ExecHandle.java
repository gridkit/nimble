package org.gridkit.nimble.execution;

public interface ExecHandle {
    ExecHandle start();
    
    ExecBuilder join();
        
    void stop();
}
