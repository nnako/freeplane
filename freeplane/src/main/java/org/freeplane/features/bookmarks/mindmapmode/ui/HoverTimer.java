package org.freeplane.features.bookmarks.mindmapmode.ui;

import javax.swing.JButton;
import javax.swing.Timer;

import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;

class HoverTimer {
	private Timer hoverTimer;
	private static final int HOVER_DELAY_MS = 2000;

	HoverTimer() {
	}

	void startHoverTimer(BookmarkButton targetButton) {
		cancelHoverTimer();
		NodeBookmark bookmark = targetButton.getBookmark();

		hoverTimer = new Timer(HOVER_DELAY_MS, e -> {
			bookmark.open();
			targetButton.showFeedback(BookmarkToolbar.DropIndicatorType.NAVIGATE_FEEDBACK);
		});
		hoverTimer.setRepeats(false);
		hoverTimer.start();
	}

	void cancelHoverTimer() {
		if (hoverTimer != null) {
			hoverTimer.stop();
			hoverTimer = null;
		}
	}
}