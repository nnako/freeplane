package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Point;

import javax.swing.JButton;

import org.freeplane.core.ui.components.FreeplaneToolBar;

class DropValidator {
	private final BookmarkIndexCalculator indexCalculator;

	DropValidator(BookmarkToolbar toolbar, BookmarksToolbarBuilder toolbarBuilder) {
		this.indexCalculator = new BookmarkIndexCalculator(toolbar, toolbarBuilder);
	}

	DropValidation validateDrop(int sourceIndex, JButton targetButton, Point dropPoint) {
		if (!indexCalculator.isValidBookmarkMove(sourceIndex, targetButton, dropPoint)) {
			return DropValidation.forBookmarkMove(false, -1, false);
		}

		int finalTargetIndex = indexCalculator.calculateBookmarkMoveIndex(sourceIndex, targetButton, dropPoint);
		boolean dropsAfter = indexCalculator.isDropAfter(targetButton, dropPoint);

		return DropValidation.forBookmarkMove(true, finalTargetIndex, dropsAfter);
	}

	DropValidation validateNodeDrop(JButton targetButton, Point dropPoint) {
		boolean isInsertionDrop = indexCalculator.isInInsertionZone(targetButton, dropPoint);
		boolean dropsAfter = indexCalculator.isDropAfter(targetButton, dropPoint);

		return DropValidation.forNodeDrop(true, isInsertionDrop, dropsAfter);
	}
}