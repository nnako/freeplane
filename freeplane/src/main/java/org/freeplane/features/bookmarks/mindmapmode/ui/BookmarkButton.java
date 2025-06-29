package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToolTip;

import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.map.ITooltipProvider.TooltipTrigger;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;
import org.freeplane.view.swing.map.FreeplaneTooltip;

@SuppressWarnings("serial")
class BookmarkButton extends JButton {
	private final NodeBookmark bookmark;
	public NodeBookmark getBookmark() {
		return bookmark;
	}

	private final ModeController modeController;

	BookmarkButton(NodeBookmark bookmark, ModeController modeController) {
		this.bookmark = bookmark;
		this.modeController = modeController;
		setOpaque(false);
	}

	@Override
	public JToolTip createToolTip() {
		return createBookmarkTooltip();
	}

	@Override
	public String getToolTipText() {
		return modeController.createToolTip(bookmark.getNode(), this, TooltipTrigger.LINK);
	}

	private FreeplaneTooltip createBookmarkTooltip() {
		FreeplaneTooltip tip = new FreeplaneTooltip(getGraphicsConfiguration(), FreeplaneTooltip.TEXT_HTML, false);
		tip.setBorder(BorderFactory.createEmptyBorder());
		final URL url = bookmark.getNode().getMap().getURL();
		if (url != null) {
			tip.setBase(url);
		} else {
			try {
				tip.setBase(new URL("file: "));
			} catch (MalformedURLException e) {
			}
		}
		return tip;
	}

	NodeModel getNode() {
		return bookmark.getNode();
	}

	public void clearVisualFeedback() {
		BookmarkToolbar toolbar = getBookmarkToolbar();
		if (toolbar != null) {
			toolbar.clearVisualFeedback();
		}
	}

	public void showFeedback(BookmarkToolbar.DropIndicatorType type) {
		BookmarkToolbar toolbar = getBookmarkToolbar();
		if (toolbar != null) {
			toolbar.showVisualFeedback(this, type);
		}
	}

	public void showDropZoneIndicator(boolean dropsAfter) {
		BookmarkToolbar.DropIndicatorType type = dropsAfter ?
			BookmarkToolbar.DropIndicatorType.DROP_AFTER :
			BookmarkToolbar.DropIndicatorType.DROP_BEFORE;
		showFeedback(type);
	}

	private BookmarkToolbar getBookmarkToolbar() {
		return (BookmarkToolbar) getParent();
	}
}