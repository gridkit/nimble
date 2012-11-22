package org.gridkit.nimble.monitoring;

import java.io.Serializable;
import java.util.Collection;
import java.util.EnumSet;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.jvm.attach.JavaProcessMatcher;
import org.gridkit.lab.sigar.SigarFactory;
import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleKey;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleWriter;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PrintConfig;
import org.gridkit.nimble.probe.JvmMatcherPidProvider;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.probe.common.PollProbe;
import org.gridkit.nimble.probe.common.PollProbeDeployer;
import org.gridkit.nimble.probe.common.SamplerProvider;
import org.gridkit.nimble.probe.common.TargetLocator;
import org.gridkit.nimble.probe.probe.JmxProbes;
import org.gridkit.nimble.probe.probe.MonitoringDriver;
import org.gridkit.nimble.probe.probe.SamplerPrototype;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;
import org.gridkit.nimble.util.Seconds;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class ProcessCpuMonitoring extends AbstractMonitoringBundle {

	public static final SigarCpuMetric CPU_TOTAL = SigarCpuMetric.CPU_TOTAL;
	public static final SigarCpuMetric CPU_SYS = SigarCpuMetric.CPU_SYS;
	public static final SigarCpuMetric CPU_USER = SigarCpuMetric.CPU_USER;
	
	
	public enum SigarCpuMetric implements SampleKey, ProcCpuAttr {
		CPU_TOTAL("CPU (total)") {
			@Override
			public double extract(ProcCpu prev, ProcCpu last) {
				return Seconds.fromMillis(last.getTotal() - prev.getTotal());
			}
		},
		CPU_SYS("CPU (sys)") {
			@Override
			public double extract(ProcCpu prev, ProcCpu last) {
				return Seconds.fromMillis(last.getSys() - prev.getSys());
			}
		},
		CPU_USER("CPU (user)") {
			@Override
			public double extract(ProcCpu prev, ProcCpu last) {
				return Seconds.fromMillis(last.getUser() - prev.getUser());
			}
		}
		;
		private final String caption;

		private SigarCpuMetric(String caption) {
			this.caption = caption;
		}
		
		public String getCaption() {
			return caption;
		}		
	}
	
	private enum PidKey implements SampleKey {
		PID
	}
	
	private TargetLocator<Long> locator;
	private SchemaConfigurer<Long> schemaConfigurer = new NoSchema<Long>();
	private EnumSet<SigarCpuMetric> metrics = EnumSet.of(CPU_SYS);
	private long pollPeriod = 1000;
	
	public ProcessCpuMonitoring(String namespace) {
		super(namespace);
	}
	
	@Override
	public String getDescription() {
		return "Process CPU utilization";
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
	public void configurePivot(Pivot pivot) {
		Pivot.Level base = pivot.root().level(namespace)
				.filter(Measure.PRODUCER, getProducerId());
		for(Object g: groupping) {
			base = base.group(g);
		}
		
		Pivot.Level b = base.level("");
		b.calcFrequency(Measure.MEASURE);
		b.calcDistinct(PidKey.PID);
		
		for(SigarCpuMetric m: metrics) {
			b.calcFrequency(m);
		}
		
		Pivot.Level p = b.pivot("pid").group(PidKey.PID);
		p.calcFrequency(Measure.MEASURE);
		
		for(SigarCpuMetric m: metrics) {
			p.calcFrequency(m);
		}
	}

	@Override
	public void configurePrinter(PrintConfig printer) {
		printConfig.replay(printer);
		
		DisplayBuilder.with(printer, namespace)
			.constant("Metric", "CPU usage [100% = core]")
			.frequency().caption("All, CPU (total) [%%]").asPercent()
			.decorated("pid").calc().min().frequency().caption("Min. CPU per process [%%]").asPercent()
			.decorated("pid").calc().mean().frequency().caption("Avg. CPU per process [%%]").asPercent()
			.decorated("pid").calc().max().frequency().caption("Max. CPU per process [%%]").asPercent();

		for(SigarCpuMetric m: metrics) {
			if (m != CPU_TOTAL) {
				DisplayBuilder.with(printer, namespace)
					.frequency(m).caption("All, " + m.getCaption() + "[%%]").asPercent();
			}
		}
		
		DisplayBuilder.with(printer, namespace)
			.distinct(PidKey.PID).caption("Processes [N]")
			.duration().caption("Observed [Sec]");		
	}

	@Override
	public void deploy(ScenarioBuilder sb, ServiceProvider context, TimeLine timeLine) {
		MonitoringDriver driver = context.lookup(MonitoringDriver.class);
		
		sb.from(timeLine.getInitCheckpoint());
		Activity probe = driver.deploy(locator, new SigarProcMonDeployer(), createSchemaConfig(), createSampler(), pollPeriod);
		sb.join(timeLine.getStartCheckpoint());
		
		sb.fromStart();
		probe.join();
		sb.join(timeLine.getDoneCheckpoint());

		sb.from(timeLine.getStopCheckpoint());
		probe.stop();
		sb.join(timeLine.getDoneCheckpoint());
	}

	private SchemaConfigurer<Long> createSchemaConfig() {
		return new SchemaEnricher(schemaConfigurer);
	}
	
	private SamplerPrototype<ProcCpuSampler> createSampler() {
		return new SigarSamplerProto(getProducerId(), metrics);
	}

	private static final class SchemaEnricher implements SchemaConfigurer<Long>, Serializable {

		private static final long serialVersionUID = 20121116L;
		
		private final SchemaConfigurer<Long> nested;
		
		public SchemaEnricher(SchemaConfigurer<Long> nested) {
			this.nested = nested;
		}

		@Override
		public SampleSchema configure(Long target, SampleSchema root) {
			SampleSchema ss = root.createDerivedScheme();
			ss.setStatic(PidKey.PID, target);
			return nested.configure(target, ss);
		}
	}
	
	private static final class SigarSamplerProto implements SamplerPrototype<ProcCpuSampler>, Serializable {

		private static final long serialVersionUID = 20121116L;
		
		private final Object producerId;
		private final EnumSet<SigarCpuMetric> metrics;

		public SigarSamplerProto(Object producerId, EnumSet<SigarCpuMetric> metrics) {
			this.producerId = producerId;
			this.metrics = metrics;
		}

		@Override
		public ProcCpuSampler instantiate(SampleSchema schema) {
			SampleSchema root = schema.createDerivedScheme();
			root.setStatic(Measure.PRODUCER, producerId);

			root.declareDynamic(Measure.TIMESTAMP, double.class);
			root.declareDynamic(Measure.DURATION, double.class);
			// Total CPU
			root.declareDynamic(Measure.MEASURE, double.class);
			
			for(SigarCpuMetric m: metrics) {
				root.declareDynamic(m, double.class);
			}
			
			final SampleFactory factory = root.createFactory();
			
			return new ProcCpuSampler() {

				@Override
				public void report(long pid, long prevTimestamp, ProcCpu prev, long lastTimestamp, ProcCpu last) {					// TODO Auto-generated method stub
					if (prev != null) {
						SampleWriter sw = factory.newSample()
							.setTimeBounds(prevTimestamp, lastTimestamp)
							.setMeasure(CPU_TOTAL.extract(prev, last));
						for(SigarCpuMetric m: metrics) {
							sw.set(m, m.extract(prev, last));
						}
						sw.submit();
					}
				}
			};
		}
	}
	
	private static final class SigarProcMonDeployer implements PollProbeDeployer<Long, ProcCpuSampler>, Serializable {

		private static final long serialVersionUID = 20121116;
		
		private Sigar sigar;
		
		public Sigar getSigar() {
			if (sigar == null) {
				sigar = SigarFactory.newSigar();
			}
			return sigar;
		}
		
		@Override
		public PollProbe deploy(Long target, SamplerProvider<Long, ProcCpuSampler> provider) {

			ProcCpuSampler s = provider.getSampler(target);
			
			
			if (s == null) {
				return null;
			}
			
			return new ProcCpuPollProbe(target, s, getSigar());
		}

		@Override
		public String toString() {
			return "SigarProcCpu";
		}
	}
		
	private final static class ProcCpuPollProbe implements PollProbe {
		
		private final long pid;
		private final ProcCpuSampler sampler;
		private final Sigar sigar;
		private long prevTimestamp = 0;
		private ProcCpu prevInfo;
	
		private ProcCpuPollProbe(long pid, ProcCpuSampler s, Sigar sigar) {
			this.sampler = s;
			this.pid = pid;
			this.sigar = sigar;
		}
	
		@Override
		public void poll() {
			long lastTimestamp = System.nanoTime();
			ProcCpu lastInfo;
			try {
				lastInfo = sigar.getProcCpu(pid);
			} catch (SigarException e) {
				// TODO logging
				return;
			}
			sampler.report(pid, prevTimestamp, prevInfo, lastTimestamp, lastInfo);
			prevTimestamp = lastTimestamp;
			prevInfo = lastInfo;
		}
	
		@Override
		public void stop() {
			// nothing to do
		}
	}

	private static interface ProcCpuSampler {
	
		public void report(long pid, long prevTimestamp, ProcCpu prev, long lastTimestamp, ProcCpu last);
		
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
	
	private interface ProcCpuAttr {
		
		public double extract(ProcCpu prev, ProcCpu last);
		
	}
}
