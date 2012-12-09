package org.gridkit.nimble.btrace;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.java.btrace.agent.Server;
import net.java.btrace.api.core.BTraceLogger;
import net.java.btrace.api.extensions.ExtensionsRepository;
import net.java.btrace.api.extensions.ExtensionsRepositoryFactory;

import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessDetails;
import org.gridkit.nimble.util.CriticalSection;

public class BTraceClientFactory {
    static {
        BTraceLogger.useSlf4j(true);
    }
    
    private static int MAX_PORT_NUMBER = 65535;
    
    private static final CriticalSection connectSection = new CriticalSection();
    private static final AtomicInteger nextPort = new AtomicInteger(Server.BTRACE_DEFAULT_PORT);
    private static final ConcurrentMap<Integer, Integer> portCache = new ConcurrentHashMap<Integer, Integer>();
    
    private final BTraceClientSettings clientSettings;

    public BTraceClientFactory(BTraceClientSettings clientSettings) {
        this.clientSettings = clientSettings;
    }

    public NimbleClient newClient(int pid, BTraceScriptSettings scriptSettings) throws Exception {
        return connectSection.execute(pid, new ClientConnector(pid, scriptSettings));
    }
    
    public class ClientConnector implements Callable<NimbleClient> {
        private final int pid;
        private final BTraceScriptSettings scriptSettings;

        public ClientConnector(int pid, BTraceScriptSettings scriptSettings) {
            this.pid = pid;
            this.scriptSettings = scriptSettings;
        }

        @Override
        public NimbleClient call() throws Exception {
            return NimbleClient.execute(new Callable<NimbleClient>() {
                @Override
                public NimbleClient call() throws Exception {
                    NimbleClient client = newClient();
                    
                    int port;
                    
                    if (portCache.containsKey(pid)) {
                        port = portCache.get(pid);
                    } else {
                        port = getPort();
                        portCache.put(pid, port);
                    }
                    
                    client.setPort(port);
                    client.attach();

                    return client;
                }
            }, scriptSettings.getTimeoutMs());
        }
        
        private NimbleClient newClient() throws Exception {
            NimbleClient client = new NimbleClient(pid, scriptSettings);
            
            String extPath = clientSettings.getExtensionsPath();
            
            ExtensionsRepository extRep = ExtensionsRepositoryFactory.fixed(ExtensionsRepository.Location.CLIENT, extPath);
            
            if (clientSettings.isDumpClasses()) {
                File dumpDir = new File(clientSettings.getDumpDir());
                dumpDir.mkdirs();
            }
            
            client.setDebug(clientSettings.isDebug());
            client.setBootstrapPath(clientSettings.getRuntimePath());
            client.setAgentPath(clientSettings.getAgentPath());
            client.setExtRepository(extRep);
            client.setTrackRetransforms(clientSettings.isTrackRetransform());
            client.setUnsafe(clientSettings.isUnsafe());
            client.setDumpClasses(clientSettings.isDumpClasses());
            client.setDumpDir(clientSettings.getDumpDir());
            client.setProbeDescPath(clientSettings.getProbeDescPath());
            
            //TODO should add tools.jar of target VM
            client.setSysCp(client.getSysCp());
            
            return client;
        }
        
        private int getPort() throws Exception {
            Integer port = null;

            JavaProcessDetails vm = AttachManager.getDetails(pid);
            
            String portPropery = vm.getSystemProperties().getProperty(Server.BTRACE_PORT_KEY);
            
            if (portPropery != null) {
                port = Integer.valueOf(portPropery);
            } else {
                port = borrowFreePort();
                
                if (port == null) {
                    throw new Exception("Failed to borrow free TCP port for pid " + pid);
                }
            }

            return port;
        }
    }
    
    private Integer borrowFreePort() {
        while (true) {
            int port = nextPort.getAndIncrement();
            
            if (port > MAX_PORT_NUMBER) {
                return null;
            }

            if (free(port)) {
                return port;
            }
        }
    }

    private static boolean free(int port) {
        boolean result = true;
        
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
        } catch (IOException e) {
            result = false;
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    result = false;
                }
            }
        }
    
        return result;
    }
}