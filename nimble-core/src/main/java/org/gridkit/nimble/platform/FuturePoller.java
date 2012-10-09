package org.gridkit.nimble.platform;

import java.util.concurrent.Future;

import com.google.common.util.concurrent.ListenableFuture;

public interface FuturePoller {
    <T> ListenableFuture<T> poll(Future<T> future);
    
    void shutdown();
}
