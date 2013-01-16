package org.gridkit.lab.monitoring.probe;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 *
 * @param <T> monitoring target
 * @param <S> sampler interface
 */
public interface SamplerProvider<T, S> {
	
	public S getSampler(T target);

}
