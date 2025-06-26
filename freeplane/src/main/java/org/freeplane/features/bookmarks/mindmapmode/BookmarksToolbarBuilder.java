package org.freeplane.features.bookmarks.mindmapmode;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
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
import javax.swing.border.Border;

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

public class BookmarksToolbarBuilder {
	private static final DataFlavor BOOKMARK_FLAVOR = new DataFlavor(NodeBookmark.class, "NodeBookmark");
	private static final Icon BOOKMARK_ROOT_ICON = IconStoreFactory.ICON_STORE.getUIIcon("bookmarkAsRoot.svg").getIcon();
	private static final Icon SELECTED_ROOT_ICON = IconStoreFactory.ICON_STORE.getUIIcon("currentRoot.svg").getIcon();
	private static final Icon SELECTED_SUBTREE_ICON = IconStoreFactory.ICON_STORE.getUIIcon("selectedSubtreeBookmark.svg").getIcon();
	private final BookmarksController bookmarksController;
	private final ModeController modeController;

	public BookmarksToolbarBuilder(ModeController modeController, BookmarksController bookmarksController) {
		this.modeController = modeController;
		this.bookmarksController = bookmarksController;
	}

	public void updateBookmarksToolbar(FreeplaneToolBar toolbar, MapModel map, IMapSelection selection) {
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
		dragSource.createDefaultDragGestureRecognizer(button, DnDConstants.ACTION_MOVE,
			new BookmarkDragGestureListener(button));

		DropTarget dropTarget = new DropTarget(button, DnDConstants.ACTION_MOVE, new BookmarkDropTargetListener(toolbar));

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

	private int getComponentIndex(FreeplaneToolBar toolbar, Component component) {
		Component[] components = toolbar.getComponents();
		for (int i = 0; i < components.length; i++) {
			if (components[i] == component) {
				return i;
			}
		}
		return -1;
	}

	private class BookmarkTransferable implements Transferable {
		private final int sourceIndex;

		public BookmarkTransferable(int sourceIndex) {
			this.sourceIndex = sourceIndex;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[]{BOOKMARK_FLAVOR};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return BOOKMARK_FLAVOR.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (!isDataFlavorSupported(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			return sourceIndex;
		}
	}

	private class BookmarkDragGestureListener implements DragGestureListener {
		private final JButton button;

		public BookmarkDragGestureListener(JButton button) {
			this.button = button;
		}

		@Override
		public void dragGestureRecognized(DragGestureEvent dge) {
			FreeplaneToolBar toolbar = (FreeplaneToolBar) button.getParent();
			int sourceIndex = BookmarksToolbarBuilder.this.getComponentIndex(toolbar, button);
			Transferable transferable = new BookmarkTransferable(sourceIndex);
			dge.startDrag(DragSource.DefaultMoveDrop, transferable);
		}
	}

	private class BookmarkDropTargetListener extends DropTargetAdapter {
		private final FreeplaneToolBar toolbar;
		private Border originalBorder;

		public BookmarkDropTargetListener(FreeplaneToolBar toolbar) {
			this.toolbar = toolbar;
		}

		@Override
		public void dragEnter(DropTargetDragEvent dtde) {
			JButton button = (JButton) dtde.getDropTargetContext().getComponent();
			originalBorder = button.getBorder();
		}

		@Override
		public void dragOver(DropTargetDragEvent dtde) {
			if (!dtde.isDataFlavorSupported(BOOKMARK_FLAVOR)) {
				dtde.rejectDrag();
				return;
			}

			try {
				int sourceIndex = (Integer) dtde.getTransferable().getTransferData(BOOKMARK_FLAVOR);
				JButton targetButton = (JButton) dtde.getDropTargetContext().getComponent();
				Point dropPoint = dtde.getLocation();

				DropValidation validation = validateDrop(sourceIndex, targetButton, dropPoint);
				if (!validation.isValid) {
					dtde.rejectDrag();
					clearVisualFeedback(targetButton);
					return;
				}

				dtde.acceptDrag(DnDConstants.ACTION_MOVE);
				showDropZoneIndicator(targetButton, validation.movesAfter);

			} catch (Exception e) {
				dtde.rejectDrag();
				clearVisualFeedback((JButton) dtde.getDropTargetContext().getComponent());
			}
		}

		@Override
		public void dragExit(DropTargetEvent dte) {
			JButton button = (JButton) dte.getDropTargetContext().getComponent();
			clearVisualFeedback(button);
		}

		private void showDropZoneIndicator(JButton button, boolean dropAfter) {
			Color highlightColor = button.getForeground();

			if (dropAfter) {
				button.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 0, 3, highlightColor),
					originalBorder));
			} else {
				button.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 3, 0, 0, highlightColor),
					originalBorder));
			}
		}

		private void clearVisualFeedback(JButton button) {
			if (originalBorder != null) {
				button.setBorder(originalBorder);
			}
		}

		private class DropValidation {
			final boolean isValid;
			final int finalTargetIndex;
			final boolean movesAfter;

			DropValidation(boolean isValid, int finalTargetIndex, boolean movesAfter) {
				this.isValid = isValid;
				this.finalTargetIndex = finalTargetIndex;
				this.movesAfter = movesAfter;
			}
		}

		private DropValidation validateDrop(int sourceIndex, JButton targetButton, Point dropPoint) {
			FreeplaneToolBar targetToolbar = (FreeplaneToolBar) targetButton.getParent();

			if (targetToolbar != toolbar) {
				return new DropValidation(false, -1, false);
			}

			int targetIndex = BookmarksToolbarBuilder.this.getComponentIndex(targetToolbar, targetButton);
			if (targetIndex == sourceIndex) {
				return new DropValidation(false, -1, false);
			}

			int buttonWidth = targetButton.getWidth();
			int leftThird = buttonWidth / 3;
			int rightThird = buttonWidth * 2 / 3;

			if (dropPoint.x > leftThird && dropPoint.x < rightThird) {
				return new DropValidation(false, -1, false);
			}

			boolean movesAfter = dropPoint.x >= rightThird;
			int finalTargetIndex = movesAfter
				? (sourceIndex < targetIndex ? targetIndex : targetIndex + 1)
				: (sourceIndex < targetIndex ? targetIndex - 1 : targetIndex);

			if (sourceIndex == finalTargetIndex) {
				return new DropValidation(false, -1, false);
			}

			return new DropValidation(true, finalTargetIndex, movesAfter);
		}

		private void executeMove(int sourceIndex, int finalTargetIndex) {
			MapModel map = (MapModel) toolbar.getClientProperty("bookmarksMap");
			MapBookmarks bookmarks = bookmarksController.getBookmarks(map);
			NodeBookmark bookmarkToMove = bookmarks.getBookmarks().get(sourceIndex);
			SwingUtilities.invokeLater(() ->
				bookmarksController.moveBookmark(bookmarkToMove.getNode(), finalTargetIndex));
		}

		@Override
		public void drop(DropTargetDropEvent dtde) {
			JButton targetButton = (JButton) dtde.getDropTargetContext().getComponent();
			try {
				if (!dtde.isDataFlavorSupported(BOOKMARK_FLAVOR)) {
					dtde.rejectDrop();
					return;
				}

				int sourceIndex = (Integer) dtde.getTransferable().getTransferData(BOOKMARK_FLAVOR);
				Point dropPoint = dtde.getLocation();

				DropValidation validation = validateDrop(sourceIndex, targetButton, dropPoint);
				if (!validation.isValid) {
					dtde.rejectDrop();
					return;
				}

				dtde.acceptDrop(DnDConstants.ACTION_MOVE);
				executeMove(sourceIndex, validation.finalTargetIndex);
				dtde.dropComplete(true);

			} catch (Exception e) {
				dtde.dropComplete(false);
			} finally {
				clearVisualFeedback(targetButton);
			}
		}
	}
}