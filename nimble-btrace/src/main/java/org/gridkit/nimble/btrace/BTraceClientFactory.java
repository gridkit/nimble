package org.gridkit.nimble.btrace;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import net.java.btrace.api.extensions.ExtensionsRepository;
import net.java.btrace.api.extensions.ExtensionsRepositoryFactory;
import net.java.btrace.client.Client;

import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessDetails;
import org.gridkit.nimble.util.CriticalSection;

public class BTraceClientFactory {
    private static int MAX_PORT_NUMBER = 65535;
    
    public static final int BTRACE_PORT = 2020;
    public static final String BTRACE_PORT_PROPERTY = "btrace.port";

    private CriticalSection connectSection = new CriticalSection();
    private AtomicInteger nextPort = new AtomicInteger(BTRACE_PORT);
        
    public Client newClient(int pid, BTraceClientSettings settings) throws ClientCreateException {
        try {
            return connectSection.execute(pid, new ClientConnector(pid, settings));
        } catch (ClientCreateException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientCreateException("Failed to build BTrace client for process id " + pid, e);
        }
    }
    
    public class ClientConnector implements Callable<Client> {
        private final int pid;
        private final BTraceClientSettings settings;

        public ClientConnector(int pid, BTraceClientSettings settings) {
            this.pid = pid;
            this.settings = settings;
        }

        @Override
        public Client call() throws Exception {
        	int retries = 1;
        	while(true) {
	            int port = getPort();
	            
	            Client client = Client.forProcess(pid);
	            
	            String extPath = settings.getExtensionsPath();
	            
	            ExtensionsRepository extRep = ExtensionsRepositoryFactory.fixed(ExtensionsRepository.Location.BOTH, extPath);
	            
	            if (settings.isDumpClasses()) {
	                File dumpDir = new File(settings.getDumpDir());
	                dumpDir.mkdirs();
	            }
	            
	            client.setBootstrapPath(settings.getRuntimePath());
	            client.setAgentPath(settings.getAgentPath());
	            client.setExtRepository(extRep);
	            client.setTrackRetransforms(settings.isTrackRetransform());
	            client.setUnsafe(settings.isUnsafe());
	            client.setDumpClasses(settings.isDumpClasses());
	            client.setDumpDir(settings.getDumpDir());
	            client.setProbeDescPath(settings.getProbeDescPath());
	            client.setPort(port);

	            try {
	            	client.attach();
	            }
	            catch(IOException e) {
	            	if (e.getMessage().startsWith("Can not attach")) {
	            		if (retries-- > 0) {
	            			continue;
	            		}
	            	}
	            	throw e;
	            }
	            
	            return client;
        	}        	
        }
        
        private int getPort() throws Exception {
            Integer port = null;

            JavaProcessDetails vm = AttachManager.getDetails(pid);
            
            String portPropery = vm.getSystemProperties().getProperty(BTRACE_PORT_PROPERTY);
            
            if (portPropery != null) {
                port = Integer.valueOf(portPropery);
            } else {
                port = borrowFreePort();
                
                if (port == null) {
                    throw new ClientCreateException("Failed to borrow free TCP port for pid " + pid);
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