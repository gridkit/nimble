package org.gridkit.nimble.sensor;

import java.io.Serializable;

import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.SigarProxy;

@SuppressWarnings("serial")
public abstract class SigarHolder implements Serializable {
    private transient SigarProxy sigar;
    
    protected SigarProxy getSigar() {
        if (sigar == null) {
            sigar = SigarFactory.newSigar();
        }
        return sigar;
    }
}
