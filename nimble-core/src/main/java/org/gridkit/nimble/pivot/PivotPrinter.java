package org.gridkit.nimble.pivot;

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
	public void print(Contetx context) {
		for(RowPath path: reporter.listChildren(RowPath.root())) {
			printLine("", context, path);
		}
	}
	
	private void printLine(String linePrefix, Contetx context, RowPath path) {
		if (path.isLevel()) {
			printHLevel(linePrefix, context, path);
		}
		else {
			printHGroup(linePrefix, context, path);
		}		
	}

	private void printHLevel(String linePrefix, Contetx context, RowPath path) {
		int id = path.getLevelId();
		Pivot.Level level = pivot.getLevel(id);
		String pref = combine(linePrefix, level.getName());
//		for(Level)
	}
	
	private void printHGroup(String linePrefix, Contetx context, RowPath path) {
		// TODO Auto-generated method stub
		
	}

	private String combine(String l, String r) {
		if (l.length() > 0 && r.length() > 0) {
			return l + "." + r;
		}
		else {
			return null;
		}
	}
	
	private class HorizontalRow implements Contetx {

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
		public void newline() {
			context.newline();
		}
	}
}
