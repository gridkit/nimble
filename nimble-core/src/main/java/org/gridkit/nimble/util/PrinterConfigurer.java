package org.gridkit.nimble.util;

import java.util.Arrays;
import java.util.List;

import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PivotPrinter2;

public interface PrinterConfigurer {
    void configure(PivotPrinter2 printer);
    
    public static class Factory {
        public static PrinterConfigurer newComposite(PrinterConfigurer... configurers) {
            return new CompositeConfigurer(configurers);
        }
        
        public static PrinterConfigurer newParametric(List<BenchParam> params) {
            return new ParametricConfigurer(params);
        }
    }
    
    public static class CompositeConfigurer implements PrinterConfigurer {
        private List<PrinterConfigurer> configurers;
        
        public CompositeConfigurer(PrinterConfigurer... configurers) {
            this.configurers = Arrays.asList(configurers);
        }

        @Override
        public void configure(PivotPrinter2 printer) {
            for (PrinterConfigurer configurer : configurers) {
                configurer.configure(printer);
            }
        }
    }
    
    public static class ParametricConfigurer implements PrinterConfigurer {
        private final List<BenchParam> params;

        public ParametricConfigurer(List<BenchParam> params) {
            this.params = params;
        }

        @Override
        public void configure(PivotPrinter2 printer) {
            for (BenchParam param : params) {
                String name = param.getName();
                String unit = param.getUnit();

                if (unit != null) {
                    name = name + " [" + unit + "]";
                }
                
                DisplayBuilder.with(printer).constant(name, param.getValue());
            }
        }
    }
}
