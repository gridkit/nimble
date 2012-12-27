package org.gridkit.nimble.btrace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

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
import org.gridkit.nimble.pivot.Filters;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.SampleExtractor;
import org.gridkit.nimble.pivot.SampleFilter;
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
	
	private List<BTraceScriptSettings> scripts = new ArrayList<BTraceScriptSettings>();
	
	private TargetLocator<Long> locator;
	
	private Set<ReportLine> reportLines = Collections.newSetFromMap(new IdentityHashMap<ReportLine, Boolean>());
	
	private boolean showTimeDistr = false;
	private boolean showDistr = false;
	private boolean showFreq = false;
	private boolean showWeighFreq = false;
	
	private static final Object EVENT_FREQ_KEY = "EVENT_FREQ_KEY";
	
	@SuppressWarnings("unused")
	private SchemaConfigurer<Long> schemaConfigurer = new NoSchema<Long>();
	
	public BTraceMonitoring(String namespace) {
		super(namespace);
	}

	public void addScript(Class<?> script) {
        BTraceScriptSettings settings = new BTraceScriptSettings();
        settings.setScriptClass(script);
        addScript(settings);
	}
	
	public void addScript(BTraceScriptSettings settings) {
	    scripts.add(settings);
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
	
    public void showValueDistribution() {
        showDistr = true;
    }

    public void showTimeDistribution() {
        showTimeDistr = true;
    }

    public void showEventFrequency() {
        showFreq = true;
    }

    public void showWeightedFrequency() {
        showWeighFreq = true;
    }
    
	public void showScalarSamples() {
	    reportLines.add(SCALAR_LINE);
	}
	
    public void showPointSamples() {
        reportLines.add(POINT_LINE);
    }
	
    public void showSpanSamples() {
        reportLines.add(SPAN_LINE);
    }
    
    public void showMissedStats() {
        reportLines.add(MISSED_LINE);
    }
    
    public void showReceivedStats() {
        reportLines.add(RECEIVED_LINE);
    }
	
	@Override
	public String getDescription() {
		return "BTrace profiler";
	}

	@Override
	public void configurePivot(Pivot pivot) {
		for (ReportLine reportLine : reportLines) {
		    Pivot.Level base = pivot.root().level(reportLine.getNamespace(namespace))
		                                   .filter(reportLine.getFilter());
		    
		    for(Object group : groupping) {
	            base = base.group(group);
	        }
		    
            for(Object group : reportLine.getGroups()) {
                base = base.group(group);
            }
            
            reportLine.configureLevel(base.level(reportLine.getNamespace(namespace)));
		}
	}

	@Override
	public void configurePrinter(PrintConfig printer) {
		printConfig.replay(printer);
		
		for (ReportLine reportLine : reportLines) {
		    reportLine.configureDisplay(DisplayBuilder.with(printer, reportLine.getNamespace(namespace)));
		}
	}

	@Override
	public void deploy(ScenarioBuilder sb, ServiceProvider context, TimeLine timeLine) {
		MeteringDriver metering = context.lookup(MeteringDriver.class);
		BTraceDriver bTrace = context.lookup(BTraceDriver.class);
		MeteringSink<BTraceSamplerFactoryProvider> sink = metering.bind(BTrace.defaultReporter());

		for(BTraceScriptSettings script : scripts) {
			sb.from(timeLine.getInitCheckpoint());
			ProbeHandle probe = bTrace.trace(new PP(locator), script, sink);
			sb.join(timeLine.getStartCheckpoint());
			
			sb.fromStart();
			probe.join();
			sb.join(timeLine.getDoneCheckpoint());
	
			sb.from(timeLine.getStopCheckpoint());
			probe.stop();
			sb.join(timeLine.getDoneCheckpoint());
		}
	}
	
	private static class ScriptExtractor implements SampleExtractor {
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
	}
	
	private abstract class ReportLine {
        public abstract void configureLevel(Pivot.Level level);
        
        public abstract void configureDisplay(DisplayBuilder db);
        
        public abstract List<Object> getGroups();
        
        public abstract SampleFilter getFilter();
        
        public String getNamespace(String namespace) {
            return namespace + "[" + this.toString() + "]";
        }
	}
	
	private ReportLine SCALAR_LINE = new ReportLine() {
        @Override
        public List<Object> getGroups() {
            return Arrays.<Object>asList(BTraceMeasure.SCRIPT_KEY, BTraceMeasure.STORE_KEY, BTraceMeasure.SAMPLE_KEY);
        }
        
        @Override
        public SampleFilter getFilter() {
            return Filters.equals(BTraceMeasure.SAMPLE_TYPE_KEY, BTraceMeasure.SAMPLE_TYPE_SCALAR);
        }
        
        @Override
        public void configureLevel(Pivot.Level level) {
            level.calcDistribution(Measure.MEASURE);
        }
        
        @Override
        public void configureDisplay(DisplayBuilder db) {
            db.value(new ScriptExtractor()).caption("Script")
              .attribute(BTraceMeasure.STORE_KEY).caption("Store")
              .attribute(BTraceMeasure.SAMPLE_KEY).caption("Metric")
              .count().caption("Count")
              .distributionStats(Measure.MEASURE);
        }
        
        @Override
        public String toString() {
            return "SCALAR_LINE";
        };
	};
	
	private ReportLine POINT_LINE = new ReportLine() {
        @Override
        public List<Object> getGroups() {
            return Arrays.<Object>asList(BTraceMeasure.SCRIPT_KEY, BTraceMeasure.STORE_KEY, BTraceMeasure.SAMPLE_KEY);
        }
        
        @Override
        public SampleFilter getFilter() {
            return Filters.in(BTraceMeasure.SAMPLE_TYPE_KEY, BTraceMeasure.SAMPLE_TYPE_POINT);
        }
        
        @Override
        public void configureLevel(Pivot.Level level) {            
            if (showDistr) {
                level = level.calcDistribution(Measure.MEASURE);
            }
            
            if (showFreq) {
                level = level.calcFrequency(EVENT_FREQ_KEY, 1.0);
            }
            
            if (showWeighFreq) {
                level = level.calcFrequency(Measure.MEASURE);
            }
        }
        
        @Override
        public void configureDisplay(DisplayBuilder db) {
            db.value(new ScriptExtractor()).caption("Script")
              .attribute(BTraceMeasure.STORE_KEY).caption("Store")
              .attribute(BTraceMeasure.SAMPLE_KEY).caption("Metric");
            
            if (showDistr) {
                db.count(Measure.MEASURE).caption("Count");
                db.distributionStats(Measure.MEASURE);
            }
           
            if (showFreq) {
                db.frequency(EVENT_FREQ_KEY).caption("Freq. [Op/S]");
            }
            
            if (showWeighFreq) {
                db.frequency().caption("W.Freq. [1/S]");
            }
            
            if (showFreq) {
                db.duration(EVENT_FREQ_KEY).caption("Duration [s]");
            } else if (showWeighFreq) {
                db.duration().caption("Duration [s]");
            }
        }
        
        @Override
        public String toString() {
            return "POINT_LINE";
        };
	};
	
	private ReportLine SPAN_LINE = new ReportLine() {
        @Override
        public List<Object> getGroups() {
            return Arrays.<Object>asList(BTraceMeasure.SCRIPT_KEY, BTraceMeasure.STORE_KEY, BTraceMeasure.SAMPLE_KEY);
        }
        
        @Override
        public SampleFilter getFilter() {
            return Filters.in(BTraceMeasure.SAMPLE_TYPE_KEY, BTraceMeasure.SAMPLE_TYPE_SPAN);
        }
        
        @Override
        public void configureLevel(Pivot.Level level) {
            if (showTimeDistr) {
                level = level.calcDistribution(Measure.DURATION);
            }
            
            if (showDistr) {
                level = level.calcDistribution(Measure.MEASURE);
            }
            
            if (showFreq) {
                level = level.calcFrequency(EVENT_FREQ_KEY, 1.0);
            }
            
            if (showWeighFreq) {
                level = level.calcFrequency(Measure.MEASURE);
            }
        }
        
        @Override
        public void configureDisplay(DisplayBuilder db) {
            db.value(new ScriptExtractor()).caption("Script")
              .attribute(BTraceMeasure.STORE_KEY).caption("Store")
              .attribute(BTraceMeasure.SAMPLE_KEY).caption("Metric");
              
            if (showTimeDistr) {
                db.count(Measure.DURATION).caption("Count");
            } else if (showDistr) {
                db.count(Measure.MEASURE).caption("Count");
            }
            
            if (showTimeDistr) {
                db.distributionStats(Measure.DURATION).caption("Dur. %s [ms]").asMillis();
            }
            
            if (showDistr) {
                db.distributionStats(Measure.MEASURE);
            }
           
            if (showFreq) {
                db.frequency(EVENT_FREQ_KEY).caption("Freq. [Op/S]");
            }
            
            if (showWeighFreq) {
                db.frequency().caption("W.Freq. [1/S]");
            }
            
            if (showFreq) {
                db.duration(EVENT_FREQ_KEY).caption("Duration [s]");
            } else if (showWeighFreq) {
                db.duration().caption("Duration [s]");
            }    
        }
        
        @Override
        public String toString() {
            return "SPAN_LINE";
        };
	};
	
	private ReportLine MISSED_LINE = new ReportLine() {
        @Override
        public List<Object> getGroups() {
            return Arrays.<Object>asList(BTraceMeasure.SCRIPT_KEY, BTraceMeasure.STORE_KEY);
        }
        
        @Override
        public SampleFilter getFilter() {
            return Filters.equals(BTraceMeasure.SAMPLE_TYPE_KEY, BTraceMeasure.SAMPLE_TYPE_MISSED);
        }
        
        @Override
        public void configureLevel(Pivot.Level level) {
            level.calcDistribution(Measure.MEASURE)
                 .calcFrequency(Measure.MEASURE);
        }
        
        @Override
        public void configureDisplay(DisplayBuilder db) {
            db.value(new ScriptExtractor()).caption("Script")
              .attribute(BTraceMeasure.STORE_KEY).caption("Store")
              .constant("Metric", "Missed samples")
              .count().caption("Count")
              .distributionStats(Measure.MEASURE)
              .sum(Measure.MEASURE)
              .frequency().caption("W.Freq. [1/S]")
              .duration().caption("Duration [s]");
        }
        
        @Override
        public String toString() {
            return "MISSED_LINE";
        };
	};
	
	private ReportLine RECEIVED_LINE = new ReportLine() {
        @Override
        public List<Object> getGroups() {
            return Arrays.<Object>asList(BTraceMeasure.SCRIPT_KEY, BTraceMeasure.STORE_KEY);
        }
        
        @Override
        public SampleFilter getFilter() {
            return Filters.equals(BTraceMeasure.SAMPLE_TYPE_KEY, BTraceMeasure.SAMPLE_TYPE_RECEIVED);
        }
        
        @Override
        public void configureLevel(Pivot.Level level) {
            level.calcDistribution(Measure.MEASURE)
                 .calcFrequency(Measure.MEASURE);
        }
        
        @Override
        public void configureDisplay(DisplayBuilder db) {
            db.value(new ScriptExtractor()).caption("Script")
              .attribute(BTraceMeasure.STORE_KEY).caption("Store")
              .constant("Metric", "Received samples")
              .count().caption("Count")
              .distributionStats(Measure.MEASURE)
              .sum(Measure.MEASURE)
              .frequency().caption("W.Freq. [1/S]")
              .duration().caption("Duration [s]");
        }
        
        @Override
        public String toString() {
            return "RECEIVED_LINE";
        };
	};

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
					LOGGER.info("BTrace target " + pd.getJavaProcId());
				}
			}
			return pids;			
		}
	}	
}
