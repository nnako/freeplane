package org.freeplane.features.icon.mindmapmode;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.JLabel;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.svgicons.FreeplaneIconFactory;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.factory.IconFactory;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;

public class TagPanelManager {
    final private JPanel tagPanel;
    private JTagTree tagTree; // JTagTree extends our FilterableJTree
    private Font treeFont;
    private TagCategories treeCategories;
    private final MIconController iconController;
    private final JButton editCategoriesButton;

    // The search field and its timer.
    private final JTextField filterField = new JTextField();
    private Timer filterTimer;

    private class TableCreator implements INodeSelectionListener, IMapChangeListener {

        @Override
        public void onDeselect(NodeModel node) {
            TagPanelManager.this.updateTreeAndButton(null);
        }

        @Override
        public void onSelect(NodeModel node) {
            onChange(node.getMap());
        }

        private void onChange(MapModel map) {
            TagCategories newCategories = map.getIconRegistry().getTagCategories();
            Font newFont = iconController.getTagFont(map.getRootNode());
            if (tagTree == null || treeCategories != newCategories || !treeFont.equals(newFont)) {
                treeCategories = newCategories;
                treeFont = newFont;
                tagTree = new TagTreeViewerFactory(newCategories, newFont).getTree();
                tagTree.setFont(newFont);
                tagTree.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if(e.getClickCount() != 2)
                            return;
                        JTagTree tree = (JTagTree) e.getSource();
                        int x = e.getX();
                        int y = e.getY();
                        TreePath path = tree.getPathForLocation(x, y);
                        if (path == null) return; // Clicked outside any node
                        Rectangle nodeBounds = tree.getUI().getPathBounds(tree, path);
                        if (nodeBounds == null) return;
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Tag tag = treeCategories.categorizedTag(node);
                        iconController.insertTagsIntoSelectedNodes(Collections.singletonList(tag));
                    }
                });
                // (Re)hook our filter field listener to update the tree filter.
                setupFilterField();
            }
            TagPanelManager.this.updateTreeAndButton(tagTree);
        }

        @Override
        public void mapChanged(MapChangeEvent event) {
            onChange(event.getMap());
        }
    }

    public TagPanelManager(ModeController modeController) {
        tagPanel = new JPanel(new GridBagLayout());
        tagPanel.setMinimumSize(new Dimension(0, 0));
        tagPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Create button with constant width
        editCategoriesButton = new JButton(modeController.getAction("ManageTagCategoriesAction"));

        final TableCreator tableCreator = new TableCreator();
        final MapController mapController = modeController.getMapController();
        mapController.addNodeSelectionListener(tableCreator);
        mapController.addMapChangeListener(tableCreator);
        iconController = (MIconController) modeController.getExtension(IconController.class);

        updateTreeAndButton(null);
    }

    public JPanel getTagPanel() {
        return tagPanel;
    }

    // Updates the panel with the filter field at the top, then the tree (if available) and always the button underneath.
    private void updateTreeAndButton(JTree tree) {
        tagPanel.removeAll();
        int gridy = 0;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 5, 0, 5);

        // Add the filter label and field at the top in a row
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setOpaque(false);

        // Add the label
        JLabel filterLabel = new JLabel();
        Icon filterIcon = ResourceController.getResourceController().getIcon("filterIcon");
        Icon scaledIcon = IconFactory.getInstance().getScaledIcon(filterIcon, filterLabel);
        filterLabel.setIcon(FreeplaneIconFactory.toImageIcon(scaledIcon));
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = 0;
        labelGbc.insets = new Insets(0, 0, 0, 5); // 5-pixel gap between label and field
        filterPanel.add(filterLabel, labelGbc);

        // Add the filter field
        GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.gridx = 1;
        fieldGbc.gridy = 0;
        fieldGbc.fill = GridBagConstraints.HORIZONTAL;
        fieldGbc.weightx = 1.0;
        filterPanel.add(filterField, fieldGbc);

        // Add the filter panel to the main panel
        gbc.gridy = gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 1.0;
        tagPanel.add(filterPanel, gbc);

        if (tree != null) {
            gbc.gridy = gridy++;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(0, 5, 5, 5);
            tagPanel.add(tree, gbc);
        }

        // Add the button with left alignment.
        GridBagConstraints gbcButton = new GridBagConstraints();
        gbcButton.gridx = 0;
        gbcButton.gridy = gridy++;
        gbcButton.insets = new Insets(5, 5, 5, 5);
        gbcButton.anchor = GridBagConstraints.WEST;
        gbcButton.fill = GridBagConstraints.NONE;
        tagPanel.add(editCategoriesButton, gbcButton);

        // Filler to push components to the top.
        GridBagConstraints gbcFiller = new GridBagConstraints();
        gbcFiller.gridx = 0;
        gbcFiller.gridy = gridy;
        gbcFiller.weighty = 1.0;
        gbcFiller.fill = GridBagConstraints.VERTICAL;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        tagPanel.add(filler, gbcFiller);

        tagPanel.revalidate();
        tagPanel.repaint();
    }

    /**
     * Setup the filter field to start/restart a timer on text changes.
     * When the timer fires, the content of the text field is used to build a predicate
     * which is then set on the JTagTree.
     */
    private void setupFilterField() {
        // Remove any existing listeners/timers first.
        if (filterTimer != null) {
            filterTimer.stop();
        }
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            private void restartTimer() {
                if (filterTimer != null) {
                    filterTimer.restart();
                }
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                restartTimer();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                restartTimer();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                restartTimer();
            }
        });
        // Delay of 300 ms (adjust as needed).
        filterTimer = new Timer(300, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterTimer.stop();
                String text = filterField.getText().trim();
                // Build predicate: if empty, no filtering; else, filter nodes whose user object toString() contains the text.
                if (tagTree != null) {
                    tagTree.setFilter(text.isEmpty() ? null : node -> node.toString().toLowerCase().contains(text.toLowerCase()));
                }
            }
        });
        filterTimer.setRepeats(false);
    }
}
