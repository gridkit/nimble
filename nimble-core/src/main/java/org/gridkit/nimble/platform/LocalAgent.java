package org.gridkit.nimble.platform;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;

public interface LocalAgent {
    Set<String> getLabels();
    
    TimeService getTimeService();
    
    ConcurrentMap<String, Object> getAttrsMap();
    
    Logger getLogger(String name);
}
