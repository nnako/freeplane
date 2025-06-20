/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks.mindmapmode;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.io.WriteManager;
import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;

public class BookmarksController implements IExtension{
	private final ModeController modeController;
	private final BookmarksToolbarBuilder toolbarBuilder;

	public BookmarksController(ModeController modeController) {
		super();
		this.modeController = modeController;
		this.toolbarBuilder = new BookmarksToolbarBuilder(modeController, this);
		final MapController mapController = modeController.getMapController();
		final ReadManager readManager = mapController.getReadManager();
		final WriteManager writeManager = mapController.getWriteManager();
		final BookmarksBuilder bookmarksBuilder = new BookmarksBuilder();
		readManager.addElementHandler("bookmarks", bookmarksBuilder);
		readManager.addElementHandler("bookmark", bookmarksBuilder);
		writeManager.addExtensionElementWriter(MapBookmarks.class, bookmarksBuilder);
		modeController.addAction(new BookmarkNodeAction(modeController, this));
	}

	public void addBookmark(NodeModel node, NodeBookmarkDescriptor descriptor) {
		final MapModel map = node.getMap();
		getBookmarks(map).add(node.getID(), descriptor);
		fireBookmarksChanged(map);
	}

	public void removeBookmark(NodeModel node) {
		final MapModel map = node.getMap();
		if(getBookmarks(map).remove(node.getID()))
			fireBookmarksChanged(map);
	}

	public void removeAllBookmarks(MapModel map) {
		if(getBookmarks(map).clear())
			fireBookmarksChanged(map);

	}

	public void moveBookmark(NodeModel node, int index) {
		final MapModel map = node.getMap();
		if(getBookmarks(map).move(node.getID(), index))
			fireBookmarksChanged(map);
	}

	private void fireBookmarksChanged(MapModel map) {
		modeController.getMapController().fireMapChanged(new MapChangeEvent(this, map, MapBookmarks.class, null, null));
	}

	public MapBookmarks getBookmarks(MapModel map) {
		return MapBookmarks.of(map);
	}

	public void updateBookmarksToolbar(FreeplaneToolBar toolbar, MapModel map) {
		final IMapSelection selection = Controller.getCurrentController().getSelection();
		toolbarBuilder.updateBookmarksToolbar(toolbar, map, selection);
	}

}
