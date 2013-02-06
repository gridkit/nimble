package org.gridkit.nimble.npivot;

public interface Measure<E, S> {
    /**
     * either Function, Measure, CalculatedMeasure, SampleKey
     */
    Object element();

    S addElement(E value);
    
    S addElement(E value, S summary);
    
    S addSummary(S summary1, S summary2);
}
