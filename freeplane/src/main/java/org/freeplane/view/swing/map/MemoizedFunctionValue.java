/*
 * Created on 7 Jan 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map;

import javax.swing.JComponent;

public class MemoizedFunctionValue<U, V> {
	@FunctionalInterface
	public interface Supplier<T, E extends Exception> {
	    T get() throws E;
	}
	private U argument;
	private V value;

	private MemoizedFunctionValue(U input, V output) {
		this.argument = input;
		this.value = output;
	}

	public static <U, V, E extends Exception> V memoize(JComponent component, U argument, Supplier<V, E> valueSupplier) throws E {
		if(component == null)
			return valueSupplier.get();
		@SuppressWarnings("unchecked")
		MemoizedFunctionValue<U, V> cache = (MemoizedFunctionValue<U, V>) component.getClientProperty(MemoizedFunctionValue.class);
		if(cache == null) {
			V output = valueSupplier.get();
			cache = new MemoizedFunctionValue<U, V>(argument, output);
			component.putClientProperty(MemoizedFunctionValue.class, cache);
		}
		else if(! cache.argument.equals(argument)) {
			cache.value = valueSupplier.get();
			cache.argument = argument;
		}
		return cache.value;
	}

}
