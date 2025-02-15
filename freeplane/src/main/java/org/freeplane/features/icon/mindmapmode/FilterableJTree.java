package org.freeplane.features.icon.mindmapmode;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.util.*;
import java.util.function.Predicate;

public class FilterableJTree extends JTree {

    private static final long serialVersionUID = 1L;

    public FilterableJTree(TreeModel model) {
        super(wrapModel(model));
    }

    @Override
    public void setModel(TreeModel model) {
        super.setModel(wrapModel(model));
    }

    private static TreeModel wrapModel(TreeModel model) {
        return new FilterableTreeModel(model);
    }

    /**
     * Applies a new filter while preserving expanded state and selection.
     * If a previously selected node is hidden, selects its closest visible ancestor.
     */
    public void setFilter(Predicate<Object> filter) {
        TreeModel m = getModel();
        if (!(m instanceof FilterableTreeModel)) return;
        FilterableTreeModel model = (FilterableTreeModel) m;

        // Save current selection.
        TreePath[] oldSelection = getSelectionPaths();

        // Save current expanded paths.
        TreePath rootPath = new TreePath(model.getRoot());
        List<TreePath> expandedPaths = new ArrayList<>();
        Enumeration<TreePath> en = getExpandedDescendants(rootPath);
        if (en != null) while (en.hasMoreElements()) expandedPaths.add(en.nextElement());

        // Apply the filter.
        model.setFilter(filter);
        updateUI();

        // Restore expanded paths that are still visible.
        for (TreePath path : expandedPaths) {
            if (getRowForPath(path) != -1) expandPath(path);
        }

        // Restore selection.
        if (oldSelection != null && oldSelection.length > 0) {
            List<TreePath> newSelection = new ArrayList<>();
            for (TreePath sel : oldSelection) {
                TreePath visible = findClosestVisiblePath(sel);
                if (visible != null) newSelection.add(visible);
            }
            if (!newSelection.isEmpty()) setSelectionPaths(newSelection.toArray(new TreePath[0]));
            else setSelectionPath(rootPath);
        }
    }

    /**
     * Traverses upward until a visible node is found.
     */
    private TreePath findClosestVisiblePath(TreePath path) {
        while (path != null && getRowForPath(path) == -1) path = path.getParentPath();
        return path;
    }

    /**
     * Inner class encapsulating filtering logic with selective cache invalidation.
     * Since the filter depends only on the node's own user object, we only need to
     * invalidate the cache entry for the parent when its child list changes.
     */
    private static class FilterableTreeModel implements TreeModel, TreeModelListener {
        private final TreeModel delegate;
        private Predicate<Object> filter = null; // null means no filtering
        private final Map<Object, List<Object>> filteredCache = new WeakHashMap<>();

        public FilterableTreeModel(TreeModel delegate) {
            this.delegate = delegate;
            delegate.addTreeModelListener(this);
        }

        public void setFilter(Predicate<Object> filter) {
            this.filter = filter;
            filteredCache.clear();
            // Optionally fire a structure change event.
        }

        @Override
        public Object getRoot() {
            return delegate.getRoot();
        }

        @Override
        public Object getChild(Object parent, int index) {
            return isFiltering() ? getFilteredChildren(parent).get(index)
                                 : delegate.getChild(parent, index);
        }

        @Override
        public int getChildCount(Object parent) {
            return isFiltering() ? getFilteredChildren(parent).size()
                                 : delegate.getChildCount(parent);
        }

        @Override
        public boolean isLeaf(Object node) {
            if (delegate.isLeaf(node)) return true;
            return isFiltering() && getFilteredChildren(node).isEmpty();
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            return isFiltering() ? getFilteredChildren(parent).indexOf(child)
                                 : delegate.getIndexOfChild(parent, child);
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
            delegate.addTreeModelListener(l);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
            delegate.removeTreeModelListener(l);
        }

        private boolean isFiltering() {
            return filter != null;
        }

        /**
         * Lazily computes filtered children and caches the result.
         */
        private List<Object> getFilteredChildren(Object node) {
            return filteredCache.computeIfAbsent(node, n -> {
                List<Object> visible = new ArrayList<>();
                int count = delegate.getChildCount(n);
                for (int i = 0; i < count; i++) {
                    Object child = delegate.getChild(n, i);
                    List<Object> childVisible = getFilteredChildren(child);
                    // Since the filter depends only on the node's user object,
                    // we only need to test the node itself.
                    if (filter.test(child) || !childVisible.isEmpty()) {
                        visible.add(child);
                    }
                }
                return visible;
            });
        }

        /**
         * Invalidate cache for the given nodes.
         * With the given assumption, it's sufficient to invalidate only the parent's cache.
         */
        private void invalidateCache(Object[] nodes) {
            if (nodes != null) {
                for (Object node : nodes) {
                    // Instead of invalidating each child's cache, we rely on the parent's cache being recomputed.
                    filteredCache.remove(node);
                }
            }
        }

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            if(isFiltering())
                invalidateCache(e.getPath());
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            if(isFiltering())
                invalidateCache(e.getPath());
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            if(isFiltering())
                invalidateCache(e.getPath());
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            if(isFiltering())
                filteredCache.clear();
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            delegate.valueForPathChanged(path, newValue);
            if(isFiltering())
                invalidateCache(path.getPath());
        }
    }
}
