package org.gridkit.nimble.btrace;

import static org.gridkit.nimble.util.StringOps.F;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

import net.java.btrace.client.Client;

import org.gridkit.nimble.btrace.ext.PollSamplesCmdResult;
import org.gridkit.nimble.btrace.ext.model.Sample;
import org.gridkit.nimble.btrace.ext.model.SpanSample;
import org.gridkit.nimble.btrace.ext.model.TimestampSample;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BTraceProbe implements Callable<Void> {
    private static final Logger log = LoggerFactory.getLogger(BTraceProbe.class);
    
    private long pid;
    private BTraceClientOps clientOps;
    private BTraceClientSource clientSource;
    private long timeoutMs;
    
    private SampleFactory sampleFactory;
    private SampleFactory timestampSampleFactory;
    private SampleFactory spanSampleFactory;

    private BTraceProbe() {}

    @Override
    public Void call() throws ClientCreateException, InterruptedException, IOException {
        Client client = null;
        
        try {
            client = clientSource.getClient(pid);
        } catch (ClientCreateException e) {
            log.error(F("Failed to connect to client with pid %d", pid));
            throw e;
        }
        
        Map<String, PollSamplesCmdResult<?>> result = null;
        
        try {
            result = clientOps.poll(client, timeoutMs);
        } catch (IOException e) {
            log.error(F("Failed to poll samples form client with pid %d", pid));
            throw e;
        }
        
        for (Map.Entry<String, PollSamplesCmdResult<?>> sampleSet : result.entrySet()) {
            submit(sampleSet.getValue(), sampleSet.getKey());
        }
        
        return null;
    }
    
    private void submit(PollSamplesCmdResult<?> sampleSet, String sampleStore) {
        for (Sample sample : sampleSet.getSamples()) {
            if (sample instanceof TimestampSample) {
                submit((TimestampSample)sample, sampleStore);
            } else if (sample instanceof SpanSample) {
                submit((SpanSample)sample, sampleStore);
            } else {
                submit(sample, sampleStore);
            }
        }
    }
    
    private void submit(Sample sample, String sampleStore) {
        SampleWriter writer = sampleFactory.newSample();
        
        writer.set(BTraceMeasure.SAMPLE_STORE_KEY, sampleStore);
        writer.set(Measure.MEASURE, sample.getValue().doubleValue());
        
        writer.submit();
    }
    
    private void submit(TimestampSample sample, String sampleStore) {
        SampleWriter writer = timestampSampleFactory.newSample();
        
        writer.set(BTraceMeasure.SAMPLE_STORE_KEY, sampleStore);
        writer.set(Measure.MEASURE, sample.getValue().doubleValue());
        writer.set(Measure.TIMESTAMP, sample.getTimestamp());
        
        writer.submit();
    }
    
    private void submit(SpanSample sample, String sampleStore) {
        SampleWriter writer = spanSampleFactory.newSample();
        
        writer.set(BTraceMeasure.SAMPLE_STORE_KEY, sampleStore);
        writer.set(Measure.MEASURE, sample.getValue().doubleValue());
        writer.set(Measure.TIMESTAMP, sample.getStartTimestamp());
        writer.set(Measure.END_TIMESTAMP, sample.getFinishTimestamp());
        
        writer.submit();
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public void setClientOps(BTraceClientOps clientOps) {
        this.clientOps = clientOps;
    }

    public void setClientSource(BTraceClientSource clientSource) {
        this.clientSource = clientSource;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public void setSampleFactory(SampleFactory sampleFactory) {
        this.sampleFactory = sampleFactory;
    }

    public void setTimestampSampleFactory(SampleFactory timestampSampleFactory) {
        this.timestampSampleFactory = timestampSampleFactory;
    }

    public void setSpanSampleFactory(SampleFactory spanSampleFactory) {
        this.spanSampleFactory = spanSampleFactory;
    }
}
