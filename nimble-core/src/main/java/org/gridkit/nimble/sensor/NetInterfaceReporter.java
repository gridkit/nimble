package org.gridkit.nimble.sensor;

import static org.gridkit.nimble.statistics.simple.SimpleStatsOps.mark;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.sensor.NetInterfaceSensor.InterfaceMeasure;
import org.gridkit.nimble.statistics.StatsOps;
import org.gridkit.nimble.statistics.StatsReporter;

@SuppressWarnings("serial")
public class NetInterfaceReporter implements Sensor.Reporter<List<NetInterfaceSensor.InterfaceMeasure>>, Serializable {
    public static final String SAMPLER_NAME = "ni";
    
    public static String SENT_BYTES     = "sent";
    public static String RECEIVED_BYTES = "received";
    public static String MS             = "ms";
    
    private String statistica;
    private Set<String> interfaces;
    private StatsReporter reporter;
    
    public NetInterfaceReporter(String statistica, Set<String> interfaces, StatsReporter reporter) {
        this.statistica = statistica;
        this.reporter = reporter;
        this.interfaces = interfaces;
    }
    
    public NetInterfaceReporter(String statistica, StatsReporter reporter) {
        this(statistica, null, reporter);
    }

    @Override
    public void report(List<InterfaceMeasure> measures) {        
        for (InterfaceMeasure measure : measures) {
            if (isReported(measure.getInterfaceName())) {
                long sent = measure.getRightState().getTxBytes() - measure.getLeftState().getTxBytes();
                long received = measure.getRightState().getRxBytes() -  measure.getLeftState().getRxBytes();
                                            
                Map<String, Object> sample = new HashMap<String, Object>();
                
                sample.put(mark(SAMPLER_NAME, statistica, measure.getInterfaceName(), SENT_BYTES),     sent);
                sample.put(mark(SAMPLER_NAME, statistica, measure.getInterfaceName(), RECEIVED_BYTES), received);
                
                sample.put(
                    mark(SAMPLER_NAME, statistica, measure.getInterfaceName(), MS),
                    StatsOps.convert(measure.getRightTsNs() - measure.getLeftTsNs(), TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS)
                );
                
                reporter.report(sample);
            }
         }
    }
    
    private boolean isReported(String inter) {
        return interfaces == null ? true : interfaces.contains(inter);
    }
}
