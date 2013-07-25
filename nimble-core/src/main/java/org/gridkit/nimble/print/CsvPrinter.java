package org.gridkit.nimble.print;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

public class CsvPrinter extends TablePrinter {
    private char separator = ',';
    
    @Override
    protected void print(PrintStream stream, List<List<Object>> table) {
        for (List<Object> row : table) {
            Iterator<Object> iter = row.iterator();
            
            while (iter.hasNext()) {
                stream.print(toString(iter.next()));
                
                if (iter.hasNext()) {
                    stream.print(separator);
                }
            }
            
            stream.println();
        }
    }

    public char getSeparator() {
        return separator;
    }

    public void setSeparator(char separator) {
        this.separator = separator;
    }
}
