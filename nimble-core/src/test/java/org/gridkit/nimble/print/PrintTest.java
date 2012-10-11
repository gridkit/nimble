package org.gridkit.nimble.print;

import java.io.PrintStream;

import org.junit.Test;

public class PrintTest {
    @Test
    public void testPretty() {
        print(System.err, new PrettyPrinter());
    }
    
    @Test
    public void testCsv() {
        print(System.err, new CsvPrinter());
    }
    
    private void print(PrintStream stream, TablePrinter printer) {
        printer.print(stream, new LinePrinter() {
            @Override
            public void print(Context context) {
                context.cell("a", 1);
                context.cell("b", 1.0);
                context.newline();
                context.cell("c", "a");
            }
        });
    }
}
