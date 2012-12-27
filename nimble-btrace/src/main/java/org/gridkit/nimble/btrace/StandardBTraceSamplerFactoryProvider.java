package org.gridkit.nimble.btrace;

import java.io.Serializable;

import org.gridkit.nimble.driver.MeteringAware;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.metering.PointSampler;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.metering.ScalarSampler;
import org.gridkit.nimble.metering.SpanSampler;
import org.gridkit.nimble.probe.SamplerFactory;
import org.gridkit.nimble.probe.SchemeSamplerFactory;

public class StandardBTraceSamplerFactoryProvider implements BTraceSamplerFactoryProvider, MeteringAware<BTraceSamplerFactoryProvider>, Serializable {
    private static final long serialVersionUID = -8634420203507579307L;

    private SampleSchema globalSchema;

    @Override
    public SamplerFactory getUserSampleFactory(long pid, Class<?> scriptClass, String sampleStore) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(BTraceMeasure.PID_KEY, pid);
        schema.setStatic(BTraceMeasure.SCRIPT_KEY, scriptClass.getName());
        schema.setStatic(BTraceMeasure.STORE_KEY, sampleStore);
        
        return newUserSampleFactory(pid, scriptClass, sampleStore, schema);
    }

    @Override
    public SamplerFactory getProbeSamplerFactory(long pid, Class<?> scriptClass, String sampleStore) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(BTraceMeasure.PID_KEY, pid);
        schema.setStatic(BTraceMeasure.SCRIPT_KEY, scriptClass.getName());
        schema.setStatic(BTraceMeasure.STORE_KEY, sampleStore);
                
        return newProbeSamplerFactory(pid, scriptClass, sampleStore, schema);
    }
    
    protected SamplerFactory newUserSampleFactory(long pid, Class<?> scriptClass, String sampleStore, SampleSchema schema) {
        return new SchemeSamplerFactory(schema, BTraceMeasure.SAMPLE_KEY) {
            @Override
            protected ScalarSampler newScalarSampler(String key, SampleSchema schema) {
                schema.setStatic(BTraceMeasure.SAMPLE_TYPE_KEY, BTraceMeasure.SAMPLE_TYPE_SCALAR);
                return super.newScalarSampler(key, schema);
            }
            
            @Override
            protected PointSampler newPointSampler(String key, SampleSchema schema) {
                schema.setStatic(BTraceMeasure.SAMPLE_TYPE_KEY, BTraceMeasure.SAMPLE_TYPE_POINT);
                return super.newPointSampler(key, schema);
            }
            
            @Override
            protected SpanSampler newSpanSampler(String key, SampleSchema schema) {
                schema.setStatic(BTraceMeasure.SAMPLE_TYPE_KEY, BTraceMeasure.SAMPLE_TYPE_SPAN);
                return super.newSpanSampler(key, schema);
            }
        };
    }
    
    protected SamplerFactory newProbeSamplerFactory(long pid, Class<?> scriptClass, String sampleStore, SampleSchema schema) {
        return new SchemeSamplerFactory(schema, BTraceMeasure.SAMPLE_TYPE_KEY);
    }
    
    protected SampleSchema getGlobalSchema() {
        return globalSchema;
    }
    
    @Override
    public BTraceSamplerFactoryProvider attach(MeteringDriver metering) {
        this.globalSchema = metering.getSchema();
        return this;
    }
}
