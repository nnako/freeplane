package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;

import javax.swing.JButton;

import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;

class BookmarkDragGestureListener implements DragGestureListener {
	private final BookmarkButton button;

	public BookmarkDragGestureListener(BookmarkButton button) {
		this.button = button;
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		BookmarkToolbar toolbar = (BookmarkToolbar) button.getParent();
		NodeBookmark bookmark = button.getBookmark();

		DragActionDetector.DragActionResult actionResult = DragActionDetector.detectDragAction(dge);

		int sourceIndex = toolbar.getComponentIndex(button);
		BookmarkTransferables.CombinedTransferable transferable =
			BookmarkTransferableFactory.createCombinedTransferable(bookmark, sourceIndex, actionResult.dragAction);

		dge.startDrag(actionResult.cursor, transferable);
	}
}