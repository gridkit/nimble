package org.gridkit.nimble.sensor;

import static org.gridkit.nimble.statistics.simple.SimpleStatsOps.mark;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.statistics.StatsReporter;

public class NumericMaxReporter implements Sensor.Reporter<Map<?, Object>>, Serializable {
	
	private static final long serialVersionUID = 20121005L;
	
	private String statistica;
    private StatsReporter statsReporter;

    public NumericMaxReporter(String statistica, StatsReporter statsReporter) {
        this.statistica = statistica;
        this.statsReporter = statsReporter;
    }

	@Override
	public void report(Map<?, Object> m) {
		int count = m.size();
		if (count > 0) {
			double max = Double.MIN_VALUE;
			for(Object v: m.values()) {
				double d = ((Number)v).doubleValue();
				if (d > max) {
					max = d;
				}
			}
			
            Map<String, Object> sample = new HashMap<String, Object>();
            
            if (max > Double.MIN_VALUE) {
            	sample.put(mark("avgmax", statistica, "max"), max);
            }
			
			statsReporter.report(sample);
		}
	}
}
