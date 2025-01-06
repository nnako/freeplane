/*
 * Created on 6 Jan 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map;

import java.awt.Component;
import java.awt.Insets;

import javax.swing.border.EmptyBorder;

public class ZoomedEmptyBorder extends EmptyBorder {
	@FunctionalInterface
	public interface IntToIntFunction {

	    /**
	     * Applies this function to the given argument.
	     *
	     * @param value the function argument
	     * @return the function result
	     */
	    int applyAsInt(long value);
	}

	private static final long serialVersionUID = 1L;
	private final IntToIntFunction zoomFunction;

	public ZoomedEmptyBorder(Insets borderInsets, IntToIntFunction zoomFunction) {
		super(borderInsets);
		this.zoomFunction = zoomFunction;
	}

	public ZoomedEmptyBorder(int top, int left, int bottom, int right, IntToIntFunction zoomFunction) {
		super(top, left, bottom, right);
		this.zoomFunction = zoomFunction;
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		 return zoom(super.getBorderInsets(c, insets));
	}

	private Insets zoom(Insets insets) {
		if(insets.left != 0)
			insets.left = zoomFunction.applyAsInt(insets.left);
		if(insets.right != 0)
			insets.right = zoomFunction.applyAsInt(insets.right);
		if(insets.top != 0)
			insets.top = zoomFunction.applyAsInt(insets.top);
		if(insets.bottom != 0)
			insets.bottom = zoomFunction.applyAsInt(insets.bottom);
		return insets;
	}

	@Override
	public Insets getBorderInsets() {
		 return zoom(super.getBorderInsets());
	}

}
