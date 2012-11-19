package org.gridkit.nimble.zootest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.gridkit.vicluster.ViGroup;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.VoidCallable;

public class ZooEnsemble {

	private List<ZooMemberInfo> ensemble = new ArrayList<ZooEnsemble.ZooMemberInfo>();
	private Map<String, Integer> nextPort = new HashMap<String, Integer>();
	private int baseZooport = 33000;

	public String getConnectionURI() {
		StringBuilder sb = new StringBuilder();
		for(ZooMemberInfo zmi: ensemble) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(zmi.hostname).append(":").append(zmi.clientPort);
		}
		return sb.toString();
	}
	
	public void setBaseZookeeperPort(int basePort) {
		this.baseZooport = basePort;
	}
	
	public void addToEnsemble(ViNode node) {

		String hostname = node.exec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return Inet4Address.getLocalHost().getHostName();
			}
		});

		int basePort = baseZooport;
		if (nextPort.containsKey(hostname)) {
			basePort = nextPort.get(hostname);
		}
		nextPort.put(hostname, basePort + 10);

		ZooMemberInfo memInfo = new ZooMemberInfo();
		ensemble.add(memInfo);

		memInfo.zooId = ensemble.size();
		memInfo.hostname = hostname;
		memInfo.clientPort = basePort;
		memInfo.quorumPort = basePort + 1;
		memInfo.leaderPort = basePort + 2;
		memInfo.node = node;

		node.setProp("vi-zookeeper.maxClientCnxns", "100");
		node.setProp("vi-zookeeper.tickTime", "2000");
		node.setProp("vi-zookeeper.initLimit", "10");
		node.setProp("vi-zookeeper.syncLimit", "5");
		node.setProp("vi-zookeeper.baseDataDir", ".zookeeper");
		node.setProp("vi-zookeeper.clientPort",	String.valueOf(memInfo.clientPort));
		node.setProp("vi-zookeeper.myid", String.valueOf(memInfo.zooId));
		node.setProp("vi-zookeeper.clientPort", String.valueOf(memInfo.clientPort));
	}

	public void startEnsemble() {

		Map<String, String> ensembleProps = new HashMap<String, String>();

		for (ZooMemberInfo zmi : ensemble) {
			String row = zmi.hostname + ":" + zmi.quorumPort + ":" + zmi.leaderPort;
			ensembleProps.put("vi-zookeeper.server." + zmi.zooId, row);
		}

		ViGroup cluster = new ViGroup();
		for (ZooMemberInfo zmi : ensemble) {
			cluster.addNode(zmi.node);
		}
		cluster.setProps(ensembleProps);
		cluster.exec(new ZooStarter());
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// catching up with console
		}
		
		System.out.println("ZooEnsemble started");

	}

	@SuppressWarnings("serial")
	static class ZooStarter implements VoidCallable, Serializable {
		@Override
		public void call() throws IOException, ConfigException, InterruptedException {
			
			String id = System.getProperty("vi-zookeeper.myid");
			String baseDir = System.getProperty("vi-zookeeper.baseDataDir", "");
			if (baseDir.indexOf('~') >= 0) {
				String home = System.getProperty("user.home");
				baseDir.replaceFirst("[~]", home);
			}
//			baseDir = normalize(baseDir);
			String dataDir = new File(new File(baseDir), "node-" + id).getCanonicalPath();
			
			Properties props = PropHelper.filterByPrefix("vi-zookeeper", System.getProperties());
			props.put("dataDir", dataDir);

			File dir = new File(dataDir);
			rmrf(dir);
			dir.mkdirs();
			new File(dir, "version-2").mkdirs();
			FileWriter fw = new FileWriter(new File(dir, "myid"));
			fw.append(String.valueOf(id));
			fw.close();
			
			
			QuorumPeerConfig qpc = new QuorumPeerConfig();
			qpc.parseProperties(props);
			startPeer(qpc);
		}

		private void startPeer(QuorumPeerConfig config) throws IOException, InterruptedException {
			System.out.println("Starting quorum peer");

			QuorumPeer quorumPeer;
			ServerCnxnFactory cnxnFactory = ServerCnxnFactory.createFactory();
			cnxnFactory.configure(config.getClientPortAddress(),
					config.getMaxClientCnxns());

			quorumPeer = new QuorumPeer();
			quorumPeer.setClientPortAddress(config.getClientPortAddress());
			quorumPeer.setTxnFactory(
					new FileTxnSnapLog(
							new File(config.getDataLogDir()), 
							new File(config.getDataDir())
					));
			quorumPeer.setQuorumPeers(config.getServers());
			quorumPeer.setElectionType(config.getElectionAlg());
			quorumPeer.setMyid(config.getServerId());
			quorumPeer.setTickTime(config.getTickTime());
			quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
			quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
			quorumPeer.setInitLimit(config.getInitLimit());
			quorumPeer.setSyncLimit(config.getSyncLimit());
			quorumPeer.setQuorumVerifier(config.getQuorumVerifier());
			quorumPeer.setCnxnFactory(cnxnFactory);
			quorumPeer.setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory()));
			quorumPeer.setLearnerType(config.getPeerType());

			quorumPeer.start();
			System.out.println("Quorum peer started, looking for quorum");
			while(quorumPeer.getPeerState() == ServerState.LOOKING) {
				Thread.sleep(100);
			}
			System.out.println("Peer state: " + quorumPeer.getServerState());
		}
	}

	public static void rmrf(File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.exists()) {
					if (file.isDirectory()) {
						rmrf(file);
					} else {
						file.delete();
					}
				}
			}
		}
		dir.delete();
	}	
	
	private static class ZooMemberInfo {
		int zooId;
		String hostname;
		int clientPort;
		int quorumPort;
		int leaderPort;
		ViNode node;
	}
}
