package org.gridkit.nimble.probe.probe;

import java.io.Serializable;

import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.probe.common.GenericPollProbeManager;
import org.gridkit.nimble.probe.common.PollProbeDeployer;
import org.gridkit.nimble.probe.common.ProbeHandle;
import org.gridkit.nimble.probe.common.SamplerProvider;
import org.gridkit.nimble.probe.common.TargetLocator;

public class Monitoring {

	public static MonitoringDriver deployDriver(ScenarioBuilder sb, MeteringDriver metering) {
		return sb.deploy(newDriver()).newDriver(metering);				
	}

	public static MonitoringDriver deployDriver(String target, ScenarioBuilder sb, MeteringDriver metering) {
		return sb.deploy(target, newDriver()).newDriver(metering);				
	}

	private static DriverFactory newDriver() {
		return new DriverFactoryImpl();
	}
	
	static interface DriverFactory {
		
		MonitoringDriver newDriver(MeteringDriver metering);
						
	}
	
	static class DriverFactoryImpl implements DriverFactory, Serializable {

		private static final long serialVersionUID = 20121106L;

		@Override
		public MonitoringDriver newDriver(MeteringDriver metering) {
			return new GenericPollDriver(metering);
		}
	}
	
	private static class GenericPollDriver implements MonitoringDriver {
		
		private SampleSchema rootSchema;
		private GenericPollProbeManager manager;
		
		public GenericPollDriver(MeteringDriver metering) {
			rootSchema = metering.getSchema();
			rootSchema.freeze();
			manager = new GenericPollProbeManager();
		}

		@Override
		public <T, S> Activity deploy(TargetLocator<T> locator, PollProbeDeployer<T, S> deployer, SchemaConfigurer<T> schConfig, SamplerPrototype<S> sampleProto, long periodMs) {
			
			MetringSamplerProvider<T, S> sp = new MetringSamplerProvider<T, S>(rootSchema, schConfig, sampleProto);
			ProbeHandle handle = manager.deploy(locator, deployer, sp, periodMs);
			
			return new ProbeActivity(handle);
		}
	}
	
	private static class MetringSamplerProvider<T, S> implements SamplerProvider<T, S> {

		private final SampleSchema rootSchema;
		private final SchemaConfigurer<T> targetConfig;
		private final SamplerPrototype<S> proto;

		public MetringSamplerProvider(SampleSchema rootSchema, SchemaConfigurer<T> targetConfig, SamplerPrototype<S> proto) {
			this.rootSchema = rootSchema;
			this.targetConfig = targetConfig;
			this.proto = proto;
		}

		@Override
		public S getSampler(T target) {
			SampleSchema schema = targetConfig.configure(target, rootSchema);
			if (schema == null) {
				return null;
			}
			return proto.instantiate(schema);
		}
	}
	
	private static class ProbeActivity implements Activity {
		
		private final ProbeHandle handle;
		
		public ProbeActivity(ProbeHandle handle) {
			this.handle = handle;
		}

		@Override
		public void stop() {
			handle.stop();
			
		}

		@Override
		public void join() {
			try {
				handle.getStopFuture().get();
			} catch (Exception e) {
				// ignore
			}			
		}
	}	
}
