/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2003 -2013 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
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
package org.freeplane.core.resources.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import org.freeplane.api.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.StringUtils;
import org.dpolivaev.mnemonicsetter.MnemonicSetter;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.resources.components.IValidator.ValidationResult;
import org.freeplane.core.ui.FileOpener;
import org.freeplane.core.ui.LabelAndMnemonicSetter;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.Controller;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.factories.Borders;

class OptionPanel {
	private static final String FILE_EXTENSION = "freeplaneoptions";

	interface IOptionPanelFeedback {
		void writeProperties(Properties props);
	}

	static final String PREFERENCE_STORAGE_PROPERTY = "OptionPanel_Window_Properties";
	private static JTextField selectionColorCheckingTextFieldComponent;

	private Vector<IPropertyControl> controls;
	final private IOptionPanelFeedback feedback;
	final private HashMap<String, Integer> tabStringToIndexMap = new HashMap<String, Integer>();
	final private HashMap<Integer, String> tabIndexToStringMap = new HashMap<Integer, String>();
	private String selectedPanel;
	final private JDialog topDialog;
	private JFileChooser fileChooser;
	private boolean arePropertyValidatorsEnabled;

	/**
	 * @throws IOException
	 */
	OptionPanel(final JDialog d, final IOptionPanelFeedback feedback) {
		super();
		topDialog = d;
		this.arePropertyValidatorsEnabled = false;
		this.feedback = feedback;
		new OptionPanelBuilder();
	}



	void enablePropertyValidators() {
        this.arePropertyValidatorsEnabled = true;
    }



    /**
	 * Builds and returns a right aligned button bar with the given buttons.
	 *
	 * @param buttons  an array of buttons to add
	 * @return a right aligned button bar with the given buttons
	 */
	private static JPanel buildRightAlignedBar(JButton... buttons) {
		ButtonBarBuilder builder = new ButtonBarBuilder();
		builder.addGlue();
		builder.addButton(buttons);
		final JPanel panel = builder.getPanel();
		final JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		outerPanel.add(panel);
		return outerPanel;
	}


	/**
	 * This method builds the preferences panel.
	 * A list of IPropertyControl is iterated through and
	 * if the IPropertyControl is an instance of TabProperty,
	 * it creates a new "tab" that can be clicked to reveal the appropriate panel.
	 * If the previous selected tab was saved on close,
	 * the appropriate tab is reopened.
	 *
	 * @param controlsTree  This is the data that needs to be built
	 */
	public void buildPanel(final DefaultMutableTreeNode controlsTree) {
		final JPanel centralPanel = new JPanel();
		centralPanel.setLayout(new GridLayout(1, 1));
		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		initControls(controlsTree);
		final Iterator<IPropertyControl> iterator = controls.iterator();
		int tabIndex = 0;
		PropertyPane propertyPane = null;
		while (iterator.hasNext()) {
			final IPropertyControl control = iterator.next();
            if (control instanceof TabProperty) {
				final TabProperty newTab = (TabProperty) control;
				propertyPane = new PropertyPane(newTab.getLayout());
				propertyPane.border(Borders.DIALOG);
				final JScrollPane bottomComponent = new JScrollPane(propertyPane);
				UITools.setScrollbarIncrement(bottomComponent);
				final String tabName = TextUtils.getOptionalText(newTab.getLabel());
				tabStringToIndexMap.put(tabName, tabIndex);
				tabIndexToStringMap.put(tabIndex, tabName);
				tabbedPane.addTab(tabName, bottomComponent);
				tabIndex++;
			}
			else {
			    propertyPane.addProperty(control);
			}
		}
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent event) {
				final JTabbedPane c = (JTabbedPane) event.getSource();
				selectedPanel = tabIndexToStringMap.get(c.getSelectedIndex());
			}
		});
		centralPanel.add(tabbedPane);
		if (selectedPanel != null && tabStringToIndexMap.containsKey(selectedPanel)) {
			// Without the containsKey call the loading of the tab "behaviour"/"behavior" gives a nullpointer exception
			tabbedPane.setSelectedIndex(tabStringToIndexMap.get(selectedPanel));
		}
		topDialog.getContentPane().add(centralPanel, BorderLayout.CENTER);
		final FileOpener fileOpener = new FileOpener(FILE_EXTENSION, new FileOpener.Listener() {

			@Override
			public void filesDropped(Collection<URL> urls) throws Exception {
				for(URL url :urls) {
					try (final InputStream inputStream = url.openStream()){
						loadOptions(inputStream);
					}
				}

			}
		});

		new DropTarget(centralPanel, fileOpener);

		final JButton saveButton = new JButton();
		LabelAndMnemonicSetter.setLabelAndMnemonic(saveButton, TextUtils.getRawText("save"));
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				saveOptionsToFile();
			}
		});

		final JButton loadButton = new JButton();
		LabelAndMnemonicSetter.setLabelAndMnemonic(loadButton, TextUtils.getRawText("load"));
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				loadOptionsFromFile();
			}

		});

		final JButton cancelButton = new JButton();
		LabelAndMnemonicSetter.setLabelAndMnemonic(cancelButton, TextUtils.getRawText("cancel"));
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				closeWindow();
			}
		});
		final JButton okButton = new JButton();
		LabelAndMnemonicSetter.setLabelAndMnemonic(okButton, TextUtils.getRawText("ok"));
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (validate()) {
					closeWindow();
					feedback.writeProperties(getOptionProperties());
				}
			}
		});
		topDialog.getRootPane().setDefaultButton(okButton);
		JButton[] buttons = { saveButton, loadButton, cancelButton, okButton };
		final JPanel buttonPanel = buildRightAlignedBar(buttons);
		buttonPanel.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
		MnemonicSetter.INSTANCE.setComponentMnemonics(buttonPanel);
		topDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

	}

	public void highlight(String selectedProperty)
	{
		for (IPropertyControl control: this.controls)
		{
			if (control instanceof PropertyAdapter)
			{
				final PropertyAdapter property = (PropertyAdapter) control;
				if (property.getName().equals(selectedProperty))
				{
					JLabel label = property.getLabelComponent();
					highlight(label);
					final JViewport viewPort = (JViewport) label.getParent().getParent();
					Rectangle bounds = label.getBounds();
					// make sure the whole label is visible!
					bounds.setBounds(bounds.getLocation().x, bounds.getLocation().y, bounds.getSize().width, bounds.getSize().height * 3);
					viewPort.scrollRectToVisible(bounds);
					property.getValueComponent().requestFocusInWindow();
				}
			}
		}
	}

	private void highlight(JLabel label) {
		if(selectionColorCheckingTextFieldComponent == null)
			selectionColorCheckingTextFieldComponent = new JTextField();
		Color selectionColor = selectionColorCheckingTextFieldComponent.getSelectionColor();
		Color selectedTextColor = selectionColorCheckingTextFieldComponent.getSelectedTextColor();
		if(selectionColor == null || selectedTextColor == null) {
			selectionColor = label.getForeground();
			selectedTextColor = label.getBackground();
		}
		if(selectionColor == null || selectedTextColor == null) {
			selectionColor = Color.BLUE;
			selectedTextColor = Color.WHITE;
		}
		Font font = label.getFont();
		label.setUI(new BasicLabelUI());
		label.setOpaque(true);
		label.setFont(font);
		label.setForeground(selectedTextColor);
		label.setBackground(selectionColor);
		label.setBorder(BorderFactory.createLineBorder(selectionColor, 3, true));
	}

	private void saveOptionsToFile() {
		final Properties properties = getOptionProperties();
		if(! validate(properties))
			return;
		JFileChooser fileChooser = getFileChooser();
		final int status = fileChooser.showSaveDialog(topDialog);
		if(status != JFileChooser.APPROVE_OPTION)
			return;
		final File outputFile = getOutputFile(fileChooser);
		try (final BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))){
			properties.store(output, "");
		} catch (Exception e) {
			LogUtils.warn(e);
		}
	}

	private File getOutputFile(JFileChooser fileChooser) {
		File outputFile = fileChooser.getSelectedFile();
		if(outputFile.getName().contains("."))
			return outputFile;
		else
			return new File(outputFile.getPath() + '.' + FILE_EXTENSION);
	}

	private JFileChooser getFileChooser() {
		if(fileChooser == null)
			fileChooser = createFileChooser();
		return fileChooser;
	}

	private JFileChooser createFileChooser() {
		final JFileChooser fileChooser = UITools.newFileChooser(new File(Compat.getApplicationUserDirectory()));
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setAcceptAllFileFilterUsed(false);
		final FileNameExtensionFilter filter = new FileNameExtensionFilter(FILE_EXTENSION, FILE_EXTENSION);
		fileChooser.setFileFilter(filter);
		return fileChooser;
	}

	private void loadOptionsFromFile() {
		JFileChooser fileChooser = getFileChooser();
		final int status = fileChooser.showOpenDialog(topDialog);
		if(status != JFileChooser.APPROVE_OPTION)
			return;
		final File inputFile = fileChooser.getSelectedFile();
		if(! inputFile.canRead())
			return;
		try {
			final BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(inputFile));
			loadOptions(inStream);
		} catch (Exception e) {
			LogUtils.warn(e);
		}
	}

	private void loadOptions(final InputStream inputStream) throws IOException {
		final Properties properties = new Properties();
		properties.load(inputStream);
		for (final IPropertyControl control : controls) {
			if (control instanceof PropertyBean) {
				final PropertyBean bean = (PropertyBean) control;
				final String name = bean.getName();
				final String value = properties.getProperty(name);
				if(value != null)
					bean.setValue(value);
			}
		}
	}
	private boolean validate() {
		final Properties properties = getOptionProperties();
		return validate(properties);
	}

	private boolean validate(final Properties properties) {
	    if(! arePropertyValidatorsEnabled)
	        return true;
		final ValidationResult result = new ValidationResult();
		for (final IValidator validator : Controller.getCurrentController().getOptionValidators()) {
			result.add(validator.validate(properties));
		}
		if (!result.isValid()) {
			UITools.errorMessage(formatErrors("OptionPanel.validation_error", result.getErrors()));
			LogUtils.severe(result.toString());
		}
		else if (result.hasWarnings()) {
			UITools.informationMessage(formatErrors("OptionPanel.validation_warning", result.getWarnings()));
			LogUtils.warn(result.toString());
		}
		return result.isValid();
	}

	private String formatErrors(final String key, final ArrayList<String> errors) {
		// TextUtils.format() xml escapes the format arguments - we don't want that
		final MessageFormat formatter = new MessageFormat(TextUtils.getText(key));
		return formatter.format(new Object[] { StringUtils.join(errors.iterator(), "<br>") });
	}

	/**
	 * This is where the controls are added to the "controls" IProperty Vector
	 * @param controlsTree This is the tree that gets built
	 */
	private void initControls(final DefaultMutableTreeNode controlsTree) {
		controls = new Vector<IPropertyControl>();
		addChildControls(null, controlsTree);
	}

	private void addChildControls(BooleanProperty parentControl, final DefaultMutableTreeNode controlsTree) {
		for (final Enumeration<TreeNode> i = controlsTree.children(); i.hasMoreElements();) {
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode) i.nextElement();
			final IPropertyControlCreator creator = (IPropertyControlCreator) node.getUserObject();
			if (creator != null) {
				String propertyName = creator.getPropertyName();
				if(ResourceController.getResourceController().getBooleanProperty(propertyName + ".hide"))
					continue;
				final IPropertyControl control = creator.createControl();
				controls.add(control);
				if (parentControl != null)
					parentControl.enables(control);
				addChildControls(asBooleanProperty(control), node);
			}
			else
				addChildControls(parentControl, node);
		}
	}

	private BooleanProperty asBooleanProperty(final IPropertyControl control) {
	    return (control instanceof BooleanProperty) ? (BooleanProperty) control : null;
    }

	public void closeWindow() {
		final OptionPanelWindowConfigurationStorage storage = new OptionPanelWindowConfigurationStorage();
		storage.setPanel(OptionPanelConstants.OPTION_PANEL_RESOURCE_PREFIX + selectedPanel);
		storage.storeDialogPositions(topDialog, OptionPanel.PREFERENCE_STORAGE_PROPERTY);
		topDialog.setVisible(false);
		topDialog.dispose();
	}

	private Properties getOptionProperties() {
		final Properties p = new Properties();
		for (final IPropertyControl control : controls) {
			if (control instanceof PropertyBean) {
				final PropertyBean bean = (PropertyBean) control;
				final String value = bean.getValue();
				if (value != null) {
					p.setProperty(bean.getName(), value);
				}
			}
		}
		return p;
	}

	public void setProperties() {
		for (final IPropertyControl control : controls) {
			if (control instanceof PropertyBean) {
				final PropertyBean bean = (PropertyBean) control;
				final String name = bean.getName();
				final String value = ResourceController.getResourceController().getProperty(name);
				bean.setValue(value);
			}
		}
	}

	void setSelectedPanel(final String panel) {
		if (panel.startsWith(OptionPanelConstants.OPTION_PANEL_RESOURCE_PREFIX)) {
			selectedPanel = panel.substring(OptionPanelConstants.OPTION_PANEL_RESOURCE_PREFIX.length());
		}
	}
}
