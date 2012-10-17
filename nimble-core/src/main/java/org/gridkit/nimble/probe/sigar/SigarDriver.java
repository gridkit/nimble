package org.gridkit.nimble.probe.sigar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.probe.ProbeHandle;
import org.gridkit.nimble.probe.ProbeOps;
import org.gridkit.nimble.probe.ProbeOps.SingleProbeFactory;
import org.gridkit.nimble.util.NamedThreadFactory;
import org.hyperic.sigar.SigarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface SigarDriver {
    public PidProvider newCurrentPidProvider();
    
    public PidProvider newPtqlPidProvider(String query);
    
    public ProbeHandle monitorSysCpu(MeteringDriver metering);
    
    public ProbeHandle monitorSysMem(MeteringDriver metering);

    public ProbeHandle monitorSysNet(MeteringDriver metering);
    
    public ProbeHandle monitorProcCpu(PidProvider provider, MeteringDriver metering);

    public ProbeHandle monitorProcMem(PidProvider provider, MeteringDriver metering);
    
    public void stop();
    
    @SuppressWarnings("serial")
    public static class Impl extends SigarHolder implements SigarDriver, Serializable {
        private static final Logger log = LoggerFactory.getLogger(Impl.class);
        
        private transient ScheduledExecutorService executor;
        private final int corePoolSize;
        private final long delayMs;
        private final Random rnd;
        
        public Impl(int corePoolSize, long delay, TimeUnit unit) {
            this.corePoolSize = corePoolSize;
            this.delayMs = Math.max(1, unit.toMillis(delay));
            this.rnd = new Random();
        }

        @Override
        public PidProvider newCurrentPidProvider() {
            return new CurrentPidProvider();
        }

        @Override
        public PidProvider newPtqlPidProvider(String query) {
            return new PtqlPidProvider(query);
        }
        
        @Override
        public ProbeHandle monitorSysCpu(MeteringDriver metering) {      
            Runnable probe = SysCpuProbe.newInstance(metering.getSchema());

            return new SigarProbeHandle(schedule(Collections.singleton(probe)));
        }
        
        @Override
        public ProbeHandle monitorSysMem(MeteringDriver metering) {
            Runnable probe = SysMemProbe.newInstance(metering.getSchema());

            return new SigarProbeHandle(schedule(Collections.singleton(probe)));
        }
        
        @Override
        public ProbeHandle monitorSysNet(MeteringDriver metering) {
            List<String> interfaces = getNetInterfaceList();
             
            List<Runnable> probes = ProbeOps.instantiate(interfaces, NetInterfaceProbe.FACTORY, metering);
            
            return new SigarProbeHandle(schedule(probes));
        }
        
        @Override
        public ProbeHandle monitorProcCpu(PidProvider provider, MeteringDriver metering) {
            return monitorProcess(ProcTimeProbe.FACTORY, provider, metering);
        }
        
        @Override
        public ProbeHandle monitorProcMem(PidProvider provider, MeteringDriver metering) {
            return monitorProcess(ProcMemProbe.FACTORY, provider, metering);
        }
        
        private ProbeHandle monitorProcess(SingleProbeFactory<Long> factory, PidProvider provider, MeteringDriver metering) {
            Collection<Long> pids = provider.getPids();
            
            List<Runnable> probes = ProbeOps.instantiate(pids, factory, metering);
            
            for (Long pid : pids) {
                probes.add(factory.newProbe(pid, metering.getSchema()));
            }
            
            return new SigarProbeHandle(schedule(probes));
        }
        
        private List<Future<?>> schedule(Collection<Runnable> probes) {
            List<Future<?>> futures = new ArrayList<Future<?>>();
            
            for (Runnable probe : probes) {
                Future<?> future = getExecutor().scheduleWithFixedDelay(probe, getInitialDelay(), delayMs, TimeUnit.MILLISECONDS);
                futures.add(future);
            }
            
            return futures;
        }
                
        @Override
        public void stop() {
            getExecutor().shutdownNow();
        }

        private long getInitialDelay() {
            return Math.abs(rnd.nextLong()) % delayMs;
        }
        
        private synchronized ScheduledExecutorService getExecutor() {
            if (executor == null) {
                executor = Executors.newScheduledThreadPool(
                    corePoolSize, new NamedThreadFactory("Pool[" + this.getClass().getSimpleName() + "]", true, Thread.NORM_PRIORITY)
                );
            }
            
            return executor;
        }
        
        private List<String> getNetInterfaceList() {
            try {
                return Arrays.asList(getSigar().getNetInterfaceList());
            } catch (SigarException e) {
                log.error("Failed to retrieve net interfaces list", e);
                return Collections.emptyList();
            }
        }
        
        private static class SigarProbeHandle implements ProbeHandle {
            private final Collection<Future<?>> futures;
            
            public SigarProbeHandle(Collection<Future<?>> futures) {
                this.futures = futures;
            }

            @Override
            public void stop() {
                for (Future<?> future : futures) {
                    future.cancel(true);
                }
            }
        }
    }
}
