package org.gridkit.nimble.pivot;

import java.util.Map;

import org.gridkit.nimble.print.LinePrinter;
import org.gridkit.nimble.print.PrettyPrinter;

public class PivotDumper implements LinePrinter {

	public static void dump(PivotReporter reporter) {
		new PrettyPrinter().print(System.out, new PivotDumper(reporter));
	}
	
	
	private PivotReporter reporter;


	public PivotDumper(PivotReporter reporter) {
		this.reporter = reporter;
	}


	@Override
	public void print(Context context) {
		print(LevelPath.root(), context);
	}

	private void print(LevelPath path, Context context) {
		Map<Object, Object> row = reporter.getRowData(path);
		if (row != null) {
			context.cell("_row_", row);
			for(Object key: row.keySet()) {
				context.cell(String.valueOf(key), row.get(key));
			}
			context.newline();
		}
		for(LevelPath spath: reporter.listChildren(path)) {
			print(spath, context);
		}		
	}
}
