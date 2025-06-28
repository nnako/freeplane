package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JButton;

import org.freeplane.core.ui.components.FreeplaneToolBar;

class BookmarkIndexCalculator {
	private final BookmarkToolbar toolbar;
	private final BookmarksToolbarBuilder toolbarBuilder;

	BookmarkIndexCalculator(BookmarkToolbar toolbar, BookmarksToolbarBuilder toolbarBuilder) {
		this.toolbar = toolbar;
		this.toolbarBuilder = toolbarBuilder;
	}

	int calculateBookmarkMoveIndex(int sourceIndex, JButton targetButton, Point dropPoint) {
		int targetIndex = toolbarBuilder.getComponentIndex(toolbar, targetButton);
		boolean movesAfter = isDropAfter(targetButton, dropPoint);

		return movesAfter ? (sourceIndex < targetIndex ? targetIndex : targetIndex + 1)
		        : (sourceIndex < targetIndex ? targetIndex - 1 : targetIndex);
	}

	boolean isValidBookmarkMove(int sourceIndex, JButton targetButton, Point dropPoint) {
		BookmarkToolbar targetToolbar = (BookmarkToolbar) targetButton.getParent();
		if (targetToolbar != toolbar) {
			return false;
		}

		int targetIndex = toolbarBuilder.getComponentIndex(targetToolbar, targetButton);
		if (targetIndex == sourceIndex) {
			return false;
		}

		if (!isInInsertionZone(targetButton, dropPoint)) {
			return false;
		}

		int finalTargetIndex = calculateBookmarkMoveIndex(sourceIndex, targetButton, dropPoint);
		return sourceIndex != finalTargetIndex;
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
					int buttonIndex = toolbarBuilder.getComponentIndex(toolbar, component);
					return new ToolbarDropPosition(ToolbarDropPosition.Type.BEFORE_BUTTON, buttonIndex);
				}
			}
		}
		
		// Check buttons to the left (within GAP distance)
		for (Component component : components) {
			if (component instanceof BookmarkButton) {
				BookmarkButton button = (BookmarkButton) component;
				int buttonRight = button.getX() + button.getWidth();
				if (dropPoint.x >= buttonRight - BookmarkToolbar.GAP && dropPoint.x <= buttonRight + BookmarkToolbar.GAP) {
					int buttonIndex = toolbarBuilder.getComponentIndex(toolbar, component);
					return new ToolbarDropPosition(ToolbarDropPosition.Type.AFTER_BUTTON, buttonIndex);
				}
			}
		}
		
		// Default: at the end
		return new ToolbarDropPosition(ToolbarDropPosition.Type.AT_END, components.length);
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