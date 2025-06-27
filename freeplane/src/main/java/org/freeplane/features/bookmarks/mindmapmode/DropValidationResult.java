package org.freeplane.features.bookmarks.mindmapmode;

class DropValidationResult {
	static class DropValidation {
		final boolean isValid;
		final int finalTargetIndex;
		final boolean movesAfter;

		DropValidation(boolean isValid, int finalTargetIndex, boolean movesAfter) {
			this.isValid = isValid;
			this.finalTargetIndex = finalTargetIndex;
			this.movesAfter = movesAfter;
		}
	}

	static class NodeDropValidation {
		final boolean isValid;
		final boolean isInsertionDrop;
		final boolean dropsAfter;

		NodeDropValidation(boolean isValid, boolean isInsertionDrop, boolean dropsAfter) {
			this.isValid = isValid;
			this.isInsertionDrop = isInsertionDrop;
			this.dropsAfter = dropsAfter;
		}
	}
} 