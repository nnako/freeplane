package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;

import javax.swing.JButton;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;

class BookmarkDragGestureListener implements DragGestureListener {
	private final JButton button;
	private final BookmarksToolbarBuilder toolbarBuilder;

	public BookmarkDragGestureListener(JButton button, BookmarksToolbarBuilder toolbarBuilder) {
		this.button = button;
		this.toolbarBuilder = toolbarBuilder;
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		BookmarkToolbar toolbar = (BookmarkToolbar) button.getParent();
		NodeBookmark bookmark = (NodeBookmark) button.getClientProperty("bookmark");

		DragActionDetector.DragActionResult actionResult = DragActionDetector.detectDragAction(dge);

		int sourceIndex = toolbarBuilder.getComponentIndex(toolbar, button);
		BookmarkTransferables.CombinedTransferable transferable =
			BookmarkTransferableFactory.createCombinedTransferable(bookmark, sourceIndex, actionResult.dragAction);

		dge.startDrag(actionResult.cursor, transferable);
	}
}