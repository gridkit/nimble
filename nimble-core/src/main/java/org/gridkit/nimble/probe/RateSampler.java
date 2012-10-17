package org.gridkit.nimble.probe;

import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.SampleWriter;
import org.gridkit.nimble.util.Pair;

public class RateSampler {
    private final SampleSchema schema;
    private final Object nameKey;
    private final Object typeKey;
    private final Object typeValue;

    private final Map<Object, SampleFactory> factories = new HashMap<Object, SampleFactory>();
    
    private final Map<Object, Pair<Long, Long>> prevValues = new HashMap<Object, Pair<Long, Long>>(); // (value, timestamp)
    
    public RateSampler(SampleSchema schema, Object nameKey, Object typeKey, Object typeValue) {
        this.schema = schema;
        this.nameKey = nameKey;
        this.typeKey = typeKey;
        this.typeValue = typeValue;
    }
    
    public void sample(Object measure, long value, long timestamp) {
        Pair<Long, Long> nextValue = Pair.newPair(value, timestamp);
        
        if (prevValues.containsKey(measure)) {
            Pair<Long, Long> prevValue = prevValues.get(measure);
            
            SampleFactory factory = getFactory(measure);
            
            SampleWriter sample = factory.newSample();
            
            sample.set(Measure.MEASURE,       nextValue.getA() - prevValue.getA());
            sample.set(Measure.TIMESTAMP,     prevValue.getB());
            sample.set(Measure.END_TIMESTAMP, nextValue.getB());

            sample.submit();
        }
        
        prevValues.put(measure, nextValue);
    }
    
    private SampleFactory getFactory(Object measure) {
        SampleFactory factory = factories.get(measure);
        
        if (factory == null) {
            SampleSchema writerSchema = schema.createDerivedScheme();
            
            writerSchema.setStatic(nameKey, measure);
            writerSchema.setStatic(typeKey, typeValue);
            
            writerSchema.declareDynamic(Measure.MEASURE,       long.class);
            writerSchema.declareDynamic(Measure.TIMESTAMP,     long.class);
            writerSchema.declareDynamic(Measure.END_TIMESTAMP, long.class);

            factory = writerSchema.createFactory();
            
            factories.put(measure, factory);
        }
        
        return factory;
    }
}
