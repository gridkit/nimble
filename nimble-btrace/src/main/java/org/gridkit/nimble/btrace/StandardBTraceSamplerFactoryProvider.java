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
    public SamplerFactory getProcSampleFactory(long pid) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(BTraceMeasure.PID_KEY, pid);

        return new SchemeSamplerFactory(schema, BTraceMeasure.SAMPLE_KEY);
    }

    @Override
    public BTraceSamplerFactoryProvider attach(MeteringDriver metering) {
        this.globalSchema = metering.getSchema();
        return this;
    }
}
