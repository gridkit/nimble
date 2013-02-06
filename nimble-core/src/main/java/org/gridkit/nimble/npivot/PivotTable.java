package org.gridkit.nimble.npivot;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class PivotTable {    
    private List<Object> columnKeys = Collections.emptyList();
    private List<Object> rowKeys = Collections.emptyList();
    
    public PivotTable onColumns(Object... keys) {
        return null;
    }
    
    public PivotTable onRows(Object... keys) {
        return null;
    }
    
    public PivotTable measure(Object key) {
        return null;
    }
    
    public Table draw(SampleCursor samples) {
        return null;
    }
    
    public static class MeasureSet {
        private final Set<Object> measures;
        
        public MeasureSet(Collection<? extends Object> measures) {
            this.measures = Collections.unmodifiableSet(
                new HashSet<Object>(measures)
            );
        }
        
        public Set<Object> getMeasures() {
            return measures;
        }
        
        public static MeasureSet of(Object... measures) {
            return new MeasureSet(Arrays.asList(measures));
        }
    }
}
