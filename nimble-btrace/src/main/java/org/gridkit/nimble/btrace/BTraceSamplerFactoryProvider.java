package org.gridkit.nimble.btrace;

import org.gridkit.nimble.probe.SamplerFactory;

public interface BTraceSamplerFactoryProvider {
    /**
     * Used to report samples from users scripts
     */
    SamplerFactory getUserSampleFactory(long pid, Class<?> scriptClass, String sampleStore);
    
    /**
     * Used to report statistics about received and missed samples
     */
    SamplerFactory getProbeSamplerFactory(long pid, Class<?> scriptClass, String sampleStore);
}
