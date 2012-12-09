package org.gridkit.nimble.btrace;

import static org.gridkit.nimble.util.StringOps.F;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.gridkit.nimble.driver.MeteringSink;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.probe.ProbeHandle;
import org.gridkit.nimble.probe.ProbeOps;
import org.gridkit.nimble.util.NamedThreadFactory;
import org.gridkit.nimble.util.RunnableAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface BTraceDriver {
    ProbeHandle trace(PidProvider pidProvider, Class<?> scriptClass, MeteringSink<BTraceSamplerFactoryProvider> factoryProvider);
    
    ProbeHandle trace(PidProvider pidProvider, BTraceScriptSettings settings, MeteringSink<BTraceSamplerFactoryProvider> factoryProvider);
    
    void stop();
    
    public static class Impl implements BTraceDriver, Serializable {
        private static final long serialVersionUID = -7698303441795919196L;
        private static final Logger log = LoggerFactory.getLogger(Impl.class);

        private final int corePoolSize;
        private final long pollDelayMs;
        private final long timeoutMs;

        private final BTraceClientSettings settings;
        
        private transient ScheduledExecutorService executor;
        
        private transient List<NimbleClient> clients;
        
        private transient BTraceClientFactory clientFactory;

        public Impl(int corePoolSize, long pollDelayMs, long timeoutMs) {
            this.corePoolSize = corePoolSize;
            this.pollDelayMs = pollDelayMs;
            this.timeoutMs = timeoutMs;
            this.settings = new BTraceClientSettings();
        }

        @Override
        public ProbeHandle trace(PidProvider pidProvider, BTraceScriptSettings settings, MeteringSink<BTraceSamplerFactoryProvider> factoryProvider) {
            List<Runnable> probes = new ArrayList<Runnable>();

            settings = settings.init(pollDelayMs, timeoutMs);
            
            for (Long pid : pidProvider.getPids()) {                
                BTraceProbe probe = new BTraceProbe();

                probe.setPid(pid);
                probe.setSettings(settings);
                
                probe.setClient(getClient(pid, settings));
                
                probe.setFactoryProvider(factoryProvider.getSink());

                probes.add(new RunnableAdapter(probe));
            }
            
            return ProbeOps.schedule(probes, getExecutor(), settings.getPollDelayMs());
        }

        @Override
        public ProbeHandle trace(PidProvider pidProvider, Class<?> scriptClass, MeteringSink<BTraceSamplerFactoryProvider> factoryProvider) {
            BTraceScriptSettings settings = new BTraceScriptSettings();
            
            settings.setScriptClass(scriptClass);
            
            return trace(pidProvider, settings, factoryProvider);
        }
        
        public NimbleClient getClient(long pid, BTraceScriptSettings scriptSettings) {            
            NimbleClient client = null;
            
            try {
                client = clientFactory.newClient((int)pid, scriptSettings);
                
                if (!client.submit()) {
                    throw new Exception("Failed to submit BTrace script with settings " + scriptSettings);
                }
                
                if (!client.configureSession()) {
                    throw new Exception("Failed to configure BTrace session with settings " + scriptSettings);
                }
                
                clients.add(client);
            } catch (Exception e) {
                log.error(F("Failed to connect to client with pid %d", pid));
                
                if (client != null) {
                    client.close();
                }
                
                throw new RuntimeException(e);
            }
            
            return client;
        }
        
        @Override
        public void stop() {
            getExecutor().shutdownNow();

            for (NimbleClient client : clients) {
                client.close();
            }
        }
        
        private synchronized ScheduledExecutorService getExecutor() {
            if (executor == null) {
                executor = Executors.newScheduledThreadPool(
                    corePoolSize, new NamedThreadFactory("Pool[" + this.getClass().getSimpleName() + "]", true, Thread.NORM_PRIORITY)
                );
            }
            
            return executor;
        }
        
        private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            this.clients = new CopyOnWriteArrayList<NimbleClient>();
            this.clientFactory = new BTraceClientFactory(settings);
       }
    }
}
