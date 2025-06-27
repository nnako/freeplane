package org.freeplane.features.bookmarks.mindmapmode;

import javax.swing.SwingUtilities;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.features.map.MapModel;

class BookmarkMover {
	private final FreeplaneToolBar toolbar;
	private final BookmarksController bookmarksController;

	BookmarkMover(FreeplaneToolBar toolbar, BookmarksController bookmarksController) {
		this.toolbar = toolbar;
		this.bookmarksController = bookmarksController;
	}

	void moveBookmark(int sourceIndex, int targetIndex) {
		MapModel map = (MapModel) toolbar.getClientProperty("bookmarksMap");
		MapBookmarks bookmarks = bookmarksController.getBookmarks(map);
		NodeBookmark bookmarkToMove = bookmarks.getBookmarks().get(sourceIndex);
		SwingUtilities.invokeLater(() -> bookmarksController.moveBookmark(bookmarkToMove
		        .getNode(), targetIndex));
	}
} 