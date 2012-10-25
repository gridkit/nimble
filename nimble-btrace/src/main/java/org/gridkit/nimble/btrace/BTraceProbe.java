package org.gridkit.nimble.btrace;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.java.btrace.client.Client;

import org.gridkit.nimble.btrace.ext.PollSamplesCmdResult;
import org.gridkit.nimble.btrace.ext.model.PointSample;
import org.gridkit.nimble.btrace.ext.model.SampleStoreContents;
import org.gridkit.nimble.btrace.ext.model.ScalarSample;
import org.gridkit.nimble.btrace.ext.model.SpanSample;
import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.metering.ScalarSampler;
import org.gridkit.nimble.metering.SpanSampler;
import org.gridkit.nimble.probe.RateSampler;
import org.gridkit.nimble.probe.SamplerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BTraceProbe implements Callable<Void> {
    private static final Logger log = LoggerFactory.getLogger(BTraceProbe.class);
    
    private long pid;
    private BTraceScriptSettings settings;
    private long timeoutMs;
    
    private BTraceClientOps clientOps;
    private BTraceClientSource clientSource;
    
    private SamplerFactory samplerFactory;
    private Map<String, RateSampler> rateSamplers = new HashMap<String, RateSampler>();
    
    private Client client;

    @Override
    public Void call() throws Exception {
        try {            
            Client client = getClient();
    
            PollSamplesCmdResult result = clientOps.pollSamples(
                client, getScriptClasses(), timeoutMs
            );
            
            submit(result);
            
            return null;
        } catch (Exception e) {
            log.error("Error while execiting BTraceProbe with settings " + settings, e);
            throw e;
        }
    }
    
    private void submit(PollSamplesCmdResult result) {
        for (SampleStoreContents contents : result.getData()) {
            for (ScalarSample sample : contents.getSamples()) {
                submit(sample);
            }
        }
    }
    
    private void submit(ScalarSample rawSample) {       
        if (rawSample instanceof PointSample) {
            submitPoint((PointSample)rawSample);
        } else if (rawSample instanceof SpanSample) {
            submitSpan((SpanSample)rawSample);
        } else {
            submitScalar(rawSample);
        }
    }

    public void submitScalar(ScalarSample rawSample) {
        ScalarSampler sampler = samplerFactory.getScalarSampler(rawSample.getKey());
        sampler.write(rawSample.getValue().doubleValue());
    }

    public void submitSpan(SpanSample sample) {
        SpanSampler sampler = samplerFactory.getSpanSampler(sample.getKey());
        
        long startTsNs = TimeUnit.MILLISECONDS.toNanos(sample.getStartTimestampMs());
        long finishTsNs = TimeUnit.MILLISECONDS.toNanos(sample.getFinishTimestampMs());
        
        sampler.write(sample.getValue().doubleValue(), startTsNs, finishTsNs);
    }

    public void submitPoint(PointSample sample) {
        long ts = TimeUnit.MILLISECONDS.toNanos(sample.getTimestampMs());
        
        if (sample.isRate()) {
            RateSampler sampler = getRateSampler(sample.getKey());
            sampler.write(sample.getTimestampMs(), ts);
        } else {
            PointSampler sampler = samplerFactory.getPointSampler(sample.getKey());
            
            if (sample.isDuration()) {
                double durationS = sample.getValue().doubleValue() / TimeUnit.SECONDS.toNanos(1);
                sampler.write(durationS, ts);
            } else {
                sampler.write(sample.getValue().doubleValue(), sample.getTimestampMs());
            }
        }
    }

    private RateSampler getRateSampler(String key) {
        if (!rateSamplers.containsKey(key)) {
            rateSamplers.put(key, new RateSampler(samplerFactory.getSpanSampler(key)));
        }
        
        return rateSamplers.get(key);
    }
    
    private Client getClient() throws Exception {
        if (client == null) {
            try {
                client = clientSource.getClient(pid);
                
                System.err.println(client);
                
                // submit is first because it is the only method initializing command channel
                clientOps.submit(client, settings.getScriptClass(), settings.getArgsArray(), timeoutMs);
                
                clientOps.clearSamples(client, getScriptClasses(), timeoutMs);
            } catch (Exception e) {
                log.error(F("Failed to connect to client with pid %d", pid));
                throw e;
            }
        }
        
        return client;
    }
    
    private Collection<Class<?>> getScriptClasses() {
        return Collections.<Class<?>>singleton(settings.getScriptClass());
    }
    
    public void setPid(long pid) {
        this.pid = pid;
    }

    public void setSettings(BTraceScriptSettings settings) {
        this.settings = settings;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public void setClientOps(BTraceClientOps clientOps) {
        this.clientOps = clientOps;
    }

    public void setClientSource(BTraceClientSource clientSource) {
        this.clientSource = clientSource;
    }

    public void setSamplerFactory(SamplerFactory samplerFactory) {
        this.samplerFactory = samplerFactory;
    }
}
