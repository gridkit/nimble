package org.gridkit.nimble.npivot;

import java.util.Set;

public interface Sample {
    Set<Object> keys();
    
    <T> T get(Object key);
}
