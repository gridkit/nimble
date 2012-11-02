package org.gridkit.nimble.metering;

import java.util.Map;

public interface SampleTransformer {

	public boolean match(SampleReader sample);
	
	public Map<Object, Object> getOverrides(SampleReader sample);
	
}
