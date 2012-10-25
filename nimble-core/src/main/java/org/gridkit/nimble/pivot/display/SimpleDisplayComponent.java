package org.gridkit.nimble.pivot.display;

import java.util.Collections;
import java.util.Map;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.SampleExtractor;

public class SimpleDisplayComponent implements DisplayComponent, DisplayConfigurable {

	private String caption;
	private SampleExtractor extractor;
	private Formatter formater;
	private UnitDeco deco;
	
	SimpleDisplayComponent(String caption, SampleExtractor extractor) {
		this.caption = caption;
		this.extractor = extractor;
	}

	@Override
	public Map<String, Object> display(SampleReader reader) {		
		Object extract = extractor.extract(reader);
		if (deco != null) {
			if (extract != null) {
				if (extract instanceof Number) {
					extract = deco.getMultiplier() * (((Number) extract).doubleValue());
				}
				else {
					throw new IllegalArgumentException("Usage of UnitDeco is allowed only with numric values");
				}
			}
		}
		if (formater != null) {
			extract = formater.format(extract);
		}
		return Collections.singletonMap(caption, extract);
	}

	@Override
	public void setCaption(String caption) {
		this.caption = caption;
	}

	public void setUnits(UnitDeco units) {
		this.deco = units;
	}
}
