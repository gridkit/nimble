package org.gridkit.nimble.pivot.display;

import java.util.Collections;
import java.util.Map;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.SampleExtractor;

public class SimpleDisplayComponent implements DisplayComponent {

	private String caption;
	private SampleExtractor extractor;
	private Formatter formater;
	
	SimpleDisplayComponent(String caption, SampleExtractor extractor) {
		this.caption = caption;
		this.extractor = extractor;
	}

	@Override
	public Map<String, Object> display(SampleReader reader) {		
		Object extract = extractor.extract(reader);
		if (formater != null) {
			extract = formater.format(extract);
		}
		return Collections.singletonMap(caption, extract);
	}
}
