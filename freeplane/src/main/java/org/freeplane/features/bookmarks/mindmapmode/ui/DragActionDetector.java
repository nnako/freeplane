package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Cursor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragSource;

class DragActionDetector {

	static class DragActionResult {
		final int dragAction;
		final Cursor cursor;

		private DragActionResult(int dragAction, Cursor cursor) {
			this.dragAction = dragAction;
			this.cursor = cursor;
		}
	}

	static DragActionResult detectDragAction(DragGestureEvent dge) {
		int dragActionType = dge.getDragAction();
		java.awt.Cursor cursor = getCursorByAction(dragActionType);

		if ((dge.getTriggerEvent().getModifiersEx() & java.awt.event.InputEvent.BUTTON2_DOWN_MASK) != 0) {
			cursor = DragSource.DefaultCopyDrop;
			dragActionType = DnDConstants.ACTION_COPY;
		}
		else if (isLinkDragEvent(dge)) {
			cursor = DragSource.DefaultLinkDrop;
			dragActionType = DnDConstants.ACTION_LINK;
		}

		return new DragActionResult(dragActionType, cursor);
	}

	private static boolean isLinkDragEvent(final DragGestureEvent e) {
	    return (e.getTriggerEvent().getModifiersEx() & java.awt.event.InputEvent.BUTTON3_DOWN_MASK) != 0;
    }

	private static java.awt.Cursor getCursorByAction(final int dragAction) {
		switch (dragAction) {
			case DnDConstants.ACTION_COPY:
				return DragSource.DefaultCopyDrop;
			case DnDConstants.ACTION_LINK:
				return DragSource.DefaultLinkDrop;
			default:
				return DragSource.DefaultMoveDrop;
		}
	}
}