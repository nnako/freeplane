package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.Point;
import java.util.List;

import javax.swing.JButton;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.features.map.MapModel;

class BookmarkIndexCalculator {
	private final FreeplaneToolBar toolbar;
	private final BookmarksToolbarBuilder toolbarBuilder;
	private final BookmarksController bookmarksController;

	BookmarkIndexCalculator(FreeplaneToolBar toolbar, BookmarksToolbarBuilder toolbarBuilder, BookmarksController bookmarksController) {
		this.toolbar = toolbar;
		this.toolbarBuilder = toolbarBuilder;
		this.bookmarksController = bookmarksController;
	}

	int calculateBookmarkMoveIndex(int sourceIndex, JButton targetButton, Point dropPoint) {
		int targetIndex = toolbarBuilder.getComponentIndex(toolbar, targetButton);
		boolean movesAfter = isDropAfter(targetButton, dropPoint);
		
		return movesAfter ? (sourceIndex < targetIndex ? targetIndex : targetIndex + 1)
		        : (sourceIndex < targetIndex ? targetIndex - 1 : targetIndex);
	}

	int calculateNodeInsertionIndex(NodeBookmark targetBookmark, boolean dropAfter, MapModel map) {
		List<NodeBookmark> currentBookmarks = bookmarksController.getBookmarks(map).getBookmarks();
		int targetIndex = bookmarksController.findBookmarkPosition(currentBookmarks, targetBookmark);
		return dropAfter ? targetIndex + 1 : targetIndex;
	}

	boolean isValidBookmarkMove(int sourceIndex, JButton targetButton, Point dropPoint) {
		FreeplaneToolBar targetToolbar = (FreeplaneToolBar) targetButton.getParent();
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