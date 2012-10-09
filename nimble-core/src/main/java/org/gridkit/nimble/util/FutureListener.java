package org.gridkit.nimble.util;

public interface FutureListener<V> {
    public static enum FailureEvent {
        OnFailure, OnSuccess, OnCancel
    }
    
    void onSuccess(V result);

    void onFailure(Throwable t, FailureEvent event);
    
    void onCancel();
}
