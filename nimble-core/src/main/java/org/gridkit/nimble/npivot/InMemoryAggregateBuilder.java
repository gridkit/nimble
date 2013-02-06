package org.gridkit.nimble.npivot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.gridkit.nimble.util.Pair;

public class InMemoryAggregateBuilder implements AggregateBuilder {
    private long nextId = 0;
    
    private Map<Term, Set<Long>> index = new HashMap<Term, Set<Long>>();
    
    private Map<Long, SettableSample> samples = new HashMap<Long, SettableSample>();
    private SettableSample emptySample;
    
    @Override
    public SettableSample getAggregateSample(Sample gSample) {
        if (gSample.keys().isEmpty()) {
            return getEmptySample();
        }
        
        Set<Long> candidates = new HashSet<Long>();
        
        Iterator<Object> iter = gSample.keys().iterator();
        
        if (iter.hasNext()) {
            Object key = iter.next();
            candidates.addAll(aggregates(key, gSample.get(key)));
        }
        
        while (iter.hasNext() && !candidates.isEmpty()) {
            Object key = iter.next();
            candidates.retainAll(aggregates(key, gSample.get(key)));
        }
        
        if (candidates.isEmpty()) {
            return newAggregateSample(gSample); 
        } else if (candidates.size() == 1) {
            long id = candidates.iterator().next();
            return samples.get(id);
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    public SettableSample newAggregateSample(Sample gSample) {
        long id = ++nextId;
        
        Map<Object, Object> mResult = new HashMap<Object, Object>();
                
        for (Object key : gSample.keys()) {
            Object value = gSample.get(key);
            aggregates(key, value).add(id);
            mResult.put(key, value);
        }
        
        SettableSample result = new Samples.MapSample(mResult); 
        samples.put(id, result);
        
        return result;
    }
    
    public SettableSample getEmptySample() {
        if (emptySample == null) {
            emptySample = new Samples.MapSample();
            samples.put(++nextId, emptySample);
        }
        
        return emptySample;
    }
    
    private Set<Long> aggregates(Object key, Object value) {
        Term term = new Term(key, value);
        
        Set<Long> termSet = index.get(term);
        
        if (termSet == null) {
            termSet = new HashSet<Long>();
            index.put(term, termSet);
        }
        
        return termSet;
    }
    
    @Override
    public SampleSet build() {
        Collection<Sample> samples = new ArrayList<Sample>(this.samples.size());
        samples.addAll(this.samples.values());
        return new InMemorySampleSet(samples);
    }

    @SuppressWarnings("serial")
    private static class Term extends Pair<Object, Object> {
        public Term(Object key, Object value) {
            super(key, value);
        }
    }
}
