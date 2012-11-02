package org.gridkit.nimble.platform.remote;

import java.util.Arrays;
import java.util.HashSet;

import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;

public class LocalAgentFactory {

	private JvmNodeProvider nodeProvider;
	private ViNodeConfig config;
	
	public LocalAgentFactory(String... options) {
		nodeProvider = new JvmNodeProvider(new LocalJvmProcessFactory());
		config = new ViNodeConfig();
		for(String option: options) {
			JvmProps.addJvmArg(config, option);
		}
	}
	
	public RemoteAgent createAgent(String name, String... tags) {
		ViNode node = nodeProvider.createNode(name, config);
		return new ViNodeAgent(node, new HashSet<String>(Arrays.asList(tags)));
	}
}
