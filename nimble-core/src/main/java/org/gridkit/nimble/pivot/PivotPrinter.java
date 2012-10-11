package org.gridkit.nimble.pivot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.DisplayFunction.CellPrinter;
import org.gridkit.nimble.pivot.Pivot.Level;
import org.gridkit.nimble.print.LinePrinter;

public class PivotPrinter implements LinePrinter {
	
	private final String idHeader;
	private final Pivot pivot;
	private final PivotReporter reporter;

	public PivotPrinter(Pivot pivot, PivotReporter reporter) {
		this(null, pivot, reporter);
	}

	public PivotPrinter(String idHeader, Pivot pivot, PivotReporter reporter) {
		this.idHeader = null;
		this.pivot = pivot;
		this.reporter = reporter;
	}
	
	@Override
	public void print(Context context) {
		context = new NormalRow("", context, true);
		for(RowPath path: reporter.listChildren(RowPath.root())) {
			printHLine("", new NormalRow("", context), path);
		}
		context.newline();
	}
	
	private void printHLine(String linePrefix, PrintContext context, RowPath path) {
		if (path.isLevel()) {
			printHLevel(linePrefix, context, path);
		}
		else {
			printHGroup(linePrefix, context, path);
		}		
	}
	
	private void printVLine(String linePrefix, PrintContext context, RowPath path) {
		if (path.isLevel()) {
			printVLevel(linePrefix, context, path);
		}
		else {
			printVGroup(linePrefix, context, path);
		}				
	}

	private void printHLevel(String linePrefix, PrintContext context, RowPath path) {
		int id = path.l();
		Pivot.Level level = pivot.getLevel(id);
		if (level.isPivoted()) {
			printVLevel(linePrefix, new PivotedRow("", context), path);
		}
		else {
			String pref = combine(linePrefix, level.getName());
			if (level.isVisible()) {
				context.newline();
				if (idHeader != null) {
					context.addCell(idHeader, pref);
				}
				RowSampleReader reader = createRowReader(path);
				for(DisplayFunction df: level.getAllDisplayFunction()) {
					df.getDisplayValue(context, reader);
				}
			}
			for(RowPath subpath: reporter.listChildren(path)) {
				printHLine(pref, context, subpath);
			}
		}
	}
	
	private void printHGroup(String linePrefix, PrintContext context, RowPath path) {
		int id = path.l();
		Pivot.Level level = pivot.getLevel(id);
		String pref = combine(linePrefix, String.valueOf(path.g()));
		if (level.isVisible()) {
			context.newline();
			if (idHeader != null) {
				context.addCell(idHeader, pref);
			}
			RowSampleReader reader = createRowReader(path);
			for(DisplayFunction df: level.getAllDisplayFunction()) {
				df.getDisplayValue(context, reader);
			}
		}
		for(RowPath subpath: reporter.listChildren(path)) {
			printHLine(pref, context, subpath);
		}
	}

	private void printVLevel(String linePrefix, PrintContext context,	RowPath path) {
		Pivot.Level level = pivot.getLevel(path.l());
		String name = level.getName();
		name = name == null ? "" : name;
		PrintContext ctx = new PivotedRow(name, context);
		
		if (level.isVisible()) {
			RowSampleReader reader = createRowReader(path);
			for(Object obj: level.getDisplayOrder()) {
				if (obj instanceof DisplayFunction) {
					DisplayFunction df = (DisplayFunction) obj;
					df.getDisplayValue(ctx, reader);
				}
				else {
					Pivot.Level sub = (Level) obj;
					for(RowPath subpath: reporter.listChildren(path)) {
						if (subpath.l() == sub.getId()) {
							printVLine(linePrefix, ctx, subpath);
						}
					}
				}
			}
		}
		else {
			for(RowPath subpath: reporter.listChildren(path)) {
				printVLine(linePrefix, ctx, subpath);
			}
		}
	}

	private void printVGroup(String linePrefix, PrintContext context,	RowPath path) {
		int id = path.l();
		Pivot.Level level = pivot.getLevel(id);
		String group = String.valueOf(path.g());
		PivotedRow ctx = new PivotedRow(group, context);
		if (level.isVisible()) {
			RowSampleReader reader = createRowReader(path);
			for(DisplayFunction df: level.getAllDisplayFunction()) {
				df.getDisplayValue(ctx, reader);
			}
		}
		for(RowPath subpath: reporter.listChildren(path)) {
			printVLine(linePrefix, ctx, subpath);
		}
	}
	
	private RowSampleReader createRowReader(RowPath path) {
		Map<Object, Object> row = new LinkedHashMap<Object, Object>();
		loadRow(path, row);
		return new RowSampleReader(row);
	}
	
	private void loadRow(RowPath path, Map<Object, Object> row) {
		if (path != null) {
			loadRow(path.parent(), row);
			Map<Object, Object> level = reporter.getRowData(path);
			if (level != null) {
				row.putAll(level);
			}
		}
	}

	private String combine(String l, String r) {
		if (notBlank(l) && notBlank(r)) {
			return l + "." + r;
		}
		else if (notBlank(l)){
			return l;
		}
		else {
			return r;
		}
	}

	private boolean notBlank(String l) {
		return l != null && l.length() > 0;
	}
	
	private interface PrintContext extends Context, CellPrinter {
		
	}
	
	private class NormalRow implements PrintContext {

		private boolean skipLine;
		private String cellPrefix;
		private Context context;

		public NormalRow(String cellPrefix, Context context) {
			this.cellPrefix = cellPrefix;
			this.context = context;
		}

		public NormalRow(String cellPrefix, Context context, boolean skipLine) {
			this.cellPrefix = cellPrefix;
			this.context = context;
			this.skipLine = true;
		}

		@Override
		public void cell(String name, Object object) {
			context.cell(cellPrefix + name, object);
		}
		
		@Override
		public void addCell(String caption, Object value) {
			cell(caption, value);			
		}

		@Override
		public void newline() {
			if (skipLine) {
				skipLine = false;
			}
			else {
				context.newline();
			}
		}
	}
	
	private class PivotedRow implements PrintContext {
		
		private String cellPrefix;
		private Context context;
		
		public PivotedRow(String cellPrefix, Context context) {
			this.cellPrefix = cellPrefix;
			this.context = context;
		}

		@Override
		public void addCell(String caption, Object value) {
			cell(caption, value);
		}

		@Override
		public void newline() {
		}

		@Override
		public void cell(String name, Object object) {
			context.cell(combine(cellPrefix, name), object);
		}
	}
	
	private static class RowSampleReader implements SampleReader {
		
		private final Map<Object, Object> row;
		
		public RowSampleReader(Map<Object, Object> row) {
			this.row = row;
		}

		@Override
		public boolean next() {
			return false;
		}

		@Override
		public List<Object> keySet() {
			return new ArrayList<Object>(row.keySet());
		}

		@Override
		public Object get(Object key) {
			return row.get(key);
		}
	}
}
