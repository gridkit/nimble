package org.gridkit.nimble.pivot;

import java.io.Serializable;

import org.gridkit.nimble.metering.SampleReader;

public interface SampleFilter extends Serializable {
	public boolean match(SampleReader sample);
}