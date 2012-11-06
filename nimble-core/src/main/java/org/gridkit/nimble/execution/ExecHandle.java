package org.gridkit.nimble.execution;

public interface ExecHandle {
    void start();
    
    void join();
    
    ExecHandle proceed(ExecConfig config);
        
    void stop();
}
