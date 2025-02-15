/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2011 dimitry
 *
 *  This file author is dimitry
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
package org.freeplane.features.icon.mindmapmode;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;

/**
 * @author Dimitry Polivaev
 * Jan 9, 2011
 */
public class TagPanelManager {
    final private JPanel tagPanel;
    private JTree tagTree;
    private Font treeFont;
    private TagCategories treeCategories;
    private final MIconController iconController;
    private final JButton editCategoriesButton;

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

    // Updates the panel with the tree (if available) and always the button underneath, left-aligned.
    private void updateTreeAndButton(JTree tree) {
        tagPanel.removeAll();
        int gridy = 0;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);

        if (tree != null) {
            gbc.gridy = gridy++;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            tagPanel.add(tree, gbc);
        }

        // Add the button with left alignment and constant width.
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
}
