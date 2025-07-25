/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
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
package org.freeplane.core.ui.components;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.icon.factory.IconFactory;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.styles.LogicalStyleController.StyleOption;

public class MultipleImageIcon implements Icon {
    private static final int UNKNOWN = -1;
    final private IconRow iconRow = new IconRow();
	final private List<NamedIcon> mUIIcons = new ArrayList<>();
	final private List<TagIcon> mTags = new ArrayList<>();
	private int horizontalAlignment = SwingConstants.LEFT;

	public MultipleImageIcon() {
	}

	public int getHorizontalAlignment() {
		return horizontalAlignment;
	}

	public void setHorizontalAlignment(int alignment) {
		this.horizontalAlignment = alignment;
	}

	public void addIcon(final NamedIcon uiIcon) {
		Icon icon = uiIcon.getIcon();
		Objects.requireNonNull(icon);
		iconRow.addIcon(icon);
		mUIIcons.add(uiIcon);
	}

	public void addIcon(final NamedIcon uiIcon, Quantity<LengthUnit> iconHeight) {
        Icon icon = uiIcon.getIcon(iconHeight);
        Objects.requireNonNull(icon);
        iconRow.addIcon(icon);
		mUIIcons.add(uiIcon);
	}

    public void addLinkIcon(Icon icon, NodeModel node, StyleOption option) {
        Objects.requireNonNull(icon);
        final Quantity<LengthUnit> iconHeight = IconController.getController().getIconSize(node, option);
        final IconFactory iconFactory = IconFactory.getInstance();
        final Icon scaledIcon = iconFactory.canScaleIcon(icon) ? iconFactory.getScaledIcon(icon, iconHeight) : icon;
        iconRow.addIcon(scaledIcon);
        mUIIcons.add(null);
    }

    public void addIcon(Icon icon) {
        Objects.requireNonNull(icon);
        iconRow.addIcon(icon);
        mUIIcons.add(null);
    }

    public void addTag(TagIcon tag) {
        Objects.requireNonNull(tag);
        mTags.add(tag);
    }

	@Override
    public int getIconHeight() {
		int height = getGraphicalIconHeight();
		for(Icon tag : mTags)
		    height += tag.getIconHeight();
        return height;
	}

    private int getGraphicalIconHeight() {
        return iconRow.getIconHeight();
    }

	@Override
    public int getIconWidth() {
		int width = getGraphicalIconWidth();
        for(Icon tag : mTags)
            width = Math.max(width, tag.getIconWidth());
        return width;
	}

    private int getGraphicalIconWidth() {
        return iconRow.getIconWidth();
    }

	public int getImageCount() {
		return iconRow.getImageCount();
	}

	@Override
	public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
		boolean isLeftToRight = horizontalAlignment == SwingConstants.LEFT;
	    final int graphicalIconWidth = isLeftToRight ? 0 : getGraphicalIconWidth();
	    final int iconWidth = isLeftToRight ? 0 : getIconWidth();
	    {
	        int myX = x;
	        if (horizontalAlignment == SwingConstants.CENTER)
	        	myX += (iconWidth - graphicalIconWidth)/2;
	        else if (horizontalAlignment == SwingConstants.RIGHT)
	        	myX +=iconWidth - graphicalIconWidth;
	        iconRow.paintIcon(c, g, myX, y);
	    }
	    int graphicalIconHeight = getGraphicalIconHeight();
	    int myY = graphicalIconHeight == 0 ? y : y + graphicalIconHeight;
	    for (final Icon icon : mTags) {
	        int myX = x;
	        if (horizontalAlignment == SwingConstants.CENTER)
	        	myX += (iconWidth - icon.getIconWidth())/2;
	        else if (horizontalAlignment == SwingConstants.RIGHT)
	        	myX +=iconWidth - icon.getIconWidth();
	        icon.paintIcon(c, g, myX, myY);
	        myY += icon.getIconHeight();
	    }
	}

	public NamedIcon getUIIconAt(Point coordinate){
		if(! iconRow.containsIcons() || coordinate.x < 0 || coordinate.y < 0 || coordinate.y >= getGraphicalIconHeight())
			return null;
		boolean isLeftToRight = horizontalAlignment == SwingConstants.LEFT;
		final int graphicalIconWidth = isLeftToRight ? 0 : getGraphicalIconWidth();
		final int iconWidth = isLeftToRight ? 0 : getIconWidth();
		int iconX = 0;
		if (horizontalAlignment == SwingConstants.CENTER)
			iconX += (iconWidth - graphicalIconWidth)/2;
		else if (horizontalAlignment == SwingConstants.RIGHT)
			iconX +=iconWidth - graphicalIconWidth;
		for (int iconIndex = 0; iconIndex < iconRow.getImageCount(); iconIndex++)
		{
			iconX += iconRow.getIcon(iconIndex).getIconWidth();
			if(coordinate.x <= iconX){
				return mUIIcons.get(iconIndex);
			}
		}
		return null;
	}
    public TagIcon getTagIconAt(Point coordinate) {
        if(mTags.isEmpty() || coordinate.x < 0 || coordinate.y <= getGraphicalIconHeight() || coordinate.x >= getIconWidth())
            return null;
        int graphicalIconHeight = getGraphicalIconHeight();
        int myY = graphicalIconHeight == 0 ? 0 : graphicalIconHeight;
        for (final TagIcon icon : mTags) {
            final int iconHeight = icon.getIconHeight();
            if(myY <= coordinate.y && coordinate.y < myY + iconHeight)
                return icon;
            myY += iconHeight;
        }
        return null;
    }

	//DOCEAR - get a rect relative to this image for a specific icon
	public Rectangle getIconR(Icon icon) {
		return iconRow.getIconR(icon);
	}

    public boolean containsIcons() {
        return iconRow.containsIcons() || ! mTags.isEmpty();
    }

}
