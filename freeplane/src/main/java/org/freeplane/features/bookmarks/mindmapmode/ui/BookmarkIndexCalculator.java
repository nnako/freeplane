package org.freeplane.features.bookmarks.mindmapmode.ui;

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
}