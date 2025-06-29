package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.KeyboardFocusManager;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.freeplane.core.ui.components.FocusRequestor;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmarkDescriptor;

class BookmarkPopupMenu extends JPopupMenu {
	private static final int RENAME_TEXTFIELD_WIDTH = 40;

	private final BookmarksController bookmarksController;

	BookmarkPopupMenu(NodeBookmark bookmark, BookmarksController bookmarksController) {
		this.bookmarksController = bookmarksController;
		buildMenu(bookmark);
	}

	private void buildMenu(NodeBookmark bookmark) {
		addGotoNodeMenuItem(bookmark);
		addOpenAsRootDirectMenuItem(bookmark);
		addSeparator();
		addRemoveMenuItem(bookmark);
		addRenameMenuItem(bookmark);
		addOpenAsRootToggleMenuItem(bookmark);
	}

	private void addGotoNodeMenuItem(NodeBookmark bookmark) {
		JMenuItem selectItem = TranslatedElementFactory.createMenuItem("bookmark.goto_node");
		selectItem.addActionListener(e -> bookmark.open(false));
		add(selectItem);
	}

	private void addOpenAsRootDirectMenuItem(NodeBookmark bookmark) {
		JMenuItem openAsRootDirectItem = TranslatedElementFactory.createMenuItem("bookmark.open_as_root");
		openAsRootDirectItem.addActionListener(e -> bookmark.open(true));
		add(openAsRootDirectItem);
	}

	private void addRemoveMenuItem(NodeBookmark bookmark) {
		JMenuItem removeItem = TranslatedElementFactory.createMenuItem("bookmark.delete");
		removeItem.addActionListener(e -> bookmarksController.removeBookmark(bookmark.getNode()));
		add(removeItem);
	}

	private void addRenameMenuItem(NodeBookmark bookmark) {
		JMenuItem renameItem = TranslatedElementFactory.createMenuItem("bookmark.rename");
		renameItem.addActionListener(e -> showRenameDialog(bookmark));
		add(renameItem);
	}

	private void addOpenAsRootToggleMenuItem(NodeBookmark bookmark) {
		JCheckBoxMenuItem openAsRootItem = TranslatedElementFactory.createCheckboxMenuItem("bookmark.opens_as_root");
		openAsRootItem.setSelected(bookmark.getDescriptor().opensAsRoot());
		openAsRootItem.addActionListener(e -> toggleOpenAsRoot(bookmark));

		if (bookmark.getNode().isRoot()) {
			openAsRootItem.setEnabled(false);
		}
		add(openAsRootItem);
	}

	private void showRenameDialog(NodeBookmark bookmark) {
		final String currentName = bookmark.getDescriptor().getName();
		final boolean currentOpensAsRoot = bookmark.getDescriptor().opensAsRoot();

		final String title = TextUtils.getText("bookmark.rename");
		final JTextField nameInput = new JTextField(currentName, RENAME_TEXTFIELD_WIDTH);
		FocusRequestor.requestFocus(nameInput);

		if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
				KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
				nameInput,
				title,
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE)) {

			final String bookmarkName = nameInput.getText().trim();
			if (!bookmarkName.isEmpty()) {
				final NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor(bookmarkName, currentOpensAsRoot);
				bookmarksController.addBookmark(bookmark.getNode(), descriptor);
			}
		}
	}

	private void toggleOpenAsRoot(NodeBookmark bookmark) {
		boolean newOpensAsRoot = !bookmark.getDescriptor().opensAsRoot();
		NodeBookmarkDescriptor newDescriptor = new NodeBookmarkDescriptor(
			bookmark.getDescriptor().getName(),
			newOpensAsRoot
		);
		bookmarksController.addBookmark(bookmark.getNode(), newDescriptor);
	}
}