package org.gridkit.nimble.npivot;

import java.io.PrintStream;

public interface TablePrinter {
    void print(Table table, PrintStream writer);
}
