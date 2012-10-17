package org.gridkit.nimble.probe.sigar;

import java.util.Collection;
import java.util.Collections;

import org.gridkit.nimble.probe.PidProvider;

public class CurrentPidProvider extends SigarHolder implements PidProvider {
    @Override
    public Collection<Long> getPids() {
        return Collections.singleton(getSigar().getPid());
    }
}
