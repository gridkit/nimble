package org.gridkit.nimble.metering;

public interface SampleSchema {

	public SampleSchema createDerivedScheme();
	
	public SampleFactory createFactory();
	
	public SampleSchema setStatic(Object key, Object value);
	
	public SampleSchema declareDynamic(Object key, Class<?> type);
	
	public void freeze();
	
}
