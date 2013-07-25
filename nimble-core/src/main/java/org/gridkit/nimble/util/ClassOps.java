package org.gridkit.nimble.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClassOps {
    public static Class<?>[] getInterfaces(Class<?> clazz) {
        Set<Class<?>> interfaces = new HashSet<Class<?>>();
        getInterfaces(clazz, interfaces);
        return interfaces.toArray(new Class<?>[interfaces.size()]);
    }

    private static void getInterfaces(Class<?> clazz, Set<Class<?>> result) {
        if (Object.class.equals(clazz)) {
            return;
        } else {
            result.addAll(Arrays.asList(clazz.getInterfaces()));
            getInterfaces(clazz.getSuperclass(), result);
        }
    }
}
