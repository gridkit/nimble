package org.gridkit.nimble.scenario;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.gridkit.nimble.platform.Play;
import org.gridkit.nimble.platform.RemoteAgent;

public interface Scenario {
    Play play(Context context);
    
    public interface Context {
        String getContextId();
                
        ExecutorService getExecutor();
        
        Collection<RemoteAgent> getAgents();
    }
}
