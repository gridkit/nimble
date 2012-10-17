package org.gridkit.nimble.probe.sigar;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.driver.PivotMeteringDriver;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.orchestration.Scenario;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.Pivot.Level;
import org.gridkit.nimble.pivot.PivotPrinter;
import org.gridkit.nimble.print.PrettyPrinter;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.util.concurrent.Barriers;
import org.gridkit.util.concurrent.BlockingBarrier;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.telecontrol.isolate.IsolateCloudFactory;
import org.junit.Test;

public class SigarDriverTest {
    @Test
    public void test_proc_cpu() throws Exception {
        ViManager cloud = IsolateCloudFactory.createCloud();
        
        cloud.node("node1");
        cloud.node("node2");
        
        Pivot pivot = new Pivot();

        pivot.root().group(MeteringDriver.NODE).group(SigarMeasure.PROBE_TYPE).group(SigarMeasure.MEASURE_NAME)
            .level("stats")
                .show()
                .display(MeteringDriver.NODE)
                .display(SigarMeasure.MEASURE_NAME)
                .calcFrequency(Measure.MEASURE)
                .displayThroughput(Measure.MEASURE);
        
        PivotMeteringDriver metrics = new PivotMeteringDriver(pivot, 1024);
        
        Scenario scenario = createTestScenario(metrics);
        
        scenario.play(cloud);
        
        PivotPrinter printer = new PivotPrinter(pivot, metrics.getReporter());
        
        PrettyPrinter pp = new PrettyPrinter();
        
        pp.print(System.out, printer);
        
        System.out.println("Done");
        
        cloud.shutdown();
    }
    
    public static interface Driver {
        void start() throws Exception;
    }

    private Scenario createTestScenario(MeteringDriver metrics) throws Exception {        
        ScenarioBuilder sb = new ScenarioBuilder();
        
        MeteringDriver metering = sb.deploy("**", metrics);
        
        Driver driver = sb.deploy("**", new DriverImpl());
        
        sb.checkpoint("init");
        
        SigarDriver sigar = sb.deploy("**", new SigarDriver.Impl(2, 100, TimeUnit.MILLISECONDS));
        
        PidProvider provider = sigar.newPtqlPidProvider("Exe.Name.ct=java");
        
        sb.checkpoint("start");
        
        driver.start();
        
        sigar.monitorProcCpu(provider, metering);
        
        sb.checkpoint("finish");
            
        sigar.stop();
        
        metering.flush();

        return sb.getScenario();
    }
    
    @SuppressWarnings("serial")
    public static class DriverImpl implements Driver, Serializable {
        @Override
        public void start() throws Exception {
            ExecutorService executor = Executors.newCachedThreadPool();
            
            BlockingBarrier barrier = Barriers.speedLimit(500);
            
            executor.submit(new SinCalc(barrier));
            
            Thread.sleep(2000);
            
            executor.shutdownNow();
        }
    }
    
    public static class SinCalc implements Callable<Void> {
        public static double SIN = 0.0;
        
        private final BlockingBarrier barrier;

        public SinCalc(BlockingBarrier barrier) {
            this.barrier = barrier;
        }
        
        @Override
        public Void call() throws Exception {
            System.err.println("start sin calc");
            
            while(!Thread.interrupted()) {
                barrier.pass();
                SIN += Math.sin(new Random().nextDouble());
            }
            
            System.err.println("finish sin calc");
            
            return null;
        }
    }
}
