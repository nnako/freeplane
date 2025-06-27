package org.freeplane.features.bookmarks.mindmapmode;

class DropValidation {
	final boolean isValid;
	final int finalTargetIndex;
	final boolean isInsertionDrop;
	final boolean dropsAfter;

	DropValidation(boolean isValid, int finalTargetIndex, boolean isInsertionDrop, boolean dropsAfter) {
		this.isValid = isValid;
		this.finalTargetIndex = finalTargetIndex;
		this.isInsertionDrop = isInsertionDrop;
		this.dropsAfter = dropsAfter;
	}

	static DropValidation forBookmarkMove(boolean isValid, int finalTargetIndex, boolean dropsAfter) {
		return new DropValidation(isValid, finalTargetIndex, false, dropsAfter);
	}

	static DropValidation forNodeDrop(boolean isValid, boolean isInsertionDrop, boolean dropsAfter) {
		return new DropValidation(isValid, -1, isInsertionDrop, dropsAfter);
	}
} 