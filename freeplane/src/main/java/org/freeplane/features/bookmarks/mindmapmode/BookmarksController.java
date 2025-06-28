/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks.mindmapmode;

import java.util.List;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.io.WriteManager;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.features.bookmarks.mindmapmode.ui.BookmarkToolbar;
import org.freeplane.features.icon.IStateIconProvider;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.UIIcon;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.bookmarks.mindmapmode.ui.BookmarksToolbarBuilder;
import org.freeplane.features.filter.condition.CJKNormalizer;
import org.freeplane.features.text.TextController;

public class BookmarksController implements IExtension{
	public static final String SHOW_BOOKMARK_ICONS = "show_bookmark_icons";
	private static final UIIcon bookmarkIcon= IconStoreFactory.ICON_STORE.getUIIcon("node-bookmark.svg");
	private static final UIIcon bookmarkAsRootIcon= IconStoreFactory.ICON_STORE.getUIIcon("node-bookmark-root.svg");
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
		modeController.getExtension(IconController.class).addStateIconProvider(new IStateIconProvider() {
			@Override
			public UIIcon getStateIcon(NodeModel node) {
				final boolean showIcon = ResourceController.getResourceController().getBooleanProperty(SHOW_BOOKMARK_ICONS);
				if(! showIcon)
					return null;
				final NodeBookmark bookmark = getBookmarks(node.getMap()).getBookmark(node.getID());
				return bookmark == null ? null
						: bookmark.opensAsRoot() ? bookmarkAsRootIcon
						: bookmarkIcon;
			}

			@Override
			public boolean mustIncludeInIconRegistry() {
				return true;
			}
		});
		modeController.getMapController().addNodeSelectionListener(new INodeSelectionListener() {

			@Override
			public void onSelect(NodeModel node) {
				node.getMap().getExtension(MapBookmarks.class).onSelect(node);
			}

		});
	}

	public void addBookmark(NodeModel node, NodeBookmarkDescriptor descriptor) {
		final MapModel map = node.getMap();
		getBookmarks(map).add(node.createID(), descriptor);
		fireBookmarksChanged(map);
		fireBookmarkChanged(node);
	}

	private void addBookmarkAtPosition(NodeModel node, NodeBookmarkDescriptor descriptor, int position) {
		final MapModel map = node.getMap();
		getBookmarks(map).addAtPosition(node.createID(), descriptor, position);
		fireBookmarksChanged(map);
		fireBookmarkChanged(node);
	}

	public void removeBookmark(NodeModel node) {
		final MapModel map = node.getMap();
		if(getBookmarks(map).remove(node.getID())) {
			fireBookmarksChanged(map);
			fireBookmarkChanged(node);
		}
	}

	public void removeAllBookmarks(MapModel map) {
		final MapBookmarks bookmarks = getBookmarks(map);
		final List<String> nodeIDs = bookmarks.getNodeIDs();
		if(bookmarks.clear()) {
			fireBookmarksChanged(map);
			nodeIDs.stream().map(map::getNodeForID).forEach(this::fireBookmarkChanged);
		}

	}

	public void moveBookmark(NodeModel node, int index) {
		final MapModel map = node.getMap();
		if(getBookmarks(map).move(node.getID(), index))
			fireBookmarksChanged(map);
	}

	private void fireBookmarksChanged(MapModel map) {
		modeController.getMapController().fireMapChanged(new MapChangeEvent(this, map, MapBookmarks.class, null, null));
	}

	private void fireBookmarkChanged(NodeModel node) {
		modeController.getMapController().nodeRefresh(new NodeChangeEvent(node, NodeBookmark.class, null, null, false, false));
	}

	public MapBookmarks getBookmarks(MapModel map) {
		return MapBookmarks.of(map);
	}

	public void updateBookmarksToolbar(BookmarkToolbar toolbar, MapModel map) {
		final IMapSelection selection = Controller.getCurrentController().getSelection();
		toolbarBuilder.updateBookmarksToolbar(toolbar, map, selection);
	}

	public boolean createBookmarkFromNode(NodeModel draggedNode, MapModel map, int insertionIndex) {
		if (draggedNode == null) {
			return false;
		}

		NodeBookmark existingBookmark = getBookmarks(map).getBookmark(draggedNode.getID());

		if (existingBookmark != null) {
			moveExistingBookmarkToPosition(existingBookmark, insertionIndex, map);
		} else {
			createNewBookmarkAtPosition(draggedNode, insertionIndex);
		}

		return true;
	}

	private void moveExistingBookmarkToPosition(NodeBookmark existingBookmark, int insertionIndex, MapModel map) {
		List<NodeBookmark> currentBookmarks = getBookmarks(map).getBookmarks();
		int currentPosition = findBookmarkPosition(currentBookmarks, existingBookmark);

		if (currentPosition != -1 && currentPosition < insertionIndex) {
			insertionIndex = insertionIndex - 1;
		}

		moveBookmark(existingBookmark.getNode(), insertionIndex);
	}

	private void createNewBookmarkAtPosition(NodeModel draggedNode, int insertionIndex) {
		String bookmarkName = suggestBookmarkNameFromText(draggedNode);
		NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor(bookmarkName, false);

		addBookmarkAtPosition(draggedNode, descriptor, insertionIndex);
	}

	String suggestBookmarkNameFromText(NodeModel node) {
		final String shortText = modeController.getExtension(TextController.class).getShortPlainText(node, 20, "");
		final String plainText = shortText.replaceAll("\\s+\\n", " ");
		final String normalizedText = CJKNormalizer.removeSpacesBetweenCJKCharacters(plainText);
		return normalizedText.isEmpty() ? "Bookmark" : normalizedText;
	}

	public int findBookmarkPosition(List<NodeBookmark> bookmarks, NodeBookmark target) {
		for (int i = 0; i < bookmarks.size(); i++) {
			if (bookmarks.get(i).getNode().getID().equals(target.getNode().getID())) {
				return i;
			}
		}
		return -1;
	}
}
