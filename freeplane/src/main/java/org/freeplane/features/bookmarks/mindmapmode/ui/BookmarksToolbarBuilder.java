package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.util.List;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.ModeController;

public class BookmarksToolbarBuilder {
	private static final String BOOKMARKS_MAP_PROPERTY = "bookmarksMap";
	
	private final BookmarksController bookmarksController;
	private final ModeController modeController;
	private final BookmarkButtonConfigurator buttonConfigurator;

	public BookmarksToolbarBuilder(ModeController modeController, BookmarksController bookmarksController) {
		this.modeController = modeController;
		this.bookmarksController = bookmarksController;
		this.buttonConfigurator = new BookmarkButtonConfigurator(bookmarksController, modeController, this);
	}

	public void updateBookmarksToolbar(BookmarkToolbar toolbar, MapModel map, IMapSelection selection) {
		toolbar.removeAll();
		toolbar.putClientProperty(BOOKMARKS_MAP_PROPERTY, map);
		setupToolbarDropTarget(toolbar);

		List<NodeBookmark> bookmarks = bookmarksController.getBookmarks(map).getBookmarks();
		for (NodeBookmark bookmark : bookmarks) {
			final BookmarkButton button = createBookmarkButton(bookmark, toolbar, selection);
			toolbar.add(button);
		}

		toolbar.revalidate();
		toolbar.repaint();
	}

	private void setupToolbarDropTarget(BookmarkToolbar toolbar) {
		if (toolbar.getDropTarget() == null) {
			DropTarget dropTarget = new DropTarget(toolbar,
					DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE,
					new BookmarkDropTargetListener(toolbar, bookmarksController, this));
		}
	}

	private BookmarkButton createBookmarkButton(NodeBookmark bookmark, BookmarkToolbar toolbar, IMapSelection selection) {
		final BookmarkButton button = new BookmarkButton(bookmark.getNode(), modeController);
		buttonConfigurator.configureButton(button, bookmark, toolbar, selection);
		return button;
	}

	int getComponentIndex(BookmarkToolbar toolbar, Component component) {
		Component[] components = toolbar.getComponents();
		for (int i = 0; i < components.length; i++) {
			if (components[i] == component) {
				return i;
			}
		}
		return -1;
	}
}