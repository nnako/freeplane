/*
 * Created on 27 Jan 2024
 *
 * author dimitry
 */
package org.freeplane.core.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import javax.swing.Icon;

import org.freeplane.features.icon.Tag;

public class TagIcon implements Icon {
    private final Tag tag;
    private final Font font;
    private final int width;
    private final int height;
    private final Color tagTextColor;
    private final Color tagBackgroundColor;
    public TagIcon(Tag tag, Font font) {
        this(tag, font, null, null);
    }
    public TagIcon(Tag tag, Font font, Color tagTextColor, Color tagBackgroundColor) {
        super();
        this.tag = tag;
        this.font = font;
        this.tagTextColor = tagTextColor;
        this.tagBackgroundColor = tagBackgroundColor;
        String content = tag.isEmpty() ? "*" : tag.getContent();
        Rectangle2D rect = font.getStringBounds(content , 0, content.length(),
            new FontRenderContext(new AffineTransform(), true, true));
        double textHeight = rect.getHeight();
        width = tag.isEmpty() ? 0 : (int) Math.ceil(rect.getWidth() + textHeight);
        height = (int)  Math.ceil(textHeight * 1.2);
    }

    @Override
    public void paintIcon(Component c, Graphics prototypeGraphics, int x, int y) {
        if (tag.isEmpty())
            return;

        Graphics2D g = (Graphics2D) prototypeGraphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color backgroundColor = tagBackgroundColor != null ? tagBackgroundColor : tag.getColor();
        Color textColor = tagTextColor != null ? tagTextColor : UITools.getTextColorForBackground(backgroundColor);

        g.setColor(backgroundColor);
        int r = (int) (UITools.FONT_SCALE_FACTOR * 10);

        // Define custom shape with rounded right side
        GeneralPath path = new GeneralPath();
        path.moveTo(x, y); // Top-left corner
        path.lineTo(x + width - r, y); // Top edge to the rounded corner
        path.quadTo(x + width, y, x + width, y + r); // Top-right rounded corner
        path.lineTo(x + width, y + height - r); // Right edge
        path.quadTo(x + width, y + height, x + width - r, y + height); // Bottom-right rounded corner
        path.lineTo(x, y + height); // Bottom edge to the flat left
        path.closePath();

        g.fill(path);

        g.setColor(textColor);
        g.draw(path);

        g.setFont(font);
        g.drawString(tag.getContent(), x + height / 4, y + height * 4 / 5);

        g.dispose();
    }
    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    public Tag getTag() {
        return tag;
    }

	@Override
	public String toString() {
		return "TagIcon [tag=" + tag + ", font=" + font + ", width=" + width + ", height=" + height
				+ "]";
	}


}
