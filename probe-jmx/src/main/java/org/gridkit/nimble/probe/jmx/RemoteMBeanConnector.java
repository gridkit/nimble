package org.gridkit.nimble.probe.jmx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class RemoteMBeanConnector implements MBeanConnector, Serializable {

	private static final long serialVersionUID = 20121109L;
	
	private final String[] hosts;
	private final int[] ports;
	
	@SuppressWarnings("unused")
	private String user;
	@SuppressWarnings("unused")
	private String password;
	
	public RemoteMBeanConnector(String host, int... ports) {
		this.hosts = new String[]{host};
		this.ports = ports;
	}

	public RemoteMBeanConnector(Collection<String> hosts, int... ports) {
		this.hosts = hosts.toArray(new String[hosts.size()]);
		this.ports = ports;
	}
	
	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	@Override
	public Collection<MBeanServerConnection> connect() {
		List<Future<MBeanServerConnection>> fresult = new ArrayList<Future<MBeanServerConnection>>();
		for(final String host: hosts) {
			for(final int port: ports) {
				
				final String uri = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
				Callable<MBeanServerConnection> connTask = new Callable<MBeanServerConnection>() {
					@Override
					public MBeanServerConnection call() throws Exception {
						JMXServiceURL jmxurl = new JMXServiceURL(uri);
						JMXConnector conn = JMXConnectorFactory.connect(jmxurl);
						// TODO credetials
						MBeanServerConnection mserver = conn.getMBeanServerConnection();
						return mserver;
					}					
				};
				
				FutureTask<MBeanServerConnection> mtask = new FutureTask<MBeanServerConnection>(connTask);
				Thread t = new Thread(mtask);
				t.setDaemon(true);
				t.setName("Connect: " + uri);
				t.start();
				
				fresult.add(mtask);
			}
		}
		
		List<MBeanServerConnection> result = new ArrayList<MBeanServerConnection>();
		for(Future<MBeanServerConnection> f: fresult) {
			try {
				MBeanServerConnection conn = f.get();
				if (conn != null) {
					result.add(conn);
				}
			} catch (InterruptedException e) {
				// ignore
			} catch (ExecutionException e) {
				// ignore
			}
		}
		return result;
	}
}
