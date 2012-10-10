package org.gridkit.nimble.print;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class PrettyPrinter extends TablePrinter {
    @Override
    protected void print(PrintStream stream, List<List<Object>> table) {
        List<Integer> lens = columnLens(table);
        
        for (List<Object> row : table) {
            stream.print("| ");
            
            for (int c = 0; c < lens.size(); ++c) {
                Object value = row.get(c);
                
                String cell = c < row.size() ? toString(value) : "";
                
                boolean number = value instanceof Number;
                
                if (number) {
                    stream.printf("%" + lens.get(c) + "s | ", cell);
                }
                else {
                    stream.printf("%-" + lens.get(c) + "s | ", cell);
                }
            }
            
            stream.println();
        }
    }
    
    private List<Integer> columnLens(List<List<Object>> rows) {
        int maxSize = maxSize(rows);
        
        List<Integer> lens = new ArrayList<Integer>(maxSize);
        
        for (int c = 0; c < maxSize; ++c) {
            int len = 1;
            
            for (int r = 0; r < rows.size(); ++r) {
                List<Object> row = rows.get(r);
                
                if (c < row.size()) {
                    len = Math.max(len, toString(row.get(c)).length());
                }
            }
            
            lens.add(len);
        }
        
        return lens;
    }
    
    private static int maxSize(List<List<Object>> rows) {
        int max = Integer.MIN_VALUE;
        
        for (List<Object> list : rows) {
            max = Math.max(max, list.size());
        }
        
        return max;
    }
}
