package org.gridkit.nimble.util;

public class SettableReference<T> {
    private T value;

    public SettableReference() {
        this(null);
    }
    
    public SettableReference(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}
