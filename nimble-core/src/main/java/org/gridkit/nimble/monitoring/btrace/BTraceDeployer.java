package org.gridkit.nimble.monitoring.btrace;

import java.io.Serializable;
import java.net.InetAddress;

import org.gridkit.lab.monitoring.probe.PollProbe;
import org.gridkit.lab.monitoring.probe.PollProbeDeployer;
import org.gridkit.lab.monitoring.probe.SamplerProvider;
import org.gridkit.nimble.btrace.BTraceClientFactory;
import org.gridkit.nimble.btrace.BTraceClientSettings;
import org.gridkit.nimble.btrace.BTraceProbe;
import org.gridkit.nimble.btrace.BTraceScriptSampler;
import org.gridkit.nimble.btrace.BTraceScriptSettings;
import org.gridkit.nimble.btrace.NimbleClient;

public class BTraceDeployer implements PollProbeDeployer<Long, BTraceScriptSampler>, Serializable {

	private static final long serialVersionUID = 20130111L;
	
	private BTraceScriptSettings scriptSettings;
	private BTraceClientSettings clientSettings;
	
	private transient BTraceClientFactory factory;
	
	public void setScriptSettings(BTraceScriptSettings scriptSettings) {
		this.scriptSettings = scriptSettings;
	}

	public void setClientSettings(BTraceClientSettings clientSettings) {
		this.clientSettings = clientSettings;
	}

	@Override	
	public PollProbe deploy(Long target, SamplerProvider<Long, BTraceScriptSampler> provider) {
		
		Probe probe = new Probe();
		
		NimbleClient client = getClient(target.intValue(), scriptSettings);
		
        probe.setPid(target);
        probe.setSettings(scriptSettings);      
        probe.setClient(client);
        
        probe.setSampler(provider.getSampler(target));
		
		return probe;
	}
	
    private NimbleClient getClient(int pid, BTraceScriptSettings scriptSettings) {            
        
		synchronized (this) {
			if (factory == null) {
				factory = new BTraceClientFactory(clientSettings);
			}			
		}

    	NimbleClient client = null;
        
        try {
            client = factory.newClient(pid, scriptSettings);
            
            if (!client.submit()) {
                throw new Exception("Failed to submit BTrace script with settings " + scriptSettings);
            }
            
            if (!client.configureSession()) {
                throw new Exception("Failed to configure BTrace session with settings " + scriptSettings);
            }
            
        } catch (Exception e) {
            if (client != null) {
                client.close();
            }
            String localhost = "???";
            try {
            	localhost = InetAddress.getLocalHost().getHostName();
            }
            catch(Exception e2) {
            	// ignore
            }
            throw new RuntimeException("Failed to connect to client with pid " + pid + " at " + localhost, e);
        }
        
        return client;
    }
	
	private static class Probe extends BTraceProbe implements PollProbe {

		public void setSampler(BTraceScriptSampler sampler) {
			this.sampler = sampler;			
		}

		@Override
		public void poll() {
			try {
				this.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void stop() {
			poll();
			client.close();
		}
	}
}
