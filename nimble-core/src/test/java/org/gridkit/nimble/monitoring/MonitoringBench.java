package org.gridkit.nimble.monitoring;

import java.io.PrintStream;
import java.util.concurrent.Callable;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.jvm.attach.JavaProcessMatcher;
import org.gridkit.lab.jvm.attach.PatternJvmMatcher;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.driver.PivotMeteringDriver;
import org.gridkit.nimble.metering.DistributedMetering;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PivotPrinter2;
import org.gridkit.nimble.print.PrettyPrinter;
import org.gridkit.nimble.probe.jmx.AttachMBeanConnector;
import org.gridkit.nimble.probe.probe.MonitoringDriver;
import org.gridkit.nimble.probe.probe.Monitoring;
import org.gridkit.nimble.probe.probe.SchemaConfigurer;
import org.gridkit.vicluster.ViManager;
import org.junit.Test;

public class MonitoringBench {

	private String clusterName = "data-fabric-cluster-dev1";

	private SchemaConfigurer<MBeanServerConnection> sconfig = new CoherenceNodeSchemaConfig(new CommonJmxSchemaConfig());
	private MonitoringStack mstack = new MonitoringStack();
	
	
//	@Before
	public  void initCoherenceMonitoring() {
		
		CoherenceCpuMonitoring cohCpuTotal;
		cohCpuTotal = new CoherenceCpuMonitoring("coherence-cpu-total");
		cohCpuTotal.setSchemaConfig(sconfig);
		cohCpuTotal.setLocator(new AttachMBeanConnector(getClusterMatcher()));
		cohCpuTotal.groupBy(CoherenceMetricsKey.MEMBER_ROLE);		
		cohCpuTotal.sortByField(CoherenceMetricsKey.MEMBER_ROLE);
		cohCpuTotal.sortByField(CoherenceMetricsKey.SERVICE_NAME);
		cohCpuTotal.sortByField(CoherenceMetricsKey.THREAD_TYPE);
		DisplayBuilder.with(cohCpuTotal)
		.attribute("Role", CoherenceMetricsKey.MEMBER_ROLE);

		mstack.addBundle(cohCpuTotal, "Coherence CPU usage");
	}

	public  void initCoherenceMonitoringPerHost() {
		
		CoherenceCpuMonitoring cohCpuPerHost;
		cohCpuPerHost = new CoherenceCpuMonitoring("coherence-cpu-per-host");
		cohCpuPerHost.setSchemaConfig(sconfig);
		cohCpuPerHost.setLocator(new AttachMBeanConnector(getClusterMatcher()));
		cohCpuPerHost.groupBy(CoherenceMetricsKey.MEMBER_ROLE);
		cohCpuPerHost.groupBy(DistributedMetering.HOSTNAME);
		cohCpuPerHost.sortByField(DistributedMetering.HOSTNAME);
		cohCpuPerHost.sortByField(CoherenceMetricsKey.MEMBER_ROLE);
		cohCpuPerHost.sortByField(CoherenceMetricsKey.SERVICE_NAME);
		cohCpuPerHost.sortByField(CoherenceMetricsKey.THREAD_TYPE);
		DisplayBuilder.with(cohCpuPerHost)
		.attribute("Hostname", DistributedMetering.HOSTNAME)
		.attribute("Role", CoherenceMetricsKey.MEMBER_ROLE);

		mstack.addBundle(cohCpuPerHost, "Coherence CPU usage (per host)");
	}

	public  void initProcCPUMonitoring() {
		SchemaConfigurer<MBeanServerConnection> sconfig = new CoherenceNodeSchemaConfig(new CommonJmxSchemaConfig());
		
		ProcessCpuMonitoring cpuTotal;
		cpuTotal = new ProcessCpuMonitoring("sigar-proc-cpu-total");
		cpuTotal.setJmxSchemaConfig(sconfig);
		cpuTotal.setLocator(getClusterMatcher());
		cpuTotal.groupBy(CoherenceMetricsKey.MEMBER_ROLE);		
//		cpuTotal.groupBy(DistributedMetering.HOSTNAME);
		cpuTotal.sortByField(CoherenceMetricsKey.MEMBER_ROLE);
//		cpuTotal.sortByField(DistributedMetering.HOSTNAME);
		DisplayBuilder.with(cpuTotal)
//		.attribute("Hostname", DistributedMetering.HOSTNAME)
		.attribute("Role", CoherenceMetricsKey.MEMBER_ROLE)
		;

		mstack.addBundle(cpuTotal, "Process CPU usage");		
	}

	public  void initNetworkMonitoring() {

		NetworkMonitoring netMon;
		netMon = new NetworkMonitoring("sigar-network");
//		netMon.filterInterfaces("bond.*");
//		netMon.groupBy(DistributedMetering.HOSTNAME);
//		netMon.sortByField(DistributedMetering.HOSTNAME);
		DisplayBuilder.with(netMon)
		.attribute("Hostname", DistributedMetering.HOSTNAME)
//		.attribute("Role", CoherenceMetricsKey.MEMBER_ROLE)
		;
		
		mstack.addBundle(netMon, "Network throughput");		
	}
	
	@Test
	public void watchDf_network() throws InterruptedException {
		initNetworkMonitoring();
		watchDfCluster(true);
	}

	@Test
	public void watchJsmDev3_network() throws InterruptedException {
		initNetworkMonitoring();
		watchJsmDev3Cluster(true);
	}

	@Test
	public void watchJsmDev3_proc_cpu() throws InterruptedException {
		initProcCPUMonitoring();
		watchJsmDev3Cluster(true);
	}

	@Test
	public void watchDf_proc_cpu() throws InterruptedException {
		initProcCPUMonitoring();
		watchDfCluster(true);
	}

	@Test
	public void watchDf_all() throws InterruptedException {
		initProcCPUMonitoring();
		initNetworkMonitoring();
		initCoherenceMonitoring();
		initCoherenceMonitoringPerHost();
		
		watchDfCluster(false);
	}
	
	private void watchDfCluster(boolean dump) throws InterruptedException {
		clusterName = "data-fabric-cluster-dev1";
		ViManager cloud = CloudFactory.createSshCloud("~/devenv.viconf");
//		ViManager cloud = CloudFactory.createLocalCloud("~/devenv.viconf");
		cloud.node("longmrdfappd1.MON");
		cloud.node("longmrdfappd2.MON");
		cloud.node("longmrdfappd3.MON");
		watchCluster(cloud, dump);		
	}

	private void watchJsmDev3Cluster(boolean dump) throws InterruptedException {
		clusterName = "JobStateCacheCredit";
		ViManager cloud = CloudFactory.createSshCloud("~/devenv.viconf");
		cloud.node("longmchcu2.MON");		
		watchCluster(cloud, dump);		
	}

	public void watchCluster(ViManager cloud, boolean dump) throws InterruptedException {

		// warming up
		cloud.node("**").exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				return null;
			}
		});

		ScenarioBuilder sb = new ScenarioBuilder();
		Pivot pivot = new Pivot();
	
		mstack.configurePivot(pivot);
		
		PivotMeteringDriver pd = new PivotMeteringDriver(pivot);
		MeteringDriver metering = sb.deploy("**", pd);
		MonitoringDriver pollDriver = Monitoring.deployDriver(sb, metering);
		
		sb.checkpoint("init");
		sb.checkpoint("start");
		sb.sleep(10000);
		sb.checkpoint("stop");
		sb.checkpoint("done");
		metering.flush();

		mstack.inject(MeteringDriver.class, metering);
		mstack.inject(MonitoringDriver.class, pollDriver);
		mstack.deploy(sb, new TimeLine("init", "start", "stop", "done"));
		
		sb.getScenario().play(cloud);
		
		mstack.printSections(System.out, pd.getReporter());
		
		if (dump) {
			dump(pd.getReader());
		}
	};
	
	@SuppressWarnings("unused")
	private void print(PrintStream out, MonitoringBundle bundle, SampleReader reader) {
		System.out.println("\n" + bundle.getClass().getCanonicalName());
		System.out.println("\n" + bundle.getDescription() + "\n");
		PivotPrinter2 pp = new PivotPrinter2();
		bundle.configurePrinter(pp);
		
		new PrettyPrinter().print(out, pp.print(reader));
		
	}

	private JavaProcessMatcher getClusterMatcher() {
    	PatternJvmMatcher storage = new PatternJvmMatcher();
    	
        storage.matchVmName(".*WrapperStartStopApp.*");
    	storage.matchPropExact("tangosol.coherence.cluster", clusterName);
    	storage.matchProp("tangosol.coherence.member", "server.*");

    	PatternJvmMatcher proxy = new PatternJvmMatcher();
    	
    	proxy.matchVmName(".*WrapperStartStopApp.*");
    	proxy.matchPropExact("tangosol.coherence.cluster", clusterName);
    	proxy.matchProp("tangosol.coherence.member", "proxy.*");
    	
    	return new JavaProcessMatcher.Union(storage, proxy);

	}
	
//	private void addCoherenceMonitoring(ScenarioBuilder sb, Pivot pivot, MonitoringDriver poll, CoherenceCpuMonitoring cpuMon) {
//		cpuMon.setConnector(new AttachMBeanConnector(getClusterMatcher()));
//		cpuMon.deploy(sb, poll, new TimeLine("init", "start", "stop", "done"));
//	}
//	
//	private CoherenceCpuMonitoring getCoherenceCpuMon() {
//		SchemaConfigurer<MBeanServerConnection> sconfig = new CoherenceNodeSchemaConfig(new CommonJmxSchemaConfig());
//		
//		CoherenceCpuMonitoring cpuMon = new CoherenceCpuMonitoring("coherence-cpu");
//		cpuMon.setSchemaConfig(sconfig);
////		cpuMon.groupBy(DistributedMetering.HOSTNAME);
////		cpuMon.groupBy(CoherenceMonitoringBundle.MEMBER_ROLE);
//		
//		return cpuMon;
//	}

	private void dump(SampleReader reader) {
		System.out.println("\nDump\n");
		PivotPrinter2 pp = new PivotPrinter2();
		pp.dumpUnprinted();
		new PrettyPrinter().print(System.out, pp.print(reader));
	}
	
}
