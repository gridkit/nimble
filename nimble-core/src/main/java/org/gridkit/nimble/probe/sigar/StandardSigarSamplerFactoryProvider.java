package org.gridkit.nimble.probe.sigar;

import java.io.Serializable;

import org.gridkit.nimble.driver.MeteringAware;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.SamplerFactory;
import org.gridkit.nimble.probe.SchemeSamplerFactory;

public class StandardSigarSamplerFactoryProvider implements SigarSamplerFactoryProvider, MeteringAware<SigarSamplerFactoryProvider>, Serializable {
    private static final long serialVersionUID = -5760137045611926786L;
    
    private SampleSchema globalSchema;
    
    @Override
    public SamplerFactory getProcMemSampleFactory(long pid) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.PROC_MEM_PROBE);
        schema.setStatic(SigarMeasure.PID_KEY, pid);
        
        postprocessSchema(schema);
        
        return new SchemeSamplerFactory(schema, SigarMeasure.MEASURE_KEY);
    }

    @Override
    public SamplerFactory getProcCpuSampleFactory(long pid) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.PROC_CPU_PROBE);
        schema.setStatic(SigarMeasure.PID_KEY, pid);
        
        postprocessSchema(schema);
        
        return new SchemeSamplerFactory(schema, SigarMeasure.MEASURE_KEY);
    }

    @Override
    public SamplerFactory getNetInterfaceSampleFactory(String interfaceName) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.SYS_NET_PROBE);
        schema.setStatic(SigarMeasure.NET_INTERFACE_KEY, interfaceName);
        
        postprocessSchema(schema);
        
        return new SchemeSamplerFactory(schema, SigarMeasure.MEASURE_KEY);
    }

    @Override
    public SamplerFactory getSysCpuSampleFactory() {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.SYS_CPU_PROBE);
        
        postprocessSchema(schema);
        
        return new SchemeSamplerFactory(schema, SigarMeasure.MEASURE_KEY);
    }

    @Override
    public SamplerFactory getSysMemSampleFactory() {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.SYS_MEM_PROBE);
        
        postprocessSchema(schema);
        
        return new SchemeSamplerFactory(schema, SigarMeasure.MEASURE_KEY);
    }
    
    protected void postprocessSchema(SampleSchema schema) {
        
    }
    
    @Override
    public StandardSigarSamplerFactoryProvider attach(MeteringDriver metering) {
        this.globalSchema = metering.getSchema();
        return this;
    }
}
