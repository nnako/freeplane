package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import org.freeplane.features.bookmarks.mindmapmode.NodeBookmarkDescriptor;
import org.freeplane.features.filter.hidden.NodeVisibility;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

class BookmarkIconFactory {
	private static final Icon BOOKMARK_ROOT_ICON = IconStoreFactory.ICON_STORE.getUIIcon("bookmarkAsRoot.svg").getIcon();
	private static final Icon SELECTED_ROOT_ICON = IconStoreFactory.ICON_STORE.getUIIcon("currentRoot.svg").getIcon();
	private static final Icon SELECTED_SUBTREE_ICON = IconStoreFactory.ICON_STORE.getUIIcon("selectedSubtreeBookmark.svg").getIcon();
	private static final Icon HIDDEN_ICON = IconStoreFactory.ICON_STORE.getUIIcon("hidden.svg").getIcon();
	private static final Icon REMOVE_FILTER_ICON = IconStoreFactory.ICON_STORE.getUIIcon("bookmark-remove-filter.svg").getIcon();

	static Icon createIcon(NodeModel node, NodeBookmarkDescriptor descriptor) {
		if (descriptor.opensAsRoot()) {
			return new RootBookmarkIcon(node);
		} else {
			return new SubtreeBookmarkIcon(node);
		}
	}
	private static class RootBookmarkIcon implements Icon {
		private final NodeModel node;

		public RootBookmarkIcon(NodeModel node) {
			this.node = node;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Icon icon;
			if (Controller.getCurrentController().getSelection().getSelectionRoot() == node) {
				icon = SELECTED_ROOT_ICON;
			} else {
				icon = BOOKMARK_ROOT_ICON;
			}
			icon.paintIcon(c, g, x, y);
		}

		@Override
		public int getIconWidth() {
			return SELECTED_ROOT_ICON.getIconWidth();
		}

		@Override
		public int getIconHeight() {
			return SELECTED_ROOT_ICON.getIconHeight();
		}
	}

	private static class SubtreeBookmarkIcon implements Icon {
		private final NodeModel node;

		public SubtreeBookmarkIcon(NodeModel node) {
			this.node = node;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			if(NodeVisibility.isHidden(node)) {
				HIDDEN_ICON.paintIcon(c, g, x, y);
				return;
			}
			final IMapSelection selection = Controller.getCurrentController().getSelection();
			if(! node.isVisible(selection.getFilter())) {
				REMOVE_FILTER_ICON.paintIcon(c, g, x, y);
				return;
			}
			final NodeModel selected = selection.getSelected();
			final boolean isBookmarkSubtreeSelected = selected == node || selected.isDescendantOf(node);

			if (isBookmarkSubtreeSelected) {
				SELECTED_SUBTREE_ICON.paintIcon(c, g, x, y);
			}
		}

		@Override
		public int getIconWidth() {
			return SELECTED_SUBTREE_ICON.getIconWidth();
		}

		@Override
		public int getIconHeight() {
			return SELECTED_SUBTREE_ICON.getIconHeight();
		}
	}
}