package org.gridkit.nimble.npivot;

public class InMemoryAggregateBuilderFactory implements AggregateBuilderFactory {
    @Override
    public AggregateBuilder newAggregateBuilder() {
        return new InMemoryAggregateBuilder();
    }
}
