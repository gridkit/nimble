package org.gridkit.nimble.probe;

import java.util.Collection;

public interface PidProvider extends org.gridkit.nimble.sensor.PidProvider {
    /**
     * @return Collection of matched processes IDs
     */
    Collection<Long> getPids();
}
