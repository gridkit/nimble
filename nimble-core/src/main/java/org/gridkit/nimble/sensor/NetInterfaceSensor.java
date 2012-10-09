package org.gridkit.nimble.sensor;

import static org.gridkit.nimble.util.StringOps.F;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.util.Pair;
import org.gridkit.nimble.util.SetOps;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.SigarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class NetInterfaceSensor extends IntervalMeasureSensor<List<NetInterfaceSensor.InterfaceMeasure>, Map<String, Pair<Long, NetInterfaceStat>>> {
    private static final Logger log = LoggerFactory.getLogger(NetInterfaceSensor.class);
    
    private static long MIN_MEASURE_INTERVAL_S = 3;

    private transient String[] interfaces;
    
    public static class InterfaceMeasure extends IntervalMeasure<NetInterfaceStat> {
        private String interfaceName;

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }
    }
    
    public NetInterfaceSensor(long measureInterval, TimeUnit unit) {
        super(Math.max(unit.toMillis(measureInterval), TimeUnit.SECONDS.toMillis(MIN_MEASURE_INTERVAL_S)));
    }
    
    public NetInterfaceSensor(long measureIntervalS) {
        this(measureIntervalS, TimeUnit.SECONDS);
    }
    
    public NetInterfaceSensor() {
        this(MIN_MEASURE_INTERVAL_S);
    }

    @Override
    protected Map<String, Pair<Long, NetInterfaceStat>> getState() {
        Map<String, Pair<Long, NetInterfaceStat>> result = new HashMap<String, Pair<Long,NetInterfaceStat>>();
        
        for (String inter : getInterfaces()) {
            try {
                result.put(inter, Pair.newPair(System.nanoTime(), getSigar().getNetInterfaceStat(inter)));
            } catch (SigarException e) {
                log.warn(F("Failed to retrieve interface stats for '%s'", inter), e);
            }
        }
        
        return result;
    }

    @Override
    protected List<InterfaceMeasure> getMeasure(Map<String, Pair<Long, NetInterfaceStat>> leftState, Map<String, Pair<Long, NetInterfaceStat>> rightState) {
        List<InterfaceMeasure> result = new ArrayList<InterfaceMeasure>();
        
        for (String inter : SetOps.<String>intersection(leftState.keySet(), rightState.keySet())) {
            InterfaceMeasure measure = new InterfaceMeasure();
            
            Pair<Long, NetInterfaceStat> left = leftState.get(inter);
            Pair<Long, NetInterfaceStat> right = rightState.get(inter);
            
            measure.setLeftTsNs(left.getA());
            measure.setLeftState(left.getB());
            
            measure.setRightTsNs(right.getA());
            measure.setRightState(right.getB());
            
            measure.setInterfaceName(inter);

            result.add(measure);
        }
        
        return result;
    }

    public String[] getInterfaces() {
        if (interfaces == null || interfaces.length == 0) {
            try {
                interfaces = getSigar().getNetInterfaceList();
            } catch (SigarException e) {
                log.warn("Failed to retrieve interfaces list", e);
                interfaces = new String[] {};
            }
        }
        
        return interfaces;
    }
}
