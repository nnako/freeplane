/*
 * Created on 18 May 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.text.AttributedCharacterIterator;

public class InputMethodBuffer implements InputMethodRequests {
	private final Component owner;


	public InputMethodBuffer(Component owner) {
		super();
		this.owner = owner;
	}

	@Override
    public Rectangle getTextLocation(TextHitInfo offset) {
    	Point p = owner.getLocationOnScreen();
    	return new Rectangle(p.x, p.y, 3, owner.getFontMetrics(owner.getFont()).getMaxAscent());
    }

    @Override
    public TextHitInfo getLocationOffset(int x, int y) {
        return TextHitInfo.leading(0);
    }

    @Override
    public int getInsertPositionOffset() {
        return 0;
    }

    @Override
    public AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex, AttributedCharacterIterator.Attribute[] attributes) {
        return MainView.EMPTY_ATTRIBUTED_STRING.getIterator();
    }

    @Override
    public int getCommittedTextLength() {
        return 0;
    }

    @Override
    public AttributedCharacterIterator cancelLatestCommittedText(AttributedCharacterIterator.Attribute[] attributes) {
        return MainView.EMPTY_ATTRIBUTED_STRING.getIterator();
    }

    @Override
    public AttributedCharacterIterator getSelectedText(AttributedCharacterIterator.Attribute[] attributes) {
        return MainView.EMPTY_ATTRIBUTED_STRING.getIterator();
    }
}