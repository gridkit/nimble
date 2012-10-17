package org.gridkit.nimble.util;

import java.rmi.Remote;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public interface RemoteMap<K, V> extends Remote {

	public V get(K key);
	
	public V put(K key, V value);

	public Map<K, V> getAll();

	public Map<K, V> getAll(Collection<K> keys);
	
	public void putAll(Map<K, V> data);
	
	public int size();
	
	public static class Wrapper<K, V> implements RemoteMap<K, V> {
		
		public static <K, V> Wrapper<K, V> wrap(Map<K, V> map) {
			return new Wrapper<K, V>(map);
		}
		
		private final Map<K, V> backingMap;

		public Wrapper(Map<K, V> backingMap) {
			this.backingMap = backingMap;
		}

		@Override
		public V get(K key) {
			return backingMap.get(key);
		}

		@Override
		public V put(K key, V value) {
			return backingMap.put(key, value);
		}

		@Override
		public Map<K, V> getAll(Collection<K> keys) {
			Map<K, V> copy = new LinkedHashMap<K, V>();
			for(K k: keys) {
				if (backingMap.containsKey(k)) {
					copy.put(k, backingMap.get(k));
				}
			}
			return copy;
		}

		@Override
		public Map<K, V> getAll() {
			Map<K, V> copy = new LinkedHashMap<K, V>(backingMap); 
			return copy;
		}

		@Override
		public void putAll(Map<K, V> data) {
			backingMap.putAll(data);			
		}

		@Override
		public int size() {
			return backingMap.size();
		}
	}	
}
