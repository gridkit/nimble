package org.gridkit.nimble.pivot;

import org.gridkit.nimble.pivot.display.UnitDeco;
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
	public static final StatAppraisal VARIANCE = StatAppraisal.VARIANCE;
	public static final StatAppraisal SUM = StatAppraisal.SUM;
	public static final StatAppraisal DURATION = StatAppraisal.DURATION;
	public static final StatAppraisal FREQUENCY = StatAppraisal.FREQUENCY;
	
	public static final StatAppraisal[] DISTRIBUTION_STATS = {COUNT, MEAN, STD_DEV, MIN, MAX, SUM}; 
	public static final StatAppraisal[] FREQUENCY_STATS = {COUNT, FREQUENCY, SUM, DURATION};
	
	public enum StatAppraisal {
		
		COUNT() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof CountSummary ? ((CountSummary)summary).getN() : null;
			}
			
			@Override
			public double transalte(UnitDeco deco) {
				return 1;
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
			public double transalte(UnitDeco deco) {
				switch(deco.getType()) {
				case NUMERATOR: return deco.getMultiplier();
				case DENOMINATOR: return 1;
				default: throw new Error();
				}
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
			public double transalte(UnitDeco deco) {
				switch(deco.getType()) {
				case NUMERATOR: return deco.getMultiplier();
				case DENOMINATOR: return 1;
				default: throw new Error();
				}
			}

			@Override
			public String toString() {
				return "StdDev";
			}						
		},
		VARIANCE() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof DistributionSummary ? ((DistributionSummary)summary).getVariance() : null;
			}
			
			@Override
			public double transalte(UnitDeco deco) {
				switch(deco.getType()) {
				case NUMERATOR: return deco.getMultiplier();
				case DENOMINATOR: return 1;
				default: throw new Error();
				}
			}
			
			@Override
			public String toString() {
				return "Variance";
			}									
		},
		MIN() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof DistributionSummary ? ((DistributionSummary)summary).getMin() : null;
			}
			
			@Override
			public double transalte(UnitDeco deco) {
				switch(deco.getType()) {
				case NUMERATOR: return deco.getMultiplier();
				case DENOMINATOR: return 1;
				default: throw new Error();
				}
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
			public double transalte(UnitDeco deco) {
				switch(deco.getType()) {
				case NUMERATOR: return deco.getMultiplier();
				case DENOMINATOR: return 1;
				default: throw new Error();
				}
			}
			
			@Override
			public String toString() {
				return "Max";
			}												
		},
		SUM() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof SumSummary ? ((SumSummary)summary).getSum() : null;
			}
			
			@Override
			public double transalte(UnitDeco deco) {
				switch(deco.getType()) {
				case NUMERATOR: return deco.getMultiplier();
				case DENOMINATOR: return 1;
				default: throw new Error();
				}
			}

			@Override
			public String toString() {
				return "Sum";
			}															
		},
		DURATION() {
			@Override
			public Object extract(Summary summary) {
				return summary instanceof FrequencySummary ? ((FrequencySummary)summary).getDuration() : null;
			}
			
			@Override
			public double transalte(UnitDeco deco) {
				switch(deco.getType()) {
				case NUMERATOR: return 1;
				case DENOMINATOR: return 1 / deco.getMultiplier();
				default: throw new Error();
				}
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
			public double transalte(UnitDeco deco) {
				switch(deco.getType()) {
				case NUMERATOR: return deco.getMultiplier();
				case DENOMINATOR: return deco.getMultiplier();
				default: throw new Error();
				}
			}
			
			@Override
			public String toString() {
				return "Freq.";
			}															
		}
		
		;
		
		public abstract Object extract(Summary summary);
		
		public abstract double transalte(UnitDeco deco);
	}
}
