package org.gridkit.nimble.pivot;

import java.io.Serializable;

import org.gridkit.nimble.metering.SampleReader;

public interface SampleExtractor extends Serializable {
	public Object extract(SampleReader sample);
}