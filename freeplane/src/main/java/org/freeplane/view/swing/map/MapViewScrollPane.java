/*
 *  Freeplane - mind map editor
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
	public interface ViewportHiddenAreaSupplier {
		Rectangle getHiddenArea();
	}
	@SuppressWarnings("serial")
    static class MapViewPort extends JViewport{
		private List<ViewportHiddenAreaSupplier> hiddenAreaSuppliers = new ArrayList<>();
		private boolean layoutInProgress = false;

		void addHiddenAreaSupplier(ViewportHiddenAreaSupplier hiddenAreaSupplier) {
			removeHiddenAreaSupplier(hiddenAreaSupplier);
			this.hiddenAreaSuppliers.add(hiddenAreaSupplier);
		}
		void removeHiddenAreaSupplier(ViewportHiddenAreaSupplier hiddenAreaSupplier) {
			this.hiddenAreaSuppliers.remove(hiddenAreaSupplier);
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

        public void setBackgroundComponent(ScalableComponent backgroundComponent) {
            this.backgroundComponent = backgroundComponent;
        }



        @Override
        public void scrollRectToVisible(final Rectangle newContentRectangle) {
            // Start with a copy of the original rectangle.
            Rectangle candidateRect = new Rectangle(newContentRectangle);
            
            // Compute the maximum insets from all hidden area suppliers.
            // These insets represent the area obscured by overlays on each side.
            int leftInset = 0, rightInset = 0, topInset = 0, bottomInset = 0;
            for(ViewportHiddenAreaSupplier supplier : hiddenAreaSuppliers) {
                Rectangle hidden = supplier.getHiddenArea();
                if(hidden.width == 0 || hidden.height == 0)
                    continue;
                // Translate hidden area to viewport coordinates.
                Rectangle h = new Rectangle(hidden);
                h.x -= getX();
                if(h.x < 0) {
                    h.width += h.x; // reduce width by the negative offset
                    h.x = 0;
                }
                h.y -= getY();
                if(h.y < 0) {
                    h.height += h.y;
                    h.y = 0;
                }
                if(h.x + h.width > getWidth())
                    h.width = getWidth() - h.x;
                if(h.y + h.height > getHeight())
                    h.height = getHeight() - h.y;
                
                // Determine which side the hidden area is drawn on.
                // (We assume a hidden area is "small" relative to the full viewport.)
                if(h.x == 0 && h.width < getWidth() / 2) {
                    leftInset = Math.max(leftInset, h.width);
                }
                if(h.x + h.width == getWidth() && h.x >= getWidth() / 2) {
                    rightInset = Math.max(rightInset, h.width);
                }
                if(h.y == 0 && h.height < getHeight() / 2) {
                    topInset = Math.max(topInset, h.height);
                }
                if(h.y + h.height == getHeight() && h.y >= getHeight() / 2) {
                    bottomInset = Math.max(bottomInset, h.height);
                }
            }
            
            // --- Horizontal adjustment ---
            // The idea is that the "safe" (visible) horizontal area is
            // [candidateRect.x + leftInset, candidateRect.x + candidateRect.width - rightInset]
            // and that area should contain newContentRectangle's x-range.
            boolean adjustLeft = leftInset > 0 && newContentRectangle.x < leftInset;
            boolean adjustRight = rightInset > 0 &&
                    (newContentRectangle.x + newContentRectangle.width) > (getWidth() - rightInset);
            
            if(adjustLeft && !adjustRight && leftInset >= rightInset) {
                // One-sided left adjustment is sufficient.
                candidateRect.x = newContentRectangle.x - leftInset;
                candidateRect.width = newContentRectangle.width + leftInset;
            }
            else if(adjustRight && !adjustLeft && rightInset >= leftInset) {
                // One-sided right adjustment is sufficient.
                candidateRect.width = newContentRectangle.width + rightInset;
            }
            else if(adjustLeft && adjustRight) {
                // Both sides intrude, so expand candidateRect on both sides.
                candidateRect.x = newContentRectangle.x - leftInset;
                candidateRect.width = newContentRectangle.width + leftInset + rightInset;
            }
            
            // --- Vertical adjustment ---
            // Similarly, the vertical safe area is
            // [candidateRect.y + topInset, candidateRect.y + candidateRect.height - bottomInset].
            boolean adjustTop = topInset > 0 && newContentRectangle.y < topInset;
            boolean adjustBottom = bottomInset > 0 &&
                    (newContentRectangle.y + newContentRectangle.height) > (getHeight() - bottomInset);
            
            if(adjustTop && !adjustBottom && topInset >= bottomInset) {
                candidateRect.y = newContentRectangle.y - topInset;
                candidateRect.height = newContentRectangle.height + topInset;
            }
            else if(adjustBottom && !adjustTop && bottomInset >= topInset) {
                candidateRect.height = newContentRectangle.height + bottomInset;
            }
            else if(adjustTop && adjustBottom) {
                candidateRect.y = newContentRectangle.y - topInset;
                candidateRect.height = newContentRectangle.height + topInset + bottomInset;
            }
            
            // Finally, scroll so that the expanded candidateRect (which now
            // accounts for the hidden areas using one-sided adjustments whenever possible)
            // is visible.
            super.scrollRectToVisible(candidateRect);
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
				Integer scrollingDelay = (Integer) getClientProperty(ViewController.SLOW_SCROLLING);
				if(scrollingDelay != null && scrollingDelay != 0){
					putClientProperty(ViewController.SLOW_SCROLLING, null);
					slowSetViewPosition(p, scrollingDelay);
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

		private void slowSetViewPosition(final Point p, final int delay) {
			stopTimer();
			final Point viewPosition = getViewPosition();
	        int dx = p.x - viewPosition.x;
	        int dy = p.y - viewPosition.y;
	        int slowDx = calcScrollIncrement(dx);
	        int slowDy = calcScrollIncrement(dy);
	        viewPosition.translate(slowDx, slowDy);
	        super.setViewPosition(viewPosition);
	        if(slowDx == dx && slowDy == dy)
	            return;
	        timer = new Timer(delay, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					timer = null;
					MapViewPort.this.slowSetViewPosition(p, delay);
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
			int v = ResourceController.getResourceController().getIntProperty("scrolling_speed");
			final int absDx = Math.abs(dx);
			final double sqrtDx = Math.sqrt(absDx);
			final int slowDX = (int) Math.max(absDx * sqrtDx / 20, 20 * sqrtDx) * v / 100;
			if (Math.abs(dx) > 2 && slowDX < Math.abs(dx)) {
	            dx = slowDX * Integer.signum(dx);
            }
			return dx;
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

	public void addViewportHiddenAreaSupplier(ViewportHiddenAreaSupplier hiddenAreaSupplier) {
		((MapViewPort)getViewport()).addHiddenAreaSupplier(hiddenAreaSupplier);
	}

	public void removeHiddenAreaSupplier(ViewportHiddenAreaSupplier hiddenAreaSupplier) {
		((MapViewPort)getViewport()).removeHiddenAreaSupplier(hiddenAreaSupplier);
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
