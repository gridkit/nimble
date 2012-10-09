package org.gridkit.nimble.platform;

import org.gridkit.nimble.scenario.Scenario;
import com.google.common.util.concurrent.ListenableFuture;

public interface Play {
    Scenario getScenario();
        
    Status getStatus();
    
    ListenableFuture<Void> getCompletionFuture();
    
    public static enum Status {
        Success, Failure, InProgress, Canceled
    }
}
