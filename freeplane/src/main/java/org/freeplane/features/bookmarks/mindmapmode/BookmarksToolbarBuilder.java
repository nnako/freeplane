package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;

import org.freeplane.core.ui.components.FocusRequestor;
import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.ITooltipProvider.TooltipTrigger;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.view.swing.map.FreeplaneTooltip;
import org.freeplane.view.swing.map.NodeTooltipManager;

class BookmarksToolbarBuilder {
	private static final Icon BOOKMARK_ROOT_ICON = IconStoreFactory.ICON_STORE.getUIIcon("bookmarkAsRoot.svg").getIcon();
	private static final Icon SELECTED_ROOT_ICON = IconStoreFactory.ICON_STORE.getUIIcon("currentRoot.svg").getIcon();
	private static final Icon SELECTED_SUBTREE_ICON = IconStoreFactory.ICON_STORE.getUIIcon("selectedSubtreeBookmark.svg").getIcon();
	private final BookmarksController bookmarksController;
	private final ModeController modeController;

	BookmarksToolbarBuilder(ModeController modeController, BookmarksController bookmarksController) {
		this.modeController = modeController;
		this.bookmarksController = bookmarksController;
	}

	void updateBookmarksToolbar(FreeplaneToolBar toolbar, MapModel map, IMapSelection selection) {
		toolbar.removeAll();
		toolbar.putClientProperty("bookmarksMap", map);

		List<NodeBookmark> bookmarks = bookmarksController.getBookmarks(map).getBookmarks();
		for (NodeBookmark bookmark : bookmarks) {
			final JButton button = createBookmarkButton(bookmark, toolbar, selection);
			toolbar.add(button);
		}

		toolbar.revalidate();
		toolbar.repaint();
	}

	@SuppressWarnings("unused")
	private JButton createBookmarkButton(NodeBookmark bookmark, FreeplaneToolBar toolbar, IMapSelection selection) {
		final NodeModel node = bookmark.getNode();
		@SuppressWarnings("serial")
		final JButton button = new JButton() {
			@Override
		    public JToolTip createToolTip() {
				FreeplaneTooltip tip = new FreeplaneTooltip(this.getGraphicsConfiguration(), FreeplaneTooltip.TEXT_HTML, false);
		        tip.setComponent(this);
		        tip.setComponentOrientation(getComponentOrientation());
		        tip.setBorder(BorderFactory.createEmptyBorder());
				final URL url = node.getMap().getURL();
				if (url != null) {
					tip.setBase(url);
				}
				else {
					try {
			            tip.setBase(new URL("file: "));
		            }
		            catch (MalformedURLException e) {
		            }
				}
		        return tip;
		    }

			@Override
			public String getToolTipText() {
				return modeController.createToolTip(node, this, TooltipTrigger.LINK);
			}

		};
		final NodeBookmarkDescriptor descriptor = bookmark.getDescriptor();
		button.setText(descriptor.getName());
		button.addActionListener(action -> bookmark.open());
		button.putClientProperty("bookmark", bookmark);
		button.putClientProperty(NodeTooltipManager.TOOLTIP_LOCATION_PROPERTY, NodeTooltipManager.TOOLTIP_LOCATION_ABOVE);
		NodeTooltipManager toolTipManager = NodeTooltipManager.getSharedInstance(modeController);
		toolTipManager.registerComponent(button);

		if (descriptor.opensAsRoot()) {
			button.setIcon(new Icon() {

				@Override
				public void paintIcon(Component c, Graphics g, int x, int y) {
					Icon icon;
					if(Controller.getCurrentController().getSelection().getSelectionRoot() == node)
						icon = SELECTED_ROOT_ICON;
					else
						icon = BOOKMARK_ROOT_ICON;
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

			});
		}
		else {
			button.setIcon(new Icon() {

				@Override
				public void paintIcon(Component c, Graphics g, int x, int y) {
					if(isBookmarkSubtreeSelected(node))
						SELECTED_SUBTREE_ICON.paintIcon(c, g, x, y);
				}

				private boolean isBookmarkSubtreeSelected(final NodeModel node) {
					final IMapSelection selection = Controller.getCurrentController().getSelection();
					final NodeModel selected = selection.getSelected();
					final boolean isActive = selected == node || selected.isDescendantOf(node);
					return isActive;
				}

				@Override
				public int getIconWidth() {
					return SELECTED_SUBTREE_ICON.getIconWidth();
				}

				@Override
				public int getIconHeight() {
					return SELECTED_SUBTREE_ICON.getIconHeight();
				}

			});

		}

		final boolean isVisible = node.isVisible(selection.getFilter());
		button.setEnabled(isVisible);

		DragSource dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(button, DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK,
			new BookmarkDragGestureListener(button, this));

		DropTarget dropTarget = new DropTarget(button, DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE, new BookmarkDropTargetListener(toolbar, bookmarksController, this));

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					showBookmarkPopupMenu(e, bookmark, button);
				}
			}
		});

		return button;
	}

	private void showBookmarkPopupMenu(MouseEvent e, NodeBookmark bookmark, JButton button) {
		JPopupMenu popup = new JPopupMenu();

		JMenuItem selectItem = TranslatedElementFactory.createMenuItem("bookmark.goto_node");
		selectItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				bookmark.open(false);
			}
		});
		popup.add(selectItem);

		JMenuItem openAsRootDirectItem = TranslatedElementFactory.createMenuItem("bookmark.open_as_root");
		openAsRootDirectItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				bookmark.open(true);
			}
		});
		popup.add(openAsRootDirectItem);

		popup.addSeparator();

		JMenuItem removeItem = TranslatedElementFactory.createMenuItem("bookmark.delete");
		removeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				bookmarksController.removeBookmark(bookmark.getNode());
			}
		});
		popup.add(removeItem);

		JMenuItem renameItem = TranslatedElementFactory.createMenuItem("bookmark.rename");
		renameItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				showRenameDialog(bookmark);
			}
		});
		popup.add(renameItem);

		JCheckBoxMenuItem openAsRootItem = TranslatedElementFactory.createCheckboxMenuItem("bookmark.opens_as_root");
		openAsRootItem.setSelected(bookmark.getDescriptor().opensAsRoot());
		openAsRootItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				toggleOpenAsRoot(bookmark);
			}
		});

		if(bookmark.getNode().isRoot())
			openAsRootItem.setEnabled(false);
		popup.add(openAsRootItem);

		popup.show(button, e.getX(), e.getY());
	}

	private void showRenameDialog(NodeBookmark bookmark) {
		final String currentName = bookmark.getDescriptor().getName();
		final boolean currentOpensAsRoot = bookmark.getDescriptor().opensAsRoot();

		final String title = TextUtils.getText("bookmark.rename");
		final JTextField nameInput = new JTextField(currentName, 40);
		FocusRequestor.requestFocus(nameInput);

		if(JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
				KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
				nameInput,
				title,
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE)) {

			final String bookmarkName = nameInput.getText().trim();
			if (!bookmarkName.isEmpty()) {
				final NodeBookmarkDescriptor descriptor = new NodeBookmarkDescriptor(bookmarkName, currentOpensAsRoot);
				bookmarksController.addBookmark(bookmark.getNode(), descriptor);
			}
		}
	}

	private void toggleOpenAsRoot(NodeBookmark bookmark) {
		boolean newOpensAsRoot = !bookmark.getDescriptor().opensAsRoot();
		NodeBookmarkDescriptor newDescriptor = new NodeBookmarkDescriptor(
			bookmark.getDescriptor().getName(),
			newOpensAsRoot
		);
		bookmarksController.addBookmark(bookmark.getNode(), newDescriptor);
	}

	int getComponentIndex(FreeplaneToolBar toolbar, Component component) {
		Component[] components = toolbar.getComponents();
		for (int i = 0; i < components.length; i++) {
			if (components[i] == component) {
				return i;
			}
		}
		return -1;
	}
}