package org.gridkit.nimble.monitoring.btrace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.jvm.attach.JavaProcessMatcher;
import org.gridkit.lab.monitoring.probe.TargetLocator;
import org.gridkit.nimble.btrace.BTraceClientSettings;
import org.gridkit.nimble.btrace.BTraceMeasure;
import org.gridkit.nimble.btrace.BTraceScriptSampler;
import org.gridkit.nimble.btrace.BTraceScriptSettings;
import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleKey;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleWriter;
import org.gridkit.nimble.monitoring.AbstractMonitoringBundle;
import org.gridkit.nimble.monitoring.NoSchema;
import org.gridkit.nimble.monitoring.PollingBundle;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Filters;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.SampleExtractor;
import org.gridkit.nimble.pivot.SampleFilter;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PrintConfig;
import org.gridkit.nimble.probe.JvmMatcherPidProvider;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.probe.probe.JmxProbes;
import org.gridkit.nimble.probe.probe.MonitoringDriver;
import org.gridkit.nimble.probe.probe.SamplerPrototype;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BTraceMonitoring extends AbstractMonitoringBundle implements PollingBundle {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(BTraceMonitoring.class);

	private enum AttrKey implements SampleKey {
		SCRIPT,
		STORE,
		KEY,
		TYPE,
		MISSED
	}
	
	private TargetLocator<Long> locator;
	private SchemaConfigurer<Long> schemaConfigurer = new NoSchema<Long>();
	
	private BTraceClientSettings clientSettings = new BTraceClientSettings();
	
	private Class<?> scriptClass;
    private List<String> args = new ArrayList<String>();
    private long pollPeriodMS = 5000;
    private long timeoutMs = 2000;
	
	private Map<ReportLine, ReportLine> reportLines = new LinkedHashMap<ReportLine, ReportLine>();
		
	public BTraceMonitoring(String namespace) {
		super(namespace);
	}

	public void setScript(Class<?> script) {
		this.scriptClass = script;
	}
	
	@Override
	public void setPollPeriod(long periodMs) {
		this.pollPeriodMS = periodMs;
	}
	
	public void setDebug(boolean debug) {
		this.clientSettings.setDebug(debug);
	}

	public void setUnsafe(boolean unsafe) {
		this.clientSettings.setUnsafe(unsafe);
	}
	
	public void addExtension(String jarPath) {
		this.clientSettings.addExtension(jarPath);
	}
	
	public void setConnectionTimeout(long timeoutMS) {
		this.timeoutMs = timeoutMS;
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
	
	public ReportGroup showStore(String matcher) {
		ReportLine line = new ReportLine(matcher);
		if (reportLines.get(line) == null) {
			reportLines.put(line, line);
		}
		return line;
	}
	
    public void showMissedStats() {
    	MissedLine line = new MissedLine();
    	if (!reportLines.containsKey(line)) {
    		reportLines.put(line, line);
    	}
    }
    
	@Override
	public String getDescription() {
		return "BTrace profiler";
	}

	@Override
	public void configurePivot(Pivot pivot) {
		Pivot.Level base = pivot.root();

		base = base.level(namespace).filter(Measure.PRODUCER, getProducerId());
		
		for(Object group: groupping) {
			base = base.group(group);
		}
		base = base.group(AttrKey.SCRIPT).group(AttrKey.STORE).level("");
		
		for (ReportLine reportLine : reportLines.values()) {
            reportLine.configureLevel(base);
		}
	}

	@Override
	public void configurePrinter(PrintConfig printer) {
		printConfig.replay(printer);
		
		for (ReportLine reportLine : reportLines.values()) {
		    reportLine.configurePrinter(printer);
		}
	}

	@Override
	public void deploy(ScenarioBuilder sb, ServiceProvider context, TimeLine timeLine) {
		MonitoringDriver monDriver = context.lookup(MonitoringDriver.class);

		BTraceDeployer deployer = new BTraceDeployer();
		deployer.setClientSettings(clientSettings);
		
		BTraceScriptSettings settings = new BTraceScriptSettings();
		settings.setScriptClass(scriptClass);
		settings.setArgs(args);
		settings.setTimeoutMs(timeoutMs);
		
		deployer.setScriptSettings(settings);
		
		sb.from(timeLine.getInitCheckpoint());
		Activity probe = monDriver.deploy(locator, deployer, new SchemaEnricher(scriptClass.getName(), schemaConfigurer), new SamplerProvider(getProducerId()), pollPeriodMS);
		sb.join(timeLine.getStartCheckpoint());
			
		sb.fromStart();
		probe.join();
		sb.join(timeLine.getDoneCheckpoint());
	
		sb.from(timeLine.getStopCheckpoint());
		probe.stop();
		sb.join(timeLine.getDoneCheckpoint());
	}
	
	private static final class SchemaEnricher implements SchemaConfigurer<Long>, Serializable {

		private static final long serialVersionUID = 20121116L;
		
		private final String scriptName;
		private final SchemaConfigurer<Long> nested;
		
		public SchemaEnricher(String scriptName, SchemaConfigurer<Long> nested) {
			this.scriptName = scriptName;
			this.nested = nested;
		}

		@Override
		public SampleSchema configure(Long target, SampleSchema root) {
			try {
				SampleSchema ss = root.createDerivedScheme();
				ss.setStatic(AttrKey.SCRIPT, scriptName);
				return nested.configure(target, ss);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static class SamplerProvider implements SamplerPrototype<BTraceScriptSampler>, Serializable {
		
		private static final long serialVersionUID = 20121113L;
		
		private final Object producerId;
		
		public SamplerProvider(Object producerId) {
			this.producerId = producerId;
		}

		@Override
		public BTraceScriptSampler instantiate(SampleSchema schema) {
			final SampleSchema ss = schema.createDerivedScheme();
			ss.setStatic(Measure.PRODUCER, producerId);
			
			return new Sampler(ss);
		}
	}
	
	private static class Sampler implements BTraceScriptSampler {
		
		private final SampleSchema root;
		private Map<String, SampleFactory> factories = new HashMap<String, SampleFactory>();
	
		Sampler(SampleSchema root) {
			this.root = root;
		}
	
		@Override
		public void reportScalar(String store, String metric, double value) {
			SampleWriter sw = getScalarWriter(store, metric);
			sw.setMeasure(value).submit();
		}
	
		@Override
		public void reportPoint(String store, String metric, double timestampS, double value) {
			SampleWriter sw = getPointWriter(store, metric);
			sw
				.set(Measure.TIMESTAMP, timestampS)
				.setMeasure(value)
				.submit();
		}
	
		@Override
		public void reportDuration(String store, String metric, double timestampS, long durationNS) {
			double durationS = ns2s(durationNS);
			SampleWriter sw = getDurationWriter(store, metric);
			sw
				.setTimeAndDuration(timestampS, durationS)
				.setMeasure(durationS)
				.submit();
		}
	
		@Override
		public void reportSpan(String store, String metric, double timestampS, long durationNS, double value) {
			double durationS = ns2s(durationNS);
			SampleWriter sw = getSpanWriter(store, metric);
			sw
				.setTimeAndDuration(timestampS, durationS)
				.setMeasure(value)
				.submit();
		}
	
		@Override
		public void reportMissedSamples(String store, double timestampS, long count) {
			SampleWriter sw = getMissedWriter(store);
			sw
				.set(Measure.TIMESTAMP, timestampS)
				.setMeasure(count)
				.submit();
		}
	
		private SampleWriter getScalarWriter(String store, String metric) {
			String key = "S\0" + store + '\0' + metric;
			SampleFactory factory = factories.get(key);
			if (factory == null) {
				factory = root.createDerivedScheme()
						.setStatic(AttrKey.STORE, store)
						.setStatic(AttrKey.KEY, metric)
						.declareDynamic(Measure.MEASURE, double.class)
						.createFactory();
				factories.put(key, factory);
			}
			return factory.newSample();
		}
	
		private SampleWriter getPointWriter(String store, String metric) {
			String key = "P\0" + store + '\0' + metric;
			SampleFactory factory = factories.get(key);
			if (factory == null) {
				factory = root.createDerivedScheme()
						.setStatic(AttrKey.STORE, store)
						.setStatic(AttrKey.KEY, metric)
						.declareDynamic(Measure.TIMESTAMP, double.class)
						.declareDynamic(Measure.MEASURE, double.class)
						.createFactory();
				factories.put(key, factory);
			}
			return factory.newSample();
		}
	
		private SampleWriter getDurationWriter(String store, String metric) {
			return getSpanWriter(store, metric);
		}
	
		private SampleWriter getSpanWriter(String store, String metric) {
			String key = "S\0" + store + '\0' + metric;
			SampleFactory factory = factories.get(key);
			if (factory == null) {
				factory = root.createDerivedScheme()
						.setStatic(AttrKey.STORE, store)
						.setStatic(AttrKey.KEY, metric)
						.declareDynamic(Measure.TIMESTAMP, double.class)
						.declareDynamic(Measure.DURATION, double.class)
						.declareDynamic(Measure.MEASURE, double.class)
						.createFactory();
				factories.put(key, factory);
			}
			return factory.newSample();
		}
	
		private SampleWriter getMissedWriter(String store) {
			String key = "M\0" + store + "\0\0";
			SampleFactory factory = factories.get(key);
			if (factory == null) {
				factory = root.createDerivedScheme()
						.setStatic(AttrKey.STORE, store)
						.setStatic(AttrKey.KEY, AttrKey.MISSED)
						.declareDynamic(Measure.TIMESTAMP, double.class)
						.declareDynamic(Measure.MEASURE, double.class)
						.createFactory();
				factories.put(key, factory);
			}
			return factory.newSample();
		}
	
		private double ns2s(long durationNS) {
			return ((double) durationNS) / TimeUnit.SECONDS.toNanos(1);
		}
	}

	public static interface ReportGroup {
		
		public ReportGroup showValueDistribution();

		public ReportGroup showTimeDistribution();

		public ReportGroup showEventFrequency();

		public ReportGroup showWeightedFrequency();
		
	}
	
	private class ReportLine implements ReportGroup {

		private String matcher;

		private boolean showValueDistribution;
		private boolean showTimeDistribution;
		private boolean showEventFrequency;
		private boolean showWeigthedFrequency;

		public ReportLine(String matcher) {
			this.matcher = matcher;
		}
		
	    @Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((matcher == null) ? 0 : matcher.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReportLine other = (ReportLine) obj;
			if (matcher == null) {
				if (other.matcher != null)
					return false;
			} else if (!matcher.equals(other.matcher))
				return false;
			return true;
		}

		@Override
		public ReportGroup showValueDistribution() {
	    	showValueDistribution = true;
			return this;
		}

		@Override
		public ReportGroup showTimeDistribution() {
			showTimeDistribution = true;
			return this;
		}

		@Override
		public ReportGroup showEventFrequency() {
			showEventFrequency = true;
			return this;
		}

		@Override
		public ReportGroup showWeightedFrequency() {
			showWeigthedFrequency = true;
			return this;
		}

		public void configureLevel(Pivot.Level base) {
			base = base.group(AttrKey.KEY).level(token());
			base.filter(getFilter());
			
			if (!showValueDistribution
					&& !showTimeDistribution
					&& !showEventFrequency
					&& !showWeigthedFrequency) {
				throw new IllegalArgumentException("Not statistics have been eabled for store [" + matcher + "]");
			}
			
			if (showValueDistribution) {
				base.calcDistribution(Measure.MEASURE);
			}
			if (showTimeDistribution) {
				base.calcDistribution(Measure.DURATION);
			}

			// use as base sample counter
			if (showEventFrequency) {
				base.calcFrequency(Measure.DURATION, 1);
			}
			if (showWeigthedFrequency) {
				base.calcFrequency(Measure.MEASURE);
			}
		}
	    
	    public void configurePrinter(PrintConfig pp) {
	    	
	    	DisplayBuilder.ForLevelDisplayBuider db = DisplayBuilder.with(pp, namespace + "." + token());
	    	db
	    	.value(new ScriptExtractor()).caption("Script")
	    	.attribute(AttrKey.STORE).caption("Store")
	    	.attribute(AttrKey.KEY).caption("Metric");
	    	
	    	if (showValueDistribution || showWeigthedFrequency) {
	    		db.count();
	    	}
	    	else {
	    		db.count(Measure.DURATION);
	    	}
	    		
    		
    		if (showValueDistribution) {
    			db
    			.mean().caption("Mean [1]")
    			.stdDev().caption("StdDev [1]");
    		}
    		
    		if (showTimeDistribution) {
    			db
    			.mean(Measure.DURATION).caption("Mean [S]")
    			.stdDev(Measure.DURATION).caption("StdDev [S]");    			
    		}
    		
    		if (showEventFrequency) {
    			db
    			.frequency(Measure.DURATION).caption("Freq. [Event/S]");
    		}
    		
    		if (showWeigthedFrequency) {
    			db
    			.frequency().caption("WFreq. [1/S]");
    		}
	    	
    		if (showTimeDistribution || showWeigthedFrequency) {
    			db
    			.sum().caption("Sum [1]");
    		}
    		
    		if (showEventFrequency) {
    			db
    			.duration(Measure.DURATION).caption("Observed [S]");
    		}
    		else if (showWeigthedFrequency) {
    			db
    			.duration().caption("Observed [S]");    			
    		}
	    }
	    
	    public SampleFilter getFilter() {
    		return Filters.and(Filters.dotGlob(AttrKey.STORE, matcher), Filters.ne(AttrKey.KEY, AttrKey.MISSED));
	    }

		public String token() {
			// levels also use dot nomation, so we have to escape dots
			// ugly, but it is not ment to be user visible
			return "report-" + matcher.replace('.', '~').replace('*', '$');
		}
		
		@Override
		public String toString() {
			return "ReportLine[" + matcher + "]";
		}
	}

	private final class MissedLine extends ReportLine {
	
		public MissedLine() {
			super("");
		}
		
		@Override
	    public SampleFilter getFilter() {
	        return Filters.equals(BTraceMeasure.SAMPLE_TYPE_KEY, BTraceMeasure.SAMPLE_TYPE_MISSED);
	    }
	
		@Override
	    public void configureLevel(Pivot.Level base) {			
			base = base.level(token());
			
			base.pivot("missed")
					.filter(AttrKey.KEY, AttrKey.MISSED)
					.calcDistribution(Measure.MEASURE);

			base.pivot("received")
					.filter(Filters.ne(AttrKey.KEY, AttrKey.MISSED))
					.calcDistribution(Measure.MEASURE);
	    }
	
		@Override
	    public void configurePrinter(PrintConfig pp) {
			DisplayBuilder db = DisplayBuilder.with(pp, namespace + "." + token());
			db
			.value(new ScriptExtractor()).caption("Script")
			.attribute(AttrKey.STORE).caption("Store")
			.constant("Metric", "<accuracy>")
			.deco("missed").count().caption("Missed samples")
			.deco("received").count().caption("Received samples");
	    }
	
		public String token() {
			return "missed";
		}
		
		@Override
	    public String toString() {
	        return "MissedLine";
	    }
	}

	private static class ScriptExtractor implements SampleExtractor {
        @Override
        public Object extract(SampleReader sample) {
            String cn = (String) sample.get(AttrKey.SCRIPT);
            if (cn != null) {
                int n = cn.lastIndexOf('.');
                return n < 0 ? cn : cn.substring(n + 1);
            }
            else {
                return null;
            }
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
}
