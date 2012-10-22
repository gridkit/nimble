package org.gridkit.nimble.pivot.display;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.CommonStats;
import org.gridkit.nimble.pivot.SampleExtractor;
import org.gridkit.nimble.statistics.Summary;

public class StatsDisplayComponent implements DisplayComponent {

	private final String captionFormat;
	private final SampleExtractor extractor;
	private final CommonStats.StatAppraisal[] appraisals;
	
	public StatsDisplayComponent(SampleExtractor extractor, CommonStats.StatAppraisal... stats) {
		this("%s", extractor, stats);
	}
	
	public StatsDisplayComponent(String captionFormat, SampleExtractor extractor, CommonStats.StatAppraisal... stats) {
		this.captionFormat = captionFormat;
		this.extractor = extractor;
		this.appraisals = stats;
	}

	@Override
	public Map<String, Object> display(SampleReader reader) {
		Object x = extractor.extract(reader);
		if (x instanceof Summary) {
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			
			Summary ss = (Summary) x;
			for(CommonStats.StatAppraisal m: appraisals) {
				result.put(String.format(captionFormat, m.toString()), m.extract(ss));
			}
			return result;
		}
		else {
			return Collections.emptyMap();
		}
	}
}
