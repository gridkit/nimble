package org.gridkit.nimble.btrace;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.Collections;
import java.util.concurrent.Callable;

import net.java.btrace.client.Client;

import org.gridkit.nimble.btrace.ext.PollSamplesCmdResult;
import org.gridkit.nimble.btrace.ext.model.PointSample;
import org.gridkit.nimble.btrace.ext.model.ScalarSample;
import org.gridkit.nimble.btrace.ext.model.SpanSample;
import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.metering.ScalarSampler;
import org.gridkit.nimble.metering.SpanSampler;
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

    private Client client;

    @Override
    public Void call() throws Exception {
        try {            
            Client client = getClient();
    
            PollSamplesCmdResult result = clientOps.poll(
                client, Collections.<Class<?>>singleton(settings.getScriptClass()), timeoutMs
            );
            
            submit(result);
            
            return null;
        } catch (Exception e) {
            log.error("Error while execiting BTraceProbe with settrings " + settings);
            throw e;
        }
    }
    
    private void submit(PollSamplesCmdResult result) {
        for (PollSamplesCmdResult.Element element : result.getElements()) {
            for (ScalarSample sample : element.getSamples()) {
                submit(sample);
            }
        }
    }
    
    // TODO convert to nanos
    private void submit(ScalarSample rawSample) {        
        if (rawSample instanceof PointSample) {
            PointSample sample = (PointSample)rawSample;
            PointSampler sampler = samplerFactory.getPointSampler(sample.getKey());
            sampler.write(sample.getValue().doubleValue(), sample.getTimestamp());
        } else if (rawSample instanceof SpanSample) {
            SpanSample sample = (SpanSample)rawSample;
            SpanSampler sampler = samplerFactory.getSpanSampler(sample.getKey());
            sampler.write(sample.getValue().doubleValue(), sample.getStartTimestamp(), sample.getFinishTimestamp());
        } else {
            ScalarSampler sampler = samplerFactory.getScalarSampler(rawSample.getKey());
            sampler.write(rawSample.getValue().doubleValue());
        }
    }
    
    private Client getClient() throws Exception {
        if (client == null) {
            try {
                client = clientSource.getClient(pid);
                clientOps.submit(client, settings.getScriptClass(), settings.getArgsArray(), timeoutMs);
            } catch (Exception e) {
                log.error(F("Failed to connect to client with pid %d", pid));
                throw e;
            }
        }
        
        return client;
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
