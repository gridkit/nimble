package org.gridkit.nimble.btrace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BTraceScriptSettings implements Serializable {
    private static final long serialVersionUID = 2727909469417318175L;
    
    private Class<?> scriptClass;
    private List<String> args = new ArrayList<String>();
    private long pollDelayMs; 
    
    public Class<?> getScriptClass() {
        return scriptClass;
    }
    
    public void setScriptClass(Class<?> scriptClass) {
        this.scriptClass = scriptClass;
    }
    
    public List<String> getArgs() {
        return args;
    }
    
    public void setArgs(List<String> args) {
        this.args = args;
    }
    
    public String[] getArgsArray() {
        return args.toArray(new String[args.size()]);
    }

    public long getPollDelayMs() {
        return pollDelayMs;
    }

    public void setPollDelayMs(long pollDelayMs) {
        this.pollDelayMs = pollDelayMs;
    }
}
