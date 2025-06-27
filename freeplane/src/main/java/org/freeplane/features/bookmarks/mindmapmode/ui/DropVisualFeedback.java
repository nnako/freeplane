package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;

class DropVisualFeedback {
	private Border originalBorder;

	void saveOriginalBorder(JButton button) {
		originalBorder = button.getBorder();
	}

	void showDropZoneIndicator(JButton button, boolean dropAfter) {
		Color highlightColor = button.getForeground();

		if (dropAfter) {
			button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(
			        0, 0, 0, 3, highlightColor), originalBorder));
		} else {
			button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(
			        0, 3, 0, 0, highlightColor), originalBorder));
		}
	}

	void showNodeDropZoneIndicator(JButton button, boolean dropAfter) {
		Color highlightColor = button.getForeground();

		if (dropAfter) {
			button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(
			        0, 0, 0, 3, highlightColor), originalBorder));
		} else {
			button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(
			        0, 3, 0, 0, highlightColor), originalBorder));
		}
	}

	void showHoverFeedback(JButton button) {
		Color hoverColor = button.getForeground();
		button.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 3, 0, 3, hoverColor), originalBorder));
	}

	void showNavigatedFeedback(JButton button) {
		Color navigatedColor = button.getForeground();
		button.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 3, 0, 3, navigatedColor), originalBorder));
	}

	void clearVisualFeedback(JButton button) {
		if (originalBorder != null) {
			button.setBorder(originalBorder);
		}
	}
}