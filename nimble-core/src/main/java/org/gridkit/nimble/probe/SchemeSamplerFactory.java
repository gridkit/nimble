package org.gridkit.nimble.probe;

import java.util.Map;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleWriter;
import org.gridkit.nimble.metering.ScalarSampler;
import org.gridkit.nimble.metering.SpanSampler;

public class SchemeSamplerFactory implements SamplerFactory {
    private final SampleSchema schema;
    private final String samplerKey;
    
    public SchemeSamplerFactory(SampleSchema schema, String samplerKey, Map<Object, Object> globals) {
        this.schema = schema.createDerivedScheme();
        this.samplerKey = samplerKey;
        
        for (Map.Entry<Object, Object> global : globals.entrySet()) {
            schema.setStatic(global.getKey(), global.getValue());
        }
    }

    @Override
    public ScalarSampler getScalarSampler(String key) {
        SampleSchema samplerSchema = schema.createDerivedScheme();
        
        samplerSchema.setStatic(samplerKey, key);
        samplerSchema.declareDynamic(Measure.MEASURE, double.class);
        
        final SampleFactory factory = samplerSchema.createFactory();
        
        return new ScalarSampler() {
            @Override
            public void write(double value) {
                SampleWriter sample = factory.newSample();
                
                sample.set(Measure.MEASURE, value);
                
                sample.submit();
            }
        };
    }

    @Override
    public PointSampler getPointSampler(String key) {
        SampleSchema samplerSchema = schema.createDerivedScheme();
        
        samplerSchema.setStatic(samplerKey, key);
        samplerSchema.declareDynamic(Measure.MEASURE, double.class);
        samplerSchema.declareDynamic(Measure.TIMESTAMP, long.class);
        
        final SampleFactory factory = samplerSchema.createFactory();
        
        return new PointSampler() {
            @Override
            public void write(double value, long nanotimestamp) {
                SampleWriter sample = factory.newSample();
                
                sample.set(Measure.MEASURE, value);
                sample.set(Measure.TIMESTAMP, nanotimestamp);
                
                sample.submit();
            }
        };
    }

    @Override
    public SpanSampler getSpanSampler(String key) {
        SampleSchema samplerSchema = schema.createDerivedScheme();
        
        samplerSchema.setStatic(samplerKey, key);
        samplerSchema.declareDynamic(Measure.MEASURE, double.class);
        samplerSchema.declareDynamic(Measure.TIMESTAMP, long.class);
        samplerSchema.declareDynamic(Measure.END_TIMESTAMP, long.class);
        
        final SampleFactory factory = samplerSchema.createFactory();
        
        return new SpanSampler() {
            @Override
            public void write(double value, long nanoStart, long nanoFinish) {
                SampleWriter sample = factory.newSample();
                
                sample.set(Measure.MEASURE, value);
                sample.set(Measure.TIMESTAMP, nanoStart);
                sample.set(Measure.END_TIMESTAMP, nanoFinish);
                
                sample.submit();
            }
        };
    }
}
