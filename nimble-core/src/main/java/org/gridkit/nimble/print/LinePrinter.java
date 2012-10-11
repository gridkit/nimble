package org.gridkit.nimble.print;

public interface LinePrinter {
    void print(Context context);
    
    public interface Context {
        void newline();
        
        void cell(String name, Object object);
    }
}
