package org.gridkit.nimble.probe.sigar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.gridkit.lab.monitoring.probe.TargetLocator;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http://support.hyperic.com/display/SIGAR/PTQL
 */
public class PtqlPidProvider extends SigarHolder implements TargetLocator<Long>, Serializable {

	private static final long serialVersionUID = 20130114L;

	private static final Logger log = LoggerFactory.getLogger(PtqlPidProvider.class);
    
    private final String query;

    public PtqlPidProvider(String query) {
        this.query = query;
    }
    
    
    @Override
	public Collection<Long> findTargets() {
        List<Long> pids = new ArrayList<Long>();
        
        try {
            for (long pid : ProcessFinder.find(getSigar(), query)) {
                pids.add(pid);
            }
        } catch (SigarException e) {
            log.error("Error while getting processes CPU usage by query '" + query + "'", e);
            return Collections.emptySet();
        }

        return pids;
    }
}
