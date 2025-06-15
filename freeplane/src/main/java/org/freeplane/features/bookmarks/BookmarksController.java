/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks;

import javax.swing.JButton;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.io.WriteManager;
import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;

public class BookmarksController implements IExtension{
	private final ModeController modeController;

	public BookmarksController(ModeController modeController) {
		super();
		this.modeController = modeController;
		final MapController mapController = modeController.getMapController();
		final ReadManager readManager = mapController.getReadManager();
		final WriteManager writeManager = mapController.getWriteManager();
		final BookmarksBuilder bookmarksBuilder = new BookmarksBuilder();
		readManager.addElementHandler("bookmarks", bookmarksBuilder);
		readManager.addElementHandler("bookmark", bookmarksBuilder);
		writeManager.addExtensionElementWriter(MapBookmarks.class, bookmarksBuilder);
	}

	public void addBookmark(NodeModel node, NodeBookmarkDescriptor descriptor) {
		if(getBookmarks(node.getMap()).add(node.getID(), descriptor))
			fireBookmarksChanged();
	}

	public void removeBookmark(NodeModel node) {
		if(getBookmarks(node.getMap()).remove(node.getID()))
			fireBookmarksChanged();
	}

	public void moveBookmark(NodeModel node, int index) {
		if(getBookmarks(node.getMap()).move(node.getID(), index))
			fireBookmarksChanged();
	}

	private void fireBookmarksChanged() {
		modeController.getMapController().fireMapChanged(new MapChangeEvent(this, MapBookmarks.class, null, null));
	}

	public MapBookmarks getBookmarks(MapModel map) {
		return MapBookmarks.of(map);
	}

	public void updateBookmarksToolbar(FreeplaneToolBar toolbar, MapModel map) {
		toolbar.removeAll();
		MapBookmarks bookmarks = getBookmarks(map);
		for (NodeBookmark bookmark : bookmarks.getBookmarks()) {
			final JButton button = new JButton();
			button.setText(bookmark.getDescriptor().getName());
			button.addActionListener(action -> bookmark.open());
			toolbar.add(button);
		}
	}

}
