package org.gridkit.nimble.probe;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleWriter;
import org.gridkit.nimble.metering.ScalarSampler;
import org.gridkit.nimble.metering.SpanSampler;

public class SchemeSamplerFactory implements SamplerFactory {
    private final SampleSchema schema;
    private final String samplerKeyName;
    
    public SchemeSamplerFactory(SampleSchema schema, String samplerKeyName) {
        this.schema = schema.createDerivedScheme();
        this.samplerKeyName = samplerKeyName;
    }

    @Override
    public ScalarSampler getScalarSampler(String key) {
        SampleSchema samplerSchema = schema.createDerivedScheme();
        
        samplerSchema.setStatic(samplerKeyName, key);
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
        
        samplerSchema.setStatic(samplerKeyName, key);
        samplerSchema.declareDynamic(Measure.MEASURE, double.class);
        samplerSchema.declareDynamic(Measure.TIMESTAMP, long.class);
        
        final SampleFactory factory = samplerSchema.createFactory();
        
        return new PointSampler() {
            @Override
            public void write(double value, long nanotimestamp) {
                SampleWriter sample = factory.newSample();
                
                sample.set(Measure.MEASURE, value);
                sample.setTimestamp(nanotimestamp);
                
                sample.submit();
            }
        };
    }

    @Override
    public SpanSampler getSpanSampler(String key) {
        SampleSchema samplerSchema = schema.createDerivedScheme();
        
        samplerSchema.setStatic(samplerKeyName, key);
        samplerSchema.declareDynamic(Measure.MEASURE, double.class);
        samplerSchema.declareDynamic(Measure.TIMESTAMP, long.class);
        samplerSchema.declareDynamic(Measure.END_TIMESTAMP, long.class);
        
        final SampleFactory factory = samplerSchema.createFactory();
        
        return new SpanSampler() {
            @Override
            public void write(double value, long nanoStart, long nanoFinish) {
                SampleWriter sample = factory.newSample();
                
                sample.set(Measure.MEASURE, value);
                sample.setTimeBounds(nanoStart, nanoFinish);
                
                sample.submit();
            }
        };
    }
}
