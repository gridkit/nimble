package org.gridkit.nimble.probe.sigar;

import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.SigarProxy;

public abstract class SigarHolder {
    private transient SigarProxy sigar;
    
    protected synchronized SigarProxy getSigar() {
        if (sigar == null) {
            sigar = SigarFactory.newSigar();
        }
        return sigar;
    }
}
