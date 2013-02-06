package org.gridkit.nimble.npivot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gridkit.nimble.npivot.Samples.MapSample;
import org.gridkit.nimble.npivot.Samples.SubsetSample;

public class StandardQueryProcessor implements QueryProcessor {
    private final Solver solver;
    private final AggregateBuilderFactory builderFactory;

    public StandardQueryProcessor(Solver solver, AggregateBuilderFactory builderFactory) {
        this.solver = solver;
        this.builderFactory = builderFactory;
    }

    @Override
    public Aggregate aggregate(Aggregate aggr, Query query) {
        Filter filter = solver.adapt(aggr.query().filter(), query.filter(), aggr.query().groups());
        
        if (filter == null) {
            throw new IllegalArgumentException();
        }
        
        Query fQuery = new QueryBuilder().filter(filter)
                                         .groups(query.groups())
                                         .build();
        
        Map<Object, Aggregate> aggrs = new HashMap<Object, Aggregate>();
        
        for (Object measure : query.measures()) {
            Aggregate mAggr;
            
            if (measure instanceof Measure) {
                mAggr = aggregate(aggr, fQuery, (Measure<?, ?>)measure);
            } else if (measure instanceof CalculatedMeasure) {
                mAggr = aggregate(aggr, fQuery, (CalculatedMeasure)measure);
            } else {
                throw new IllegalArgumentException();
            }
            
            aggrs.put(measure, mAggr);
        }
        
        AggregateBuilder builder = builderFactory.newAggregateBuilder();
        
        for (Map.Entry<Object, Aggregate> entry : aggrs.entrySet()) {
            Object mKey = entry.getKey();
            Aggregate mAggr = entry.getValue();
            
            SampleCursor cursor = mAggr.samples().newSampleCursor();
            
            while (cursor.isFound()) {                
                Sample gSample = new SubsetSample(cursor, query.groups());
                
                SettableSample mSample = builder.getAggregateSample(gSample);

                mSample.set(mKey, cursor.get(mKey));
                
                cursor.next();
            }
        }

        return new StandardAggregate(query, builder.build(), this);
    }
    
    private Aggregate aggregate(Aggregate aggr, Query query, Measure<?, ?> rawMeasure) {
        @SuppressWarnings("unchecked")
        Measure<Object, Object> measure = (Measure<Object, Object>)rawMeasure;

        Object element = measure.element();
                
        if (aggr.query().groups().contains(element)) {
            return aggregateElements(aggr, query, measure);
        } else if (aggr.query().measures().contains(measure)) {
            return aggregateMeasures(aggr, query, measure);
        } else if (Measures.isMeasure(element)) {
            Set<Object> groups = new HashSet<Object>();
            
            groups.addAll(query.groups());
            if (element instanceof CalculatedMeasure) {
                groups.addAll(((CalculatedMeasure)element).groups());
            }
            
            Query mQuery = new QueryBuilder().filter(query.filter())
                                             .groups(groups)
                                             .measures(element)
                                             .build();
            
            aggr = aggregate(aggr, mQuery);
            
            return aggregateElements(aggr, query, measure);
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    private Aggregate aggregateElements(Aggregate aggr, Query query, Measure<Object, Object> measure) {
        Projector projector = solver.project(aggr.query().groups(), query.groups());
        
        if (projector == null) {
            throw new IllegalArgumentException();
        }
        
        Object element = measure.element();
        
        AggregateBuilder builder = builderFactory.newAggregateBuilder();
        
        SampleCursor cursor = aggr.samples().newSampleCursor();
        
        while (cursor.isFound()) {
            if (query.filter().apply(cursor)) {
                Object elementVal = cursor.get(element);
                
                SettableSample gSample = new MapSample();
                projector.project(cursor, gSample);
                
                SettableSample gAggr = builder.getAggregateSample(gSample);
                
                Object summaryVal;
                
                if (gAggr.keys().contains(measure)) {
                    summaryVal = gAggr.get(measure);
                    summaryVal = measure.addElement(elementVal, summaryVal);
                } else {
                    summaryVal = measure.addElement(elementVal);
                }
                
                gAggr.set(measure, summaryVal);
            }
            cursor.next();
        }
        
        Query mQuery = new QueryBuilder().filter(query.filter())
                                         .groups(query.groups())
                                         .measures(measure)
                                         .build();
        
        return new StandardAggregate(mQuery, builder.build(), this);
    }
    
    private Aggregate aggregateMeasures(Aggregate aggr, Query query, Measure<Object, Object> measure) {
        Projector projector = solver.project(aggr.query().groups(), query.groups());
        
        if (projector == null) {
            throw new IllegalArgumentException();
        }
                
        AggregateBuilder builder = builderFactory.newAggregateBuilder();
        
        SampleCursor cursor = aggr.samples().newSampleCursor();
        
        while (cursor.isFound()) {
            if (query.filter().apply(cursor)) {
                Object cSummaryVal = cursor.get(measure);
                
                SettableSample gSample = new MapSample();
                projector.project(cursor, gSample);
                
                SettableSample gAggr = builder.getAggregateSample(gSample);
                
                Object summaryVal;
                
                if (gAggr.keys().contains(measure)) {
                    Object gSummaryVal = gAggr.get(measure);
                    summaryVal = measure.addSummary(cSummaryVal, gSummaryVal);
                } else {
                    summaryVal = cSummaryVal;
                }
                
                gAggr.set(measure, summaryVal);
            }
            cursor.next();
        }
        
        Query mQuery = new QueryBuilder().filter(query.filter())
                                         .groups(query.groups())
                                         .measures(measure)
                                         .build();
        
        return new StandardAggregate(mQuery, builder.build(), this);
    }
    
    private Aggregate aggregate(Aggregate aggr, Query query, CalculatedMeasure measure) {
        Projector projector = solver.project(query.groups(), measure.groups());
        
        if (projector == null) {
            throw new UnsupportedOperationException();
        }
        
        Query mQuery = new QueryBuilder().filter(query.filter())
                                         .groups(query.groups())
                                         .measures(measure.measures())
                                         .build();
        
        aggr = aggregate(aggr, mQuery);
        
        projector = new CalculatedMeasureProjector(measure, projector);
        
        SampleSet samples = aggr.samples().project(projector);

        return new StandardAggregate(mQuery, samples, this);
    }
  
    private static class CalculatedMeasureProjector implements Projector {
        private final CalculatedMeasure measure;
        private final Projector projector;

        public CalculatedMeasureProjector(CalculatedMeasure measure, Projector projector) {
            this.measure = measure;
            this.projector = projector;
        }

        @Override
        public void project(Sample sample, SettableSample projection) {
            SettableSample mSample = new MapSample();
            
            projector.project(sample, mSample);
            
            for (Object m : measure.measures()) {
                mSample.set(m, sample.get(m));
            }
            
            Object value = measure.calculate(mSample);
            
            for (Object key : sample.keys()) {
                if (!measure.measures().contains(key)) {
                    projection.set(key, sample.get(key));
                }
            }
            
            projection.set(measure, value);
        }
    }
}
