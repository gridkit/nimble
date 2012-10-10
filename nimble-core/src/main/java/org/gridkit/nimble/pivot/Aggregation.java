package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.SampleReader;

public interface Aggregation<V> {

	public void addSamples(SampleReader reader);
	
	public void addAggregate(V aggregate);
	
	public V getResult();
	
}
