package org.gridkit.nimble.execution;

public interface Task {
    void run() throws Exception;
    
    void cancel(Interruptible thread) throws Exception;
    
    public interface Interruptible {
        void interrupt();
    }
}
