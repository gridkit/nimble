package org.gridkit.nimble.pivot.display;

import java.util.Arrays;

import org.gridkit.nimble.pivot.CommonStats;
import org.gridkit.nimble.pivot.Extractors;

public class DisplayFactory {

	public static SimpleDisplayComponent constant(String caption, Object value) {
		return new SimpleDisplayComponent(caption, Extractors.constant(value));
	}

	public static SimpleDisplayComponent attribute(Object key) {
		return new SimpleDisplayComponent(String.valueOf(key), Extractors.field(key));
	}

	public static SimpleDisplayComponent attribute(String caption, Object key) {
		return new SimpleDisplayComponent(caption, Extractors.field(key));
	}

	public static StatsDisplayComponent genericStats(Object key, CommonStats.StatAppraisal... stats) {
		return new StatsDisplayComponent(Extractors.field(key), stats);
	}

	public static StatsDisplayComponent genericStats(String captionFormat, Object key, CommonStats.StatAppraisal... stats) {
		return new StatsDisplayComponent(captionFormat, Extractors.field(key), stats);
	}
	
	public static StatsDisplayComponent distributionStats(Object key) {
		return new StatsDisplayComponent(Extractors.field(key), CommonStats.DISTRIBUTION_STATS);
	}

	public static StatsDisplayComponent frequencyStats(Object key) {
		return new StatsDisplayComponent(Extractors.field(key), CommonStats.FREQUENCY_STATS);
	}
	
	public static DisplayComponent decorated(String captionFormat, DisplayComponent component, Object... deco) {
		return new DecorationAdapter(captionFormat, Arrays.asList(deco), component);
	}

	public static DisplayComponent decorated(DisplayComponent component, Object... deco) {
		return new DecorationAdapter(Arrays.asList(deco), component);
	}	
}
