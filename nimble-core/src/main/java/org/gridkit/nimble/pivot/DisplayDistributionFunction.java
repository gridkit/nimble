package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.statistics.DistributionSummary;

class DisplayDistributionFunction implements DisplayFunction {

	private final SampleExtractor extractor;
	private final CommonStats.StatAppraisal[] appraisals;
	
	DisplayDistributionFunction(SampleExtractor extractor, CommonStats.StatAppraisal... stats) {
		this.extractor = extractor;
		this.appraisals = stats;
	}

	@Override
	public void getDisplayValue(CellPrinter printer, SampleReader row) {
		Object x = extractor.extract(row);
		if (x instanceof DistributionSummary) {
			DistributionSummary ss = (DistributionSummary) x;
			for(CommonStats.StatAppraisal m: appraisals) {
				printer.addCell(m.toString(), m.extract(ss));
			}
		}
	}
}
