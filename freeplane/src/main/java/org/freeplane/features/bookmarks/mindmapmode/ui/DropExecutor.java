package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.MapBookmarks;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.clipboard.MindMapNodesSelection;

class DropExecutor {
	private final BookmarksController bookmarksController;
	private final BookmarkToolbar toolbar;

	DropExecutor(BookmarkToolbar toolbar, BookmarksController bookmarksController) {
		this.toolbar = toolbar;
		this.bookmarksController = bookmarksController;
	}

	void moveBookmark(int sourceIndex, int targetIndex) {
		MapModel map = toolbar.getMap();
		MapBookmarks bookmarks = bookmarksController.getBookmarks(map);
		NodeBookmark bookmarkToMove = bookmarks.getBookmarks().get(sourceIndex);
		SwingUtilities.invokeLater(() -> bookmarksController.moveBookmark(bookmarkToMove
		        .getNode(), targetIndex));
	}

	boolean createBookmarkFromNode(Transferable transferable, NodeBookmark targetBookmark, boolean dropAfter, JButton targetButton) {
		try {
			MapModel map = getMapFromButton(targetButton);
			BookmarkToolbar toolbar = (BookmarkToolbar) targetButton.getParent();
			NodeModel draggedNode = extractSingleNode(transferable, toolbar);
			if (draggedNode == null) {
				return false;
			}

			int insertionIndex = calculateInsertionIndex(targetBookmark, dropAfter, map);

			return bookmarksController.createBookmarkFromNode(draggedNode, map, insertionIndex);

		} catch (Exception e) {
			// Ignore exceptions during bookmark creation (e.g., transferable data issues)
			return false;
		}
	}

	boolean createBookmarkFromNodeAtEnd(Transferable transferable, BookmarkToolbar toolbar) {
		try {
			NodeModel draggedNode = extractSingleNode(transferable, toolbar);
			if (draggedNode == null) {
				return false;
			}

			MapModel map = toolbar.getMap();
			List<NodeBookmark> currentBookmarks = bookmarksController.getBookmarks(map).getBookmarks();
			int insertionIndex = currentBookmarks.size();

			return bookmarksController.createBookmarkFromNode(draggedNode, map, insertionIndex);

		} catch (Exception e) {
			return false;
		}
	}

	boolean createBookmarkFromNodeAtPosition(Transferable transferable, BookmarkToolbar toolbar, int insertionIndex) {
		try {
			NodeModel draggedNode = extractSingleNode(transferable, toolbar);
			if (draggedNode == null) {
				return false;
			}

			MapModel map = toolbar.getMap();
			return bookmarksController.createBookmarkFromNode(draggedNode, map, insertionIndex);

		} catch (Exception e) {
			return false;
		}
	}

	boolean createBookmarkFromNodeAtPosition(DropTargetDropEvent dtde, int insertionIndex) {
		try {
			NodeModel draggedNode = extractSingleNode(dtde);
			if (draggedNode == null) {
				return false;
			}

			BookmarkToolbar toolbar = getToolbarFromEvent(dtde);
			MapModel map = toolbar.getMap();
			return bookmarksController.createBookmarkFromNode(draggedNode, map, insertionIndex);

		} catch (Exception e) {
			return false;
		}
	}

	private NodeModel extractSingleNode(DropTargetDropEvent dtde) throws Exception {
		BookmarkToolbar toolbar = getToolbarFromEvent(dtde);
		return extractSingleNode(dtde.getTransferable(), toolbar);
	}

	private BookmarkToolbar getToolbarFromEvent(DropTargetEvent dte) {
		Component component = dte.getDropTargetContext().getComponent();
		if (component instanceof BookmarkToolbar) {
			return (BookmarkToolbar) component;
		} else if (component instanceof BookmarkButton) {
			return (BookmarkToolbar) component.getParent();
		}
		throw new IllegalArgumentException("Event target is neither BookmarkToolbar nor BookmarkButton");
	}

	private NodeModel extractSingleNode(Transferable transferable, BookmarkToolbar toolbar) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<NodeModel> draggedNodesCollection = (Collection<NodeModel>) transferable
		        .getTransferData(MindMapNodesSelection.mindMapNodeObjectsFlavor);
		List<NodeModel> draggedNodes = new ArrayList<>(draggedNodesCollection);

		if (draggedNodes.size() != 1) {
			return null;
		}

		NodeModel draggedNode = draggedNodes.get(0);
		MapModel nodeMap = draggedNode.getMap();
		MapModel toolbarMap = toolbar.getMap();

		return (nodeMap != null && nodeMap.equals(toolbarMap)) ? draggedNode : null;
	}

	private MapModel getMapFromButton(JButton targetButton) {
		BookmarkToolbar toolbar = (BookmarkToolbar) targetButton.getParent();
		return toolbar.getMap();
	}

	private int calculateInsertionIndex(NodeBookmark targetBookmark, boolean dropAfter, MapModel map) {
		List<NodeBookmark> currentBookmarks = bookmarksController.getBookmarks(map).getBookmarks();
		int targetIndex = bookmarksController.findBookmarkPosition(currentBookmarks, targetBookmark);
		return dropAfter ? targetIndex + 1 : targetIndex;
	}
}