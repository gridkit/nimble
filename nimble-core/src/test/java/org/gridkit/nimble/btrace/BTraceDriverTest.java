package org.gridkit.nimble.btrace;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.driver.PivotMeteringDriver;
import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.monitoring.MonitoringStack;
import org.gridkit.nimble.monitoring.btrace.BTraceMonitoring;
import org.gridkit.nimble.orchestration.Scenario;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.display.PivotPrinter2;
import org.gridkit.nimble.print.PrettyPrinter;
import org.gridkit.nimble.probe.probe.Monitoring;
import org.gridkit.nimble.probe.probe.MonitoringDriver;
import org.gridkit.nimble.probe.sigar.PtqlPidProvider;
import org.gridkit.vicluster.ViProps;
import org.junit.AfterClass;
import org.junit.Test;

public class BTraceDriverTest {
    public static final String MONITOR = "monitor";
    public static final String EXECUTOR = "executor";
    
    // initialization
    public static final String INIT = "init";
    // monitoring start
    public static final String START = "start";
    // test execution
    public static final String FINISHED = "finished";
    // monitoring stop
    public static final String DONE = "done";
    // metering flush
    
    @AfterClass
    public static void waitConsole() throws InterruptedException {
    	Thread.sleep(500);
    }
    
    @Test
    public void test() throws Exception {
        Cloud cloud = CloudFactory.createCloud();
        ViProps.at(cloud.node("**")).setLocalType();

        cloud.node(MONITOR);
        cloud.node(EXECUTOR);
        
        // Allow debugging for MONITOR node
        ViProps.at(cloud.node(MONITOR)).setInProcessType();

        MonitoringStack mstack = new MonitoringStack();
     
        configureMonitoring(mstack, cloud);
     
        Pivot pivot = new Pivot();
        
        mstack.configurePivot(pivot);
     
        PivotMeteringDriver pivotMetering = new PivotMeteringDriver(pivot, 512 << 10);
     
        Scenario scenario = newScenario(pivotMetering, mstack);
        
        scenario.play(cloud);
        
        Thread.sleep(100); // for console catch up
        
        dump(pivotMetering.getReporter().getReader());
        
        mstack.printSections(System.out, pivotMetering.getReporter());

        cloud.shutdown();
    }
    
	private void dump(SampleReader reader) {
		System.out.println("\nSample reader dump\n");
		PivotPrinter2 pp = new PivotPrinter2();
		pp.dumpUnprinted();
		new PrettyPrinter().print(System.out, pp.print(reader));
	}
    
    
    public static interface Driver {
        void execute();
    }

    private Scenario newScenario(PivotMeteringDriver pivotMetering, MonitoringStack mstack) throws Exception {
        ScenarioBuilder sb = new ScenarioBuilder();

        sb.checkpoint(INIT);
        sb.checkpoint(START);
        sb.checkpoint(FINISHED);
        sb.checkpoint(DONE);
     
        sb.fromStart();
            MeteringDriver metering = sb.deploy(pivotMetering);
            mstack.inject(MeteringDriver.class, metering);
            
            MonitoringDriver monitoring = Monitoring.deployDriver(MONITOR, sb, metering);
            mstack.inject(MonitoringDriver.class, monitoring);
            
            Driver driver = sb.deploy(EXECUTOR, new DriverImpl());
        sb.join(INIT);

        mstack.deploy(sb, new TimeLine(INIT, START, FINISHED, DONE));

        sb.from(START);
            driver.execute();
            sb.sleep(1500);
        sb.join(FINISHED);
        
        sb.from(DONE);
            metering.flush();
        
        return sb.getScenario();
    }

    private void configureMonitoring(MonitoringStack mstack, Cloud cloud) {
        int pid = cloud.node(EXECUTOR).exec(new GetPid());

        BTraceMonitoring bTraceMon = new BTraceMonitoring("btrace-test");
        
        bTraceMon.setScript(ServiceScript.class);
        bTraceMon.setLocator(new PtqlPidProvider("Pid.Pid.eq=" + pid));
        bTraceMon.setDebug(true);
        bTraceMon.setUnsafe(true);
        bTraceMon.setConnectionTimeout(1 << 20);
        bTraceMon.setPollPeriod(1 << 20);
        // workaround to run from Eclipse
        if (BTTestHelper.isNimbleExpanded()) {
        	bTraceMon.addExtension(BTTestHelper.getNimbleBTraceJar());
        }
        
        bTraceMon.showMissedStats();

        bTraceMon.showStore("SPAN")
	        .showValueDistribution()
	        .showTimeDistribution()
	        .showEventFrequency()
	        .showWeightedFrequency();

        bTraceMon.showStore("SCALAR")
	        .showValueDistribution();

        bTraceMon.showStore("POINT")
        	.showValueDistribution()
        	.showEventFrequency();
        
        bTraceMon.showStore("RATE")
        	.showWeightedFrequency();

        bTraceMon.showStore("DURATION")
        	.showTimeDistribution();


        mstack.addBundle(bTraceMon, "BTrace statistics");
    }
    
    @SuppressWarnings("serial")
    public static class DriverImpl implements Driver, Serializable {
        @Override
        public void execute() {
            Service srv = new Service();
            
            long ts = System.currentTimeMillis();
            long dur = TimeUnit.MILLISECONDS.toNanos(500);

            for (int i = 0; i < ServiceScript.STORE_SIZE_BASE + 1; ++i) {
                srv.duartion(dur, ts);
                srv.point(i, ts);
                srv.rate(i, ts);
                srv.span(i, ts, dur);
                srv.scalar(i);
                ts += TimeUnit.SECONDS.toMillis(1);
            }
        }
    }

    public static class Service {
        public static volatile double VALUE = 0;

        public void scalar(Number value) {
            VALUE += value.doubleValue();
        }
        
        public void duartion(long durationNs, long timestampMs) {
            VALUE += durationNs + timestampMs;
        }

        public void rate(Number value, long timestampMs) {
            VALUE += value.doubleValue() + timestampMs;
        }
        
        public void point(Number value, long timestampMs) {
            VALUE += value.doubleValue() + timestampMs;
        }
        
        public void span(Number value, long timestampMs, long durationNs) {
            VALUE += value.doubleValue() + timestampMs + durationNs;
        }
    }
    
    @SuppressWarnings("serial")
    public static class GetPid implements Callable<Integer>, Serializable {
        @Override
        public Integer call() throws Exception {
            return getPid();
        }
    }
    
    public static int getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        
        int dogIndex = name.indexOf('@');
        
        if (dogIndex != -1) {
            try {
                return Integer.valueOf(name.substring(0, dogIndex));
            } catch (NumberFormatException e) {
                return -1;
            }
        } else {
            return -1;
        }
    }
}
