package org.gridkit.nimble.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

import org.gridkit.nimble.pivot.display.DisplayBuilder;
import org.gridkit.nimble.pivot.display.PrintConfig;

@Deprecated // use org.gridkit.nimble.util.PrinterConfigurer
public class ConfigurationTemplate implements Serializable, Cloneable {
    private static final long serialVersionUID = -3251922539739961467L;
    
    private transient Collection<ParameterInfo> parameters;
    
    public ConfigurationTemplate() {
        initParameters();
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Parameter {
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

    public void print(PrintConfig printer) {
        for (ParameterInfo param : parameters) { 
            DisplayBuilder.with(printer).constant(
                param.getNameWithUnit(), param.getValue()
            );
        }
    }
    
    public void print(PrintStream stream) {
        stream.println("-- " + this.getClass().getSimpleName() + " --");
        
        for (ParameterInfo param : parameters) {
            stream.print(param.getName());
            stream.print(" = ");
            stream.println(param.getValueWithUnit());
        }
    }

    private class ParameterInfo {
        Field field;
        String name;
        String unit;
        
        public String getName() {
            return name;
        }
        
        public Object getValue() {
            try {
                return field.get(ConfigurationTemplate.this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        public String getStringValue() {
            Object value = getValue();
            
            if (value != null) {
                return value.toString();
            } else {
                return "null";
            }
        }
        
        public String getValueWithUnit() {
            return withUnit(getStringValue());
        }
        
        public String getNameWithUnit() {
            return withUnit(getName());
        }
        
        private String withUnit(String str) {
            if (isEmpty(unit)) {
                return str;
            } else {
                return str + " [" + unit + "]";
            }
        }
    }
    
    private void initParameters() {
        parameters = new ArrayList<ParameterInfo>();
        
        for (Field field : this.getClass().getFields()) {
            if (field.getAnnotation(Ignore.class) != null || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            
            ParameterInfo param = new ParameterInfo();
            
            param.field = field;
            param.name = field.getName();
            
            Parameter property = field.getAnnotation(Parameter.class);
            
            if (property != null) {
                if (!isEmpty(property.name())) {
                    param.name = property.name();
                }
                
                if (!isEmpty(property.unit())) {
                    param.unit = property.unit();
                }
            }

            parameters.add(param);
        }
    }
        
    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        initParameters();
    }
}
