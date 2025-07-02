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

		buttonConfigurator.configureNonBookmarkComponent(addRootBranchButton);

		toolbar.addSeparator();
		toolbar.add(addRootBranchButton);

		if(focusIndex >= 0) {
			final int componentCount = toolbar.getComponentCount();
			if(componentCount > focusIndex) {
				Component component = toolbar.getComponent(focusIndex);
				if (component.isFocusable()) {
					component.requestFocusInWindow();
				}
			}
			else if(componentCount > 0) {
				// Find the last focusable component
				for (int i = componentCount - 1; i >= 0; i--) {
					Component component = toolbar.getComponent(i);
					if (component.isFocusable()) {
						component.requestFocusInWindow();
						break;
					}
				}
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