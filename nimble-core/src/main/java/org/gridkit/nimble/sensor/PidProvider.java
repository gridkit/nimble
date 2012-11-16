package org.gridkit.nimble.sensor;

import static org.gridkit.nimble.util.StringOps.F;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface PidProvider {
    Collection<Long> getPids();
    
    @SuppressWarnings("serial")
    public static class CurPidProvider extends SigarHolder implements PidProvider {
        @Override
        public Collection<Long> getPids() {
            return Collections.singleton(getSigar().getPid());
        }
    }

    /**
     * http://support.hyperic.com/display/SIGAR/PTQL
     */
    @SuppressWarnings("serial")
    public static class PtqlPidProvider extends SigarHolder implements PidProvider {
        private static final Logger log = LoggerFactory.getLogger(PtqlPidProvider.class);
        
        private String query;

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
                return null;
            }

            return pids;
        }
    }
    
    
    @SuppressWarnings("serial")
    public static class CompositePidProvider implements PidProvider, Serializable {
        private Collection<PidProvider> pidProviders;

        public CompositePidProvider(Collection<PidProvider> pidProviders) {
            this.pidProviders = pidProviders;
        }

        public CompositePidProvider(PidProvider... pidProviders) {
            this(Arrays.asList(pidProviders));
        }

        @Override
        public Collection<Long> getPids() {
            Collection<Long> result = new HashSet<Long>();
            
            for (PidProvider pidProvider : pidProviders) {
                result.addAll(pidProvider.getPids());
            }
            
            return result;
        }
    }
}
