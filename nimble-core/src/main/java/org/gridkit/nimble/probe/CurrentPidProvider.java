package org.gridkit.nimble.probe;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;


public class CurrentPidProvider implements PidProvider {
    @Override
    public Collection<Long> getPids() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        
        int dogIndex = name.indexOf('@');
        
        if (dogIndex != -1) {
            try {
                Long pid = Long.valueOf(name.substring(0, dogIndex));
                return Collections.singleton(pid);
            } catch (NumberFormatException e) {
                return Collections.emptySet();
            }
        } else {
            return Collections.emptySet();
        }
    }
}
