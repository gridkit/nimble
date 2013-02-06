package org.gridkit.nimble.npivot;

public interface AggregateBuilder {
    SettableSample getAggregateSample(Sample gSample);
        
    SampleSet build();
}
