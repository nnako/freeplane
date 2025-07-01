package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmarkDescriptor;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;
import org.freeplane.view.swing.map.NodeTooltipManager;

class BookmarkButtonConfigurator {
	private final BookmarksController bookmarksController;
	private final ModeController modeController;

	BookmarkButtonConfigurator(BookmarksController bookmarksController,
									  ModeController modeController) {
		this.bookmarksController = bookmarksController;
		this.modeController = modeController;
	}

	void configureButton(BookmarkButton button, NodeBookmark bookmark,
								BookmarkToolbar toolbar, IMapSelection selection) {
		final NodeBookmarkDescriptor descriptor = bookmark.getDescriptor();
		final NodeModel node = bookmark.getNode();

		button.setText(descriptor.getName());
		button.addActionListener(this::applyAction);
		button.putClientProperty(NodeTooltipManager.TOOLTIP_LOCATION_PROPERTY, NodeTooltipManager.TOOLTIP_LOCATION_ABOVE);

		registerTooltip(button);
		setButtonIcon(button, node, descriptor);
		setupDragAndDrop(button, toolbar);
		setupActionMap(button, toolbar);
		addMouseListener(button);
	}

	void configureNonBookmarkComponent(Component component) {
		new DropTarget(component, DnDConstants.ACTION_NONE, new DropTargetAdapter() {
			@Override
			public void drop(DropTargetDropEvent dtde) {
				dtde.rejectDrop();
			}
		});
	}

	private void applyAction(ActionEvent action) {
		final BookmarkButton button = (BookmarkButton) action.getSource();
		if((action.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK)
			showBookmarkPopupMenu(button);
		else
			button.getBookmark().open();
	}

	private void registerTooltip(BookmarkButton button) {
		NodeTooltipManager toolTipManager = NodeTooltipManager.getSharedInstance(modeController);
		toolTipManager.registerComponent(button);
	}

	private void setButtonIcon(BookmarkButton button, NodeModel node, NodeBookmarkDescriptor descriptor) {
		button.setIcon(BookmarkIconFactory.createIcon(node, descriptor));
	}

	private void setupDragAndDrop(BookmarkButton button, BookmarkToolbar toolbar) {
		DragSource dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(button,
			DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK,
			new BookmarkDragGestureListener(button));

		@SuppressWarnings("unused")
		DropTarget dropTarget = new DropTarget(button,
			DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE,
			new BookmarkDropTargetListener(toolbar, bookmarksController));
	}

	private void setupActionMap(BookmarkButton button, BookmarkToolbar toolbar) {
		BookmarkClipboardHandler clipboardHandler = toolbar.getClipboardHandler();
		clipboardHandler.setupButtonClipboardActions(button);
		
		Action showContextMenuAction = new AbstractAction("showContextMenu") {
			@Override
			public void actionPerformed(ActionEvent e) {
				showBookmarkPopupMenu(button);
			}
		};
		
		button.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "showContextMenu");
		button.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, KeyEvent.CTRL_DOWN_MASK), "showContextMenu");
		
		button.getActionMap().put("showContextMenu", showContextMenuAction);
	}

	private void addMouseListener(BookmarkButton button) {
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					showBookmarkPopupMenu(button);
				}
			}
		});
	}
	private void showBookmarkPopupMenu(BookmarkButton button) {
		BookmarkPopupMenu popup = new BookmarkPopupMenu(button.getBookmark(), bookmarksController);
		int menuHeight = popup.getPreferredSize().height;
		popup.show(button, 0, -menuHeight);
	}
}