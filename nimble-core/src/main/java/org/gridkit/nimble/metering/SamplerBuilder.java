package org.gridkit.nimble.metering;

public interface SamplerBuilder {
	
	public static final SampleKey DOMAIN = SamplerAttribute.DOMAIN;
	public static final SampleKey OPERATION = SamplerAttribute.OPERATION;
	public static final SampleKey DESCRIMINATOR = SamplerAttribute.DESCRIMINATOR;
	
	public SamplerBuilder set(Object key, Object value);
	
	public TimeReporter timeReporter(String name);

	public <T extends Enum<T>> DTimeReporter<T> timeReporter(String name, Class<T> descriminator);
	
	public SpanReporter spanReporter(String name);

	public <T extends Enum<T>> DSpanReporter<T> spanReporter(String name, Class<T> descriminator);
	
	public ScalarSampler scalarSampler(String name);

	public PointSampler pointSampler(String name);
	
	public SpanSampler spanSampler(String name);
	
	enum SamplerAttribute implements SampleKey { DOMAIN, OPERATION, DESCRIMINATOR };
	
	/** This object is used internally to filter samples for reporting */
	enum Producer { USER };
}
