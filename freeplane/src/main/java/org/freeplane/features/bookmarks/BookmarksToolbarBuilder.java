package org.freeplane.features.bookmarks;

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.freeplane.core.ui.components.FocusRequestor;
import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.ModeController;

public class BookmarksToolbarBuilder {
	private final BookmarksController bookmarksController;

	public BookmarksToolbarBuilder(BookmarksController bookmarksController) {
		this.bookmarksController = bookmarksController;
	}

	public void updateBookmarksToolbar(FreeplaneToolBar toolbar, MapModel map) {
		toolbar.removeAll();
		MapBookmarks bookmarks = bookmarksController.getBookmarks(map);
		for (NodeBookmark bookmark : bookmarks.getBookmarks()) {
			final JButton button = createBookmarkButton(bookmark, map);
			toolbar.add(button);
		}
	}

	private JButton createBookmarkButton(NodeBookmark bookmark, MapModel map) {
		final JButton button = new JButton();
		button.setText(bookmark.getDescriptor().getName());
		button.addActionListener(action -> bookmark.open());

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					showBookmarkPopupMenu(e, bookmark, button);
				}
			}
		});

		return button;
	}

	private void showBookmarkPopupMenu(MouseEvent e, NodeBookmark bookmark, JButton button) {
		JPopupMenu popup = new JPopupMenu();
		final boolean isRootNode = bookmark.getNode().isRoot();
		if(! isRootNode) {
			JMenuItem removeItem = TranslatedElementFactory.createMenuItem("delete");
			removeItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					bookmarksController.removeBookmark(bookmark.getNode());
				}
			});
			popup.add(removeItem);
		}
		JMenuItem renameItem = TranslatedElementFactory.createMenuItem("rename");
		renameItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				showRenameDialog(bookmark);
			}
		});
		popup.add(renameItem);

		JCheckBoxMenuItem openAsRootItem = new JCheckBoxMenuItem(TextUtils.getText("opens_as_root"));
		openAsRootItem.setSelected(bookmark.getDescriptor().opensAsRoot());
		openAsRootItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				toggleOpenAsRoot(bookmark);
			}
		});
		if(isRootNode)
			openAsRootItem.setEnabled(false);
		popup.add(openAsRootItem);

		popup.show(button, e.getX(), e.getY());
	}

	private void showRenameDialog(NodeBookmark bookmark) {
		final String currentName = bookmark.getDescriptor().getName();
		final boolean currentOpensAsRoot = bookmark.getDescriptor().opensAsRoot();

		final String title = TextUtils.getText("rename");
		final JLabel nameLabel = TranslatedElementFactory.createLabel("bookmark_name");
		final JTextField nameInput = new JTextField(currentName, 40);
		FocusRequestor.requestFocus(nameInput);
		final JCheckBox opensAsRootCheckBox = TranslatedElementFactory.createCheckBox("opens_as_root");
		opensAsRootCheckBox.setSelected(currentOpensAsRoot);

		Box components = Box.createVerticalBox();
		components.add(nameLabel);
		components.add(nameInput);
		components.add(opensAsRootCheckBox);

		if(JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
				KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
				components,
				title,
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE)) {

			final String bookmarkName = nameInput.getText().trim();
			if (!bookmarkName.isEmpty()) {
				bookmarksController.removeBookmark(bookmark.getNode());
				final NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor(bookmarkName, opensAsRootCheckBox.isSelected());
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
		bookmarksController.removeBookmark(bookmark.getNode());
		bookmarksController.addBookmark(bookmark.getNode(), newDescriptor);
	}
}