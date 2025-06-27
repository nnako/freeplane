package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.Point;

import javax.swing.JButton;

import org.freeplane.core.ui.components.FreeplaneToolBar;

class DropValidator {
	private final BookmarkIndexCalculator indexCalculator;

	DropValidator(FreeplaneToolBar toolbar, BookmarksToolbarBuilder toolbarBuilder, BookmarksController bookmarksController) {
		this.indexCalculator = new BookmarkIndexCalculator(toolbar, toolbarBuilder, bookmarksController);
	}

	DropValidationResult.DropValidation validateDrop(int sourceIndex, JButton targetButton, Point dropPoint) {
		if (!indexCalculator.isValidBookmarkMove(sourceIndex, targetButton, dropPoint)) {
			return new DropValidationResult.DropValidation(false, -1, false);
		}

		int finalTargetIndex = indexCalculator.calculateBookmarkMoveIndex(sourceIndex, targetButton, dropPoint);
		boolean movesAfter = indexCalculator.isDropAfter(targetButton, dropPoint);
		
		return new DropValidationResult.DropValidation(true, finalTargetIndex, movesAfter);
	}

	DropValidationResult.NodeDropValidation validateNodeDrop(JButton targetButton, Point dropPoint) {
		boolean isInsertionDrop = indexCalculator.isInInsertionZone(targetButton, dropPoint);
		boolean dropsAfter = indexCalculator.isDropAfter(targetButton, dropPoint);
		
		return new DropValidationResult.NodeDropValidation(true, isInsertionDrop, dropsAfter);
	}
} 