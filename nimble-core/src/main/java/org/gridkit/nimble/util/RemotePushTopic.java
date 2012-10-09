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
    
	private RemoteTopic<M> hub = new Hub<M>();
    private transient Thread pushThread;
    private transient BlockingQueue<M> publishQueue;
	
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
    	
    	private List<Subscriber<M>> subscribers = new CopyOnWriteArrayList<Subscriber<M>>();
    	private BlockingQueue<M> buffer = new ArrayBlockingQueue<M>(256); 
    	
        @Override
        public void subscribe(Subscriber<M> subscriber) {
            subscribers.add(subscriber);
        }

        @Override
        public void publish(Collection<M> msgs) {
        	try {
	        	List<M> incoming = new ArrayList<M>(msgs);
	        	while(!incoming.isEmpty()) {
	        		move(incoming, buffer);
		        	synchronized (this) {
		        		List<M> data = new ArrayList<M>();	        		
		        		buffer.drainTo(data);
		        		if (!data.isEmpty()) {	        		
			        		List<Subscriber<M>> shuffledSubscribers = new ArrayList<Subscriber<M>>(subscribers);
			        		
			        		Collections.shuffle(shuffledSubscribers);
			        		
			        		for (Subscriber<M> subscriber : shuffledSubscribers) {
			        			try {
			        				subscriber.push(msgs);
			        			}
			        			catch(UndeclaredThrowableException e) {
			        				// ignore
			        				subscribers.remove(subscriber);
			        			}
			        		}
			        		System.err.println("Published " + data.size() + " messages to " + shuffledSubscribers.size() + " subscribers");
		        		}
					}
	        	}
        	}
        	catch(Exception e) {
        		// TODO
        		// ignore
        	}
        }

		@Override
		public void sync() throws InterruptedException {
			while(!buffer.isEmpty()) {
				Thread.sleep(100);
			}
		}    	
    }
    
    public interface RemoteTopic<M> extends PushTopic<M>, Remote {    	
    }
    
    public interface RemoteSubscriber<M> extends PushTopic.Subscriber<M>, Remote {    	
    }
}
