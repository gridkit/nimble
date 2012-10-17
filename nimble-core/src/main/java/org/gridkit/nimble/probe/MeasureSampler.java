package org.gridkit.nimble.probe;

import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleWriter;

public class MeasureSampler {
    private final SampleSchema schema;
    private final Object typeKey;
    private final Object typeValue;
    
    private final Map<Object, SampleFactory> factories = new HashMap<Object, SampleFactory>();
        
    public MeasureSampler(SampleSchema schema, Object typeKey, Object typeValue) {
        this.schema = schema;
        this.typeKey = typeKey;
        this.typeValue = typeValue;
    }
    
    public void sample(Object measure, long value, long timestamp) {            
        SampleFactory factory = getFactory(measure);
        
        SampleWriter sample = factory.newSample();
        
        sample.set(Measure.MEASURE,   measure);
        sample.set(Measure.TIMESTAMP, timestamp);

        sample.submit();
    }
    
    private SampleFactory getFactory(Object measure) {
        SampleFactory factory = factories.get(measure);
        
        if (factory == null) {
            SampleSchema writerSchema = schema.createDerivedScheme();
            
            writerSchema.setStatic(typeKey, typeValue);
            
            writerSchema.declareDynamic(Measure.MEASURE,   long.class);
            writerSchema.declareDynamic(Measure.TIMESTAMP, long.class);

            factory = writerSchema.createFactory();
            
            factories.put(measure, factory);
        }
        
        return factory;
    }
}
