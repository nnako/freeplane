package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.clipboard.MindMapNodesSelection;

class DropExecutor {
	private final BookmarkMover bookmarkMover;
	private final BookmarksController bookmarksController;

	DropExecutor(FreeplaneToolBar toolbar, BookmarksController bookmarksController) {
		this.bookmarkMover = new BookmarkMover(toolbar, bookmarksController);
		this.bookmarksController = bookmarksController;
	}

	void moveBookmark(int sourceIndex, int targetIndex) {
		bookmarkMover.moveBookmark(sourceIndex, targetIndex);
	}

	boolean createBookmarkFromNode(Transferable transferable, NodeBookmark targetBookmark, boolean dropAfter, JButton targetButton) {
		try {
			NodeModel draggedNode = extractSingleNode(transferable);
			if (draggedNode == null) {
				return false;
			}

			MapModel map = getMapFromButton(targetButton);
			int insertionIndex = calculateInsertionIndex(targetBookmark, dropAfter, map);
			
			return bookmarksController.createBookmarkFromNode(draggedNode, map, insertionIndex);

		} catch (Exception e) {
			return false;
		}
	}

	private NodeModel extractSingleNode(Transferable transferable) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<NodeModel> draggedNodesCollection = (Collection<NodeModel>) transferable
		        .getTransferData(MindMapNodesSelection.mindMapNodeObjectsFlavor);
		List<NodeModel> draggedNodes = new ArrayList<>(draggedNodesCollection);
		
		return draggedNodes.size() == 1 ? draggedNodes.get(0) : null;
	}

	private MapModel getMapFromButton(JButton targetButton) {
		FreeplaneToolBar toolbar = (FreeplaneToolBar) targetButton.getParent();
		return (MapModel) toolbar.getClientProperty("bookmarksMap");
	}

	private int calculateInsertionIndex(NodeBookmark targetBookmark, boolean dropAfter, MapModel map) {
		List<NodeBookmark> currentBookmarks = bookmarksController.getBookmarks(map).getBookmarks();
		int targetIndex = bookmarksController.findBookmarkPosition(currentBookmarks, targetBookmark);
		return dropAfter ? targetIndex + 1 : targetIndex;
	}
} 