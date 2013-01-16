package org.gridkit.nimble.probe;

import java.util.Collection;

public interface PidProvider {
    /**
     * @return Collection of matched processes IDs
     */
    Collection<Long> getPids();
}
