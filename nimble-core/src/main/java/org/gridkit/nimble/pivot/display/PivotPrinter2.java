package org.gridkit.nimble.pivot.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.PivotReporter;
import org.gridkit.nimble.print.LinePrinter;
import org.gridkit.nimble.print.LinePrinter.Context;

public class PivotPrinter2 {
	
	private final List<DisplayComponent> components = new ArrayList<DisplayComponent>();
	
	private boolean dumpUnprinted;
	
	public void add(DisplayComponent component) {
		this.components.add(component);
	}

	public void add(String pattern, DisplayComponent component) {
		this.components.add(new LevelFilter(GlobHelper.translate(pattern, "."), component));
	}
	
	public void dumpUnprinted() {
		dumpUnprinted = true;
	}

	
	public LinePrinter print(final SampleReader reader) {
		return new LinePrinter() {
			@Override
			public void print(Context context) {
				if (reader.isReady() || reader.next()) {
					while(true) {
						printLine(context, reader);
						if (!reader.next()) {
							break;
						}
					}
				}
			}
		};		
	}
	
	private void printLine(Context context, SampleReader reader) {
		SingleSampleReader row = new SingleSampleReader(reader);
		for(DisplayComponent unit: components) {
			Map<String, Object> cells = unit.display(row);
			for(String key: cells.keySet()) {
				context.cell(key, cells.get(key));
			}			
		}
		if (dumpUnprinted) {
			List<Object> keys = reader.keySet();
			for(Object key: keys) {
				if (!row.touchedKeys.contains(key)) {
					context.cell(String.valueOf(key), reader.get(key));
				}
			}
		}
		context.newline();
	}

	public static class LevelFilter implements DisplayComponent {
		
		private final Pattern filter;
		private final DisplayComponent unit;
		
		public LevelFilter(Pattern filter, DisplayComponent unit) {
			this.filter = filter;
			this.unit = unit;
		}

		@Override
		public Map<String, Object> display(SampleReader reader) {
			String level = (String) reader.get(PivotReporter.LEVEL_KEY);
			if (filter.matcher(level).matches()) {
				return unit.display(reader);
			}
			else {
				return Collections.emptyMap();
			}
		}
	}
	
	
	private static class SingleSampleReader implements SampleReader {

		final List<Object> touchedKeys = new ArrayList<Object>();
		final SampleReader reader;
		
		public SingleSampleReader(SampleReader reader) {
			this.reader = reader;
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public boolean next() {
			return false;
		}

		@Override
		public List<Object> keySet() {
			return reader.keySet();
		}

		@Override
		public Object get(Object key) {
			if (!touchedKeys.contains(key)) {
				touchedKeys.add(key);
			}
			return reader.get(key);
		}
	}
}
