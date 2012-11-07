package org.gridkit.nimble.execution;

public interface Task {
    void run() throws Exception;
    
    void cancel(Thread thread) throws Exception;
}
