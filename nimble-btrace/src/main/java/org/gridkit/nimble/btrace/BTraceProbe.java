package org.gridkit.nimble.btrace;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.btrace.ext.PollSamplesCmdResult;
import org.gridkit.nimble.btrace.ext.RingBuffer;
import org.gridkit.nimble.btrace.ext.model.DurationSample;
import org.gridkit.nimble.btrace.ext.model.PointSample;
import org.gridkit.nimble.btrace.ext.model.RateSample;
import org.gridkit.nimble.btrace.ext.model.SampleStoreContents;
import org.gridkit.nimble.btrace.ext.model.ScalarSample;
import org.gridkit.nimble.btrace.ext.model.SpanSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BTraceProbe implements Callable<Void> {
    
	private static final Logger log = LoggerFactory.getLogger(BTraceProbe.class);
    
    private long pid;
    private BTraceScriptSettings settings;
    
    protected NimbleClient client; 
    protected BTraceScriptSampler sampler;
    
    private Map<String, SampleStoreProcessor> processors = new HashMap<String, SampleStoreProcessor>();
    
    @Override
    public Void call() throws Exception {
        try {
            log.debug(String.format("Polling samples from pid %d with settings %s", pid, settings));
            PollSamplesCmdResult result = client.pollSamples();
            
            for (SampleStoreContents contents : result.getData()) {
                getProcessor(contents.getSampleStore()).process(contents);
            }
            
            return null;
        } catch (InterruptedException e) {
            log.debug(String.format("BTrace probe for pid %d with settings %s was interrupted", pid, settings));
            return null;
        } catch (Exception e) {
            log.error(String.format("Error while executing BTrace probe for pid %d with settings %s", pid, settings), e);
            throw e;
        }
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public void setSettings(BTraceScriptSettings settings) {
        this.settings = settings;
    }

    public void setClient(NimbleClient client) {
        this.client = client;
    }

    private SampleStoreProcessor getProcessor(String sampleStore) {
        if (!processors.containsKey(sampleStore)) {
            processors.put(sampleStore, new SampleStoreProcessor(sampleStore));
        }
        
        return processors.get(sampleStore);
    }
    
    private class SampleStoreProcessor {        
	
		private String store;
	    private long lastSeqNum = RingBuffer.START_ID - 1;
	    private Map<String, Last> levels = new HashMap<String, Last>();
	    
	    public SampleStoreProcessor(String store) {
	    	this.store = store;
		}
	    
	    public void process(SampleStoreContents contents) {
	    	double timestampS = Seconds.currentTime();
	
	    	long newLastSeqNum = lastSeqNum;
	    	for (ScalarSample sample : contents.getSamples()) {
	        	newLastSeqNum = Math.max(newLastSeqNum, sample.getSeqNumber());
	            submitSample(sample);
	        }
	
	    	long missed = newLastSeqNum - lastSeqNum - contents.getSamples().size();
	
	    	if (missed > 0) {
	    		sampler.reportMissedSamples(store, timestampS, missed);
	    	}
	    	
	    	lastSeqNum = newLastSeqNum;
	    }
	    
	    private void submitSample(ScalarSample rawSample) {       
	        if (rawSample instanceof SpanSample) {
	            submitSpan((SpanSample)rawSample);
	        } else if (rawSample instanceof DurationSample) {
	            submitDuration((DurationSample)rawSample);
	        } else if (rawSample instanceof RateSample) {
	            submitRate((RateSample)rawSample);
	        } else if (rawSample instanceof PointSample) {
	            submitPoint((PointSample)rawSample);
	        } else {
	            submitScalar(rawSample);
	        }
	    }
	
	    private void submitScalar(ScalarSample sample) {        	
	    	sampler.reportScalar(store, sample.getKey(), sample.getValue().doubleValue());
	    }
	
	    private void submitSpan(SpanSample sample) {            
	
	    	double timestampS = Seconds.fromMillis(sample.getTimestampMs());
	        
	        sampler.reportSpan(store, sample.getKey(), timestampS, sample.getDurationNs(), sample.getValue().doubleValue());
	    }
	    
	    private void submitRate(RateSample sample) {
	        double timestampS = Seconds.fromMillis(sample.getTimestampMs());
	        
	        Last last = levels.get(sample.getKey());
	        if (last == null) {
	        	last = new Last();
	        	last.timestamp = timestampS;
	        	last.value = sample.getValue().doubleValue();
	        	levels.put(sample.getKey(), last);
	        }
	        else {
	        	long durationNS = (long)((timestampS - last.timestamp) * TimeUnit.SECONDS.toNanos(1));
	        	sampler.reportSpan(store, sample.getKey(), last.timestamp, durationNS, sample.getValue().doubleValue() - last.value);
	        	last.timestamp = timestampS;
	        	last.value = sample.getValue().doubleValue();
	        }            
	    }
	    
	    private void submitDuration(DurationSample sample) {
	        
	        double timestampS = Seconds.fromMillis(sample.getTimestampMs());
	        long durationNS = sample.getValue().longValue();
	        
	        sampler.reportDuration(store, sample.getKey(), timestampS, durationNS);
	    }
	
	    private void submitPoint(PointSample sample) {
	        double timestampS = Seconds.fromMillis(sample.getTimestampMs());
	        
	        sampler.reportPoint(store, sample.getKey(), timestampS, sample.getValue().doubleValue());
	    }        
	}

	private static class Last {

    	double timestamp;
    	double value;
    	
    }
}
