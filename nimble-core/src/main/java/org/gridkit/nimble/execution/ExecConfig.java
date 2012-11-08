package org.gridkit.nimble.execution;

import java.util.Collection;

public interface ExecConfig {
    Collection<Task> getTasks();
    
    ExecCondition getCondition();
            
    boolean isManualShutdown();
}
