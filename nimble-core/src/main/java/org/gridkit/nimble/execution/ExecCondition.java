package org.gridkit.nimble.execution;

public interface ExecCondition {
    void init();
    
    boolean satisfied();
}
