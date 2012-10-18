package org.gridkit.nimble.btrace;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

import net.java.btrace.client.Client;
import net.java.btrace.ext.Printer;

import org.gridkit.nimble.btrace.ext.Nimble;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.probe.ProbeHandle;
import org.gridkit.nimble.util.CriticalSection;
import org.gridkit.nimble.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface BTraceDriver {
    ProbeHandle trace(PidProvider provider, Collection<Class<?>> classes, long pollDelayMs);
    
    void stop();
    
    public static class Impl implements BTraceDriver, BTraceClientSource {
        private static final Logger log = LoggerFactory.getLogger(Impl.class);
        
        private static final long CLIENT_OPERATION_TIMEOUT = 1000;
        
        private transient ScheduledExecutorService executor;
        private final int corePoolSize;

        private final BTraceClientOps clientOps = new BTraceClientOps();
        private final BTraceClientFactory clientFactory = new BTraceClientFactory();
        
        private final Map<Long, Client> clients = Collections.synchronizedMap(new HashMap<Long, Client>());
        
        private final CriticalSection connectSection = new CriticalSection();
        
        private final BTraceClientSettings settings = new BTraceClientSettings();

        public Impl(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            settings.setExtensionClasses(Nimble.class, Printer.class);
        }

        @Override
        public ProbeHandle trace(PidProvider provider, Collection<Class<?>> classes, long pollDelayMs) {
            return null;
        }
        
        @Override
        public Client getClient(final long pid) throws ClientCreateException {
            try {
                return connectSection.execute(pid, new Callable<Client>() {
                    @Override
                    public Client call() throws Exception {
                        if (!clients.containsKey(pid)) {
                            clients.put(pid, clientFactory.newClient((int)pid, settings));
                        }
                        return clients.get(pid);
                    }
                });
            } catch (ClientCreateException e) {
                throw e;
            } catch (Exception e) {
                throw new ClientCreateException("Failed to build BTrace client for process id " + pid, e);
            }
        }
        
        @Override
        public void stop() {
            getExecutor().shutdownNow();
            
            Map<Long, Client> clients = new HashMap<Long, Client>();
            clients.putAll(this.clients);
            
            for (Map.Entry<Long, Client> client : clients.entrySet()) {
                try {
                    clientOps.exit(client.getValue(), 0, CLIENT_OPERATION_TIMEOUT);
                } catch (TimeoutException ignored) { 
                } catch (Exception e) {
                    log.error("Failed to exit client for process id " + client.getKey(), e);
                }
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
    }
}
