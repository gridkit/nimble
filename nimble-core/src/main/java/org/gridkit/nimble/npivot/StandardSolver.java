package org.gridkit.nimble.npivot;

import java.util.Set;

public class StandardSolver implements Solver {
    @Override
    public Projector project(final Set<Object> preimage, final Set<Object> image) {
        if (!preimage.containsAll(image)) {
            return null;
        }
        
        return new Projector() {
            @Override
            public void project(Sample sample, SettableSample projection) {
                for (Object key : image) {
                    projection.set(key, sample.get(key));
                }
            }
        };
    }

    @Override
    public Filter adapt(Filter original, Filter required, Set<Object> groups) {
        return Filters.constant(true);
    }
}
