package org.gridkit.nimble.probe.sigar;

import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.probe.SamplerFactory;
import org.gridkit.nimble.probe.SchemeSamplerFactory;

public class StandardSigarSamplerFactoryProvider implements SigarSamplerFactoryProvider {
    private static final long serialVersionUID = -5760137045611926786L;
    
    private SampleSchema globalSchema;
    
    @Override
    public SamplerFactory getProcMemSampleFactory(long pid) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.PROC_MEM_PROBE);
        schema.setStatic(SigarMeasure.PID_KEY, pid);
        
        return new SchemeSamplerFactory(schema, SigarMeasure.MEASURE_KEY);
    }

    @Override
    public SamplerFactory getProcCpuSampleFactory(long pid) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.PROC_CPU_PROBE);
        schema.setStatic(SigarMeasure.PID_KEY, pid);
        
        return new SchemeSamplerFactory(schema, SigarMeasure.MEASURE_KEY);
    }

    @Override
    public SamplerFactory getNetInterfaceSampleFactory(String interfaceName) {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.SYS_NET_PROBE);
        schema.setStatic(SigarMeasure.NET_INTERFACE_KEY, interfaceName);
        
        return new SchemeSamplerFactory(schema, SigarMeasure.MEASURE_KEY);
    }

    @Override
    public SamplerFactory getSysCpuSampleFactory() {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.SYS_CPU_PROBE);
        
        return new SchemeSamplerFactory(schema, SigarMeasure.MEASURE_KEY);
    }

    @Override
    public SamplerFactory getSysMemSampleFactory() {
        SampleSchema schema = globalSchema.createDerivedScheme();
        
        schema.setStatic(SigarMeasure.PROBE_KEY, SigarMeasure.SYS_MEM_PROBE);
        
        return new SchemeSamplerFactory(schema, SigarMeasure.MEASURE_KEY);
    }
    
    @Override
    public void setMetering(MeteringDriver metering) {
        this.globalSchema = metering.getSchema();
    }
}
