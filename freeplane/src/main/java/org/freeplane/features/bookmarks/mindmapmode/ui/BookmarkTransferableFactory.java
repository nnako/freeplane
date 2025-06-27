package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.datatransfer.Transferable;
import java.util.Collections;

import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.map.clipboard.MapClipboardController;
import org.freeplane.features.map.clipboard.MapClipboardController.CopiedNodeSet;
import org.freeplane.features.map.clipboard.MindMapNodesSelection;

class BookmarkTransferableFactory {
	
	static BookmarkTransferables.CombinedTransferable createCombinedTransferable(
			NodeBookmark bookmark, int sourceIndex, int dragActionType) {
		
		Transferable bookmarkTransferable = new BookmarkTransferables.BookmarkTransferable(sourceIndex);
		
		final Transferable nodeTransferable = MapClipboardController.getController().copy(
			Collections.singletonList(bookmark.getNode()), CopiedNodeSet.ALL_NODES, CopiedNodeSet.ALL_NODES);
		
		if (nodeTransferable instanceof MindMapNodesSelection) {
			((MindMapNodesSelection) nodeTransferable).setDropAction(dragActionType);
			((MindMapNodesSelection) nodeTransferable).setNodeObjects(Collections.singletonList(bookmark.getNode()), false);
		}
		
		return new BookmarkTransferables.CombinedTransferable(bookmarkTransferable, nodeTransferable);
	}
} 