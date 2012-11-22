package org.gridkit.nimble.metering;

public interface SamplerBuilder {
	
	public SamplerBuilder set(Object key, Object value);
	
	public TimeReporter timeReporter(String name);

	public <T extends Enum<T>> DTimeReporter<T> timeReporter(String name, Class<T> descriminator);
	
	public SpanReporter snapReporter(String name);

	public <T extends Enum<T>> DSpanReporter<T> snapReporter(String name, Class<T> descriminator);
	
	public ScalarSampler scalarSampler(String name);
	
	/** This object is used internally to filter samples for reporting */
	enum Producer { USER };
}
