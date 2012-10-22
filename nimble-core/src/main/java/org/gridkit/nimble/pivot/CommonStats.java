package org.gridkit.nimble.pivot;

import org.gridkit.nimble.statistics.DistributionSummary;
import org.gridkit.nimble.statistics.FrequencySummary;
import org.gridkit.nimble.statistics.Summary;
import org.gridkit.nimble.statistics.Summary.CountSummary;
import org.gridkit.nimble.statistics.Summary.SumSummary;

public class CommonStats {

	public static final StatAppraisal COUNT = StatAppraisal.COUNT;
	public static final StatAppraisal MAX = StatAppraisal.MAX;
	public static final StatAppraisal MIN = StatAppraisal.MIN;
	public static final StatAppraisal MEAN = StatAppraisal.MEAN;
	public static final StatAppraisal STD_DEV = StatAppraisal.STD_DEV;
	public static final StatAppraisal TOTAL = StatAppraisal.TOTAL;
	public static final StatAppraisal DURATION = StatAppraisal.DURATION;
	public static final StatAppraisal FREQUENCY = StatAppraisal.FREQUENCY;
	
	public static final StatAppraisal[] DISTRIBUTION_STATS = {COUNT, MEAN, STD_DEV, MAX, MIN, TOTAL}; 
	public static final StatAppraisal[] FREQUENCY_STATS = {COUNT, FREQUENCY, TOTAL, DURATION};
	
	public enum StatAppraisal {
		
		COUNT() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof CountSummary ? ((CountSummary)summary).getN() : null;
			}
			
			@Override
			public String toString() {
				return "Count";
			}
		},
		MEAN() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof DistributionSummary ? ((DistributionSummary)summary).getMean() : null;
			}

			@Override
			public String toString() {
				return "Mean";
			}			
		},
		STD_DEV() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof DistributionSummary ? ((DistributionSummary)summary).getStandardDeviation() : null;
			}
			
			@Override
			public String toString() {
				return "StdDev";
			}						
		},
		MIN() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof DistributionSummary ? ((DistributionSummary)summary).getMin() : null;
			}
			
			@Override
			public String toString() {
				return "Min";
			}									
		},
		MAX() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof DistributionSummary ? ((DistributionSummary)summary).getMax() : null;
			}
			
			@Override
			public String toString() {
				return "Max";
			}												
		},
		TOTAL() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof SumSummary ? ((SumSummary)summary).getSum() : null;
			}
			
			@Override
			public String toString() {
				return "Total";
			}															
		},
		DURATION() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof FrequencySummary ? ((FrequencySummary)summary).getDuration() : null;
			}
			
			@Override
			public String toString() {
				return "Duration";
			}															
		},
		FREQUENCY() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof FrequencySummary ? ((FrequencySummary)summary).getWeigthedFrequency() : null;
			}
			
			@Override
			public String toString() {
				return "Freq.";
			}															
		}
		
		;
		
		public abstract Object extract(Summary summary);
	}
}
