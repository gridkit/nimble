package org.gridkit.nimble.metering;

import java.util.List;

public interface SampleReader {

	public boolean next();
	
	public List<Object> keySet();
	
	public Object get(Object key);
	
}
