package org.gridkit.nimble.btrace;

import org.gridkit.nimble.probe.SamplerFactory;

public interface BTraceSamplerFactoryProvider {
    SamplerFactory getProcSampleFactory(long pid);
}
