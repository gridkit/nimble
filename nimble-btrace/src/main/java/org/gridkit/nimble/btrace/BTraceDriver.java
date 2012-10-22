package org.gridkit.nimble.btrace;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

import net.java.btrace.client.Client;
import net.java.btrace.ext.Printer;

import org.gridkit.nimble.btrace.ext.Nimble;
import org.gridkit.nimble.driver.MeteringSink;
import org.gridkit.nimble.probe.CachingSamplerFactory;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.probe.ProbeHandle;
import org.gridkit.nimble.probe.ProbeOps;
import org.gridkit.nimble.util.NamedThreadFactory;
import org.gridkit.nimble.util.RunnableAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface BTraceDriver {
    ProbeHandle trace(PidProvider pidProvider, BTraceScriptSettings settings, MeteringSink<BTraceSamplerFactoryProvider> factoryProvider);
    
    void stop();
    
    public static class Impl implements BTraceDriver, BTraceClientSource, Serializable {
        private static final long serialVersionUID = -7698303441795919196L;
        private static final Logger log = LoggerFactory.getLogger(Impl.class);
        private static final long CLIENT_OPERATION_TIMEOUT = 1000;

        private final int corePoolSize;
        private final BTraceClientSettings settings;
        
        private transient ScheduledExecutorService executor;
        
        private transient List<Client> clients;
        
        private transient BTraceClientOps clientOps;
        private transient BTraceClientFactory clientFactory;

        public Impl(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            this.settings = new BTraceClientSettings();
            
            settings.setExtensionClasses(Nimble.class, Printer.class);
        }

        @Override
        public ProbeHandle trace(PidProvider pidProvider, BTraceScriptSettings settings, MeteringSink<BTraceSamplerFactoryProvider> factoryProvider) {
            List<Runnable> probes = new ArrayList<Runnable>();

            for (Long pid : pidProvider.getPids()) {                
                BTraceProbe probe = new BTraceProbe();
                
                probe.setPid(pid);
                probe.setSettings(settings);
                probe.setTimeoutMs(CLIENT_OPERATION_TIMEOUT);
                
                probe.setClientOps(clientOps);
                probe.setClientSource(this);
                
                probe.setSamplerFactory(new CachingSamplerFactory(factoryProvider.getSink().getProcSampleFactory(pid)));

                probes.add(new RunnableAdapter(probe));
            }
            
            return ProbeOps.schedule(probes, getExecutor(), settings.getPollDelayMs());
        }

        @Override
        public Client getClient(final long pid) throws ClientCreateException {
            Client client = clientFactory.newClient((int)pid, settings);
            clients.add(client);
            return client;
        }
        
        @Override
        public void stop() {
            getExecutor().shutdownNow();

            for (Client client : clients) {
                try {
                    clientOps.exit(client, 0, CLIENT_OPERATION_TIMEOUT);
                } catch (TimeoutException ignored) { 
                } catch (Exception e) {
                    log.error("Failed to stop client " + client, e);
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
        
        private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            this.clients = new CopyOnWriteArrayList<Client>();
            this.clientOps = new BTraceClientOps();
            this.clientFactory = new BTraceClientFactory();
       }
    }
}
