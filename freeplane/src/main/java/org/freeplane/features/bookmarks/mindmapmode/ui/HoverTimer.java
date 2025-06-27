package org.freeplane.features.bookmarks.mindmapmode.ui;

import javax.swing.JButton;
import javax.swing.Timer;

import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;

class HoverTimer {
	private Timer hoverTimer;
	private static final int HOVER_DELAY_MS = 2000;
	private final DropVisualFeedback visualFeedback;

	HoverTimer(DropVisualFeedback visualFeedback) {
		this.visualFeedback = visualFeedback;
	}

	void startHoverTimer(JButton targetButton) {
		cancelHoverTimer();
		NodeBookmark bookmark = (NodeBookmark) targetButton.getClientProperty("bookmark");

		hoverTimer = new Timer(HOVER_DELAY_MS, e -> {
			bookmark.open();
			visualFeedback.showNavigatedFeedback(targetButton);
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