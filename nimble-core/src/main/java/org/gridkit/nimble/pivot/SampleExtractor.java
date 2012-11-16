package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.SampleReader;

public interface SampleExtractor {
	public Object extract(SampleReader sample);
}