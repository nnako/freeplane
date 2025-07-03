package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
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
	private static final String COPY_ACTION_KEY = "bookmarkCopy";
	private static final String PASTE_ACTION_KEY = "bookmarkPaste";
	private static final String ENTER_ACTION_KEY = "bookmarkEnter";
	private static final ButtonEnterAction CLICK_ACTION = new ButtonEnterAction();
	private static final int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	private static final KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask);
	private static final KeyStroke pasteKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask);
	private static final KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
	private static final KeyStroke altEnterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_MASK);

	@SuppressWarnings("serial")
	private static class ButtonCopyAction extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			BookmarkButton button = (BookmarkButton) e.getSource();
			Component parent = button.getParent();
			if (parent instanceof BookmarkToolbar) {
				BookmarkToolbar toolbar = (BookmarkToolbar) parent;
				BookmarkClipboardHandler clipboardHandler = toolbar.getClipboardHandler();
				clipboardHandler.copyBookmark(button);
			}
		}
	}

	@SuppressWarnings("serial")
	private static class ButtonPasteAction extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			BookmarkButton button = (BookmarkButton) e.getSource();
			Component parent = button.getParent();
			if (parent instanceof BookmarkToolbar) {
				BookmarkToolbar toolbar = (BookmarkToolbar) parent;
				BookmarkClipboardHandler clipboardHandler = toolbar.getClipboardHandler();
				clipboardHandler.pasteBookmarkAtButton(button);
			}
		}
	}

	@SuppressWarnings("serial")
	private static class ButtonEnterAction extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent e) {
			AbstractButton button = (AbstractButton) e.getSource();
			button.doClick();
			button.requestFocus();
		}
	}

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
		@SuppressWarnings("unused")
		final DropTarget dropTarget = new DropTarget(component, DnDConstants.ACTION_NONE, new DropTargetAdapter() {
			@Override
			public void drop(DropTargetDropEvent dtde) {
				dtde.rejectDrop();
			}
		});
		if(component instanceof AbstractButton) {
			final AbstractButton button = (AbstractButton)component;
			button.getInputMap().put(enterKeyStroke, ENTER_ACTION_KEY);
			button.getActionMap().put(ENTER_ACTION_KEY, CLICK_ACTION);
		}
	}

	private void applyAction(ActionEvent action) {
		final BookmarkButton button = (BookmarkButton) action.getSource();
		if((action.getModifiers() & ActionEvent.CTRL_MASK) != 0)
			showBookmarkPopupMenu(button);
		else {
			final NodeBookmark bookmark = button.getBookmark();
			if((action.getModifiers() & ActionEvent.ALT_MASK) != 0)
				bookmark.alternativeOpen();
			else
				bookmark.open();
		}
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
		InputMap inputMap = button.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = button.getActionMap();


		inputMap.put(copyKeyStroke, COPY_ACTION_KEY);
		inputMap.put(pasteKeyStroke, PASTE_ACTION_KEY);
		inputMap.put(enterKeyStroke, ENTER_ACTION_KEY);
		inputMap.put(altEnterKeyStroke, ENTER_ACTION_KEY);

		actionMap.put(COPY_ACTION_KEY, new ButtonCopyAction());
		actionMap.put(PASTE_ACTION_KEY, new ButtonPasteAction());
		actionMap.put(ENTER_ACTION_KEY, CLICK_ACTION);

		Action showContextMenuAction = new AbstractAction("showContextMenu") {
			@Override
			public void actionPerformed(ActionEvent e) {
				BookmarkButton button = (BookmarkButton) e.getSource();
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