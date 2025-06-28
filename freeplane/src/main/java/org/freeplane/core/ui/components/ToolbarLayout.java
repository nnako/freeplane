package org.freeplane.core.ui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.JSeparator;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

public class ToolbarLayout implements LayoutManager {

    public static final int MAX_WIDTH_BY_PARENT_WIDTH = -1;

    public static ToolbarLayout fix() {
        return new ToolbarLayout(BlockEndPosition.ON_EVERY_SEPARATOR);
    }

    public static ToolbarLayout horizontal() {
        return new ToolbarLayout(BlockEndPosition.ON_SEPARATOR);
    }

    public static ToolbarLayout vertical() {
        return new ToolbarLayout(BlockEndPosition.ANYWHERE);
    }

    private BlockEndPosition blockEndPosition;
	private int maximumWidth = MAX_WIDTH_BY_PARENT_WIDTH;
	private int gap = 0;
	private boolean addsMargins = false;

	enum BlockEndPosition{ON_SEPARATOR, ON_EVERY_SEPARATOR, ANYWHERE}
	ToolbarLayout(BlockEndPosition blockEndPosition){
		this.blockEndPosition = blockEndPosition;

	}

	public int getMaximumWidth() {
        return maximumWidth;
    }

	public void setMaximumWidth(int maximumWidth) {
        this.maximumWidth = maximumWidth;
    }

	public int getGap() {
		return gap;
	}

	public void setGap(int horizontalGap, boolean addMargins) {
		this.gap = horizontalGap;
		this.addsMargins = addMargins;
	}

	public boolean addsMargins() {
		return addsMargins;
	}

	public void setAddsMargins(boolean addsMargins) {
		this.addsMargins = addsMargins;
	}

    @Override
    public void addLayoutComponent(final String name, final Component comp) {
	}

	@Override
    public void layoutContainer(final Container container) {
		if(! container.isVisible())
			return;
		int horizontalGap = gap;
		int verticalGap = gap;
		int blockWidth = 0;
		int blockHeight = 0;
		boolean hasVisibleComponentInBlock = false;
		int lastBlockWidth = 0;
		int lastBlockHeight = 0;
		int lastBlockStart = 0;
		int lastBlockFinish = 0;
		Insets insets = container.getInsets();
		int leftMargin = insets.left;
		int height =  insets.top;
		if (addsMargins) {
			height += verticalGap;
		}
		final int maximumWidth = calculateMaxWidth(container) - insets.left - insets.right;
		for (int i = 0;; i++) {
			final Component component = i < container.getComponentCount() ? container.getComponent(i) : null;
			if (component == null || component instanceof JSeparator || blockEndPosition == BlockEndPosition.ANYWHERE) {
				int totalBlockWidth = blockWidth;
				if (i > container.getComponentCount() || blockEndPosition == BlockEndPosition.ON_EVERY_SEPARATOR || lastBlockWidth + totalBlockWidth > maximumWidth) {
					int x = leftMargin;
					if (addsMargins) {
						x += horizontalGap;
					}
					int actualWidth = 0;
					if (addsMargins) {
						actualWidth += horizontalGap;
					}
					boolean addGap = false;
					for (int j = lastBlockStart; j < lastBlockFinish; j++) {
						final Component c = container.getComponent(j);
						final int width = getPreferredWidth(c, maximumWidth);
						if (addGap && c.isVisible()) {
							x += horizontalGap;
							actualWidth += horizontalGap;
						}
						c.setBounds(x, height, width, lastBlockHeight);
						x += width;
						actualWidth += width;
						if (c.isVisible()) {
							addGap = true;
						}
					}
					if (addsMargins) {
						actualWidth += horizontalGap;
					}
					assert actualWidth == lastBlockWidth : "Width calculation mismatch: calculated=" + lastBlockWidth + ", actual=" + actualWidth;
					if (lastBlockHeight > 0) {
						height += lastBlockHeight + verticalGap;
					}
					lastBlockWidth = totalBlockWidth;
					lastBlockHeight = blockHeight;
					lastBlockStart = lastBlockFinish;
				}
				else {
					lastBlockWidth += totalBlockWidth;
					lastBlockHeight = Math.max(blockHeight, lastBlockHeight);
				}
				lastBlockFinish = i;
				blockWidth = blockHeight = 0;
				hasVisibleComponentInBlock = false;
			}
			if (component == null) {
				if (lastBlockStart == container.getComponentCount()) {
					break;
				}
				lastBlockFinish = container.getComponentCount();
				continue;
			}
			if (component.isVisible() && hasVisibleComponentInBlock) {
				blockWidth += horizontalGap;
			}
			blockWidth += getPreferredWidth(component, maximumWidth);
			if (component.isVisible() && !hasVisibleComponentInBlock && addsMargins) {
				blockWidth += 2 * horizontalGap;
			}
			if (component.isVisible()) {
				hasVisibleComponentInBlock = true;
				final Dimension compPreferredSize = component.getPreferredSize();
				blockHeight = Math.max(compPreferredSize.height, blockHeight);
			}
		}
	}

    private int calculateMaxWidth(final Container container) {
        if(container.isMaximumSizeSet())
            return container.getMaximumSize().width;
        Container viewport = SwingUtilities.getAncestorOfClass(JViewport.class, container);
        if (viewport != null)
            return viewport.getWidth();
        Container parent = container.getParent();
        if (parent != null)
            return parent.getWidth();
        if (maximumWidth >= 0)
            return maximumWidth;

        return Integer.MAX_VALUE;
    }
	private int getPreferredWidth(final Component c, final int maxWidth) {
		final int width = ! c.isVisible() ? 0 :
				c instanceof JSeparator && blockEndPosition == BlockEndPosition.ANYWHERE ? maxWidth :
					c.getPreferredSize().width;
		return width;
	}

	@Override
    public Dimension minimumLayoutSize(final Container comp) {
		return new Dimension(0, 0);
	}

	@Override
    public Dimension preferredLayoutSize(final Container container) {
		Insets insets = container.getInsets();
		int maxWidth = calculateMaxWidth(container) - insets.left - insets.right;
		int horizontalGap = gap;
		int verticalGap = gap;
		for(;;) {
	        int width = 0;
	        int height = 0;
	        if (addsMargins) {
	            height += verticalGap;
	        }
	        int blockWidth = 0;
	        int blockHeight = 0;
	        boolean hasVisibleComponentInBlock = false;
	        int lastBlockWidth = 0;
	        int lastBlockHeight = 0;
	        int lastBlockStart = 0;
	        int lastBlockFinish = 0;
	        for (int i = 0;; i++) {
	            final Component component = i < container.getComponentCount() ? container.getComponent(i) : null;
	            if (component == null || component instanceof JSeparator || blockEndPosition == BlockEndPosition.ANYWHERE) {
	                int totalBlockWidth = blockWidth;
	                if (i > container.getComponentCount() || blockEndPosition == BlockEndPosition.ON_EVERY_SEPARATOR || lastBlockWidth + totalBlockWidth > maxWidth) {
	                    if (lastBlockHeight > 0) {
	                        height += lastBlockHeight + verticalGap;
	                    }
	                    lastBlockWidth = totalBlockWidth;
	                    lastBlockHeight = blockHeight;
	                    lastBlockStart = lastBlockFinish;
	                }
	                else {
	                    lastBlockWidth += totalBlockWidth;
	                    lastBlockHeight = Math.max(blockHeight, lastBlockHeight);
	                }
	                width = Math.max(width, lastBlockWidth);
	                lastBlockFinish = i;
	                blockWidth = blockHeight = 0;
	                hasVisibleComponentInBlock = false;
	            }
	            if (component == null) {
	                if (lastBlockStart == container.getComponentCount()) {
	                    break;
	                }
	                lastBlockFinish = container.getComponentCount();
	                continue;
	            }
	            if (component.isVisible() && hasVisibleComponentInBlock) {
	                blockWidth += horizontalGap;
	            }
	            blockWidth += getPreferredWidth(component, maxWidth);
	            if (component.isVisible() && !hasVisibleComponentInBlock && addsMargins) {
	                blockWidth += 2 * horizontalGap;
	            }
	            if (component.isVisible()) {
	                hasVisibleComponentInBlock = true;
	                final Dimension compPreferredSize = component.getPreferredSize();
	                blockHeight = Math.max(compPreferredSize.height, blockHeight);
	            }
	        }
	        if (!addsMargins && height > 0) {
	            height -= verticalGap;
	        }
	        if(maxWidth >= width) {
	            Dimension preferredSize = new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
	            return preferredSize;
	        }
	        else
	            maxWidth = width;
		}
	}

	@Override
    public void removeLayoutComponent(final Component comp) {
	}
}
