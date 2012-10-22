package org.gridkit.nimble.probe.sigar;

import org.gridkit.nimble.driver.MeteringAware;

public class Sigar {

	public static SigarDriver newDriver() {
		return newDriver(2, 1000);
	}

	public static SigarDriver newDriver(long pollTime) {
		return newDriver(2, pollTime);
	}

	public static SigarDriver newDriver(int poolSize, long pollTime) {
		return new SigarDriver.Impl(poolSize, pollTime);
	}
	
	public static MeteringAware<SigarSamplerFactoryProvider> defaultReporter() {
		return new StandardSigarSamplerFactoryProvider();
	}
	
}
