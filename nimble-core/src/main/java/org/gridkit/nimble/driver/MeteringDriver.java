package org.gridkit.nimble.driver;

import org.gridkit.nimble.metering.DistributedMetering;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SamplerBuilder;

public interface MeteringDriver {
	
	public static final Object NODE = DistributedMetering.NODENAME;
	public static final Object HOSTNAME = DistributedMetering.HOSTNAME;

	public SampleSchema getSchema();
	
	public void setGlobal(Object key, Object value);
	
	public SamplerBuilder samplerBuilder();
	
	public void flush();
	
	public <S, T extends MeteringAware<S>> MeteringSink<S> bind(T sink);
	
}
