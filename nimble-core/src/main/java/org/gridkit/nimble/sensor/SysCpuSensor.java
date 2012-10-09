package org.gridkit.nimble.sensor;

import java.util.concurrent.TimeUnit;

import org.gridkit.nimble.util.Pair;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.SigarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class SysCpuSensor extends IntervalMeasureSensor<IntervalMeasure<Cpu>, Pair<Long, Cpu>> {
    private static final Logger log = LoggerFactory.getLogger(SysCpuSensor.class);
    
    private static long MIN_MEASURE_INTERVAL_MS = 3;
    
    public SysCpuSensor(long measureInterval, TimeUnit unit) {
        super(Math.max(unit.toMillis(measureInterval), TimeUnit.SECONDS.toMillis(MIN_MEASURE_INTERVAL_MS)));
    }
    
    public SysCpuSensor(long measureIntervalS) {
        this(measureIntervalS, TimeUnit.SECONDS);
    }
    
    public SysCpuSensor() {
        this(MIN_MEASURE_INTERVAL_MS);
    }

    @Override
    protected Pair<Long, Cpu> getState() {
        try {
            return Pair.newPair(System.nanoTime(), getSigar().getCpu());
        } catch (SigarException e) {
            log.error("Failed to retrieve OS cpu", e);
            return null;
        }
    }

    @Override
    protected IntervalMeasure<Cpu> getMeasure(Pair<Long, Cpu> leftState, Pair<Long, Cpu> rightState) {
        if (leftState == null || rightState == null) {
            return null;
        }
        
        return new IntervalMeasure<Cpu>(leftState, rightState);
    }
}
