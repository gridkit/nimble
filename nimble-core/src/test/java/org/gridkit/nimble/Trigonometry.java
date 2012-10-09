package org.gridkit.nimble;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.platform.Director;
import org.gridkit.nimble.platform.Play;
import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.nimble.platform.local.ThreadPoolAgent;
import org.gridkit.nimble.platform.remote.LocalAgentFactory;
import org.gridkit.nimble.print.PrettyPrinter;
import org.gridkit.nimble.print.TablePrinter;
import org.gridkit.nimble.scenario.DemonScenario;
import org.gridkit.nimble.scenario.ParScenario;
import org.gridkit.nimble.scenario.Scenario;
import org.gridkit.nimble.scenario.SeqScenario;
import org.gridkit.nimble.sensor.IntervalMeasure;
import org.gridkit.nimble.sensor.NetInterfacePrinter;
import org.gridkit.nimble.sensor.NetInterfaceReporter;
import org.gridkit.nimble.sensor.NetInterfaceSensor;
import org.gridkit.nimble.sensor.PidProvider;
import org.gridkit.nimble.sensor.ProcCpuPrinter;
import org.gridkit.nimble.sensor.ProcCpuReporter;
import org.gridkit.nimble.sensor.ProcCpuSensor;
import org.gridkit.nimble.sensor.SensorDemon;
import org.gridkit.nimble.sensor.SysCpuPrinter;
import org.gridkit.nimble.sensor.SysCpuReporter;
import org.gridkit.nimble.sensor.SysCpuSensor;
import org.gridkit.nimble.statistics.StatsReporter;
import org.gridkit.nimble.statistics.simple.AggregatingSimpleStatsReporter;
import org.gridkit.nimble.statistics.simple.QueuedSimpleStatsAggregator;
import org.gridkit.nimble.statistics.simple.SimpleStats;
import org.gridkit.nimble.statistics.simple.SimpleStatsAggregator;
import org.gridkit.nimble.statistics.simple.SimpleStatsTablePrinter;
import org.gridkit.nimble.statistics.simple.SimpleStatsTablePrinter.SimpleStatsLinePrinter;
import org.gridkit.nimble.statistics.simple.ThroughputLatencyPrinter;
import org.gridkit.nimble.statistics.simple.ThroughputLatencyReporter;
import org.gridkit.nimble.statistics.simple.WhitelistPrinter;
import org.gridkit.nimble.task.SimpleStatsReporterFactory;
import org.gridkit.nimble.task.Task;
import org.gridkit.nimble.task.TaskSLA;
import org.gridkit.nimble.task.TaskScenario;
import org.hyperic.sigar.Cpu;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class Trigonometry {    
    private static final String SIN = "sin";
    private static final String COS = "cos";
    private static final String TAN = "tan";
    
    private static final long WARMUP_NUMBERS = 5;
    private static final long WARMUP_ITERATIONS = 25000;
    private static final long WARMUP_DURATION = 3; // seconds
    
    private static final long NUMBERS = 10;
    private static final long ITERATIONS = 100000;
    private static final long DURATION = 15; // seconds

    private static LocalAgentFactory localFactory = new LocalAgentFactory();
    
    public RemoteAgent createAgent(String mode, String... labels) {
    	if ("in-proc".equals(mode)) {
    		return new ThreadPoolAgent(new HashSet<String>(Arrays.asList(labels)));
    	}
    	if ("local".equals(mode)) {
    		return localFactory.createAgent("agent" + Arrays.toString(labels), labels);
    	}
    	else {
    		throw new IllegalArgumentException("Unknown mode: " + mode);
    	}
    }
    
    @Test
    public void inproc_test() throws Exception {
    	runTest("in-proc");
    }

    @Test
    public void local_test() throws Exception {
    	runTest("local");
    }
    
    public void runTest(String mode) throws Exception {        
        RemoteAgent sinAgent = createAgent(mode, SIN);
        RemoteAgent cosAgent = createAgent(mode, COS);
        
        Director director = new Director(Arrays.asList(sinAgent, cosAgent));

        SimpleStatsAggregator wAggr = new QueuedSimpleStatsAggregator();        
        SimpleStatsAggregator rAggr = new QueuedSimpleStatsAggregator();
        
        Play play;
        
        try {
            play = director.play(getScenario(WARMUP_NUMBERS, WARMUP_ITERATIONS, WARMUP_DURATION, wAggr));
            play.getCompletionFuture().get();
            
            play = director.play(getScenario(NUMBERS, ITERATIONS, DURATION, rAggr));
            play.getCompletionFuture().get();
        } finally {
            director.shutdown(false);
        }
        
        SimpleStats stats = rAggr.calculate();
        
        TablePrinter tablePrinter = new PrettyPrinter();
        
        SimpleStatsTablePrinter statsPrinter = new SimpleStatsTablePrinter();

        statsPrinter.setStatsPrinters(Collections.<SimpleStatsLinePrinter>singletonList(new ThroughputLatencyPrinter()));
        statsPrinter.print(System.err, tablePrinter, stats);
        
        System.err.println();
        
        statsPrinter.setStatsPrinters(Collections.<SimpleStatsLinePrinter>singletonList(new ProcCpuPrinter()));
        statsPrinter.print(System.err, tablePrinter, stats);

        System.err.println();
        
        statsPrinter.setStatsPrinters(Collections.<SimpleStatsLinePrinter>singletonList(new SysCpuPrinter()));
        statsPrinter.print(System.err, tablePrinter, stats);
        
        System.err.println();
        
        statsPrinter.setStatsPrinters(Collections.<SimpleStatsLinePrinter>singletonList(new NetInterfacePrinter()));
        statsPrinter.print(System.err, tablePrinter, stats);
        
        System.err.println();
        
        statsPrinter.setStatsPrinters(Collections.<SimpleStatsLinePrinter>singletonList(new WhitelistPrinter()));
        statsPrinter.print(System.err, tablePrinter, stats);
    }
    
    @SuppressWarnings("unchecked")
    private static Scenario getScenario(long numbers, long iterations, long duration, SimpleStatsAggregator aggr) {
        Task sinInitTask = new InitTask("SinInitTask", SIN, new Sin());
        Task cosInitTask = new InitTask("CosInitTask", COS, new Cos());
        Task tanInitTask = new InitTask("TanInitTask", TAN, new Tan());
        
        TaskSLA sinSLA = new TaskSLA();
        sinSLA.setLabels(Collections.singleton(SIN));

        TaskSLA cosSLA = new TaskSLA();
        cosSLA.setLabels(Collections.singleton(COS));
        
        TaskSLA tanSLA = new TaskSLA();
        tanSLA.setLabels(new HashSet<String>(Arrays.asList(SIN, COS)));
        
        Scenario sinInitScen = new TaskScenario(
            sinInitTask.toString(), Collections.singleton(sinInitTask), sinSLA, aggr
        );
        
        Scenario cosInitScen = new TaskScenario(
            cosInitTask.toString(), Collections.singleton(cosInitTask), cosSLA, aggr
        );
        
        Scenario tanInitScen = new TaskScenario(
            tanInitTask.toString(), Collections.singleton(tanInitTask), tanSLA, aggr
        );
        
        sinSLA = sinSLA.clone();
        sinSLA.setIterationsCount(iterations);
        
        cosSLA = cosSLA.clone();
        cosSLA.setIterationsCount(iterations);
        
        tanSLA = tanSLA.clone();
        tanSLA.setFinishDelay(duration, TimeUnit.SECONDS);
        tanSLA.setIterationsCount(null);
        
        List<Task> sinTasks = new ArrayList<Task>();
        List<Task> cosTasks = new ArrayList<Task>();
        List<Task> tanTasks = new ArrayList<Task>();
        
        for (long i = 1; i <= numbers; ++i) {
            sinTasks.add(new CalcTask("SinCalcTask#"+i, SIN, i));
            cosTasks.add(new CalcTask("CosCalcTask#"+i, COS, i));
            tanTasks.add(new CalcTask("TanCalcTask#"+i, TAN, i));
        }

        Scenario sinCalcScen = new TaskScenario(
            "sin cals scen", sinTasks, sinSLA, new SimpleStatsReporterFactory(aggr)
        );
            
        Scenario cosCalcScen = new TaskScenario(
            "cos cals scen", cosTasks, cosSLA, new SimpleStatsReporterFactory(aggr)
        );
            
        Scenario tanCalsScen = new TaskScenario(
            "tan cals scen", tanTasks, tanSLA, new SimpleStatsReporterFactory(aggr)
        );

        Scenario init = new ParScenario(Arrays.asList(sinInitScen, cosInitScen, tanInitScen));
        
        StatsReporter tanCpuRep  = new AggregatingSimpleStatsReporter(aggr, 1);
        StatsReporter netStatRep = new AggregatingSimpleStatsReporter(aggr, 1);
        StatsReporter sysCpuRep  = new AggregatingSimpleStatsReporter(aggr, 1);
        
        SensorDemon<?> tanCpuDemon = new SensorDemon<List<ProcCpuSensor.ProcCpuMeasure>>(
            new ProcCpuSensor(newPidProvider()), new ProcCpuReporter("TAN_CPU", tanCpuRep)
        );

        SensorDemon<?> sysCpuDemon = new SensorDemon<IntervalMeasure<Cpu>>(
            new SysCpuSensor(), new SysCpuReporter("SYS_CPU", sysCpuRep)
        );
        
        SensorDemon<?> netStatsDemon = new SensorDemon<List<NetInterfaceSensor.InterfaceMeasure>>(
            new NetInterfaceSensor(), new NetInterfaceReporter("net", netStatRep)
        );
        
        Scenario first = new ParScenario(Arrays.asList(sinCalcScen, cosCalcScen));
        
        Scenario second = DemonScenario.newInstance(
            "TanCpu", tanCalsScen, Collections.singleton(COS), Arrays.<Callable<Void>>asList(tanCpuDemon, sysCpuDemon)
        );

        Scenario whole = new SeqScenario(Arrays.asList(init, first, second));
        
        return DemonScenario.newInstance("Whole", whole, Collections.singleton(SIN), Arrays.<Callable<Void>>asList(netStatsDemon));
    }    
    
    private static PidProvider newPidProvider() {
        Predicate<Properties> propsPred = new Predicate<Properties>() {
            @Override
            public boolean apply(Properties input) {
                return "a".equals(input.getProperty("a"));
            }
        };
        
        Predicate<String> namePred = Predicates.and(Predicates.alwaysTrue(), Predicates.containsPattern("Bootstraper"));
        
        return new PidProvider.JavaPidProvider(namePred, propsPred);
    }
    
    @SuppressWarnings("serial")
    public static class InitTask implements Task {
        private final String name;
        private final String funcName;
        private final Function<Double, Double> func;

        public InitTask(String name, String funcName, Function<Double, Double> func) {
            this.name = name;
            this.funcName = funcName;
            this.func = func;
        }

        @Override
        public void excute(Context context) throws Exception {
            System.setProperty("a", "a");
            Thread.sleep(250);
            context.getLogger().info("log " + name);
            context.getAttrsMap().put(funcName, func);
        }

        @Override
        public String toString() {
            return name;
        }
    }
    
    @SuppressWarnings("serial")
    public static class CalcTask implements Task {
        private final String name;
        private final String funcName;
        private final double value;
        
        public CalcTask(String name, String funcName, double value) {
            this.name = name;
            this.funcName = funcName;
            this.value = value;
        }

        @Override
        public void excute(Context context) throws Exception {            
            ThroughputLatencyReporter reporter = new ThroughputLatencyReporter(context.getStatReporter(), context.getTimeService());
            
            String initStats = initStats(funcName);
            String calsStats = calcStats(funcName);
            
            reporter.start(initStats);
            @SuppressWarnings("unchecked")
            Function<Double, Double> func = (Function<Double, Double>)context.getAttrsMap().get(funcName);
            reporter.finish(initStats);
            
            reporter.start(calsStats);
            func.apply(value);
            reporter.finish(calsStats);
        }

        @Override
        public String toString() {
            return name;
        }
    }
    
    public static String initStats(String name) {
        return "get_" + name;
    }
    
    public static String calcStats(String name) {
        return "calc_" + name;
    }
    
    @SuppressWarnings("serial")
    public static class Sin implements Function<Double, Double>, Serializable {
        @Override
        public Double apply(Double arg) {
            return Math.sin(arg);
        }
    }
    
    @SuppressWarnings("serial")
    public static class Cos implements Function<Double, Double>, Serializable {
        @Override
        public Double apply(Double arg) {
            return Math.cos(arg);
        }
    }
    
    @SuppressWarnings("serial")
    public static class Tan implements Function<Double, Double>, Serializable {
        @Override
        public Double apply(Double arg) {
            return Math.tan(arg);
        }
    }
}
