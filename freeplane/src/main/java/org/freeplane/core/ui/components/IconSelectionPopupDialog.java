/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  @author <a href="mailto:labe@users.sourceforge.net">Lars Berning</a>
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
package org.freeplane.core.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.resources.WindowConfigurationStorage;
import org.freeplane.core.resources.components.GrabKeyDialog;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.icon.IconDescription;
import org.freeplane.features.icon.factory.IconFactory;
import org.freeplane.features.icon.mindmapmode.FastAccessableIcons.ActionPanel;

public class IconSelectionPopupDialog extends JDialog implements MouseListener {

	private static final String WINDOW_CONFIG_PROPERTY = "icon_selection_window_configuration";

    private static int BORDER_THICKNESS = 2;

	private static final Border USUAL = BorderFactory.createEmptyBorder(BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS);
    private static final Border HIGHLIGHTED =  BorderFactory.createLineBorder(Color.RED, BORDER_THICKNESS);
    /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static String lastSearchText = "";
	private final JLabel descriptionLabel;
	final private List<JLabel> iconLabels;
	final private JPanel iconPanel = new JPanel();
	final private List<? extends IconDescription> icons;
	private final JTextField filterTextField;
	private int mModifiers;
	final private int numOfIcons;
	private int selectedIconIndex;
	private JLabel selected;
    private Timer filterTimer;

	private ActionListener listener;
	private JCheckBox closeAfterSelection;

	private Box statusPanel;

	private final MouseListener focusRequester = new MouseAdapter() {
		@Override
		public void mouseEntered(MouseEvent e) {
			e.getComponent().requestFocusInWindow();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			filterTextField.requestFocusInWindow();
		}
	};

	private final ActionListener actionPanelActionListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if(closeAfterSelection != null && closeAfterSelection.isSelected())
				dispose();
		}
	};

	public IconSelectionPopupDialog(final Frame frame, final List<? extends IconDescription> icons) {
		super(frame, TextUtils.getText("select_icon"));
		Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
		this.icons = icons;
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent we) {
				close();
			}
		});
		numOfIcons = icons.size();
        final int singleIconSize = (int) ((IconFactory.DEFAULT_UI_ICON_HEIGHT.toBaseUnits()+ 0.5) * 1.1);
        int xDimension = Math.min(20, (int) Math.ceil(Math.sqrt(numOfIcons)) * 16 / 9);
        final ToolbarLayout layout = ToolbarLayout.vertical();
        layout.setMaximumWidth(Math.min(singleIconSize * xDimension, UITools.getScreenBounds(frame.getGraphicsConfiguration()).width * 4 / 5));

		iconPanel.setLayout(layout);
		iconLabels = new ArrayList<>(numOfIcons);
		for (int i = 0; i < numOfIcons; ++i) {
			final IconDescription icon = icons.get(i);
			JLabel label = new JLabel(icon.getActionIcon());
			label.putClientProperty(IconDescription.class, icon);
			iconLabels.add(label);
            iconPanel.add(label);
            label.setBorder(USUAL);
            label.addMouseListener(this);
		}
		Dimension preferredSize = iconPanel.getPreferredSize();
		JScrollPane scrollPane = new JScrollPane(iconPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
		        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(singleIconSize);
		scrollPane.setPreferredSize(new Dimension(preferredSize.width, preferredSize.width / 2));
		filterTextField = setupFilterTextField_and_KeyListener();
		addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowLostFocus(WindowEvent e) {
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                filterTextField.requestFocusInWindow();
                removeWindowFocusListener(this);
            }
        });
        contentPane.add(filterTextField, BorderLayout.NORTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);
		descriptionLabel = new JLabel(" ");
		statusPanel = Box.createHorizontalBox();
		statusPanel.add(descriptionLabel);
		statusPanel.add(Box.createHorizontalGlue());
		contentPane.add(statusPanel, BorderLayout.SOUTH);
		selected = iconLabels.get(0);
        highlightSelected();
        final WindowConfigurationStorage windowConfigurationStorage = new WindowConfigurationStorage(WINDOW_CONFIG_PROPERTY);
        windowConfigurationStorage.setBounds(this);
        filterTimer = new Timer(300, this::filterIcons);
        filterTimer.setRepeats(false);

	}

	private JTextField setupFilterTextField_and_KeyListener() {
	        JTextField filterTextField = new JTextField();
	        filterTextField.setText(lastSearchText);
	        filterTextField.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void removeUpdate(DocumentEvent e) {
                    filterIconsLater();
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    filterIconsLater();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    filterIconsLater();
                }
            });
	        filterTextField.addKeyListener(new KeyListener() {
	            public void keyPressed(KeyEvent keyEvent) {
	                processKeyEvent(keyEvent);
	            }

	            public void keyReleased(KeyEvent keyEvent) {
	            }

	            public void keyTyped(KeyEvent keyEvent) {
	            }
	        });
	        return filterTextField;
	    }

	    private void filterIconsLater() {
	        filterTimer.restart();
	    }

	    private void filterIcons(ActionEvent e) {
	        String filterText = filterTextField.getText().toLowerCase();

	        if (filterText.trim().length() > 0) {
	            final Pattern regex;
	            if (filterText.startsWith("/")) {
                    if (filterText.trim().length() >= 2) {
                        try {
                            regex = Pattern.compile(filterText.substring(1).trim(), Pattern.CASE_INSENSITIVE);
                        } catch (PatternSyntaxException pse) {
                            return;
                        }
                    } else
                        return;
                } else
	                regex = null;

	            for (JLabel label : iconLabels) {
	                boolean matches = false;
                    for (String tag : getTags(label)) {
                        if (regex != null) {
                            matches = regex.matcher(tag).matches();
                        } else {
                            if (filterText.contains(" ")) {
                                matches = true;
                                StringTokenizer tokenizer = new StringTokenizer(filterText);
                                while (tokenizer.hasMoreTokens()) {
                                    String token = tokenizer.nextToken();
                                    matches = matches && tag.contains(token);
                                    if (!matches) {
                                        break;
                                    }
                                }
                            } else {
                                matches = tag.contains(filterText);
                            }
                        }
                        if(matches)
                            break;
                    }
                    label.setVisible(matches);
	            }
	        }
	        else {
	            for (Component component : iconPanel.getComponents()) {
	                component.setVisible(true);
	            }
	        }
	        SwingUtilities.invokeLater(this::adjustSelection);
	    }

        private void adjustSelection() {
            if(! iconPanel.isValid()) {
                SwingUtilities.invokeLater(this::adjustSelection);
                return;
            }
            if(selected == null || ! selected.isVisible()) {
                select(new Point(0, 0));
            }
            else
                scrollToSelected();
        }

        private String[] getTags(JLabel label) {
            IconDescription iconDescription = (IconDescription) label.getClientProperty(IconDescription.class);
            String iconLabel = iconDescription.getTranslatedDescription();
            if (iconLabel.startsWith("icon_")) {
                iconLabel = iconLabel.substring(5);
            }
            return new String[] {iconLabel.toLowerCase(), iconDescription.getFile().toLowerCase()};
        }

	private void addIcon(final int pModifiers) {
		addIcon(iconLabels.indexOf(selected), pModifiers);
	}

	private void addIcon(int iconIndex, final int pModifiers) {
		selectedIconIndex =  iconIndex;
		mModifiers = pModifiers;
		if(listener != null) {
			listener.actionPerformed(new ActionEvent(this, selectedIconIndex, "", System.currentTimeMillis(), mModifiers));
			if(closeAfterSelection.isSelected())
				dispose();
		} else
			dispose();
	}

	private int findIndex(final Point location) {
		for(int i = 0; i < iconLabels.size(); i++) {
		    JLabel label = iconLabels.get(i);
		    if(label.getBounds().contains(location))
		        return i;
		}
		return -1;
	}

	private void close() {
		selectedIconIndex = -1;
		mModifiers = 0;
		dispose();
	}

	private void cursorDown() {
	    if(selected == null)
	        return;
		final Point newPosition = new Point(selected.getX(), selected.getY() + selected.getWidth() + 1);
		int newIndex = findIndex(newPosition);
		if (newIndex >= 0) {
			select(newIndex);
		}
	}

	private void cursorLeft() {
        if(selected == null)
            return;
        final Point newPosition = new Point(selected.getX() - 1, selected.getY());
        int newIndex = findIndex(newPosition);
        if (newIndex >= 0) {
            select(newIndex);
        }
	}

	private void cursorRight() {
        if(selected == null)
            return;
        final Point newPosition = new Point(selected.getX() + selected.getHeight() + 1, selected.getY());
        int newIndex = findIndex(newPosition);
        if (newIndex >= 0) {
            select(newIndex);
        }
	}

	private void cursorUp() {
        if(selected == null)
            return;
        final Point newPosition = new Point(selected.getX(), selected.getY() - 1);
        int newIndex = findIndex(newPosition);
        if (newIndex >= 0) {
            select(newIndex);
        }
	}

	public KeyStroke getKeyStroke(String keystrokeResourceName) {
		final String keyStrokeDescription = ResourceController.getResourceController().getProperty(keystrokeResourceName);
		return UITools.getKeyStroke(keyStrokeDescription);
	}

	private int findIndexByKeyEvent(final KeyEvent keyEvent) {
		for (int i = 0; i < icons.size(); i++) {
			final IconDescription info = icons.get(i);
			final KeyStroke iconKeyStroke = getKeyStroke(info.getShortcutKey());
			if (iconKeyStroke != null
			        && (keyEvent.getKeyCode() == iconKeyStroke.getKeyCode()
			            && keyEvent.getKeyCode() != 0
			                && (iconKeyStroke.getModifiers() & InputEvent.SHIFT_MASK) == (keyEvent.getModifiers() & InputEvent.SHIFT_MASK)
			                ||
			                (keyEvent.getKeyChar() == iconKeyStroke.getKeyChar()) && keyEvent.getKeyChar() != 0
			                && keyEvent.getKeyChar() != KeyEvent.CHAR_UNDEFINED)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Transfer shift masks from InputEvent to ActionEvent. But, why don't they
	 * use the same constants???? Java miracle.
	 */
	public int getModifiers() {
		int m = mModifiers;
		if ((mModifiers & (ActionEvent.SHIFT_MASK | InputEvent.SHIFT_DOWN_MASK)) != 0) {
			m |= ActionEvent.SHIFT_MASK;
		}
		if ((mModifiers & (ActionEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK)) != 0) {
			m |= ActionEvent.CTRL_MASK;
		}
		if ((mModifiers & (ActionEvent.META_MASK | InputEvent.META_DOWN_MASK)) != 0) {
			m |= ActionEvent.META_MASK;
		}
		if ((mModifiers & (ActionEvent.ALT_MASK | InputEvent.ALT_DOWN_MASK)) != 0) {
			m |= ActionEvent.ALT_MASK;
		}
		return m;
	}

	public int getIconIndex() {
		return selectedIconIndex;
	}

    private void highlightSelected() {
        selected.setBorder(HIGHLIGHTED);
        scrollToSelected();
    }

    private void scrollToSelected() {
        if(selected != null)
            selected.scrollRectToVisible(new Rectangle(0, 0, selected.getWidth(), selected.getHeight()));
    }


    public void processKeyEvent(final KeyEvent keyEvent) {
		boolean areModifiersDown = keyEvent.isControlDown() || keyEvent.isMetaDown();
		switch (keyEvent.getKeyCode()) {
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_KP_RIGHT:
				cursorRight();
				return;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_KP_LEFT:
				cursorLeft();
				return;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_KP_DOWN:
				cursorDown();
				return;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_KP_UP:
				cursorUp();
				return;
			case KeyEvent.VK_ESCAPE:
				keyEvent.consume();
				close();
				return;
			case KeyEvent.VK_ENTER:
				keyEvent.consume();
				addIcon(keyEvent.getModifiers());
				if(listener != null && closeAfterSelection.isSelected())
					dispose();
				return;
		}
		if(areModifiersDown) {
		    final int index = findIndexByKeyEvent(keyEvent);
		    if (index != -1) {
		    	keyEvent.consume();
		        addIcon(index, keyEvent.getModifiers());
		        if(listener != null && closeAfterSelection.isSelected())
		        	dispose();
		    }
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
    @Override
    public void mouseClicked(final MouseEvent mouseEvent) {
    	if(mouseEvent.isControlDown()) {
    		changeKeystroke();
    	}
    	else
			addIcon(mouseEvent.getModifiers());
	}

	private void changeKeystroke() {
		final int selectedIndex = iconLabels.indexOf(selected);
		final String keystrokeResourceName = icons.get(selectedIndex).getShortcutKey();
		final String keyStrokeDescription = ResourceController.getResourceController().getProperty(keystrokeResourceName);
		final GrabKeyDialog keyDialog = new GrabKeyDialog(keyStrokeDescription, KeyEvent.ALT_MASK | KeyEvent.CTRL_MASK | KeyEvent.META_MASK);
		keyDialog.setVisible(true);
		if (keyDialog.isOK()) {
			 ResourceController.getResourceController().setProperty(keystrokeResourceName, keyDialog.getShortcut());
			 select(selectedIndex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	@Override
    public void mouseEntered(final MouseEvent arg0) {
		select(((JLabel) arg0.getSource()).getLocation());
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	@Override
    public void mouseExited(final MouseEvent arg0) {/**/}

	/*
	 * (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	@Override
    public void mousePressed(final MouseEvent arg0) {/**/}

	/*
	 * (non-Javadoc)
	 * @see
	 * java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	@Override
    public void mouseReleased(final MouseEvent arg0) {/**/}

	private void select(final Point location) {
	    int index = findIndex(location);
	    select(index);
	}

	private void select(final int index) {
	    unhighlightSelected();
	    final String message;
	    if(index >= 0) {
	        JLabel newSelected = iconLabels.get(index);
	        this.selected = newSelected;
	        highlightSelected();
	        final IconDescription iconInformation = icons.get(index);
	        KeyStroke accelerator = getKeyStroke(iconInformation.getShortcutKey());
	        if (accelerator != null) {
	            String accText = "";
	            if (accelerator != null) {
	                int modifiers = accelerator.getModifiers() | ((Compat.isMacOsX() ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK));
	                if (modifiers > 0) {
	                    accText = InputEvent.getModifiersExText(modifiers);
	                    accText += "+";
	                }
	                int keyCode = accelerator.getKeyCode();
	                if (keyCode != 0) {
	                    accText += KeyEvent.getKeyText(keyCode);
	                } else {
	                    accText += accelerator.getKeyChar();
	                }
	            }

	            message = iconInformation.getTranslatedDescription() + ", " + accText;
	        }
	        else {
	            message = iconInformation.getTranslatedDescription();
	        }
	    }
	    else {
	        this.selected = null;
	        message = "";
	    }
	    descriptionLabel.setText(message);
	}
	private void unhighlightSelected() {
	    if(selected != null)
	        selected.setBorder(USUAL);
	}

	public void setActionListener(ActionListener listener) {
		this.listener = listener;
		closeAfterSelection = TranslatedElementFactory.createPropertyCheckbox("icon_selection_close_after_selection", "close_after_selection");
		statusPanel.add(closeAfterSelection);
	}

	public void addActionPanel(ActionPanel actionPanel) {
		actionPanel.setDisablesFocus(false);
		actionPanel.setButtonConfigurer(this::configureButton);

		Container contentPane = getContentPane();
		JPanel newContentPane = new JPanel(new BorderLayout());
		setContentPane(newContentPane);
		newContentPane.add(contentPane, BorderLayout.CENTER);
		newContentPane.add(actionPanel, BorderLayout.NORTH);
	}

	private void configureButton(AbstractButton b) {
		b.addMouseListener(focusRequester);
		b.addActionListener(actionPanelActionListener);
	}
}
