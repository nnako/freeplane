package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JList;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.ToolBarUI;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.ui.components.ToolbarLayout;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.ui.BookmarkIndexCalculator.ToolbarDropPosition.Type;
import org.freeplane.features.map.MapModel;

public class BookmarkToolbar extends FreeplaneToolBar {
	private static final LayoutFocusTraversalPolicy FOCUS_TRAVERSAL_POLICY = new LayoutFocusTraversalPolicy();
	static {
		FOCUS_TRAVERSAL_POLICY.setImplicitDownCycleTraversal(false);
	}
	static final int GAP = (int) (10 * UITools.FONT_SCALE_FACTOR);
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("serial")
	private static final Border TOOLBAR_BORDER = new BookmarkToolbarBorder();


	private static class BookmarkToolbarBorder extends EtchedBorder {
		private static final long serialVersionUID = 1L;
		static final private Color focusColor = new JList<>().getSelectionBackground();
		@Override
		public Color getHighlightColor(Component c) {
			return c.hasFocus() ? focusColor : super.getHighlightColor(c);
		}
	}

	enum DropIndicatorType {
		NONE,
		DROP_BEFORE,
		DROP_AFTER,
		HOVER_FEEDBACK,
		NAVIGATE_FEEDBACK,
		END_DROP_INDICATOR
	}

	private final BookmarkIndexCalculator indexCalculator;
	private Component targetComponent;
	private DropIndicatorType indicatorType = DropIndicatorType.NONE;
	private final BookmarkClipboardHandler clipboardHandler;
	private final DropExecutor dropExecutor;
	private MapModel map;

	public BookmarkToolbar(BookmarksController bookmarksController, MapModel map) {
		super(FreeplaneToolBar.FLOATING_HORIZONTAL);
		this.map = map;
    	ToolbarLayout layout = (ToolbarLayout) getLayout();
    	layout.setGap(GAP, true, false);
    	setDisablesFocus(false);
    	setFocusable(true);
    	setFocusCycleRoot(true);
    	final LayoutFocusTraversalPolicy policy = FOCUS_TRAVERSAL_POLICY;
		setFocusTraversalPolicy(policy);
    	addFocusMouseListener();
    	addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				repaint();
			}

			@Override
			public void focusGained(FocusEvent e) {
				repaint();
			}
		});
    	this.indexCalculator = new BookmarkIndexCalculator(this);
    	this.dropExecutor = new DropExecutor(this, bookmarksController);
    	this.clipboardHandler = new BookmarkClipboardHandler(bookmarksController, dropExecutor);

    	new DropTarget(this,
				DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE,
				new BookmarkDropTargetListener(this, bookmarksController));

		clipboardHandler.setupToolbarClipboardActions(this);
	}



	public MapModel getMap() {
		return map;
	}



	public void setMap(MapModel map) {
		this.map = map;
	}

	public BookmarkClipboardHandler getClipboardHandler() {
		return clipboardHandler;
	}

	private void addFocusMouseListener() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					handleFocusOnClick(e.getPoint());
				}
			}
		});
	}

	private void handleFocusOnClick(Point clickPoint) {
		if (getComponentCount() == 0) {
			requestFocusInWindow();
		} else if (indexCalculator != null) {
			BookmarkIndexCalculator.ToolbarDropPosition position = indexCalculator.calculateToolbarDropPosition(clickPoint);
			Component componentToFocus = getComponentToFocus(position);
			if (componentToFocus != null) {
				componentToFocus.requestFocusInWindow();
			} else {
				requestFocusInWindow();
			}
		}
	}

	private Component getComponentToFocus(BookmarkIndexCalculator.ToolbarDropPosition position) {
		if (position.buttonIndex >= 0 && (position.type == Type.BEFORE_BUTTON && position.buttonIndex < getComponentCount()
				|| position.type == Type.AFTER_BUTTON && position.buttonIndex < getComponentCount() - 1)) {
			return getComponent(position.buttonIndex);
		}
		else
			return this;
	}

	@Override
	public void setUI(ToolBarUI ui) {
		super.setUI(ui);
		setBorder(TOOLBAR_BORDER);
		final Insets borderInsets = TOOLBAR_BORDER.getBorderInsets(this);
		setMinimumSize(new Dimension(2 * GAP + borderInsets.left + borderInsets.right, 2 * GAP + borderInsets.top + borderInsets.bottom));
	}

	public void showVisualFeedback(Component button, DropIndicatorType type) {
		this.targetComponent = button;
		this.indicatorType = type;
		repaint();
	}

	public void clearVisualFeedback() {
		showVisualFeedback(null, DropIndicatorType.NONE);
	}

	public void showEndDropIndicator() {
		showVisualFeedback(this, DropIndicatorType.END_DROP_INDICATOR);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (targetComponent != null && indicatorType != DropIndicatorType.NONE) {
			g.setColor(targetComponent.getForeground());
			paintVisualFeedback(g);
		}
	}



	@Override
	protected void paintBorder(Graphics g) {
		super.paintBorder(g);
		if(hasFocus()) {
			g.setColor(Color.BLUE);
			g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		}
	}

	private void paintVisualFeedback(Graphics g) {
		if (indicatorType == DropIndicatorType.END_DROP_INDICATOR) {
			paintEndDropLine(g);
		} else {
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

	private void paintEndDropLine(Graphics g) {
		if(getComponentCount() == 0) {
			final Insets insets = getInsets();
			final Rectangle bounds = new Rectangle(getWidth() - insets.right - GAP,
					getHeight() - insets.bottom,
					GAP, getHeight() - insets.bottom - insets.top);
			paintDropLine(g, bounds, GAP, true);
		}
		else {
			final Rectangle bounds = getComponent(getComponentCount() - 1).getBounds();
			paintDropLine(g, bounds, GAP, false);
		}
	}

	public void requestInitialFocusInWindow() {
		for(int i = 0; i < getComponentCount(); i++) {
			final Component component = getComponent(i);
			if(component.isEnabled() && component.isFocusable())
				component.requestFocusInWindow();
			return;
		}
		requestFocusInWindow();
	}
}