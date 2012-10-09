package org.gridkit.nimble.platform.remote;

import static org.gridkit.nimble.util.StringOps.F;

import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.Remote;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.gridkit.nimble.platform.LocalAgent;
import org.gridkit.nimble.platform.RemoteAgent;
import org.gridkit.nimble.platform.SystemTimeService;
import org.gridkit.nimble.platform.TimeService;
import org.gridkit.nimble.util.NamedThreadFactory;
import org.gridkit.vicluster.ViNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

//TODO handle remote agents shutdown situations
public class ViNodeAgent implements RemoteAgent {
	private ViNode node;
	private Set<String> labels;
	private LocalAgentHandle localAgent;
	
	public ViNodeAgent(ViNode node, final Set<String> labels) {
		this.node = node;
		this.labels = new HashSet<String>(labels);
		
		this.localAgent = node.exec(new Callable<LocalAgentHandle>() {
			@Override
			public LocalAgentHandle call() throws Exception {
				return new ViLocalAgent(labels);
			}
		});
	}

	@Override
	public Set<String> getLabels() {
		return Collections.unmodifiableSet(labels);
	}

	@Override
	public void shutdown(boolean hard) {
		node.shutdown();
	}

	@Override
	public <T> ListenableFuture<T> invoke(final Invocable<T> invocable) {	    
	    RemoteResultPromise<T> remoteResult = new RemoteResultPromise<T>();
	    final RemoteResult<T> remoteResultHandle = remoteResult;
	    
	    final LocalAgentHandle localAgent = this.localAgent;

	    Future<RemoteExecutionHandle> execHandle = node.submit(new Callable<RemoteExecutionHandle>() {
            @Override
            public RemoteExecutionHandle call() throws Exception {
               return localAgent.invoke(invocable, remoteResultHandle);
            }
	    });
	    
	    remoteResult.getResultFuture().setExecHandle(execHandle);
	    
	    return remoteResult.getResultFuture();
	}

	private static interface RemoteResult<T> extends Remote {
	    void set(T result);
	    void setException(Throwable throwable);
	}
	
    private static interface RemoteExecutionHandle extends Remote {     
        void cancel(Boolean hard);
    }
    
    private static interface LocalAgentHandle extends Remote, LocalAgent {
        <T> RemoteExecutionHandle invoke(Invocable<T> invocable, RemoteResult<T> resut);
    }
	    
    private static class StaticRemoteExecutionHandle implements RemoteExecutionHandle {
        private Future<?> resultFuture;
        
        public StaticRemoteExecutionHandle(Future<?> resultFuture) {
            this.resultFuture = resultFuture;
        }

        @Override
        public void cancel(Boolean hard) {
            resultFuture.cancel(hard);
        }
    }
    
	private static class RemoteResultPromise<T> implements RemoteResult<T> {
	    private RemoteResultFuture<T> resultFuture = new RemoteResultFuture<T>();
	    
        @Override
        public void set(T result) {
            resultFuture.set(result);
        }
        
        @Override
        public void setException(Throwable throwable) {
            resultFuture.setException(throwable);
        }
        
        public RemoteResultFuture<T> getResultFuture() {
            return resultFuture;
        }
	}
	
	private static class RemoteResultFuture<T> extends AbstractFuture<T> {
	    private static final Logger log = LoggerFactory.getLogger(RemoteResultFuture.class);
	    
	    private Future<RemoteExecutionHandle> execHandle;
	    
        public boolean cancel(boolean hard) {
            if (isDone()) {
                return false;
            }
            
            if (execHandle != null) {
            	try {
            		execHandle.get().cancel(hard);
            	} catch(UndeclaredThrowableException e) {
            		// ignore
            	} catch (InterruptedException e) {
            	    // ignore
                } catch (ExecutionException e) {
                    log.error("Error while waiting for RemoteExecutionHandle", e);
                }
            } else {
                throw new IllegalStateException();
            }
            
            return super.cancel(hard);
        };
        
        public boolean set(T value) {
            return super.set(value);
        };
        
        public boolean setException(Throwable throwable) {
            return super.setException(throwable);
        };

	    public void setExecHandle(Future<RemoteExecutionHandle> execHandle) {
            this.execHandle = execHandle;
        }
	}

	private static class ViLocalAgent implements LocalAgentHandle {
	    private Set<String> labels;
		private ConcurrentMap<String, Object> attrs;
		
		public ViLocalAgent(Set<String> labels) {		    
			this.labels = new HashSet<String>(labels);
			this.attrs = new ConcurrentHashMap<String, Object>();
		}

        @Override
        public <T> RemoteExecutionHandle invoke(final Invocable<T> invocable, final RemoteResult<T> resut) {
            String threadGroup = F("%s[%s]", ViLocalAgent.class.getSimpleName(), invocable);
            
            ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory(threadGroup));
            
            final Future<?> resultFuture = executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        resut.set(invocable.invoke(ViLocalAgent.this));
                    } catch (Throwable t) {
                        resut.setException(t);
                    }
                }
            });

            return new StaticRemoteExecutionHandle(resultFuture);
        }
		
		@Override
		public Set<String> getLabels() {
		    return labels;
		}
		
		
		@Override
		public Logger getLogger(String name) {
			return LoggerFactory.getLogger(name);
		}

        @Override
        public ConcurrentMap<String, Object> getAttrsMap() {
            return attrs;
        }

        @Override
        public TimeService getTimeService() {
            return SystemTimeService.getInstance();
        }
	}
	
    public String toString() {
        return F("ViNodeAgent[%s]", node.toString());
    }
}
