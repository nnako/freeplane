/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.mindmapmode.TagSelection;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeModel.Side;
import org.freeplane.features.map.clipboard.MapClipboardController;
import org.freeplane.features.map.clipboard.MindMapNodesSelection;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.map.mindmapmode.clipboard.MMapClipboardController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MainView.DragOverRelation;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.MapViewIconListComponent;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.ui.MouseEventActor;

public class MNodeDropListener implements DropTargetListener {
private static final int UNFOLD_DELAY_MILLISECONDS = 500;
private Timer timer;

// 	final private ModeController modeController;

	public MNodeDropListener() {
	}


	public void addDropListener(MainView component) {
		addDropListener((Component)component);
	}

	public void addDropListener(MapViewIconListComponent component) {
		addDropListener((Component)component);
	}

	private void addDropListener(Component component) {
		final DropTarget dropTarget = new DropTarget(component, this);
		dropTarget.setActive(true);
	}

	/**
	 * The method is called when the cursor carrying the dragged item enteres
	 * the area of the node. The name "dragEnter" seems to be confusing to me. I
	 * think the difference between dragAcceptable and dropAcceptable is that in
	 * dragAcceptable, you tell if the type of the thing being dragged is OK,
	 * where in dropAcceptable, you tell if your really willing to accept the
	 * item.
	 */
	@Override
	public void dragEnter(final DropTargetDragEvent dtde) {
		if (isDragAcceptable(dtde)) {
			supportFolding(dtde);
			dtde.acceptDrag(DnDConstants.ACTION_MOVE);

		}
		else {
			dtde.rejectDrag();
		}
	}

	private void supportFolding(final DropTargetDragEvent dtde) {
		final MainView node = getMainView(dtde);
		if(isInFoldingRegion(dtde)){
			startUnfoldTimer(node);
		}
		else{
			stopUnfoldTimer();
		}
    }

	private boolean isInFoldingRegion(DropTargetDragEvent dtde) {
	    final MainView node = getMainView(dtde);
	    return node.dragOverRelation(dtde.getLocation()).isChild();
	}

	@Override
	public void dragExit(final DropTargetEvent e) {
	    MainView node = getMainView(e);
	    stopUnfoldTimer();
	    final MainView mainView = node;
	    mainView.stopDragOver();
	    mainView.repaint();
	}

	private MainView getMainView(final DropTargetEvent e) {
	    DropTargetContext dropTargetContext = e.getDropTargetContext();
		return getMainView(dropTargetContext);
    }

	private MainView getMainView(DropTargetContext dropTargetContext) {
		final Component component = dropTargetContext.getComponent();
		if(component instanceof MainView)
			return (MainView) component;
		NodeView nodeView = (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class, component);
		return nodeView.getMainView();
	}

	@Override
	public void dragOver(final DropTargetDragEvent dtde) {
		if(isDragAcceptable(dtde)) {
			supportFolding(dtde);

			if(! dtde.isDataFlavorSupported(TagSelection.tagFlavor)) {
				final MainView dropTarget = getMainView(dtde.getDropTargetContext());
				dropTarget.setDragOverDirection(dtde.getLocation());
			}
		}
	}

	private void startUnfoldTimer(final MainView mainView) {
		if(timer == null){
			timer = new Timer(UNFOLD_DELAY_MILLISECONDS, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
				    if(mainView.isDisplayable()){
				        NodeView nodeView = mainView.getNodeView();
				        if(nodeView.isFolded()) {
				            final NodeModel node = nodeView.getNode();
				            MapView map = nodeView.getMap();
				            if(map.isSelected())
				                map.getModeController().getMapController().unfold(node, map.getFilter());
				            else
				                nodeView.setFolded(false);
				        }
					}
				}
			});
			timer.setRepeats(false);
			timer.start();
		}
    }

	private void stopUnfoldTimer() {
	    if(timer != null){
	    	timer.stop();
	    	timer = null;
	    }

    }

	public void dragScroll(final DropTargetDragEvent e) {
	}

	private boolean isDropAcceptable(final DropTargetDropEvent event, int dropAction) {
		boolean containsTags = event.isDataFlavorSupported(TagSelection.tagFlavor);
		if(event.getDropTargetContext().getComponent() instanceof MapViewIconListComponent && ! containsTags) {
			return false;
		}
		if(containsTags) {
			try {
				NodeView nodeView = getMainView(event.getDropTargetContext()).getNodeView();
				List<Tag> nodeTags = nodeView.getMap().getModeController().getExtension(IconController.class).getTags(nodeView.getNode());
				String tagData = (String) event.getTransferable().getTransferData(TagSelection.tagFlavor);
				Tag tag = TagCategories.readTag(tagData);
				if(nodeTags.contains(tag))
					return false;
			} catch (IOException | UnsupportedFlavorException e) {
				return false;
			}
		}

		if (!event.isLocalTransfer())
			return true;

		if (! event.isDataFlavorSupported(MindMapNodesSelection.mindMapNodeObjectsFlavor))
			 return dropAction != DnDConstants.ACTION_LINK;
		final List<NodeModel> droppedNodes;
		try {
			final Transferable t = event.getTransferable();
			droppedNodes = getNodeObjects(t);
		}
		catch (Exception e) {
			return dropAction != DnDConstants.ACTION_LINK;
		}
		final NodeModel node = getMainView(event.getDropTargetContext()).getNodeView().getNode();
		if (dropAction == DnDConstants.ACTION_LINK) {
			return isFromSameMap(node, droppedNodes);
		}

		if (dropAction == DnDConstants.ACTION_MOVE) {
			return !isFromDescencantNode(node, droppedNodes);
		}
		return ! droppedNodesContainTargetNode(node, droppedNodes);
	}

	private boolean droppedNodesContainTargetNode(final NodeModel targetNode, final List<NodeModel> droppedNodes) {
		for (final NodeModel selected : droppedNodes) {
			if (targetNode == selected)
				return true;
		}
		return false;
	}

	private boolean isFromSameMap(final NodeModel targetNode, final Collection<NodeModel> droppedNodes) {
		for (final NodeModel selected : droppedNodes) {
			if (selected.getMap() != targetNode.getMap())
				return false;
		}
		return true;
	}

	private boolean isFromDescencantNode(final NodeModel targetNode, final List<NodeModel> droppedNodes) {
		for (final NodeModel selected : droppedNodes) {
			if ((targetNode == selected) || targetNode.isDescendantOf(selected))
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private List<NodeModel> getNodeObjects(final Transferable t) throws UnsupportedFlavorException, IOException {
	    return (List<NodeModel>) t.getTransferData(MindMapNodesSelection.mindMapNodeObjectsFlavor);
    }

	@Override
	public void drop(final DropTargetDropEvent dtde) {
		try {
		    stopUnfoldTimer();
			final MainView mainView = getMainView(dtde.getDropTargetContext());
			final NodeView targetNodeView = mainView.getNodeView();
			final MapView mapView = targetNodeView.getMap();
			mapView.select();
			final NodeModel targetNode = targetNodeView.getNode();
			final Controller controller = Controller.getCurrentController();
			int dropAction = getDropAction(dtde);
			final Transferable t = dtde.getTransferable();
			mainView.stopDragOver();
			mainView.repaint();
			if (!isDropAcceptable(dtde, dropAction)) {
				dtde.rejectDrop();
				return;
			}
			DragOverRelation dragOverRelation = dtde.isDataFlavorSupported(TagSelection.tagFlavor )
					? DragOverRelation.CHILD_AFTER
					: mainView.dragOverRelation(dtde.getLocation());
			if(dragOverRelation == DragOverRelation.NOT_AVAILABLE || dragOverRelation == DragOverRelation.SIBLING_AFTER) {
			    dtde.rejectDrop();
			    return;
			}
            final boolean dropAsSibling = dragOverRelation == DragOverRelation.SIBLING_BEFORE;
			ModeController modeController = controller.getModeController();
			final MMapController mapController = (MMapController) modeController.getMapController();
			if ((dropAction == DnDConstants.ACTION_MOVE || dropAction == DnDConstants.ACTION_COPY)) {
				final NodeModel parent = dropAsSibling ? targetNode.getParentNode() : targetNode;
				if (!mapController.isWriteable(parent)) {
					dtde.rejectDrop();
					final String message = TextUtils.getText("node_is_write_protected");
					UITools.errorMessage(message);
					return;
				}
			}
			final boolean isTopOrLeft = dragOverRelation == DragOverRelation.CHILD_BEFORE;
			if (!dtde.isLocalTransfer()) {
				dtde.acceptDrop(DnDConstants.ACTION_COPY);
				Side side = dropAsSibling ? Side.AS_SIBLING : isTopOrLeft ? Side.TOP_OR_LEFT :  Side.BOTTOM_OR_RIGHT;
				((MMapClipboardController) MapClipboardController.getController()).paste(t, targetNode,
						dropAsSibling ? Side.AS_SIBLING : side, dropAction);
				dtde.dropComplete(true);
				return;
			}
			dtde.acceptDrop(dropAction);
			if (dropAction == DnDConstants.ACTION_LINK) {
				int yesorno = JOptionPane.YES_OPTION;
				if (controller.getSelection().size() >= 5) {
					yesorno = JOptionPane.showConfirmDialog(controller.getViewController().getCurrentRootComponent(), TextUtils
					    .getText("lots_of_links_warning"), Integer.toString(controller.getSelection().size())
					        + " links to the same node", JOptionPane.YES_NO_OPTION);
				}
				if (yesorno == JOptionPane.YES_OPTION) {
					for (final NodeModel sourceNodeModel : getNodeObjects(t)) {

						((MLinkController) LinkController.getController(modeController)).addConnector(
						    sourceNodeModel, targetNode);
					}
				}
			}
			else {
				final Collection<NodeModel> selecteds = mapController.getSelectedNodes();
				if (DnDConstants.ACTION_MOVE == dropAction
						&& t.isDataFlavorSupported(MindMapNodesSelection.mindMapNodeObjectsFlavor)
						&& isFromSameMap(targetNode, selecteds)) {
					final NodeModel[] array = selecteds.toArray(new NodeModel[selecteds.size()]);
					moveNodes(mapController, targetNode, t, dropAsSibling, isTopOrLeft);

					if(dropAsSibling || ! targetNodeView.isFolded())
						MouseEventActor.INSTANCE.withMouseEvent( () ->
							controller.getSelection().replaceSelection(array));
					else
						MouseEventActor.INSTANCE.withMouseEvent( () ->
							mapView.selectAsTheOnlyOneSelected(targetNodeView));
				}
				else if (DnDConstants.ACTION_COPY == dropAction || DnDConstants.ACTION_MOVE == dropAction) {
					Side side = dropAsSibling ? Side.AS_SIBLING : isTopOrLeft ? Side.TOP_OR_LEFT :  Side.BOTTOM_OR_RIGHT;
					((MMapClipboardController) MapClipboardController.getController()).paste(t, targetNode,
							dropAsSibling ? Side.AS_SIBLING : side);
					MouseEventActor.INSTANCE.withMouseEvent( () ->
						controller.getSelection().selectAsTheOnlyOneSelected(targetNode));
				}
			}
		}
		catch (final Exception e) {
			LogUtils.severe("Drop exception:", e);
			dtde.dropComplete(false);
			return;
		}
		dtde.dropComplete(true);
	}

	private int getDropAction(final DropTargetDropEvent dtde) throws UnsupportedFlavorException, IOException {
		int dropAction = dtde.getDropAction();
		final Transferable t = dtde.getTransferable();
		if (! dtde.isLocalTransfer()) {
			dropAction = DnDConstants.ACTION_COPY;
		} else if (t.isDataFlavorSupported(MindMapNodesSelection.dropActionFlavor)) {
			dropAction = (Integer) t.getTransferData(MindMapNodesSelection.dropActionFlavor);
		}
		return dropAction;
	}

	private void moveNodes(final MMapController mapController, final NodeModel targetNode, Transferable t,
			final boolean dropAsSibling, final boolean isTopOrLeft) throws UnsupportedFlavorException, IOException{
		final List<NodeModel> movedNodes = getNodeObjects(t);
		MouseEventActor.INSTANCE.withMouseEvent( () -> {
			if (dropAsSibling) {
				mapController.moveNodesBefore(movedNodes, targetNode);
				mapController.setSide(movedNodes, targetNode.getSide());
			}
			else {
				List<NodeModel> nodesChangingParent = movedNodes.stream().filter(node -> targetNode != node.getParentNode()).collect(Collectors.toList());
				mapController.moveNodesAsChildren(movedNodes, targetNode);
				Side side = isTopOrLeft ? Side.TOP_OR_LEFT : Side.BOTTOM_OR_RIGHT;
				mapController.setSide(side == Side.DEFAULT ? nodesChangingParent : movedNodes, side);
			}
		});
	}

	@Override
	public void dropActionChanged(final DropTargetDragEvent e) {
	}

	private boolean isDragAcceptable(final DropTargetDragEvent ev) {
		if(ev.getDropTargetContext().getComponent() instanceof MainView)
			return ev.isDataFlavorSupported(DataFlavor.stringFlavor)
				||ev.isDataFlavorSupported(MindMapNodesSelection.fileListFlavor)
				||ev.isDataFlavorSupported(DataFlavor.imageFlavor);
		else
			return ev.isDataFlavorSupported(TagSelection.tagFlavor);
	}
}
