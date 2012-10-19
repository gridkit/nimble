package org.gridkit.nimble.btrace;

import java.io.Serializable;

import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.driver.PivotMeteringDriver;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleSchema;
import org.gridkit.nimble.orchestration.Scenario;
import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.PivotPrinter;
import org.gridkit.nimble.print.PrettyPrinter;
import org.gridkit.nimble.probe.PidProvider;
import org.gridkit.nimble.probe.sigar.SigarDriver;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.junit.Test;

public class BTraceDriverTest {
    @Test
    public void test() throws Exception {
        ViManager cloud = new ViManager(new JvmNodeProvider(new LocalJvmProcessFactory()));
        
        ViNode node = cloud.node("node");
        cloud.node("monitor");
        
        int pid = node.exec(new BTraceClientFactoryTest.GetPid());
        
        Pivot pivot = new Pivot();

        pivot.root().group(MeteringDriver.NODE).group(BTraceMeasure.SAMPLE_KEY)
             .level("stats")
             .show()
             .display(MeteringDriver.NODE)
             .display(BTraceMeasure.SAMPLE_KEY)
             .calcDistribution(Measure.MEASURE)
             .displayDistribution(Measure.MEASURE);
        
        PivotMeteringDriver metrics = new PivotMeteringDriver(pivot, 1024);
        
        Scenario scenario = createTestScenario(pid, metrics);
        
        scenario.play(cloud);
        
        PivotPrinter printer = new PivotPrinter(pivot, metrics.getReporter());
        
        PrettyPrinter pp = new PrettyPrinter();
        
        pp.print(System.out, printer);
        
        System.out.println("Done");
        
        cloud.shutdown();
    }
    
    public static interface Driver {
        void start(long count, long sleepMs) throws Exception;
    }

    private Scenario createTestScenario(long pid, MeteringDriver metrics) throws Exception {        
        ScenarioBuilder sb = new ScenarioBuilder();
        
        MeteringDriver metering = sb.deploy("**", metrics);
        
        Driver driver = sb.deploy("node", new DriverImpl());
        
        BTraceDriver btrace = sb.deploy("monitor", new BTraceDriver.Impl(1));

        SigarDriver sigar = sb.deploy("monitor", new SigarDriver.Impl(1, 100));
        
        PidProvider provider = sigar.newPtqlPidProvider("Pid.Pid.eq=" + pid);

        SampleSchema schema = metering.getSchema();
        
        sb.checkpoint("init");
        
        BTraceScriptSettings settings = new BTraceScriptSettings();
        settings.setScriptClass(ServiceScript.class);
        settings.setPollDelayMs(100);
        
        btrace.trace(provider, schema, settings);

        sb.checkpoint("start");
        
        driver.start(1, 3000);
        
        sb.sync();
        
        driver.start(100, 25);
        
        sb.sleep(1000);
                
        sb.checkpoint("finish");

        btrace.stop();
        
        metering.flush();

        return sb.getScenario();
    }
    
    @SuppressWarnings("serial")
    public static class DriverImpl implements Driver, Serializable {
        @Override
        public void start(long count, long sleepMs) throws Exception {
            Service srv = new Service();
            
            for (int i = 0; i < count; ++i) {
                srv.serve(sleepMs);
            }
        }
    }

    public static class Service {
        public static int count = 0;

        public void serve(long sleepMs) throws Exception {
            Thread.sleep(sleepMs);
            count++;
        }
    }
}
