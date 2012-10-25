package org.gridkit.nimble.btrace;

import java.io.Serializable;

import org.gridkit.nimble.driver.MeteringAware;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.SamplerFactory;
import org.gridkit.nimble.probe.SchemeSamplerFactory;

public class StandardBTraceSamplerFactoryProvider implements BTraceSamplerFactoryProvider, MeteringAware<BTraceSamplerFactoryProvider>, Serializable {
    private static final long serialVersionUID = -8634420203507579307L;

    private SampleSchema globalSchema;

    @Override
    public SamplerFactory getReceivedSampleFactory(long pid, Class<?> scriptClass, String sampleStore) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(BTraceMeasure.PID_KEY, pid);
        schema.setStatic(BTraceMeasure.TYPE_KEY, BTraceMeasure.TYPE_RECEIVED);
        schema.setStatic(BTraceMeasure.SCRIPT_KEY, scriptClass.getName());
        schema.setStatic(BTraceMeasure.STORE_KEY, sampleStore);

        postprocessSchema(schema);
        
        return new SchemeSamplerFactory(schema, BTraceMeasure.SAMPLE_KEY);
    }

    @Override
    public SamplerFactory getMissedSamplerFactory(long pid, Class<?> scriptClass) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(BTraceMeasure.PID_KEY, pid);
        schema.setStatic(BTraceMeasure.TYPE_KEY, BTraceMeasure.TYPE_MISSED);
        schema.setStatic(BTraceMeasure.SCRIPT_KEY, scriptClass.getName());

        postprocessSchema(schema);
        
        return new SchemeSamplerFactory(schema, BTraceMeasure.STORE_KEY);
    }

    @Override
    public BTraceSamplerFactoryProvider attach(MeteringDriver metering) {
        this.globalSchema = metering.getSchema();
        return this;
    }
    
    protected void postprocessSchema(SampleSchema schema) {

    }
}
