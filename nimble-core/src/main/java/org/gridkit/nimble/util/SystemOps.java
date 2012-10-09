package org.gridkit.nimble.util;

import java.lang.management.ManagementFactory;

public class SystemOps {
    public static final int UNKNOWN_PID = -1;
    
    public static int getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        
        int dogIndex = name.indexOf('@');
        
        if (dogIndex != -1) {
            try {
                return Integer.valueOf(name.substring(0, dogIndex));
            } catch (NumberFormatException e) {
                return UNKNOWN_PID;
            }
        } else {
            return UNKNOWN_PID;
        }
    }
}
