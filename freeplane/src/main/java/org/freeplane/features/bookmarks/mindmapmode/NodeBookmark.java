/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks.mindmapmode;

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

	public void open(boolean asRoot) {
		final Controller controller = Controller.getCurrentController();
		final IMapViewManager mapViewManager = controller.getMapViewManager();
		final IMapSelection selection = controller.getSelection();
		if(asRoot)
			mapViewManager.setViewRoot(node);
		else if(! selection.isVisible(node))
			return;
		else
			mapViewManager.displayOnCurrentView(node);
		selection.selectAsTheOnlyOneSelected(node);
		selection.scrollNodeTreeToVisible(node, false);
	}

	public void open() {
		if(descriptor.opensAsRoot()) {
			open(true);
		}
		else {
			setViewRoot();
			open(false);
		}
	}

	private void setViewRoot() {
		final Controller controller = Controller.getCurrentController();
		final IMapViewManager mapViewManager = controller.getMapViewManager();
		if(descriptor.opensAsRoot()) {
			mapViewManager.setViewRoot(node);
			return;
		}
		final IMapSelection selection = controller.getSelection();
		MapBookmarks mapBookmarks = node.getMap().getExtension(MapBookmarks.class);
		for(NodeModel node = this.node; selection.getSelectionRoot() != node; node = node.getParentNode()) {
			if(node.isRoot() || mapBookmarks.opensAsRoot(node)) {
				mapViewManager.setViewRoot(node);
				return;
			}

		}

	}
}
