package org.gridkit.nimble.npivot;

public interface Aggregate {
    Query query();

    SampleSet samples();
    
    Aggregate aggregate(Query query);
}
