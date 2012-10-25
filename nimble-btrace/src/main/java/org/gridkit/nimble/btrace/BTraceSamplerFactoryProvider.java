package org.gridkit.nimble.btrace;

import org.gridkit.nimble.probe.SamplerFactory;

public interface BTraceSamplerFactoryProvider {
    SamplerFactory getReceivedSampleFactory(long pid, Class<?> scriptClass, String sampleStore);
    
    SamplerFactory getMissedSamplerFactory(long pid, Class<?> scriptClass);
}
