package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;

import javax.swing.JButton;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;

class BookmarkDragGestureListener implements DragGestureListener {
	private final JButton button;

	public BookmarkDragGestureListener(JButton button) {
		this.button = button;
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		BookmarkToolbar toolbar = (BookmarkToolbar) button.getParent();
		NodeBookmark bookmark = (NodeBookmark) button.getClientProperty("bookmark");

		DragActionDetector.DragActionResult actionResult = DragActionDetector.detectDragAction(dge);

		int sourceIndex = toolbar.getComponentIndex(button);
		BookmarkTransferables.CombinedTransferable transferable =
			BookmarkTransferableFactory.createCombinedTransferable(bookmark, sourceIndex, actionResult.dragAction);

		dge.startDrag(actionResult.cursor, transferable);
	}
}