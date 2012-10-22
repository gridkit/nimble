package org.gridkit.nimble.btrace;

import java.io.Serializable;

import org.gridkit.nimble.driver.MeteringAware;
import org.gridkit.nimble.probe.SamplerFactory;

public interface BTraceSamplerFactoryProvider extends MeteringAware, Serializable {
    SamplerFactory getProcSampleFactory(long pid);
}
