package org.gridkit.nimble.metering;

public interface SamplerBuilder {
	
	public SamplerBuilder set(Object key, Object value);
	
	public TimeReporter timeReporter(String name);
	
	public SpanReporter snapReporter(String name);
	
	public ScalarSampler scalarSampler(String name);

}
