package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.KeyboardFocusManager;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
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

	public static class BookmarkSelectionResult {
		public enum Action {
			ADD_BOOKMARKS,
			DELETE_BOOKMARKS,
			CANCEL
		}

		private final Action action;
		private final String bookmarkName;
		private final boolean opensAsRoot;
		private final boolean overwriteNames;

		public BookmarkSelectionResult(Action action) {
			this(action, null, false, false);
		}

		public BookmarkSelectionResult(Action action, String bookmarkName, boolean opensAsRoot, boolean overwriteNames) {
			this.action = action;
			this.bookmarkName = bookmarkName;
			this.opensAsRoot = opensAsRoot;
			this.overwriteNames = overwriteNames;
		}

		public Action getAction() {
			return action;
		}

		public String getBookmarkName() {
			return bookmarkName;
		}

		public boolean opensAsRoot() {
			return opensAsRoot;
		}

		public boolean shouldOverwriteNames() {
			return overwriteNames;
		}
	}

	public BookmarkSelectionResult editBookmarksForSelection(final IMapSelection selection, final MapBookmarks bookmarks, final String suggestedBookmarkName) {
		final boolean isSingleSelection = selection.size() == 1;
		final boolean hasAnyBookmark = hasAnyExistingBookmarks(selection, bookmarks);

		final BookmarkDialogComponents dialogComponents;
		if (isSingleSelection) {
			final NodeModel selectedNode = selection.getSelected();
			final NodeBookmark existingBookmark = bookmarks.getBookmark(selectedNode.getID());
			dialogComponents = createSingleNodeDialogComponents(selectedNode, existingBookmark, suggestedBookmarkName);
		} else {
			dialogComponents = createMultipleNodesDialogComponents(hasAnyBookmark);
		}

		final Object[] options = createDialogOptions(hasAnyBookmark);
		final int result = showBookmarkDialog(dialogComponents, options, "BookmarkNodeAction.text");

		return createBookmarkSelectionResult(result, dialogComponents, isSingleSelection, hasAnyBookmark);
	}

	public NodeBookmarkDescriptor showAddNewNodeDialog() {
		final BookmarkDialogComponents dialogComponents = createNewNodeDialogComponents();
		final Object[] options = createSimpleDialogOptions();
		final int result = showBookmarkDialog(dialogComponents, options, "menu_newNode");

		final int OK_OPTION = 0;
		if (result != OK_OPTION) {
			return null;
		}
		final String content = dialogComponents.getNameInput().getText().trim();
		if (content.isEmpty()) {
			return null;
		}
		return new NodeBookmarkDescriptor(content, dialogComponents.getOpensAsRootCheckBox().isSelected());
	}

	private BookmarkDialogComponents createSingleNodeDialogComponents(final NodeModel node, final NodeBookmark existingBookmark, final String suggestedBookmarkName) {
		final boolean currentOpensAsRoot = existingBookmark != null ? existingBookmark.getDescriptor().opensAsRoot() : false;
		final JCheckBox opensAsRootCheckBox = TranslatedElementFactory.createCheckBox("bookmark.opens_as_root");
		opensAsRootCheckBox.setSelected(currentOpensAsRoot);

		if (node.isRoot()) {
			opensAsRootCheckBox.setEnabled(false);
		}

		final String currentName = existingBookmark != null ? existingBookmark.getDescriptor().getName() : suggestedBookmarkName;
		return createNameAndRootComponents(currentName, opensAsRootCheckBox);
	}

	private BookmarkDialogComponents createMultipleNodesDialogComponents(final boolean hasAnyBookmark) {
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

	private BookmarkDialogComponents createNewNodeDialogComponents() {
		final JCheckBox opensAsRootCheckBox = TranslatedElementFactory.createCheckBox("bookmark.opens_as_root");
		opensAsRootCheckBox.setSelected(true);

		return createNameAndRootComponents("", opensAsRootCheckBox, "bookmark.nodeContent");
	}

	private int showBookmarkDialog(final BookmarkDialogComponents dialogComponents, final Object[] options, final String titleKey) {
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

	private BookmarkSelectionResult createBookmarkSelectionResult(final int result, final BookmarkDialogComponents dialogComponents, final boolean isSingleSelection, final boolean hasAnyBookmark) {
		final int OK_OPTION = 0;
		final int DELETE_OPTION = hasAnyBookmark ? 1 : -1;

		if (result == OK_OPTION) {
			return isSingleSelection ? createSingleSelectionResult(dialogComponents) : createMultipleSelectionResult(dialogComponents);
		}
		
		if (result == DELETE_OPTION) {
			return new BookmarkSelectionResult(BookmarkSelectionResult.Action.DELETE_BOOKMARKS);
		}
		
		return new BookmarkSelectionResult(BookmarkSelectionResult.Action.CANCEL);
	}

	private BookmarkSelectionResult createSingleSelectionResult(final BookmarkDialogComponents dialogComponents) {
		final String bookmarkName = dialogComponents.getNameInput().getText().trim();
		if (bookmarkName.isEmpty()) {
			return new BookmarkSelectionResult(BookmarkSelectionResult.Action.CANCEL);
		}
		final boolean opensAsRoot = dialogComponents.getOpensAsRootCheckBox().isSelected();
		return new BookmarkSelectionResult(BookmarkSelectionResult.Action.ADD_BOOKMARKS, bookmarkName, opensAsRoot, false);
	}

	private BookmarkSelectionResult createMultipleSelectionResult(final BookmarkDialogComponents dialogComponents) {
		final boolean opensAsRoot = dialogComponents.getOpensAsRootCheckBox().isSelected();
		final JCheckBox overwriteNamesCheckBox = dialogComponents.getOverwriteNamesCheckBox();
		final boolean shouldOverwriteNames = overwriteNamesCheckBox != null && overwriteNamesCheckBox.isSelected();
		return new BookmarkSelectionResult(BookmarkSelectionResult.Action.ADD_BOOKMARKS, null, opensAsRoot, shouldOverwriteNames);
	}

	private BookmarkDialogComponents createNameAndRootComponents(final String nameText, final JCheckBox opensAsRootCheckBox) {
		final JTextField nameInput = createNameInput(nameText, "bookmark.name");

		final Box components = Box.createVerticalBox();
		components.add(nameInput);
		components.add(opensAsRootCheckBox);

		return new BookmarkDialogComponents(components, nameInput, opensAsRootCheckBox);
	}

	private BookmarkDialogComponents createNameAndRootComponents(final String nameText, final JCheckBox opensAsRootCheckBox, final String textKey) {
		final JTextField nameInput = createNameInput(nameText, textKey);

		final Box components = Box.createVerticalBox();
		components.add(nameInput);
		components.add(opensAsRootCheckBox);

		return new BookmarkDialogComponents(components, nameInput, opensAsRootCheckBox);
	}

	private JTextField createNameInput(final String initialText, final String textKey) {
		final JTextField nameInput = new JTextField(initialText, 40);
		FocusRequestor.requestFocus(nameInput);
		TranslatedElementFactory.createTitledBorder(nameInput, textKey);
		return nameInput;
	}

	private Object[] createDialogOptions(final boolean hasAnyBookmark) {
		if (hasAnyBookmark) {
			return new Object[] {
				TextUtils.getText("icon_button_ok"),
				TextUtils.getText("delete"),
				TextUtils.getText("cancel")
			};
		} else {
			return createSimpleDialogOptions();
		}
	}

	private Object[] createSimpleDialogOptions() {
		return new Object[] {
			TextUtils.getText("icon_button_ok"),
			TextUtils.getText("cancel")
		};
	}

	private boolean hasAnyExistingBookmarks(final IMapSelection selection, final MapBookmarks bookmarks) {
		return selection.getOrderedSelection().stream()
			.anyMatch(node -> bookmarks.getBookmark(node.getID()) != null);
	}
}