package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.KeyboardFocusManager;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.freeplane.core.ui.components.FocusRequestor;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;

class BookmarkEditor {

	public static class BookmarkDialogComponents {
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

	private final BookmarksController controller;

	public BookmarkEditor(final BookmarksController controller) {
		this.controller = controller;
	}

	public void handleBookmarkSelection(final IMapSelection selection) {
		final MapBookmarks bookmarks = controller.getBookmarks(selection.getSelected().getMap());
		final boolean isSingleSelection = selection.size() == 1;
		final boolean hasAnyBookmark = hasAnyExistingBookmarks(selection);

		final BookmarkDialogComponents dialogComponents;
		if (isSingleSelection) {
			final NodeModel selectedNode = selection.getSelected();
			final NodeBookmark existingBookmark = bookmarks.getBookmark(selectedNode.getID());
			dialogComponents = createSingleNodeDialogComponents(selectedNode, existingBookmark);
		} else {
			dialogComponents = createMultipleNodesDialogComponents(hasAnyBookmark);
		}

		final Object[] options = createDialogOptions(hasAnyBookmark);
		final int result = showBookmarkDialog(dialogComponents, options, "BookmarkNodeAction");

		handleBookmarkDialogResult(result, selection, dialogComponents, bookmarks, isSingleSelection, hasAnyBookmark);
	}

	public BookmarkDialogComponents createSingleNodeDialogComponents(final NodeModel node, final NodeBookmark existingBookmark) {
		final boolean currentOpensAsRoot = existingBookmark != null ? existingBookmark.getDescriptor().opensAsRoot() : false;
		final JCheckBox opensAsRootCheckBox = TranslatedElementFactory.createCheckBox("bookmark.opens_as_root");
		opensAsRootCheckBox.setSelected(currentOpensAsRoot);

		if (node.isRoot()) {
			opensAsRootCheckBox.setEnabled(false);
		}

		final String currentName = existingBookmark != null ? existingBookmark.getDescriptor().getName() : controller.suggestBookmarkNameFromText(node);
		final JTextField nameInput = new JTextField(currentName, 40);
		FocusRequestor.requestFocus(nameInput);

		final JLabel nameLabel = TranslatedElementFactory.createLabel("bookmark.name");
		final Box components = Box.createVerticalBox();
		components.add(nameLabel);
		components.add(nameInput);
		components.add(opensAsRootCheckBox);

		return new BookmarkDialogComponents(components, nameInput, opensAsRootCheckBox);
	}

	public BookmarkDialogComponents createMultipleNodesDialogComponents(final boolean hasAnyBookmark) {
		final JCheckBox opensAsRootCheckBox = TranslatedElementFactory.createCheckBox("bookmark.opens_as_root");
		opensAsRootCheckBox.setSelected(false);

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

	public Object[] createDialogOptions(final boolean hasAnyBookmark) {
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

	public int showBookmarkDialog(final BookmarkDialogComponents dialogComponents, final Object[] options, final String titleKey) {
		final String title = TextUtils.getText(titleKey);
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

	public boolean hasAnyExistingBookmarks(final IMapSelection selection) {
		final MapBookmarks bookmarks = controller.getBookmarks(selection.getSelected().getMap());
		return selection.getOrderedSelection().stream()
			.anyMatch(node -> bookmarks.getBookmark(node.getID()) != null);
	}

	private void handleBookmarkDialogResult(final int result, final IMapSelection selection, final BookmarkDialogComponents dialogComponents, final MapBookmarks bookmarks, final boolean isSingleSelection, final boolean hasAnyBookmark) {
		final int OK_OPTION = 0;
		final int DELETE_OPTION = hasAnyBookmark ? 1 : -1;

		if (result == OK_OPTION) {
			if (isSingleSelection) {
				addSingleBookmark(selection.getSelected(), dialogComponents);
			} else {
				addMultipleBookmarks(selection, bookmarks, dialogComponents);
			}
		} else if (result == DELETE_OPTION) {
			for (NodeModel node : selection.getOrderedSelection()) {
				controller.removeBookmark(node);
			}
		}
	}

	private void addSingleBookmark(final NodeModel node, final BookmarkDialogComponents dialogComponents) {
		final String bookmarkName = dialogComponents.getNameInput().getText().trim();
		if (!bookmarkName.isEmpty()) {
			final boolean opensAsRoot = dialogComponents.getOpensAsRootCheckBox().isSelected();
			final NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor(bookmarkName, opensAsRoot);
			controller.addBookmark(node, descriptor);
		}
	}

	private void addMultipleBookmarks(final IMapSelection selection, final MapBookmarks bookmarks, final BookmarkDialogComponents dialogComponents) {
		final boolean userWantsOpenAsRoot = dialogComponents.getOpensAsRootCheckBox().isSelected();
		final JCheckBox overwriteNamesCheckBox = dialogComponents.getOverwriteNamesCheckBox();
		final boolean shouldOverwriteNames = overwriteNamesCheckBox != null && overwriteNamesCheckBox.isSelected();

		for (NodeModel node : selection.getOrderedSelection()) {
			final String bookmarkName = getBookmarkName(node, bookmarks, shouldOverwriteNames);
			final boolean opensAsRoot = node.isRoot() || userWantsOpenAsRoot;
			final NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor(bookmarkName, opensAsRoot);
			controller.addBookmark(node, descriptor);
		}
	}

	private String getBookmarkName(final NodeModel node, final MapBookmarks bookmarks, final boolean forceOverwrite) {
		final NodeBookmark existing = bookmarks.getBookmark(node.getID());

		if (forceOverwrite || existing == null) {
			return controller.suggestBookmarkNameFromText(node);
		} else {
			return existing.getDescriptor().getName();
		}
	}
} 