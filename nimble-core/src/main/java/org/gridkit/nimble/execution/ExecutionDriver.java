package org.gridkit.nimble.execution;

public interface ExecutionDriver {
    public ExecHandle newExecution(ExecConfig config);
    
    public void shutdown();
}
