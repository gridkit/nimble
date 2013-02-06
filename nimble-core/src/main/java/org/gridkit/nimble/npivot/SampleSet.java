package org.gridkit.nimble.npivot;

public interface SampleSet {
    SampleSet filter(Filter filter);
    
    SampleSet project(Projector projector);

    SampleCursor newSampleCursor();
    
    long size();
}
