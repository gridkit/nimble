package org.gridkit.nimble.platform;

import java.rmi.Remote;
import java.util.Collection;

public interface PushTopic<M> {
    
    public void subscribe(Subscriber<M> subscriber);
    
    public void publish(Collection<M> msgs);
    
    public void sync() throws InterruptedException;

    public static interface Subscriber<M> extends Remote {
    	public void push(Collection<M> msgs);
    }    
}
