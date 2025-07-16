package org.freeplane.view.swing.map;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.text.View;

import org.freeplane.core.ui.components.html.ScaledHTML;
import org.freeplane.core.util.TextUtils;

/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2009 Dimitry
 *
 *  This file author is Dimitry
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
/**
 * @author Dimitry Polivaev
 * 23.08.2009
 */
public class ZoomableLabelUI extends BasicLabelUI {
	private static final char RLM = '\u200F';
	private static final char LRM = '\u200E';

	private static String bidiSafe(JLabel lbl, String s) {
	    if (s == null || s.isEmpty() || BasicHTML.isHTMLString(s))
	        return s;

	    boolean compLTR = lbl.getComponentOrientation().isLeftToRight();
	    int i = 0;
	    while (i < s.length()) {
	        int cp = s.codePointAt(i);
	        byte dir = Character.getDirectionality(cp);
	        if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
	            return compLTR ? s : RLM + s;
	        }
	        if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
	            || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
	            return compLTR ? LRM + s : s;
	        }
	        i += Character.charCount(cp);      // skip neutrals, whitespace, punctuation
	    }
	    return s;   // no strong character found
	}

	private boolean isPainting = false;

	static ZoomableLabelUI labelUI = new ZoomableLabelUI();
	private Rectangle iconR = new Rectangle();
	private Rectangle textR = new Rectangle();
	private Rectangle viewR = new Rectangle();

	public static class LayoutData{
		final public Rectangle viewR;
		final public Rectangle iconR;
		final public Rectangle textR;
		public LayoutData(Rectangle viewR, Rectangle textR, Rectangle iconR) {
			super();
			this.viewR = viewR;
			this.textR = textR;
			this.iconR = iconR;
		}

	}



	@Override
	public Dimension getPreferredSize(final JComponent c) {
		final Dimension preferredSize = super.getPreferredSize(c);
		final ZoomableLabel zoomableLabel = (ZoomableLabel) c;
		if(zoomableLabel.getIcon() == null){
			final int fontHeight = zoomableLabel.getFontMetrics().getHeight();
			final Insets insets = c.getInsets();
			preferredSize.width = Math.max(preferredSize.width, fontHeight/2  + insets.left + insets.right);
			preferredSize.height = Math.max(preferredSize.height, fontHeight + insets.top + insets.bottom);
		}
		final float zoom = zoomableLabel.getZoom();
		if (zoom != 1f) {
			preferredSize.width = (int) (Math.ceil(zoom * preferredSize.width));
			preferredSize.height = (int) (Math.ceil(zoom * preferredSize.height));
		}
		int minimumWidth = zoomableLabel.getMinimumWidth();
		if(minimumWidth != 0)
		preferredSize.width = Math.max(minimumWidth, preferredSize.width);
		return preferredSize;
	}

	public static ComponentUI createUI(final JComponent c) {
		return labelUI;
	}

	@Override
	protected String layoutCL(final JLabel label, final FontMetrics fontMetrics, final String text, final Icon icon,
			final Rectangle viewR, final Rectangle iconR, final Rectangle textR) {
		LayoutData preferredLayoutData = (LayoutData) label.getClientProperty("preferredLayoutData");
		final String bidiSafeText = bidiSafe(label, text);
		if(preferredLayoutData != null) {
			viewR.x = preferredLayoutData.viewR.x;
			viewR.y = preferredLayoutData.viewR.y;
			viewR.width = preferredLayoutData.viewR.width;
			viewR.height = preferredLayoutData.viewR.height;
			textR.x = preferredLayoutData.textR.x;
			textR.y = preferredLayoutData.textR.y;
			textR.width = preferredLayoutData.textR.width;
			textR.height = preferredLayoutData.textR.height;
			iconR.x = preferredLayoutData.iconR.x;
			iconR.y = preferredLayoutData.iconR.y;
			iconR.width = preferredLayoutData.iconR.width;
			iconR.height = preferredLayoutData.iconR.height;
			return bidiSafeText;
		}
		final ZoomableLabel zLabel = (ZoomableLabel) label;
		final float zoom = zLabel.getZoom();
		final float scale = zoom;
		if (isPainting) {
			final Insets insets = zLabel.getInsets();
			final int width = zLabel.getWidth();
			final int height = zLabel.getHeight();
			viewR.x = insets.left;
			viewR.y = insets.top;
			viewR.width = (int) (width  / scale) - (insets.left + insets.right);
			viewR.height = (int)(height / scale) - (insets.top + insets.bottom);
			if(viewR.width < 0)
				viewR.width = 0;
		}
		else {
			if(zLabel.getMaximumWidth() != Integer.MAX_VALUE){
				final int maximumWidth = (int) (zLabel.getMaximumWidth() / scale);
				final Insets insets = label.getInsets();
				viewR.width = maximumWidth - insets.left - insets.right;
				if(viewR.width < 0)
					viewR.width = 0;
				ScaledHTML.Renderer v = (ScaledHTML.Renderer) label.getClientProperty(BasicHTML.propertyKey);
				if (v != null) {
					int availableTextWidth = viewR.width;
					if(icon != null && label.getVerticalTextPosition() != SwingConstants.BOTTOM)
						availableTextWidth -= icon.getIconWidth() + label.getIconTextGap();
					float minimumWidth = v.getMinimumSpan(View.X_AXIS);
					if(minimumWidth > availableTextWidth){
						viewR.width += minimumWidth - availableTextWidth;
						availableTextWidth = (int) minimumWidth;
					}
					int currentWidth = v.getWidth();
					if(currentWidth != availableTextWidth) {
						float viewPreferredWidth = v.getPreferredWidth();

						if(viewPreferredWidth > availableTextWidth){
							v.setWidth(availableTextWidth);
							layoutCompoundLabel(bidiSafeText, icon, viewR, iconR, textR, zLabel);
							return bidiSafeText;
						}
						else if(currentWidth != viewPreferredWidth)
							v.resetWidth();
					}
				}
			}
		}
		Icon textRenderingIcon = zLabel.getTextRenderingIcon();
		if(textRenderingIcon != null){
			layoutLabelWithTextIcon(textRenderingIcon, icon, viewR, iconR, textR, zLabel);
		} else
            layoutCompoundLabel(bidiSafeText, icon, viewR, iconR, textR, zLabel);

		if(! isPainting)
			return bidiSafeText;

		int reservedIconWidth = iconR.width == 0 ? 0 : iconR.width + label.getIconTextGap();
		int availableTextWidth = viewR.width;
		if(iconR.width > 0 && label.getVerticalTextPosition() != SwingConstants.BOTTOM) {
			availableTextWidth -= reservedIconWidth;
		}
		ScaledHTML.Renderer v = (ScaledHTML.Renderer) label.getClientProperty(BasicHTML.propertyKey);
		if (v != null) {
			if (textR.width < availableTextWidth) {
				textR.width = availableTextWidth;
				v.setWidth(availableTextWidth);
			}
		}

		switch(label.getVerticalTextPosition()) {
		case SwingConstants.BOTTOM: {
			iconR.x = viewR.x;
			iconR.y = viewR.y;
			textR.x = viewR.x;
			textR.y = iconR.height == 0 ? viewR.y : viewR.y + iconR.height + label.getIconTextGap();
			int extraTopMargin = (viewR.height - (textR.y + textR.height - viewR.y))/2;
			iconR.y += extraTopMargin;
			textR.y += extraTopMargin;
			int horizontalAlignment = zLabel.getEffectiveHorizontalAlignment();
			switch (horizontalAlignment) {
			case SwingConstants.CENTER:
				iconR.x += (viewR.width - iconR.width)/2;
				textR.x += (viewR.width - textR.width)/2;
				break;
			case SwingConstants.RIGHT:
				iconR.x += viewR.width - iconR.width;
				textR.x += viewR.width - textR.width;
				break;
			}
			break;
		}
		default: {
			switch(label.getVerticalTextPosition()) {
			case SwingConstants.TOP:
				iconR.y = textR.y = viewR.y + (viewR.height - Math.max(textR.height, iconR.height))/2;
				break;
			case SwingConstants.CENTER:
				iconR.y = viewR.y + (viewR.height - iconR.height) / 2;
				textR.y = viewR.y + (viewR.height - textR.height) / 2;
				break;
			}
			int effectiveHorizontalTextPosition = zLabel.getEffectiveHorizontalTextPosition();
			iconR.x = effectiveHorizontalTextPosition == SwingConstants.RIGHT ? viewR.x : viewR.x + viewR.width - iconR.width;
			textR.x = effectiveHorizontalTextPosition == SwingConstants.LEFT || iconR.width == 0 ? viewR.x : iconR.x + iconR.width + label.getIconTextGap();
			int horizontalAlignment = zLabel.getEffectiveHorizontalAlignment();
			switch (horizontalAlignment) {
			case SwingConstants.CENTER:
				textR.x += (availableTextWidth - textR.width)/2;
				break;
			case SwingConstants.RIGHT:
				textR.x += availableTextWidth - textR.width;
				break;
			}
			break;
		}}
		return bidiSafeText;
	}

    private void layoutCompoundLabel(final String text, final Icon icon, final Rectangle viewR,
            final Rectangle iconR, final Rectangle textR, final ZoomableLabel zLabel) {
        int verticalTextPosition = zLabel.getVerticalTextPosition();
        SwingUtilities.layoutCompoundLabel(
        zLabel,
        zLabel.getFontMetrics(),
        text,
        icon,
        zLabel.getVerticalAlignment(),
        zLabel.getHorizontalAlignment(),
        verticalTextPosition,
        verticalTextPosition != SwingConstants.BOTTOM ? SwingConstants.LEFT : SwingConstants.CENTER,
        viewR,
        iconR,
        textR,
        zLabel.getIconTextGap());
    }

	static private void layoutLabelWithTextIcon(final Icon textRenderingIcon, final Icon icon,
			final Rectangle viewR, final Rectangle iconR,
			final Rectangle textR, final ZoomableLabel zLabel) {
		int verticalTextPosition = zLabel.getVerticalTextPosition();
		if (icon != null) {
			iconR.width = icon.getIconWidth();
			iconR.height = icon.getIconHeight();
		}
		else {
			iconR.width = iconR.height = 0;
			textR.x = viewR.x;
			textR.y = viewR.y;
		}
		/* Unless both text and icon are non-null, we effectively ignore
		 * the value of textIconGap.
		 */
		int gap = iconR.width == 0 ? 0 : zLabel.getIconTextGap();
		int availTextWidth;
		if (verticalTextPosition == SwingConstants.BOTTOM) {
			availTextWidth = viewR.width;
		}
		else {
			availTextWidth = viewR.width - (iconR.width + gap);
		}
		textR.width = Math.min(availTextWidth, textRenderingIcon.getIconWidth());
		textR.height = textRenderingIcon.getIconHeight();
		if(iconR.width != 0) {
			if (verticalTextPosition == SwingConstants.BOTTOM) {
				iconR.x = textR.x = viewR.x;
				iconR.y = viewR.y;
				textR.y = iconR.y + iconR.height + gap;
			}
			else {
				iconR.y = textR.y = viewR.y;
				iconR.x = viewR.x;
				textR.x = iconR.x + iconR.width + gap;
			}
		}
	}

	@Override
	public void paint(final Graphics g, final JComponent label) {
		final ZoomableLabel mainView = (ZoomableLabel) label;
		if (!mainView.useFractionalMetrics()) {
			try {
				isPainting = true;
				superPaintSafe(g, mainView);
			}
			finally {
				isPainting = false;
			}
			return;
		}
		final Graphics2D g2 = (Graphics2D) g;
		final Object oldRenderingHintFM = g2.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
		final Object newRenderingHintFM = RenderingHints.VALUE_FRACTIONALMETRICS_ON;
		if (oldRenderingHintFM != newRenderingHintFM) {
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, newRenderingHintFM);
		}
		final AffineTransform transform = g2.getTransform();
		final float factor = 0.97f;
		final float zoom = mainView.getZoom() * factor;
		if(mainView.getVerticalAlignment() == SwingConstants.CENTER) {
			final float translationFactorY = 0.5f;
			g2.translate(0, mainView.getHeight() * (1f - factor) * translationFactorY);
		}
		g2.scale(zoom, zoom);
		final boolean htmlViewSet = null != label.getClientProperty(BasicHTML.propertyKey);
		try {
			isPainting = true;
			if(htmlViewSet){
				ScaledHTML.resetPainter();
			}
			superPaintSafe(g, mainView);
		}
		finally {
			isPainting = false;
			if(htmlViewSet){
				ScaledHTML.resetPainter();
			}
		}
		g2.setTransform(transform);
		if (oldRenderingHintFM != newRenderingHintFM) {
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, oldRenderingHintFM != null ? oldRenderingHintFM
					: RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
		}
	}

	// Workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7126361
	private void superPaintSafe(final Graphics g, final ZoomableLabel label) {
		try {
			final boolean isTextTransparent = label.getForeground().getAlpha() == 0;
			Icon textRenderingIcon = label.getTextRenderingIcon();
			if(isTextTransparent)
				paintIcon(g, label);
			else if (textRenderingIcon  != null)
				paintIcons(g, label, textRenderingIcon);
			else
				super.paint(g, label);
		} catch (ClassCastException e) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					label.setText(TextUtils.format("html_problem", label.getText()));
				}
			});
		}
	}

	private void paintIcon(Graphics g, ZoomableLabel label) {
		Icon icon = (label.isEnabled()) ? label.getIcon() : label.getDisabledIcon();

		if ((icon == null)) {
			return;
		}
		FontMetrics fm = label.getFontMetrics(g.getFont());
		String text = label.getText();
		Rectangle paintViewR = new Rectangle();
		Rectangle paintIconR = new Rectangle();
		Rectangle paintTextR = new Rectangle();
		layoutCL(label, fm, text, icon, paintViewR, paintIconR, paintTextR);

		if (icon != null) {
			icon.paintIcon(label, g, paintIconR.x, paintIconR.y);
		}
	}

	private void paintIcons(Graphics g, ZoomableLabel label, Icon textRenderingIcon) {
		Icon icon = (label.isEnabled()) ? label.getIcon() : label.getDisabledIcon();
		Rectangle paintViewR = new Rectangle();
		Rectangle paintIconR = new Rectangle();
		Rectangle paintTextR = new Rectangle();
		layoutCL(label, null, null, icon, paintViewR, paintIconR, paintTextR);
		if (icon != null) {
			icon.paintIcon(label, g, paintIconR.x, paintIconR.y);
		}
		textRenderingIcon.paintIcon(label, g, paintTextR.x, paintTextR.y);
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		ZoomableLabel lbl = ((ZoomableLabel) e.getSource());
		String propertyName = e.getPropertyName();
		if (propertyName == "text" || "font" == propertyName || "foreground" == propertyName
				|| "horizontalAlignment" == propertyName || "componentOrientation" == propertyName
				|| ("ancestor" == propertyName || "graphicsConfiguration" == propertyName) && e.getNewValue() != null
				|| ZoomableLabel.CUSTOM_CSS == propertyName)
			updateRendererOnPropertyChange(lbl, propertyName);
		else {
			super.propertyChange(e);
			View view = (View) lbl.getClientProperty(BasicHTML.propertyKey);
			if (view != null && ! (view instanceof ScaledHTML.Renderer))
				updateRendererOnPropertyChange(lbl, propertyName);
		}
	}

	private void updateRendererOnPropertyChange(ZoomableLabel lbl, String propertyName) {
		if(lbl.getTextRenderingIcon() !=  null){
			ScaledHTML.updateRenderer(lbl, "");
		}
		else{
			ScaledHTML.updateRendererOnPropertyChange(lbl, propertyName);
		}
	}

	@Override
	protected void installComponents(JLabel c) {
		ScaledHTML.updateRenderer(c, c.getText());
		c.setInheritsPopupMenu(true);
	}

	public Rectangle getIconR(ZoomableLabel label) {
		layoutZoomed(label);
		return iconR;
	}

	public Rectangle getTextR(ZoomableLabel label) {
		layoutZoomed(label);
		return textR;
	}

	public Rectangle getAvailableTextR(ZoomableLabel label) {
		layoutZoomed(label);
		int availableTextWidth = viewR.width;
		if(iconR.width > 0 && label.getVerticalTextPosition() != SwingConstants.BOTTOM)
			availableTextWidth -= iconR.width + label.getIconTextGap();
		if(availableTextWidth == textR.width)
			return textR;
		Rectangle availableTextR = new Rectangle(textR);
		int horizontalAlignment = label.getEffectiveHorizontalAlignment();
		switch (horizontalAlignment) {
		case SwingConstants.CENTER:
			availableTextR.x -= (availableTextWidth - textR.width)/2;
			break;
		case SwingConstants.RIGHT:
			availableTextR.x -= availableTextWidth - textR.width;
			break;
		}
		availableTextR.width = availableTextWidth;
		return availableTextR;
	}

	private void layoutZoomed(ZoomableLabel label) {
		layoutIgnoringZoom(label);
		final float zoom = label.getZoom();
		if(zoom != 1f) {
			viewR.x = (int)(iconR.x * zoom);
			viewR.y = (int)(iconR.y * zoom);
			viewR.width = (int)(viewR.width * zoom);
			viewR.height = (int)(viewR.height * zoom);
			iconR.x = (int)(iconR.x * zoom);
			iconR.y = (int)(iconR.y * zoom);
			iconR.width = (int)(iconR.width * zoom);
			iconR.height = (int)(iconR.height * zoom);
			textR.x = (int)(textR.x * zoom);
			textR.y = (int)(textR.y * zoom);
			textR.width = (int)(textR.width * zoom);
			textR.height = (int)(textR.height * zoom);
		}
	}

	void layoutIgnoringZoom(ZoomableLabel label) {
		boolean wasPainting = isPainting;
		try{
			isPainting = true;
			iconR.x = iconR.y = iconR.width = iconR.height = 0;
			textR.x = textR.y = textR.width = textR.height = 0;
			String text = label.getText();
			Icon icon = (label.isEnabled()) ? label.getIcon() :
				label.getDisabledIcon();
			layoutCL(label, label.getFontMetrics(), text, icon, viewR, iconR,textR);
		}
		finally{
			isPainting = wasPainting;
		}
	}

	public void preserveLayout(ZoomableLabel zoomableLabel) {
		layoutIgnoringZoom(zoomableLabel);
		zoomableLabel.putClientProperty("preferredLayoutData", new LayoutData(new Rectangle(viewR),
				new Rectangle(textR),
				new Rectangle(iconR)));
	}

	public void releaseLayout(ZoomableLabel zoomableLabel) {
		zoomableLabel.putClientProperty("preferredLayoutData", null);
	}

}
