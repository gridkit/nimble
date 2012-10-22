package org.gridkit.nimble.probe.sigar;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.gridkit.nimble.driver.MeteringAware;
import org.gridkit.nimble.driver.MeteringSink;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.probe.ProbeHandle;
import org.gridkit.nimble.probe.ProbeOps;
import org.gridkit.nimble.probe.ProbeOps.SingleProbeFactory;
import org.gridkit.nimble.probe.SamplerFactory;
import org.gridkit.nimble.util.NamedThreadFactory;
import org.gridkit.nimble.util.Pair;
import org.gridkit.nimble.util.SafeCallable;
import org.hyperic.sigar.SigarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface SigarDriver {
    public PidProvider newCurrentPidProvider();
    
    public PidProvider newPtqlPidProvider(String query);
    
    public ProbeHandle monitorSysCpu(MeteringSink<SigarSamplerFactoryProvider> provider);
    
    public ProbeHandle monitorSysMem(MeteringSink<SigarSamplerFactoryProvider> provider);

    public ProbeHandle monitorSysNet(MeteringSink<SigarSamplerFactoryProvider> provider);
    
    public ProbeHandle monitorProcCpu(PidProvider pidProvider, MeteringSink<SigarSamplerFactoryProvider> factoryProvider);

    public ProbeHandle monitorProcMem(PidProvider pidProvider, MeteringSink<SigarSamplerFactoryProvider> factoryProvider);
    
    public void stop();
    
    @SuppressWarnings("serial")
    public static class Impl extends SigarHolder implements SigarDriver, Serializable {
        private static final Logger log = LoggerFactory.getLogger(Impl.class);
        
        private transient ScheduledExecutorService executor;
        private final int corePoolSize;
        private final long delayMs;

        public Impl(int corePoolSize, long delayMs) {
            this.corePoolSize = corePoolSize;
            this.delayMs = delayMs;
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
        public ProbeHandle monitorSysCpu(MeteringSink<SigarSamplerFactoryProvider> provider) {      
            Runnable probe = new SafeCallable<Void>(new SysCpuProbe(provider.getSink().getSysCpuSampleFactory()));

            return ProbeOps.schedule(Collections.singleton(probe), getExecutor(), delayMs);
        }
        
        @Override
        public ProbeHandle monitorSysMem(MeteringSink<SigarSamplerFactoryProvider> provider) {
            Runnable probe = new SafeCallable<Void>(new SysMemProbe(provider.getSink().getSysMemSampleFactory()));

            return ProbeOps.schedule(Collections.singleton(probe), getExecutor(), delayMs);
        }
        
        @Override
        public ProbeHandle monitorSysNet(MeteringSink<SigarSamplerFactoryProvider> provider) {
            List<String> interfaces = getNetInterfaceList();
             
            Collection<Pair<String, SigarSamplerFactoryProvider>> params = ProbeOps.with(interfaces, provider.getSink());
            
            List<Runnable> probes = ProbeOps.instantiate(params, new NetInterfaceFactory());
            
            return ProbeOps.schedule(probes, getExecutor(), delayMs);
        }
        
        @Override
        public ProbeHandle monitorProcCpu(PidProvider pidProvider, MeteringSink<SigarSamplerFactoryProvider> factoryProvider) {
            return monitorProcess(new ProcCpuFactory(), pidProvider, factoryProvider);
        }
        
        @Override
        public ProbeHandle monitorProcMem(PidProvider pidProvider, MeteringSink<SigarSamplerFactoryProvider> factoryProvider) {
            return monitorProcess(new ProcMemFactory(), pidProvider, factoryProvider);
        }
        
        private ProbeHandle monitorProcess(SingleProbeFactory<Pair<Long, SigarSamplerFactoryProvider>> factory,
                                           PidProvider pidProvider, MeteringSink<SigarSamplerFactoryProvider> factoryProvider) {
            Collection<Long> pids = pidProvider.getPids();
            
            Collection<Pair<Long, SigarSamplerFactoryProvider>> params = ProbeOps.with(pids, factoryProvider.getSink());
            
            List<Runnable> probes = ProbeOps.instantiate(params, factory);
                        
            return ProbeOps.schedule(probes, getExecutor(), delayMs);
        }
   
        @Override
        public void stop() {
            getExecutor().shutdownNow();
        }
        
        private synchronized ScheduledExecutorService getExecutor() {
            if (executor == null) {
                executor = Executors.newScheduledThreadPool(
                    corePoolSize, new NamedThreadFactory("Pool[" + this.getClass().getSimpleName() + "]", true, Thread.NORM_PRIORITY)
                );
            }
            
            return executor;
        }
        
        public static MeteringAware<SigarSamplerFactoryProvider> newStandardSamplerFactoryProvider() {
            return new StandardSigarSamplerFactoryProvider();
        }
        
        private List<String> getNetInterfaceList() {
            try {
                return Arrays.asList(getSigar().getNetInterfaceList());
            } catch (SigarException e) {
                log.error("Failed to retrieve net interfaces list", e);
                return Collections.emptyList();
            }
        }
        
        private static class ProcMemFactory implements SingleProbeFactory<Pair<Long, SigarSamplerFactoryProvider>> {
            @Override
            public Runnable newProbe(Pair<Long, SigarSamplerFactoryProvider> param) {
                SamplerFactory factory = param.getB().getProcMemSampleFactory(param.getA());
                return new SafeCallable<Void>(new ProcMemProbe(param.getA(), factory));
            }
        }
        
        private static class ProcCpuFactory implements SingleProbeFactory<Pair<Long, SigarSamplerFactoryProvider>> {
            @Override
            public Runnable newProbe(Pair<Long, SigarSamplerFactoryProvider> param) {
                SamplerFactory factory = param.getB().getProcCpuSampleFactory(param.getA());
                return new SafeCallable<Void>(new ProcCpuProbe(param.getA(), factory));
            }
        };
        
        private static class NetInterfaceFactory implements SingleProbeFactory<Pair<String, SigarSamplerFactoryProvider>> {
            @Override
            public Runnable newProbe(Pair<String, SigarSamplerFactoryProvider> param) {
                SamplerFactory factory = param.getB().getNetInterfaceSampleFactory(param.getA());
                return new SafeCallable<Void>(new NetInterfaceProbe(param.getA(), factory));
            }
        };
    }
}
