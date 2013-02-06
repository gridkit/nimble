package org.gridkit.nimble.npivot;

import java.util.Set;

public interface Function {
    Object apply(Sample sample);
    
    Set<Object> parameters();
}
