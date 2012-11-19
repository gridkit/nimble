package org.gridkit.nimble.driver;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.metering.ArraySampleManager;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleWriter;
import org.gridkit.nimble.metering.SamplerBuilder;
import org.gridkit.nimble.metering.ScalarSampler;
import org.gridkit.nimble.metering.SpanReporter;
import org.gridkit.nimble.metering.TimeReporter;
import org.gridkit.nimble.orchestration.DeployableBean;
import org.gridkit.nimble.pivot.DistributedPivotReporter;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.PivotReporter;
import org.gridkit.nimble.pivot.SampleAccumulator;
import org.gridkit.vicluster.ViNode;

public class PivotMeteringDriver implements MeteringDriver, DeployableBean {
	
	private final DistributedPivotReporter reporter;
	private final int bufferSize;
	private final Map<String, RemoteSlave> slaves = new HashMap<String, RemoteSlave>();
	
	public PivotMeteringDriver(Pivot pivot) {
		this(pivot, 16 << 10);
	}
	
	public PivotMeteringDriver(Pivot pivot, int bufferSize) {
		this.bufferSize = bufferSize;
		this.reporter = new DistributedPivotReporter(pivot);		
	}
	
	public PivotReporter getReporter() {
		return reporter;
	}
	
	public SampleReader getReader() {
		return reporter.getReader();
	}

	@Override
	public SampleSchema getSchema() {
		throw new UnsupportedOperationException("Should be called in node scope");
	}

	@Override
	public void setGlobal(Object key, Object value) {
		throw new UnsupportedOperationException("Should be called in node scope");
	}

	@Override
	public SamplerBuilder samplerBuilder() {
		throw new UnsupportedOperationException("Should be called in node scope");
	}

	@Override
	public void flush() {
		throw new UnsupportedOperationException("Should be called in node scope");
	}
	
	
    @Override
	public <S, T extends MeteringAware<S>> MeteringSink<S> bind(T sink) {
    	throw new UnsupportedOperationException("Should be called in node scope");
    }
	
	@Override
	public synchronized DeploymentArtifact createArtifact(ViNode target, DepolymentContext context) {
		String nodename = target.toString();
		// TODO keep track on slaves ?
		if (slaves.containsKey(nodename)) {
			throw new IllegalStateException("Duplicate slave creation, node " + nodename);
		}
		
		return new Deployer(nodename, reporter.createSlaveReporter(), bufferSize);
	}

	private static class Deployer implements DeploymentArtifact, Serializable {

		private static final long serialVersionUID = 20121017L;
		
		private final String nodename;
		private final SampleAccumulator accumulator;
		private final int bufferSize;
		
		public Deployer(String nodename, SampleAccumulator accumulator, int bufferSize) {
			this.nodename = nodename;
			this.accumulator = accumulator;
			this.bufferSize = bufferSize;
		}

		@Override
		public Object deploy(EnvironmentContext context) {
			return new Slave(nodename, accumulator, bufferSize);
		}
	}

	private interface RemoteSlave extends MeteringDriver, Remote {

	}
	
	private static class Slave implements MeteringDriver {
		
		private final String nodename;
		private final ArraySampleManager manager;
		private final SampleAccumulator accumulator;		
		
		private final Map<Object, Object> globals =  new HashMap<Object, Object>();
		@SuppressWarnings("unused")
		private final Thread reporter;
		
		public Slave(String nodename, SampleAccumulator accumulator, int bufferSize) {
			this.nodename = nodename;
			this.manager = new ArraySampleManager(bufferSize);
			this.accumulator = accumulator;
			this.reporter = startReporter();
			
			globals.put(NODE, nodename);
			try {
				globals.put(HOSTNAME, InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e) {
				globals.put(HOSTNAME, "unknown:" + nodename);
			}
		}

		private Thread startReporter() {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					while(true) {
						processSamples();
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// TODO logging
							e.printStackTrace();
							return;
						}
					}
				}
			};
			Thread h = new Thread(r);
			h.setDaemon(true);
			h.setName("StatsProcessor[" + nodename + "]");
			h.start();
			return h;
		}

		@Override
		public SampleSchema getSchema() {
			SampleSchema ss = ArraySampleManager.newScheme();
			for(Object key: globals.keySet()) {
				ss.setStatic(key, globals.get(key));
			}
			manager.adopt(ss);
			return ss;
		}

		@Override
		public void setGlobal(Object key, Object value) {
			globals.put(key, value);			
		}

		@Override
		public SamplerBuilder samplerBuilder() {
			return new Builder(getSchema());
		}

		@Override
		public void flush() {
			processSamples();
			accumulator.flush();
		}

		private synchronized void processSamples() {
			accumulator.accumulate(manager);
		}
		
		@Override
		public <S, T extends MeteringAware<S>> MeteringSink<S> bind(final T sink) {
	        final S attached = sink.attach(this);

	        return new MeteringSink<S>() {
				@Override
	            public S getSink() {
	                return attached;
	            }
	        };
	    }
	}
	
	private static class Builder implements SamplerBuilder {

		private SampleSchema schema;
		
		private Builder(SampleSchema schema) {
			this.schema = schema.createDerivedScheme();
			this.schema.setStatic(Measure.PRODUCER, SamplerBuilder.class);
		}

		@Override
		public SamplerBuilder set(Object key, Object value) {
			schema.setStatic(key, value);
			return this;
		}

		@Override
		public TimeReporter timeReporter(String name) {
			SampleSchema s = schema.createDerivedScheme();
			s.setStatic(Measure.NAME, name);
			s.declareDynamic(Measure.TIMESTAMP, double.class);
			s.declareDynamic(Measure.DURATION, double.class);
			return new SimpleTimeReporter(s.createFactory());
		}

		@Override
		public SpanReporter snapReporter(String name) {
			SampleSchema s = schema.createDerivedScheme();
			s.setStatic(Measure.NAME, name);
			s.declareDynamic(Measure.TIMESTAMP, double.class);
			s.declareDynamic(Measure.DURATION, double.class);
			s.declareDynamic(Measure.MEASURE, double.class);
			return new SimpleSnapReporter(s.createFactory());
		}

		@Override
		public ScalarSampler scalarSampler(String name) {
			SampleSchema s = schema.createDerivedScheme();
			s.setStatic(Measure.NAME, name);
			s.declareDynamic(Measure.MEASURE, double.class);
			return new SimpleScalarReporter(s.createFactory());
		}
	}
	
	private static class SimpleTimeReporter implements TimeReporter {

		private final SampleFactory factory;
		
		private SimpleTimeReporter(SampleFactory factory) {
			this.factory = factory;
//			this.factory.trace(true);
		}

		@Override
		public StopWatch start() {
			return new StopWatch() {
				
				private final long nsStart = System.nanoTime();
				private final SampleWriter writer = factory.newSample();
				
				@Override
				public void finish() {
					long nsEnd = System.nanoTime();
					writer.setTimeBounds(nsStart, nsEnd);
					writer.submit();
				}
			};
		}		
	}

	private static class SimpleSnapReporter implements SpanReporter {
		
		private final SampleFactory factory;
		
		private SimpleSnapReporter(SampleFactory factory) {
			this.factory = factory;
		}
		
		@Override
		public StopWatch start() {
			return new StopWatch() {
				
				private final long nsStart = System.nanoTime();
				private final SampleWriter writer = factory.newSample();
				
				
				@Override
				public void finish(double measure) {
					long nsEnd = System.nanoTime();
					writer.setTimeBounds(nsStart, nsEnd);
					writer.setMeasure(measure);
					writer.submit();
				}
			};
		}		
	}
	
	public static class SimpleScalarReporter implements ScalarSampler {
		
		private final SampleFactory factory;

		private SimpleScalarReporter(SampleFactory factory) {
			this.factory = factory;
		}

		@Override
		public void write(double value) {
			factory.newSample()
				.setMeasure(value)
				.submit();
		}
	}
}
