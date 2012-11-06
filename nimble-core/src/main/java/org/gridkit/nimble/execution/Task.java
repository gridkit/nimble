package org.gridkit.nimble.execution;

import java.util.concurrent.Future;

public interface Task {
    void execute() throws Exception;
    
    void cancel(Future<Void> future) throws Exception;
}
