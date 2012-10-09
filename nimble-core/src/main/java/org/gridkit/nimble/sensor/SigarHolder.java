package org.gridkit.nimble.sensor;

import java.io.Serializable;

import org.gridkit.lab.sigar.SigarFactory;
import org.hyperic.sigar.Sigar;

@SuppressWarnings("serial")
public abstract class SigarHolder implements Serializable {
    private transient Sigar sigar;
    
    protected Sigar getSigar() {
        if (sigar == null) {
            sigar = SigarFactory.newSigar();
        }
        return sigar;
    }
}
