package org.gridkit.nimble.sensor;

import static org.gridkit.nimble.statistics.simple.SimpleStatsOps.mark;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.nimble.statistics.StatsReporter;

public class NumericAverageReporter implements Sensor.Reporter<Map<?, Object>>, Serializable {
	
	private static final long serialVersionUID = 20121005L;
	
	private String statistica;
    private StatsReporter statsReporter;

    public NumericAverageReporter(String statistica, StatsReporter statsReporter) {
        this.statistica = statistica;
        this.statsReporter = statsReporter;
    }

	@Override
	public void report(Map<?, Object> m) {
		if (m.size() > 0) {
			double total = 0;
			int count = 0;
			for(Object v: m.values()) {
				double d = ((Number)v).doubleValue();
				if (!Double.isNaN(d)) {
					total += d;
					++count;
				}
			}
			
			if (count > 0) {
				double avg = total / count;
//				System.out.println("AVG: " + statistica + " -> " + avg + " total: " + total + " count:" + count);
	            Map<String, Object> sample = new HashMap<String, Object>();
	            sample.put(mark("avgmax", statistica, "avg"), avg);
				
				statsReporter.report(sample);
			}
		}
	}
}
