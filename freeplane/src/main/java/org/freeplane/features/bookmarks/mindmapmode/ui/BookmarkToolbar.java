package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.SwingConstants;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.ui.components.ToolbarLayout;
import org.freeplane.core.ui.components.UITools;

public class BookmarkToolbar extends FreeplaneToolBar {
	private static final int GAP = (int) (10 * UITools.FONT_SCALE_FACTOR);
	private static final long serialVersionUID = 1L;

	enum DropIndicatorType {
		NONE,
		DROP_BEFORE,
		DROP_AFTER,
		HOVER_FEEDBACK,
		NAVIGATE_FEEDBACK
	}

	private Component targetComponent;
	private DropIndicatorType indicatorType = DropIndicatorType.NONE;

	public BookmarkToolbar() {
		super(SwingConstants.VERTICAL);
    	ToolbarLayout layout = (ToolbarLayout) getLayout();
    	layout.setGap(GAP, true);
	}

	public BookmarkToolbar(String name, int orientation) {
		super(name, orientation);
	}

	public void showVisualFeedback(Component button, DropIndicatorType type) {
		this.targetComponent = button;
		this.indicatorType = type;
		repaint();
	}

	public void clearVisualFeedback() {
		showVisualFeedback(null, DropIndicatorType.NONE);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (targetComponent != null && indicatorType != DropIndicatorType.NONE) {
			g.setColor(targetComponent.getForeground());
			paintVisualFeedback(g);
		}
	}

	private void paintVisualFeedback(Graphics g) {
			Rectangle buttonBounds = targetComponent.getBounds();

			switch (indicatorType) {
				case DROP_BEFORE:
					paintDropLine(g, buttonBounds, true);
					break;
				case DROP_AFTER:
					paintDropLine(g, buttonBounds, false);
					break;
				case HOVER_FEEDBACK:
				case NAVIGATE_FEEDBACK:
					paintHoverFeedback(g, buttonBounds);
					break;
				default:
					break;
			}
	}

	private void paintDropLine(Graphics g, Rectangle buttonBounds, boolean before) {
		paintDropLine(g, buttonBounds, GAP, before);
	}

	private void paintDropLine(Graphics g, Rectangle buttonBounds, final int lineWidth,
	        boolean before) {
		int x = before ? buttonBounds.x - lineWidth : buttonBounds.x + buttonBounds.width;
		int y1 = buttonBounds.y + 2;
		int y2 = buttonBounds.y + buttonBounds.height - 2;
		g.fillRect(x , y1, lineWidth, y2 - y1);
	}

	private void paintHoverFeedback(Graphics g, Rectangle buttonBounds) {
		paintDropLine(g, buttonBounds, GAP/2, false);
		paintDropLine(g, buttonBounds, GAP/2, true);
	}
}