package org.gridkit.nimble.probe;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleWriter;
import org.gridkit.nimble.metering.ScalarSampler;
import org.gridkit.nimble.metering.SpanSampler;
import org.gridkit.nimble.util.Seconds;

public class SchemeSamplerFactory implements SamplerFactory {
    private final SampleSchema globalSchema;
    private final String samplerKey;
    
    public SchemeSamplerFactory(SampleSchema schema, String samplerKey) {
        this.globalSchema = schema.createDerivedScheme();
        this.samplerKey = samplerKey;
    }

    @Override
    public ScalarSampler getScalarSampler(String key) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(samplerKey, key);
        schema.declareDynamic(Measure.MEASURE, double.class);
        
        return newScalarSampler(key, schema);
    }

    @Override
    public PointSampler getPointSampler(String key) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(samplerKey, key);
        schema.declareDynamic(Measure.MEASURE, double.class);
        schema.declareDynamic(Measure.TIMESTAMP, long.class);
        
        return newPointSampler(key, schema);
    }

    @Override
    public SpanSampler getSpanSampler(String key) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(samplerKey, key);
        schema.declareDynamic(Measure.MEASURE, double.class);
        schema.declareDynamic(Measure.TIMESTAMP, double.class);
        schema.declareDynamic(Measure.DURATION, double.class);
        
        return newSpanSampler(key, schema);
    }

    protected ScalarSampler newScalarSampler(String key, SampleSchema schema) {
        final SampleFactory factory = schema.createFactory();
        
        return new ScalarSampler() {
            @Override
            public void write(double value) {
                SampleWriter sample = factory.newSample();
                
                sample.set(Measure.MEASURE, value);
                
                sample.submit();
            }
        };
    }
    
    protected PointSampler newPointSampler(String key, SampleSchema schema) {
        final SampleFactory factory = schema.createFactory();
        
        return new PointSampler() {
            @Override
            public void write(double value, double timestampS) {
                SampleWriter sample = factory.newSample();
                
                sample.set(Measure.MEASURE, value);
                sample.setTimestamp((long)Seconds.toNanos(timestampS));
                
                sample.submit();
            }
        };
    }
    
    protected SpanSampler newSpanSampler(String key, SampleSchema schema) {
        final SampleFactory factory = schema.createFactory();
        
        return new SpanSampler() {
            @Override
            public void write(double value, double timestampS, double durationS) {
                SampleWriter sample = factory.newSample();
                
                sample.set(Measure.MEASURE, value);
                sample.setTimeAndDuration(timestampS, durationS);
                
                sample.submit();
            }
        };
    }
}
