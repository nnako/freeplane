package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.components.FocusRequestor;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.condition.CJKNormalizer;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.text.TextController;

class BookmarkNodeAction extends AFreeplaneAction {
	private static final long serialVersionUID = 1L;
	private ModeController modeController;

	public BookmarkNodeAction(final ModeController modeController) {
		super("BookmarkNodeAction");
		this.modeController = modeController;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final NodeModel node = modeController.getMapController().getSelectedNode();
		final BookmarksController bookmarksController = modeController.getExtension(BookmarksController.class);
		final MapBookmarks bookmarks = bookmarksController.getBookmarks(node.getMap());
		final NodeBookmark existingBookmark = bookmarks.getBookmark(node.getID());

		final String currentName;
		if (existingBookmark != null) {
			currentName = existingBookmark.getDescriptor().getName();
		} else {
			final String shortText = modeController.getExtension(TextController.class).getShortPlainText(node, 20, "");
			String plainText = shortText.replaceAll("\\s+\\n", " ");
			String singleLine = CJKNormalizer.removeSpacesBetweenCJKCharacters(plainText);
			currentName = singleLine;
		}

		final boolean currentOpensAsRoot = existingBookmark != null ? existingBookmark.getDescriptor().opensAsRoot() : false;

		final String title = TextUtils.getText(getTextKey());
		final JLabel nameLabel = TranslatedElementFactory.createLabel("bookmark_name");
		final JTextField nameInput = new JTextField(currentName, 40);
		FocusRequestor.requestFocus(nameInput);
		JCheckBox opensAsRootCheckBox = TranslatedElementFactory.createCheckBox("bookmark.opens_as_root");
		opensAsRootCheckBox.setSelected(currentOpensAsRoot);
		if(node.isRoot())
			opensAsRootCheckBox.setEnabled(false);

		Box components = Box.createVerticalBox();
		components.add(nameLabel);
		components.add(nameInput);
		components.add(opensAsRootCheckBox);

		Object[] options;
		if (existingBookmark != null) {
			options = new Object[] {
				TextUtils.getText("icon_button_ok"),
				TextUtils.getText("delete"),
				TextUtils.getText("cancel")
			};
		} else {
			options = new Object[] {
				TextUtils.getText("icon_button_ok"),
				TextUtils.getText("cancel")
			};
		}

		int result = JOptionPane.showOptionDialog(
				KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
				components,
				title,
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				options,
				options[0]);

		if (result == 0) {
			final String bookmarkName = nameInput.getText().trim();
			if (!bookmarkName.isEmpty()) {
				if (existingBookmark != null) {
					bookmarksController.removeBookmark(node);
				}
				final NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor(bookmarkName, opensAsRootCheckBox.isSelected());
				bookmarksController.addBookmark(node, descriptor);
			}
		} else if (result == 1 && existingBookmark != null && !node.isRoot()) {
			bookmarksController.removeBookmark(node);
		}
	}
}