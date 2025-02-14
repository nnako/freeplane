/*
 * Created on 14 Feb 2025
 *
 * author dimitry
 */
package org.freeplane.features.icon.mindmapmode;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.freeplane.core.ui.components.TagIcon;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.TagCategories;

@SuppressWarnings("serial") class TagCellRenderer extends DefaultTreeCellRenderer {
    private final TagCategories tagCategories;

	public TagCellRenderer(TagCategories tagCategories) {
        this.tagCategories = tagCategories;
		setHorizontalAlignment(CENTER);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, null, sel, expanded, leaf, row, hasFocus);
        if (value instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Tag tag = tagCategories.tagWithoutCategories(node);
            if (! tag.isEmpty()) {
                setText(null);
                Font font = getFont();
                setIcon(new TagIcon(tag, font, getFontMetrics(font).getFontRenderContext()));
            } else if (node.getUserObject() != null) {
                setText(node.getUserObject().toString());
            }
        }

        return this;
    }
}