package org.gridkit.nimble.driver;

import org.gridkit.nimble.metering.SampleSchema;

public interface MeteringDriver {
	
	public static final String NODE = "node";
	public static final String HOSTNAME = "host";

	public SampleSchema getSchema();
	
	public void setGlobal(Object key, Object value);
	
	public void flush();
	
	public <T extends MeteringAware<?>> MeteringSink<T> bind(T sink);
	
}
