package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.UUID;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.freeplane.core.ui.components.IconListComponent;
import org.freeplane.core.ui.components.TagIcon;
import org.freeplane.core.util.ColorUtils;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.icon.mindmapmode.TagSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.clipboard.MapClipboardController;
import org.freeplane.features.map.clipboard.MindMapNodesSelection;
import org.freeplane.features.mode.Controller;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.NodeViewFolder;
import org.freeplane.view.swing.ui.MouseEventActor;

/**
 * The NodeDragListener which belongs to every NodeView
 */
public class MNodeDragListener implements DragGestureListener {

	private final NodeViewFolder nodeFolder = new NodeViewFolder(false);

	public void addDragListener(MainView mainView) {
		addDragListenerToComponent(mainView);
	}
	public void addDragListener(IconListComponent iconListComponent) {
		addDragListenerToComponent(iconListComponent);
	}
	private void addDragListenerToComponent(Component component) {
		final DragSource dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(component, DnDConstants.ACTION_COPY
		        | DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK, this);
	}

	@Override
	public void dragGestureRecognized(final DragGestureEvent e) {
		nodeFolder.reset();
		final Component component =  e.getComponent();
		final NodeView nodeView = (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class, component);
		final MapView mapView = nodeView.getMap();
		mapView.select();
		if(! nodeView.isSelected()){
			MouseEventActor.INSTANCE.withMouseEvent( () ->
				nodeView.getMap().selectAsTheOnlyOneSelected(nodeView));
		}
		Rectangle bounds = new Rectangle(0, 0, component.getWidth(), component.getHeight());
		if(!bounds.contains(e.getDragOrigin()))
			return;
		final TagIcon tag;
		if(component instanceof MainView)
			tag = ((MainView) component).getTagIconAt(e.getDragOrigin());
		else if(component instanceof IconListComponent) {
			Icon icon = ((IconListComponent) component).getIconAt(e.getDragOrigin());
			if(icon instanceof TagIcon)
				tag = (TagIcon) icon;
			else
				return;
		}
		else
			return;
		if(tag != null)
			startTagDrag(e, nodeView, tag);
		else
			startNodeDrag(e, nodeView);
	}

	private void startTagDrag(final DragGestureEvent e, NodeView nodeView, final TagIcon tagIcon) {
		int dragActionType = e.getDragAction();
		if (dragActionType == DnDConstants.ACTION_LINK || isLinkDragEvent(e)) {
			return;
		}
		Cursor cursor = getCursorByAction(dragActionType);
		Tag tag = tagIcon.getTag().qualifiedTag();
		final TagSelection t = new TagSelection(UUID.randomUUID(),
				tag.getContent() + ColorUtils.colorToRGBAString(tag.getColor()));
		if ((e.getTriggerEvent().getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0) {
			cursor = DragSource.DefaultCopyDrop;
			dragActionType = DnDConstants.ACTION_COPY;
			t.setDropAction(dragActionType);
		}
		try {
			BufferedImage image = new BufferedImage(tagIcon.getIconWidth(), tagIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics();
			tagIcon.paintIcon(e.getComponent(), graphics, 0, 0);
			DragSourceListener trashBinListener =
					dragActionType == DnDConstants.ACTION_MOVE
					? TrashBin.INSTANCE.showTrashBin(e, () -> {/**/})
					: null;
			int effectiveDragActionType = dragActionType;
			e.startDrag(cursor, image, new Point(), t, new DragSourceAdapter() {
				@Override
				public void dragDropEnd(DragSourceDropEvent dsde) {
					if(! dsde.getDropSuccess()) {
						nodeFolder.adjustFolding(Collections.emptySet());
						nodeView.scrollNodeToVisible();
					}
					nodeFolder.reset();
					if(trashBinListener != null)
						trashBinListener.dragDropEnd(dsde);
					if(dsde.getDropSuccess() && dsde.getDropAction() == DnDConstants.ACTION_MOVE
							&& effectiveDragActionType  == DnDConstants.ACTION_MOVE)
						SwingUtilities.invokeLater(() -> removeTag(nodeView.getNode(), tag));
				}
			});
		}
		catch (final InvalidDnDOperationException ex) {
		}
	}

	private void removeTag(NodeModel node, Tag tag) {
		((MIconController)IconController.getController()).removeTags(node, Collections.singleton(tag));
	}

	private void startNodeDrag(final DragGestureEvent e, final NodeView nodeView) {
		final int dragActionType = e.getDragAction();
		if (dragActionType == DnDConstants.ACTION_MOVE) {
			if (nodeView.isRoot()) {
				if(! isLinkDragEvent(e))
					return;
			}
		}
		Cursor cursor = getCursorByAction(dragActionType);
		final Transferable t = MapClipboardController.getController().copy(Controller.getCurrentController().getSelection());
		if (isLinkDragEvent(e)) {
			cursor = DragSource.DefaultLinkDrop;
			((MindMapNodesSelection) t).setDropAction(DnDConstants.ACTION_LINK);
		}
		else if ((e.getTriggerEvent().getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0) {
			cursor = DragSource.DefaultCopyDrop;
			((MindMapNodesSelection) t).setDropAction(DnDConstants.ACTION_COPY);
		}
		try {
			e.startDrag(cursor, t, new DragSourceAdapter() {

				@Override
				public void dragDropEnd(DragSourceDropEvent dsde) {
					if(! dsde.getDropSuccess()) {
						nodeFolder.adjustFolding(Collections.emptySet());
						nodeView.scrollNodeToVisible();
					}
					nodeFolder.reset();
				}
			});
		}
		catch (final InvalidDnDOperationException ex) {
		}
	}

	private boolean isLinkDragEvent(final DragGestureEvent e) {
	    return (e.getTriggerEvent().getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0;
    }

	public Cursor getCursorByAction(final int dragAction) {
		switch (dragAction) {
			case DnDConstants.ACTION_COPY:
				return DragSource.DefaultCopyDrop;
			case DnDConstants.ACTION_LINK:
				return DragSource.DefaultLinkDrop;
			default:
				return DragSource.DefaultMoveDrop;
		}
	}
	public DropTargetListener createDropListener() {
		return new MNodeDropListener(nodeFolder);
	}

}
