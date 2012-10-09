package org.gridkit.nimble.util;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.Collection;
import java.util.Map;

public class ValidOps {
    public static void notNull(Object obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(shouldNotBe(name, "null"));
        }
    }
    
    public static void notEmpty(Collection<?> coll, String name) {
        notNull(coll, name);
        
        if (coll.isEmpty()) {
            throw new IllegalArgumentException(shouldNotBe(name, "empty"));
        }
    }
    
    public static void notEmpty(String str, String name) {
        notNull(str, name);
        
        if (str.trim().isEmpty()) {
            throw new IllegalArgumentException(shouldNotBe(name, "empty"));
        }
    }
    
    public static void notEmpty(Map<?, ?> map, String name) {
        notNull(map, name);
        
        if (map.isEmpty()) {
            throw new IllegalArgumentException(shouldNotBe(name, "empty"));
        }
    }
    
    public static void positive(Integer num, String name) {
        notNull(num, name);
        
        if (num <= 0) {
            throw new IllegalArgumentException(shouldBe(name, "positive")); 
        }
    }

    public static void positive(Double num, String name) {
    	notNull(num, name);
    	
    	if (num <= 0) {
    		throw new IllegalArgumentException(shouldBe(name, "positive")); 
    	}
    }
    
    public static void positive(Long num, String name) {
        notNull(num, name);
        
        if (num <= 0) {
            throw new IllegalArgumentException(shouldBe(name, "positive")); 
        }
    }
    
    private static String shouldBe(String subject, String feature) {
        return F("'%s' should be %s", subject, feature);
    }
    
    private static String shouldNotBe(String subject, String feature) {
        return F("'%s' should not be %s", subject, feature);
    }
}
