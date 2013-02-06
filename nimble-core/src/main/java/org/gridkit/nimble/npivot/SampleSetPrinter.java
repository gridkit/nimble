package org.gridkit.nimble.npivot;

import org.gridkit.nimble.print.LinePrinter;

public class SampleSetPrinter implements LinePrinter {
    private final SampleSet samples;
    
    public SampleSetPrinter(Aggregate aggr) {
        this(aggr.samples());
    }
    
    public SampleSetPrinter(SampleSet samples) {
        this.samples = samples;
    }

    @Override
    public void print(Context context) {
        SampleCursor cursor = samples.newSampleCursor();
        
        while (cursor.isFound()) {
            for (Object key : cursor.keys()) {
                context.cell(key.toString(), cursor.get(key));
            }
            
            if (cursor.next()) {
                context.newline();
            }
        }
    }
}
