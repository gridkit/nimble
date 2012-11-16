package org.gridkit.nimble.pivot.display;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.Extractors;
import org.gridkit.nimble.pivot.Filters;
import org.gridkit.nimble.pivot.PivotReporter;
import org.gridkit.nimble.pivot.SampleExtractor;
import org.gridkit.nimble.pivot.SampleFilter;
import org.gridkit.nimble.print.LinePrinter;
import org.gridkit.nimble.print.LinePrinter.Context;

public class PivotPrinter2 implements PrintConfig {
	
	private final static LineKey LINE_KEY = LineKey.LINE_KEY;
	private enum LineKey { LINE_KEY };
	
	private final List<SampleExtractor> sortOrder = new ArrayList<SampleExtractor>();
	private final List<SampleFilter> filters = new ArrayList<SampleFilter>();
	private final List<DisplayComponent> components = new ArrayList<DisplayComponent>();	
	
	private boolean dumpUnprinted;
	
	@Override
	public void sortBy(SampleExtractor extractor) {
		sortOrder.add(extractor);
	}

	@Override
	public void sortByField(Object... key) {
		for(Object k: key) {
			sortOrder.add(Extractors.field(k));
		}		
	}

	@Override
	public void sortByColumn(String... colName) {
		for(String col: colName) {
			sortOrder.add(new ColumnExtractor(col));
		}
	}
	
	public void filter(String... patterns) {
		if (patterns.length == 0) {
			throw new IllegalArgumentException("Should not be empty");
		}
		else if (patterns.length == 1) {
			filters.add(new LevelSampleFilter(GlobHelper.translate(patterns[0], ".")));
		}
		else {
			SampleFilter[] f = new SampleFilter[patterns.length];
			for(int i = 0; i != patterns.length; ++i ){
				f[i] = new LevelSampleFilter(GlobHelper.translate(patterns[i], "."));
			}
			filters.add(Filters.or(f));
		}
	}

	public void filter(SampleFilter filter) {
		filters.add(filter);
	}
	
	@Override
	public void add(DisplayComponent component) {
		this.components.add(component);
	}

	@Override
	public void add(String pattern, DisplayComponent component) {
		this.components.add(new LevelFilter(GlobHelper.translate(pattern, "."), component));
	}
	
	public void dumpUnprinted() {
		dumpUnprinted = true;
	}
	
	public LinePrinter print(final SampleReader reader) {

		final Printer printer;
		if (sortOrder.isEmpty()) {
			printer = new DirectPrinter();
		}
		else {
			printer = new SortingPrinter();
		}
		
		return new LinePrinter() {
			@Override
			public void print(Context context) {
				if (reader.isReady() || reader.next()) {
					while(true) {
						printLine(printer, context, reader);
						if (!reader.next()) {
							printer.print(context);
							break;
						}
					}
				}
			}
		};		
	}
	
	void printLine(Printer printer, Context context, SampleReader reader) {
		for(SampleFilter sf : filters) {
			if (!sf.match(reader)) {
				return;
			}
		}
		Map<String, Object> line = new LinkedHashMap<String, Object>();
		SingleSampleReader row = new SingleSampleReader(reader);
		for(DisplayComponent unit: components) {
			Map<String, Object> cells = unit.display(row);
			for(String key: cells.keySet()) {
				Object cell = cells.get(key);
				if (line.containsKey(key) && cell != null) {
					if (line.get(key) != null) {
						throw new IllegalArgumentException("Cell value overlaps: " + key);
					}
				}
				if (cell != null || !line.containsKey(key)) {
					line.put(key, cell);
				}				
			}			
		}
		if (dumpUnprinted) {
			List<Object> keys = reader.keySet();
			for(Object key: keys) {
				if (!row.touchedKeys.contains(key)) {
					line.put(String.valueOf(key), reader.get(key));
				}
			}
		}
		if (!line.isEmpty()) {
			printer.printLine(context, reader, line);
		}
	}

	private Object[] extractSortKey(SampleReader reader) {
		Object[] sortKey = new Object[sortOrder.size()];
		for(int i = 0; i != sortKey.length; ++i) {
			sortKey[i] = sortOrder.get(i).extract(reader);
		}
		return sortKey;
	}
	
	private static class PrintedRawSampleReader implements SampleReader {
		
		final SampleReader reader;
		final Map<String, Object> line;
		
		public PrintedRawSampleReader(SampleReader reader, Map<String, Object> line) {
			this.reader = reader;
			this.line = line;
		}

		@Override
		public boolean isReady() {
			return reader.isReady();
		}

		@Override
		public boolean next() {
			return reader.next();
		}

		@Override
		public List<Object> keySet() {
			return reader.keySet();
		}

		@Override
		public Object get(Object key) {
			return key == LINE_KEY ? line : reader.get(key);
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
			if (key != PivotReporter.LEVEL_KEY && !touchedKeys.contains(key)) {
				touchedKeys.add(key);
			}
			return reader.get(key);
		}
	}
	
	private static interface Printer {
		
		public void printLine(Context ctx, SampleReader reader, Map<String, Object> line);
	
		public void print(Context ctx);
	}

	private static class DirectPrinter implements Printer {
		
		public void printLine(Context ctx, SampleReader reader, Map<String, Object> line) {
			for(String key: line.keySet()) {
				ctx.cell(key, line.get(key));
			}
			ctx.newline();
		}

		@Override
		public void print(Context ctx) {
			// do nothing
		}			
	}

	private class SortingPrinter implements Printer {
		
		private List<PrintRow> rows = new ArrayList<PrintRow>();
		
		public void printLine(Context ctx, SampleReader reader, Map<String, Object> line) {
			Object[] sortKey = extractSortKey(new PrintedRawSampleReader(reader, line));
			rows.add(new PrintRow(sortKey, line));
		}		
		
		public void print(Context ctx) {
			Collections.sort(rows);
			for(PrintRow row: rows) {
				row.print(ctx);
				ctx.newline();
			}
		}
	}
	
	private static class PrintRow implements LinePrinter, Comparable<PrintRow> {
		
		private Object[] sortKey;
		private Map<String, Object> cells;

		public PrintRow(Object[] sortKey, Map<String, Object> cells) {
			this.sortKey = sortKey;
			this.cells = cells;
		}

		@Override
		public int compareTo(PrintRow o) {
			int c = 0;
			for(int i = 0; i != sortKey.length; ++i) {
				c = compare(o, i);
				if (c != 0) {
					return c;
				}
			}
			return 0;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private int compare(PrintRow o, int i) {
			if (sortKey[i] == null && o.sortKey[i] == null) {
				return 0;
			}
			else if (sortKey[i] == null) {
				return -1;
			}
			else if (o.sortKey[i] == null) {
				return 1;
			}
			else {
				if (sortKey[i] instanceof String || o.sortKey[i] instanceof String) {
					return String.valueOf(sortKey[i]).compareTo(String.valueOf(o.sortKey[i]));
				}
				else {
					return ((Comparable)sortKey[i]).compareTo(o.sortKey[i]);
				}
			}
		}

		@Override
		public void print(Context context) {
			for(String key: cells.keySet()) {
				context.cell(key, cells.get(key));
			}
		}
	}

	private static class ColumnExtractor implements SampleExtractor {

		private final String columnName;
		
		public ColumnExtractor(String columnName) {
			this.columnName = columnName;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Object extract(SampleReader sample) {
			return ((Map)sample.get(LINE_KEY)).get(columnName);
		}
	}
	
	public static class LevelSampleFilter implements SampleFilter, Serializable {

		private static final long serialVersionUID = 20121025L;
		
		private final Pattern filter;
		
		public LevelSampleFilter(Pattern filter) {
			this.filter = filter;
		}
		
		@Override
		public boolean match(SampleReader sample) {
			String level = (String) sample.get(PivotReporter.LEVEL_KEY);
			level = level == null ? "" : level;			
			return 
					   filter.matcher(level).matches()
					|| filter.matcher(level + ".").matches()
					|| filter.matcher("." + level).matches()
					|| filter.matcher("." + level + ".").matches();
		}		
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
			if (matches(level)) {
				return unit.display(reader);
			}
			else {
				return Collections.emptyMap();
			}
		}

		private boolean matches(String level) {
			return 
					   filter.matcher(level).matches()
					|| filter.matcher(level + ".").matches()
					|| filter.matcher("." + level).matches()
					|| filter.matcher("." + level + ".").matches();
		}
	}
}
