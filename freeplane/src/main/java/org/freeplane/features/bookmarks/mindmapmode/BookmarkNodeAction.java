package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.components.FocusRequestor;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.condition.CJKNormalizer;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.text.TextController;

class BookmarkNodeAction extends AFreeplaneAction {
	private static final long serialVersionUID = 1L;
	private static final int OK_OPTION = 0;
	private static final int DELETE_OPTION = 1;

	private final ModeController modeController;
	private final BookmarksController bookmarksController;

	public BookmarkNodeAction(final ModeController modeController, final BookmarksController bookmarksController) {
		super("BookmarkNodeAction");
		this.modeController = modeController;
		this.bookmarksController = bookmarksController;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final IMapSelection selection = modeController.getController().getSelection();
		final MapBookmarks bookmarks = getMapBookmarks(selection);
		final NodeBookmark primaryBookmark = getPrimaryBookmark(selection, bookmarks);

		final BookmarkDialogComponents dialogComponents = createDialogComponents(selection, primaryBookmark);
		final Object[] options = createDialogOptions(selection, bookmarks);

		final int result = showBookmarkDialog(dialogComponents, options);
		handleDialogResult(result, selection, dialogComponents, bookmarks);
	}

	private MapBookmarks getMapBookmarks(final IMapSelection selection) {
		final NodeModel selectedNode = selection.getSelected();
		return bookmarksController.getBookmarks(selectedNode.getMap());
	}

	private NodeBookmark getPrimaryBookmark(final IMapSelection selection, final MapBookmarks bookmarks) {
		final NodeModel primaryNode = selection.getSelected();
		return bookmarks.getBookmark(primaryNode.getID());
	}

	private String getCurrentBookmarkName(final NodeModel node, final NodeBookmark existingBookmark) {
		if (existingBookmark != null) {
			return existingBookmark.getDescriptor().getName();
		}

		return suggestBookmarkNameFromText(node);
	}

	private String suggestBookmarkNameFromText(final NodeModel node) {
		final String shortText = modeController.getExtension(TextController.class).getShortPlainText(node, 20, "");
		final String plainText = shortText.replaceAll("\\s+\\n", " ");
		return CJKNormalizer.removeSpacesBetweenCJKCharacters(plainText);
	}

	private BookmarkDialogComponents createDialogComponents(final IMapSelection selection, final NodeBookmark existingBookmark) {
		final boolean currentOpensAsRoot = existingBookmark != null ? existingBookmark.getDescriptor().opensAsRoot() : false;
		final JCheckBox opensAsRootCheckBox = TranslatedElementFactory.createCheckBox("bookmark.opens_as_root");
		opensAsRootCheckBox.setSelected(currentOpensAsRoot);

		if (isSingleSelection(selection)) {
			return createSingleNodeDialog(selection, existingBookmark, opensAsRootCheckBox);
		} else {
			final boolean hasAnyBookmark = hasAnyExistingBookmarks(selection, getMapBookmarks(selection));
			return createMultipleNodesDialog(opensAsRootCheckBox, hasAnyBookmark);
		}
	}

	private boolean isSingleSelection(final IMapSelection selection) {
		return selection.size() == 1;
	}

	private BookmarkDialogComponents createSingleNodeDialog(final IMapSelection selection, final NodeBookmark existingBookmark, final JCheckBox opensAsRootCheckBox) {
		final NodeModel selectedNode = selection.getSelected();

		if (selectedNode.isRoot()) {
			opensAsRootCheckBox.setEnabled(false);
		}

		final String currentName = getCurrentBookmarkName(selectedNode, existingBookmark);
		final JTextField nameInput = new JTextField(currentName, 40);
		FocusRequestor.requestFocus(nameInput);

		final JLabel nameLabel = TranslatedElementFactory.createLabel("bookmark.name");
		final Box components = Box.createVerticalBox();
		components.add(nameLabel);
		components.add(nameInput);
		components.add(opensAsRootCheckBox);

		return new BookmarkDialogComponents(components, nameInput, opensAsRootCheckBox);
	}

	private BookmarkDialogComponents createMultipleNodesDialog(final JCheckBox opensAsRootCheckBox, final boolean hasAnyBookmark) {
		if (hasAnyBookmark) {
			final JCheckBox overwriteNamesCheckBox = TranslatedElementFactory.createCheckBox("bookmark.overwrite_names");
			overwriteNamesCheckBox.setSelected(false);
			final Box components = Box.createVerticalBox();
			components.add(overwriteNamesCheckBox);
			components.add(opensAsRootCheckBox);
			return new BookmarkDialogComponents(components, null, opensAsRootCheckBox, overwriteNamesCheckBox);
		} else {
			return new BookmarkDialogComponents(opensAsRootCheckBox, null, opensAsRootCheckBox, null);
		}
	}

	private Object[] createDialogOptions(final IMapSelection selection, final MapBookmarks bookmarks) {
		final boolean hasAnyBookmark = hasAnyExistingBookmarks(selection, bookmarks);

		if (hasAnyBookmark) {
			return new Object[] {
				TextUtils.getText("icon_button_ok"),
				TextUtils.getText("delete"),
				TextUtils.getText("cancel")
			};
		} else {
			return new Object[] {
				TextUtils.getText("icon_button_ok"),
				TextUtils.getText("cancel")
			};
		}
	}

	private boolean hasAnyExistingBookmarks(final IMapSelection selection, final MapBookmarks bookmarks) {
		return selection.getOrderedSelection().stream()
			.anyMatch(node -> bookmarks.getBookmark(node.getID()) != null);
	}

	private int showBookmarkDialog(final BookmarkDialogComponents dialogComponents, final Object[] options) {
		final String title = TextUtils.getText(getTextKey());
		return JOptionPane.showOptionDialog(
				KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
				dialogComponents.getContainer(),
				title,
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				options,
				options[0]);
	}

	private void handleDialogResult(final int result, final IMapSelection selection, final BookmarkDialogComponents dialogComponents, final MapBookmarks bookmarks) {
		if (result == OK_OPTION) {
			addBookmarks(selection, bookmarks, dialogComponents);
		} else if (result == DELETE_OPTION) {
			removeBookmarks(selection);
		}
	}

	private void removeBookmarks(final IMapSelection selection) {
		for (NodeModel node : selection.getOrderedSelection()) {
			bookmarksController.removeBookmark(node);
		}
	}

	private void addBookmarks(final IMapSelection selection, final MapBookmarks bookmarks, final BookmarkDialogComponents dialogComponents) {
		if (isSingleSelection(selection)) {
			addSingleBookmark(selection.getSelected(), dialogComponents);
		} else {
			addMultipleBookmarks(selection, bookmarks, dialogComponents);
		}
	}

	private void addSingleBookmark(final NodeModel node, final BookmarkDialogComponents dialogComponents) {
		final String bookmarkName = dialogComponents.getNameInput().getText().trim();
		if (!bookmarkName.isEmpty()) {
			final boolean opensAsRoot = dialogComponents.getOpensAsRootCheckBox().isSelected();
			addBookmark(node, bookmarkName, opensAsRoot);
		}
	}

	private void addMultipleBookmarks(final IMapSelection selection, final MapBookmarks bookmarks, final BookmarkDialogComponents dialogComponents) {
		final boolean userWantsOpenAsRoot = dialogComponents.getOpensAsRootCheckBox().isSelected();
		final JCheckBox overwriteNamesCheckBox = dialogComponents.getOverwriteNamesCheckBox();
		final boolean shouldOverwriteNames = overwriteNamesCheckBox != null && overwriteNamesCheckBox.isSelected();

		for (NodeModel node : selection.getOrderedSelection()) {
			final String bookmarkName = getBookmarkName(node, bookmarks, shouldOverwriteNames);
			final boolean opensAsRoot = node.isRoot() || userWantsOpenAsRoot;
			addBookmark(node, bookmarkName, opensAsRoot);
		}
	}

	private String getBookmarkName(final NodeModel node, final MapBookmarks bookmarks, final boolean forceOverwrite) {
		final NodeBookmark existing = bookmarks.getBookmark(node.getID());

		if (forceOverwrite || existing == null) {
			return suggestBookmarkNameFromText(node);
		} else {
			return existing.getDescriptor().getName();
		}
	}

	private void addBookmark(final NodeModel node, final String bookmarkName, final boolean opensAsRoot) {
		final NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor(bookmarkName, opensAsRoot);
		bookmarksController.addBookmark(node, descriptor);
	}

	private static class BookmarkDialogComponents {
		private final JComponent container;
		private final JTextField nameInput;
		private final JCheckBox opensAsRootCheckBox;
		private final JCheckBox overwriteNamesCheckBox;

		public BookmarkDialogComponents(final JComponent container, final JTextField nameInput, final JCheckBox opensAsRootCheckBox) {
			this(container, nameInput, opensAsRootCheckBox, null);
		}

		public BookmarkDialogComponents(final JComponent container, final JTextField nameInput, final JCheckBox opensAsRootCheckBox, final JCheckBox overwriteNamesCheckBox) {
			this.container = container;
			this.nameInput = nameInput;
			this.opensAsRootCheckBox = opensAsRootCheckBox;
			this.overwriteNamesCheckBox = overwriteNamesCheckBox;
		}

		public JComponent getContainer() {
			return container;
		}

		public JTextField getNameInput() {
			return nameInput;
		}

		public JCheckBox getOpensAsRootCheckBox() {
			return opensAsRootCheckBox;
		}

		public JCheckBox getOverwriteNamesCheckBox() {
			return overwriteNamesCheckBox;
		}
	}
}