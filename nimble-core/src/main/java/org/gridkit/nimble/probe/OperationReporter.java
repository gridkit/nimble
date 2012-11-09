package org.gridkit.nimble.probe;

// TODO add more methods as needed
public interface OperationReporter {
    void start(String operation);
        
    void finish(String operation);
    
    void scalar(String key, double value);
}
