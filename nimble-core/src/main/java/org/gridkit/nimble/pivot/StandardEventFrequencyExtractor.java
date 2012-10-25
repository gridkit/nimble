package org.gridkit.nimble.pivot;

import java.io.Serializable;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.statistics.FrequencySummary;

public class StandardEventFrequencyExtractor extends FrequencySummary.Values implements EventFrequencyExtractor, Serializable {

	private static final long serialVersionUID = 20121025L;
	
	private final SampleExtractor weight;
	
	public StandardEventFrequencyExtractor(SampleExtractor weight) {
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
			Object val = weight.extract(reader);
			w = val == null ? 1 : asDouble(val);
		}
		total = w;
		try {
			first = asDouble(reader.get(Measure.TIMESTAMP));
			last = first;
			Object s = reader.get(Measure.END_TIMESTAMP);
			if (s != null) {
				last = asDouble(s);
			}
		}
		catch(NullPointerException e) {
			throw new IllegalArgumentException("Cannot process sample " + reader.keySet() + " missing time bounds");
		}
		
		return this;
	}


	private double asDouble(Object object) {
		return ((Number)object).doubleValue();
	}	
}
