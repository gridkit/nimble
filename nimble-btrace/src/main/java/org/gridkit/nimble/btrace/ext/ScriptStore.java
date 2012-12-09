package org.gridkit.nimble.btrace.ext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptStore {
    private final String scriptClass;
    private Map<String, SampleStore> sampleStores = new HashMap<String, SampleStore>();
    
    public ScriptStore(String scriptClass) {
        this.scriptClass = scriptClass;
    }
    
    public synchronized SampleStore addSampleStore(String name, int capacity) {
        if (sampleStores != null) {
            SampleStore result = new SampleStore(name, capacity);
            sampleStores.put(name, result);
            return result;
        } else {
            return null;
        }
    }
    
    public synchronized Collection<SampleStore> getSampleStores() {
        if (sampleStores != null) {
            List<SampleStore> result = new ArrayList<SampleStore>(sampleStores.size());
            result.addAll(sampleStores.values());
            return result;
        } else {
            return Collections.emptyList();
        }
    }
    
    public synchronized void close() {
        for (SampleStore sampleStore : sampleStores.values()) {
            sampleStore.close();
        }
        sampleStores = null;
    }

    public String getScriptClass() {
        return scriptClass;
    }
}
