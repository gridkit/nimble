package org.gridkit.nimble.btrace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BTraceScriptSettings implements Serializable {
    private static final long serialVersionUID = 2727909469417318175L;
    
    private Class<?> scriptClass;
    private List<String> args = new ArrayList<String>();
    private Long pollDelayMs;
    private Long timeoutMs;
    
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

    public Long getPollDelayMs() {
        return pollDelayMs;
    }

    public void setPollDelayMs(Long pollDelayMs) {
        this.pollDelayMs = pollDelayMs;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long operationTimeoutMs) {
        this.timeoutMs = operationTimeoutMs;
    }

    public BTraceScriptSettings init(long pollDelayMs, long timeoutMs) {
        BTraceScriptSettings result = new BTraceScriptSettings();
        
        result.setScriptClass(this.scriptClass);
        result.setArgs(this.args);
        
        if (this.pollDelayMs == null) {
            result.setPollDelayMs(pollDelayMs);
        } else {
            result.setPollDelayMs(this.pollDelayMs);
        }
        
        if (this.timeoutMs == null) {
            result.setTimeoutMs(timeoutMs);
        } else {
            result.setTimeoutMs(this.timeoutMs);
        }
        
        return result;
    }
    
    @Override
    public String toString() {
        return "BTraceScriptSettings [scriptClass=" + scriptClass + ", args="
                + args + ", pollDelayMs=" + pollDelayMs
                + ", timeoutMs=" + timeoutMs + "]";
    }
}
