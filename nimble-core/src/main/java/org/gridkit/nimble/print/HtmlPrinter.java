package org.gridkit.nimble.print;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class HtmlPrinter extends TablePrinter {
    private String id;
    private String clazz;
    private String caption;
    
    @Override
    protected void print(PrintStream stream, List<List<Object>> table) {
        stream.println(open("table", "id", id, "class", clazz));
        
        if (caption != null) {
            stream.print(open("caption"));
            stream.print(caption);
            stream.println(close("caption"));
        }
        
        List<Object> header = Collections.emptyList();
        List<List<Object>> rows = Collections.emptyList();
        
        if (!table.isEmpty()) {
            header = table.get(0);
            rows = table.subList(1, table.size());
        }
        
        if (rows.isEmpty()) {
            rows = new ArrayList<List<Object>>();
            rows.add(Arrays.<Object>asList("empty table"));
        }
        
        stream.println(open("thead"));
        printRow(stream, header, "th");
        stream.println(close("thead"));
        
        stream.println(open("tbody"));
        for (List<Object> row : rows) {
            printRow(stream, row, "td");
        }
        stream.println(close("tbody"));
        
        stream.println(close("table"));
    }

    private void printRow(PrintStream stream, List<Object> row, String cellTag) {
        stream.println(open("tr"));
        for (Object cell : row) {
            stream.print(open(cellTag));
            stream.print(toString(cell));
            stream.println(close(cellTag));
        }
        stream.println(close("tr"));
    }
    
    private static String open(String tag, String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("args.length % 2 != 0");
        }
        
        StringBuilder sb = new StringBuilder();
        
        sb.append('<');
        sb.append(tag);

        Iterator<String> iter = Arrays.asList(args).iterator();
        
        while (iter.hasNext()) {
            String attr = iter.next();
            String value = iter.next();
            
            if (value != null) {
                sb.append(' ');
                sb.append(attr);
                sb.append("=\"");
                sb.append(value);
                sb.append("\"");
            }
        }
        
        sb.append('>');
        
        return sb.toString();
    }
    
    private static String close(String tag) {
        return "</" + tag + ">";
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public void setClass(String clazz) {
        this.clazz = clazz;
    }
    
    public void setCaption(String caption) {
        this.caption = caption;
    }
}
