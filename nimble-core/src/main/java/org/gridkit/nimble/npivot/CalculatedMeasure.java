package org.gridkit.nimble.npivot;

import java.util.Set;

public interface CalculatedMeasure {
    Set<Object> groups();
    
    Set<Object> measures();
    
    Object calculate(Sample sample);
}
