package org.gridkit.nimble.monitoring.coherence;

import java.io.Serializable;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.lab.util.jmx.mxstruct.coherence.ConnectionManagerMXStruct;
import org.gridkit.lab.util.jmx.mxstruct.coherence.MemberMBeanLocator;
import org.gridkit.lab.util.jmx.mxstruct.common.RuntimeMXStruct;
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
import org.gridkit.nimble.pivot.Pivot.Level;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PrintConfig;
import org.gridkit.nimble.probe.jmx.MBeanConnector;
import org.gridkit.nimble.probe.jmx.MBeanProbe;
import org.gridkit.nimble.probe.jmx.MBeanSampler;
import org.gridkit.nimble.probe.jmx.MBeanTarget;
import org.gridkit.nimble.probe.probe.MonitoringDriver;
import org.gridkit.nimble.probe.probe.SamplerPrototype;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;

public class CoherenceExtendMonitoring extends AbstractMonitoringBundle implements CoherenceMonitoringBundle, PollingBundle {


	private enum JvmId implements SampleKey {
		ID
	}
	
	private enum ConnAttrKey implements SampleKey {
		SENT_BYTES(){

			@Override
			public double read(ConnectionManagerMXStruct connBean) {
				return connBean.getTotalBytesSent();
			}
			
		},
		SENT_MSGS(){

			@Override
			public double read(ConnectionManagerMXStruct connBean) {
				return connBean.getTotalMessagesSent();
			}
			
		},
		RECEIVED_BYTES(){

			@Override
			public double read(ConnectionManagerMXStruct connBean) {
				return connBean.getTotalBytesReceived();
			}
			
		},
		RECEIVED_MSGS(){

			@Override
			public double read(ConnectionManagerMXStruct connBean) {
				return connBean.getTotalMessagesReceived();
			}
			
		}		
		;
		
		public abstract double read(ConnectionManagerMXStruct connBean);
	}
	
	private MBeanConnector connector;
	private SchemaConfigurer<MBeanServerConnection> schemaConfig = new NoSchema<MBeanServerConnection>();
	private long pollPeriod = 5000;
	
	public CoherenceExtendMonitoring(String namespace) {
		super(namespace);
	}

	@Override
	public String getDescription() {
		return "Coherence thread utilization";
	}

	public void setPollPeriod(long millis) {
		this.pollPeriod = millis;
	}
	
	public void setLocator(MBeanConnector connector) {
		this.connector = connector;
	}

	public void setSchemaConfig(SchemaConfigurer<MBeanServerConnection> schemeConfig) {
		this.schemaConfig = schemeConfig;
	}

	public void deploy(ScenarioBuilder sb, MonitoringDriver pollDriver, TimeLine phase) {
		if (connector == null) {
			throw new IllegalArgumentException("Connector is not set");
		}
		sb.from(phase.getInitCheckpoint());
		Activity probe = pollDriver.deploy(new MemberMBeanLocator(connector, ConnectionManagerMXStruct.NAME), new MBeanProbe(), createSchemaConfig(), createSampler(), pollPeriod);
		sb.join(phase.getStartCheckpoint());
		
		sb.from(phase.getStopCheckpoint());
		probe.stop();
		sb.join(phase.getDoneCheckpoint());
		
		sb.fromStart();
		probe.join();
		sb.join(phase.getDoneCheckpoint());
	}

	@Override
	public void deploy(ScenarioBuilder sb, ServiceProvider context, TimeLine phase) {
		MonitoringDriver pollDriver = context.lookup(MonitoringDriver.class);
		deploy(sb, pollDriver, phase);
	}

	private SchemaConfigurer<MBeanTarget> createSchemaConfig() {
		return new SchemaEnricher(schemaConfig);
	}
	
	private SamplerPrototype<MBeanSampler> createSampler() {
		return new SamplerProvider(getProducerId());
	}

	@Override
	public void configurePivot(Pivot pivot) {
		Pivot.Level base = pivot.root().level(namespace)
				.filter(Measure.PRODUCER, getProducerId());
		for(Object g: groupping) {
			base = base.group(g);
		}
		
		Pivot.Level report = base.level("");
		
		// added to track observed time
		report.calcFrequency(Measure.MEASURE, 1);
		
		for(ConnAttrKey key: ConnAttrKey.values()) {
			addToPivot(key, report);
		}
	}

	private void addToPivot(ConnAttrKey key, Level report) {
		report.calcFrequency(key);		
	}

	@Override
	public void configurePrinter(PrintConfig printer) {
		
		printConfig.replay(printer);
		
		DisplayBuilder.with(printer, namespace)
			.frequency(ConnAttrKey.SENT_BYTES).caption("Sent//[MiB/s]").asMiB()
			.sum(ConnAttrKey.SENT_BYTES).caption("Sent//Total [MiB]").asMiB()
			.frequency(ConnAttrKey.SENT_MSGS).caption("Sent//[Message/s]")
			.sum(ConnAttrKey.SENT_MSGS).caption("Sent//Total [Message]")
			.frequency(ConnAttrKey.RECEIVED_BYTES).caption("Received//[MiB/s]").asMiB()
			.sum(ConnAttrKey.RECEIVED_BYTES).caption("Received//Total [MiB]").asMiB()
			.frequency(ConnAttrKey.RECEIVED_MSGS).caption("Received//[Message/s]")
			.sum(ConnAttrKey.RECEIVED_MSGS).caption("Received//Total [Message]")
			.duration().caption("Observed [Sec]");
	}
	

	private static final class SchemaEnricher implements SchemaConfigurer<MBeanTarget>, Serializable {

		private static final long serialVersionUID = 20121116L;
		
		private final SchemaConfigurer<MBeanServerConnection> nested;
		
		public SchemaEnricher(SchemaConfigurer<MBeanServerConnection> nested) {
			this.nested = nested;
		}

		@Override
		public SampleSchema configure(MBeanTarget target, SampleSchema root) {
			try {
				SampleSchema ss = root.createDerivedScheme();
				ConnectionManagerMXStruct conn = ConnectionManagerMXStruct.PROTO.read(target.getConnection(), target.getMbeanName());
				ss.setStatic(JvmId.ID, RuntimeMXStruct.get(target.getConnection()).getName());
				ss.setStatic(CoherenceMetricsKey.SERVICE_NAME, conn.getServiceName());
				return nested.configure(target.getConnection(), ss);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static class SamplerProvider implements SamplerPrototype<MBeanSampler>, Serializable {
		
		private static final long serialVersionUID = 20121113L;
		
		private final Object producerId;
		
		public SamplerProvider(Object producerId) {
			this.producerId = producerId;
		}

		@Override
		public MBeanSampler instantiate(SampleSchema schema) {
			SampleSchema ss = schema.createDerivedScheme();
			ss.declareDynamic(Measure.TIMESTAMP, double.class);
			ss.declareDynamic(Measure.DURATION, double.class);
			for(ConnAttrKey key: ConnAttrKey.values()) {
				ss.declareDynamic(key, double.class);				
			}
			ss.setStatic(Measure.PRODUCER, producerId);
			final SampleFactory factory = ss.createFactory();
			
			return new MBeanSampler() {
				
				private long prevTimestamp;
				private double[] prev;

				@Override
				public void report(MBeanServerConnection connection, ObjectName target) {
					try {
						ConnectionManagerMXStruct mconn = ConnectionManagerMXStruct.PROTO.read(connection, target);
						long lastTimestamp = mconn.getMXStructTimestamp();
						ConnAttrKey[] attrs = ConnAttrKey.values();
						double[] nv = new double[attrs.length];
						for(int i = 0; i != nv.length; ++i) {
							nv[i] = attrs[i].read(mconn);
						}
						if (prev != null) {
							SampleWriter sw = factory.newSample();
							sw.setTimeBounds(prevTimestamp, lastTimestamp);
							for(int i = 0; i != nv.length; ++i) {
								sw.set(attrs[i], nv[i] - prev[i]);
							}
							sw.submit();
						}
						prev = nv;
						prevTimestamp = lastTimestamp;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}					
				}
			};
		}
	}
}
