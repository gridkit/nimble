package org.gridkit.nimble.metering;

import java.util.List;
import java.util.Map;

public interface RawSampleSink {

	public void push(List<Map<Object, Object>> rows);
	
	public void done();
	
}