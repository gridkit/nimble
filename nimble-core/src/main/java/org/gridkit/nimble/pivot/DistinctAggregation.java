package org.gridkit.nimble.pivot;

import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.statistics.DistinctSummary;

public class DistinctAggregation implements Aggregation<DistinctSummary> {

	private final SampleExtractor extractor;
	private final Map<Object, Long> counters = new HashMap<Object, Long>();
	
	public DistinctAggregation(SampleExtractor extractor) {
		this.extractor = extractor;
	}

	@Override
	public synchronized void addSamples(SampleReader reader) {
		inc(extractor.extract(reader), 1);
	}

	@Override
	public synchronized void addAggregate(DistinctSummary summary) {
		for(Object v: summary.getDistinctValues()) {
			inc(v, summary.getValueCount(v));
		}
	}

	private void inc(Object v, long valueCount) {
		if (v != null) {
			Long n = counters.get(v);
			counters.put(v, n == null ? valueCount : n + valueCount);
		}
	}

	@Override
	public synchronized DistinctSummary getResult() {
		return new DistinctSummary.Values(counters);
	}
}
