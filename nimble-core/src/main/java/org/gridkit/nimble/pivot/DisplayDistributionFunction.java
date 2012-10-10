package org.gridkit.nimble.pivot;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.gridkit.nimble.metering.SampleReader;

class DisplayDistributionFunction implements DisplayFunction {

	private final Pivot.Extractor extractor;
	private final GenericStats.StatAppraisal[] appraisals;
	
	DisplayDistributionFunction(Pivot.Extractor extractor, GenericStats.StatAppraisal... stats) {
		this.extractor = extractor;
		this.appraisals = stats;
	}

	@Override
	public void getDisplayValue(CellPrinter printer, SampleReader row) {
		Object x = extractor.extract(row);
		if (x instanceof StatisticalSummary) {
			StatisticalSummary ss = (StatisticalSummary) x;
			for(GenericStats.StatAppraisal m: appraisals) {
				printer.addCell(m.toString(), m.extract(ss));
			}
		}
	}
}
