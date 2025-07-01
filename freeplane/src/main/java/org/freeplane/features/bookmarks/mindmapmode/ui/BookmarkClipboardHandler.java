package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
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
	private static final String COPY_ACTION_KEY = "bookmarkCopy";
	private static final String PASTE_ACTION_KEY = "bookmarkPaste";
	private static final String ENTER_ACTION_KEY = "bookmarkEnter";

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

	void setupButtonClipboardActions(BookmarkButton button) {
		InputMap inputMap = button.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = button.getActionMap();

		int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask);
		KeyStroke pasteKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask);
		KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

		inputMap.put(copyKeyStroke, COPY_ACTION_KEY);
		inputMap.put(pasteKeyStroke, PASTE_ACTION_KEY);
		inputMap.put(enterKeyStroke, ENTER_ACTION_KEY);

		actionMap.put(COPY_ACTION_KEY, new ButtonCopyAction(button));
		actionMap.put(PASTE_ACTION_KEY, new ButtonPasteAction(button));
		actionMap.put(ENTER_ACTION_KEY, new ButtonEnterAction(button));
	}

	@SuppressWarnings("serial")
	private class ButtonCopyAction extends AbstractAction {
		private final BookmarkButton button;

		ButtonCopyAction(BookmarkButton button) {
			this.button = button;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			copyBookmark(button);
		}
	}

	@SuppressWarnings("serial")
	private class ButtonPasteAction extends AbstractAction {
		private final BookmarkButton button;

		ButtonPasteAction(BookmarkButton button) {
			this.button = button;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			pasteBookmarkAtButton(button);
		}
	}

	@SuppressWarnings("serial")
	private class ButtonEnterAction extends AbstractAction {
		private final BookmarkButton button;

		ButtonEnterAction(BookmarkButton button) {
			this.button = button;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			button.doClick();
			button.requestFocus();
		}
	}

	@SuppressWarnings("serial")
	private class ToolbarPasteAction extends AbstractAction {
		private final BookmarkToolbar toolbar;

		ToolbarPasteAction(BookmarkToolbar toolbar) {
			this.toolbar = toolbar;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			pasteBookmarkAtEnd(toolbar);
		}
	}

	private void copyBookmark(BookmarkButton button) {
		BookmarkToolbar toolbar = (BookmarkToolbar) button.getParent();
		NodeBookmark bookmark = button.getBookmark();
		int sourceIndex = toolbar.getComponentIndex(button);

		BookmarkTransferables.CombinedTransferable transferable =
			BookmarkTransferableFactory.createCombinedTransferable(bookmark, sourceIndex,
				java.awt.dnd.DnDConstants.ACTION_COPY);

		ClipboardAccessor.getInstance().setClipboardContents(transferable);
	}

	private void pasteBookmarkAtButton(BookmarkButton button) {
		Transferable clipboardContents = ClipboardAccessor.getInstance().getClipboardContents();
		if (clipboardContents == null) {
			return;
		}

		BookmarkToolbar toolbar = (BookmarkToolbar) button.getParent();
		NodeBookmark targetBookmark = button.getBookmark();

		if (clipboardContents.isDataFlavorSupported(BookmarkTransferables.BOOKMARK_FLAVOR)) {
			handleBookmarkPaste(clipboardContents, targetBookmark, false, toolbar);
		} else if (clipboardContents.isDataFlavorSupported(
				org.freeplane.features.map.clipboard.MindMapNodesSelection.mindMapNodeObjectsFlavor)) {
			// Add nodes as children to the bookmarked node instead of creating new bookmarks
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
}