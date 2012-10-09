package org.gridkit.nimble.statistics.simple;

import org.junit.Ignore;

@Ignore
public class PrintTest {
    /*
    public static void main(String[] args) throws InterruptedException, IOException {
        SimpleStats stats = gerateStats();
        
        SimplePrettyPrinter prettyPrinter = new SimplePrettyPrinter(Arrays.asList("A", "B"), Arrays.asList("Y", "Z"));
        
        SimpleCsvPrinter csvPrinter = new SimpleCsvPrinter();
        
        prettyPrinter.print(System.err, stats, Arrays.asList("1", "2"), Arrays.asList("9", "0"));
        System.err.println();
        
        prettyPrinter.print(System.err, stats, Arrays.asList("1"), Collections.<String>emptyList());
        System.err.println();
        
        csvPrinter.print(System.err, stats);
        System.err.println();
    }

    public static SimpleStats gerateStats() throws InterruptedException {
        SimpleStatsProducer producer = new SimpleStatsProducer();
        SmartReporter reporter = new SmartReporter(producer, timeService);
        
        for (int i = 0; i < 5; ++i) {
            reporter.start("sleep");
            Thread.sleep(10);
            reporter.describe("sleep", "i", i);
            reporter.finish("sleep");
        }
        
        for (int s = 0; s < 5; ++s) {
            reporter.start("sin");
            double sin = Math.sin(s);
            Thread.sleep(2);
            reporter.describe("sin", "s", sin);
            reporter.finish("sin");
        }
        
        return producer.produce();
    }
    
    public static TimeService timeService = new TimeService() {
        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public long currentTimeNanos() {
            return System.nanoTime();
        }
    };
    */
}
