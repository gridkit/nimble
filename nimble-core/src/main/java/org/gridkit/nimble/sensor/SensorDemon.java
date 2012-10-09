package org.gridkit.nimble.sensor;

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class SensorDemon<M> implements Callable<Void>, Serializable {
    private static final Logger log = LoggerFactory.getLogger(SensorDemon.class);
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static <M, N> SensorDemon<M> create(Sensor<M> sensor, Sensor.Reporter<N> reporter, long interval) {
    	return new SensorDemon(sensor, reporter, interval);
    }
    
    private Sensor<M> sensor;
    private Sensor.Reporter<M> reporter;
    private boolean ignoreFailures;
    private long delay;

    public SensorDemon(Sensor<M> sensor, Sensor.Reporter<M> reporter) {
    	this(sensor, reporter, 0);
    }
    
    public SensorDemon(Sensor<M> sensor, Sensor.Reporter<M> reporter, long delayMillis) {
        this.sensor = sensor;
        this.reporter = reporter;
        this.ignoreFailures = true;
        this.delay = delayMillis;
    }

    @Override
    public Void call() throws Exception {
        while (!Thread.interrupted()) {
            try {
                if (delay > 0) {
                	Thread.sleep(delay);
                }
                M m = sensor.measure();
                reporter.report(m);
            } catch (InterruptedException e) {
                return null;
            } catch (Throwable t) {
                log.error("Throwable while executing SensorDemon", t);
                
                if (!ignoreFailures) {
                    log.error("SensorDemon will be terminated");
                    return null;
                }
            }
        }
        
        return null;
    }
    
    @Override
    public String toString() {
    	return "SensorDemon:" + sensor;
    }
}
