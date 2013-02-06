package org.gridkit.nimble.npivot;

import java.util.EnumSet;

import org.gridkit.nimble.print.PrettyPrinter;

public class StandardQueryProcessorTest {
    public static enum Key {
        A, B, TIMESTAMP, DURATION
    }
    
    public static void main(String[] args) {
        PrettyPrinter printer = new PrettyPrinter();
        
        Aggregate base = newAggregate();
        
        Query query1 = new QueryBuilder().groups(Key.A, Key.B)
                                         .filter(Filters.constant(true))
                                         .measures(
                                             M.max(Key.TIMESTAMP),
                                             M.min(Key.TIMESTAMP),
                                             M.max(Key.DURATION),
                                             M.min(Key.DURATION)
                                         ).build();

        Aggregate aggr1 = base.aggregate(query1);
        
        printer.print(System.err, new SampleSetPrinter(aggr1));
        
        System.err.print("\n---\n");
        
        Query query2 = new QueryBuilder().groups()
                                         .filter(Filters.constant(true))
                                         .measures(
                                             M.mean(M.calculate(M.max(Key.DURATION), Key.A)),
                                             M.max(Key.TIMESTAMP),
                                             M.min(Key.TIMESTAMP)
                                         ).build();
        
        Aggregate aggr2 = aggr1.aggregate(query2);
        
        printer.print(System.err, new SampleSetPrinter(aggr2));
    }
    
    public static Aggregate newAggregate() {
        AggregateBuilderFactory factory = new InMemoryAggregateBuilderFactory();
        
        AggregateBuilder builder = factory.newAggregateBuilder();
        
        sample("a1", "b1", 0, 1, builder);
        sample("a2", "b2", 1, 2, builder);
        sample("a3", "b3", 2, 4, builder);
        
        sample("a1", "b11", 1, 0, builder);
        sample("a2", "b22", 3, 8, builder);
        sample("a3", "b33", 5, 16, builder);
        
        Query query = new QueryBuilder().groups(EnumSet.allOf(Key.class))
                                        .filter(Filters.constant(true))
                                        .measures()
                                        .build();
        
        Solver solver = new StandardSolver();
        
        QueryProcessor processor = new StandardQueryProcessor(solver, factory);
        
        return new StandardAggregate(query, builder.build(), processor);
    }
    
    public static void sample(String a, String b, long ts, long dur, AggregateBuilder builder) {
        SettableSample sample = new Samples.MapSample();
        
        sample.set(Key.A, a);
        sample.set(Key.B, b);
        sample.set(Key.TIMESTAMP, ts);
        sample.set(Key.DURATION, dur);
        
        builder.getAggregateSample(sample);
    }
    
}
