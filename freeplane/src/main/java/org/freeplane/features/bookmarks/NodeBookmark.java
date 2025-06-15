/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks;

import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.ui.IMapViewManager;

public class NodeBookmark {
	private final NodeModel node;
	private final NodeBookmarkDescriptor descriptor;

	public NodeBookmark(NodeModel node, NodeBookmarkDescriptor descriptor) {
		super();
		this.node = node;
		this.descriptor = descriptor;
	}

	public NodeModel getNode() {
		return node;
	}

	public NodeBookmarkDescriptor getDescriptor() {
		return descriptor;
	}

	public void open() {
		final Controller controller = Controller.getCurrentController();
		final IMapViewManager mapViewManager = controller.getMapViewManager();
		if(descriptor.opensAsRoot())
			mapViewManager.setViewRoot(node);
		else
			mapViewManager.displayOnCurrentView(node);
		final IMapSelection selection = controller.getSelection();
		selection.selectAsTheOnlyOneSelected(node);
		selection.scrollNodeTreeToVisible(node);
	}
}
