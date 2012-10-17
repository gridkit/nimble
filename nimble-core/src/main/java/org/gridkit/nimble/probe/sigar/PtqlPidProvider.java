package org.gridkit.nimble.probe.sigar;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.gridkit.nimble.probe.PidProvider;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http://support.hyperic.com/display/SIGAR/PTQL
 */
public class PtqlPidProvider extends SigarHolder implements PidProvider {
    private static final Logger log = LoggerFactory.getLogger(PtqlPidProvider.class);
    
    private final String query;

    public PtqlPidProvider(String query) {
        this.query = query;
    }

    @Override
    public Collection<Long> getPids() {
        List<Long> pids = new ArrayList<Long>();
        
        try {
            for (long pid : ProcessFinder.find(getSigar(), query)) {
                pids.add(pid);
            }
        } catch (SigarException e) {
            log.error(F("Error while getting processes CPU usage by query '%s'", query), e);
            return Collections.emptySet();
        }

        return pids;
    }
}
