package org.gridkit.nimble.probe.jmx;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.gridkit.lab.util.jmx.mxstruct.common.ExtendedThreadMXBean;

public class MXBeanFactory {    
    public static ExtendedThreadMXBean newThreadMXBean(MBeanServerConnection conn) {
        return newMXBean(conn, ManagementFactory.THREAD_MXBEAN_NAME, ExtendedThreadMXBean.class);
    }
    
    public static ClassLoadingMXBean newClassLoadingMXBean(MBeanServerConnection conn)  {
        return newMXBean(conn, ManagementFactory.CLASS_LOADING_MXBEAN_NAME, ClassLoadingMXBean.class);
    }
    
    public static <T> T newMXBean(MBeanServerConnection conn, String name, Class<T> clazz) {
        ObjectName objectName = null;
        
        try {
            objectName = ObjectName.getInstance(name);
        } catch (MalformedObjectNameException ignore) {
        }
        
        return JMX.newMXBeanProxy(conn, objectName, clazz);
    }
}
