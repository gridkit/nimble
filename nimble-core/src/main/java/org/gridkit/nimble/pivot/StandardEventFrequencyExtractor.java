package org.gridkit.nimble.pivot;

import java.io.Serializable;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.statistics.FrequencySummary;

public class StandardEventFrequencyExtractor extends FrequencySummary.Values implements EventFrequencyExtractor, Serializable {

	private final Pivot.Extractor weight;
	
	public StandardEventFrequencyExtractor(Pivot.Extractor weight) {
		super(0, 0, 0, 0);
		this.weight = weight;
	}
	
	@Override
	public FrequencySummary extractFrequencySummary(SampleReader reader) {
		n = 1;
		double w;
		if (weight == null) {
			w = 1;
		}
		else {
			w = asDouble(weight.extract(reader));
		}
		total = w;
		first = asDouble(reader.get(Measure.TIMESTAMP));
		last = first;
		Object s = reader.get(Measure.END_TIMESTAMP);
		if (s != null) {
			last = asDouble(s);
		}
		
		return this;
	}


	private double asDouble(Object object) {
		return ((Number)object).doubleValue();
	}	
}
