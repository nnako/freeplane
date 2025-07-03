/*
 * Created on 15 Jun 2025
 *
 * author dimitry
 */
package org.freeplane.features.bookmarks.mindmapmode;

import org.freeplane.features.filter.Filter;
import org.freeplane.features.filter.FilterController;
import org.freeplane.features.filter.hidden.NodeVisibility;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.ui.IMapViewManager;

public class NodeBookmark {
	private final NodeModel node;
	private final NodeBookmarkDescriptor descriptor;

	NodeBookmark(NodeModel node, NodeBookmarkDescriptor descriptor) {
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
		final NodeModel selectedNode;
		if(asRoot) {
			final MapBookmarks bookmarks = selection.getMap().getExtension(MapBookmarks.class);
			final NodeModel lastSelectedNode = bookmarks.getSelectedNodeForRoot(node);

			if (selection.getSelectionRoot() != node) {
				mapViewManager.setViewRoot(node);
				selectedNode = lastSelectedNode;
			}
			else
				selectedNode = node;
		}
		else
			selectedNode = node;
		if(! NodeVisibility.isHidden(selectedNode)){
			if(selection.getSelectionRoot() != selectedNode
					&& ! selectedNode.isDescendantOf(selection.getSelectionRoot())) {
				mapViewManager.setViewRoot(node.getMap().getRootNode());
			}
			final Filter filter = selection.getFilter();
			if(! selectedNode.isVisible(filter)) {
				FilterController.getController(controller).applyNoFiltering(node.getMap());
			}
			controller.getModeController().getMapController().displayNode(selectedNode);
			if(selectedNode.isRoot()){
				selection.selectRoot();
			}
			else {
				selection.selectAsTheOnlyOneSelected(selectedNode);
				selection.scrollNodeTreeToVisible(selectedNode, false);
			}
		}
	}

	public void open() {
		open(opensAsRoot());
	}

	public String getName() {
		return descriptor.getName();
	}

	boolean opensAsRoot() {
		return descriptor.opensAsRoot();
	}

	public void openAsNewView() {
		final Controller controller = Controller.getCurrentController();
		final IMapViewManager mapViewManager = controller.getMapViewManager();
		mapViewManager.newMapView(node.getMap(), controller.getModeController());
		open(true);
	}

	public void alternativeOpen() {
		open(!opensAsRoot());
	}
}
