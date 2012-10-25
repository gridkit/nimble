package org.gridkit.nimble.btrace.ext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.java.btrace.ext.Printer;

public class ScriptStore {
    private final String scriptClass;
    private final ConcurrentMap<String, SampleStore> sampleStores = new ConcurrentHashMap<String, SampleStore>();
    
    public ScriptStore(String scriptClass) {
        this.scriptClass = scriptClass;
    }
    
    public SampleStore add(String name, int capacity) {
        SampleStore result = new SampleStore(name, capacity);

        if (sampleStores.put(name, result) != null) {
            Printer.println("Replacing existing sample store with name '" + name + "' for script '" + scriptClass + "'");
        }

        return result;
    }
    
    public Collection<SampleStore> getSampleStores() {
        List<SampleStore> result = new ArrayList<SampleStore>(sampleStores.size());
        result.addAll(sampleStores.values());
        return result;
    }

    public String getScriptClass() {
        return scriptClass;
    }
}
