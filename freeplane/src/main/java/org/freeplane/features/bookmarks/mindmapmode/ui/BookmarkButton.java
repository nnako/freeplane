package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToolTip;

import org.freeplane.features.map.ITooltipProvider.TooltipTrigger;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;
import org.freeplane.view.swing.map.FreeplaneTooltip;

@SuppressWarnings("serial")
class BookmarkButton extends JButton {
	private final NodeModel node;
	private final ModeController modeController;

	BookmarkButton(NodeModel node, ModeController modeController) {
		this.node = node;
		this.modeController = modeController;
	}

	@Override
	public JToolTip createToolTip() {
		return createBookmarkTooltip();
	}

	@Override
	public String getToolTipText() {
		return modeController.createToolTip(node, this, TooltipTrigger.LINK);
	}

	private FreeplaneTooltip createBookmarkTooltip() {
		FreeplaneTooltip tip = new FreeplaneTooltip(getGraphicsConfiguration(), FreeplaneTooltip.TEXT_HTML, false);
		tip.setBorder(BorderFactory.createEmptyBorder());
		final URL url = node.getMap().getURL();
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
		return node;
	}
}