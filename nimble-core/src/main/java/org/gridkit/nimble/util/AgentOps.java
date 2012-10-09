package org.gridkit.nimble.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.platform.RemoteAgent;

public class AgentOps {
    public static Map<String, Integer> countByLabel(Collection<RemoteAgent> agents) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        
        for (RemoteAgent agent : agents) {
            for (String label : agent.getLabels()) {
                Integer val = result.get(label);
                
                if (val == null) {
                    result.put(label, 1);
                } else {
                    result.put(label, val + 1);
                }
            }
        }
        
        return result;
    }
    
    public static int getAgentsCount(String label, Map<String, Integer> countByLabel) {
        Integer count = countByLabel.get(label);
        return count == null ? 0 : count;
    }
}
