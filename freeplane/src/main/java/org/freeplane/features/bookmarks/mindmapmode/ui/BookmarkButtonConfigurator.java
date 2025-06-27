package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmarkDescriptor;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;
import org.freeplane.view.swing.map.NodeTooltipManager;

class BookmarkButtonConfigurator {
	private static final String BOOKMARK_CLIENT_PROPERTY = "bookmark";
	
	private final BookmarksController bookmarksController;
	private final ModeController modeController;
	private final BookmarksToolbarBuilder toolbarBuilder;

	BookmarkButtonConfigurator(BookmarksController bookmarksController, 
									  ModeController modeController,
									  BookmarksToolbarBuilder toolbarBuilder) {
		this.bookmarksController = bookmarksController;
		this.modeController = modeController;
		this.toolbarBuilder = toolbarBuilder;
	}

	void configureButton(BookmarkButton button, NodeBookmark bookmark, 
								FreeplaneToolBar toolbar, IMapSelection selection) {
		final NodeBookmarkDescriptor descriptor = bookmark.getDescriptor();
		final NodeModel node = bookmark.getNode();
		
		button.setText(descriptor.getName());
		button.addActionListener(action -> bookmark.open());
		button.putClientProperty(BOOKMARK_CLIENT_PROPERTY, bookmark);
		button.putClientProperty(NodeTooltipManager.TOOLTIP_LOCATION_PROPERTY, NodeTooltipManager.TOOLTIP_LOCATION_ABOVE);
		
		registerTooltip(button);
		setButtonIcon(button, node, descriptor);
		setButtonEnabledState(button, node, selection);
		setupDragAndDrop(button, toolbar);
		addMouseListener(button, bookmark);
	}

	private void registerTooltip(BookmarkButton button) {
		NodeTooltipManager toolTipManager = NodeTooltipManager.getSharedInstance(modeController);
		toolTipManager.registerComponent(button);
	}

	private void setButtonIcon(BookmarkButton button, NodeModel node, NodeBookmarkDescriptor descriptor) {
		button.setIcon(BookmarkIconFactory.createIcon(node, descriptor));
	}

	private void setButtonEnabledState(BookmarkButton button, NodeModel node, IMapSelection selection) {
		final boolean isVisible = node.isVisible(selection.getFilter());
		button.setEnabled(isVisible);
	}

	private void setupDragAndDrop(BookmarkButton button, FreeplaneToolBar toolbar) {
		DragSource dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(button, 
			DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK,
			new BookmarkDragGestureListener(button, toolbarBuilder));

		DropTarget dropTarget = new DropTarget(button, 
			DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE, 
			new BookmarkDropTargetListener(toolbar, bookmarksController, toolbarBuilder));
	}

	private void addMouseListener(BookmarkButton button, NodeBookmark bookmark) {
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					showBookmarkPopupMenu(e, bookmark, button);
				}
			}
		});
	}

	private void showBookmarkPopupMenu(MouseEvent e, NodeBookmark bookmark, BookmarkButton button) {
		BookmarkPopupMenu popup = new BookmarkPopupMenu(bookmark, button, bookmarksController);
		popup.show(button, e.getX(), e.getY());
	}
} 