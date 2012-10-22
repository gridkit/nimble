package org.gridkit.nimble.driver;

import org.gridkit.nimble.metering.DisrtibutedMetering;
import org.gridkit.nimble.metering.SampleSchema;

public interface MeteringDriver {
	
	public static final Object NODE = DisrtibutedMetering.NODENAME;
	public static final Object HOSTNAME = DisrtibutedMetering.HOSTNAME;

	public SampleSchema getSchema();
	
	public void setGlobal(Object key, Object value);
	
	public void flush();
	
	public <S, T extends MeteringAware<S>> MeteringSink<S> bind(T sink);
	
}
