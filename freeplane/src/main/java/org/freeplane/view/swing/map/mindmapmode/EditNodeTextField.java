/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.view.swing.map.mindmapmode;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.io.Writer;
import java.text.AttributedCharacterIterator;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultEditorKit.PasteAction;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position.Bias;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.StyledEditorKit.BoldAction;
import javax.swing.text.StyledEditorKit.ItalicAction;
import javax.swing.text.StyledEditorKit.StyledTextAction;
import javax.swing.text.StyledEditorKit.UnderlineAction;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLWriter;
import javax.swing.text.html.StyleSheet;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.ui.components.UndoEnabler;
import org.freeplane.core.ui.components.html.CssRuleBuilder;
import org.freeplane.core.ui.components.html.ScaledEditorKit;
import org.freeplane.core.ui.components.html.StyleSheetConfigurer;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.HtmlProcessor;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.clipboard.ClipboardAccessor;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.spellchecker.mindmapmode.SpellCheckerController;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.mindmapmode.EditNodeBase;
import org.freeplane.features.text.mindmapmode.EventBuffer;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.features.ui.IMapViewManager;
import org.freeplane.view.swing.map.FreeplaneTooltip;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.ZoomableLabel;
import org.freeplane.view.swing.map.ZoomableLabelUI;

import com.lightdev.app.shtm.SHTMLPanel;
import com.lightdev.app.shtm.SHTMLWriter;
import com.lightdev.app.shtm.bugfix.MapElementRemovingWorkaround;


/**
 * @author foltin
 */
public class EditNodeTextField extends EditNodeBase {
    private static class StrikeThroughAction extends StyledEditorKit.StyledTextAction {
        private static final String STRIKE_THROUGH = "text-decoration";
        private static final String STRIKE_VAL = "line-through";

        public StrikeThroughAction() {
            super("font-strikethrough");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JEditorPane editor = getEditor(e);
            if (editor != null) {
                StyledEditorKit kit = getStyledEditorKit(editor);
                MutableAttributeSet attr = kit.getInputAttributes();
                boolean strikethrough = (StyleConstants.isStrikeThrough(attr));
                SimpleAttributeSet sas = new SimpleAttributeSet();
                StyleConstants.setStrikeThrough(sas, !strikethrough);
                setCharacterAttributes(editor, sas, false);
            }
        }
    }

    private class MyNavigationFilter extends NavigationFilter {
    	private final JEditorPane textfield;
        public MyNavigationFilter(JEditorPane textfield) {
	        this.textfield = textfield;
        }

		/* (non-Javadoc)
         * @see javax.swing.text.NavigationFilter#moveDot(javax.swing.text.NavigationFilter.FilterBypass, int, javax.swing.text.Position.Bias)
         */
        @Override
		public void moveDot(final FilterBypass fb, int dot, final Bias bias) {
            dot = getValidPosition(dot);
            super.moveDot(fb, dot, bias);
        }

        /* (non-Javadoc)
         * @see javax.swing.text.NavigationFilter#setDot(javax.swing.text.NavigationFilter.FilterBypass, int, javax.swing.text.Position.Bias)
         */
        @Override
		public void setDot(final FilterBypass fb, int dot, final Bias bias) {
            dot = getValidPosition(dot);
            super.setDot(fb, dot, bias);
        }

        private int getValidPosition(int position) {
        	final HTMLDocument doc = (HTMLDocument) textfield.getDocument();
        	if (doc.getDefaultRootElement().getElementCount() > 1) {
        		final int startPos = doc.getDefaultRootElement().getElement(1).getStartOffset();
        		final int validPosition = Math.max(position, startPos);
        		return validPosition;
        	}
        	return position;
        }
    }

	private static class InputMethodInUseListener implements InputMethodListener {
		private boolean imeInUse = false;

		@Override
		public void inputMethodTextChanged(InputMethodEvent event) {
			updateImeInUseState(event);
		}

		@Override
		public void caretPositionChanged(InputMethodEvent event) {
			updateImeInUseState(event);
		}

		public boolean isIMEInUse(){
			return imeInUse;
		}

		private void updateImeInUseState(InputMethodEvent event) {
	        AttributedCharacterIterator aci = event.getText();
			if(aci != null) {
				int inputLen = aci.getEndIndex() - aci.getBeginIndex();
				int committedLen = event.getCommittedCharacterCount();
				imeInUse = inputLen > 0 && inputLen != committedLen;
			}
            else
	            imeInUse = false;
        }

	}

	private int extraWidth;
	final private boolean layoutMapOnTextChange;

	private final class MyDocumentListener implements DocumentListener {
		private boolean updateRunning = false;
		@Override
		public void changedUpdate(final DocumentEvent e) {
			onUpdate();
		}

		private void onUpdate() {
			if(updateRunning){
				return;
			}
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					updateRunning = true;
					layout();
					updateRunning = false;
				}
			});
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			onUpdate();
		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			onUpdate();
		}
	}

	private void layout() {
		if (textfield == null) {
			return;
		}
		final int lastWidth = textfield.getWidth();
		final int lastHeight = textfield.getHeight();
		final boolean lineWrap = lastWidth == maxWidth;
		Dimension preferredSize = textfield.getPreferredSize();
		if (!lineWrap) {
			preferredSize.width ++;
			if (preferredSize.width > maxWidth) {
				setLineWrap();
				preferredSize = textfield.getPreferredSize();
			}
			else {
				if (preferredSize.width < lastWidth) {
					preferredSize.width = lastWidth;
				}
				else {
					preferredSize.width = Math.min(preferredSize.width + extraWidth, maxWidth);
					if (preferredSize.width == maxWidth) {
						setLineWrap();
					}
				}
			}
		}
		else {
			preferredSize.width = Math.max(maxWidth, preferredSize.width);
		}
		if(preferredSize.width != lastWidth){
			preferredSize.height = lastHeight;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					layout();
				}
			});
		}
		else{
			preferredSize.height = Math.max(preferredSize.height, lastHeight);
		}
		if (preferredSize.width == lastWidth && preferredSize.height == lastHeight) {
			textfield.repaint();
			return;
		}
		textfield.setSize(preferredSize);
		if(layoutMapOnTextChange)
			parent.setPreferredSize(new Dimension(preferredSize.width + horizontalSpace , preferredSize.height + verticalSpace));
		textfield.revalidate();
		final NodeView nodeView = (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class, parent);
		final MapView mapView = (MapView) SwingUtilities.getAncestorOfClass(MapView.class, nodeView);
		if(mapView == null)
			return;
		if(layoutMapOnTextChange)
			mapView.scrollNodeToVisible(nodeView);
		else
			mapView.scrollRectToVisible(textfield.getBounds());
	}

	private void setLineWrap() {
		if(null != textfield.getClientProperty("EditNodeTextField.linewrap") || inputMethodInUseListener.isIMEInUse()){
			return;
		}

	    final HTMLDocument document = (HTMLDocument) textfield.getDocument();
	    document.getStyleSheet().addRule("body { width: " + maxWidth + "}");
	    // bad hack: call "setEditable" only to update view
	    textfield.setEditable(false);
	    textfield.setEditable(true);
	    textfield.putClientProperty("EditNodeTextField.linewrap", true);
    }

	private static final int SPLIT_KEY_CODE;
	static {
		String rawLabel = TextUtils.getRawText("split");
		final int mnemoSignIndex = rawLabel.indexOf('&');
		if (mnemoSignIndex >= 0 && mnemoSignIndex + 1 < rawLabel.length()) {
			final char charAfterMnemoSign = rawLabel.charAt(mnemoSignIndex + 1);
			if (charAfterMnemoSign != ' ') {
				SPLIT_KEY_CODE = charAfterMnemoSign;
			}
			else SPLIT_KEY_CODE = -1;
		}
		else SPLIT_KEY_CODE = -1;
	}
	private class TextFieldListener implements KeyListener, FocusListener, MouseListener, AWTEventListener {
		private static final int KEYSTROKE_MODIFIERS = KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK;
		final int CANCEL = 2;
		final int EDIT = 1;
		Integer eventSource = EDIT;
		private boolean popupShown;

		public TextFieldListener() {
		}

		private void conditionallyShowPopup(final MouseEvent e) {
			if (Compat.isPopupTrigger(e)) {
				final JComponent component = (JComponent) e.getComponent();
				final JPopupMenu popupMenu = createPopupMenu(component);
				popupShown = true;
				popupMenu.show(component, e.getX(), e.getY());
				e.consume();
			}
		}



		@Override
		public void focusGained(final FocusEvent e) {
			popupShown = false;
			ModeController modeController = Controller.getCurrentModeController();
            modeController.setBlocked(true);
            ((MTextController)modeController.getExtension(TextController.class)).setCurrentBlockingEditor(EditNodeTextField.this);
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.FOCUS_EVENT_MASK);
		}


		@Override
		public void eventDispatched(AWTEvent event) {
		    if (event instanceof FocusEvent) {
		        FocusEvent fe = (FocusEvent) event;

		        // If focus is moving away from the text editor
		        if (textfield != null
		        		&& fe.getID() == FocusEvent.FOCUS_GAINED && ! fe.isTemporary()
		        		&& fe.getComponent() != textfield
		        		&& fe.getOppositeComponent() != textfield
		        		&& fe.getOppositeComponent() != null) {
		        	focusLost(new FocusEvent(textfield, FocusEvent.FOCUS_LOST, false, fe.getComponent()));
		        }
		    }
		}

		@Override
		public void focusLost(final FocusEvent e) {
			if (textfield == null || !textfield.isVisible() || eventSource == CANCEL || popupShown) {
				return;
			}
			if (e == null) {
				submitText();
				hideMe();
				eventSource = CANCEL;
				return;
			}
			Component oppositeComponent = e.getOppositeComponent();
			if (e.isTemporary() && (oppositeComponent == null ||
					oppositeComponent == SwingUtilities.getRootPane(nodeView)
					|| SwingUtilities.getAncestorOfClass(FreeplaneTooltip.class, oppositeComponent) != null)) {
				return;
			}
			Window myWindow = SwingUtilities.getWindowAncestor(e.getComponent());
			if (myWindow != null && oppositeComponent != null && SwingUtilities.getWindowAncestor(oppositeComponent)
					!= myWindow) {
				myWindow.addWindowFocusListener(new WindowFocusListener() {

					@Override
					public void windowLostFocus(WindowEvent e) {
					}

					@Override
					public void windowGainedFocus(WindowEvent e) {
						myWindow.removeWindowFocusListener(this);
						MainView mainView = nodeView.getMainView();
						if(mainView != null)
							mainView.requestFocusInWindow();
					}
				});
			}
			submitText();
			hideMe();
		}

		private void submitText() {
	        submitText(getNewText());
        }

		private void submitText(final String output) {
			getEditControl().ok(output);
        }

		@Override
		public void keyPressed(final KeyEvent e) {
			if (eventSource == CANCEL||textfield==null) {
				return;
			}
			final int keyCode = e.getKeyCode();
			switch (keyCode) {
				case KeyEvent.VK_ESCAPE:
					if (e.isControlDown() || e.isMetaDown())
						break;
					eventSource = CANCEL;
					hideMe();
					getEditControl().cancel();
					nodeView.requestFocusInWindow();
					e.consume();
					break;
				case KeyEvent.VK_ENTER: {
					if (e.isControlDown() || e.isMetaDown())
						break;
					if (e.isAltDown() || e.isShiftDown()) {
						e.consume();
						final Component component = e.getComponent();
						final KeyEvent keyEvent = new KeyEvent(component, e.getID(), e.getWhen(), 0, keyCode, e
						    .getKeyChar(), e.getKeyLocation());
						SwingUtilities.processKeyBindings(keyEvent);
						break;
					}
					final String output = getNewText();
					e.consume();
					eventSource = CANCEL;
					hideMe();
					submitText(output);
					nodeView.requestFocusInWindow();
				}
				break;
				case KeyEvent.VK_SPACE:
					if (e.isControlDown() || e.isMetaDown())
						break;
					e.consume();
					break;
				default:
					if(isSplitActionTriggered(e) && getEditControl().canSplit()){
						eventSource = CANCEL;
						final String output = getNewText();
						final int caretPosition = textfield.getCaretPosition();
						hideMe();
						getEditControl().split(output, caretPosition);
						nodeView.requestFocusInWindow();
						e.consume();
					}
					break;
			}
		}

		protected boolean isSplitActionTriggered(final KeyEvent e) {
			final int keyCode = e.getKeyCode();
			if (keyCode == SPLIT_KEY_CODE && keyCode != -1 && e.isAltDown()&& !e.isAltGraphDown()&& !e.isControlDown() && ! Compat.isMacOsX())
				return true;
			final KeyStroke splitNodeHotKey = ResourceController.getResourceController().getAcceleratorManager().getAccelerator("SplitNode");
			return splitNodeHotKey != null && splitNodeHotKey.getKeyCode() == keyCode  &&
					(e.getModifiersEx() & KEYSTROKE_MODIFIERS)  == (splitNodeHotKey.getModifiers() & KEYSTROKE_MODIFIERS);
		}

		@Override
		public void keyReleased(final KeyEvent e) {
		}

		@Override
		public void keyTyped(final KeyEvent e) {
		}

		@Override
		public void mouseClicked(final MouseEvent ev) {
			if (textfield != null && (ev.getModifiers() & MouseEvent.CTRL_MASK) != 0) {
				final String linkURL = HtmlUtils.getURLOfExistingLink((HTMLDocument) textfield.getDocument(), textfield.viewToModel(ev.getPoint()));
				if (linkURL != null) {
					try {
						LinkController.getController().loadURI(nodeView.getNode(), LinkController.createHyperlink(linkURL));
					} catch (Exception e) {
						LogUtils.warn(e);
					}
				}
			}
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
		}

		@Override
		public void mouseExited(final MouseEvent e) {
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			conditionallyShowPopup(e);
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			conditionallyShowPopup(e);
		}
	}

	private class MapViewChangeListener implements IMapViewChangeListener{
		@Override
		public void beforeViewChange(Component oldView, Component newView) {
			final String output = getNewText();
			hideMe();
			getEditControl().ok(output);
        }
	}

	private JEditorPane textfield;
	final private InputMethodInUseListener inputMethodInUseListener;
	private final DocumentListener documentListener;
	private int maxWidth;

	@SuppressWarnings("serial")
    public EditNodeTextField(final NodeModel node, final ZoomableLabel parent, final String text, final IEditControl editControl) {
		super(node, text, true, editControl);
		this.parent = parent;
		this.layoutMapOnTextChange = ResourceController.getResourceController().getBooleanProperty("layout_map_on_text_change");
		documentListener = new MyDocumentListener();

		pasteAction = new DefaultEditorKit.PasteAction(){

			@Override
			public void actionPerformed(ActionEvent e) {
				JTextComponent target = getTextComponent(e);
				if (target == null) {
					return;
				}
				final Transferable contents = ClipboardAccessor.getInstance().getClipboardContents();
				if(contents !=  null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)){
					try {
						String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
						target.replaceSelection(text);
					}
					catch (Exception ex) {
					}
				}
			}
		};

		boldAction = new StyledEditorKit.BoldAction();
		boldAction.putValue(Action.NAME, TextUtils.getText("BoldAction.text"));
		boldAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control B"));

		italicAction = new StyledEditorKit.ItalicAction();
		italicAction.putValue(Action.NAME, TextUtils.getText("ItalicAction.text"));
		italicAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control I"));

		underlineAction = new StyledEditorKit.UnderlineAction();
		underlineAction.putValue(Action.NAME, TextUtils.getText("UnderlineAction.text"));
		underlineAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control U"));

		strikeThroughAction = new StrikeThroughAction();
		strikeThroughAction.putValue(Action.NAME, TextUtils.getText("StrikeThroughAction.text"));
		strikeThroughAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift U"));

		redAction = new CharacterColorAction(TextUtils.getText("simplyhtml.redFontColorLabel"), CSS.Attribute.COLOR, SHTMLPanel.DARK_RED, SHTMLPanel.LIGHT_RED);
		redAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control R"));

		greenAction = new CharacterColorAction(TextUtils.getText("simplyhtml.greenFontColorLabel"), CSS.Attribute.COLOR, SHTMLPanel.DARK_GREEN, SHTMLPanel.LIGHT_GREEN);
		greenAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control G"));

		blueAction = new CharacterColorAction(TextUtils.getText("simplyhtml.blueFontColorLabel"), CSS.Attribute.COLOR, SHTMLPanel.DARK_BLUE, SHTMLPanel.LIGHT_BLUE);
		blueAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control L"));

		blackAction = new CharacterColorAction(TextUtils.getText("simplyhtml.blackFontColorLabel"), CSS.Attribute.COLOR, Color.BLACK, Color.WHITE);
		blackAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control K"));

		defaultColorAction = new ExtendedEditorKit.RemoveStyleAttributeAction(CSS.Attribute.COLOR, TextUtils.getText("simplyhtml.removeFontColorLabel"));
		defaultColorAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control D"));

        redHighlightAction = new CharacterColorAction(TextUtils.getText("simplyhtml.redHighlightColorLabel"), CSS.Attribute.BACKGROUND_COLOR, SHTMLPanel.LIGHT_RED.brighter(), SHTMLPanel.DARK_RED.darker());
        redHighlightAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift R"));

        greenHighlightAction = new CharacterColorAction(TextUtils.getText("simplyhtml.greenHighlightColorLabel"), CSS.Attribute.BACKGROUND_COLOR, SHTMLPanel.LIGHT_GREEN.brighter(), SHTMLPanel.DARK_GREEN.darker());
        greenHighlightAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift G"));

        blueHighlightAction = new CharacterColorAction(TextUtils.getText("simplyhtml.blueHighlightColorLabel"), CSS.Attribute.BACKGROUND_COLOR, SHTMLPanel.LIGHT_BLUE.brighter(), SHTMLPanel.LIGHT_BLUE.darker());
        blueHighlightAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift L"));

        yellowHighlightAction = new CharacterColorAction(TextUtils.getText("simplyhtml.yellowHighlightColorLabel"), CSS.Attribute.BACKGROUND_COLOR, Color.YELLOW, Color.ORANGE.darker());
        yellowHighlightAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift Y"));

        removeHighlightAction = new ExtendedEditorKit.RemoveStyleAttributeAction(CSS.Attribute.BACKGROUND_COLOR, TextUtils.getText("simplyhtml.removeHighlightColorLabel"));
        removeHighlightAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control shift D"));

        removeFormattingAction = new ExtendedEditorKit.RemoveStyleAttributeAction(null, TextUtils.getText("simplyhtml.clearFormatLabel"));
		removeFormattingAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control T"));

		inputMethodInUseListener = new InputMethodInUseListener();
		if(editControl != null ){
			final ModeController modeController = Controller.getCurrentModeController();
			final MTextController textController = (MTextController) TextController.getController(modeController);
			textfield = textController.createEditorPane(MTextController.NODE_TEXT);
			textfield.setNavigationFilter(new MyNavigationFilter(textfield));
			textfield.addInputMethodListener(inputMethodInUseListener);
		}
	}

	public String getNewText() {
		final SHTMLWriter shtmlWriter = new SHTMLWriter((HTMLDocument) textfield.getDocument());
		try {
	        shtmlWriter.write();
        }
        catch (Exception e) {
	        LogUtils.severe(e);
        }
		return shtmlWriter.toString();
    }

	private void hideMe() {
		if (textfield == null) {
			return;
		}
		Toolkit.getDefaultToolkit().removeAWTEventListener((AWTEventListener) textFieldListener);
		ModeController modeController = nodeView.getMap().getModeController();
        modeController.setBlocked(false);
		((MTextController)modeController.getExtension(TextController.class)).unsetCurrentBlockingEditor(EditNodeTextField.this);
		final JEditorPane textfield = this.textfield;
		this.textfield = null;
		textfield.getDocument().removeDocumentListener(documentListener);
		final IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
		mapViewManager.removeMapViewChangeListener(mapViewChangeListener);
		mapViewChangeListener = null;
		parent.preserveLayout(null);
		if(nodeView.isShowing()) {
			nodeView.update();
			preserveRootNodeLocationOnScreen();
		}
		final Dimension textFieldSize = textfield.getSize();
		final Point textFieldCoordinate = new Point();
		final MapView mapView = nodeView.getMap();
		UITools.convertPointToAncestor(textfield, textFieldCoordinate, mapView);
		Container textFieldParentComponent = textfield.getParent();
		if(textFieldParentComponent != null)
		    textFieldParentComponent.remove(textfield);
		mapView.onEditingFinished(parent);
		parent.revalidate();
		parent.repaint();
		mapView.repaint(textFieldCoordinate.x, textFieldCoordinate.y, textFieldSize.width, textFieldSize.height);
	}

	private final ZoomableLabel parent;
	private NodeView nodeView;
	private Font font;
	private final PasteAction pasteAction;
	private final BoldAction boldAction;
	private final ItalicAction italicAction;
	private final UnderlineAction underlineAction;
	private final StrikeThroughAction strikeThroughAction;

	private final CharacterColorAction redAction;
	private final CharacterColorAction greenAction;
	private final CharacterColorAction blueAction;
	private final CharacterColorAction blackAction;
	private final StyledTextAction defaultColorAction;

	private final CharacterColorAction redHighlightAction;
	private final CharacterColorAction greenHighlightAction;
	private final CharacterColorAction blueHighlightAction;
	private final CharacterColorAction yellowHighlightAction;
	private final StyledTextAction removeHighlightAction;

	private StyledTextAction removeFormattingAction;
	private int verticalSpace;
	private int horizontalSpace;
	private MapViewChangeListener mapViewChangeListener;


    @Override
    protected JPopupMenu createPopupMenu(JComponent component) {
		JPopupMenu menu = super.createPopupMenu(component);


		Action undoAction = component.getActionMap().get(UndoEnabler.UNDO_ACTION);
		if(undoAction != null) {
		    menu.add(undoAction);
		}

        Action redoAction = component.getActionMap().get(UndoEnabler.REDO_ACTION);
        if(redoAction != null) {
            menu.add(redoAction);
        }

        JMenu formatMenu = new JMenu(TextUtils.getText("simplyhtml.formatLabel"));
	    menu.add(formatMenu);
		if (textfield.getSelectionStart() == textfield.getSelectionEnd()){
			formatMenu.setEnabled(false);
			return menu;
		}
	    formatMenu.add(boldAction);
	    formatMenu.add(italicAction);
	    formatMenu.add(underlineAction);
	    formatMenu.add(strikeThroughAction);

	    formatMenu.add(redAction);
	    formatMenu.add(greenAction);
	    formatMenu.add(blueAction);
	    formatMenu.add(blackAction);
	    formatMenu.add(defaultColorAction);

        formatMenu.add(redHighlightAction);
        formatMenu.add(greenHighlightAction);
        formatMenu.add(blueHighlightAction);
        formatMenu.add(yellowHighlightAction);
        formatMenu.add(removeHighlightAction);

        formatMenu.add(removeFormattingAction);
		return menu;
    }

	/* (non-Javadoc)
	 * @see org.freeplane.view.swing.map.INodeTextField#show()
	 */
	@SuppressWarnings("serial")
    @Override
	public void show(final Window window) {
		final ModeController modeController = Controller.getCurrentModeController();
		final IMapViewManager viewController = modeController.getController().getMapViewManager();
		final MTextController textController = (MTextController) TextController.getController(modeController);
		nodeView = (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class, parent);
		font = parent.getFont();
		float zoom = viewController.getZoom();
		if (zoom != 1F) {
			final float fontSize = (int) (Math.rint(font.getSize() * zoom));
			font = font.deriveFont(fontSize);
		}
		final HTMLEditorKit kit = new ScaledEditorKit(){
			@Override
			public void write(Writer out, Document doc, int pos, int len) throws IOException, BadLocationException {
				if (doc instanceof HTMLDocument) {
					HTMLWriter w = new SHTMLWriter(out, (HTMLDocument) doc, pos, len);
					w.write();
				}
				else {
					super.write(out, doc, pos, len);
				}
			}
		};
		textfield.setEditorKit(kit);
		textfield.setComponentOrientation(nodeView.getMainView().getComponentOrientation());

		final InputMap inputMap = textfield.getInputMap();
		final ActionMap actionMap = textfield.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "ignore-insert-tab");
        inputMap.put(KeyStroke.getKeyStroke('\t'), "ignore-insert-tab");

		actionMap.put(DefaultEditorKit.pasteAction, pasteAction);

		inputMap.put((KeyStroke) boldAction.getValue(Action.ACCELERATOR_KEY), "boldAction");
		actionMap.put("boldAction",boldAction);

		inputMap.put((KeyStroke) italicAction.getValue(Action.ACCELERATOR_KEY), "italicAction");
		actionMap.put("italicAction", italicAction);

		inputMap.put((KeyStroke) underlineAction.getValue(Action.ACCELERATOR_KEY), "underlineAction");
		actionMap.put("underlineAction", underlineAction);

		inputMap.put((KeyStroke) strikeThroughAction.getValue(Action.ACCELERATOR_KEY), "strikethroughAction");
		actionMap.put("strikethroughAction", strikeThroughAction);

		inputMap.put((KeyStroke) redAction.getValue(Action.ACCELERATOR_KEY), "redAction");
		actionMap.put("redAction", redAction);

		inputMap.put((KeyStroke) greenAction.getValue(Action.ACCELERATOR_KEY), "greenAction");
		actionMap.put("greenAction", greenAction);

		inputMap.put((KeyStroke) blueAction.getValue(Action.ACCELERATOR_KEY), "blueAction");
		actionMap.put("blueAction", blueAction);

        inputMap.put((KeyStroke) blackAction.getValue(Action.ACCELERATOR_KEY), "blackAction");
        actionMap.put("blackAction", blackAction);

        inputMap.put((KeyStroke) defaultColorAction.getValue(Action.ACCELERATOR_KEY), "defaultColorAction");
        actionMap.put("defaultColorAction", defaultColorAction);

        inputMap.put((KeyStroke) redHighlightAction.getValue(Action.ACCELERATOR_KEY), "redHighlightAction");
        actionMap.put("redHighlightAction", redHighlightAction);

        inputMap.put((KeyStroke) greenHighlightAction.getValue(Action.ACCELERATOR_KEY), "greenHighlightAction");
        actionMap.put("greenHighlightAction", greenHighlightAction);

        inputMap.put((KeyStroke) blueHighlightAction.getValue(Action.ACCELERATOR_KEY), "blueHighlightAction");
        actionMap.put("blueHighlightAction", blueHighlightAction);

        inputMap.put((KeyStroke) yellowHighlightAction.getValue(Action.ACCELERATOR_KEY), "yellowHighlightAction");
        actionMap.put("yellowHighlightAction", yellowHighlightAction);

        inputMap.put((KeyStroke) removeHighlightAction.getValue(Action.ACCELERATOR_KEY), "removeHighlightAction");
        actionMap.put("removeHighlightAction", removeHighlightAction);

		inputMap.put((KeyStroke) removeFormattingAction.getValue(Action.ACCELERATOR_KEY), "removeFormattingAction");
		actionMap.put("removeFormattingAction", removeFormattingAction);

		final Color nodeTextColor = parent.getUnselectedForeground();
		textfield.setCaretColor(nodeTextColor);
		final StringBuilder ruleBuilder = new StringBuilder(100);
		ruleBuilder.append("body {");
		final int labelHorizontalAlignment = parent.getHorizontalAlignment();
		ruleBuilder.append(new CssRuleBuilder()
				.withCSSFont(font, UITools.FONT_SCALE_FACTOR)
				.withColor(nodeTextColor)
				.withBackground(getBackground())
				.withAlignment(labelHorizontalAlignment));
		ruleBuilder.append("}\n");
		final HTMLDocument document = (HTMLDocument) textfield.getDocument();
		final StyleSheet styleSheet = document.getStyleSheet();
		StyleSheet ownStyleSheet =StyleSheetConfigurer.createDefaultStyleSheet();
		ownStyleSheet.addRule(ruleBuilder.toString());
		styleSheet.addStyleSheet(ownStyleSheet);
		styleSheet.addStyleSheet(parent.getStyleSheet());
		MapElementRemovingWorkaround.removeAllMapElements(document);
		HtmlProcessor.configureUnknownTags(document);
		textfield.setText(getText());
		UndoEnabler.addUndoRedoFunctionality(textfield);
		final MapView mapView = nodeView.getMap();
		if(! mapView.isValid())
			mapView.validate();
		final NodeStyleController nsc = NodeStyleController.getController(modeController);
		maxWidth = Math.max(mapView.getLayoutSpecificMaxNodeWidth(),
				Math.max(mapView.getZoomed(nsc.getMaxWidth(node, nodeView.getStyleOption()).toBaseUnitsRounded()), parent.getWidth()));
		boolean isTextPlacedUnderIcon = parent.getVerticalTextPosition() == SwingConstants.BOTTOM;
		int reservedIconSpace = 0;
		if(! isTextPlacedUnderIcon) {
			final Icon icon = parent.getIcon();
			if(icon != null){
				reservedIconSpace = mapView.getZoomed(icon.getIconWidth() + parent.getIconTextGap());
				maxWidth -= reservedIconSpace;
			}
		}
		Insets parentInsets = parent.getZoomedInsets();
		maxWidth -= parentInsets.left + parentInsets.right;
		extraWidth = ResourceController.getResourceController().getIntProperty("editor_extra_width", 80);
		extraWidth = mapView.getZoomed(extraWidth);
		final TextFieldListener textFieldListener = new TextFieldListener();
		this.textFieldListener = textFieldListener;
		textfield.addFocusListener(textFieldListener);
		textfield.addKeyListener(textFieldListener);
		textfield.addMouseListener(textFieldListener);
		mapViewChangeListener = new MapViewChangeListener();
		Controller.getCurrentController().getMapViewManager().addMapViewChangeListener(mapViewChangeListener);
		SpellCheckerController.getController().enableAutoSpell(textfield, true);
		mapView.scrollNodeToVisible(nodeView);
		assert( parent.isValid());
		final int textFieldBorderWidth = 2;
		textfield.setBorder(new MatteBorder(textFieldBorderWidth, textFieldBorderWidth, textFieldBorderWidth, textFieldBorderWidth,
				MapView.drawsRectangleForSelection() ? MapView.getSelectionRectangleColor() : nodeView.getTextBackground()));
		final Dimension textFieldMinimumSize = textfield.getPreferredSize();
		textFieldMinimumSize.width = 1 + textFieldMinimumSize.width * 21 / 20;
        if(textFieldMinimumSize.width < extraWidth)
            textFieldMinimumSize.width = extraWidth;
        int minWidth = mapView.getZoomed(10);
        maxWidth = Math.max(maxWidth, minWidth);
        if(textFieldMinimumSize.width < minWidth)
            textFieldMinimumSize.width = minWidth;
		if (textFieldMinimumSize.width > maxWidth) {
			textFieldMinimumSize.width = maxWidth;
			setLineWrap();
			textFieldMinimumSize.height = textfield.getPreferredSize().height;
		}

		final EventBuffer eventQueue = MTextController.getController().getEventQueue();
		AWTEvent firstEvent = eventQueue.getFirstEvent();

		final ZoomableLabelUI parentUI = parent.getUI();
		final Rectangle textR = parentUI.getAvailableTextR(parent);
		Point mouseEventPoint = null;
		if (firstEvent instanceof MouseEvent) {
			MouseEvent currentEvent = (MouseEvent) firstEvent;
			MouseEvent mouseEvent = currentEvent;
			if(mouseEvent.getComponent().equals(parent)){
				mouseEventPoint = mouseEvent.getPoint();
				mouseEventPoint.x -= textR.x;
				mouseEventPoint.y -= textR.y;
			}
		}


		textFieldMinimumSize.width = Math.max(textFieldMinimumSize.width, textR.width + 2 * textFieldBorderWidth);
		textFieldMinimumSize.height = Math.max(textFieldMinimumSize.height, textR.height + 2 * textFieldBorderWidth);
		int textFieldX = Math.max(0, textR.x  - textFieldBorderWidth);
		int textFieldY = Math.max(0, textR.y  - textFieldBorderWidth);
        verticalSpace = Math.max(textFieldY, parent.getHeight() - textFieldMinimumSize.height);
		final Dimension newParentSize = new Dimension(textFieldX + textFieldMinimumSize.width + parentInsets.right,
				verticalSpace + textFieldMinimumSize.height);
		if (parent.getEffectiveHorizontalTextPosition() == SwingConstants.LEFT)
			newParentSize.width += reservedIconSpace;
		horizontalSpace = newParentSize.width - textFieldMinimumSize.width;
		final Point location = new Point(textFieldX, textFieldY);

		final int widthAddedToTextField = textFieldMinimumSize.width - (textR.width + 2 * textFieldBorderWidth);
		if(widthAddedToTextField > 0){
			switch(labelHorizontalAlignment){
			case SwingConstants.CENTER:
				if(mouseEventPoint != null)
					mouseEventPoint.x += widthAddedToTextField / 2;
				break;
			case SwingConstants.RIGHT:
				if(mouseEventPoint != null)
					mouseEventPoint.x += widthAddedToTextField;
				break;
			}
		}

        preserveRootNodeLocationOnScreen();
		parent.preserveLayout(newParentSize);
		parent.setText("");
        mapView.onEditingStarted(parent);
        if(getEditControl().getEditType() == EditedComponent.TEXT)
        	nodeView.setTextBackground(getBackground());

		if(! layoutMapOnTextChange) {
			mapView.doLayout();
			UITools.convertPointToAncestor(parent, location, mapView);
		}

		textfield.setBounds(location.x, location.y, textFieldMinimumSize.width, textFieldMinimumSize.height);
		if(layoutMapOnTextChange)
			parent.add(textfield, 0);
		else
			mapView.add(textfield, 0);

		redispatchKeyEvents(textfield, firstEvent);
		if (firstEvent instanceof MouseEvent) {
			final int caretPosition;
			final int textLength = document.getLength();
			if(mouseEventPoint != null)
				caretPosition = Math.max(0, Math.min(textLength, textfield.viewToModel(mouseEventPoint)));
			else
				caretPosition = textLength;
			textfield.setCaretPosition(caretPosition);
		}
		document.addDocumentListener(documentListener);
		if(textController.isMinimized(node)
				|| textfield.getPreferredSize().height > textfield.getHeight()){
			layout();
		}
		textfield.repaint();
		textfield.requestFocusInWindow();
	}

	private void preserveRootNodeLocationOnScreen() {
	    nodeView.getMap().preserveRootNodeLocationOnScreen();
	}
}
