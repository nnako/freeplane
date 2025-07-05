package org.freeplane.view.swing.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

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
import org.freeplane.features.map.mindmapmode.InsertionRelation;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.map.mindmapmode.clipboard.MMapClipboardController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.view.swing.map.MapViewIconListComponent;

public class NodeDropUtils {

	public static boolean isDragAcceptable(final DropTargetDragEvent event, NodeModel targetNode) {
		boolean containsTags = event.isDataFlavorSupported(TagSelection.tagFlavor);
		if(containsTags) {
			try {
				List<Tag> nodeTags = Controller.getCurrentModeController().getExtension(IconController.class).getTags(targetNode);
				String tagData = (String) event.getTransferable().getTransferData(TagSelection.tagFlavor);
				Tag tag = TagCategories.readTag(tagData);
				if(nodeTags.contains(tag))
					return false;
			} catch (IOException | UnsupportedFlavorException e) {
				return false;
			}
		}
		if(event.getDropTargetContext().getComponent() instanceof MapViewIconListComponent && !containsTags)
			return containsTags;
		else
			return event.isDataFlavorSupported(DataFlavor.stringFlavor)
				||event.isDataFlavorSupported(MindMapNodesSelection.fileListFlavor)
				||event.isDataFlavorSupported(DataFlavor.imageFlavor)
				||containsTags;
	}

	public static boolean isDragAcceptableForGenericTarget(final DropTargetDragEvent event, NodeModel targetNode) {
		boolean containsTags = event.isDataFlavorSupported(TagSelection.tagFlavor);
		if(containsTags) {
			try {
				List<Tag> nodeTags = Controller.getCurrentModeController().getExtension(IconController.class).getTags(targetNode);
				String tagData = (String) event.getTransferable().getTransferData(TagSelection.tagFlavor);
				Tag tag = TagCategories.readTag(tagData);
				if(nodeTags.contains(tag))
					return false;
			} catch (IOException | UnsupportedFlavorException e) {
				return false;
			}
		}
		return event.isDataFlavorSupported(DataFlavor.stringFlavor)
			||event.isDataFlavorSupported(MindMapNodesSelection.fileListFlavor)
			||event.isDataFlavorSupported(DataFlavor.imageFlavor)
			||containsTags;
	}

	public static boolean isDropAcceptable(final DropTargetDropEvent event, NodeModel targetNode, int dropAction) {
		boolean containsTags = event.isDataFlavorSupported(TagSelection.tagFlavor);
		if(event.getDropTargetContext().getComponent() instanceof MapViewIconListComponent && ! containsTags) {
			return false;
		}
		if (!event.isLocalTransfer())
			return true;
		if(containsTags) {
			try {
				List<Tag> nodeTags = Controller.getCurrentModeController().getExtension(IconController.class).getTags(targetNode);
				String tagData = (String) event.getTransferable().getTransferData(TagSelection.tagFlavor);
				Tag tag = TagCategories.readTag(tagData);
				if(nodeTags.contains(tag))
					return false;
			} catch (IOException | UnsupportedFlavorException e) {
				return false;
			}
		}

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
		if (dropAction == DnDConstants.ACTION_LINK) {
			return areFromSameMap(targetNode, droppedNodes);
		}

		if (dropAction == DnDConstants.ACTION_MOVE) {
			return !isFromDescendantNode(targetNode, droppedNodes);
		}
		return !droppedNodesContainTargetNode(targetNode, droppedNodes);
	}

	public static boolean isDropAcceptableForGenericTarget(final DropTargetDropEvent event, NodeModel targetNode, int dropAction) {
		boolean containsTags = event.isDataFlavorSupported(TagSelection.tagFlavor);
		if (!event.isLocalTransfer())
			return true;
		if(containsTags) {
			try {
				List<Tag> nodeTags = Controller.getCurrentModeController().getExtension(IconController.class).getTags(targetNode);
				String tagData = (String) event.getTransferable().getTransferData(TagSelection.tagFlavor);
				Tag tag = TagCategories.readTag(tagData);
				if(nodeTags.contains(tag))
					return false;
			} catch (IOException | UnsupportedFlavorException e) {
				return false;
			}
		}

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
		if (dropAction == DnDConstants.ACTION_LINK) {
			return areFromSameMap(targetNode, droppedNodes);
		}

		if (dropAction == DnDConstants.ACTION_MOVE) {
			return !isFromDescendantNode(targetNode, droppedNodes);
		}
		return !droppedNodesContainTargetNode(targetNode, droppedNodes);
	}

	public static boolean isDropAcceptable(Transferable transferable, NodeModel targetNode, int dropAction) {
		if (!transferable.isDataFlavorSupported(MindMapNodesSelection.mindMapNodeObjectsFlavor))
			return dropAction != DnDConstants.ACTION_LINK;

		try {
			final List<NodeModel> droppedNodes = getNodeObjects(transferable);

			if (dropAction == DnDConstants.ACTION_LINK) {
				return areFromSameMap(targetNode, droppedNodes);
			}

			if (dropAction == DnDConstants.ACTION_MOVE) {
				return !isFromDescendantNode(targetNode, droppedNodes);
			}
			return !droppedNodesContainTargetNode(targetNode, droppedNodes);
		} catch (Exception e) {
			return dropAction != DnDConstants.ACTION_LINK;
		}
	}

	public static boolean droppedNodesContainTargetNode(final NodeModel targetNode, final List<NodeModel> droppedNodes) {
		for (final NodeModel selected : droppedNodes) {
			if (targetNode == selected)
				return true;
		}
		return false;
	}

	public static boolean areFromSameMap(final NodeModel targetNode, final Collection<NodeModel> droppedNodes) {
		for (final NodeModel selected : droppedNodes) {
			if (selected.getMap() != targetNode.getMap())
				return false;
		}
		return true;
	}

	public static boolean areFromSameMap(Transferable transferable, NodeModel targetNode) throws UnsupportedFlavorException, IOException {
		final List<NodeModel> draggedNodes = getNodeObjects(transferable);
		return areFromSameMap(targetNode, draggedNodes);
	}

	public static boolean isFromDescendantNode(final NodeModel targetNode, final List<NodeModel> droppedNodes) {
		for (final NodeModel selected : droppedNodes) {
			if ((targetNode == selected) || targetNode.isDescendantOf(selected))
				return true;
		}
		return false;
	}

	// Legacy method name with typo for compatibility
	public static boolean isFromDescencantNode(final NodeModel targetNode, final List<NodeModel> droppedNodes) {
		return isFromDescendantNode(targetNode, droppedNodes);
	}

	@SuppressWarnings("unchecked")
	public static List<NodeModel> getNodeObjects(final Transferable t) throws UnsupportedFlavorException, IOException {
		return (List<NodeModel>) t.getTransferData(MindMapNodesSelection.mindMapNodeObjectsFlavor);
	}

	public static int getDropAction(final Transferable t, int defaultDropAction) {
		int dropAction = defaultDropAction;

		if (t.isDataFlavorSupported(MindMapNodesSelection.dropCopyActionFlavor)) {
			dropAction = DnDConstants.ACTION_COPY;
		} else if (t.isDataFlavorSupported(MindMapNodesSelection.dropLinkActionFlavor)) {
			dropAction = DnDConstants.ACTION_LINK;
		}

		return dropAction;
	}

	public static int getDropAction(final DropTargetDropEvent dtde) {
		return getDropAction(dtde.getTransferable(), dtde.getDropAction());
	}



	public static void handleLinkAction(Transferable transferable, NodeModel targetNode,
			Controller controller, ModeController modeController) throws UnsupportedFlavorException, IOException {
		int yesorno = JOptionPane.YES_OPTION;
		if (controller.getSelection().size() >= 5) {
			yesorno = JOptionPane.showConfirmDialog(controller.getViewController().getCurrentRootComponent(),
				TextUtils.getText("lots_of_links_warning"),
				Integer.toString(controller.getSelection().size()) + " links to the same node",
				JOptionPane.YES_NO_OPTION);
		}
		if (yesorno == JOptionPane.YES_OPTION) {
			for (final NodeModel sourceNodeModel : getNodeObjects(transferable)) {
				((MLinkController) LinkController.getController(modeController)).addConnector(
					sourceNodeModel, targetNode);
			}
		}
	}

	public static void handleMoveOrCopyAction(Transferable transferable, NodeModel targetNode,
			int dropAction, boolean isLocalTransfer, InsertionRelation insertionRelation, Side side)
			throws UnsupportedFlavorException, IOException {

		final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();

		if (DnDConstants.ACTION_MOVE == dropAction && isLocalTransfer && areFromSameMap(transferable, targetNode)) {
			final List<NodeModel> movedNodes = getNodeObjects(transferable);
			MouseEventActor.INSTANCE.withMouseEvent(() -> {
				if (insertionRelation != InsertionRelation.AS_CHILD) {
					mapController.moveNodes(movedNodes, targetNode, insertionRelation);
					mapController.setSide(movedNodes, targetNode.getSide());
				} else {
					List<NodeModel> nodesChangingParent = movedNodes.stream()
						.filter(node -> targetNode != node.getParentNode())
						.collect(Collectors.toList());
					mapController.moveNodes(movedNodes, targetNode, insertionRelation);
					mapController.setSide(nodesChangingParent.isEmpty() ? movedNodes : nodesChangingParent, side);
				}
			});
		}
		else if (DnDConstants.ACTION_COPY == dropAction || DnDConstants.ACTION_MOVE == dropAction) {
			((MMapClipboardController) MapClipboardController.getController()).paste(transferable, targetNode, side, dropAction);
		}
	}


}