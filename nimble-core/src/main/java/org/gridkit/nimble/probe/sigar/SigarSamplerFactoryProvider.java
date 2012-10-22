package org.gridkit.nimble.probe.sigar;

import java.io.Serializable;

import org.gridkit.nimble.driver.MeteringAware;
import org.gridkit.nimble.probe.SamplerFactory;

public interface SigarSamplerFactoryProvider extends MeteringAware<SigarSamplerFactoryProvider>, Serializable {
    SamplerFactory getProcMemSampleFactory(long pid);
    
    SamplerFactory getProcCpuSampleFactory(long pid);
    
    SamplerFactory getNetInterfaceSampleFactory(String interfaceName);
    
    SamplerFactory getSysCpuSampleFactory();
    
    SamplerFactory getSysMemSampleFactory();
}
