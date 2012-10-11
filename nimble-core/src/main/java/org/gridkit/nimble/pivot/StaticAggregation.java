package org.gridkit.nimble.pivot;

import java.io.Serializable;

import org.gridkit.nimble.metering.SampleReader;

public class StaticAggregation<V> implements Aggregation<V>, Serializable {

	private final V value;
	
	public StaticAggregation(V value) {
		this.value = value;
	}

	@Override
	public void addSamples(SampleReader reader) {
		// do nothing
	}

	@Override
	public void addAggregate(V aggregate) {
		// do nothing
	}

	@Override
	public V getResult() {
		return value;
	}
}
