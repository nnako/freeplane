package org.freeplane.plugin.codeexplorer.configurator;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JComponent;
import org.freeplane.api.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.freeplane.core.ui.components.AutoResizedTable;
import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.ui.components.OptionalDontShowMeAgainDialog;
import org.freeplane.core.ui.components.OptionalDontShowMeAgainDialog.MessageType;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.IMapSelectionListener;
import org.freeplane.features.map.MapModel;
import org.freeplane.plugin.codeexplorer.map.CodeMap;
import org.freeplane.plugin.codeexplorer.task.CodeExplorerConfiguration;
import org.freeplane.plugin.codeexplorer.task.CodeExplorerConfigurations;
import org.freeplane.plugin.codeexplorer.task.ConfigurationChange;
import org.freeplane.plugin.codeexplorer.task.ParsedConfiguration;
import org.freeplane.plugin.codeexplorer.task.UserDefinedCodeExplorerConfiguration;

class CodeExplorerConfigurator extends JPanel implements IMapSelectionListener {

    private static final long serialVersionUID = 1L;
    private static final FileNameExtensionFilter JAR_FILTER = new FileNameExtensionFilter("jar", "jar");
    private static final Comparator<File> BY_NAME = Comparator.comparing(File::getName);


    private DefaultTableModel configTableModel;
    private DefaultTableModel locationsTableModel;
    private JTable configTable;
    private JTable locationsTable;
    private final CodeProjectController codeProjectController;
    private JTextArea rules;
    private JFileChooser fileChooser;
    private ConfigurationChange configurationChange;
    private JToggleButton helpToggleButton;
    private int selectedConfigurationIndex;
    private int exploredConfigurationIndex;


    CodeExplorerConfigurator(CodeProjectController codeProjectController) {
        this.codeProjectController = codeProjectController;
        configurationChange = ConfigurationChange.CODE_BASE;
        selectedConfigurationIndex = -1;
        exploredConfigurationIndex = -1;
        initializeComponents();
        updateConfigurationsTable(explorerConfigurations());
    }


    private void updateConfigurationsTable(CodeExplorerConfigurations explorerConfigurations) {
        configTableModel.setRowCount(0); // Clear existing data
        for (UserDefinedCodeExplorerConfiguration config : explorerConfigurations.getConfigurations()) {
            configTableModel.addRow(new Object[]{config.getProjectName()});
        }
    }


    private void initializeComponents() {
        createPanels();
        createFileChooser();
    }


    private void createFileChooser() {
        fileChooser = UITools.newFileChooser(null);
        fileChooser.setMultiSelectionEnabled(true);
    }

    private JComponent createConfigurationsPanel() {
        configTableModel = new DefaultTableModel(new Object[]{""}, 0);
        configTable = new JTable(configTableModel);
        configTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        configTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        JScrollPane configTableScrollPane = new JScrollPane(configTable);

        configTable.getSelectionModel().addListSelectionListener(e -> updateConfiguration());

        configTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int firstRow = e.getFirstRow();
                if (e.getType() == TableModelEvent.UPDATE && firstRow >= 0) {
                    int lastRow = e.getLastRow();
                    updateConfigurationNames(firstRow, lastRow);
                }
                else if (e.getType() == TableModelEvent.DELETE) {
                    if (selectedConfigurationIndex >= e.getFirstRow() && selectedConfigurationIndex <= e.getLastRow()) {
                        selectedConfigurationIndex = -1;
                    }
                    if (exploredConfigurationIndex >= e.getFirstRow() && exploredConfigurationIndex <= e.getLastRow()) {
                        exploredConfigurationIndex = -1;
                    }
                }

            }
        });
        return configTableScrollPane;
     }

    @SuppressWarnings("serial")
    private JComponent createLocationsPane(JLabel paneLabel, JToggleButton helpToggleButton) {

        locationsTableModel = new DefaultTableModel(new Object[]{""}, 0);
        locationsTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() >= 0 && e.getColumn() >= 0) {
                    updateSelectedConfigurationLocations();
                }
            }
        });

        locationsTable = new AutoResizedTable(locationsTableModel);
        locationsTable.getTableHeader().setVisible(false);
        locationsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        locationsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        CellRendererWithTooltip cellRenderer = new CellRendererWithTooltip();
        TableColumn locationsColumn = locationsTable.getColumnModel().getColumn(0);
        locationsColumn.setCellRenderer(cellRenderer);
        JScrollPane locationsTableScrollPane = new JScrollPane(locationsTable);
        locationsTableScrollPane.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                locationsTable.revalidate();
                locationsTable.repaint();
            }

        });

        // Use CardLayout to switch views
        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);

        // Add the table scroll pane to the card panel
        cardPanel.add(locationsTableScrollPane, "Locations");

        // Create a help text component and add it to the card panel
        JTextArea helpText = new JTextArea(ParsedConfiguration.HELP);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpText.setEditable(false); // make it read-only if it's a text area
        cardPanel.add(new JScrollPane(helpText,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), "Help");

        String rulesHelpHeaderText = TextUtils.getText("code.helplabel");
        String locationsHeaderText = TextUtils.getText("code.locations");
        paneLabel.setText(locationsHeaderText);
        paneLabel.addHierarchyListener(new HierarchyListener() {

            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                Component component = e.getComponent();
                if(0 != (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) && component.isShowing()) {
                    paneLabel.setText(rulesHelpHeaderText);
                    Dimension rulesPreferredSize = paneLabel.getPreferredSize();
                    paneLabel.setText(locationsHeaderText);
                    Dimension locationPreferredSize = paneLabel.getPreferredSize();
                    paneLabel.setMinimumSize(new Dimension(Math.max(locationPreferredSize.width, rulesPreferredSize.width), Math.max(locationPreferredSize.height, rulesPreferredSize.height)));
                    paneLabel.removeHierarchyListener(this);
                }
            }
        });
        helpToggleButton.addActionListener(e -> {
            if (helpToggleButton.isSelected()) {
                paneLabel.setText(rulesHelpHeaderText);
                cardLayout.show(cardPanel, "Help");
            } else {
                paneLabel.setText(locationsHeaderText);
                cardLayout.show(cardPanel, "Locations");
            }
        });
        return cardPanel;
    }

    private JComponent createRulesPane() {
        rules = new JTextArea();
        JScrollPane rulesScrollPane = new JScrollPane(rules);
        return rulesScrollPane;
    }


    private void updateConfigurationNames(int firstRow, int lastRow) {
        for (int row = firstRow; row <= lastRow; row++) {
            updateConfigurationName(row);
        }
    }

    private void updateConfigurationName(int row) {
        String projectName = ((String) configTableModel.getValueAt(row, 0)).trim();
        UserDefinedCodeExplorerConfiguration config = getConfiguration(row);
        config.setProjectName(projectName);
    }

    private void updateConfiguration() {
        if(selectedConfigurationIndex == configTable.getSelectedRow()
                 || selectedConfigurationIndex != -1
                    && ! setConfigurationRules()) {
            return;
        }

        locationsTableModel.setRowCount(0); // Clear existing data

        selectedConfigurationIndex = configTable.getSelectedRow();
        if (configTable.getSelectedRowCount() == 1) {
            UserDefinedCodeExplorerConfiguration config = getConfiguration(selectedConfigurationIndex);
            for (File location : config.getLocations()) {
                locationsTableModel.addRow(new Object[]{location.getAbsolutePath()});
            }
            rules.setText(config.getConfigurationRules());
            Rectangle rect = configTable.getCellRect(selectedConfigurationIndex, 0, true);
            configTable.scrollRectToVisible(rect);
        } else
            rules.setText("");
        configurationChange = ConfigurationChange.SAME;
    }

    private void exploreSelectedConfiguration(boolean reloadCodebase) {
        final UserDefinedCodeExplorerConfiguration selectedConfiguration = getSelectedConfiguration();
        if(selectedConfiguration != null) {
            exploredConfigurationIndex = selectedConfigurationIndex;
            setConfigurationRules();
            codeProjectController.exploreConfiguration(selectedConfiguration, reloadCodebase);
            configurationChange = ConfigurationChange.SAME;
        }
    }

    UserDefinedCodeExplorerConfiguration getSelectedConfiguration() {
        UserDefinedCodeExplorerConfiguration selectedConfiguration = getConfiguration(selectedConfigurationIndex);
        return selectedConfiguration;
    }

    private UserDefinedCodeExplorerConfiguration getConfiguration(int selectedConfigurationIndex) {
        if(selectedConfigurationIndex >= 0)
            return explorerConfigurations().getConfigurations().get(selectedConfigurationIndex);
        else
            return null;
    }

    void addNewConfiguration() {
        UserDefinedCodeExplorerConfiguration newConfig = new UserDefinedCodeExplorerConfiguration();
        explorerConfigurations().getConfigurations().add(newConfig);
        configTableModel.addRow(new Object[]{newConfig.getProjectName()});
        int newRow = configTable.getRowCount() - 1;
        configTable.setRowSelectionInterval(newRow, newRow);
        configTable.editCellAt(newRow, 0);
        configTable.getEditorComponent().requestFocusInWindow();
    }

    void deleteSelectedConfigurations() {
        final ListSelectionModel selectionModel = configTable.getSelectionModel();
        int minSelectionIndex = selectionModel.getMinSelectionIndex();
        if(minSelectionIndex == -1)
            return;
        final int showResult = OptionalDontShowMeAgainDialog.show("code.really_remove_configurations",
                MessageType.ONLY_OK_SELECTION_IS_STORED);
        if (showResult != JOptionPane.OK_OPTION) {
            return;
        }
        int maxSelectionIndex = selectionModel.getMaxSelectionIndex();
        for(int row = maxSelectionIndex; row >= minSelectionIndex; row--) {
            configTableModel.removeRow(row);
            explorerConfigurations().getConfigurations().remove(row);
        }
        int rowCount = configTableModel.getRowCount();
        if(minSelectionIndex < rowCount)
            configTable.setRowSelectionInterval(minSelectionIndex, minSelectionIndex);
        else if (rowCount > 0)
            configTable.setRowSelectionInterval(rowCount-1, rowCount-1);
        selectedConfigurationIndex = -1;
        updateConfiguration();
    }

    private void moveSelectedLocationsToTheTop() {
        int[] selectedRows = locationsTable.getSelectedRows();
        Arrays.sort(selectedRows);
        for (int i = 0; i < selectedRows.length; i++) {
            locationsTableModel.moveRow(selectedRows[i], selectedRows[i], i);
        }
        locationsTable.getSelectionModel().setValueIsAdjusting(true);
        locationsTable.clearSelection();
        for (int i = 0; i < selectedRows.length; i++) {
            locationsTable.addRowSelectionInterval(i, i);
        }
        locationsTable.getSelectionModel().setValueIsAdjusting(false);
        updateSelectedConfigurationLocations();
    }

    private void moveSelectedLocationsUp() {
        int[] selectedRows = locationsTable.getSelectedRows();
        if (selectedRows.length > 0 && selectedRows[0] > 0) {
            for (int i = 0; i < selectedRows.length; i++) {
                int selectedIndex = selectedRows[i];
                locationsTableModel.moveRow(selectedIndex, selectedIndex, selectedIndex - 1);
            }
            locationsTable.getSelectionModel().setValueIsAdjusting(true);
            locationsTable.clearSelection();
            for (int selectedIndex : selectedRows) {
                locationsTable.addRowSelectionInterval(selectedIndex - 1, selectedIndex - 1);
            }
            locationsTable.getSelectionModel().setValueIsAdjusting(false);
            updateSelectedConfigurationLocations();
        }
    }

    private void moveSelectedLocationsDown() {
        int[] selectedRows = locationsTable.getSelectedRows();
        if (selectedRows.length > 0 && selectedRows[selectedRows.length - 1] < locationsTableModel.getRowCount() - 1) {
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int selectedIndex = selectedRows[i];
                locationsTableModel.moveRow(selectedIndex, selectedIndex, selectedIndex + 1);
            }
            locationsTable.getSelectionModel().setValueIsAdjusting(true);
            locationsTable.clearSelection();
            for (int selectedIndex : selectedRows) {
                locationsTable.addRowSelectionInterval(selectedIndex + 1, selectedIndex + 1);
            }
            locationsTable.getSelectionModel().setValueIsAdjusting(false);
            updateSelectedConfigurationLocations();
        }
    }

    private void moveSelectedLocationsToTheBottom() {
        int[] selectedRows = locationsTable.getSelectedRows();
        Arrays.sort(selectedRows);
        int rowCount = locationsTableModel.getRowCount();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            int selectedIndex = selectedRows[i];
            locationsTableModel.moveRow(selectedIndex, selectedIndex, rowCount - 1 - (selectedRows.length - 1 - i));
        }
        locationsTable.getSelectionModel().setValueIsAdjusting(true);
        locationsTable.clearSelection();
        for (int i = 0; i < selectedRows.length; i++) {
            int newRow = rowCount - selectedRows.length + i;
            locationsTable.addRowSelectionInterval(newRow, newRow);
        }
        locationsTable.getSelectionModel().setValueIsAdjusting(false);
        updateSelectedConfigurationLocations();
    }


    private void updateSelectedConfigurationLocations() {
        int selectedConfigRow = selectedConfigurationIndex;
        if (selectedConfigRow >= 0) {
            UserDefinedCodeExplorerConfiguration config = getConfiguration(selectedConfigRow);
            config.removeAllLocations();
            for(int row = 0; row < locationsTableModel.getRowCount(); row++)
                config.addLocation((String)locationsTableModel.getValueAt(row, 0));
        }
    }


    void removeSelectedLocations() {
        int[] selectedRows = locationsTable.getSelectedRows();
        if(selectedRows.length == 0)
            return;
        final int showResult = OptionalDontShowMeAgainDialog.show("code.really_remove_locations",
                MessageType.ONLY_OK_SELECTION_IS_STORED);
        if (showResult != JOptionPane.OK_OPTION) {
            return;
        }

        Arrays.sort(selectedRows);
        int removedRowCount = 0;
        for (int selectedIndex : selectedRows) {
            int row = selectedIndex - removedRowCount;
            int selectedConfigRow = selectedConfigurationIndex;
            if (selectedConfigRow >= 0) {
                UserDefinedCodeExplorerConfiguration config = getConfiguration(selectedConfigRow);
                config.removeLocation(locationsTableModel.getValueAt(row, 0).toString());
            }
            locationsTableModel.removeRow(row);
            removedRowCount++;
        }
        if(removedRowCount > 0)
            configurationChange = ConfigurationChange.CODE_BASE;
    }


    void addJarsAndFolders() {
        addJarsAndFolders(false);
    }
    void addFoldersRecursively() {
        addJarsAndFolders(true);
    }

    private void addJarsAndFolders(boolean addsFoldersResursively) {
        if(configTable.getRowCount() == 0)
            addNewConfiguration();
        int selectedConfigRow = selectedConfigurationIndex;
        UserDefinedCodeExplorerConfiguration selectedConfig = getConfiguration(selectedConfigRow);
        int selectedRow = locationsTable.getSelectedRow();
        if(selectedRow >= 0) {
            File selectedFile = new File(locationsTable.getValueAt(selectedRow, 0).toString());
            File selectedDirectory = selectedFile.getParentFile();
            if(selectedDirectory != null) {
                fileChooser.setSelectedFile(null);
                fileChooser.setCurrentDirectory(selectedDirectory);
            }
        }
        if(addsFoldersResursively) {
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setFileFilter(null);
        } else {
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setFileFilter(JAR_FILTER);
        }

        int option = fileChooser.showOpenDialog(CodeExplorerConfigurator.this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                addLocation(selectedConfig, file, addsFoldersResursively);
            }
        }
    }


    private int addLocation(UserDefinedCodeExplorerConfiguration selectedConfig, File location, boolean addsProjectFoldersResursively) {
        String path = location.getAbsolutePath();
        if(! location.exists()) {
            path = path.trim();
            location = new File(path);
        }
        if (! selectedConfig.containsLocation(path) && location.exists()
                && (! addsProjectFoldersResursively || containsProject(location))) {
            if(addsProjectFoldersResursively) {
                File[] subdirectories = location.listFiles(File::isDirectory);
                if (subdirectories != null) {
                    final ArrayList<File> subdirectoryList = new ArrayList<>(Arrays.asList(subdirectories));
                    Collections.sort(subdirectoryList, BY_NAME);
                    final int addedLocations = subdirectoryList.stream()
                    .mapToInt(d -> addLocation(selectedConfig, d, addsProjectFoldersResursively))
                    .sum();
                    if(addedLocations > 0)
                        return addedLocations;
                }
            }
            locationsTableModel.addRow(new Object[]{path});
            selectedConfig.addLocation(location);
            configurationChange = ConfigurationChange.CODE_BASE;
            return 1;
        }
        return 0;
    }


    private boolean containsProject(File location) {
        return Stream.of("pom.xml", "build.gradle", "build.gradle.kts", "build.xml", "scala.sbt", "project.clj")
                .anyMatch(projectFile ->
                    new File(location, projectFile).canRead());
    }

    private void cancelAnalysis() {
        codeProjectController.cancelAnalysis();
    }

    private void applyConfigurationRules() {
        setConfigurationRules();
        if(exploredConfigurationIndex != selectedConfigurationIndex) {
            exploreSelectedConfiguration(true);
        }
        else switch(configurationChange) {
        case CODE_BASE:
            exploreSelectedConfiguration(true);
            break;
        case GROUPS:
            exploreSelectedConfiguration(false);
            break;
        case CONFIGURATION:
            codeProjectController.updateProjectConfiguration();
            break;
        case SAME:
            break;
        }
        configurationChange = ConfigurationChange.SAME;
    }
    private boolean setConfigurationRules() {
        UserDefinedCodeExplorerConfiguration selectedConfiguration = getSelectedConfiguration();
        if(selectedConfiguration != null) {
            String ruleSpecification = rules.getText();
            if(! selectedConfiguration.getConfigurationRules().equals(ruleSpecification)) {
                try {
                    ConfigurationChange status = selectedConfiguration.applyConfigurationRules(ruleSpecification);
                    configurationChange = ConfigurationChange.max(configurationChange, status);
                } catch (IllegalArgumentException e) {
                    configTable.getSelectionModel().setSelectionInterval(selectedConfigurationIndex, selectedConfigurationIndex);
                    if(! helpToggleButton.isSelected())
                        helpToggleButton.doClick();
                    rules.requestFocusInWindow();
                    String text = e.getMessage();
                    UITools.informationMessage(text);
                    return false;
                }
            }
        }
        return true;
    }

    private void createPanels() {
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        JLabel configurationsLabel = new JLabel(TextUtils.getText("code.configurations"));
        JComponent configurationsPanel = createConfigurationsPanel();
        JComponent configurationTableToolbar = createConfigurationTableToolbar();
        JLabel locationsLabel = new JLabel();
        helpToggleButton = TranslatedElementFactory.createToggleButtonWithIcon("code.help.icon", "code.help");
        JComponent locationsToolbar = createLocationButtons();
        JComponent locationsPane = createLocationsPane(locationsLabel, helpToggleButton);
        JLabel rulesLabel = new JLabel(TextUtils.getText("code.rules"));
        JComponent rulesToolbar = createRulesButtons(helpToggleButton);
        JComponent rulesPane = createRulesPane();


        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1; // Span across all columns

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor=GridBagConstraints.CENTER;
        add(configurationsLabel, gbc);

        gbc.gridy = 1;
        gbc.anchor=GridBagConstraints.LINE_START;
        add(configurationTableToolbar, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor=GridBagConstraints.CENTER;
        add(locationsLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor=GridBagConstraints.LINE_START;
        add(locationsToolbar, gbc);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor=GridBagConstraints.LINE_START;
        add(rulesToolbar, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor=GridBagConstraints.CENTER;
        add(rulesLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1;
        configurationsPanel.setPreferredSize(new Dimension(1, 1));
        add(configurationsPanel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 2;
        locationsPane.setPreferredSize(new Dimension(1, 1));
        add(locationsPane, gbc);

        gbc.gridx = 2;
        gbc.weightx = 1;
        rulesPane.setPreferredSize(new Dimension(1, 1));
        add(rulesPane, gbc);
    }

    private JComponent createConfigurationTableToolbar() {
        FreeplaneToolBar toolbar = new FreeplaneToolBar(SwingConstants.HORIZONTAL);
        JButton addConfigurationButton = TranslatedElementFactory.createButtonWithIcon("code.AddConfigurationAction.icon", "code.AddConfigurationAction.text");
        addConfigurationButton.addActionListener(e -> addNewConfiguration());
        JButton deleteConfigurationButton = TranslatedElementFactory.createButtonWithIcon("code.DeleteConfigurationsAction.icon", "code.DeleteConfigurationsAction.text");
        deleteConfigurationButton.addActionListener(e -> deleteSelectedConfigurations());
        JButton applyButton = TranslatedElementFactory.createButtonWithIcon("code.apply" + ".icon", "code.apply");
        applyButton.addActionListener(e ->applyConfigurationRules());

        JButton exploreConfigurationButton = TranslatedElementFactory.createButtonWithIcon("code.explore" + ".icon", "code.explore");
        exploreConfigurationButton.addActionListener(e -> exploreSelectedConfiguration(true));

        JButton cancelButton = TranslatedElementFactory.createButtonWithIcon("code.cancel" + ".icon", "code.cancel");
        cancelButton.addActionListener(e -> cancelAnalysis());

        JComponent panelButtons[] = {addConfigurationButton, deleteConfigurationButton, exploreConfigurationButton, applyButton, cancelButton};
        Stream.of(panelButtons).forEach(button -> {
            toolbar.add(button);
        });

        JButton enablingButtons[] = {exploreConfigurationButton, applyButton};

        Stream.of(enablingButtons).forEach(button -> {
            button.setEnabled(false);
        });

        Runnable enableButtons = () -> {
            boolean enable = configTable.getSelectedRowCount() == 1;
            Stream.of(enablingButtons).forEach(button -> button.setEnabled(enable));
        };

        configTable.getSelectionModel().addListSelectionListener(l -> enableButtons.run());

        return toolbar;
    }

    private JComponent createLocationButtons() {
        FreeplaneToolBar toolbar = new FreeplaneToolBar(SwingConstants.HORIZONTAL);
        JButton addLocationsButton = TranslatedElementFactory.createButtonWithIcon("code.AddLocationsAction.icon", "code.AddLocationsAction.text");
        addLocationsButton.addActionListener(e1 -> addJarsAndFolders());

        JButton addLocationsRecursivelyButton = TranslatedElementFactory.createButtonWithIcon("code.AddFoldersRecursivelyAction.icon", "code.AddFoldersRecursivelyAction.text");
        addLocationsRecursivelyButton.addActionListener(e1 -> addFoldersRecursively());

        JButton removeLocationsButton = TranslatedElementFactory.createButtonWithIcon("code.DeleteLocationsAction.icon", "code.DeleteLocationsAction.text");
        removeLocationsButton.addActionListener(e6 -> removeSelectedLocations());

        JButton btnMoveToTheTop = TranslatedElementFactory.createButtonWithIcon("code.move_to_the_top" + ".icon", "code.move_to_the_top");
        btnMoveToTheTop.addActionListener(e4 -> moveSelectedLocationsToTheTop());

        JButton btnMoveUp = TranslatedElementFactory.createButtonWithIcon("code.move_up" + ".icon", "code.move_up");
        btnMoveUp.addActionListener(e3 -> moveSelectedLocationsUp());

        JButton btnMoveDown = TranslatedElementFactory.createButtonWithIcon("code.move_down" + ".icon", "code.move_down");
        btnMoveDown.addActionListener(e5 -> moveSelectedLocationsDown());

        JButton btnMoveToTheBottom = TranslatedElementFactory.createButtonWithIcon("code.move_to_the_bottom" + ".icon", "code.move_to_the_bottom");
        btnMoveToTheBottom.addActionListener(e2 -> moveSelectedLocationsToTheBottom());

        JComponent panelButtons[] = {addLocationsButton, addLocationsRecursivelyButton, removeLocationsButton, btnMoveToTheTop, btnMoveUp, btnMoveDown, btnMoveToTheBottom};
        Stream.of(panelButtons).forEach(button -> {
            toolbar.add(button);
        });

        JButton enablingButtons[] = {addLocationsButton, addLocationsRecursivelyButton, removeLocationsButton, btnMoveToTheTop, btnMoveUp, btnMoveDown, btnMoveToTheBottom};

        Stream.of(enablingButtons).forEach(button -> {
            button.setEnabled(false);
        });

        Runnable enableButtons = () -> {
            boolean enable = configTable.getSelectionModel().getMinSelectionIndex() >= 0
                    && ! helpToggleButton.isSelected();
            Stream.of(enablingButtons).forEach(button -> button.setEnabled(enable));
        };

        configTable.getSelectionModel().addListSelectionListener(l -> enableButtons.run());
        return toolbar;

    }

    private JComponent createRulesButtons(JToggleButton helpToggleButton) {
        FreeplaneToolBar toolbar = new FreeplaneToolBar(SwingConstants.HORIZONTAL);
        JButton revertButton = TranslatedElementFactory.createButtonWithIcon("code.revert" + ".icon", "code.revert");
        revertButton.addActionListener(e ->
            rules.setText(getSelectedConfiguration().getConfigurationRules())
        );

        JComponent panelButtons[] = {revertButton, helpToggleButton};
        Stream.of(panelButtons).forEach(button -> {
            toolbar.add(button);
        });

        JButton enablingButtons[] = {revertButton};

        Stream.of(enablingButtons).forEach(button -> {
            button.setEnabled(false);
        });

        Runnable enableButtons = () -> {
            boolean enable = configTable.getSelectionModel().getMinSelectionIndex() >= 0
                    && ! helpToggleButton.isSelected();
            Stream.of(enablingButtons).forEach(button -> button.setEnabled(enable));
        };

        configTable.getSelectionModel().addListSelectionListener(l -> enableButtons.run());
        helpToggleButton.addActionListener(l -> enableButtons.run());
        return toolbar;

    }

    public List<File> getSelectedLocations() {
        List<File> paths = new ArrayList<>();
        for (int i = 0; i < locationsTableModel.getRowCount(); i++) {
            paths.add(new File(locationsTableModel.getValueAt(i, 0).toString()));
        }
        return paths;
    }


    private CodeExplorerConfigurations explorerConfigurations() {
        return codeProjectController.explorerConfigurations();
    }


    @Override
    public void afterMapChange(MapModel oldMap, MapModel newMap) {
        if(! (newMap instanceof CodeMap))
            return;
        CodeExplorerConfiguration configuration = ((CodeMap)newMap).getConfiguration();
        if(! (configuration instanceof UserDefinedCodeExplorerConfiguration)) {
            return;
        }
        String projectName = ((UserDefinedCodeExplorerConfiguration)configuration).getProjectName();
        for(int row = 0; row < configTable.getRowCount(); row++) {
            if(projectName.equals(configTable.getValueAt(row, 0)))
                configTable.getSelectionModel().setSelectionInterval(row, row);
        }
    }
}
