package org.gridkit.nimble.util;

import java.io.PrintStream;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ConfigurationTemplate implements Serializable, Cloneable {
    private static final long serialVersionUID = -3251922539739961467L;
    
    // TODO add value format information
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Property {
        String name() default "";
        String unit() default "";
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Ignore {}
    
    @SuppressWarnings("unchecked")
    public <T extends ConfigurationTemplate> T copy() {
        try {
            return (T) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error("impossible");
        }
    }
    
    public void print(PrintStream stream) {
        stream.println("-- " + this.getClass().getSimpleName() + " --");
        
        for (Field field : this.getClass().getFields()) {
            if (field.getAnnotation(Ignore.class) != null || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            
            String name = field.getName();
            String unit = null;
            
            Object value;
            try {
                value = field.get(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            Property property = field.getAnnotation(Property.class);
            
            if (property != null) {
                if (!isEmpty(property.name())) {
                    name = property.name();
                }
                
                if (!isEmpty(property.unit())) {
                    unit = property.unit();
                }
            }
            
            print(stream, name, value, unit);
        }
    }
    
    private void print(PrintStream stream, String name, Object value, String unit) {
        stream.print(name);
        
        stream.print(" = ");
        
        if (value != null) {
            stream.print(value.toString());
        } else {
            stream.print("null");
        }
        
        if (!isEmpty(unit)) {
            stream.print(" (" + unit + ")");
        }
        
        stream.println();
    }
    
    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
