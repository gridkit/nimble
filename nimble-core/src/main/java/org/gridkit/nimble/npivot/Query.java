package org.gridkit.nimble.npivot;

import java.util.Set;

public interface Query {
    Filter filter();
    
    /**
     * either Function or SampleKey
     */
    Set<Object> groups();
    
    /**
     * either Measure or CalculatedMeasure
     */
    Set<Object> measures();
}
