package org.freeplane.view.swing.map;

import java.awt.Dimension;

import javax.swing.JComponent;

class ContentSizeCalculator {
	static final int UNSET = -1;
	public static final Dimension ZERO = new Dimension(0, 0);
	public static ContentSizeCalculator INSTANCE = new ContentSizeCalculator();
	Dimension calculateContentSize(final NodeView view) {
		if(! view.isContentVisible())
			return ZERO;
		final JComponent content = view.getContent();
		Dimension contentSize=  content.getPreferredSize();
		return contentSize;
	}

	Dimension calculateContentSize(final NodeView view, int minimumWidth) {
		Dimension contentSize = calculateContentSize(view);
        if(contentSize.width > 0 && contentSize.height > 0 && minimumWidth != UNSET) {
        	final int maximumWidth = view.getMainView().getMaximumWidth();
        	if(contentSize.width < minimumWidth && contentSize.width < maximumWidth)
        		contentSize.width = Math.min(minimumWidth, maximumWidth);
        }
        return contentSize;
    }
	Dimension calculateContentSize(NodeViewLayoutHelper accessor) {
		return accessor.calculateContentSize();
	}

}
