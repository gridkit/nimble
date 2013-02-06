package org.gridkit.nimble.npivot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class InMemorySampleSet implements SampleSet {
    private final Collection<? extends Sample> samples;

    public InMemorySampleSet(Collection<? extends Sample> samples) {
        this.samples = samples;
    }

    @Override
    public SampleSet filter(Filter filter) {
        Collection<Sample> result = new ArrayList<Sample>();
        
        for (Sample sample : samples) {
            if (filter.apply(sample)) {
                result.add(sample);
            }
        }
        
        return new InMemorySampleSet(result);
    }

    @Override
    public SampleSet project(Projector projector) {
        Collection<Sample> result = new ArrayList<Sample>(samples.size());
        
        for (Sample sample : samples) {
            SettableSample rSample = new Samples.MapSample();
            projector.project(sample, rSample);
            result.add(rSample);
        }
        
        return new InMemorySampleSet(result);
    }

    @Override
    public SampleCursor newSampleCursor() {
        return new Cursor(samples.iterator());
    }

    @Override
    public long size() {
        return samples.size();
    }
    
    private static class Cursor implements SampleCursor {
        private Sample sample;
        private Iterator<? extends Sample> iter;

        public Cursor(Iterator<? extends Sample> iter) {
            this.iter = iter;
            
            if (iter.hasNext()) {
                sample = iter.next();
            } else {
                sample = null;
            }
        }

        @Override
        public Set<Object> keys() {
            return sample.keys();
        }

        @Override
        public <T> T get(Object key) {
            return sample.get(key);
        }

        @Override
        public boolean isFound() {
            return sample != null;
        }

        @Override
        public boolean next() {
            if (sample == null) {
                throw new IllegalStateException();
            } else {
                if (iter.hasNext()) {
                    sample = iter.next();
                    return true;
                } else {
                    sample = null;
                    return false;
                }
            }
        }
    }
}
