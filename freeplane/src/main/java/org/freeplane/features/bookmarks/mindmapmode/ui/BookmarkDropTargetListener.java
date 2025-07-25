package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeModel.Side;
import org.freeplane.features.map.clipboard.MindMapNodesSelection;
import org.freeplane.features.map.mindmapmode.InsertionRelation;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.view.swing.ui.NodeDropUtils;

class BookmarkDropTargetListener extends DropTargetAdapter {
	private final DropValidator validator;
	private final DropExecutor executor;
	private final HoverTimer hoverTimer;

	public BookmarkDropTargetListener(BookmarkToolbar toolbar, BookmarksController bookmarksController) {
		this.validator = new DropValidator(toolbar);
		this.executor = new DropExecutor(toolbar, bookmarksController);
		this.hoverTimer = new HoverTimer();
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		dragOver(dtde);
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		Component targetComponent = dtde.getDropTargetContext().getComponent();

		if (targetComponent instanceof BookmarkButton) {
			BookmarkButton targetButton = (BookmarkButton) targetComponent;
			if (dtde.isDataFlavorSupported(BookmarkTransferables.BOOKMARK_FLAVOR)) {
				handleBookmarkDragOver(dtde, targetButton);
			}
			else if (NodeDropUtils.isDragAcceptable(dtde, targetButton.getBookmark().getNode(), NodeDropUtils.AcceptedContent.ANY)) {
				handleNodeDragOver(dtde, targetButton);
			}
			else {
				dtde.rejectDrag();
				targetButton.clearVisualFeedback();
				hoverTimer.cancelHoverTimer();
			}
		} else if (targetComponent instanceof BookmarkToolbar) {
			BookmarkToolbar toolbar = (BookmarkToolbar) targetComponent;
			if (dtde.isDataFlavorSupported(MindMapNodesSelection.mindMapNodeObjectsFlavor)) {
				handleNodeDragOverToolbar(dtde, toolbar);
			}
			else {
				dtde.rejectDrag();
				toolbar.clearVisualFeedback();
				hoverTimer.cancelHoverTimer();
			}
		} else {
			dtde.rejectDrag();
		}
	}

	private void handleBookmarkDragOver(DropTargetDragEvent dtde, BookmarkButton targetButton) {
		try {
			int sourceIndex = (Integer) dtde.getTransferable().getTransferData(BookmarkTransferables.BOOKMARK_FLAVOR);
			Point dropPoint = dtde.getLocation();

			DropValidation validation = validator.validateDrop(sourceIndex, targetButton, dropPoint);
			if (!validation.isValid) {
				dtde.rejectDrag();
				targetButton.clearVisualFeedback();
				hoverTimer.cancelHoverTimer();
				return;
			}

			dtde.acceptDrag(DnDConstants.ACTION_MOVE);
			targetButton.showDropZoneIndicator(validation.dropsAfter);
			hoverTimer.cancelHoverTimer();

		} catch (Exception e) {
			dtde.rejectDrag();
			targetButton.clearVisualFeedback();
			hoverTimer.cancelHoverTimer();
		}
	}

	private void handleNodeDragOver(DropTargetDragEvent dtde, BookmarkButton targetButton) {
		try {
			Point dropPoint = dtde.getLocation();
			NodeDropZone dropZone = determineNodeDropZone(targetButton, dropPoint);

			int dragActionType = NodeDropUtils.getDropAction(dtde.getTransferable(), dtde.getDropAction());
			if (dropZone.isLateral()) {
				if (dtde.isDataFlavorSupported(MindMapNodesSelection.mindMapNodeObjectsFlavor)) {
					NodeModel draggedNode = extractSingleNode(dtde);
					if (draggedNode == null) {
						dtde.rejectDrag();
						return;
					}
				}
				dtde.acceptDrag(dragActionType);
				targetButton.showDropZoneIndicator(dropZone.isAfter());
				hoverTimer.cancelHoverTimer();
			} else {
				dtde.acceptDrag(dragActionType);
				targetButton.showFeedback(BookmarkToolbar.DropIndicatorType.HOVER_FEEDBACK);
				hoverTimer.startHoverTimer(targetButton);
			}

		} catch (Exception e) {
			dtde.rejectDrag();
			targetButton.clearVisualFeedback();
		}
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		Component targetComponent = dte.getDropTargetContext().getComponent();
		if (targetComponent instanceof BookmarkButton) {
			BookmarkButton button = (BookmarkButton) targetComponent;
			button.clearVisualFeedback();
		} else if (targetComponent instanceof BookmarkToolbar) {
			BookmarkToolbar toolbar = (BookmarkToolbar) targetComponent;
			toolbar.clearVisualFeedback();
		}
		hoverTimer.cancelHoverTimer();
	}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		Component targetComponent = dtde.getDropTargetContext().getComponent();
		hoverTimer.cancelHoverTimer();

		try {
			if (targetComponent instanceof BookmarkButton) {
				BookmarkButton targetButton = (BookmarkButton) targetComponent;
				if (dtde.isDataFlavorSupported(BookmarkTransferables.BOOKMARK_FLAVOR)) {
					handleBookmarkDrop(dtde, targetButton);
				} else if (NodeDropUtils.isDropAcceptable(dtde, targetButton.getBookmark().getNode(), NodeDropUtils.getDropAction(dtde), NodeDropUtils.AcceptedContent.ANY)) {
					handleNodeDrop(dtde, targetButton);
				} else {
					dtde.rejectDrop();
				}
			} else if (targetComponent instanceof BookmarkToolbar) {
				BookmarkToolbar toolbar = (BookmarkToolbar) targetComponent;
				if (dtde.isDataFlavorSupported(MindMapNodesSelection.mindMapNodeObjectsFlavor)) {
					handleNodeDropOnToolbar(dtde, toolbar);
				} else {
					dtde.rejectDrop();
				}
			} else {
				dtde.rejectDrop();
			}

		} catch (Exception e) {
			dtde.dropComplete(false);
		} finally {
			if (targetComponent instanceof BookmarkButton) {
				((BookmarkButton) targetComponent).clearVisualFeedback();
			} else if (targetComponent instanceof BookmarkToolbar) {
				((BookmarkToolbar) targetComponent).clearVisualFeedback();
			}
		}
	}

	private void handleBookmarkDrop(DropTargetDropEvent dtde, BookmarkButton targetButton)
	        throws UnsupportedFlavorException, IOException {
		int sourceIndex = (Integer) dtde.getTransferable().getTransferData(BookmarkTransferables.BOOKMARK_FLAVOR);
		Point dropPoint = dtde.getLocation();

		DropValidation validation = validator.validateDrop(sourceIndex, targetButton, dropPoint);
		if (!validation.isValid) {
			dtde.rejectDrop();
			return;
		}

		dtde.acceptDrop(DnDConstants.ACTION_MOVE);
		executor.moveBookmark(sourceIndex, validation.finalTargetIndex);
		dtde.dropComplete(true);
	}

	private void handleNodeDrop(DropTargetDropEvent dtde, BookmarkButton targetButton) {
		try {
			Point dropPoint = dtde.getLocation();
			NodeDropZone dropZone = determineNodeDropZone(targetButton, dropPoint);

			int dropAction = NodeDropUtils.getDropAction(dtde);

			if (dropZone.isLateral()) {
				if (dtde.isDataFlavorSupported(MindMapNodesSelection.mindMapNodeObjectsFlavor)) {
					NodeModel draggedNode = extractSingleNode(dtde);
					if (draggedNode == null) {
						dtde.rejectDrop();
						return;
					}
					dtde.acceptDrop(dropAction);
					boolean success = executor.createBookmarkFromNodeAtPosition(dtde, dropZone.getInsertionIndex());
					dtde.dropComplete(success);
				} else {
					dtde.rejectDrop();
				}
			} else {
				final NodeModel targetNode = targetButton.getBookmark().getNode();
				final Transferable t = dtde.getTransferable();

				if (!dtde.isLocalTransfer()) {
					dtde.acceptDrop(DnDConstants.ACTION_COPY);
					NodeDropUtils.handleMoveOrCopyAction(t, targetNode, dropAction, dtde.isLocalTransfer(), InsertionRelation.AS_CHILD, Side.DEFAULT);
					dtde.dropComplete(true);
					return;
				}

				dtde.acceptDrop(dropAction);
				if (dropAction == DnDConstants.ACTION_LINK) {
					final Controller controller = Controller.getCurrentController();
					final ModeController modeController = controller.getModeController();
					NodeDropUtils.handleLinkAction(t, targetNode, controller, modeController);
				} else {
					NodeDropUtils.handleMoveOrCopyAction(t, targetNode, dropAction, dtde.isLocalTransfer(), InsertionRelation.AS_CHILD, Side.DEFAULT);
				}
				dtde.dropComplete(true);
			}
		} catch (Exception e) {
			dtde.rejectDrop();
		}
	}

	private void handleNodeDragOverToolbar(DropTargetDragEvent dtde, BookmarkToolbar toolbar) {
		try {
			NodeModel draggedNode = extractSingleNode(dtde);
			if (draggedNode == null) {
				dtde.rejectDrag();
				return;
			}

			Point dropPoint = dtde.getLocation();
			if (!isPointInContentArea(toolbar, dropPoint)) {
				dtde.rejectDrag();
				return;
			}

			BookmarkIndexCalculator.ToolbarDropPosition position = validator.calculateToolbarDropPosition(dropPoint);

			showToolbarDropFeedback(toolbar, position);
			hoverTimer.cancelHoverTimer();

			int dragActionType = NodeDropUtils.getDropAction(dtde.getTransferable(), dtde.getDropAction());
			dtde.acceptDrag(dragActionType);

		} catch (Exception e) {
			dtde.rejectDrag();
			toolbar.clearVisualFeedback();
		}
	}

	private void handleNodeDropOnToolbar(DropTargetDropEvent dtde, BookmarkToolbar toolbar) {
		try {
			NodeModel draggedNode = extractSingleNode(dtde);
			if (draggedNode == null) {
				dtde.rejectDrop();
				return;
			}

			Point dropPoint = dtde.getLocation();
			if (!isPointInContentArea(toolbar, dropPoint)) {
				dtde.rejectDrop();
				return;
			}

			BookmarkIndexCalculator.ToolbarDropPosition position = validator.calculateToolbarDropPosition(dropPoint);

			int dragActionType = NodeDropUtils.getDropAction(dtde.getTransferable(), dtde.getDropAction());
			dtde.acceptDrop(dragActionType);

			boolean success = executor.createBookmarkFromNodeAtPosition(dtde, position.getInsertionIndex());
			dtde.dropComplete(success);
		} catch (Exception e) {
			dtde.rejectDrop();
		}
	}

	private boolean isPointInContentArea(BookmarkToolbar toolbar, Point point) {
		Insets insets = toolbar.getInsets();
		int x = point.x;
		int y = point.y;
		int width = toolbar.getWidth();
		int height = toolbar.getHeight();

		return x >= insets.left &&
		       x < (width - insets.right) &&
		       y >= insets.top &&
		       y < (height - insets.bottom);
	}

	private NodeModel extractSingleNode(DropTargetDragEvent dtde) throws Exception {
		BookmarkToolbar toolbar = getToolbarFromEvent(dtde);
		return extractSingleNode(dtde.getTransferable(), toolbar);
	}

	private NodeModel extractSingleNode(DropTargetDropEvent dtde) throws Exception {
		BookmarkToolbar toolbar = getToolbarFromEvent(dtde);
		return extractSingleNode(dtde.getTransferable(), toolbar);
	}

	private BookmarkToolbar getToolbarFromEvent(DropTargetEvent dte) {
		Component component = dte.getDropTargetContext().getComponent();
		if (component instanceof BookmarkToolbar) {
			return (BookmarkToolbar) component;
		} else if (component instanceof BookmarkButton) {
			return (BookmarkToolbar) component.getParent();
		}
		Component parent = component.getParent();
		if (parent instanceof BookmarkToolbar) {
			return (BookmarkToolbar) parent;
		}
		throw new IllegalArgumentException("Event target is not associated with a BookmarkToolbar");
	}

	private NodeModel extractSingleNode(Transferable transferable, BookmarkToolbar toolbar) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<NodeModel> draggedNodesCollection = (Collection<NodeModel>) transferable
				.getTransferData(MindMapNodesSelection.mindMapNodeObjectsFlavor);
		List<NodeModel> draggedNodes = new ArrayList<>(draggedNodesCollection);

		if (draggedNodes.size() != 1) {
			return null;
		}

		NodeModel draggedNode = draggedNodes.get(0);
		MapModel nodeMap = draggedNode.getMap();
		MapModel toolbarMap = toolbar.getMap();

		return (nodeMap != null && nodeMap.equals(toolbarMap)) ? draggedNode : null;
	}

	private void showToolbarDropFeedback(BookmarkToolbar toolbar, BookmarkIndexCalculator.ToolbarDropPosition position) {
		switch (position.type) {
			case BEFORE_BUTTON:
				if (position.buttonIndex < toolbar.getComponentCount()) {
					Component component = toolbar.getComponent(position.buttonIndex);
					if (component instanceof BookmarkButton) {
						BookmarkButton button = (BookmarkButton) component;
						button.showFeedback(BookmarkToolbar.DropIndicatorType.DROP_BEFORE);
					}
				}
				break;
			case AFTER_BUTTON:
				if (position.buttonIndex < toolbar.getComponentCount()) {
					Component component = toolbar.getComponent(position.buttonIndex);
					if (component instanceof BookmarkButton) {
						BookmarkButton button = (BookmarkButton) component;
						button.showFeedback(BookmarkToolbar.DropIndicatorType.DROP_AFTER);
					}
				}
				break;
			case AT_END:
				toolbar.showEndDropIndicator();
				break;
		}
	}

	private NodeDropZone determineNodeDropZone(BookmarkButton button, Point dropPoint) {
		int buttonWidth = button.getWidth();
		int edgeThreshold = Math.max(BookmarkToolbar.GAP, buttonWidth / 6);

		if (dropPoint.x <= edgeThreshold) {
			BookmarkToolbar toolbar = (BookmarkToolbar) button.getParent();
			int buttonIndex = toolbar.getComponentIndex(button);
			return new NodeDropZone(true, false, buttonIndex);
		} else if (dropPoint.x >= buttonWidth - edgeThreshold) {
			BookmarkToolbar toolbar = (BookmarkToolbar) button.getParent();
			int buttonIndex = toolbar.getComponentIndex(button);
			return new NodeDropZone(true, true, buttonIndex + 1);
		} else {
			return new NodeDropZone(false, false, -1);
		}
	}

	private static class NodeDropZone {
		private final boolean lateral;
		private final boolean after;
		private final int insertionIndex;

		public NodeDropZone(boolean lateral, boolean after, int insertionIndex) {
			this.lateral = lateral;
			this.after = after;
			this.insertionIndex = insertionIndex;
		}

		public boolean isLateral() {
			return lateral;
		}

		public boolean isAfter() {
			return after;
		}

		public int getInsertionIndex() {
			return insertionIndex;
		}
	}
}