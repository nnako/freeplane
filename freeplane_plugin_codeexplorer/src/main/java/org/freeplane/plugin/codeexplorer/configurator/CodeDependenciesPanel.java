package org.freeplane.plugin.codeexplorer.configurator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.IMapSelectionListener;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.codeexplorer.dependencies.CodeDependency;
import org.freeplane.plugin.codeexplorer.map.CodeMap;
import org.freeplane.plugin.codeexplorer.map.CodeNode;
import org.freeplane.plugin.codeexplorer.map.SelectedNodeDependencies;
import org.freeplane.plugin.codeexplorer.task.GroupMatcher.MatchingCriteria;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;

class CodeDependenciesPanel extends JPanel implements INodeSelectionListener, IMapSelectionListener, IFreeplanePropertyListener, IMapChangeListener{

    private static final String[] COLUMN_NAMES = new String[]{"Verdict", "Origin", "Target","Dependency"};

    private static final long serialVersionUID = 1L;
    private final JTextField filterField;
    private final JTable dependencyViewer;
    private final JLabel countLabel;
    private final List<Consumer<Object>> dependencySelectionCallbacks;
    private List<CodeDependency> allDependencies;
    private boolean isLastColumnVisible = true;

    private AFreeplaneAction filterAction;

    private class DependenciesWrapper extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        @Override
        public int getRowCount() {
            return allDependencies.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            CodeDependency row = allDependencies.get(rowIndex);
            switch (columnIndex) {
                case 0: return row.describeVerdict();
                case 1: {
                    String originName = toDisplayedFullName(row.getOriginClass());
                    String targetName = toDisplayedFullName(row.getTargetClass());
                    return shortenClassNameIfCommonPrefix(originName, targetName);
                }
                case 2: {
                    String originName = toDisplayedFullName(row.getOriginClass());
                    String targetName = toDisplayedFullName(row.getTargetClass());
                    return shortenClassNameIfCommonPrefix(targetName, originName);
                }
                case 3: return row.getDescription();
                default: return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex > 0;
        }

        private String shortenClassNameIfCommonPrefix(String name, String otherName) {
            String[] nameParts = name.split("\\.");
            String[] otherParts = otherName.split("\\.");

            int commonPrefixCount = 0;
            int minLength = Math.min(nameParts.length, otherParts.length);

            for (int i = 0; i < minLength - 1; i++) {
                if (nameParts[i].equals(otherParts[i])) {
                    commonPrefixCount++;
                } else {
                    break;
                }
            }

            if (commonPrefixCount > 0) {
                // Build shortened name with prefix replaced by ".."
                StringBuilder shortened = new StringBuilder("..");
                for (int i = commonPrefixCount; i < nameParts.length; i++) {
                    shortened.append(".").append(nameParts[i]);
                }
                return shortened.toString();
            }

            return name;
        }
    }

    CodeDependenciesPanel(AFreeplaneAction filterAction) {
         this.filterAction = filterAction;
        dependencySelectionCallbacks = new ArrayList<>();
         // Create the top panel for sorting options
         JPanel topPanel = new JPanel(new BorderLayout());

         JButton filterButton = TranslatedElementFactory.createButtonWithIcon(filterAction);
         filterButton.setEnabled(false);
         countLabel = new JLabel();
         final int countLabelMargin = (int) (UITools.FONT_SCALE_FACTOR * 10);
         Box filterBox = Box.createHorizontalBox();
         filterBox.add(filterButton);
         filterBox.add(Box.createHorizontalStrut(countLabelMargin));
         filterBox.add(countLabel);
         filterBox.add(Box.createHorizontalStrut(countLabelMargin));

         // Add the box of left-aligned components to the top panel at the WEST
         topPanel.add(filterBox, BorderLayout.WEST);

         // Configure filterField to expand and fill the remaining space
         filterField = new JTextField();
         filterField.addActionListener(e -> updateDependencyFilter());
         // Add the filterField to the CENTER to occupy the maximum available space
         topPanel.add(filterField, BorderLayout.CENTER);

         // Add toggle button for last column visibility
         JCheckBox toggleLastColumnButton = TranslatedElementFactory.createCheckBox("code.toggle_dependencies");
         toggleLastColumnButton.setSelected(isLastColumnVisible);
         toggleLastColumnButton.addActionListener(e -> toggleLastColumnVisibility());
         topPanel.add(toggleLastColumnButton, BorderLayout.EAST);

         dependencyViewer = new JTable() {

            private static final long serialVersionUID = 1L;

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                JComponent component = (JComponent) super.prepareRenderer(renderer, row, column);
                int modelColumn = convertColumnIndexToModel(column);
                if(modelColumn == 1 || modelColumn == 2) {
                    CodeDependency codeDependency = allDependencies.get(convertRowIndexToModel(row));
                    JavaClass javaClass = modelColumn == 1 ? codeDependency.getOriginClass() : codeDependency.getTargetClass();
                    component.setToolTipText(toDisplayedFullName(javaClass));
                }
                return component;
            }

        };
        allDependencies = Collections.emptyList();
        DependenciesWrapper dataModel = new DependenciesWrapper();
        dependencyViewer.setModel(dataModel);
        CellRendererWithTooltip cellRenderer = new CellRendererWithTooltip();

        TableColumnModel columnModel = dependencyViewer.getColumnModel();
        updateColumn(columnModel, 0, 80, cellRenderer);
        updateColumn(columnModel, 1, 200, cellRenderer);
        updateColumn(columnModel, 2, 200, cellRenderer);
        updateColumn(columnModel, 3, 1200, cellRenderer);

        dependencyViewer.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        dependencyViewer.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        dependencyViewer.setCellSelectionEnabled(true);

        TableRowSorter<DependenciesWrapper> sorter = new TableRowSorter<>(dataModel);

        sorter.addRowSorterListener(e -> {
            if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
                SwingUtilities.invokeLater(this::scrollSelectedToVisible);
            }
        });

        dependencyViewer.setRowSorter(sorter);

        JTextField cellEditor = new JTextField();
        cellEditor.setEditable(false);
        dependencyViewer.setDefaultEditor(Object.class, new DefaultCellEditor(cellEditor));

        JScrollPane scrollPane = new JScrollPane(dependencyViewer);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }


    private void updateDependencyFilter() {
        String[] filteredWords = filterField.getText().trim().split("[^\\w:.$]+");
        @SuppressWarnings("unchecked")
        TableRowSorter<DependenciesWrapper> rowSorter = (TableRowSorter<DependenciesWrapper>)dependencyViewer.getRowSorter();

        // Text-based filter
        RowFilter<DependenciesWrapper, Integer> textFilter = null;
        if(!(filteredWords.length == 1 && filteredWords[0].isEmpty())) {
            textFilter = new RowFilter<DependenciesWrapper, Integer>() {
                BiPredicate<CodeDependency, String[]> combinedFilter = Stream.of(filteredWords)
                        .map(this::createPredicateFromString)
                        .reduce((x,y) -> true, BiPredicate::and);
                private BiPredicate<CodeDependency, String[]> createPredicateFromString(String searchedString) {
                    if (searchedString.startsWith("origin:")) {
                        String value = searchedString.substring("origin:".length());
                        return (dependency, row) -> dependency.getOriginClass().getName().contains(value);
                    } else if (searchedString.startsWith("target:")) {
                        String value = searchedString.substring("target:".length());
                        return (dependency, row) -> dependency.getTargetClass().getName().contains(value);
                    } else if (searchedString.startsWith("verdict:")) {
                        String value = searchedString.substring("verdict:".length());
                        return (dependency, row) -> row[0].contains(value);
                    } else if (searchedString.startsWith("dependency:")) {
                        String value = searchedString.substring("dependency:".length());
                        return (dependency, row) -> row[3].contains(value);
                    } else if (searchedString.equalsIgnoreCase(":rmi")) {
                        return this::isRmiDependency;
                    } else {
                        return (dependency, row) -> Stream.of(row).anyMatch(s-> s.contains(searchedString));
                    }
                }

                private boolean isRmiDependency(CodeDependency codeDependency, String[] row) {
                    MapModel map = Controller.getCurrentController().getSelection().getMap();
                    if(! (map instanceof CodeMap))
                        return false;
                    CodeMap codeMap = (CodeMap) map;
                    if(codeMap.matchingCriteria(codeDependency.getOriginClass(), codeDependency.getTargetClass())
                            .filter(MatchingCriteria.RMI::equals).isPresent())
                        return row[3].contains(" implements ") || row[3].contains(" extends ") || row[3].contains(" constructor ");
                    return false;
                }

                @Override
                public boolean include(RowFilter.Entry<? extends DependenciesWrapper, ? extends Integer> entry) {
                    TableModel tableData = dependencyViewer.getModel();
                    final int rowIndex = entry.getIdentifier().intValue();
                    String[] row = IntStream.range(0, 4)
                            .mapToObj(column -> tableData.getValueAt(rowIndex, column).toString())
                            .toArray(String[]::new);
                    return combinedFilter.test(allDependencies.get(rowIndex), row);
                }
            };
        }

        // Duplicate row filter (only when the last column is hidden)
        RowFilter<DependenciesWrapper, Integer> uniqueRowFilter = null;
        if (!isLastColumnVisible) {
            uniqueRowFilter = new RowFilter<DependenciesWrapper, Integer>() {
                private final Set<String> visibleRowSignatures = new HashSet<>();

                @Override
                public boolean include(Entry<? extends DependenciesWrapper, ? extends Integer> entry) {
                    // Create a signature using values from columns 0, 1, and 2
                    StringBuilder signature = new StringBuilder();
                    for (int i = 1; i < 3; i++) {
                        signature.append(entry.getModel().getValueAt(entry.getIdentifier(), i));
                        signature.append("||"); // delimiter
                    }

                    // Only include row if we haven't seen this signature before
                    return visibleRowSignatures.add(signature.toString());
                }
            };
        }

        // Apply appropriate filter(s)
        if (textFilter != null && uniqueRowFilter != null) {
            // Both filters
            rowSorter.setRowFilter(RowFilter.andFilter(Arrays.asList(textFilter, uniqueRowFilter)));
        } else if (textFilter != null) {
            // Text filter only
            rowSorter.setRowFilter(textFilter);
        } else if (uniqueRowFilter != null) {
            // Unique filter only
            rowSorter.setRowFilter(uniqueRowFilter);
        } else {
            // No filters
            rowSorter.setRowFilter(null);
        }

        dependencySelectionCallbacks.stream().forEach(x -> x.accept(this));
        scrollSelectedToVisible();
        updateRowCountLabelAndFilterAction();
    }

    private void updateColumn(TableColumnModel columns, int index, int columnWidth, TableCellRenderer cellRenderer) {
        int scaledWidth = (int) (columnWidth*UITools.FONT_SCALE_FACTOR);
        TableColumn columnModel = columns.getColumn(index);
        columnModel.setWidth(scaledWidth);
        columnModel.setPreferredWidth(scaledWidth);
        columnModel.setCellRenderer(cellRenderer);
    }

    @Override
    public void afterMapChange(MapModel oldMap, MapModel newMap) {
        update();
    }

    void update() {
        Controller controller = Controller.getCurrentController();
        update(controller.getSelection());
    }

    @Override
    public void onSelectionSetChange(IMapSelection selection) {
        update(selection);
    }

    @Override
    public void mapChanged(MapChangeEvent event) {
        if(event.getProperty().equals(Filter.class))
            SwingUtilities.invokeLater(this::update);
    }


    private void update(IMapSelection selection) {
        Set<CodeDependency> selectedDependencies = getSelectedCodeDependencies().collect(Collectors.toSet());
        int selectedColumn = dependencyViewer.getSelectedColumn();
        this.allDependencies = Collections.emptyList();
        ((DependenciesWrapper)dependencyViewer.getModel()).fireTableDataChanged();
        if (selection != null && selection.getMap() instanceof CodeMap) {
			this.allDependencies = selectedDependencies(new SelectedNodeDependencies(selection));
			updateDependencyFilter();
		} else {
	        TableRowSorter<?> rowSorter = (TableRowSorter<?>)dependencyViewer.getRowSorter();
	        rowSorter.setRowFilter(null);
			updateRowCountLabelAndFilterAction();
		}
        if(! selectedDependencies.isEmpty()) {
            IntStream.range(0, allDependencies.size())
            .filter(i -> selectedDependencies.contains(allDependencies.get(i)))
            .map(dependencyViewer::convertRowIndexToView)
            .forEach(row -> dependencyViewer.addRowSelectionInterval(row, row));
            if(dependencyViewer.getSelectedRow() != -1) {
                dependencyViewer.setColumnSelectionInterval(selectedColumn, selectedColumn);
                SwingUtilities.invokeLater(this::scrollSelectedToVisible);
            }
        }
        if(isShowing())
            dependencySelectionCallbacks.stream().forEach(x -> x.accept(this));
    }

    private List<CodeDependency> selectedDependencies(SelectedNodeDependencies selectedNodeDependencies) {
        return selectedNodeDependencies.getSelectedDependencies().map(selectedNodeDependencies.getMap()::toCodeDependency)
        .collect(Collectors.toCollection(ArrayList::new));
    }

    private Stream<CodeDependency> getSelectedCodeDependencies() {
        return IntStream.of(dependencyViewer.getSelectedRows())
        .map(dependencyViewer::convertRowIndexToModel)
        .mapToObj(allDependencies::get);
    }

    private Set<Dependency> getSelectedDependencies() {
        return getSelectedCodeDependencies()
                .map(CodeDependency::getDependency)
                .collect(Collectors.toSet());
    }

    public Set<Dependency> getFilteredDependencies() {
        Set<Dependency> selectedDependencies = getSelectedDependencies();
        if(! selectedDependencies.isEmpty())
            return selectedDependencies;
        else if(dependencyViewer.getRowCount() < allDependencies.size())
            return getVisibleDependencies();
        else
            return Collections.emptySet();
    }

    public Set<JavaClass> getSelectedClasses() {
        return getFilteredDependencies().stream()
                .flatMap(d -> Stream.of(d.getOriginClass(), d.getTargetClass()))
                .collect(Collectors.toSet());
    }

    private Set<Dependency> getVisibleDependencies() {
        return IntStream.range(0, dependencyViewer.getRowCount())
        .map(dependencyViewer::convertRowIndexToModel)
        .mapToObj(allDependencies::get)
        .map(CodeDependency::getDependency)
        .collect(Collectors.toSet());
    }

    private void updateRowCountLabelAndFilterAction() {
        int rowCount = dependencyViewer.getRowCount();
        int dependencyCount;

        if (!isLastColumnVisible) {
            // When last column is hidden, count unique dependencies based on origin/target
            Set<String> uniqueSignatures = new HashSet<>();
            for (CodeDependency dependency : allDependencies) {
                StringBuilder signature = new StringBuilder();
                signature.append(toDisplayedFullName(dependency.getOriginClass()))
                         .append("||")
                         .append(toDisplayedFullName(dependency.getTargetClass()));
                uniqueSignatures.add(signature.toString());
            }
            dependencyCount = uniqueSignatures.size();
        } else {
            // When last column is visible, use the full count
            dependencyCount = allDependencies.size();
        }

        countLabel.setText("( " + rowCount + " / " + dependencyCount + " )");
        enableFilterAction();
    }
    private void enableFilterAction() {
        int rowCount = dependencyViewer.getRowCount();
        int dependencyCount = allDependencies.size();
        int selectedRowCount = dependencyViewer.getSelectedRowCount();
        filterAction.setEnabled(rowCount > 0 && rowCount < dependencyCount
                || selectedRowCount > 0 && selectedRowCount  < dependencyCount);
    }

    private void scrollSelectedToVisible() {
        int selectedRowOnView = dependencyViewer.getSelectedRow();
        if (selectedRowOnView != -1) {
            dependencyViewer.scrollRectToVisible(new Rectangle(dependencyViewer.getCellRect(selectedRowOnView, 0, true)));
        }
    }

    @Override
    public void propertyChanged(String propertyName, String newValue, String oldValue) {
        if(propertyName.equals("code_showOutsideDependencies")) {
            Controller controller = Controller.getCurrentController();
            IMapSelection selection = controller.getSelection();
            update(selection);
            controller.getMapViewManager().getMapViewComponent().repaint();
        }
    }

    void addDependencySelectionCallback(Consumer<Object > listener) {
        dependencyViewer.getSelectionModel().addListSelectionListener(
                e -> {
                    if(!e.getValueIsAdjusting()) {
                        listener.accept(this);
                        enableFilterAction();
                    }
                });
        dependencyViewer.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                if(! e.isTemporary())
                    listener.accept(this);
            }

        });
        dependencySelectionCallbacks.add(listener);
    }

    private String toDisplayedFullName(JavaClass originClass) {
        return CodeNode.findEnclosingNamedClass(originClass).getName().replace('$', '.');
    }

    private void toggleLastColumnVisibility() {
        if (isLastColumnVisible) {
            hideLastColumn();
        } else {
            showLastColumn();
        }
        isLastColumnVisible = !isLastColumnVisible;

        // Update filters to reflect new column visibility state
        updateDependencyFilter();
    }

    private void hideLastColumn() {
        TableColumnModel columnModel = dependencyViewer.getColumnModel();
        if (columnModel.getColumnCount() > 3) {
            TableColumn column = columnModel.getColumn(3);
            columnModel.removeColumn(column);
        }
    }

    private void showLastColumn() {
        TableColumnModel columnModel = dependencyViewer.getColumnModel();
        if (columnModel.getColumnCount() == 3) {
            TableColumn column = new TableColumn(3);
            column.setHeaderValue(COLUMN_NAMES[3]);
            columnModel.addColumn(column);
            updateColumn(columnModel, 3, 1200, new CellRendererWithTooltip());
        }
    }
}