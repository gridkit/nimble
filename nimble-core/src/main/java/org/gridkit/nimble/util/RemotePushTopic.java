package org.gridkit.nimble.util;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.gridkit.nimble.platform.PushTopic;

@SuppressWarnings("serial")
public class RemotePushTopic<M> implements PushTopic<M>, Serializable {
    
	private RemoteTopic<M> hub;
    private transient Thread pushThread;
    private transient BlockingQueue<M> publishQueue;
	
    public RemotePushTopic() {
    	this(16);
    }
    
    public RemotePushTopic(int dispatchThreads) {
    	hub = new Hub<M>(dispatchThreads);
    }
    
    @Override
    public void subscribe(final Subscriber<M> subscriber) {
        hub.subscribe(new RemoteSubscriber<M>() {			
			@Override
			public void push(Collection<M> msgs) {
				subscriber.push(msgs);				
			}
		});
    }

    @Override
    public synchronized void publish(Collection<M> msgs) {
    	if (pushThread == null) {
    		publishQueue = new LinkedBlockingQueue<M>();
    		Thread t = new Thread(new Runnable() {				
				@Override
				public void run() {
					pushToHub();					
				}
			});
    		t.setDaemon(true);
    		t.setName("PushTopic.Publisher-" + t.getName());
    		t.start();
    		pushThread = t;
    	}
    	List<M> outgoing = new ArrayList<M>(msgs);
    	while(!outgoing.isEmpty()) {
    		move(outgoing, publishQueue);
    	}
    }
    
    @Override
	public void sync() throws InterruptedException {
    	if (hub instanceof Hub) {
    		hub.sync();
    	}
    	else if (publishQueue != null) {
	 		while(publishQueue.size() > 0) {
				Thread.sleep(100);
			}
    	}
	}

	private void pushToHub() {
    	List<M> buffer = new ArrayList<M>(2 << 10);
    	try {
			while(true) {
				M first = publishQueue.take();
				buffer.add(first);
				publishQueue.drainTo(buffer, (2 << 10) - 1);
				try {
					hub.publish(buffer);
				}
				catch(Exception e) {
					// ignore
				}
				buffer.clear();
			}
		} catch (InterruptedException e) {			
		}
    	synchronized (this) {
			pushThread = null;
			publishQueue = null;
		}
    }
    
    private static <M> void move(List<M> incoming, BlockingQueue<M> queue) {
		Iterator<M> it = incoming.iterator();
		while(it.hasNext()) {
			if (queue.offer(it.next())) {
				it.remove();
			}
			else {
				break;
			}
		}			
	}

	private static class Hub<M> implements RemoteTopic<M> {
    	
    	private List<SubscriberCtx<M>> subscribers = new CopyOnWriteArrayList<SubscriberCtx<M>>();
    	private BlockingQueue<M> buffer = new ArrayBlockingQueue<M>(128); 
    	private BlockingQueue<SubscriberCtx<M>> subscriberPool = new LinkedBlockingQueue<SubscriberCtx<M>>();
    	private List<Thread> threads = new ArrayList<Thread>();
    	
    	public Hub(int dispatchThreads) {
			for(int i = 0; i != dispatchThreads; ++i) {
				Thread t = new Thread() {
					@Override
					public void run() {
						pushToSubscriber();
					}
				};
				t.setName("RemotePushTopic-FanOut-" + i);
				t.setDaemon(true);
				t.start();
				threads.add(t);
			}
		}
    	
    	private void pushToSubscriber() {
			try {
				List<M> buffer = new ArrayList<M>();
				int n = 0;
				while(true) {
					++n;
					if (n >= subscribers.size()) {
						Thread.sleep(50);
						n = 0;
					}
					SubscriberCtx<M> ctx = subscriberPool.take();
					ctx.queue.drainTo(buffer);
					if (!buffer.isEmpty()) {
						try {
							ctx.subscriber.push(buffer);
							System.out.println("Push " + buffer.size() + " to " + ctx.subscriber);
						}
						catch(UndeclaredThrowableException e) {
							// ignore							
						}
						buffer.clear();
						n = 0; // delay sleep
					}
					subscriberPool.add(ctx);
				}
			} catch (InterruptedException e) {
				// terminating silently
			}    			
    	}
    	
        @Override
        public void subscribe(Subscriber<M> subscriber) {
        	SubscriberCtx<M> ctx = new SubscriberCtx<M>();
        	ctx.subscriber = subscriber;
            subscribers.add(ctx);
            subscriberPool.add(ctx);
        }

        @Override
        public void publish(Collection<M> msgs) {
        	List<M> incoming = new ArrayList<M>(msgs);
        	while(!incoming.isEmpty()) {
        		move(incoming, buffer);
	        	synchronized (this) {
	        		List<M> data = new ArrayList<M>();	        		
	        		buffer.drainTo(data);
	        		if (!data.isEmpty()) {	        		
		        		List<SubscriberCtx<M>> shuffledSubscribers = new ArrayList<SubscriberCtx<M>>(subscribers);
		        		
		        		Collections.shuffle(shuffledSubscribers);
		        		
		        		for (SubscriberCtx<M> subscriber : shuffledSubscribers) {
	        				move(new ArrayList<M>(data), subscriber.queue);
		        		}
	        		}
				}
        	}
        }

		@Override
		public void sync() throws InterruptedException {
			while(!isEmpty()) {
				Thread.sleep(100);
			}
		}    	
		
		private boolean isEmpty() {
			if (!buffer.isEmpty()) {
				return false;
			}
			else {
				for(SubscriberCtx<M> ctx: subscribers) {
					if (!ctx.queue.isEmpty()) {
						return false;
					}
				}
				return true;
			}			
		}
		
		static class SubscriberCtx<M> {
			
			Subscriber<M> subscriber;
			BlockingQueue<M> queue = new ArrayBlockingQueue<M>(256);
			
		}
    }
    
    public interface RemoteTopic<M> extends PushTopic<M>, Remote {    	
    }
    
    public interface RemoteSubscriber<M> extends PushTopic.Subscriber<M>, Remote {    	
    }
}
