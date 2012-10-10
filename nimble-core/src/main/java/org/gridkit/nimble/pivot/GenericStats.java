package org.gridkit.nimble.pivot;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

public class GenericStats {

	
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
		},
		MEAN() {
			@Override
			public Object extract(StatisticalSummary summary) {
				return summary.getMean();
			}			
		},
		STD_DEV() {
			@Override
			public Object extract(StatisticalSummary summary) {
				return summary.getStandardDeviation();
			}			
		},
		MIN() {
			@Override
			public Object extract(StatisticalSummary summary) {
				return summary.getMin();
			}			
		},
		MAX() {
			@Override
			public Object extract(StatisticalSummary summary) {
				return summary.getMax();
			}			
		},
		TOTAL() {
			@Override
			public Object extract(StatisticalSummary summary) {
				return summary.getSum();
			}			
		}
		
		;
		
		public abstract Object extract(StatisticalSummary summary);
	}
}
