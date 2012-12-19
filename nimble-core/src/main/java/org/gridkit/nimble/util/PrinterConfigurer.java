package org.gridkit.nimble.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PivotPrinter2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface PrinterConfigurer {
    void configure(PivotPrinter2 printer);
    
    public static class Factory {
        public static PrinterConfigurer newComposite(PrinterConfigurer... configurers) {
            return new CompositeConfigurer(configurers);
        }
        
        public static PrinterConfigurer newReflection(Object config, String prefix, Collection<String> ignores) {
            return new ReflectionConfigurer(config, prefix, ignores);
        }
        
        public static PrinterConfigurer newReflection(Object config, String prefix) {
            return new ReflectionConfigurer(config, prefix, Collections.<String>emptySet());
        }
        
        public static PrinterConfigurer newReflection(Object config, Collection<String> ignores) {
            return new ReflectionConfigurer(config, "", ignores);
        }
        
        public static PrinterConfigurer newReflection(Object config) {
            return new ReflectionConfigurer(config, "", Collections.<String>emptySet());
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
    
    public static class ReflectionConfigurer implements PrinterConfigurer {
        private static final Logger log = LoggerFactory.getLogger(ReflectionConfigurer.class);
        
        private final Object config;
        private final String prefix;
        private final Collection<String> ignores;

        public ReflectionConfigurer(Object config, String prefix, Collection<String> ignores) {
            this.config = config;
            this.prefix = prefix;
            this.ignores = ignores;
        }

        @Retention(RetentionPolicy.RUNTIME)
        public static @interface Parameter {
            String name() default "";
            String unit() default "";
        }

        @Override
        public void configure(PivotPrinter2 printer) {
            for (Field field : config.getClass().getFields()) {
                if (Modifier.isStatic(field.getModifiers()) || ignores.contains(field.getName())) {
                    continue;
                }

                String name = getName(field);
                Object value = getValue(field);

                DisplayBuilder.with(printer).constant(name, value);
            }
        }
        
        private Object getValue(Field field) {
            Object value;
            
            try {
                field.setAccessible(true);
                value = field.get(config);
                if (value == null) {
                    value = "<null>";
                }
            } catch (Exception e) {
                value = "<error>";
                log.warn("Failed to read the value of field " + field, e);
            }
            
            return value;
        }
        
        private String getName(Field field) {
            String name = withPrefix(field.getName());
            
            Parameter property = field.getAnnotation(Parameter.class);
            
            if (property != null) {
                if (!isEmpty(property.name())) {
                    name = withPrefix(property.name());
                }
                
                if (!isEmpty(property.unit())) {
                    name = name + " [" + property.unit() + "]";
                }
            }

            return name;
        }
        
        private String withPrefix(String str) {
            return isEmpty(prefix) ? str : prefix + "." + str;
        }
        
        private static boolean isEmpty(String str) {
            return str == null || str.trim().isEmpty();
        }
    }
}
