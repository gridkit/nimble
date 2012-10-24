package org.gridkit.nimble.pivot.display;

import java.util.Map;

import org.gridkit.nimble.metering.SampleReader;

public interface DisplayComponent {
	
	public Map<String, Object> display(SampleReader reader);
	
}