/*
 q*  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.view.swing.map;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.plaf.ScrollPaneUI;

import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.ActionAcceleratorManager;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.ui.ViewController;
import org.freeplane.view.swing.features.filepreview.ScalableComponent;

/**
 * @author Dimitry Polivaev
 * 10.01.2009
 */
public class MapViewScrollPane extends JScrollPane implements IFreeplanePropertyListener {
	private static final Dimension INVISIBLE = new Dimension(0,  0);
	public static final Rectangle EMPTY_RECTANGLE = new Rectangle();

	@FunctionalInterface
	public interface ViewportReservedAreaSupplier {
		Rectangle getReservedArea();
	}

	@SuppressWarnings("serial")
    static class MapViewPort extends JViewport{
		private List<ViewportReservedAreaSupplier> reservedAreaSuppliers = new ArrayList<>();
		private boolean layoutInProgress = false;

		void addReservedAreaSupplier(ViewportReservedAreaSupplier supplier) {
			removeReservedAreaSupplier(supplier);
			this.reservedAreaSuppliers.add(supplier);
		}
		void removeReservedAreaSupplier(ViewportReservedAreaSupplier supplier) {
			this.reservedAreaSuppliers.remove(supplier);
		}


		@Override
        public void doLayout() {
	        layoutInProgress = true;
	        super.doLayout();
	        layoutInProgress = false;
		}



		@Override
		protected void validateTree() {
		    super.validateTree();
		    final Component view = getView();
		    if(view != null) {
                ((MapView) view).scrollViewAfterLayout();
                if(! isValid())
                    super.validateTree();
            }
		}



        private Timer timer;

		private ScalableComponent backgroundComponent;
		private boolean scrollsRectangleToVisible;
		private Point targetViewPosition;
		private int scrollingDelay = 0;

        public void setBackgroundComponent(ScalableComponent backgroundComponent) {
            this.backgroundComponent = backgroundComponent;
        }



        @Override
        public void scrollRectToVisible(final Rectangle contentRectangle) {
            int width = getWidth();
			int height = getHeight();
        	boolean isTooWide = contentRectangle.width >= width;
			boolean isTooHigh = contentRectangle.height >= height;
			final int targetDX, targetDY;
			if(targetViewPosition != null) {
				final Component view = getView();
				targetDX = -targetViewPosition.x - view.getX();
				targetDY = -targetViewPosition.y - view.getY();
				contentRectangle.x += targetDX;
				contentRectangle.y += targetDY;
			}
			else
				targetDX=targetDY=0;
			if(isTooWide && isTooHigh) {
        		scrollRectToVisibleWithoutAdjustment(contentRectangle);
        	}
			else {
                // Start with a copy of the original rectangle.
                Rectangle candidateRect = new Rectangle(contentRectangle);
    			int dx = positionAdjustment(getWidth(), contentRectangle.width, contentRectangle.x);
    			int dy = positionAdjustment(getHeight(), contentRectangle.height, contentRectangle.y);

                // Compute the maximum insets from all reserved area suppliers.
                // These insets represent the area reserved (i.e. the overlays) on each side.
                int leftInset = 0, rightInset = 0, topInset = 0, bottomInset = 0;
    			for(ViewportReservedAreaSupplier supplier : reservedAreaSuppliers) {
                    Rectangle reservedArea = supplier.getReservedArea();
                    if(reservedArea.width == 0 || reservedArea.height == 0)
                        continue;
                    // Translate reservedArea to viewport coordinates.
                    Rectangle r = new Rectangle(reservedArea);
                    r.x -= getX();
                    if(r.x < 0) {
                        r.width += r.x; // reduce width by the negative offset
                        r.x = 0;
                    }
                    r.y -= getY();
                    if(r.y < 0) {
                        r.height += r.y;
                        r.y = 0;
                    }
                    if(r.x + r.width > width)
                        r.width = width - r.x;
                    if(r.y + r.height > height)
                        r.height = height - r.y;

                    // Determine which side the reserved area is drawn on.
                    boolean isOnTheLeft = r.x == 0;
                    boolean isOnTheRight = r.x + r.width == width;
                    boolean isAtTheTop = r.y == 0;
                    boolean isAtTheBottom = r.y + r.height == height;
                    if(! (isOnTheLeft || isOnTheRight || isAtTheTop || isAtTheBottom))
                    	continue;
        			final boolean overlapsOnXAxis = contentRectangle.x + dx < r.x + r.width
        					&& contentRectangle.x + dx + contentRectangle.width > r.x;
        			final boolean overlapsOnYAxis = contentRectangle.y + dy < r.y + r.height
        				&& contentRectangle.y + dy + contentRectangle.height > r.y;

                    if(! (overlapsOnXAxis && overlapsOnYAxis))
                    	continue;

    				if(isOnTheLeft && ! isOnTheRight && ! isTooWide && (isAtTheTop == isAtTheBottom || r.width < r.height || isTooHigh)) {
                        leftInset = Math.max(leftInset, r.width);
                    }
    				else if(isOnTheRight && ! isOnTheLeft && ! isTooWide && (isAtTheTop == isAtTheBottom || r.width < r.height || isTooHigh)) {
                        rightInset = Math.max(rightInset, r.width);
                    }
    				else if(isAtTheTop && ! isAtTheBottom && ! isTooHigh && (isOnTheRight == isOnTheLeft || r.height <= r.width || isTooWide)) {
                        topInset = Math.max(topInset, r.height);
                    }
    				else if(isAtTheBottom && ! isAtTheTop && ! isTooHigh && (isOnTheRight == isOnTheLeft || r.height <= r.width || isTooWide)) {
                        bottomInset = Math.max(bottomInset, r.height);
                    }
                }

                // --- Horizontal adjustment ---
                boolean adjustLeft = leftInset > 0 && contentRectangle.x < leftInset;
                boolean adjustRight = rightInset > 0 &&
                        (contentRectangle.x + contentRectangle.width) > (width - rightInset);

                if(adjustLeft && !adjustRight && leftInset >= rightInset) {
                    candidateRect.x = contentRectangle.x - leftInset;
                    candidateRect.width = contentRectangle.width + leftInset;
                }
                else if(adjustRight && !adjustLeft && rightInset >= leftInset) {
                    candidateRect.width = contentRectangle.width + rightInset;
                }
                else if(adjustLeft && adjustRight) {
                    candidateRect.x = contentRectangle.x - leftInset;
                    candidateRect.width = contentRectangle.width + leftInset + rightInset;
                }

                // --- Vertical adjustment ---
                boolean adjustTop = topInset > 0 && contentRectangle.y < topInset;
                boolean adjustBottom = bottomInset > 0 &&
                        (contentRectangle.y + contentRectangle.height) > (height - bottomInset);

                if(adjustTop && !adjustBottom && topInset >= bottomInset) {
                    candidateRect.y = contentRectangle.y - topInset;
                    candidateRect.height = contentRectangle.height + topInset;
                }
                else if(adjustBottom && !adjustTop && bottomInset >= topInset) {
                    candidateRect.height = contentRectangle.height + bottomInset;
                }
                else if(adjustTop && adjustBottom) {
                    candidateRect.y = contentRectangle.y - topInset;
                    candidateRect.height = contentRectangle.height + topInset + bottomInset;
                }

                scrollRectToVisibleWithoutAdjustment(candidateRect);
    		}
			contentRectangle.x -= targetDX;
			contentRectangle.y -= targetDY;
        }

		private void scrollRectToVisibleWithoutAdjustment(final Rectangle contentRectangle) {
			boolean scrollsRectangleToVisible = this.scrollsRectangleToVisible;
			this.scrollsRectangleToVisible = true;
			try {
				super.scrollRectToVisible(contentRectangle);
			}
			finally {
				this.scrollsRectangleToVisible =scrollsRectangleToVisible;
			}
		}



        @Override
		public Point getViewPosition() {
        	if(targetViewPosition != null && scrollsRectangleToVisible)
        		return targetViewPosition;
        	else
        		return super.getViewPosition();
		}

		private int positionAdjustment(int parentWidth, int childWidth, int childAt)    {

            //   +-----+
            //   | --- |     No Change
            //   +-----+
            if (childAt >= 0 && childWidth + childAt <= parentWidth)    {
                return 0;
            }

            //   +-----+          +-----+
            //   |   ----    ->   | ----|
            //   +-----+          +-----+
            if (childAt > 0 && childWidth <= parentWidth)    {
                return -childAt + parentWidth - childWidth;
            }

            //   +-----+          +-----+
            // ----    |     ->   |---- |
            //   +-----+          +-----+
            if (childAt <= 0 && childWidth <= parentWidth)   {
                return -childAt;
            }


            return 0;
        }
        @Override
        public void paintComponent(Graphics g) {
            if(backgroundComponent != null) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                backgroundComponent.paintComponent(g);
            }
        }

        @Override
        public boolean isOpaque() {
            return backgroundComponent != null && getBackground().getAlpha() == 255;
        }

        @Override
		public void setViewPosition(Point p) {
			if(! layoutInProgress) {
				if(scrollingDelay != 0){
					if(targetViewPosition == null)
						SwingUtilities.invokeLater(this::slowSetViewPosition);
					targetViewPosition = p;
				} else {
					stopTimer();
					layoutInProgress = true;
					super.setViewPosition(p);
					layoutInProgress = false;
					MapView view = (MapView)getView();
					if (view != null) {
						view.setAnchorContentLocation();
					}
				}
			}
        }

    	@Override
    	public void setBounds(int x, int y, int width, int height) {
    		boolean layoutWasInProgress = layoutInProgress;
     		layoutInProgress = true;
    		try {
    			int dX = (width - getWidth())/2;
    			int dY = (height - getHeight())/2;
    			if(dX != 0 || dY != 0) {
    				Point viewPosition = getViewPosition();
    				viewPosition.x += dX;
    				viewPosition.y += dY;
    				super.setViewPosition(viewPosition);
    		}
    			super.setBounds(x, y, width, height);
    		}
    		finally {
    			layoutInProgress = layoutWasInProgress;
    		}
    	}

		@Override
        public void setViewSize(Dimension newSize) {
			Component view = getView();
	        if (view != null) {
	            Dimension oldSize = view.getSize();
	            if (newSize.equals(oldSize)) {
	            	view.setSize(newSize);
	            } else {
					super.setViewSize(newSize);
				}
	        }
        }

		private void slowSetViewPosition() {
			stopTimer();
			final Point currentViewPosition = super.getViewPosition();
			if(targetViewPosition == null)
				return;
			if(isValid()) {
				int dx = targetViewPosition.x - currentViewPosition.x;
				int dy = targetViewPosition.y - currentViewPosition.y;
				int slowDx = calcScrollIncrement(dx);
				int slowDy = calcScrollIncrement(dy);
				currentViewPosition.translate(slowDx, slowDy);
				boolean layoutWasInProgress = layoutInProgress;
				layoutInProgress = true;
				try {
					super.setViewPosition(currentViewPosition);
				}
				finally {
					layoutInProgress = layoutWasInProgress;
				}
				if(slowDx == dx && slowDy == dy) {
					targetViewPosition = null;
					scrollingDelay = 0;
					MapView view = (MapView)getView();
					if (view != null) {
						view.setAnchorContentLocation();
					}
					return;
				}
			}
	        timer = new Timer(scrollingDelay, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					timer = null;
					MapViewPort.this.slowSetViewPosition();
				}
			});
	        timer.setRepeats(false);
	        timer.start();
        }

		private void stopTimer() {
			if(timer != null) {
				timer.stop();
				timer = null;
			}
		}

		private int calcScrollIncrement(int dx) {
			if(scrollingDelay == 0)
				return dx;
			int v = ResourceController.getResourceController().getIntProperty("scrolling_speed");
			final int absDx = Math.abs(dx);
			final double sqrtDx = Math.sqrt(absDx);
			final int slowDX = (int) Math.max(absDx * sqrtDx / scrollingDelay, scrollingDelay * sqrtDx) * v / 100;
			if (Math.abs(dx) > 2 && slowDX < Math.abs(dx)) {
	            dx = slowDX * Integer.signum(dx);
            }
			return dx;
        }
		void startSlowScrolling(int scrollingDelay) {
			this.scrollingDelay = scrollingDelay;
		}
	}
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	final private Border defaultBorder;

	public MapViewScrollPane() {
		super(VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_ALWAYS);
		setViewport(new MapViewPort());
		defaultBorder = getBorder();

		addHierarchyListener(new HierarchyListener() {

            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && ! isValid()) {
                    revalidate();
                    repaint();
                }
            }
        });
	}

	@Override
    public void addNotify() {
	    super.addNotify();
		setScrollbarsVisiblilty();
		UITools.setScrollbarIncrement(this);
		ResourceController.getResourceController().addPropertyChangeListener(MapViewScrollPane.this);
    }

	@Override
    public void removeNotify() {
	    super.removeNotify();
		ResourceController.getResourceController().removePropertyChangeListener(MapViewScrollPane.this);
    }



	@Override
    public void setUI(ScrollPaneUI ui) {
	    super.setUI(ui);
	    SwingUtilities.replaceUIInputMap(this, JComponent.
                WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
    }

    @Override
    public void setBorder(Border border) {
        super.setBorder(border);
    }

    @Override
	public void propertyChanged(String propertyName, String newValue, String oldValue) {
		if(ViewController.FULLSCREEN_ENABLED_PROPERTY.equals(propertyName)
				|| propertyName.startsWith("scrollbarsVisible")){
			setScrollbarsVisiblilty();
		}
		else if (propertyName.equals(UITools.SCROLLBAR_INCREMENT)) {
			final int scrollbarIncrement = Integer.valueOf(newValue);
			getHorizontalScrollBar().setUnitIncrement(scrollbarIncrement);
			getVerticalScrollBar().setUnitIncrement(scrollbarIncrement);
		}

    }

	private void setScrollbarsVisiblilty() {
	    final ViewController viewController = Controller.getCurrentController().getViewController();
		boolean areScrollbarsVisible = viewController.areScrollbarsVisible();
		if(areScrollbarsVisible) {
			getVerticalScrollBar().setPreferredSize(null);
			getHorizontalScrollBar().setPreferredSize(null);
		}
		else {
			getVerticalScrollBar().setPreferredSize(INVISIBLE);
			getHorizontalScrollBar().setPreferredSize(INVISIBLE);
		}
	    final boolean isFullScreenEnabled =  viewController.isFullScreenEnabled();
	    setBorder(isFullScreenEnabled && ! areScrollbarsVisible ? BorderFactory.createEmptyBorder() : defaultBorder);
		revalidate();
		repaint();
    }

	public void addViewportReservedAreaSupplier(ViewportReservedAreaSupplier supplier) {
		((MapViewPort)getViewport()).addReservedAreaSupplier(supplier);
	}

	public void removeReservedAreaSupplier(ViewportReservedAreaSupplier supplier) {
		((MapViewPort)getViewport()).removeReservedAreaSupplier(supplier);
	}


	@Override
    public void doLayout() {
		if(viewport != null){
			final Component view = viewport.getView();
			if(view != null)
				view.invalidate();
		}
        super.doLayout();
    }

	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
		if(viewport != null){
			final ActionAcceleratorManager acceleratorManager = ResourceController.getResourceController().getAcceleratorManager();
			if(acceleratorManager.canProcessKeyEvent(e))
				return false;
		}
		return super.processKeyBinding(ks, e, condition, pressed);
	}



}
