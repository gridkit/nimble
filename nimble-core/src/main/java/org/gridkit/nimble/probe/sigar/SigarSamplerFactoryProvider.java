package org.gridkit.nimble.probe.sigar;

import org.gridkit.nimble.probe.SamplerFactory;

public interface SigarSamplerFactoryProvider {
    SamplerFactory getProcMemSampleFactory(long pid);
    
    SamplerFactory getProcCpuSampleFactory(long pid);
    
    SamplerFactory getNetInterfaceSampleFactory(String interfaceName);
    
    SamplerFactory getSysCpuSampleFactory();
    
    SamplerFactory getSysMemSampleFactory();
}
