package org.freeplane.features.bookmarks.mindmapmode.ui;

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

import javax.swing.JButton;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.clipboard.MindMapNodesSelection;

class BookmarkDropTargetListener extends DropTargetAdapter {
	private final DropVisualFeedback visualFeedback;
	private final DropValidator validator;
	private final DropExecutor executor;
	private final HoverTimer hoverTimer;

	public BookmarkDropTargetListener(FreeplaneToolBar toolbar, BookmarksController bookmarksController, BookmarksToolbarBuilder toolbarBuilder) {
		this.visualFeedback = new DropVisualFeedback();
		this.validator = new DropValidator(toolbar, toolbarBuilder);
		this.executor = new DropExecutor(toolbar, bookmarksController);
		this.hoverTimer = new HoverTimer(visualFeedback);
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		JButton button = (JButton) dtde.getDropTargetContext().getComponent();
		visualFeedback.saveOriginalBorder(button);
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		JButton targetButton = (JButton) dtde.getDropTargetContext().getComponent();

		if (dtde.isDataFlavorSupported(BookmarkTransferables.BOOKMARK_FLAVOR)) {
			handleBookmarkDragOver(dtde, targetButton);
		}
		else if (dtde.isDataFlavorSupported(MindMapNodesSelection.mindMapNodeObjectsFlavor)) {
			handleNodeDragOver(dtde, targetButton);
		}
		else {
			dtde.rejectDrag();
			visualFeedback.clearVisualFeedback(targetButton);
			hoverTimer.cancelHoverTimer();
		}
	}

	private void handleBookmarkDragOver(DropTargetDragEvent dtde, JButton targetButton) {
		try {
			int sourceIndex = (Integer) dtde.getTransferable().getTransferData(BookmarkTransferables.BOOKMARK_FLAVOR);
			Point dropPoint = dtde.getLocation();

			DropValidation validation = validator.validateDrop(sourceIndex, targetButton, dropPoint);
			if (!validation.isValid) {
				dtde.rejectDrag();
				visualFeedback.clearVisualFeedback(targetButton);
				hoverTimer.cancelHoverTimer();
				return;
			}

			dtde.acceptDrag(DnDConstants.ACTION_MOVE);
			visualFeedback.showDropZoneIndicator(targetButton, validation.dropsAfter);
			hoverTimer.cancelHoverTimer();

		} catch (Exception e) {
			dtde.rejectDrag();
			visualFeedback.clearVisualFeedback(targetButton);
			hoverTimer.cancelHoverTimer();
		}
	}

	private void handleNodeDragOver(DropTargetDragEvent dtde, JButton targetButton) {
		try {
			@SuppressWarnings("unchecked")
			Collection<NodeModel> draggedNodesCollection = (Collection<NodeModel>) dtde.getTransferable()
			        .getTransferData(MindMapNodesSelection.mindMapNodeObjectsFlavor);
			List<NodeModel> draggedNodes = new ArrayList<>(draggedNodesCollection);

			if (draggedNodes.size() != 1) {
				dtde.rejectDrag();
				visualFeedback.clearVisualFeedback(targetButton);
				hoverTimer.cancelHoverTimer();
				return;
			}

			Point dropPoint = dtde.getLocation();
			DropValidation validation = validator.validateNodeDrop(targetButton, dropPoint);

			if (!validation.isValid) {
				dtde.rejectDrag();
				visualFeedback.clearVisualFeedback(targetButton);
				hoverTimer.cancelHoverTimer();
				return;
			}

			int dragActionType = getDropAction(dtde);
			dtde.acceptDrag(dragActionType);

			if (validation.isInsertionDrop) {
				visualFeedback.showNodeDropZoneIndicator(targetButton, validation.dropsAfter);
				hoverTimer.cancelHoverTimer();
			} else {
				visualFeedback.showHoverFeedback(targetButton);
				hoverTimer.startHoverTimer(targetButton);
			}

		} catch (Exception e) {
			dtde.rejectDrag();
			visualFeedback.clearVisualFeedback(targetButton);
			hoverTimer.cancelHoverTimer();
		}
	}

	private int getDropAction(final DropTargetDragEvent dtde) {
		int dropAction = dtde.getDropAction();
		final Transferable t = dtde.getTransferable();

		if (t.isDataFlavorSupported(MindMapNodesSelection.dropCopyActionFlavor)) {
			dropAction = DnDConstants.ACTION_COPY;
		} else if (t.isDataFlavorSupported(MindMapNodesSelection.dropLinkActionFlavor)) {
			dropAction = DnDConstants.ACTION_LINK;
		}

		return dropAction;
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		JButton button = (JButton) dte.getDropTargetContext().getComponent();
		visualFeedback.clearVisualFeedback(button);
		hoverTimer.cancelHoverTimer();
	}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		JButton targetButton = (JButton) dtde.getDropTargetContext().getComponent();
		hoverTimer.cancelHoverTimer();

		try {
			if (dtde.isDataFlavorSupported(BookmarkTransferables.BOOKMARK_FLAVOR)) {
				handleBookmarkDrop(dtde, targetButton);
			} else if (dtde.isDataFlavorSupported(
			        MindMapNodesSelection.mindMapNodeObjectsFlavor)) {
				handleNodeDrop(dtde, targetButton);
			} else {
				dtde.rejectDrop();
			}

		} catch (Exception e) {
			dtde.dropComplete(false);
		} finally {
			visualFeedback.clearVisualFeedback(targetButton);
		}
	}

	private void handleBookmarkDrop(DropTargetDropEvent dtde, JButton targetButton)
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

	private void handleNodeDrop(DropTargetDropEvent dtde, JButton targetButton) {
		Point dropPoint = dtde.getLocation();
		DropValidation validation = validator.validateNodeDrop(targetButton, dropPoint);

		if (!validation.isValid) {
			dtde.rejectDrop();
			return;
		}

		NodeBookmark bookmark = (NodeBookmark) targetButton.getClientProperty("bookmark");
		int dragActionType = getDropActionForDrop(dtde);

		dtde.acceptDrop(dragActionType);

		boolean dropAfter = validation.isInsertionDrop ? validation.dropsAfter : true;
		boolean success = executor.createBookmarkFromNode(dtde.getTransferable(), bookmark, dropAfter, targetButton);

		dtde.dropComplete(success);
	}

	private int getDropActionForDrop(final DropTargetDropEvent dtde) {
		int dropAction = dtde.getDropAction();
		final Transferable t = dtde.getTransferable();

		if (t.isDataFlavorSupported(MindMapNodesSelection.dropCopyActionFlavor)) {
			dropAction = DnDConstants.ACTION_COPY;
		} else if (t.isDataFlavorSupported(MindMapNodesSelection.dropLinkActionFlavor)) {
			dropAction = DnDConstants.ACTION_LINK;
		}

		return dropAction;
	}
}