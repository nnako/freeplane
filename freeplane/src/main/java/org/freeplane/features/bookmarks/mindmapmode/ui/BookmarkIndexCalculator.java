package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JButton;

class BookmarkIndexCalculator {
	private final BookmarkToolbar toolbar;

	BookmarkIndexCalculator(BookmarkToolbar toolbar) {
		this.toolbar = toolbar;
	}

	int calculateBookmarkMoveIndex(int sourceIndex, JButton targetButton, Point dropPoint) {
		if (!(targetButton instanceof BookmarkButton)) {
			return -1; // Invalid for non-BookmarkButton components
		}
		int targetIndex = getBookmarkButtonIndex(targetButton);
		if (targetIndex < 0) {
			return -1; // Invalid if button not found
		}
		boolean movesAfter = isDropAfter(targetButton, dropPoint);

		return movesAfter ? (sourceIndex < targetIndex ? targetIndex : targetIndex + 1)
		        : (sourceIndex < targetIndex ? targetIndex - 1 : targetIndex);
	}

	boolean isValidBookmarkMove(int sourceIndex, JButton targetButton, Point dropPoint) {
		if (!(targetButton instanceof BookmarkButton)) {
			return false; // Invalid for non-BookmarkButton components
		}
		
		Component parent = targetButton.getParent();
		if (!(parent instanceof BookmarkToolbar) || parent != toolbar) {
			return false;
		}

		int targetIndex = getBookmarkButtonIndex(targetButton);
		if (targetIndex < 0 || targetIndex == sourceIndex) {
			return false;
		}

		if (!isInInsertionZone(targetButton, dropPoint)) {
			return false;
		}

		int finalTargetIndex = calculateBookmarkMoveIndex(sourceIndex, targetButton, dropPoint);
		return finalTargetIndex >= 0 && sourceIndex != finalTargetIndex;
	}

	boolean isInInsertionZone(JButton targetButton, Point dropPoint) {
		int buttonWidth = targetButton.getWidth();
		int leftThird = buttonWidth / 3;
		int rightThird = buttonWidth * 2 / 3;
		return dropPoint.x <= leftThird || dropPoint.x >= rightThird;
	}

	boolean isDropAfter(JButton targetButton, Point dropPoint) {
		int rightThird = targetButton.getWidth() * 2 / 3;
		return dropPoint.x >= rightThird;
	}

	ToolbarDropPosition calculateToolbarDropPosition(Point dropPoint) {
		Component[] components = toolbar.getComponents();
		
		// Check buttons to the right (within GAP distance)
		for (Component component : components) {
			if (component instanceof BookmarkButton) {
				BookmarkButton button = (BookmarkButton) component;
				int buttonLeft = button.getX();
				if (dropPoint.x >= buttonLeft - BookmarkToolbar.GAP && dropPoint.x <= buttonLeft + BookmarkToolbar.GAP) {
					int buttonIndex = getBookmarkButtonIndex(component);
					if (buttonIndex >= 0) {
						return new ToolbarDropPosition(ToolbarDropPosition.Type.BEFORE_BUTTON, buttonIndex);
					}
				}
			}
		}
		
		// Check buttons to the left (within GAP distance)
		for (Component component : components) {
			if (component instanceof BookmarkButton) {
				BookmarkButton button = (BookmarkButton) component;
				int buttonRight = button.getX() + button.getWidth();
				if (dropPoint.x >= buttonRight - BookmarkToolbar.GAP && dropPoint.x <= buttonRight + BookmarkToolbar.GAP) {
					int buttonIndex = getBookmarkButtonIndex(component);
					if (buttonIndex >= 0) {
						return new ToolbarDropPosition(ToolbarDropPosition.Type.AFTER_BUTTON, buttonIndex);
					}
				}
			}
		}
		
		// Default: at the end
		return new ToolbarDropPosition(ToolbarDropPosition.Type.AT_END, getBookmarkButtonCount());
	}

	private int getBookmarkButtonIndex(Component component) {
		int bookmarkIndex = 0;
		for (int i = 0; i < toolbar.getComponentCount(); i++) {
			Component comp = toolbar.getComponent(i);
			if (comp == component) {
				return bookmarkIndex;
			}
			if (comp instanceof BookmarkButton) {
				bookmarkIndex++;
			}
		}
		return -1;
	}

	private int getBookmarkButtonCount() {
		int count = 0;
		for (Component component : toolbar.getComponents()) {
			if (component instanceof BookmarkButton) {
				count++;
			}
		}
		return count;
	}

	static class ToolbarDropPosition {
		enum Type { BEFORE_BUTTON, AFTER_BUTTON, AT_END }
		
		final Type type;
		final int buttonIndex;
		
		ToolbarDropPosition(Type type, int buttonIndex) {
			this.type = type;
			this.buttonIndex = buttonIndex;
		}
		
		int getInsertionIndex() {
			switch (type) {
				case BEFORE_BUTTON:
					return buttonIndex;
				case AFTER_BUTTON:
					return buttonIndex + 1;
				case AT_END:
				default:
					return buttonIndex; // Already the total count
			}
		}
	}
}