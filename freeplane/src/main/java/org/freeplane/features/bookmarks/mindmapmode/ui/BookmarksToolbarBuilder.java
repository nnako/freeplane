package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.List;

import javax.swing.JButton;

import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.ModeController;

public class BookmarksToolbarBuilder {
	private final BookmarksController bookmarksController;
	private final ModeController modeController;
	private final BookmarkButtonConfigurator buttonConfigurator;

	public BookmarksToolbarBuilder(ModeController modeController, BookmarksController bookmarksController) {
		this.modeController = modeController;
		this.bookmarksController = bookmarksController;
		this.buttonConfigurator = new BookmarkButtonConfigurator(bookmarksController, modeController);
	}

	public void updateBookmarksToolbar(BookmarkToolbar toolbar, MapModel map, IMapSelection selection) {
		final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
		int focusIndex = -1;
		if(focusOwner != null && focusOwner.getParent() == toolbar) {
			focusIndex = toolbar.getComponentIndex(focusOwner);
		}

		toolbar.removeAll();

		List<NodeBookmark> bookmarks = bookmarksController.getBookmarks(map).getBookmarks();
		for (NodeBookmark bookmark : bookmarks) {
			final BookmarkButton button = createBookmarkButton(bookmark, toolbar, selection);
			toolbar.add(button);
			button.setFocusable(true);
		}

		JButton addRootBranchButton = TranslatedElementFactory.createButtonWithIcon("bookmark.addRootBranch.icon", "bookmark.addRootBranch.text");
		addRootBranchButton.setFocusable(true);
		addRootBranchButton.addActionListener(e -> bookmarksController.addNewNode(map.getRootNode()));

		final int buttonCount = toolbar.getComponentCount();

		toolbar.addSeparator();
		toolbar.add(addRootBranchButton);

		for(int i = 0; i < 2; i++) {
			final Component component = toolbar.getComponent(buttonCount + i);
			buttonConfigurator.configureNonBookmarkComponent(component);
		}

		if(focusIndex >= 0) {
			final int componentCount = buttonCount;
			if(componentCount > focusIndex) {
				Component component = toolbar.getComponent(focusIndex);
				if (component.isFocusable()) {
					component.requestFocusInWindow();
				}
			}
			else {
				toolbar.requestFocusInWindow();
			}
		}
		toolbar.revalidate();
		toolbar.repaint();
	}

	private BookmarkButton createBookmarkButton(NodeBookmark bookmark, BookmarkToolbar toolbar, IMapSelection selection) {
		final BookmarkButton button = new BookmarkButton(bookmark, modeController);
		buttonConfigurator.configureButton(button, bookmark, toolbar, selection);
		return button;
	}


}