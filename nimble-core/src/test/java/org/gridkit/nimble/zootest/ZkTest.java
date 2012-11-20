package org.gridkit.nimble.zootest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.gridkit.lab.jvm.attach.PatternJvmMatcher;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nimble.driver.Activity;
import org.gridkit.nimble.driver.ExecutionDriver;
import org.gridkit.nimble.driver.ExecutionDriver.ExecutionConfig;
import org.gridkit.nimble.driver.ExecutionHelper;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.driver.PivotMeteringDriver;
import org.gridkit.nimble.metering.DTimeReporter;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.monitoring.MonitoringStack;
import org.gridkit.nimble.monitoring.ProcessCpuMonitoring;
import org.gridkit.nimble.monitoring.StandardSamplerReportBundler;
import org.gridkit.nimble.monitoring.SysPropSchemaConfig;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.PivotReporter;
import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PivotPrinter2;
import org.gridkit.nimble.print.PrettyPrinter;
import org.gridkit.nimble.probe.probe.Monitoring;
import org.gridkit.nimble.probe.probe.MonitoringDriver;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZkTest {

	private ViManager cloud = CloudFactory.createLocalCloud();
//	private ViManager cloud = CloudFactory.createIsolateCloud();
	
	private MonitoringStack mstack = new MonitoringStack();
	
	private int testTime = 60000;
	
	private int readerCount = 4;
	private int writerCount = 4;
	
	private ExecutionConfig readerExecConfig = ExecutionHelper.constantRateExecution(100, 50, true);
	private ExecutionConfig writerExecConfig = ExecutionHelper.constantRateExecution(10, 20, true);
	
	
	@After
	public void dropCloud() {
		cloud.shutdown();
	}
	
	@Before
	public void addReporting() {
		StandardSamplerReportBundler mon = new StandardSamplerReportBundler("sampler");
		mon.sortByField(Measure.NAME);		
		mstack.addBundle(mon, "Operation statistics");
	}

	@Before
	public void addCpuReporting() {
		PatternJvmMatcher matcher = new PatternJvmMatcher();
		matcher.matchVmName(".*boot.*");
		matcher.matchProp("proc-type", ".*");
		
		SysPropSchemaConfig.ProcessId config = new SysPropSchemaConfig.ProcessId();
		config.readProp("proc-type", "Process type");
		
		ProcessCpuMonitoring mon = new ProcessCpuMonitoring("proc-cpu");
		mon.setLocator(matcher);
		mon.setSchemaConfig(config);
		mon.groupBy("Process type");
		mon.sortByField("Process type");
		DisplayBuilder.with(mon)
		.attribute("Process type");
		mstack.addBundle(mon, "Process CPU utilization");
	}

	public void startZooKeeper() {
		
		ZooEnsemble ensemble = new ZooEnsemble();
		ensemble.setBaseZookeeperPort(30000);
		
		for(ViNode node: cloud.listNodes("**.ZK.**")) {
			ensemble.addToEnsemble(node);
		}
		
		ensemble.startEnsemble();
		
		String uri = ensemble.getConnectionURI();
		cloud.node("**").setProp("zooConnection", uri);
		
	}
	
	@Test
	public void start_only() {
		cloud.nodes("ZK.1", "ZK.2", "ZK.3");
		startZooKeeper();
	}

	@Test
	public void start_and_run() {
		cloud.nodes("ZK.1", "ZK.2", "ZK.3");
		cloud.nodes("WORKER.1", "WORKER.2", "WORKER.3");
		cloud.nodes("MON");
		cloud.node("ZK.**").setProp("proc-type", "ZooServer");
		cloud.node("WORKER.**").setProp("proc-type", "Worker");
		cloud.node("**").touch();
		startZooKeeper();
		
		PivotReporter reporter = runTest();

		PivotPrinter2 pp = new PivotPrinter2();
		pp.dumpUnprinted();
		new PrettyPrinter().print(System.out, pp.print(reporter.getReader()));
		
		mstack.printSections(System.out, reporter);		
	}
	
	
	public PivotReporter runTest() {
		
		ScenarioBuilder sb = new ScenarioBuilder();

		
		Pivot pivot = new Pivot();
		mstack.configurePivot(pivot);
		PivotMeteringDriver pd = new PivotMeteringDriver(pivot);
		
		MeteringDriver md = sb.deploy(pd);
		mstack.inject(MeteringDriver.class, md);
		mstack.inject(MonitoringDriver.class, Monitoring.deployDriver("**.MON.**", sb, md));
		
		sb.checkpoint("init");
		sb.checkpoint("start");
		sb.sleep(testTime);
		sb.checkpoint("stop");
		sb.checkpoint("done");

		mstack.deploy(sb, new TimeLine("init", "start", "stop", "done"));
		
		deployTest(sb, md);
		
		sb.from("done");
		md.flush();
		
		sb.debug_simulate();
		
		sb.getScenario().play(cloud);
		
		return pd.getReporter();		
	}

	private void deployTest(ScenarioBuilder sb, MeteringDriver md) {
		
		sb.fromStart();		
		TestDriver driver = new TestDriverImpl();
		driver = sb.deploy("**.WORKER.**", driver);
		ExecutionDriver exec = sb.deploy(ExecutionHelper.newDriver());
		
		for(int i = 0; i != readerCount; ++i) {
			sb.from("init");
			Runnable r = driver.getReader(md);
			sb.sleep(5000);
			sb.join("start");
			sb.from("start");
			Activity act = exec.start(r, readerExecConfig, null);
			sb.from("stop");
			act.stop();
			sb.fromStart();
			act.join();
			sb.join("done");			
		}

		for(int i = 0; i != writerCount; ++i) {
			sb.from("init");
			Runnable r = driver.getWriter(md);
			sb.join("start");
			sb.from("start");
			Activity act = exec.start(r, writerExecConfig, null);
			sb.from("stop");
			act.stop();
			sb.fromStart();
			act.join();
			sb.join("done");			
		}
	}
	
	public static interface TestDriver {
	
		public Runnable getReader(MeteringDriver metering);
		
		public Runnable getWriter(MeteringDriver metering);
		
	}
	
	@SuppressWarnings("serial")
	public static class TestDriverImpl implements TestDriver, Serializable {

		enum Op { hit, miss, create, update, fail };
		
		private String basePath = "/test";
		private int nameRange = 1000;

		private ZooKeeper connect() {
			String uri = System.getProperty("zooConnection");
			while(true) {
				final FutureBox<Void> connection = new FutureBox<Void>();
				ZooKeeper client = null;
				try {
					client = new ZooKeeper(uri, 30000, new Watcher() {
						@Override
						public void process(WatchedEvent event) {
							System.out.println(event);
							connection.setData(null);
						}
					});
					final List<ACL> acl = new ArrayList<ACL>();
					acl.add(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.ANYONE_ID_UNSAFE));
					if (client.exists(basePath, false) == null) {
						try {
							client.create(basePath, new byte[0], acl, CreateMode.PERSISTENT);
						}
						catch(NodeExistsException e) {
							// ignore
						}
					}
				} catch (ConnectionLossException e) {
					try {
						client.close();
					}
					catch(Exception ee) {
						// ignore
					}
					continue;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				
				return client;
			}
		}
		
		@Override
		public Runnable getReader(final MeteringDriver metering) {
			final ZooKeeper client;
			client = connect();
			
			return new Runnable() {
				
				DTimeReporter<Op> rep = metering.samplerBuilder().timeReporter("Read (%s)", Op.class);
				Random rand = new Random();
				
				@Override
				public void run() {
				
					try {
						int n = rand.nextInt(nameRange);
						String path = basePath + "/node-" + n;
						DTimeReporter.StopWatch<Op> sw = rep.start();
						try {
							byte[] data = client.getData(path, false, null);
							n = data.length; // just to avoid warning
							sw.stop(Op.hit);
						}
						catch(NoNodeException e) {
							sw.stop(Op.miss);
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
		}

		@Override
		public Runnable getWriter(final MeteringDriver metering) {
			final ZooKeeper client;
			client = connect();
			
			return new Runnable() {
				
				DTimeReporter<Op> rep = metering.samplerBuilder().timeReporter("Write (%s)", Op.class);
				Random rand = new Random();
				
				@Override
				public void run() {
				
					List<ACL> acl;
					acl = new ArrayList<ACL>();
					acl.add(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.ANYONE_ID_UNSAFE));
					
					try {
						int n = rand.nextInt(nameRange);
						byte[] data = new byte[128];
						rand.nextBytes(data);
						
						String path = basePath + "/node-" + n;
						DTimeReporter.StopWatch<Op> sw = rep.start();
						Stat st = client.exists(path, false);
						if (st == null) {
							try {
								client.create(path, data, acl, CreateMode.PERSISTENT);
								sw.stop(Op.create);
							}
							catch(NodeExistsException e) {
								sw.stop(Op.fail);
								return;								
							}
						}
						else {
							try {
								client.setData(path, data, st.getVersion());
								sw.stop(Op.update);
							}
							catch(BadVersionException e) {
								sw.stop(Op.fail);
								return;								
							}
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
		}
	}	
}
