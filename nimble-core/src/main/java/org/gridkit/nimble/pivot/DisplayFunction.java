package org.gridkit.nimble.pivot;

import org.gridkit.nimble.metering.SampleReader;

public interface DisplayFunction {

	public void getDisplayValue(CellPrinter printer, SampleReader level);
	
	public interface CellPrinter {
		
		public void addCell(String caption, Object value);
		
	}
	
}
