package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.statistics.FrequencySummary;

public interface EventFrequencyExtractor {

	public FrequencySummary extractFrequencySummary(SampleReader reader);
	
}
