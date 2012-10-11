package org.gridkit.nimble.pivot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gridkit.nimble.metering.SampleReader;
import org.gridkit.nimble.pivot.DisplayFunction.CellPrinter;
import org.gridkit.nimble.print.LinePrinter;

import com.sun.tools.jdi.LinkedHashMap;

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
	public void print(Contetx context) {
		for(RowPath path: reporter.listChildren(RowPath.root())) {
			printLine("", new HorizontalRow("", context), path);
		}
	}
	
	private void printLine(String linePrefix, PrintContext context, RowPath path) {
		if (path.isLevel()) {
			printHLevel(linePrefix, context, path);
		}
		else {
			printHGroup(linePrefix, context, path);
		}		
	}

	private void printHLevel(String linePrefix, PrintContext context, RowPath path) {
		int id = path.l();
		Pivot.Level level = pivot.getLevel(id);
		String pref = combine(linePrefix, level.getName());
		if (level.isVisible()) {
			if (idHeader != null) {
				context.addCell(idHeader, pref);
			}
			RowSampleReader reader = createRowReader(path);
			for(DisplayFunction df: level.getAllDisplayFunction()) {
				df.getDisplayValue(context, reader);
			}
			context.newline();
		}
		for(RowPath subpath: reporter.listChildren(path)) {
			printLine(pref, context, subpath);
		}
	}
	
	private void printHGroup(String linePrefix, PrintContext context, RowPath path) {
		int id = path.l();
		Pivot.Level level = pivot.getLevel(id);
		String pref = combine(linePrefix, String.valueOf(path.g()));
		if (level.isVisible()) {
			if (idHeader != null) {
				context.addCell(idHeader, pref);
			}
			RowSampleReader reader = createRowReader(path);
			for(DisplayFunction df: level.getAllDisplayFunction()) {
				df.getDisplayValue(context, reader);
			}
			context.newline();
		}
		for(RowPath subpath: reporter.listChildren(path)) {
			printLine(pref, context, subpath);
		}
	}

	private RowSampleReader createRowReader(RowPath path) {
		Map<Object, Object> row = new LinkedHashMap();
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
	
	private interface PrintContext extends Contetx, CellPrinter {
		
	}
	
	private class HorizontalRow implements PrintContext {

		private String cellPrefix;
		private Contetx context;

		public HorizontalRow(String cellPrefix, Contetx context) {
			this.cellPrefix = cellPrefix;
			this.context = context;
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
			context.newline();
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
