package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.statistics.FrequencySummary;

public class FrequencyAggregation implements Aggregation<FrequencySummary> {

	private final EventFrequencyExtractor extractor;
	
	private long n = 0;
	private double first = Double.NaN;
	private double last = Double.NaN;
	private double total = 0;		
	
	public FrequencyAggregation(EventFrequencyExtractor extractor) {
		this.extractor = extractor;
	}
	
	@Override
	public void addSamples(SampleReader reader) {
		addAggregate(extractor.extractFrequencySummary(reader));		
	}

	private void add(long n1, double first1, double last1, double total1) {
		if (n == 0) {
			this.n = n1;
			this.first = first1;
			this.last = last1;
			this.total = total1;
		}
		else {
			this.n += n1;
			this.first = Math.min(this.first, first1);
			this.last = Math.max(this.last, last1);
			this.total += total1;
		}
	}

	@Override
	public void addAggregate(FrequencySummary aggregate) {
		if (aggregate.getN() > 0) {
			add(aggregate.getN(), aggregate.getEarliestEventTimestamp(), aggregate.getLatestEventTimestamp(), aggregate.getSum());
		}
	}

	@Override
	public FrequencySummary getResult() {
		return new FrequencySummary.Values(n, total, first, last);
	}		
}
