package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.Component;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.clipboard.ClipboardAccessor;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeModel.Side;
import org.freeplane.features.map.clipboard.MapClipboardController;
import org.freeplane.features.map.mindmapmode.clipboard.MMapClipboardController;

class BookmarkClipboardHandler {
	private static final String PASTE_ACTION_KEY = "bookmarkPaste";

	private final BookmarksController bookmarksController;
	private final DropExecutor dropExecutor;

	BookmarkClipboardHandler(BookmarksController bookmarksController, DropExecutor dropExecutor) {
		this.bookmarksController = bookmarksController;
		this.dropExecutor = dropExecutor;
	}

	void setupToolbarClipboardActions(BookmarkToolbar toolbar) {
		InputMap inputMap = toolbar.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = toolbar.getActionMap();

		int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		KeyStroke pasteKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask);
		inputMap.put(pasteKeyStroke, PASTE_ACTION_KEY);
		actionMap.put(PASTE_ACTION_KEY, new ToolbarPasteAction(toolbar));
	}

	void copyBookmark(BookmarkButton button) {
		Component parent = button.getParent();
		if (!(parent instanceof BookmarkToolbar)) {
			return;
		}
		BookmarkToolbar toolbar = (BookmarkToolbar) parent;
		NodeBookmark bookmark = button.getBookmark();
		int sourceIndex = toolbar.getComponentIndex(button);

		BookmarkTransferables.CombinedTransferable transferable =
			BookmarkTransferableFactory.createCombinedTransferable(bookmark, sourceIndex,
				java.awt.dnd.DnDConstants.ACTION_COPY);

		ClipboardAccessor.getInstance().setClipboardContents(transferable);
	}

	void pasteBookmarkAtButton(BookmarkButton button) {
		Transferable clipboardContents = ClipboardAccessor.getInstance().getClipboardContents();
		if (clipboardContents == null) {
			return;
		}

		Component parent = button.getParent();
		if (!(parent instanceof BookmarkToolbar)) {
			return;
		}
		BookmarkToolbar toolbar = (BookmarkToolbar) parent;
		NodeBookmark targetBookmark = button.getBookmark();

		if (clipboardContents.isDataFlavorSupported(BookmarkTransferables.BOOKMARK_FLAVOR)) {
			handleBookmarkPaste(clipboardContents, targetBookmark, false, toolbar);
		} else if (clipboardContents.isDataFlavorSupported(
				org.freeplane.features.map.clipboard.MindMapNodesSelection.mindMapNodeObjectsFlavor)) {
			NodeModel targetNode = targetBookmark.getNode();
			((MMapClipboardController) MapClipboardController.getController()).paste(clipboardContents, targetNode, Side.BOTTOM_OR_RIGHT);
		}
	}

	private void pasteBookmarkAtEnd(BookmarkToolbar toolbar) {
		Transferable clipboardContents = ClipboardAccessor.getInstance().getClipboardContents();
		if (clipboardContents == null) {
			return;
		}

		if (clipboardContents.isDataFlavorSupported(BookmarkTransferables.BOOKMARK_FLAVOR)) {
			handleBookmarkPasteAtEnd(clipboardContents, toolbar);
		} else if (clipboardContents.isDataFlavorSupported(
				org.freeplane.features.map.clipboard.MindMapNodesSelection.mindMapNodeObjectsFlavor)) {
			dropExecutor.createBookmarkFromNodeAtEnd(clipboardContents, toolbar);
		}
	}

	private void handleBookmarkPaste(Transferable transferable, NodeBookmark targetBookmark,
			boolean pasteAfter, BookmarkToolbar toolbar) {
		try {
			int sourceIndex = (Integer) transferable.getTransferData(BookmarkTransferables.BOOKMARK_FLAVOR);
			MapModel map = toolbar.getMap();
			int targetIndex = bookmarksController.findBookmarkPosition(
				bookmarksController.getBookmarks(map).getBookmarks(), targetBookmark);
			int insertionIndex = pasteAfter ? targetIndex + 1 : targetIndex;

			dropExecutor.moveBookmark(sourceIndex, insertionIndex);
		} catch (Exception e) {
			// Handle paste error silently
		}
	}

	private void handleBookmarkPasteAtEnd(Transferable transferable, BookmarkToolbar toolbar) {
		try {
			int sourceIndex = (Integer) transferable.getTransferData(BookmarkTransferables.BOOKMARK_FLAVOR);
			MapModel map = toolbar.getMap();
			int insertionIndex = bookmarksController.getBookmarks(map).getBookmarks().size();

			dropExecutor.moveBookmark(sourceIndex, insertionIndex);
		} catch (Exception e) {
			// Handle paste error silently
		}
	}

	@SuppressWarnings("serial")
	private class ToolbarPasteAction extends javax.swing.AbstractAction {
		private final BookmarkToolbar toolbar;

		ToolbarPasteAction(BookmarkToolbar toolbar) {
			this.toolbar = toolbar;
		}

		@Override
		public void actionPerformed(java.awt.event.ActionEvent e) {
			pasteBookmarkAtEnd(toolbar);
		}
	}
}