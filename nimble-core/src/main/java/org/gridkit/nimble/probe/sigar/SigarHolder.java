package org.gridkit.nimble.probe.sigar;

import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.Sigar;

public abstract class SigarHolder {
    private transient Sigar sigar;
    
    protected synchronized Sigar getSigar() {
        if (sigar == null) {
            sigar = SigarFactory.newSigar();
        }
        return sigar;
    }
}
