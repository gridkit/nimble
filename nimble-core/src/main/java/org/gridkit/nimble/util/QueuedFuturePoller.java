package org.gridkit.nimble.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.gridkit.nimble.platform.FuturePoller;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

@SuppressWarnings({"rawtypes", "unchecked"})
public class QueuedFuturePoller implements FuturePoller {
    private final BlockingQueue<PollFuture> futures;
    
    private final ExecutorService executor;

    public QueuedFuturePoller(int nThreads) {
        this.futures = new LinkedBlockingQueue<PollFuture>();
        this.executor = Executors.newFixedThreadPool(nThreads);
        
        while (nThreads-- > 0) {
            this.executor.submit(new PollWorker());
        }
    }

    @Override
    public <T> ListenableFuture<T> poll(Future<T> future) {
        PollFuture result = new PollFuture(future);
        futures.add(result);
        return result;
    }
    
    private class PollWorker implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    PollFuture pollFuture = futures.take();
                    Future future = pollFuture.getFuture();
                                        
                    if (future.isDone()) {
                        if (future.isCancelled()) {
                            pollFuture.cancel(true);
                        } else {
                            try {
                                pollFuture.set(future.get());
                            } catch (ExecutionException e) {
                                pollFuture.setException(e.getCause());
                            }
                        }
                    } else {
                        futures.put(pollFuture);
                        Thread.sleep(50);
                    }
                }
            }
            catch (InterruptedException onShutdown) {}
        }
    }

    private static class PollFuture<T> extends AbstractFuture<T> {
        private final Future<T> future;

        public PollFuture(Future<T> future) {
            this.future = future;
        }

        @Override
        public boolean set(T value) {
            return super.set(value);
        };
        
        @Override
        public boolean setException(Throwable throwable) {
            return super.setException(throwable);
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (isDone()) {
                return false;
            }
            
            future.cancel(mayInterruptIfRunning);
            
            return super.cancel(false);
        }
        
        public Future<T> getFuture() {
            return future;
        }
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }
}
