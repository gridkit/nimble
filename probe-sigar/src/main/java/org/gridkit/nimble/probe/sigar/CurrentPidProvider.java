package org.gridkit.nimble.probe.sigar;

import java.util.Collection;
import java.util.Collections;

import org.gridkit.lab.monitoring.probe.TargetLocator;

public class CurrentPidProvider extends SigarHolder implements TargetLocator<Long> {
    @Override
    public Collection<Long> findTargets() {
        return Collections.singleton(getSigar().getPid());
    }
}
