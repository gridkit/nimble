package org.gridkit.nimble.btrace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessDetails;
import org.gridkit.lab.jvm.attach.JavaProcessMatcher;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.driver.MeteringSink;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.monitoring.AbstractMonitoringBundle;
import org.gridkit.nimble.monitoring.NoSchema;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Extractors;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.SampleExtractor;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PrintConfig;
import org.gridkit.nimble.probe.JvmMatcherPidProvider;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.probe.ProbeHandle;
import org.gridkit.nimble.probe.common.TargetLocator;
import org.gridkit.nimble.probe.probe.JmxProbes;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BTraceMonitoring extends AbstractMonitoringBundle {

	private static final Logger LOGGER = LoggerFactory.getLogger(BTraceMonitoring.class);
	
	private List<Class<?>> scripts = new ArrayList<Class<?>>();
	
	private TargetLocator<Long> locator;
	@SuppressWarnings("unused")
	private SchemaConfigurer<Long> schemaConfigurer = new NoSchema<Long>();
	
	public BTraceMonitoring(String namespace) {
		super(namespace);
	}

	public void addScript(Class<?> script) {
		scripts.add(script);
	}
	
	public void setLocator(TargetLocator<Long> locator) {
		this.locator = locator;
	}

	public void setLocator(PidProvider provider) {
		this.locator = new PidLocator(provider);
	}

	public void setLocator(JavaProcessMatcher matcher) {
		this.locator = new PidLocator(new JvmMatcherPidProvider(matcher));
	}
	
	public void setSchemaConfig(SchemaConfigurer<Long> config) {
		this.schemaConfigurer = config;
	}

	public void setJmxSchemaConfig(SchemaConfigurer<MBeanServerConnection> config) {
		this.schemaConfigurer = JmxProbes.jmx2pid(config);
	}
	
	@Override
	public String getDescription() {
		return "BTrace profiler";
	}

	@Override
	public void configurePivot(Pivot pivot) {
		Pivot.Level base = pivot.root().level(namespace)
				.filter(BTraceMeasure.SAMPLE_TYPE_KEY, BTraceMeasure.SAMPLE_TYPE_USER);				

		for(Object g: groupping) {
			base = base.group(g);
		}
		
		base = base.group(BTraceMeasure.SCRIPT_KEY);
		base = base.group(BTraceMeasure.STORE_KEY);
		base = base.group(BTraceMeasure.SAMPLE_KEY);
		
		base.level("")
			.calcDistribution(Measure.MEASURE, Extractors.field(Measure.DURATION))
			.calcFrequency(Measure.MEASURE, 1)
			.calcDistribution(Measure.TIMESTAMP);		
	}

	@Override
	public void configurePrinter(PrintConfig printer) {
		printConfig.replay(printer);
		
		DisplayBuilder.with(printer, namespace)
		.value(new SampleExtractor() {
			
			@Override
			public Object extract(SampleReader sample) {
				String cn = (String) sample.get(BTraceMeasure.SCRIPT_KEY);
				if (cn != null) {
					int n = cn.lastIndexOf('.');
					return n < 0 ? cn : cn.substring(n + 1);
				}
				else {
					return null;
				}
			}
		}).caption("Script")
		.attribute(BTraceMeasure.STORE_KEY).caption("Store")
		.attribute(BTraceMeasure.SAMPLE_KEY).caption("Metric")
		.count().caption("Count")
		.distributionStats().caption("%s (ms)").asMillis()
		.frequency().caption("Ops/sec")
		.duration().caption("Duration (s)");
	}

	@Override
	public void deploy(ScenarioBuilder sb, ServiceProvider context, TimeLine timeLine) {
		MeteringDriver metering = context.lookup(MeteringDriver.class);
		BTraceDriver bTrace = context.lookup(BTraceDriver.class);
		MeteringSink<BTraceSamplerFactoryProvider> sink = metering.bind(BTrace.defaultReporter());

		for(Class<?> script: scripts) {
			sb.from(timeLine.getInitCheckpoint());
			ProbeHandle probe = bTrace.trace(new PP(locator), script, sink);
			sb.sleep(10000);
			sb.join(timeLine.getStartCheckpoint());
			
			sb.fromStart();
			probe.join();
			sb.join(timeLine.getDoneCheckpoint());
	
			sb.from(timeLine.getStopCheckpoint());
			probe.stop();
			sb.join(timeLine.getDoneCheckpoint());
		}
	}
	
	private static class PidLocator implements TargetLocator<Long>, Serializable {
		
		private static final long serialVersionUID = 20121116L;
		
		private final PidProvider provider;

		public PidLocator(PidProvider provider) {
			this.provider = provider;
		}

		@Override
		public Collection<Long> findTargets() {
			return provider.getPids();
		}
		
		public String toString() {
			return provider.toString();
		}
	}	

	private static class PP implements PidProvider, Serializable {
		
		private static final long serialVersionUID = 20121116L;
		
		private final TargetLocator<Long> provider;

		private PP(TargetLocator<Long> provider) {
			this.provider = provider;
		}

		@Override
		public Collection<Long> getPids() {
			Collection<Long> pids = provider.findTargets();
			if (pids.isEmpty()) {
				LOGGER.info("No process target for BTrace");				
			}
			else {
				LOGGER.info("Going to deploy BTrace. " + pids);
				for(long pid: pids) {
					JavaProcessDetails pd = AttachManager.getDetails(pid);
					LOGGER.info("BTrace target " + pd.getJavaProcId() + " / jsm-test-role=" + pd.getSystemProperties().get("jsm-test-role"));
				}
			}
			return pids;			
		}
	}	
}
