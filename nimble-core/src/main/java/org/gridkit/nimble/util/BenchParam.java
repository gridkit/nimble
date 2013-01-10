package org.gridkit.nimble.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchParam implements Cloneable {
    private static final Logger log = LoggerFactory.getLogger(BenchParam.class);
    
    private String name;
    private Object value;
    private String unit;
    
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Parameter {
        String name() default "";
        String unit() default "";
    }

    public static List<BenchParam> extract(Object config) {
        List<BenchParam> result = new ArrayList<BenchParam>();
        
        for (Field field : config.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            BenchParam param = new BenchParam();
            
            param.name = getName(field);
            param.value = getValue(field, config);
            param.unit = getUnit(field);

            result.add(param);
        }
        
        return result;
    }
    
    public static List<BenchParam> filter(List<BenchParam> params, Collection<String> ignores) {
        List<BenchParam> result = new ArrayList<BenchParam>();

        for (BenchParam param : params) {
            if (!ignores.contains(param.name)) {
                result.add(param.clone());
            }
        }
        
        return result;
    }
    
    public static List<BenchParam> prefix(List<BenchParam> params, String prefix) {
        List<BenchParam> result = new ArrayList<BenchParam>();

        for (BenchParam param : params) {
            BenchParam rParam = param.clone();
            rParam.name = prefix + "." + rParam.name;
            result.add(param);
        }
        
        return result;
    }
        
    private static Object getValue(Field field, Object config) {
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
    
    private static String getName(Field field) {
        String name = field.getName();
        
        Parameter property = field.getAnnotation(Parameter.class);
        
        if (property != null) {
            if (!isEmpty(property.name())) {
                name = property.name();
            }
        }

        return name;
    }
    
    private static String getUnit(Field field) {
        String unit = null;
        
        Parameter property = field.getAnnotation(Parameter.class);
        
        if (property != null) {
            if (!isEmpty(property.unit())) {
                unit = property.unit();
            }
        }

        return unit;
    }

    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }
    
    @Override
    public BenchParam clone() {
        try {
            return (BenchParam)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
