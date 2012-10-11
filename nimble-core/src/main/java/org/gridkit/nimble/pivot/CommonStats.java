package org.gridkit.nimble.pivot;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

public class CommonStats {

	
	public static final StatAppraisal[] GENERIC_STATS = StatAppraisal.values();
	
	public static final StatAppraisal COUNT = StatAppraisal.COUNT;
	public static final StatAppraisal MAX = StatAppraisal.MAX;
	public static final StatAppraisal MIN = StatAppraisal.MIN;
	public static final StatAppraisal MEAN = StatAppraisal.MEAN;
	public static final StatAppraisal STD_DEV = StatAppraisal.STD_DEV;
	public static final StatAppraisal TOTAL = StatAppraisal.TOTAL;
	
	enum StatAppraisal {
		
		COUNT() {
			@Override
			public Object extract(StatisticalSummary summary) {
				return summary.getN();
			}
			
			@Override
			public String toString() {
				return "Count";
			}
		},
		MEAN() {
			@Override
			public Object extract(StatisticalSummary summary) {
				return summary.getMean();
			}

			@Override
			public String toString() {
				return "Mean";
			}			
		},
		STD_DEV() {
			@Override
			public Object extract(StatisticalSummary summary) {
				return summary.getStandardDeviation();
			}
			
			@Override
			public String toString() {
				return "SDev";
			}						
		},
		MIN() {
			@Override
			public Object extract(StatisticalSummary summary) {
				return summary.getMin();
			}
			
			@Override
			public String toString() {
				return "Min";
			}									
		},
		MAX() {
			@Override
			public Object extract(StatisticalSummary summary) {
				return summary.getMax();
			}
			
			@Override
			public String toString() {
				return "Max";
			}												
		},
		TOTAL() {
			@Override
			public Object extract(StatisticalSummary summary) {
				return summary.getSum();
			}
			
			@Override
			public String toString() {
				return "Total";
			}															
		}
		
		;
		
		public abstract Object extract(StatisticalSummary summary);
	}
}
