package org.freeplane.plugin.codeexplorer.configurator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
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
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.IMapSelectionListener;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.codeexplorer.dependencies.CodeDependency;
import org.freeplane.plugin.codeexplorer.map.ClassNode;
import org.freeplane.plugin.codeexplorer.map.CodeMap;
import org.freeplane.plugin.codeexplorer.map.CodeNode;
import org.freeplane.plugin.codeexplorer.map.SelectedNodeDependencies;
import org.freeplane.plugin.codeexplorer.task.GroupMatcher.MatchingCriteria;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;

class CodeDependenciesPanel extends JPanel implements INodeSelectionListener, IMapSelectionListener, IFreeplanePropertyListener, IMapChangeListener{

    private static final String[] COLUMN_NAMES = new String[]{"Verdict", "Origin", "Target","Dependency"};

    private static final long serialVersionUID = 1L;
    private static final Icon filterIcon = ResourceController.getResourceController().getIcon("filterDependencyIncormation.icon");
    private final JTextField filterField;
    private final JTable dependencyViewer;
    private final JLabel countLabel;
    private final List<Consumer<Object>> dependencySelectionCallbacks;
    private List<CodeDependency> allDependencies;

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
                case 1: return ClassNode.classNameWithEnclosingClasses(row.getOriginClass());
                case 2: return ClassNode.classNameWithEnclosingClasses(row.getTargetClass());
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
    }

    CodeDependenciesPanel() {
         dependencySelectionCallbacks = new ArrayList<>();
         // Create the top panel for sorting options
         JPanel topPanel = new JPanel(new BorderLayout());

         // Create a box to hold the components that should be aligned to the left
         countLabel = new JLabel(filterIcon);
         final int countLabelMargin = (int) (UITools.FONT_SCALE_FACTOR * 10);
         countLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, countLabelMargin));
         countLabel.setIconTextGap(countLabelMargin / 2);

         // Add the box of left-aligned components to the top panel at the WEST
         topPanel.add(countLabel, BorderLayout.WEST);

         // Configure filterField to expand and fill the remaining space
         filterField = new JTextField();
         filterField.addActionListener(e -> updateDependencyFilter());
         // Add the filterField to the CENTER to occupy the maximum available space
         topPanel.add(filterField, BorderLayout.CENTER);

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
        updateColumn(columnModel, 0, 200, cellRenderer);
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
        if(filteredWords.length == 1 && filteredWords[0].isEmpty())
            rowSorter.setRowFilter(null);
        else {
            RowFilter<DependenciesWrapper, Integer> dependencyFilter = new RowFilter<DependenciesWrapper, Integer>() {
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
                    } else if (searchedString.equalsIgnoreCase("speciality:rmi")) {
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

            rowSorter.setRowFilter(dependencyFilter);
        }
        dependencySelectionCallbacks.stream().forEach(x -> x.accept(this));
        scrollSelectedToVisible();
        countLabel.setText("( " + rowSorter.getViewRowCount() + " / " + rowSorter.getModelRowCount() + " )");
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
        this.allDependencies = selection == null || ! (selection.getMap() instanceof CodeMap)
                ? Collections.emptyList() :
                    selectedDependencies(new SelectedNodeDependencies(selection));
        ((DependenciesWrapper)dependencyViewer.getModel()).fireTableDataChanged();
        updateRowCountLabel();
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

    private Set<Dependency> getVisibleDependencies() {
        return IntStream.range(0, dependencyViewer.getRowCount())
        .map(dependencyViewer::convertRowIndexToModel)
        .mapToObj(allDependencies::get)
        .map(CodeDependency::getDependency)
        .collect(Collectors.toSet());
    }

    private void updateRowCountLabel() {
        countLabel.setText("( " + dependencyViewer.getRowCount() + " / " + allDependencies.size() + " )");
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
                    if(!e.getValueIsAdjusting())
                        listener.accept(this);
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
}