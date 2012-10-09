package org.gridkit.nimble.print;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gridkit.nimble.util.Pair;

// TODO add sorting priority to columns
public abstract class TablePrinter {
    private DecimalFormat decFormat = new DecimalFormat("0.00");
    private Object nullValue = "";
    private boolean printHead = true;
    
    protected abstract void print(PrintStream stream, List<List<Object>> table);
    
    public void print(PrintStream stream, LinePrinter linePrinter) {
        TablePrinterContetx context = new TablePrinterContetx();

        linePrinter.print(context);
        
        Pair<Set<String>, List<Map<String,Object>>> data = context.finish();
        
        List<List<Object>> table = new ArrayList<List<Object>>();
        
        if (printHead) {
            table.add(new ArrayList<Object>(data.getA()));
        }
        
        for (Map<String,Object> mapRow : data.getB()) {
            List<Object> listRow = new ArrayList<Object>();
            
            for (String column : data.getA()) {
                listRow.add(mapRow.get(column));
            }
            
            table.add(listRow);
        }
        
        print(stream, table);
    }
    
    private class TablePrinterContetx implements LinePrinter.Contetx {
        private Set<String> columns = newColumnsSet();
        private List<Map<String,Object>> data = new ArrayList<Map<String,Object>>();
        
        private Map<String,Object> curLine = new HashMap<String, Object>();
        private Set<String> curColumns = newColumnsSet();

        @Override
        public void newline() {
            data.add(curLine);
            columns.addAll(curColumns);
            
            curLine = new HashMap<String, Object>();
            curColumns = newColumnsSet();
        }
        
        @Override
        public void cell(String name, Object object) {
            curColumns.add(name);
            curLine.put(name, object);
        }
        
        public Pair<Set<String>, List<Map<String,Object>>> finish() {
            if (!curColumns.isEmpty()) {
                newline();
            }
            
            return Pair.newPair(columns, data);
        }
    }
    
    private Set<String> newColumnsSet() {
        return new LinkedHashSet<String>(); 
    }
    
    protected String toString(Object value) {
        if (value == null) {
            return toString(nullValue);
        } else if (value instanceof Double || value instanceof Float) {
            return decFormat.format(value);
        } else {
            return value.toString();
        }
    }

    public DecimalFormat getDecFormat() {
        return decFormat;
    }

    public void setDecFormat(DecimalFormat decFormat) {
        this.decFormat = decFormat;
    }

    public Object getNullValue() {
        return nullValue;
    }

    public void setNullValue(Object nullValue) {
        this.nullValue = nullValue;
    }

    public boolean isPrintHead() {
        return printHead;
    }

    public void setPrintHead(boolean printHead) {
        this.printHead = printHead;
    }
}
