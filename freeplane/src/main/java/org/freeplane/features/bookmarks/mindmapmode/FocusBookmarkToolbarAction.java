/*
 * Created on 28 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.features.mode.Controller;

public class FocusBookmarkToolbarAction extends AFreeplaneAction {

	private static final long serialVersionUID = 1L;
	public static final String BOOKMARK_TOOLBAR_FOCUS_PROPERTY = "bookmarkToolbarFocus";

	public FocusBookmarkToolbarAction() {
		super("FocusBookmarkToolbarAction");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final JComponent mapViewComponent = Controller.getCurrentController().getMapViewManager().getMapViewComponent();
		if(mapViewComponent != null) {
			mapViewComponent.putClientProperty(BOOKMARK_TOOLBAR_FOCUS_PROPERTY, "switch");
		}
	}

}
