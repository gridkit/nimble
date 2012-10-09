package org.gridkit.nimble.print;

public interface LinePrinter {
    void print(Contetx context);
    
    public interface Contetx {
        void newline();
        
        void cell(String name, Object object);
    }
}
