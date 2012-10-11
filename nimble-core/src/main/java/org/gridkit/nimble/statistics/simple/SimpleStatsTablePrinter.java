package org.gridkit.nimble.statistics.simple;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nimble.print.LinePrinter;
import org.gridkit.nimble.print.LinePrinter.Context;
import org.gridkit.nimble.print.TablePrinter;

public class SimpleStatsTablePrinter { 
    public interface SimpleStatsLinePrinter {
        public void print(SimpleStats stats, LinePrinter.Context context);
    }
        
    private List<SimpleStatsLinePrinter> statsPrinters = new ArrayList<SimpleStatsLinePrinter>();
    
    private Map<String, Object> leftConsts = new HashMap<String, Object>();
    private Map<String, Object> rightConsts = new HashMap<String, Object>();

    public void print(PrintStream stream, TablePrinter tablePrinter, SimpleStats stats) {
        tablePrinter.print(stream, new InternalLinePrinter(stats));
    }
    
    private class InternalLinePrinter implements LinePrinter {
        private SimpleStats stats;

        public InternalLinePrinter(SimpleStats stats) {
            this.stats = stats;
        }

        @Override
        public void print(Context context) {
            InternalLinePrinterContext internalContetx = new InternalLinePrinterContext(context);
            
            for (SimpleStatsLinePrinter statsPrinter : statsPrinters) {
                statsPrinter.print(stats, internalContetx);
            }
        }
    }
    
    private class InternalLinePrinterContext implements LinePrinter.Context {
        private LinePrinter.Context context;
        private boolean firstPrint = true;
        
        public InternalLinePrinterContext(Context context) {
            this.context = context;
        }

        @Override
        public void newline() {
            if (firstPrint) {
                printConsts(leftConsts);
            }
            
            printConsts(rightConsts);
            
            context.newline();
            
            firstPrint = true;
        }

        @Override
        public void cell(String name, Object object) {
            if (firstPrint) {
                printConsts(leftConsts);
                firstPrint = false;
            }
            context.cell(name, object);
        }
        
        private void printConsts(Map<String, Object> consts) {
            for (Map.Entry<String, Object> entry : consts.entrySet()) {
                context.cell(entry.getKey(), entry.getValue());
            }
        }
    }
    
    public Map<String, Object> getLeftConsts() {
        return leftConsts;
    }
    
    public void setLeftConsts(Map<String, Object> leftConsts) {
        this.leftConsts = leftConsts;
    }
    
    public Map<String, Object> getRightConsts() {
        return rightConsts;
    }
    
    public void setRightConsts(Map<String, Object> rightConsts) {
        this.rightConsts = rightConsts;
    }

    public List<SimpleStatsLinePrinter> getStatsPrinters() {
        return statsPrinters;
    }

    public void setStatsPrinters(List<SimpleStatsLinePrinter> statsPrinters) {
        this.statsPrinters = statsPrinters;
    }
}
