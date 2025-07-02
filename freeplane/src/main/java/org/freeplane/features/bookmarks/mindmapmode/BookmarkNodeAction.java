package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.event.ActionEvent;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.mode.ModeController;

class BookmarkNodeAction extends AFreeplaneAction {
	private static final long serialVersionUID = 1L;

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
		bookmarksController.handleBookmarkSelection(selection);
	}
}