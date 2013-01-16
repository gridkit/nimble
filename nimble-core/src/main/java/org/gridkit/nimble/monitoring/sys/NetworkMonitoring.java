package org.gridkit.nimble.monitoring.sys;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

import org.gridkit.lab.monitoring.probe.PollProbe;
import org.gridkit.lab.monitoring.probe.PollProbeDeployer;
import org.gridkit.lab.monitoring.probe.SamplerProvider;
import org.gridkit.lab.monitoring.probe.TargetLocator;
import org.gridkit.lab.sigar.SigarFactory;
import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleKey;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleWriter;
import org.gridkit.nimble.monitoring.AbstractMonitoringBundle;
import org.gridkit.nimble.monitoring.NoSchema;
import org.gridkit.nimble.monitoring.PollingBundle;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PrintConfig;
import org.gridkit.nimble.probe.probe.MonitoringDriver;
import org.gridkit.nimble.probe.probe.SamplerPrototype;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkMonitoring extends AbstractMonitoringBundle implements PollingBundle {

	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkMonitoring.class);
	
	public final static SigarNetMetric RX_BYTES = SigarNetMetric.RX_BYTES;
	public final static SigarNetMetric TX_BYTES = SigarNetMetric.TX_BYTES;
	public final static SigarNetMetric RX_PACKETS = SigarNetMetric.RX_PACKETS;
	public final static SigarNetMetric TX_PACKETS = SigarNetMetric.TX_PACKETS;
	public final static SigarNetMetric RX_DROPPED = SigarNetMetric.RX_DROPPED;
	public final static SigarNetMetric TX_DROPPED = SigarNetMetric.TX_DROPPED;
	public final static SigarNetMetric RX_ERRORS = SigarNetMetric.RX_ERRORS;
	public final static SigarNetMetric TX_ERRORS = SigarNetMetric.TX_ERRORS;
	
	public enum SigarNetMetric implements SampleKey, NetAttr {
		RX_BYTES("RX") {
			@Override
			public double extract(NetInterfaceStat prev, NetInterfaceStat last) {
				return last.getRxBytes() - prev.getRxBytes();
			}
		},
		TX_BYTES("TX") {
			@Override
			public double extract(NetInterfaceStat prev, NetInterfaceStat last) {
				return last.getRxBytes() - prev.getRxBytes();
			}
		},
		RX_PACKETS("RX Packets") {
			@Override
			public double extract(NetInterfaceStat prev, NetInterfaceStat last) {
				return last.getRxPackets() - prev.getRxPackets();
			}
		},
		TX_PACKETS("TX Packets") {
			@Override
			public double extract(NetInterfaceStat prev, NetInterfaceStat last) {
				return last.getTxPackets() - prev.getTxPackets();
			}
		},
		RX_DROPPED("Dropped (RX)") {
			@Override
			public double extract(NetInterfaceStat prev, NetInterfaceStat last) {
				return last.getRxDropped() - prev.getRxDropped();
			}
		},
		TX_DROPPED("Dropped (TX)") {
			@Override
			public double extract(NetInterfaceStat prev, NetInterfaceStat last) {
				return last.getTxDropped() - prev.getTxDropped();
			}
		},
		RX_ERRORS("Errors (RX)") {
			@Override
			public double extract(NetInterfaceStat prev, NetInterfaceStat last) {
				return last.getRxErrors() - prev.getRxErrors();
			}
		},
		TX_ERRORS("Errors (TX)") {
			@Override
			public double extract(NetInterfaceStat prev, NetInterfaceStat last) {
				return last.getTxErrors() - prev.getTxErrors();
			}
		},
		;
		private final String caption;

		private SigarNetMetric(String caption) {
			this.caption = caption;
		}
		
		public String getCaption() {
			return caption;
		}		
	}
	
	private enum IfName implements SampleKey {
		INTERFACE
	}
	
	private TargetLocator<String> locator = new NetInterfacesLocator(null);
	private SchemaConfigurer<String> schemaConfigurer = new NoSchema<String>();
	private EnumSet<SigarNetMetric> metrics = EnumSet.allOf(SigarNetMetric.class);
	private long pollPeriod = 1000;
	
	public NetworkMonitoring(String namespace) {
		super(namespace);
	}
	
	@Override
	public String getDescription() {
		return "Network utilization";
	}

	@Override
	public void setPollPeriod(long periodMs) {
		pollPeriod = periodMs;
	}

	public void filterInterfaces(String pattern) {
		this.locator = new NetInterfacesLocator(Pattern.compile(pattern));
	}
	
	public void setSchemaConfig(SchemaConfigurer<String> config) {
		this.schemaConfigurer = config;
	}

	
	@Override
	public void configurePivot(Pivot pivot) {
		Pivot.Level base = pivot.root().level(namespace)
				.filter(Measure.PRODUCER, getProducerId());
		for(Object g: groupping) {
			base = base.group(g);
		}
		
		Pivot.Level b = base.level("all");
		b.calcDistinct(IfName.INTERFACE);
		
		for(SigarNetMetric m: metrics) {
			b.calcFrequency(m);
		}
		
		Pivot.Level p = base.level("if").group(IfName.INTERFACE).level("");
		
		for(SigarNetMetric m: metrics) {
			p.calcFrequency(m);
		}
	}

	@Override
	public void configurePrinter(PrintConfig printer) {
		printConfig.replay(printer);
		printer.sortByField(IfName.INTERFACE);
		
		DisplayBuilder all = DisplayBuilder.with(printer, namespace + ".all")
			.constant("Interface", "(all)");
		
		configureNetStatsDisplay(all);		

		DisplayBuilder perIf = DisplayBuilder.with(printer, namespace + ".if")
			.attribute("Interface", IfName.INTERFACE);

		configureNetStatsDisplay(perIf);		
	}

	protected void configureNetStatsDisplay(DisplayBuilder db) {
		db
		.frequency(RX_BYTES).caption("Rx Rate (MiB/S)").asMiB()
		.frequency(TX_BYTES).caption("Tx Rate (MiB/S)").asMiB()
		.sum(RX_BYTES).caption("Rx Total (MiB)").asMiB()
		.sum(TX_BYTES).caption("Tx Total (MiB)").asMiB()
		.sum(RX_DROPPED).caption("Rx Dropped")
		.sum(TX_DROPPED).caption("Tx Dropped")
		.sum(RX_ERRORS).caption("Rx Errors")
		.sum(TX_ERRORS).caption("Tx Errors")
		.duration().caption("Observed [Sec]");
	}

	@Override
	public void deploy(ScenarioBuilder sb, ServiceProvider context, TimeLine timeLine) {
		MonitoringDriver driver = context.lookup(MonitoringDriver.class);
		
		sb.from(timeLine.getInitCheckpoint());
		Activity probe = driver.deploy(locator, new SigarNetMonDeployer(), createSchemaConfig(), createSampler(), pollPeriod);
		sb.join(timeLine.getStartCheckpoint());
		
		sb.fromStart();
		probe.join();
		sb.join(timeLine.getDoneCheckpoint());

		sb.from(timeLine.getStopCheckpoint());
		probe.stop();
		sb.join(timeLine.getDoneCheckpoint());
	}

	private SchemaConfigurer<String> createSchemaConfig() {
		return new SchemaEnricher(schemaConfigurer);
	}
	
	private SamplerPrototype<NetStatSampler> createSampler() {
		return new SigarSamplerProto(getProducerId(), metrics);
	}

	private static final class SchemaEnricher implements SchemaConfigurer<String>, Serializable {

		private static final long serialVersionUID = 20121116L;
		
		private final SchemaConfigurer<String> nested;
		
		public SchemaEnricher(SchemaConfigurer<String> nested) {
			this.nested = nested;
		}

		@Override
		public SampleSchema configure(String target, SampleSchema root) {
			SampleSchema ss = root.createDerivedScheme();
			ss.setStatic(IfName.INTERFACE, target);
			return nested.configure(target, ss);
		}
	}
	
	private static final class SigarSamplerProto implements SamplerPrototype<NetStatSampler>, Serializable {

		private static final long serialVersionUID = 20121116L;
		
		private final Object producerId;
		private final EnumSet<SigarNetMetric> metrics;

		public SigarSamplerProto(Object producerId, EnumSet<SigarNetMetric> metrics) {
			this.producerId = producerId;
			this.metrics = metrics;
		}

		@Override
		public NetStatSampler instantiate(SampleSchema schema) {
			SampleSchema root = schema.createDerivedScheme();
			root.setStatic(Measure.PRODUCER, producerId);

			root.declareDynamic(Measure.TIMESTAMP, double.class);
			root.declareDynamic(Measure.DURATION, double.class);
			
			for(SigarNetMetric m: metrics) {
				root.declareDynamic(m, double.class);
			}
			
			final SampleFactory factory = root.createFactory();
			
			return new NetStatSampler() {
				
				@Override
				public void report(String intf, long prevTimestamp, NetInterfaceStat prev, long lastTimestamp, NetInterfaceStat last) {
					if (prev != null) {
						SampleWriter sw = factory.newSample()
							.setTimeBounds(prevTimestamp, lastTimestamp);
						for(SigarNetMetric m: metrics) {
							sw.set(m, m.extract(prev, last));
						}
						sw.submit();
					}
				}
			};
		}
	}
	
	private static final class SigarNetMonDeployer implements PollProbeDeployer<String, NetStatSampler>, Serializable {

		private static final long serialVersionUID = 20121116;
		
		private SigarProxy sigar;
		
		public SigarProxy getSigar() {
			if (sigar == null) {
				sigar = SigarFactory.newSigar();
			}
			return sigar;
		}
		
		@Override
		public PollProbe deploy(String target, SamplerProvider<String, NetStatSampler> provider) {

			NetStatSampler s = provider.getSampler(target);
			
			
			if (s == null) {
				return null;
			}
			
			return new NetStatPollProbe(target, s, getSigar());
		}
	}
		
	private final static class NetStatPollProbe implements PollProbe {
		
		private final String intf;
		private final NetStatSampler sampler;
		private final SigarProxy sigar;
		private long prevTimestamp = 0;
		private NetInterfaceStat prevInfo;
		private boolean showError = true;
	
		private NetStatPollProbe(String intf, NetStatSampler s, SigarProxy sigar) {
			this.sampler = s;
			this.intf = intf;
			this.sigar = sigar;
		}
	
		@Override
		public void poll() {
			long lastTimestamp = System.nanoTime();
			NetInterfaceStat lastInfo;
			try {
				lastInfo = sigar.getNetInterfaceStat(intf);
			} catch (SigarException e) {
				if (showError) {
					LOGGER.warn("Probe exception: " + e.toString());
					showError = false;
				}
				return;
			}
			sampler.report(intf, prevTimestamp, prevInfo, lastTimestamp, lastInfo);
			prevTimestamp = lastTimestamp;
			prevInfo = lastInfo;
			showError = true;
		}
	
		@Override
		public void stop() {
			// nothing to do
		}
	}

	private static class NetInterfacesLocator implements TargetLocator<String>, Serializable {

		private static final long serialVersionUID = 20121115L;
		
		private final Pattern filter;
		
		private NetInterfacesLocator(Pattern filter) {
			this.filter = filter;
		}

		@Override
		public Collection<String> findTargets() {
			try {
			    SigarProxy s = SigarFactory.newSigar();
				String[] netIfs = s.getNetInterfaceList();
				LOGGER.info("Network interfaces found: " + Arrays.toString(netIfs));
				for(int i = 0; i != netIfs.length; ++i) {
					int n = netIfs[i].indexOf(':');
					if (n > 0) {
						netIfs[i] = netIfs[i].substring(0, n);
					}
				}
				if (filter == null) {
					return Arrays.asList(netIfs);
				}
				else {
					List<String> result = new ArrayList<String>();
					for(String i: netIfs) {
						if (filter.matcher(i).matches()) {
							result.add(i);
						}
					}
					return result;
				}
			} catch (SigarException e) {
				// TODO logging
				return Collections.emptyList();
			}
		}		
	}
	
	private static interface NetStatSampler {
	
		public void report(String intf, long prevTimestamp, NetInterfaceStat prev, long lastTimestamp, NetInterfaceStat last);
		
	}
		
	private interface NetAttr {
		
		public double extract(NetInterfaceStat prev, NetInterfaceStat last);
		
	}
}
