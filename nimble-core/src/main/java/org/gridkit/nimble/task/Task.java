package org.gridkit.nimble.task;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;

import org.gridkit.nimble.platform.TimeService;
import org.gridkit.nimble.statistics.StatsReporter;

public interface Task extends Serializable {    
    void excute(Context context) throws Exception;
    
    public interface Context {
        StatsReporter getStatReporter();
        
        ConcurrentMap<String, Object> getAttrsMap();
        
        TimeService getTimeService();
        
        Logger getLogger();
        
        void setFailure();
    }
}
