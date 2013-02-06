package org.gridkit.nimble.npivot;

//import org.gridkit.nimble.npivot.PivotTable.MeasureSet;

//@SuppressWarnings("null")
public class Example {
    /*
    public static enum Key {
        HOST_NAME, NODE_TYPE, PID, THREAD_NAME, SYS_CPU, USR_CPU, TIMESTAMP, EXPERIMENT_TIME, OPERATION, DURATION
    }

    public static void main(String[] args) {
        System.out.println((Double)(Double.MAX_VALUE / (double)(60*60*60*60)));
    }

    public static void simple() {
        Cube cube = null;

        Aggregate data = cube.query(new QueryBuilder()
            .groups(Key.EXPERIMENT_TIME, Key.NODE_TYPE, Key.OPERATION)
            .measures(Measures.mean(Key.DURATION))
            .build()
        );
        
        Table table = new PivotTable()
            .onRows(Key.EXPERIMENT_TIME)
            .onColumns(Key.NODE_TYPE, Key.OPERATION)
            .measure(Measures.mean(Key.DURATION))
            .draw(data.getSampleSet().newSampleCursor());

        TablePrinter printer = null;
        
        printer.print(table, System.out);
    }
    
    public static class CoherenceThreadType implements Function {
        private final Function threadName;
        
        public CoherenceThreadType(Object threadName) {
            this.threadName = Functions.bind(threadName);
        }

        @Override
        public Object apply(Sample sample) {
            String threadName = (String)this.threadName.apply(sample);
            
            if (threadName.equals("PacketSpeaker")) {
                return "PacketSpeaker";
            } else { // and so on 
                return null;
            }
        }
    }
    
    public static void complex() {
        Cube cube = null;
        
        Function cohThreadType = new CoherenceThreadType(Key.THREAD_NAME);
        
        Aggregate data = cube.query(new QueryBuilder()
            .groups(Key.HOST_NAME, Key.NODE_TYPE, cohThreadType, Key.PID)
            .measures(
                Measures.max(Key.SYS_CPU),   Measures.min(Key.USR_CPU),
                Measures.max(Key.USR_CPU),   Measures.min(Key.USR_CPU),
                Measures.max(Key.TIMESTAMP), Measures.min(Key.TIMESTAMP)                
             )
            .build()
        );
        
        Function usrRate = Functions.rate(Key.USR_CPU, Key.TIMESTAMP);
        Function sysRate = Functions.rate(Key.SYS_CPU, Key.TIMESTAMP);
        
        MeasureSet pivotMeasures = MeasureSet.of(
            Measures.mean(usrRate), Measures.mean(sysRate)
        );
        
        SampleSuite samples = data.getSampleSet()
            .transform(Transformers.apply(usrRate, sysRate))
            .aggregate(new QueryBuilder()
                .groups(Key.HOST_NAME, Key.NODE_TYPE, cohThreadType)
                .measures(pivotMeasures)
                .build()
            );
        
        Table table = new PivotTable()
            .onRows(Key.HOST_NAME, Key.NODE_TYPE)
            .onColumns(cohThreadType, pivotMeasures)
            .draw(samples.newSampleCursor());
        
        TablePrinter printer = null;
        
        printer.print(table, System.out);
    }
    */
}
