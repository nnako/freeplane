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
package org.freeplane.view.swing.map;

import java.awt.AWTKeyStroke;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.dnd.Autoscroll;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.freeplane.api.ChildNodesAlignment;
import org.freeplane.api.ChildrenSides;
import org.freeplane.api.LayoutOrientation;
import org.freeplane.core.awt.GraphicsHints;
import org.freeplane.core.extension.Configurable;
import org.freeplane.core.extension.HighlightedElements;
import org.freeplane.core.io.xml.TreeXmlReader;
import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.AntiAliasingConfigurator;
import org.freeplane.core.ui.IUserInputListenerFactory;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.ColorUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.attribute.ModelessAttributeController;
import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.edge.EdgeColorsConfigurationFactory;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.highlight.NodeHighlighter;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.link.ConnectorModel;
import org.freeplane.features.link.ConnectorShape;
import org.freeplane.features.link.Connectors;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.NodeLinkModel;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.INodeChangeListener;
import org.freeplane.features.map.INodeView;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeRelativePath;
import org.freeplane.features.map.NodeSubtrees;
import org.freeplane.features.map.SummaryNode;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodestyle.NodeCss;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.note.NoteController;
import org.freeplane.features.print.FitMap;
import org.freeplane.features.styles.LogicalStyleController.StyleOption;
import org.freeplane.features.styles.MapStyle;
import org.freeplane.features.styles.MapStyleModel;
import org.freeplane.features.styles.MapViewLayout;
import org.freeplane.features.text.TextController;
import org.freeplane.features.ui.IMapViewManager;
import org.freeplane.features.url.UrlManager;
import org.freeplane.view.swing.features.filepreview.IViewerFactory;
import org.freeplane.view.swing.features.filepreview.ScalableComponent;
import org.freeplane.view.swing.features.filepreview.ViewerController;
import org.freeplane.view.swing.map.MapViewScrollPane.MapViewPort;
import org.freeplane.view.swing.map.NodeView.PreferredChild;
import org.freeplane.view.swing.map.link.ConnectorView;
import org.freeplane.view.swing.map.link.EdgeLinkView;
import org.freeplane.view.swing.map.link.ILinkView;

/**
 * This class represents the view of a whole MindMap (in analogy to class
 * JTree).
 */
public class MapView extends JPanel implements Printable, Autoscroll, IMapChangeListener, IFreeplanePropertyListener, Configurable {

    private static final String MAP_VIEW_ZOOM_STEP_PROPERTY = "map_view_zoom_step";

    public enum SelectionDirection {RIGHT, LEFT, DOWN, UP;

        boolean isHorizontal() {
            return this == RIGHT || this == LEFT;
        }

        boolean isVertical() {
            return this == DOWN || this == UP;
        }

        boolean isForward() {
            return this == RIGHT || this == DOWN;
        }

        boolean isBackward() {
            return this == LEFT || this == UP;
        }
    }

    private enum PaintingPurpose {PAINTING, PRINTING, OVERVIEW}

    private static final int ROOT_NODE_COMPONENT_INDEX = 0;
	private static final String UNFOLD_ON_NAVIGATION = "unfold_on_navigation";
	private static final String SYNCHRONIZE_SELECTION_ACROSS_VISIBLE_VIEWS_PROPERTY = "synchronizeSelectionAcrossVisibleViews";
	private static final String SYNCHRONIZE_SELECTION_ONLY_ON_BRANCH_CHANGE = "synchronizeSelectionOnlyOnBranchChange";
	private static final String SHOW_TAGS_ON_MINIMIZED_NODES_PROPERTY = "showTagsOnMinimizedNodes";

    private static final BasicStroke SELECTION_RECTANGLE_STROKE = new BasicStroke(2.0f * UITools.FONT_SCALE_FACTOR, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f * UITools.FONT_SCALE_FACTOR,
            new float[] {5.0f * UITools.FONT_SCALE_FACTOR, 5.0f * UITools.FONT_SCALE_FACTOR}, 0.0f);

	private final MapScroller mapScroller;
	private MapViewLayout layoutType;
	private boolean paintConnectorsBehind;
	private Filter filter;

	public static boolean isElementHighlighted(final Component c, final Object element) {
		final MapView mapView = (MapView) SwingUtilities.getAncestorOfClass(MapView.class, c);
		if (mapView == null)
			return false;
		final HighlightedElements highlightedElements = mapView.getExtension(HighlightedElements.class);
		if (highlightedElements == null)
			return false;
		else {
			return highlightedElements.isContained(element);
		}
	}

	public MapViewLayout getLayoutType() {
		return layoutType;
	}

	protected void setLayoutType(final MapViewLayout layoutType) {
		if(this.layoutType != layoutType) {
			this.layoutType = layoutType;
			getRoot().resetLayoutPropertiesRecursively();
			if(outlineViewFitsWindowWidth())
			    updateAllNodeViews();
		}
	}

	private void updateAllNodeViews() {
		updateAllNodeViews(UpdateCause.UNKNOWN);
	}
    private void updateAllNodeViews(UpdateCause cause) {
        getRoot().updateAll(cause);
        if(mapRootView != currentRootView)
            mapRootView.updateAll(cause);
    }

	private boolean showNotes;

	boolean showNotes() {
		return showNotes;
	}

	private void setShowNotes() {
		final boolean showNotes= NoteController.getController(getModeController()).showNotesInMap(getMap());
		if(this.showNotes == showNotes){
			return;
		}
		this.showNotes = showNotes;
		updateAllNodeViews();
	}

	private PaintingMode paintingMode = null;

	@Override
	public void refresh() {
		repaint();
	}

	private class MapSelection implements IMapSelection {
		@Override
		public void moveNodeTo(final NodeModel node, final NodePosition position) {
			final boolean slowScroll = false;
			moveNodeTo(node, position, slowScroll);
		}

		@Override
		public void slowlyMoveNodeTo(final NodeModel node, final NodePosition position) {
			final boolean slowScroll = true;
			moveNodeTo(node, position, slowScroll);
		}

		private void moveNodeTo(final NodeModel node, final NodePosition position, final boolean slowScroll) {
			final NodeView nodeView = getNodeView(node);
			if (nodeView != null) {
				mapScroller.scrollNode(nodeView, position, slowScroll);
			}
		}

		@Override
		public NodeModel getSelected() {
			final NodeView selected = MapView.this.getSelected();
			return selected != null ? selected.getNode() : null;
		}

        @Override
        public NodeModel getSelectionRoot() {
            final NodeView root = MapView.this.getRoot();
            return root != null ? root.getNode() : null;
        }

        @Override
        public NodeModel getSearchRoot() {
            if(currentRootView == null)
                return null;
            NodeModel searchRoot = MapView.this.getSearchRoot();
            if(searchRoot == null)
                return null;
            NodeModel currentRoot = currentRootView.getNode();
            return searchRoot == currentRoot || searchRoot.isDescendantOf(currentRoot) ? searchRoot : null;
        }

        @Override
        public NodeModel getEffectiveSearchRoot() {
            if(currentRootView == null)
                return null;
            final NodeModel searchRoot = getSearchRoot();
            return searchRoot == null ? currentRootView.getNode() : searchRoot;
        }
		@Override
		public Set<NodeModel> getSelection() {
			return getSelectedNodes();
		}


		@Override
		public List<NodeModel> getOrderedSelection() {
			return getOrderedSelectedNodes();
        }
		@Override
		public List<NodeModel> getSortedSelection(final boolean differentSubtrees) {
			return getSelectedNodesSortedByY(differentSubtrees);
		}

		@Override
		public boolean isSelected(final NodeModel node) {
			if (! getMap().equals(node.getMap()))
				return false;
			final NodeView nodeView = getNodeView(node);
			return nodeView != null && MapView.this.isSelected(nodeView);
		}

		@Override
		public void preserveRootNodeLocationOnScreen() {
            MapView.this.preserveRootNodeLocationOnScreen();
		}

        @Override
        public void preserveSelectedNodeLocationOnScreen() {
            MapView.this.preserveSelectedNodeLocation();
        }

        @Override
        public void preserveNodeLocationOnScreen(NodeModel node) {
            final NodeView nodeView = getNodeView(node);
            MapView.this.preserveNodeLocationOnScreen(nodeView);
        }

        @Override
        public void preserveNodeLocationOnScreen(final NodeModel node, final float horizontalPoint, final float verticalPoint) {
            final NodeView nodeView = getNodeView(node);
            MapView.this.preserveNodeLocationOnScreen(nodeView, horizontalPoint, verticalPoint);
        }

		@Override
		public void scrollNodeTreeToVisible(final NodeModel  node) {
			scrollNodeTreeToVisible(node, mapScroller.shouldScrollSlowly());
		}

		@Override
		public void scrollNodeTreeToVisible(final NodeModel  node, boolean slow) {
			final NodeView nodeView = getNodeView(node);
			if(nodeView != null)
				mapScroller.scrollNodeTreeToVisible(nodeView, slow);
		}


        @Override
        public void makeTheSelected(final NodeModel node) {
            final NodeView nodeView = getNodeView(node);
            if (nodeView != null) {
                addSelected(nodeView, false);
            }
        }

        @Override
        public void makeTheSearchRoot(final NodeModel node) {
            setSearchRoot(node);
        }

		@Override
		public void scrollNodeToVisible(final NodeModel node) {
			mapScroller.scrollNodeToVisible(getNodeView(node));
		}

		@Override
		public void scrollNodeToCenter(final NodeModel node) {
			scrollNodeToCenter(node, mapScroller.shouldScrollSlowly());
		}

		@Override
		public void scrollNodeToCenter(final NodeModel node, boolean slow) {
			mapScroller.scrollNodeToCenter(getNodeView(node), slow);
		}

		@Override
		public void selectAsTheOnlyOneSelected(final NodeModel node) {
			if(node.isVisible(filter) || currentRootView.getNode() == node)
				display(node, true);
			final NodeView nodeView = getNodeView(node);
			if (nodeView != null) {
				MapView.this.selectAsTheOnlyOneSelected(nodeView);
			}
		}

		@Override
		public void selectBranch(final NodeModel node, final boolean extend) {
			if(! extend)
				selectAsTheOnlyOneSelected(node);
			addBranchToSelection(getNodeView(node));
		}

		@Override
		public void selectContinuous(final NodeModel node) {
			MapView.this.selectContinuous(getNodeView(node));
		}

		@Override
		public void selectRoot() {
			final NodeModel rootNode = currentRootView.getNode();
			selectAsTheOnlyOneSelected(rootNode);
			mapScroller.scrollToRootNode();
		}

		@Override
		public int size() {
			return selection.size();
		}

		@Override
		public void toggleSelected(final NodeModel node) {
			display(node, true);
			NodeView nodeView = getNodeView(node);
			MapView.this.toggleSelected(nodeView);
		}

        @Override
		public void replaceSelection(final NodeModel[] nodes) {
            if(nodes.length == 0)
                return;
            final ArrayList<NodeView> views = new ArrayList<NodeView>(nodes.length);
            for(final NodeModel node : nodes) {
            	if(node != null && (node.isVisible(filter) || currentRootView.getNode() == node)){
            		display(node, true);
            		final NodeView nodeView = getNodeView(node);
            		if (nodeView != null) {
            			views.add(nodeView);
            		}
            	}
            }
            if(! views.isEmpty())
            	MapView.this.replaceSelection(views.toArray(new NodeView[]{}));
        }

		@Override
		public List<String> getOrderedSelectionIds() {
			final List<NodeModel> orderedSelection = getOrderedSelection();
			final ArrayList<String> ids = new ArrayList<>(orderedSelection.size());
			for(final NodeModel node :orderedSelection)
				ids.add(node.getID());
			return ids;
		}

	    @Override
        public Filter getFilter() {
	        return filter;
	    }

	    @Override
        public void setFilter(Filter filter) {
	        MapView.this.filter = filter;
	    }

        @Override
        public boolean isFolded(NodeModel node) {
            NodeView nodeView = getNodeView(node);
            return nodeView != null ? nodeView.isFolded() : node.isFolded();
        }

        @Override
        public boolean isVisible(NodeModel node) {
            NodeView nodeView = getNodeView(node);
            return nodeView != null
                    && nodeView.isContentVisible()
                    && SwingUtilities.isDescendingFrom(nodeView, currentRootView);
        }
	}

	private class Selection {
        final private Set<NodeView> selectedSet = new LinkedHashSet<NodeView>();
		final private List<NodeView> selectedList = new ArrayList<NodeView>();
		private NodeView selectedNode = null;
		private boolean selectionChanged = false;

		public Selection() {
		}

		private void select(final NodeView node) {
			final NodeView[] oldSelecteds = selection.toArray();
			clear();
			addToSelectedSet(node);
			selectedList.add(node);
			selectedNode = node;
			addSelectionForHooks();
			repaintAfterSelectionChange(node, true);
			for (final NodeView oldSelected : oldSelecteds) {
				if (oldSelected != null && oldSelected != node) {
					repaintAfterSelectionChange(oldSelected, true);
				}
			}
		}

		private boolean add(final NodeView node) {
			if(selectedNode == null){
				select(node);
				return true;
			}
			else{
				if(addToSelectedSet(node)){
					selectedList.add(node);
					repaintAfterSelectionChange(node, true);
					return true;
				}
				return false;
			}
		}

        private boolean addToSelectedSet(final NodeView node) {
            boolean hasChanged = selectedSet.add(node);
            if(hasChanged) {
                fireSelectionChangedLater();
            }
            return hasChanged;
        }

        private void fireSelectionChangedLater() {
            if(! selectionChanged && isSelected()) {
                selectionChanged = true;
                SwingUtilities.invokeLater(this::fireSelectionChanged);
            }
        }

		private void addSelectionForHooks() {
			if(! isSelected())
				return;
			final MapController mapController = modeController.getMapController();
			final NodeModel node = selectedNode.getNode();
			mapController.onSelect(node);
			synchronizeAcrossVisibleViews(MapView::synchronizeSelection);
		}

        private void synchronizeAcrossVisibleViews(Consumer<MapView> method) {
            boolean synchronizesSelectionAcrossVisibleViews = synchronizesSelectionAcrossVisibleViews();
            if(synchronizesSelectionAcrossVisibleViews) {
                List<Component> views = Controller.getCurrentController().getMapViewManager().getViews(viewedMap);
                for(Component view:views) {
                    if (view != MapView.this && view instanceof MapView && view.isShowing())
                        method.accept(((MapView)view));
                }
            }
        }

		private void clear() {
			if (selectedNode != null) {
				removeSelectionForHooks(selectedNode);
				selectedNode = null;
				clearSelectedSet();
				selectedList.clear();
			}
		}

        private void clearSelectedSet() {
            boolean hasChanged = ! selectedSet.isEmpty();
            if(hasChanged) {
                selectedSet.clear();
                fireSelectionChangedLater();
            }

        }

        private boolean contains(final NodeView node) {
			return selectedSet.contains(node);
		}

		public Set<NodeView> getSelection() {
			return Collections.unmodifiableSet(selectedSet);
		}

		private boolean deselect(final NodeView node) {
			final boolean selectedChanged = selectedNode != null && selectedNode.equals(node);
			if (selectedChanged) {
				removeSelectionForHooks(node);
			}
			if (removeFromSelectedSet(node)){
				final int last = selectedList.size() - 1;
				if(selectedList.get(last) .equals(node))
					selectedList.remove(last);
				else
					selectedList.remove(node);
				repaintAfterSelectionChange(node, true);
				return true;
			}
			return false;
		}

        private boolean removeFromSelectedSet(final NodeView node) {
            boolean hasChanged = selectedSet.remove(node);
            if(hasChanged) {
                fireSelectionChangedLater();
            }
            return hasChanged;
        }

        private void updateSelectedNode() {
            if(selectedNode != null && ! selectedSet.contains(selectedNode)) {
                if (size() > 0) {
                	selectedNode = selectedSet.iterator().next();
                	addSelectionForHooks();
                }
                else{
                	selectedNode = null;
                }
            }
        }

		private void removeSelectionForHooks(final NodeView node) {
			if (node.getNode() == null || ! isSelected()) {
				return;
			}
			getModeController().getMapController().onDeselect(node.getNode());
		}

		private int size() {
			return selectedSet.size();
		}

		private void replace(final NodeView[] newSelection) {
		    replace(Arrays.asList(newSelection));
		}

        private void replace(final List<NodeView> newSelection) {
            if(newSelection.size() == 0)
                return;
            final NodeView firstSelectedNode = newSelection.get(0);
            final boolean selectedChanges = ! firstSelectedNode.equals(selectedNode);
            if (selectedChanges) {
            	if(selectedNode != null)
            		removeSelectionForHooks(selectedNode);
            	selectedNode = firstSelectedNode;
            }
            NodeView[] nodesAddedToSelection = newSelection.stream()
                .filter(view -> ! selectedSet.contains(view))
                .toArray(NodeView[]::new);
            final NodeView[] oldSelection = selectedSet.toArray(new NodeView[selectedSet.size()]);
            clearSelectedSet();
            selectedList.clear();
            for(final NodeView view : newSelection)
                if (addToSelectedSet(view))
                	selectedList.add(view);

            for(final NodeView view : nodesAddedToSelection)
                repaintAfterSelectionChange(view, true);
            if (selectedChanges) {
                addSelectionForHooks();
            }
            for(final NodeView view : oldSelection)
                if (!selectedSet.contains(view))
                	repaintAfterSelectionChange(view, true);
        }

		public NodeView[] toArray() {
	        return selectedList.toArray(new NodeView[selectedList.size()]);
        }

		private List<NodeView> getSelectedList() {
	        return selectedList;
        }

		private Set<NodeView> getSelectedSet() {
	        return selectedSet;
        }

		public NodeView getSelectionStart() {
			return selectedList.size() > 0 ? selectedList.get(0) : null;
		}

		public NodeView getSelectionEnd() {
			int selectedNodeCount = selectedList.size();
            return selectedNodeCount > 0 ? selectedList.get(selectedNodeCount - 1) : null;
		}

		public NodeView getSelectionBeforeEnd() {
			int selectedNodeCount = selectedList.size();
            return selectedNodeCount > 1 ? selectedList.get(selectedNodeCount - 2) : null;
		}

	    void foldingWasSet(NodeView view) {
	        if(isClientPropertyTrue(FOLDING_FOLLOWS_SELECTION)) {
	            nodeViewFolder.foldingWasSet(view);
	        }
	    }


        private void fireSelectionChanged() {
            if(selectionChanged) {
                selectionChanged = false;
                if (isSelected()) {
                    if(selection.selectedNode != null)
                        modeController.getMapController().onSelectionChange(getMapSelection());
                    if(isClientPropertyTrue(FOLDING_FOLLOWS_SELECTION)) {
                        boolean wasFolded = selectedNode.isFolded();
                        nodeViewFolder.adjustFolding(selectedSet);
                        ResourceController resourceController = ResourceController.getResourceController();
                        if(wasFolded && ! selectedNode.isFolded() && selection.size() == 1
                                && (resourceController.getBooleanProperty("scrollOnUnfold") || resourceController.getBooleanProperty("scrollOnSelect")))
                                mapScroller.scrollNodeTreeToVisible(selectedNode, mapScroller.shouldScrollSlowly());
                        else
                            scrollNodeToVisible(selectedNode);
                    }

                }
                if(isValid())
                	revalidate();
            }
        }

	}

	private static final int AUTOSCROLL_MARGIN = (int) (UITools.FONT_SCALE_FACTOR * 40);
	static boolean printOnWhiteBackground;
	static private IFreeplanePropertyListener propertyChangeListener;
	public static final String RESOURCES_SELECTED_NODE_COLOR = "standardselectednodecolor";
	public static final String RESOURCES_SELECTED_NODE_RECTANGLE_COLOR = "standardselectednoderectanglecolor";
	private static final String SPOTLIGHT_BACKGROUND_COLOR = "spotlight_background_color";
	private static final String PRESENTATION_DIMMER_TRANSPARENCY = "presentation_dimmer_transparency";
	private static final String HIDE_SINGLE_END_CONNECTORS = "hide_single_end_connectors".intern();
	private static final String SHOW_CONNECTORS_PROPERTY = "show_connectors".intern();
	private static final String SHOW_CONNECTOR_LINES = "true".intern();
	private static final String HIDE_CONNECTOR_LINES = "false".intern();
	private static final String SOME_CONNECTORS_PROPERTY = "connector_";

	private static final String HIDE_CONNECTORS = "never".intern();
	private static final String SHOW_CONNECTORS_FOR_SELECTION_ONLY = "for_selection".intern();
	private static final String SHOW_ARROWS_FOR_SELECTION_ONLY = "only_arrows_for_selection".intern();
	private static final String OUTLINE_VIEW_FITS_WINDOW_WIDTH = "outline_view_fits_window_width";
	private static final String OUTLINE_HGAP_PROPERTY = "outline_hgap";
	private static final String DRAGGING_AREA_WIDTH_PROPERTY = "dragging_area_width";
	private static final String INLINE_EDITOR_ACTIVE = "inline_editor_active";
    public static final String SPOTLIGHT_ENABLED = "spotlight";
    public static final String FOLDING_FOLLOWS_SELECTION = "folding_follows_selection";

	static private final PropertyChangeListener repaintOnClientPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			final MapView source = (MapView) evt.getSource();
			source.repaint();
		}
	};

	private static final long serialVersionUID = 1L;
	static private boolean drawsRectangleForSelection;
	static private Color selectionRectangleColor;
	/** Used to identify a right click onto a link curve. */
	private Vector<ILinkView> arrowLinkViews;
	private ScalableComponent backgroundComponent;
	private Rectangle boundingRectangle = null;
	private FitMap fitMap = FitMap.USER_DEFINED;
	private boolean isPreparedForPrinting = false;
	private PaintingPurpose paintingPurpose = PaintingPurpose.PAINTING;
	private final ModeController modeController;
	private MapModel viewedMap;

	private NodeView currentRootView = null;
	private NodeModel currentSearchRoot = null;
	private NodeView currentRootParentView = null;
	private NodeView mapRootView = null;
	private List<NodeView> rootsHistory;

	private boolean selectedsValid = true;
	final private Selection selection = new Selection();
	private int siblingMaxLevel;
	private float zoom = 1F;
    private Font detailFont;
    private Color detailForeground;
    private Color detailBackground;
    private NodeCss detailCss;
    private int detailHorizontalAlignment;

    private Font noteFont;
    private Color noteForeground;
    private Color noteBackground;
    private NodeCss noteCss;
    private int noteHorizontalAlignment;
	private static String showConnectorsPropertyValue;
	private static boolean hideSingleEndConnectorsPropertyValue;
	private String showConnectors;
	private boolean hideSingleEndConnectors;
	private boolean fitToViewport;
	private static Color spotlightBackgroundColor;
	private static int outlineHGap;
	private static boolean outlineViewFitsWindowWidth;
	private static int draggingAreaWidth;
	private static boolean showsTagsOnMinimizedNodes;
	private Rectangle selectionRectangle = null;

	final private ComponentAdapter viewportSizeChangeListener;
	private final INodeChangeListener connectorChangeListener;
	private boolean scrollsViewAfterLayout = true;
	private boolean allowsCompactLayout;
	private boolean isAutoCompactLayoutEnabled;
    private TagLocation tagLocation;
    private IconLocation iconLocation;
    private boolean repaintsViewOnSelectionChange;

    public static final int SCROLL_VELOCITY_PX = (int) (UITools.FONT_SCALE_FACTOR  * 10);
    private final NodeViewFolder nodeViewFolder;
	private final AntiAliasingConfigurator antiAliasingConfigurator;

	static {
	    final ResourceController resourceController = ResourceController.getResourceController();
	    final String drawCircle = resourceController.getProperty(
	            ResourceController.RESOURCE_DRAW_RECTANGLE_FOR_SELECTION);
	    MapView.drawsRectangleForSelection = TreeXmlReader.xmlToBoolean(drawCircle);
	    final String printOnWhite = resourceController
	            .getProperty("printonwhitebackground");
	    MapView.printOnWhiteBackground = TreeXmlReader.xmlToBoolean(printOnWhite);
	    final int alpha = 255 - resourceController.getIntProperty(PRESENTATION_DIMMER_TRANSPARENCY, 0x70);
	    resourceController.setDefaultProperty(SPOTLIGHT_BACKGROUND_COLOR, ColorUtils.colorToRGBAString(new Color(0, 0, 0, alpha)));
	    spotlightBackgroundColor = resourceController.getColorProperty(SPOTLIGHT_BACKGROUND_COLOR);
	    hideSingleEndConnectorsPropertyValue = resourceController.getBooleanProperty(HIDE_SINGLE_END_CONNECTORS);
	    showConnectorsPropertyValue = resourceController.getProperty(SHOW_CONNECTORS_PROPERTY).intern();
	    outlineHGap = resourceController.getLengthProperty(OUTLINE_HGAP_PROPERTY);
	    draggingAreaWidth = resourceController.getLengthProperty(DRAGGING_AREA_WIDTH_PROPERTY);
	    outlineViewFitsWindowWidth = resourceController.getBooleanProperty(OUTLINE_VIEW_FITS_WINDOW_WIDTH);
	    showsTagsOnMinimizedNodes = resourceController.getBooleanProperty(SHOW_TAGS_ON_MINIMIZED_NODES_PROPERTY);

	    createPropertyChangeListener();
	}

	public MapView(final MapModel viewedMap, final ModeController modeController) {
		super();
		setOpaque(false);
		antiAliasingConfigurator = new AntiAliasingConfigurator(this);
		this.viewedMap = viewedMap;
		this.modeController = modeController;
        setLayout(new MindMapLayout());
		rootsHistory = new ArrayList<>();
		setAutoscrolls(true);
        final IUserInputListenerFactory userInputListenerFactory = getModeController().getUserInputListenerFactory();
        addMouseListener(userInputListenerFactory.getMapMouseListener());
        addMouseMotionListener(userInputListenerFactory.getMapMouseListener());
        addMouseWheelListener(userInputListenerFactory.getMapMouseWheelListener());
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, emptyNodeViewSet());
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, emptyNodeViewSet());
        setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, emptyNodeViewSet());
        viewportSizeChangeListener = new ComponentAdapter() {
            boolean firstRun = true;
            @Override
            public void componentResized(final ComponentEvent e) {
                if(firstRun) {
                    loadBackgroundImage();
                    firstRun = false;
                }
                if (fitToViewport) {
                    adjustBackgroundComponentScale();
                }
                if(usesLayoutSpecificMaxNodeWidth()) {
                    currentRootView.updateAll();
                    repaint();
                }
            }
        };
		connectorChangeListener = new INodeChangeListener() {
			@Override
			public void nodeChanged(final NodeChangeEvent event) {
				if(NodeLinks.CONNECTOR.equals(event.getProperty()) &&
						event.getNode().getMap().equals(getMap()))
					repaint();
			}
		};
		addPropertyChangeListener(SPOTLIGHT_ENABLED, repaintOnClientPropertyChangeListener);
		if(ResourceController.getResourceController().getBooleanProperty("activateSpotlightByDefault"))
		    putClientProperty(SPOTLIGHT_ENABLED, Boolean.TRUE);
		nodeViewFolder = new NodeViewFolder(true);
		setMap(viewedMap);
		mapScroller = new MapScroller(this);
	}

    private NodeModel getSearchRoot() {
        return currentSearchRoot;
    }

    public void setMap(final MapModel viewedMap) {
    	final MapModel lastViewedMap = this.viewedMap;
        if(lastViewedMap != null)
            lastViewedMap.removeMapChangeListener(this);
        Point rootLocationOnScreen = isShowing() ? getRoot().getMainView().getLocationOnScreen() : null;
        this.viewedMap = viewedMap;
        if(lastViewedMap != null && lastViewedMap != viewedMap)
        	modeController.getMapController().fireMapChanged(new MapChangeEvent(this, lastViewedMap, MapView.class, this, null, false));
        setName(viewedMap.getTitle());
        final NoteController noteController = NoteController.getController(getModeController());
        showNotes= noteController != null && noteController.showNotesInMap(getMap());
        updateContentStyle();
        initRoot();
        setBackground(requiredBackground());
        final MapStyleModel mapStyleModel = MapStyleModel.getExtension(viewedMap);
        zoom = mapStyleModel.getZoom();
        layoutType = mapStyleModel.getMapViewLayout();
        final MapStyle mapStyle = getModeController().getExtension(MapStyle.class);
        final String fitToViewportAsString = mapStyle.getPropertySetDefault(viewedMap, MapStyle.FIT_TO_VIEWPORT);
        fitToViewport = Boolean.parseBoolean(fitToViewportAsString);
        allowsCompactLayout = mapStyle.allowsCompactLayout(viewedMap);
        isAutoCompactLayoutEnabled = mapStyle.isAutoCompactLayoutEnabled(viewedMap);
        tagLocation = mapStyle.tagLocation(viewedMap);
        iconLocation = mapStyle.iconLocation(viewedMap);
        rootsHistory.clear();
        filter = Filter.createTransparentFilter();
        if(lastViewedMap != viewedMap)
        	modeController.getMapController().fireMapChanged(new MapChangeEvent(this, viewedMap, MapView.class, null, this, false));

        viewedMap.addMapChangeListener(this);

        if(rootLocationOnScreen != null) {
            revalidate();
            repaint();
            SwingUtilities.invokeLater(() -> {
                Point newRootLocationOnScreen = getRoot().getMainView().getLocationOnScreen();
                Rectangle visibleRect = getVisibleRect();
                visibleRect.translate(newRootLocationOnScreen.x - rootLocationOnScreen.x, newRootLocationOnScreen.y - rootLocationOnScreen.y);
                scrollRectToVisible(visibleRect);
                preserveRootNodeLocationOnScreen();
            });
        }
    }

    public void replaceSelection(final NodeView[] views) {
        selection.replace(views);
        if(views.length > 0)
        	views[0].requestFocusInWindow();
    }

    // generics trickery
	private Set<AWTKeyStroke> emptyNodeViewSet() {
	    return Collections.emptySet();
    }

	/*
	 * (non-Javadoc)
	 * @see java.awt.dnd.Autoscroll#autoscroll(java.awt.Point)
	 */
	@Override
	public void autoscroll(final Point cursorLocn) {
	    final JViewport viewPort = (JViewport) getParent();
	    Rectangle viewRectangle = viewPort.getViewRect();
	    if (!viewRectangle.contains(cursorLocn)) {
	        return;
	    }

	    int distanceToLeft = cursorLocn.x - viewRectangle.x;
	    int distanceToRight = viewRectangle.x + viewRectangle.width - cursorLocn.x;
	    int distanceToTop = cursorLocn.y - viewRectangle.y;
	    int distanceToBottom = viewRectangle.y + viewRectangle.height - cursorLocn.y;

	    int deltaX = 0;
	    int deltaY = 0;

	    int distanceToEdge;

	    // Find the closest horizontal edge and calculate the scroll amount
	    if (distanceToLeft < AUTOSCROLL_MARGIN && distanceToLeft < distanceToRight) {
	        distanceToEdge = AUTOSCROLL_MARGIN - distanceToLeft;
	        deltaX = -calculateAutoscrollAmount(distanceToEdge);
	    } else if (distanceToRight < AUTOSCROLL_MARGIN) {
	        distanceToEdge = AUTOSCROLL_MARGIN - distanceToRight;
	        deltaX = calculateAutoscrollAmount(distanceToEdge);
	    }

	    // Find the closest vertical edge and calculate the scroll amount
	    if (distanceToTop < AUTOSCROLL_MARGIN && distanceToTop < distanceToBottom) {
	        distanceToEdge = AUTOSCROLL_MARGIN - distanceToTop;
	        deltaY = -calculateAutoscrollAmount(distanceToEdge);
	    } else if (distanceToBottom < AUTOSCROLL_MARGIN) {
	        distanceToEdge = AUTOSCROLL_MARGIN - distanceToBottom;
	        deltaY = calculateAutoscrollAmount(distanceToEdge);
	    }

	    // Scroll the view port
	    Rectangle newViewRectangle = new Rectangle(
	            viewRectangle.x + deltaX,
	            viewRectangle.y + deltaY,
	            viewRectangle.width,
	            viewRectangle.height);
	    final Rectangle innerBounds = currentRootView.getBounds();
	    final int spaceAround = currentRootView.getSpaceAround();
	    innerBounds.x += -AUTOSCROLL_MARGIN/2 + spaceAround;
	    innerBounds.y += -AUTOSCROLL_MARGIN/2 + spaceAround;
	    innerBounds.width += AUTOSCROLL_MARGIN - spaceAround * 2;
	    innerBounds.height += AUTOSCROLL_MARGIN - spaceAround * 2;
	    scrollRectToVisible(newViewRectangle.intersection(innerBounds));
	}

	private int calculateAutoscrollAmount(int distanceToEdge) {
	    return distanceToEdge * distanceToEdge * 2 / AUTOSCROLL_MARGIN;
	}

	boolean isFrameLayoutCompleted() {
		final Frame frame = JOptionPane.getFrameForComponent(this);
		final Insets frameInsets = frame.getInsets();
		final Component rootPane = frame.getComponent(0);
		final boolean frameLayoutCompleted = rootPane.getWidth() == frame.getWidth() - frameInsets.left - frameInsets.right
				&& rootPane.getHeight() == frame.getHeight() - frameInsets.top - frameInsets.bottom;
		return frameLayoutCompleted;
	}


	@Override
    public void addNotify() {
	    super.addNotify();
	    modeController.getMapController().addUINodeChangeListener(connectorChangeListener);
	    getParent().addComponentListener(viewportSizeChangeListener);
    }

	@Override
    public void removeNotify() {
		modeController.getMapController().removeNodeChangeListener(connectorChangeListener);
		getParent().removeComponentListener(viewportSizeChangeListener);
	    super.removeNotify();
    }

	boolean isLayoutCompleted() {
	    final JViewport viewPort = (JViewport) getParent();
	    final Dimension visibleDimension = viewPort.getExtentSize();
	    return visibleDimension.width > 0;
	}

	static private void createPropertyChangeListener() {
		MapView.propertyChangeListener = new IFreeplanePropertyListener() {
			@Override
			public void propertyChanged(final String propertyName, final String newValue, final String oldValue) {
				if (propertyName.equals("printonwhitebackground")) {
					MapView.printOnWhiteBackground = TreeXmlReader.xmlToBoolean(newValue);
					return;
				}
				if (propertyName.equals(DRAGGING_AREA_WIDTH_PROPERTY)) {
					MapView.draggingAreaWidth = ResourceController.getResourceController().getLengthProperty(DRAGGING_AREA_WIDTH_PROPERTY);
					return;
				}
				{
					final Component c = Controller.getCurrentController().getMapViewManager().getMapViewComponent();
					if (!(c instanceof MapView)) {
						return;
					}
					final MapView mapView = (MapView) c;
					if (propertyName.equals(RESOURCES_SELECTED_NODE_COLOR)) {
						mapView.repaintSelecteds(true);
						return;
					}
					if (propertyName.equals(RESOURCES_SELECTED_NODE_RECTANGLE_COLOR)) {
						mapView.repaintSelecteds(true);
						return;
					}
					if (propertyName.equals(ResourceController.RESOURCE_DRAW_RECTANGLE_FOR_SELECTION)) {
						MapView.drawsRectangleForSelection = TreeXmlReader.xmlToBoolean(newValue);
						mapView.repaintSelecteds(true);
						return;
					}
					if (propertyName.equals(SPOTLIGHT_BACKGROUND_COLOR)) {
						MapView.spotlightBackgroundColor = ColorUtils.stringToColor(newValue);
						mapView.repaint();
						return;
					}
				}
				for(Component c : Controller.getCurrentController().getMapViewManager().getMapViews()) {
					if (!(c instanceof MapView)) {
						continue;
					}
					final MapView mapView = (MapView) c;
					if (propertyName.equals(SHOW_TAGS_ON_MINIMIZED_NODES_PROPERTY)) {
						MapView.showsTagsOnMinimizedNodes = TreeXmlReader.xmlToBoolean(newValue);
						mapView.updateIconsRecursively();
						mapView.repaint();
						continue;
					}
					if (propertyName.equals(BookmarksController.SHOW_BOOKMARK_ICONS)) {
						mapView.updateIconsRecursively();
						mapView.repaint();
						continue;
					}
					if (propertyName.equals(SHOW_CONNECTORS_PROPERTY)) {
						MapView.showConnectorsPropertyValue = ResourceController.getResourceController().getProperty(SHOW_CONNECTORS_PROPERTY).intern();
						mapView.repaint();
						continue;
					}
					if (propertyName.startsWith(SOME_CONNECTORS_PROPERTY)) {
						MapView.showConnectorsPropertyValue = ResourceController.getResourceController().getProperty(SHOW_CONNECTORS_PROPERTY).intern();
						mapView.repaint();
						continue;
					}
					if (propertyName.equals(OUTLINE_HGAP_PROPERTY)) {
						MapView.outlineHGap = ResourceController.getResourceController().getLengthProperty(OUTLINE_HGAP_PROPERTY);
						if (mapView.isOutlineLayoutSet()) {
							mapView.getRoot().updateAll();
							mapView.repaint();
						}
						continue;
					}
					if (propertyName.equals(HIDE_SINGLE_END_CONNECTORS)) {
						MapView.hideSingleEndConnectorsPropertyValue = ResourceController.getResourceController().getBooleanProperty(HIDE_SINGLE_END_CONNECTORS);
						mapView.repaint();
						continue;
					}
					if(propertyName.equals(OUTLINE_VIEW_FITS_WINDOW_WIDTH)) {
						outlineViewFitsWindowWidth = ResourceController.getResourceController().getBooleanProperty(OUTLINE_VIEW_FITS_WINDOW_WIDTH);
						if (mapView.isOutlineLayoutSet()) {
							mapView.getRoot().updateAll();
							mapView.repaint();
						}
						continue;
					}
					break;
				}
			}

		};
		ResourceController.getResourceController().addPropertyChangeListener(MapView.propertyChangeListener);
	}

	private void updateIconsRecursively() {
		updateIconsRecursively(currentRootView);
		if(mapRootView != currentRootView)
			updateIconsRecursively(mapRootView);
	}

	public void deselect(final NodeView newSelected) {
		if (selection.contains(newSelected) && selection.deselect(newSelected) && newSelected.getParent() != null) {
			repaintAfterSelectionChange(newSelected, true);
		}
	}

	void updateSelectedNode() {
	    selection.updateSelectedNode();
	}

	private void repaintAfterSelectionChange(final NodeView node, boolean update) {
		if(! node.isShowing())
			return;
		if(update)
		    node.update(UpdateCause.SELECTION);
		if(SHOW_CONNECTORS_FOR_SELECTION_ONLY == showConnectors || SHOW_ARROWS_FOR_SELECTION_ONLY == showConnectors || repaintsViewOnSelectionChange)
			repaint(getVisibleRect());
		else
			node.repaintSelected();
	}

    public Object detectView(final Point p) {
        if (arrowLinkViews == null) {
            return null;
        }
        for (int i = 0; i < arrowLinkViews.size(); ++i) {
            final ILinkView arrowView = arrowLinkViews.get(i);
            if (arrowView.detectCollision(p, true)) {
                return arrowView;
            }
        }
        for (int i = 0; i < arrowLinkViews.size(); ++i) {
            final ILinkView arrowView = arrowLinkViews.get(i);
            if (arrowView.detectCollision(p, false)) {
                return arrowView;
            }
        }
        return null;
    }

    public Object detectObject(final Point p) {
        Object view = detectView(p);
        if(view instanceof ILinkView)
            return ((ILinkView)view).getConnector();
        return null;
    }

	/**
	 * Call preparePrinting() before printing and endPrinting() after printing
	 * to minimize calculation efforts
	 */
	public void endPrinting() {
		if (!isPreparedForPrinting)
			return;
		paintingPurpose = PaintingPurpose.PAINTING;
		updatePrintedNodes();
		isPreparedForPrinting = false;
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.dnd.Autoscroll#getAutoscrollInsets()
	 */
	@Override
	public Insets getAutoscrollInsets() {
		final Container parent = getParent();
		if (parent == null) {
			return new Insets(0, 0, 0, 0);
		}
		final Rectangle outer = getBounds();
		final Rectangle inner = parent.getBounds();
		return new Insets(inner.y - outer.y + MapView.AUTOSCROLL_MARGIN, inner.x - outer.x + MapView.AUTOSCROLL_MARGIN, outer.height
		        - inner.height - inner.y + outer.y + MapView.AUTOSCROLL_MARGIN, outer.width - inner.width - inner.x + outer.x
		        + MapView.AUTOSCROLL_MARGIN);
	}

	public Rectangle getInnerBounds() {
		final Rectangle innerBounds = currentRootView.getBounds();
		final Rectangle maxBounds = new Rectangle(0, 0, getWidth(), getHeight());
		if(arrowLinkViews != null)
			for (int i = 0; i < arrowLinkViews.size(); ++i) {
				final ILinkView arrowView = arrowLinkViews.get(i);
				arrowView.increaseBounds(innerBounds);
			}
		return innerBounds.intersection(maxBounds);
	}

	public IMapSelection getMapSelection() {
		return new MapSelection();
	}

	public ModeController getModeController() {
		return modeController;
	}

	public MapModel getMap() {
		return viewedMap;
	}

	public Point getNodeContentLocation(final NodeView nodeView) {
		final Point contentXY = new Point(0, 0);
		UITools.convertPointToAncestor(nodeView.getContent(), contentXY, this);
		return contentXY;
	}

	private NodeView getNodeView(final Object o) {
        if(! (o instanceof NodeModel))
			return null;
		final NodeView nodeView = getNodeView((NodeModel)o);
		return nodeView;
    }

	private NodeView getDisplayedNodeView(NodeModel node) {
		NodeView nodeView = getNodeView(node);
		return currentRootView == mapRootView
				||  nodeView != null && isAncestorOf(nodeView) ? nodeView : null;
	}

	public NodeView getNodeView(final NodeModel node) {
		if (node == null) {
			return null;
		}
		for (final INodeView iNodeView : node.getViewers()) {
			if(! (iNodeView instanceof NodeView)){
				continue;
			}
			final NodeView candidateView = (NodeView) iNodeView;
			if (candidateView.getMap() == this) {
				return candidateView;
			}
		}
		final NodeView root = getRoot();
		if(root.getNode().equals(node))
			return root;
		else
			return null;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.JComponent#getPreferredSize()
	 */
	@Override
	public Dimension getPreferredSize() {
		return getLayout().preferredLayoutSize(this);
	}

	public NodeView getRoot() {
		return currentRootView;
	}

	public NodeView getSelected() {
		if(! selectedsValid) {
			final NodeView node = selection.selectedNode;
	        if (node == null || ! SwingUtilities.isDescendingFrom(node, this))
		        validateSelecteds();
            else {
	            final JComponent content = node.getContent();
	            if (content == null || ! content.isVisible())
	                validateSelecteds();
            }
        }
		return selection.selectedNode;
	}

	public NodeView getSelectionEnd() {
	    getSelected();
	    return selection.getSelectionEnd();
	}

	public Set<NodeModel> getSelectedNodes() {
		validateSelecteds();
		return new AbstractSet<NodeModel>() {

			@Override
			public int size() {
				return selection.size();
			}

			@Override
            public boolean contains(final Object o) {
                final NodeView nodeView = getNodeView(o);
                if(nodeView == null)
                	return false;
                return selection.contains(nodeView);
            }

			@Override
            public boolean add(final NodeModel o) {
				final NodeView nodeView = getNodeView(o);
				if(nodeView == null)
					return false;
				return selection.add(nodeView);
            }

			@Override
            public boolean remove(final Object o) {
				final NodeView nodeView = getNodeView(o);
				if(nodeView == null)
					return false;
				if (selection.deselect(nodeView)) {
                    updateSelectedNode();
                    return true;
                }
                return false;
            }

			@Override
            public Iterator<NodeModel> iterator() {
				return new Iterator<NodeModel>() {
					final Iterator<NodeView> i = selection.getSelectedSet().iterator();

					@Override
					public boolean hasNext() {
	                    return i.hasNext();
                    }

					@Override
					public NodeModel next() {
	                    return i.next().getNode();
                    }

					@Override
					public void remove() {
	                    i.remove();
                    }

				};
            }
		};
	}

	public List<NodeModel> getOrderedSelectedNodes() {
		validateSelecteds();
		return new AbstractList<NodeModel>(){

			@Override
            public boolean add(final NodeModel o) {
				final NodeView nodeView = getNodeView(o);
				if(nodeView == null)
					return false;
				return selection.add(nodeView);
            }



			@Override
            public boolean contains(final Object o) {
                final NodeView nodeView = getNodeView(o);
                if(nodeView == null)
                	return false;
                return selection.contains(nodeView);
            }



			@Override
            public boolean remove(final Object o) {
				final NodeView nodeView = getNodeView(o);
				if(nodeView == null)
					return false;
				if (selection.deselect(nodeView)) {
				    updateSelectedNode();
                    return true;
                }
				return false;
            }

			@Override
            public NodeModel get(final int index) {
	            return selection.getSelectedList().get(index).getNode();
            }

			@Override
            public int size() {
	            return selection.size();
            }
		};
    }

	/**
	 * @param differentSubtrees
	 * @return an ArrayList of MindMapNode objects. If both ancestor and
	 *         descendant node are selected, only the ancestor ist returned
	 */
	ArrayList<NodeModel> getSelectedNodesSortedByY(final boolean differentSubtrees) {
		validateSelecteds();
		final TreeSet<NodeModel> sortedNodes = new TreeSet<NodeModel>(NodeRelativePath.comparator());
		for (final NodeView view : selection.getSelectedSet()) {
			if (! ( differentSubtrees  && viewBelongsToSelectedSubtreeOrItsClone(view))) {
				sortedNodes.add(view.getNode());
			}
		}
		if(differentSubtrees){
			return NodeSubtrees.getUniqueSubtreeRoots(sortedNodes);
		}
		else
			return new ArrayList<NodeModel>(sortedNodes);
	}

	private boolean viewBelongsToSelectedSubtreeOrItsClone(final NodeView view) {
		final HashSet<NodeModel> selectedNodesWithClones = new HashSet<NodeModel>();
		for (final NodeView selectedView : selection.getSelectedList())
			for(final NodeModel clone : selectedView.getNode().subtreeClones())
				selectedNodesWithClones.add(clone);

	    for (Component parent = view.getParent(); parent instanceof NodeView; parent = parent.getParent()) {
	    	if (selectedNodesWithClones.contains(((NodeView)parent).getNode())) {
	    		return true;
	    	}
	    }
	    return false;
    }

	/**
	 * @return
	 */
	public Collection<NodeView> getSelection() {
		validateSelecteds();
		return selection.getSelection();
	}

    boolean synchronizesSelectionAcrossVisibleViews() {
        return ResourceController.getResourceController().getBooleanProperty(SYNCHRONIZE_SELECTION_ACROSS_VISIBLE_VIEWS_PROPERTY);
    }

    boolean synchronizesSelectionOnlyOnBranchChange() {
        return ResourceController.getResourceController().getBooleanProperty(SYNCHRONIZE_SELECTION_ONLY_ON_BRANCH_CHANGE);
    }



	public int getSiblingMaxLevel() {
		return siblingMaxLevel;
	}

	/**
	 * Returns the size of the visible part of the view in view coordinates.
	 */
	public Dimension getViewportSize() {
		final JViewport mapViewport = (JViewport) getParent();
		return mapViewport == null ? null : mapViewport.getSize();
	}

	private boolean unfoldsOnNavigation() {
		return ResourceController.getResourceController().getBooleanProperty(UNFOLD_ON_NAVIGATION);
	}

	boolean isOutlineLayoutSet() {
	    return layoutType.equals(MapViewLayout.OUTLINE);
	}

	int getIndex(final NodeView node) {
	    final NodeView parent = node.getParentView();
	    for(int i = 0; i < parent.getComponentCount(); i++){
	    	if(parent.getComponent(i).equals(node))
	    		return i;
	    }
	    return -1;
    }

	public float getZoom() {
		return zoom;
	}

	public int getZoomed(final int number) {
		return (int) Math.ceil(number * zoom);
	}

	public int getZoomed(final double number) {
		return (int) Math.ceil(number * zoom);
	}


	private void initRoot() {
	    if(currentRootView != null)
	        remove(currentRootView);
	    currentRootParentView = null;
		mapRootView = currentRootView = NodeViewFactory.getInstance().newNodeView(getMap().getRootNode(), this, this, ROOT_NODE_COMPONENT_INDEX);
		selection.clear();
	}



	@Override
    public Color getBackground() {
	    return super.getBackground();
    }

    public boolean isPrinting() {
		return paintingPurpose != PaintingPurpose.PAINTING;
	}

	public boolean isSelected(final NodeView n) {
		if(isPrinting() || (! selectedsValid &&
				(selection.selectedNode == null || ! SwingUtilities.isDescendingFrom(selection.selectedNode, this)  || ! selection.selectedNode.getContent().isVisible())))
			return false;
		return selection.contains(n);
	}

	/**
	 * Add the node to the selection if it is not yet there, making it the
	 * focused selected node.
	 */
	void addSelected(final NodeView newSelected, final boolean scroll) {
		if(newSelected.isContentVisible()){
			selection.add(newSelected);
			if(scroll)
				mapScroller.scrollNodeToVisible(newSelected);
		}
	}

	@Override
	public void mapChanged(final MapChangeEvent event) {
		final Object property = event.getProperty();
		if (property.equals(MapStyle.RESOURCES_BACKGROUND_COLOR)) {
			setBackground(requiredBackground());
			return;
		}
		if (property.equals(MapStyle.MAP_STYLES)){
	        updateContentStyle();
	        getRoot().resetLayoutPropertiesRecursively();
	        revalidate();
	        repaint();
		}
        if (property.equals(Filter.class)){
            setSiblingMaxLevel(getSelected());
        }
		if (property.equals(MapStyle.MAP_STYLES) && event.getMap().equals(viewedMap)
		        || property.equals(ModelessAttributeController.ATTRIBUTE_VIEW_TYPE)
		        || property.equals(Filter.class)
		        || property.equals(UrlManager.MAP_URL)) {
			setBackground(requiredBackground());
			updateAllNodeViews();
			return;
		}
        if(property instanceof Tag || property.equals(TagCategories.class)) {
            if(TagLocation.BESIDE_NODES == getTagLocation())
                updateIconsRecursively(getRoot());
            else
                updateAllNodeViews();
            return;
        }
		if(property.equals(AttributeController.SHOW_ICON_FOR_ATTRIBUTES)
				||property.equals(NoteController.SHOW_NOTE_ICONS)
				|| property.equals(MapStyle.SHOW_TAGS_PROPERTY))
			updateIconsRecursively(getRoot());
		if(property.equals(NoteController.SHOW_NOTES_IN_MAP))
			setShowNotes();
		if (property.equals(MapStyle.RESOURCES_BACKGROUND_IMAGE)) {
			final String fitToViewportAsString = MapStyle.getController(modeController).getPropertySetDefault(viewedMap,
			    MapStyle.FIT_TO_VIEWPORT);
			setFitToViewport(Boolean.parseBoolean(fitToViewportAsString));
			loadBackgroundImage();
		}
        if (property.equals(MapStyle.ALLOW_COMPACT_LAYOUT_PROPERTY)) {
            final MapStyle mapStyle = getModeController().getExtension(MapStyle.class);
            allowsCompactLayout = mapStyle.allowsCompactLayout(viewedMap);
            getRoot().resetLayoutPropertiesRecursively();
            revalidate();
            repaint();
        }
        if (property.equals(MapStyle.AUTO_COMPACT_LAYOUT_PROPERTY)) {
            final MapStyle mapStyle = getModeController().getExtension(MapStyle.class);
            isAutoCompactLayoutEnabled = mapStyle.isAutoCompactLayoutEnabled(viewedMap);
            getRoot().resetLayoutPropertiesRecursively();
            revalidate();
            repaint();
        }
        if (property.equals(MapStyle.SHOW_TAGS_PROPERTY) || property.equals(MapStyle.SHOW_TAG_CATEGORIES_PROPERTY)) {
            final MapStyle mapStyle = getModeController().getExtension(MapStyle.class);
            tagLocation = mapStyle.tagLocation(viewedMap);
            updateAllNodeViews();
            repaint();
        }
        if (property.equals(MapStyle.SHOW_ICONS_PROPERTY)) {
            final MapStyle mapStyle = getModeController().getExtension(MapStyle.class);
            iconLocation = mapStyle.iconLocation(viewedMap);
            updateIconsRecursively(getRoot());
            repaint();
        }
		if (property.equals(MapStyle.FIT_TO_VIEWPORT)) {
			final String fitToViewportAsString = MapStyle.getController(modeController).getPropertySetDefault(viewedMap,
			    MapStyle.FIT_TO_VIEWPORT);
			setFitToViewport(Boolean.parseBoolean(fitToViewportAsString));
			adjustBackgroundComponentScale();
		}
		if(property.equals(EdgeColorsConfigurationFactory.EDGE_COLOR_CONFIGURATION_PROPERTY)){
			updateAllNodeViews();
			repaint();
		}
	}

	private void setFitToViewport(boolean fitToViewport) {
	    this.fitToViewport = fitToViewport;
	    updateBackground();
    }

    private void loadBackgroundImage() {
 		final MapStyle mapStyle = getModeController().getExtension(MapStyle.class);
		backgroundComponent = null;
		updateBackground();
		final URI uri = mapStyle.getBackgroundImage(viewedMap);
		if (uri != null) {
			final ViewerController vc = getModeController().getExtension(ViewerController.class);
			if(vc != null) {
				final IViewerFactory factory = vc.getViewerFactory();
				assignViewerToBackgroundComponent(factory, uri);
			}
		}
		repaint();
    }

    private void assignViewerToBackgroundComponent(final IViewerFactory factory, final URI uri) {
    	try {
			if (fitToViewport) {
			    final JViewport vp = (JViewport) getParent();
			    final Dimension viewPortSize = vp.getVisibleRect().getSize();
			    ScalableComponent viewer = factory.createViewer(uri, viewPortSize, () -> getParent().repaint());
			    setBackgroundComponent(viewer);

			}
            else {
                ScalableComponent viewer = factory.createViewer(uri, zoom, () -> getParent().repaint());
                setBackgroundComponent(viewer);
            }
			if(backgroundComponent == null) {
				LogUtils.warn("no viewer created for " + uri);
				return;
			}
		}
		catch (final FileNotFoundException e1) {
			LogUtils.warn(e1);
		}
		catch (final Exception e1) {
			LogUtils.severe(e1);
		}
	}

    private void setBackgroundComponent(ScalableComponent viewer) {
        this.backgroundComponent = viewer;
        updateBackground();
    }

   private void updateBackground() {
        MapViewPort viewport = (MapViewPort) getParent();
        Color background = getBackground();
        if(viewport != null) {
            if(fitToViewport) {
                viewport.setBackground(background);
                viewport.setBackgroundComponent(backgroundComponent);

            } else {
                viewport.setBackgroundComponent(null);
            }

        }
        setOpaque(! (fitToViewport && backgroundComponent != null) && background.getAlpha() == 255);
    }



   @Override
   public void setBackground(Color background) {
       super.setBackground(background);
       updateBackground();
   }

    private void updateIconsRecursively(final NodeView node) {
    	final MainView mainView = node.getMainView();
    	if(mainView == null)
    		return;
		mainView.updateIcons(node);
    	for(int i = 0; i < node.getComponentCount(); i++){
    		final Component component = node.getComponent(i);
    		if(component instanceof NodeView)
    		updateIconsRecursively((NodeView) component);
    	}
    }

    void synchronizeSelection() {
        if(isSelected())
            return;
        IMapSelection primarySelection = modeController.getController().getSelection();
        if( primarySelection == null || primarySelection.getMap() != viewedMap)
            return;

        if (getSelectedNodes().size() > 1)
            return;
        NodeModel primarySelectedNode = primarySelection.getSelected();
        NodeView mySelectedNodeView = getSelected();
        NodeModel mySelectedNode = mySelectedNodeView.getNode();
        if(mySelectedNode.equals(primarySelectedNode) || synchronizesSelectionOnlyOnBranchChange() && mySelectedNode.isDescendantOf(primarySelectedNode))
            return;
        NodeModel myViewRootNode = getRoot().getNode();
        if(myViewRootNode != primarySelection.getSelectionRoot())
            synchronizeRoot();
        if(myViewRootNode != primarySelectedNode && ! primarySelectedNode.isDescendantOf(myViewRootNode))
            return;
        for(NodeModel nodeOrAncestor = primarySelectedNode; nodeOrAncestor != null; nodeOrAncestor = nodeOrAncestor.getParentNode()) {
            NodeView anotherNodeView = getNodeView(nodeOrAncestor);
            if(anotherNodeView == null)
                continue;
            if(anotherNodeView.isContentVisible()) {
                selectAsTheOnlyOneSelected(anotherNodeView, false);
                break;
            }
        }
    }

    void synchronizeRoot() {
        if(isSelected())
            return;
        IMapSelection primarySelection = modeController.getController().getSelection();
        if( primarySelection == null || primarySelection.getMap() != viewedMap)
            return;

        NodeModel primaryRoot = primarySelection.getSelectionRoot();
        NodeView myRootNodeView = getRoot();
        NodeModel mySelectedRootNode = myRootNodeView.getNode();
        if(! mySelectedRootNode.equals(primaryRoot)) {
			setRootNode(primaryRoot);
			synchronizeSelection();
		}
    }

    private void updateContentStyle() {
        final NodeStyleController style = Controller.getCurrentModeController().getExtension(NodeStyleController.class);
        final MapModel map = getMap();
        final MapStyleModel model = MapStyleModel.getExtension(map);
        final NodeModel detailStyleNode = model.getStyleNodeSafe(MapStyleModel.DETAILS_STYLE);
        detailFont = UITools.scale(style.getFont(detailStyleNode, StyleOption.FOR_UNSELECTED_NODE));
        detailBackground = style.getBackgroundColor(detailStyleNode, StyleOption.FOR_UNSELECTED_NODE);
        detailForeground = style.getColor(detailStyleNode, StyleOption.FOR_UNSELECTED_NODE);
        detailHorizontalAlignment = style.getHorizontalTextAlignment(detailStyleNode, StyleOption.FOR_UNSELECTED_NODE).swingConstant;
        detailCss = style.getStyleSheet(detailStyleNode, StyleOption.FOR_UNSELECTED_NODE);

        final NodeModel noteStyleNode = model.getStyleNodeSafe(MapStyleModel.NOTE_STYLE);
        noteFont = UITools.scale(style.getFont(noteStyleNode, StyleOption.FOR_UNSELECTED_NODE));
        noteBackground = style.getBackgroundColor(noteStyleNode, StyleOption.FOR_UNSELECTED_NODE);
        noteForeground = style.getColor(noteStyleNode, StyleOption.FOR_UNSELECTED_NODE);
        noteHorizontalAlignment = style.getHorizontalTextAlignment(noteStyleNode, StyleOption.FOR_UNSELECTED_NODE).swingConstant;
        noteCss = style.getStyleSheet(noteStyleNode, StyleOption.FOR_UNSELECTED_NODE);
        updateSelectionColors();
	}

	public boolean selectLeft(final boolean continious) {
	    return selectRelatedNode(SelectionDirection.LEFT, continious);
	}

	private void select(final NodeView newSelected, final boolean continious) {
	    if(continious) {
            if(newSelected.isSelected()) {
                if(selection.getSelectionBeforeEnd() == newSelected) {
                    deselect(selection.getSelectionEnd());
                    setSiblingMaxLevel(selection.getSelectionEnd());
                    mapScroller.scrollNodeToVisible(newSelected);
                }
            }
            else {
                addSelected(newSelected, true);
                mapScroller.scrollNodeToVisible(newSelected);
                setSiblingMaxLevel(newSelected);
            }
        } else {
            selectAsTheOnlyOneSelected(newSelected);
            modeController.getMapController().scrollNodeTreeAfterSelect(newSelected.getNode());
        }
	}

	public boolean selectRight(final boolean continious) {
	    return selectRelatedNode(SelectionDirection.RIGHT, continious);
	}

	private boolean selectRelatedNode(SelectionDirection direction, final boolean continious) {
        if(selection.getSelectionEnd() == null)
            return false;
	    return  selectSingleNode(direction, continious)
	                || selectPreferredVisibleChild(direction, continious)
	                || selectSiblingOnTheOtherSide(direction, continious)
	                || selectPreferredVisibleSiblingOrAncestor(direction, continious)
	                || selectPreferredVisibleAncestorSibling(direction, continious)
	                || unfoldInDirection(direction)
	                || scroll(direction);

	}

    private boolean selectSingleNode(SelectionDirection direction, boolean continious) {
        if(continious)
            return false;
        NodeView selectionStart = selection.getSelectionStart();
        NodeView selectionEnd = selection.getSelectionEnd();

        if(selectionStart == selectionEnd)
            return false;
        Point startLocation = currentRootView.getRelativeLocation(selectionStart, 0.5, 0.5);
        Point endLocation = currentRootView.getRelativeLocation(selectionEnd, 0.5, 0.5);
        NodeView newSelected;
        switch(direction) {
        case DOWN:
            newSelected = startLocation.y > endLocation.y ? selectionStart : selectionEnd;
            break;
        case UP:
            newSelected = startLocation.y < endLocation.y ? selectionStart : selectionEnd;
            break;
        case LEFT:
            newSelected = startLocation.x < endLocation.x ? selectionStart : selectionEnd;
            break;
        case RIGHT:
            newSelected = startLocation.x > endLocation.x ? selectionStart : selectionEnd;
            break;
        default:
            return false;
        }
        select(newSelected, false);
        return true;
    }

    public boolean scroll(SelectionDirection direction) {
        switch(direction) {
        case DOWN:
            scrollBy(0, SCROLL_VELOCITY_PX);
            break;
        case UP:
            scrollBy(0, -SCROLL_VELOCITY_PX);
            break;
        case LEFT:
            scrollBy(-SCROLL_VELOCITY_PX, 0);
            break;
        case RIGHT:
            scrollBy(SCROLL_VELOCITY_PX, 0);
            break;
        default:
            return false;
        }
        return true;
    }

    private boolean selectPreferredVisibleChild(SelectionDirection direction,
            final boolean continious) {
        boolean isOutlineLayoutSet = isOutlineLayoutSet();
        if(isOutlineLayoutSet && direction != SelectionDirection.DOWN)
            return false;
        final NodeView oldSelected = selection.getSelectionEnd();
        NodeView newSelected = null;
        boolean selectedUsesHorizontalLayout = oldSelected.usesHorizontalLayout();
        ChildNodesAlignment childNodesAlignment = oldSelected.getChildNodesAlignment();
        if(isOutlineLayoutSet
         || selectedUsesHorizontalLayout && (!childNodesAlignment.isStacked() && (direction == SelectionDirection.UP || direction == SelectionDirection.DOWN)
                 || (childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT && direction == SelectionDirection.LEFT
                 || childNodesAlignment == ChildNodesAlignment.AFTER_PARENT && direction == SelectionDirection.RIGHT))
         || (! selectedUsesHorizontalLayout) && (!childNodesAlignment.isStacked() && (direction == SelectionDirection.LEFT || direction == SelectionDirection.RIGHT)
         || (childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT && direction == SelectionDirection.UP
            || childNodesAlignment == ChildNodesAlignment.AFTER_PARENT && direction == SelectionDirection.DOWN))){
            boolean looksAtTopOrLeft = direction == SelectionDirection.LEFT || direction == SelectionDirection.UP;
            PreferredChild preferredChild = isOutlineLayoutSet || ! selectedUsesHorizontalLayout && childNodesAlignment == ChildNodesAlignment.AFTER_PARENT || continious
                    ? PreferredChild.FIRST
                    : ! selectedUsesHorizontalLayout && childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT
                    ? PreferredChild.LAST
                    : PreferredChild.LAST_SELECTED;
            if(isOutlineLayoutSet || (direction == SelectionDirection.LEFT || direction == SelectionDirection.RIGHT) == selectedUsesHorizontalLayout) {
                newSelected = oldSelected.getPreferredVisibleChild(preferredChild, ChildrenSides.BOTH_SIDES);
            } else {
                newSelected = oldSelected.getPreferredVisibleChild(preferredChild, looksAtTopOrLeft);
            }
        }
        if(newSelected != null) {
            select(newSelected, continious);
            return true;
        }
        return false;
    }

     private boolean selectSiblingOnTheOtherSide(SelectionDirection direction, boolean continious) {
        if(isOutlineLayoutSet())
            return false;

        final NodeView oldSelected = selection.getSelectionEnd();
        boolean isTopOrLeft = oldSelected.isTopOrLeft();
        NodeView ancestorView = oldSelected.getParentView();
        for(;;) {
            if(ancestorView == null )
                return false;
            if((ancestorView.getChildNodesAlignment().isStacked() || ! ancestorView.isContentVisible())
                    && ancestorView.childrenSides() == ChildrenSides.BOTH_SIDES)
                break;
            if (ancestorView.isContentVisible())
                return false;
            isTopOrLeft =  ancestorView.isTopOrLeft();
            ancestorView = ancestorView.getParentView();
        }
        NodeView newSelected = null;
        if(ancestorView.layoutOrientation() == LayoutOrientation.TOP_TO_BOTTOM && (isTopOrLeft && direction == SelectionDirection.RIGHT || ! isTopOrLeft && direction == SelectionDirection.LEFT)
                || ancestorView.layoutOrientation() == LayoutOrientation.LEFT_TO_RIGHT && (isTopOrLeft && direction == SelectionDirection.DOWN || ! isTopOrLeft && direction == SelectionDirection.UP)){
            newSelected = ancestorView.selectNearest(PreferredChild.NEAREST_SIBLING, ChildrenSides.ofTopOrLeft(! isTopOrLeft), oldSelected);
        }
        if(newSelected != null) {
            selectPreservingSiblingMaxLevel(newSelected, continious);
            return true;
        }
        return false;
    }

    private boolean selectPreferredVisibleSiblingOrAncestor(SelectionDirection direction,
            final boolean continious) {
        boolean isOutlineLayoutSet = isOutlineLayoutSet();
        if(isOutlineLayoutSet && direction == SelectionDirection.RIGHT)
            return false;

        final NodeView oldSelected = selection.getSelectionEnd();
        if (! oldSelected.isRoot()) {
            final NodeView newSelectedAncestor = suggestNewSelectedAncestor(direction, oldSelected);
            final NodeView newSelectedSibling = suggestNewSelectedSibling(direction, oldSelected);
            final NodeView newSelectedSummary = suggestNewSelectedSummary(direction, oldSelected);

            NodeView newSelected = newSelectedSibling;

            if (newSelectedSummary != null && (newSelectedSibling == null
                    || newSelectedSummary.getNode().isDescendantOf(newSelectedSibling.getAncestorWithVisibleContent().getNode()))) {
                newSelected = newSelectedSummary;
            }

            if (newSelectedAncestor != null && (newSelected == null
                    || oldSelected == newSelected
                    || !newSelected.getNode().isDescendantOf(newSelectedAncestor.getNode()))) {
                newSelected = newSelectedAncestor;
            }

            if(newSelected != null && newSelected != oldSelected) {
                NodeView parentView = oldSelected.getParentView();
                if(newSelected.getParent() == parentView && parentView.layoutOrientation() == LayoutOrientation.TOP_TO_BOTTOM) {
                    ChildNodesAlignment childNodesAlignment = parentView.getChildNodesAlignment();
                    if (childNodesAlignment == ChildNodesAlignment.AFTER_PARENT && direction == SelectionDirection.UP) {
                        selectDescendant(newSelected, continious, PreferredChild.LAST);
                        return true;
                    }
                    if (childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT && direction == SelectionDirection.DOWN) {
                        selectDescendant(newSelected, continious, PreferredChild.FIRST);
                        return true;
                    }
                }
                if(newSelected == newSelectedAncestor)
                    select(newSelected, continious);
                else
                    selectPreservingSiblingMaxLevel(newSelected, continious);
                return true;
            }

        }
        return false;
    }

    private boolean selectPreferredVisibleAncestorSibling(SelectionDirection direction,
            final boolean continious) {
        boolean isOutlineLayoutSet = isOutlineLayoutSet();
        if(isOutlineLayoutSet)
            return false;

        int oldSiblingMaxLevel = siblingMaxLevel;
        siblingMaxLevel = -1;
        for (NodeView oldSelectedParent = selection.getSelectionEnd().getParentNodeView();
        		oldSelectedParent != null && ! oldSelectedParent.isRoot();
        		oldSelectedParent = oldSelectedParent.getParentNodeView())
        {
        	final NodeView newSelected = suggestNewSelectedSibling(direction, oldSelectedParent);
        	if(newSelected != null) {
        		siblingMaxLevel = oldSiblingMaxLevel;
        		select(newSelected, continious);
        		return true;
        	}
        }
        siblingMaxLevel = oldSiblingMaxLevel;
        return false;
    }


    private void selectPreservingSiblingMaxLevel(NodeView newSelected, final boolean continious) {
        int oldSiblingMaxLevel = this.siblingMaxLevel;
        this.siblingMaxLevel = -1;
        select(newSelected, continious);
        this.siblingMaxLevel = oldSiblingMaxLevel;
    }

    private NodeView suggestNewSelectedSibling(SelectionDirection direction,
    		final NodeView oldSelected) {
    	SiblingSelection siblingSelection = ResourceController.getResourceController().getEnumProperty("siblingSelection", SiblingSelection.CHANGE_PARENT);
    	LayoutOrientation orientation;
    	boolean down;
    	switch (direction) {
		case DOWN:
			orientation = LayoutOrientation.TOP_TO_BOTTOM;
			down = true;
			break;
		case UP:
			orientation = LayoutOrientation.TOP_TO_BOTTOM;
			down = false;
			break;
		case RIGHT:
			orientation = LayoutOrientation.LEFT_TO_RIGHT;
			down = true;
			break;
		case LEFT:
			orientation = LayoutOrientation.LEFT_TO_RIGHT;
			down = false;
			break;
		default:
		throw new IllegalArgumentException("Unknown direction");
		}

    	return getNextVisibleSibling(oldSelected, orientation, down, siblingSelection);
    }

    private NodeView getNextVisibleSibling(final NodeView oldSelected,
    		LayoutOrientation orientation, boolean down, SiblingSelection siblingSelection) {
    	NodeView nextSelectedSibling = oldSelected;
    	do {
    		NodeView nextVisibleSiblingOrSame = getNextVisibleSiblingAtAnyLevel(nextSelectedSibling, orientation, down, siblingSelection);
    		if(nextVisibleSiblingOrSame == nextSelectedSibling)
    			return oldSelected;
    		nextSelectedSibling = nextVisibleSiblingOrSame;
    	} while(nextSelectedSibling != null && nextSelectedSibling.getNode().getNodeLevel(filter) < siblingMaxLevel);
    	return nextSelectedSibling;
    }

    private NodeView suggestNewSelectedAncestor(SelectionDirection direction,
    		final NodeView oldSelected) {
    	NodeView newSelectedParent = null;
    	NodeView parentView = oldSelected.getParentView();
    	if(parentView == null)
    		return null;
    	ChildNodesAlignment childNodesAlignment = parentView.getChildNodesAlignment();
    	LayoutOrientation layoutOrientation = parentView.layoutOrientation();
    	if (direction == SelectionDirection.DOWN) {
    		newSelectedParent = oldSelected.getVisibleSummarizedOrParentView(
    				layoutOrientation == LayoutOrientation.TOP_TO_BOTTOM && childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT
    				? LayoutOrientation.TOP_TO_BOTTOM
    						:  LayoutOrientation.LEFT_TO_RIGHT,
    						layoutOrientation == LayoutOrientation.TOP_TO_BOTTOM && childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT
    						? oldSelected.isTopOrLeft() : true);
    	} else if (direction == SelectionDirection.UP) {
    		newSelectedParent = oldSelected.getVisibleSummarizedOrParentView(
    				layoutOrientation == LayoutOrientation.TOP_TO_BOTTOM && childNodesAlignment == ChildNodesAlignment.AFTER_PARENT
    				? LayoutOrientation.TOP_TO_BOTTOM
    						: LayoutOrientation.LEFT_TO_RIGHT,
    						layoutOrientation == LayoutOrientation.TOP_TO_BOTTOM && childNodesAlignment == ChildNodesAlignment.AFTER_PARENT
    						? oldSelected.isTopOrLeft() :false);
    	} else if (direction == SelectionDirection.RIGHT) {
    		newSelectedParent = oldSelected.getVisibleSummarizedOrParentView(
    				layoutOrientation == LayoutOrientation.LEFT_TO_RIGHT && childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT
    				? LayoutOrientation.LEFT_TO_RIGHT
    						:  LayoutOrientation.TOP_TO_BOTTOM,
    						layoutOrientation == LayoutOrientation.LEFT_TO_RIGHT && childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT
    						? oldSelected.isTopOrLeft() : true);
    	} else if (direction == SelectionDirection.LEFT) {
    		newSelectedParent = oldSelected.getVisibleSummarizedOrParentView(
    				layoutOrientation == LayoutOrientation.LEFT_TO_RIGHT && childNodesAlignment == ChildNodesAlignment.AFTER_PARENT
    				? LayoutOrientation.LEFT_TO_RIGHT
    						:  LayoutOrientation.TOP_TO_BOTTOM,
    						layoutOrientation == LayoutOrientation.LEFT_TO_RIGHT && childNodesAlignment == ChildNodesAlignment.AFTER_PARENT
    						? oldSelected.isTopOrLeft() : false);
    	}
    	return newSelectedParent;
    }

    private boolean canHaveSummary(final NodeView node, SelectionDirection direction) {
        return node.usesHorizontalLayout()
                && (direction == SelectionDirection.UP && node.isTopOrLeft() || direction == SelectionDirection.DOWN  && !node.isTopOrLeft())
                || (! node.usesHorizontalLayout())
                && (direction == SelectionDirection.LEFT && node.isTopOrLeft() || direction == SelectionDirection.RIGHT && !node.isTopOrLeft());
    }

    private NodeView suggestNewSelectedSummary(SelectionDirection direction, final NodeView node) {
        if(node == null || isOutlineLayoutSet() || isRoot(node))
            return null;
        final int currentSummaryLevel = SummaryNode.getSummaryLevel(currentRootView.getNode(), node.getNode());
        int level = currentSummaryLevel;
        final int requiredSummaryLevel = level + 1;
        final NodeView parent = node.getParentView();
        if(canHaveSummary(node, direction)) {
            for (int i = 1 + getIndex(node);i < parent.getComponentCount();i++){
                final Component component = parent.getComponent(i);
                if(! (component instanceof NodeView))
                    break;
                final NodeView next = (NodeView) component;
                if(next.isTopOrLeft() != node.isTopOrLeft())
                    continue;
                if(next.isSummary())
                    level++;
                else
                    level = 0;
                if(level == requiredSummaryLevel){
                    if(next.getNode().hasVisibleContent(filter))
                        return next;
                    final NodeView preferredVisibleChild = next.getPreferredVisibleChild(isOutlineLayoutSet() ? PreferredChild.FIRST : PreferredChild.LAST_SELECTED, next.isTopOrLeft());
                    if(preferredVisibleChild != null)
                        return preferredVisibleChild;
                    break;
                }
                if(level == currentSummaryLevel && SummaryNode.isFirstGroupNode(next.getNode()))
                    break;
            }
        }
        return suggestNewSelectedSummary(direction, parent);
    }

    private void selectDescendant(NodeView newSelected, boolean continious, PreferredChild preferredChild) {
        newSelected = newSelected.getDescendant(preferredChild);
        select(newSelected, continious);
    }

    private boolean unfoldInDirection(SelectionDirection direction) {
        final NodeView oldSelected = selection.getSelectionEnd();
        boolean selectedUsesHorizontalLayout = oldSelected.usesHorizontalLayout();
        if (oldSelected.isFolded() && unfoldsOnNavigation()
                && (oldSelected.getChildNodesAlignment().isStacked()
                        || selectedUsesHorizontalLayout
                        && (direction == SelectionDirection.UP || direction == SelectionDirection.DOWN)
                        || (! selectedUsesHorizontalLayout)
                        && (direction == SelectionDirection.LEFT || direction == SelectionDirection.RIGHT))) {
            final NodeModel oldModel = oldSelected.getNode();
            getModeController().getMapController().unfoldAndScroll(oldModel, filter);
            if(oldSelected.isContentVisible())
                return true;
        }
        return false;
    }

    public boolean selectUp(final boolean continious) {
	    return selectRelatedNode(SelectionDirection.UP, continious);
	}

    private boolean selectDistantSibling(final NodeView oldSelectionEnd, final boolean continious, boolean selectsForward) {
        if(oldSelectionEnd == null || oldSelectionEnd.isRoot())
            return false;
        LayoutOrientation layoutOrientation = oldSelectionEnd.getParentView().layoutOrientation();
        SiblingSelection siblingSelection = ResourceController.getResourceController().getEnumProperty("siblingSelection", SiblingSelection.CHANGE_PARENT);
        NodeView nextSelected = oldSelectionEnd;
        for(;;)  {
        	NodeView sibling = getNextVisibleSibling(nextSelected, layoutOrientation, selectsForward, SiblingSelection.STAY_AT_THE_END);
        	if(sibling == null || sibling == nextSelected)
        		break;
        	nextSelected = sibling;
        	if(continious)
        		selectPreservingSiblingMaxLevel(nextSelected, continious);
        }
        if(nextSelected == oldSelectionEnd && siblingSelection != SiblingSelection.STAY_AT_THE_END)
        	nextSelected = getNextVisibleSibling(nextSelected, layoutOrientation, selectsForward, siblingSelection);
        if(nextSelected != oldSelectionEnd && nextSelected != null) {
			if (! continious || siblingSelection != SiblingSelection.STAY_AT_THE_END) {
				selectPreservingSiblingMaxLevel(nextSelected, continious);
			}
			return true;
		}
        return false;
    }

	private NodeView getNextVisibleSiblingAtAnyLevel(final NodeView node, LayoutOrientation layoutOrientation, final boolean down, final SiblingSelection siblingSelection) {
	    return down ? node.getNextVisibleSibling(layoutOrientation, siblingSelection) : node.getPreviousVisibleSibling(layoutOrientation, siblingSelection);
    }

	public boolean selectDown(final boolean continious) {
            return selectRelatedNode(SelectionDirection.DOWN, continious);
	}

	public boolean selectPageDown(final boolean continious) {
		return selectDistantSibling(selection.getSelectionEnd(), continious, true);
    }

	public boolean selectPageUp(final boolean continious) {
		return selectDistantSibling(selection.getSelectionEnd(), continious, false);
    }

	/*****************************************************************
	 ** P A I N T I N G **
	 *****************************************************************/
	/*
	 * (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(final Graphics g) {
		if (!isPrinting() && isPreparedForPrinting){
			isPreparedForPrinting = false;
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					endPrinting();
					repaint();
				}
			});
			return;
		}

		final Graphics2D g2 = (Graphics2D) g.create();
		try {
			antiAliasingConfigurator.withAntialias(g2, () -> {
    			if(! isPrinting() && g2.getRenderingHint(GraphicsHints.CACHE_ICONS) == null) {
    				g2.setRenderingHint(GraphicsHints.CACHE_ICONS, Boolean.TRUE);
    			}
    			if (containsExtension(Connectors.class)){
    				hideSingleEndConnectors = false;
    				showConnectors = SHOW_CONNECTOR_LINES;
    				paintConnectorsBehind = false;
    			}
    			else {
    				hideSingleEndConnectors = hideSingleEndConnectorsPropertyValue;
    				showConnectors = showConnectorsPropertyValue;
    				paintConnectorsBehind = ResourceController.getResourceController().getBooleanProperty(
    						"paint_connectors_behind");
    			}
    			super.paint(g2);
			});
		}
		finally {
			paintingMode = null;
			g2.dispose();
		}
	}

	public void paintOverview(Graphics2D g) {
		g.setRenderingHint(GraphicsHints.CACHE_ICONS, Boolean.FALSE);
		paintingPurpose = PaintingPurpose.OVERVIEW;
		isPreparedForPrinting = true;
		updatePrintedSelectedNodes();
		super.print(g);
		paintingPurpose = PaintingPurpose.PAINTING;
		updatePrintedSelectedNodes();
		isPreparedForPrinting = false;
	}

	@Override
	protected void paintComponent(final Graphics g) {
	    boolean usesTransparentBackgroundForPrinting = paintingPurpose == PaintingPurpose.PRINTING && printOnWhiteBackground;
	    boolean backgroundIsPaintedByViewport = paintingPurpose == PaintingPurpose.PAINTING && backgroundComponent != null && fitToViewport;
        if(!usesTransparentBackgroundForPrinting && !backgroundIsPaintedByViewport) {
	        g.setColor(getBackground() );
	        Rectangle clip = g.getClipBounds();
	        if (clip != null) {
	            int x = Math.max(0, clip.x);
	            int y = Math.max(0, clip.y);
	            int w = Math.min(getWidth() - x, clip.width - (x - clip.x));
	            int h = Math.min(getHeight() - y, clip.height - (y - clip.y));
	            g.fillRect(x, y, w, h);
	        } else {
	            g.fillRect(0, 0, getWidth(), getHeight());
	        }	    }
        if (backgroundComponent != null && paintingPurpose != PaintingPurpose.OVERVIEW && ! fitToViewport) {
			paintBackgroundComponent(g);
		}
	}

    private void paintBackgroundComponent(final Graphics g) {
	    final Graphics backgroundGraphics = g.create();
	    try {
	    	final Point centerPoint = getRootCenterPoint();
            final Point backgroundImageTopLeft = getBackgroundImageTopLeft(centerPoint);
            backgroundGraphics.translate(backgroundImageTopLeft.x, backgroundImageTopLeft.y);
	    	backgroundComponent.paintComponent(backgroundGraphics);
	    }
	    finally {
	    	backgroundGraphics.dispose();
	    }
	}

	private Point getRootCenterPoint() {
	    final Point centerPoint = new Point(getRoot().getMainView().getWidth() / 2,
		    getRoot().getMainView().getHeight() / 2);
		UITools.convertPointToAncestor(getRoot().getMainView(), centerPoint, this);
		return centerPoint;
	}

	private Point getBackgroundImageTopLeft(final Point centerPoint) {
		final int x = centerPoint.x - (backgroundComponent.getWidth() / 2);
		final int y = centerPoint.y - (backgroundComponent.getHeight() / 2);
		return new Point(x, y);
	}

	@Override
	protected void paintChildren(final Graphics g) {
	    final PaintingMode paintModes[];
	    if(paintConnectorsBehind)
	    	paintModes = new PaintingMode[]{
	    		PaintingMode.CLOUDS,
	    		PaintingMode.LINKS, PaintingMode.NODES, PaintingMode.SELECTED_NODES
	    		};
	    else
	    	paintModes = new PaintingMode[]{
	    		PaintingMode.CLOUDS,
	    		PaintingMode.NODES, PaintingMode.SELECTED_NODES, PaintingMode.LINKS
	    		};
	    final Graphics2D g2 = (Graphics2D) g;
	    paintChildren(g2, paintModes);
	    if(isSpotlightEnabled())
	    	paintDimmer(g2, paintModes);
		paintSelecteds(g2);
		highlightEditor(g2);
		paintSelectionRectangle(g);
    }

    private void paintSelectionRectangle(Graphics g) {
        if (selectionRectangle == null)
            return;
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(SELECTION_RECTANGLE_STROKE);
        g2d.setColor(getSelectionRectangleColor());
        g2d.draw(selectionRectangle);
    }

	public void setSelectionRectangle(Rectangle newRectangle) {
        Rectangle oldRectangle = selectionRectangle;
        selectionRectangle = newRectangle;
        Rectangle repaintedRectangle = oldRectangle == null ? newRectangle
                : newRectangle == null ? oldRectangle
                : oldRectangle.union(newRectangle);
        if(repaintedRectangle != null) {
            int lineWidth = 1 + (int)SELECTION_RECTANGLE_STROKE.getLineWidth() / 2;
            repaint(repaintedRectangle.x - lineWidth, repaintedRectangle.y - lineWidth, repaintedRectangle.width + 2 * lineWidth, repaintedRectangle.height + 2 * lineWidth);
        }
    }

    public boolean isSpotlightEnabled() {
		return isClientPropertyTrue(MapView.SPOTLIGHT_ENABLED);
	}

    private boolean isClientPropertyTrue(String name) {
        return Boolean.TRUE == getClientProperty(name);
    }

	private void paintChildren(final Graphics2D g2, final PaintingMode[] paintModes) {
	    for(final PaintingMode paintingMode : paintModes){
	    	this.paintingMode = paintingMode;
			switch(paintingMode){
	    		case LINKS:
	    			if(HIDE_CONNECTORS != showConnectors && paintingPurpose != PaintingPurpose.OVERVIEW)
	    				paintConnectors(g2);
	    			break;
				default:
					super.paintChildren(g2);
			}
	    }
    }


	private void paintDimmer(final Graphics2D g2, final PaintingMode[] paintModes) {
		final Color color = g2.getColor();
		try{
			final Color dimmer = spotlightBackgroundColor;
			g2.setColor(dimmer);
			g2.fillRect(0, 0, getWidth(), getHeight());
		}
		finally{
			g2.setColor(color);
		}
		for (final NodeView selected : getSelection()) {
			highlightSelected(g2, selected, paintModes);
		}
    }

	private void highlightEditor(final Graphics2D g2) {
	    final Component editor = getComponent(0);
		if(editor instanceof NodeView)
	    	return;
	    final java.awt.Shape oldClip = g2.getClip();
	    try{
	    	g2.setClip(editor.getX(), editor.getY(), editor.getWidth(), editor.getHeight());
	    	super.paintChildren(g2);
	    }
	    finally{
	    	g2.setClip(oldClip);
	    }

    }

	protected PaintingMode getPaintingMode() {
		return paintingMode;
	}

	private void paintConnectors(final Collection<? extends NodeLinkModel> links, final Graphics2D graphics,
	                        final HashSet<ConnectorModel> alreadyPaintedLinks) {
		final Font font = graphics.getFont();
		try {
			final Iterator<? extends NodeLinkModel> linkIterator = links.iterator();
			while (linkIterator.hasNext()) {
				final NodeLinkModel next = linkIterator.next();
				if (!(next instanceof ConnectorModel)) {
					continue;
				}
				final ConnectorModel ref = (ConnectorModel) next;
				if (alreadyPaintedLinks.add(ref)) {
				    if(! ref.isVisible(getFilter()))
				        return;
					final NodeModel source = ref.getSource();
					final NodeView sourceView = getDisplayedNodeView(source);
					NodeModel target = ref.getTarget();
                    final NodeView targetView = getDisplayedNodeView(target);
					if(! isConnectorVisibleOnView(sourceView, targetView))
					    continue;
					final ILinkView arrowLink;
					final boolean areBothNodesVisible = sourceView != null && targetView != null
							&& sourceView.isContentVisible() && targetView.isContentVisible();
					boolean b = sourceView != null && sourceView.isSelected() || targetView != null && targetView.isSelected();
                    final boolean showsConnectorLinesOrArrows = showsConnectorLinesOrArrows(b);
					if(showsConnectorLinesOrArrows) {
						LinkController linkController = LinkController.getController(getModeController());
                        if (areBothNodesVisible
                                && (
                                ConnectorShape.EDGE_LIKE.equals(linkController.getShape(ref)) && ! ref.isSelfLink()
                                || sourceView.getMap().getLayoutType() == MapViewLayout.OUTLINE))
							arrowLink = new EdgeLinkView(ref, getModeController(), sourceView, targetView);
						else if(areBothNodesVisible || ! hideSingleEndConnectors)
							arrowLink = new ConnectorView(ref, sourceView, targetView, getBackground());
						else
							break;
						arrowLink.paint(graphics);
						arrowLinkViews.add(arrowLink);
					}
				}
			}
		}
		finally {
			graphics.setFont(font);
		}
	}

	private boolean isConnectorVisibleOnView(NodeView sourceView, NodeView targetView) {
	    if(paintingPurpose == PaintingPurpose.PRINTING)
	        return true;

        Rectangle sourceRectangle = sourceView != null && sourceView.isContentVisible()
                ? SwingUtilities.convertRectangle(sourceView, sourceView.getMainView().getBounds(), this)
                        : null;

        Rectangle targetRectangle = targetView != null && targetView.isContentVisible()
                ? SwingUtilities.convertRectangle(targetView, targetView.getMainView().getBounds(), this)
                        : null;

        if(sourceRectangle == null && targetRectangle == null)
            return false;

        Rectangle connectorRectangle = sourceRectangle == null ? targetRectangle :
            targetRectangle == null ? sourceRectangle : sourceRectangle.union(targetRectangle);

        final JViewport vp = (JViewport) getParent();
        final Rectangle viewRect = vp.getViewRect();
        viewRect.x -= viewRect.width;
        viewRect.y -= viewRect.height;
        viewRect.width *= 3;
        viewRect.height *= 3;
        return viewRect.intersects(connectorRectangle);
	}

    private void paintConnectors(final Graphics2D graphics) {
		arrowLinkViews = new Vector<ILinkView>();
		if(hasNodeLinks())
			paintConnectors(currentRootView, graphics, new HashSet<ConnectorModel>());
	}

	private void paintConnectors(final NodeView source, final Graphics2D graphics, final HashSet<ConnectorModel> alreadyPaintedConnectors) {
		final NodeModel node = source.getNode();
		final Collection<? extends NodeLinkModel> outLinks = getLinksFrom(node);
		paintConnectors(outLinks, graphics, alreadyPaintedConnectors);
		final Collection<? extends NodeLinkModel> inLinks = getLinksTo(node);
		paintConnectors(inLinks, graphics, alreadyPaintedConnectors);
		paintDescendantConnectors(source, graphics, alreadyPaintedConnectors);
	}

    private void paintDescendantConnectors(final NodeView source, final Graphics2D graphics,
            final HashSet<ConnectorModel> alreadyPaintedConnectors) {
        final int nodeViewCount = source.getComponentCount();
		for (int i = 0; i < nodeViewCount; i++) {
			final Component component = source.getComponent(i);
			if (!(component instanceof NodeView)) {
				continue;
			}
			final NodeView child = (NodeView) component;
			if(!child.isSubtreeVisible())
			    continue;
			if (paintingPurpose == PaintingPurpose.PAINTING && ! child.isSelected()) {
				final Rectangle bounds = SwingUtilities.convertRectangle(source, child.getBounds(), this);
				final JViewport vp = (JViewport) getParent();
				final Rectangle viewRect = vp.getViewRect();
				viewRect.x -= viewRect.width;
				viewRect.y -= viewRect.height;
				viewRect.width *= 3;
				viewRect.height *= 3;
				if (!viewRect.intersects(bounds)) {
				    paintDescendantConnectors(child, graphics, alreadyPaintedConnectors);
					continue;
				}
			}
			paintConnectors(child, graphics, alreadyPaintedConnectors);
		}
    }

	private boolean hasNodeLinks() {
		return LinkController.getController(getModeController()).hasNodeLinks(getMap(), this);
	}

	private Collection<? extends NodeLinkModel> getLinksTo(final NodeModel node) {
		return LinkController.getController(getModeController()).getLinksTo(node, this);
	}

	private Collection<? extends NodeLinkModel> getLinksFrom(final NodeModel node) {
		return LinkController.getController(getModeController()).getLinksFrom(node, this);
	}

	private void paintSelecteds(final Graphics2D g) {
		if (!MapView.drawsRectangleForSelection || isPrinting()) {
			return;
		}
		final Color c = g.getColor();
		final Stroke s = g.getStroke();
		g.setColor(getSelectionRectangleColor());
		g.setStroke(NodeHighlighter.DEFAULT_STROKE);
		for (final NodeView selected : getSelection()) {
			paintSelectionRectangle(g, selected);
		}
		g.setColor(c);
		g.setStroke(s);
	}

	private void updateSelectionColors() {
	    ResourceController resourceController = ResourceController.getResourceController();
	    selectionRectangleColor = ColorUtils.stringToColor(resourceController.getProperty(
	            MapView.RESOURCES_SELECTED_NODE_RECTANGLE_COLOR));
	}

	private RoundRectangle2D.Float getRoundRectangleAround(final NodeView selected, int gap, final int arcw) {
		final JComponent content = selected.getContent();
		final Point contentLocation = new Point();
		UITools.convertPointToAncestor(content, contentLocation, this);
		gap -= 1;
		final RoundRectangle2D.Float roundRectClip = new RoundRectangle2D.Float(
			contentLocation.x - gap, contentLocation.y - gap,
			content.getWidth() + 2 * gap, content.getHeight() + 2 * gap, arcw, arcw);
		return roundRectClip;
	}

	private void paintSelectionRectangle(final Graphics2D g, final NodeView selected) {
		if (Boolean.TRUE.equals(selected.getMainView().getClientProperty(INLINE_EDITOR_ACTIVE))) {
			return;
		}
		final RoundRectangle2D.Float roundRectClip = getRoundRectangleAround(selected, 4, 15);
		g.draw(roundRectClip);
	}

	private void highlightSelected(final Graphics2D g, final NodeView selected, final PaintingMode[] paintedModes) {
		final java.awt.Shape highlightClip;
		if (MapView.drawsRectangleForSelection)
			highlightClip = getRoundRectangleAround(selected, 4, 15);
		else
			highlightClip = getRoundRectangleAround(selected, 4, 2);
		final java.awt.Shape oldClip = g.getClip();
		final Rectangle oldClipBounds = g.getClipBounds();
		try{
			g.setClip(highlightClip);
			if(oldClipBounds != null)
				g.clipRect(oldClipBounds.x, oldClipBounds.y, oldClipBounds.width, oldClipBounds.height);
			final Rectangle clipBounds = highlightClip.getBounds();
			final Color color = g.getColor();
			g.setColor(getBackground());
			g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
			g.setColor(color);
			paintChildren(g, paintedModes);
		}
		finally{
			g.setClip(oldClip);
		}
    }

	/**
	 * Call preparePrinting() before printing and endPrinting() after printing
	 * to minimize calculation efforts
	 */
	public void preparePrinting() {
		paintingPurpose = PaintingPurpose.PRINTING;
		if (!isPreparedForPrinting) {
			isPreparedForPrinting = true;
			updatePrintedNodes();
			fitMap = FitMap.valueOf();
			if (backgroundComponent != null && fitMap == FitMap.BACKGROUND) {
				boundingRectangle = getBackgroundImageInnerBounds();
			}
			else {
				boundingRectangle = getInnerBounds();
			}
		}
	}

	private void updatePrintedNodes() {
		if (zoom == 1f) {
			updateAllNodeViews();
			synchronized (getTreeLock()) {
				validateTree();
			}
		} else
			updatePrintedSelectedNodes();
	}

	private void updatePrintedSelectedNodes() {
		if(! drawsRectangleForSelection){
			selection.selectedSet.forEach(n -> n.update(UpdateCause.SELECTION));
			synchronized (getTreeLock()) {
				validateTree();
			}
		}
	}

	private Rectangle getBackgroundImageInnerBounds() {
		final Point centerPoint = getRootCenterPoint();
		final Point backgroundImageTopLeft = getBackgroundImageTopLeft(centerPoint);
		return new Rectangle(backgroundImageTopLeft.x, backgroundImageTopLeft.y, backgroundComponent.getWidth(), backgroundComponent.getHeight());
	}

	@Override
	public void print(final Graphics g) {
		try {
			preparePrinting();
			super.print(g);
		}
		finally {
			paintingPurpose = PaintingPurpose.PAINTING;
		}
	}

	@Override
	public int print(final Graphics graphics, final PageFormat pageFormat, final int pageIndex) {
		double userZoomFactor = ResourceController.getResourceController().getDoubleProperty("user_zoom", 1);
		userZoomFactor = Math.max(0, userZoomFactor);
		userZoomFactor = Math.min(2, userZoomFactor);
		if ((fitMap == FitMap.PAGE || fitMap == FitMap.BACKGROUND) && pageIndex > 0) {
			return Printable.NO_SUCH_PAGE;
		}
		final Graphics2D g2 = (Graphics2D) graphics.create();
		preparePrinting();
		final double zoomFactor;
		final double imageableX = pageFormat.getImageableX();
		final double imageableY = pageFormat.getImageableY();
		final double imageableWidth = pageFormat.getImageableWidth();
		final double imageableHeight = pageFormat.getImageableHeight();
		g2.clipRect((int)imageableX, (int)imageableY, (int)imageableWidth, (int)imageableHeight);
		final double mapWidth = boundingRectangle.getWidth();
		final double mapHeight = boundingRectangle.getHeight();
		if (fitMap == FitMap.PAGE || fitMap == FitMap.BACKGROUND) {
			final double zoomFactorX = imageableWidth / mapWidth;
			final double zoomFactorY = imageableHeight / mapHeight;
			zoomFactor = Math.min(zoomFactorX, zoomFactorY) * 0.99;
		}
		else {
			if (fitMap == FitMap.WIDTH) {
				zoomFactor = imageableWidth / mapWidth * 0.99;
			}
			else if (fitMap == FitMap.HEIGHT) {
				zoomFactor = imageableHeight / mapHeight * 0.99;
			}
			else {
				zoomFactor = userZoomFactor / UITools.FONT_SCALE_FACTOR;
			}
			final int nrPagesInWidth = (int) Math.ceil(zoomFactor * mapWidth
				/ imageableWidth);
			final int nrPagesInHeight = (int) Math.ceil(zoomFactor * mapHeight
				/ imageableHeight);
			if (pageIndex >= nrPagesInWidth * nrPagesInHeight) {
				return Printable.NO_SUCH_PAGE;
			}
			final int yPageCoord = (int) Math.floor(pageIndex / nrPagesInWidth);
			final int xPageCoord = pageIndex - yPageCoord * nrPagesInWidth;
			g2.translate(-imageableWidth * xPageCoord, -imageableHeight * yPageCoord);
		}
		g2.translate(imageableX, imageableY);
		g2.scale(zoomFactor, zoomFactor);
		final double mapX = boundingRectangle.getX();
		final double mapY = boundingRectangle.getY();
		g2.translate(-mapX, -mapY);
		print(g2);
		g2.dispose();
		return Printable.PAGE_EXISTS;
	}

	private void repaintSelecteds(boolean update) {
	    updateSelectionColors();
		for (final NodeView selected : getSelection()) {
			repaintAfterSelectionChange(selected, update);
		}
	}

	private Color requiredBackground() {
		final MapStyle mapStyle = getModeController().getExtension(MapStyle.class);
		final Color mapBackground = mapStyle.getBackground(viewedMap);
		return mapBackground;
	}

	void revalidateSelecteds() {
		selectedsValid = false;
	}

	/**
	 * Select the node, resulting in only that one being selected.
	 */
	public void selectAsTheOnlyOneSelected(final NodeView newSelected) {
		final NodeModel node = newSelected.getNode();
		if(node.isHiddenSummary())
			throw new AssertionError("select invisible node");
		selectAsTheOnlyOneSelected(newSelected, true);
	}

	public void selectAsTheOnlyOneSelected(final NodeView newSelected, final boolean requestFocus) {
		newSelected.invalidate();
		if (requestFocus && ! newSelected.focused()) {
			newSelected.requestFocusInWindow();
		}
		selection.select(newSelected);
		mapScroller.scrollNodeToVisible(newSelected);
		Container selectionParent = newSelected.getParent();
		if (selectionParent instanceof NodeView) {
			((NodeView) selectionParent).setLastSelectedChild(newSelected);
		}
		setSiblingMaxLevel(newSelected);
	}

	/**
	 * Select the node and his descendants. On extend = false clear up the
	 * previous selection. if extend is false, the past selection will be empty.
	 * if yes, the selection will extended with this node and its children
	 */
	private void addBranchToSelection(final NodeView newlySelectedNodeView) {
		if (newlySelectedNodeView.isContentVisible()) {
			addSelected(newlySelectedNodeView, false);
		}
		for (final NodeView target : newlySelectedNodeView.getChildrenViews()) {
			addBranchToSelection(target);
		}
	}

	void selectContinuous(final NodeView newSelected) {
		final NodeView selectionStart = selection.getSelectionStart();
		final NodeView selectionEnd = selection.getSelectionEnd();
		final NodeView parentView = newSelected.getParentView();
		final boolean left = newSelected.isTopOrLeft();
		if(isRoot(newSelected)
				|| selectionStart == null || selectionEnd == null
				|| parentView != selectionStart.getParentView() || parentView != selectionEnd.getParentView()
				|| left != selectionStart.isTopOrLeft() || newSelected.isTopOrLeft() != selectionEnd.isTopOrLeft()){
			if(!newSelected.isSelected())
				selection.add(newSelected);
			mapScroller.scrollNodeToVisible(newSelected);
			return;
		}

		boolean selectionFound = false;
		boolean selectionRequired = false;
		for (final NodeView child : parentView.getChildrenViews()){
			if(child.isTopOrLeft() == left){
				final boolean onOldSelectionMargin = child == selectionStart || child == selectionEnd;
				final boolean selectionFoundNow = ! selectionFound && onOldSelectionMargin;
				selectionFound = selectionFound || selectionFoundNow;

				final boolean onNewSelectionMargin = child == selectionStart || child == newSelected;
				final boolean selectionRequiredNow = ! selectionRequired && onNewSelectionMargin;
				selectionRequired = selectionRequired || selectionRequiredNow;

				if(selectionRequired && ! selectionFound && child.getNode().hasVisibleContent(filter))
					selection.add(child);
				else if(! selectionRequired && selectionFound) {
                    selection.deselect(child);
                    updateSelectedNode();
                }

				if(selectionFound && (selectionStart == selectionEnd || ! selectionFoundNow && onOldSelectionMargin))
					selectionFound = false;
				if(selectionRequired && (selectionStart == newSelected ||  ! selectionRequiredNow && onNewSelectionMargin))
					selectionRequired = false;
			}
		}
		mapScroller.scrollNodeToVisible(newSelected);

	}

	public void setMoveCursor(final boolean isHand) {
		final int requiredCursor = isHand ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR;
		if (getCursor().getType() != requiredCursor) {
			setCursor(requiredCursor != Cursor.DEFAULT_CURSOR ? new Cursor(requiredCursor) : null);
		}
	}

	private void setSiblingMaxLevel(NodeView newSelected) {
	    if (siblingMaxLevel >= 0)
	        siblingMaxLevel = newSelected.getNode().getNodeLevel(filter);
	}

    public void setZoom(final float zoom) {
        if(this.zoom != zoom) {
            this.zoom = zoom;
            scrollsViewAfterLayout = true;
            mapScroller.anchorToNode(getSelected(), CENTER_ALIGNMENT, CENTER_ALIGNMENT);
            updateAllNodeViews(UpdateCause.ZOOM);
            adjustBackgroundComponentScale();
        }
    }

    public void setZoom(final float zoom, Point keptPoint) {
        if(this.zoom != zoom) {
            this.zoom = zoom;
            NodeView selected = getSelected();
            MainView mainView = selected.getMainView();
            float referenceWidth = mainView.getWidth();
            float referenceHeight = mainView.getHeight();
            Point mainViewLocation = new Point();
            UITools.convertPointToAncestor(mainView, mainViewLocation, this);
            float x = referenceWidth > 0 ? (keptPoint.x - mainViewLocation.x) / referenceWidth : 0;
            float y = referenceHeight > 0 ? (keptPoint.y - mainViewLocation.y) / referenceHeight : 0;
            scrollsViewAfterLayout = true;
            mapScroller.anchorToNode(selected, x, y);
            updateAllNodeViews(UpdateCause.ZOOM);
            adjustBackgroundComponentScale();
        }
    }

	private void adjustBackgroundComponentScale() {
		if (backgroundComponent != null) {
			if (fitToViewport) {
				final JViewport vp = (JViewport) getParent();
				final Dimension viewPortSize = vp.getVisibleRect().getSize();
				((ScalableComponent) backgroundComponent).setFinalViewerSize(viewPortSize);
			}
			else {
				((ScalableComponent) backgroundComponent).setMaximumComponentSize(getPreferredSize());
				((ScalableComponent) backgroundComponent).setFinalViewerSize(zoom);
			}
            SwingUtilities.invokeLater(this::repaint);
		}
	}

	/**
	 * Add the node to the selection if it is not yet there, remove it
	 * otherwise.
	 */
	private void toggleSelected(final NodeView nodeView) {
		if (isSelected(nodeView)) {
			if(selection.size() > 1) {
                selection.deselect(nodeView);
                updateSelectedNode();
            }
		}
		else {
			selection.add(nodeView);
			mapScroller.scrollNodeToVisible(nodeView);
		}
	}

	private void validateSelecteds() {
		if (selectedsValid) {
			return;
		}
		selectedsValid = true;
		final NodeView selectedView = getSelected();
		if(selectedView == null){
			final NodeView root = getRoot();
			selectAsTheOnlyOneSelected(root);
			mapScroller.scrollToRootNode();
			return;
		}
		final NodeModel selectedNode = selectedView.getNode();
		Collection<NodeView> lastSelectedNodes = selection.getSelectedList();
        final ArrayList<NodeView> selectedNodes = new ArrayList<NodeView>(lastSelectedNodes.size());
		for (final NodeView nodeView : getSelection()) {
			if (nodeView != null && nodeView.isContentVisible()) {
				selectedNodes.add(nodeView);
			}
		}
		if(lastSelectedNodes.size() == selectedNodes.size() && selectedNodes.size() > 0)
		    return;
		selection.replace(selectedNodes);
		if (getSelected() != null) {
			return;
        }
		for(NodeModel node = selectedNode.getParentNode(); node != null; node = node.getParentNode()){
			final NodeView newNodeView = getNodeView(node);
			if(newNodeView != null && newNodeView.isContentVisible() ){
				selectAsTheOnlyOneSelected(newNodeView);
				return;
			}
		}
		selectAsTheOnlyOneSelected(getRoot());
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.Container#validateTree()
	 */
	@Override
	protected void validateTree() {
		if(isDisplayable() && getRoot().isDisplayable()) {
			validateSelecteds();
			getRoot().validateTree();
			super.validateTree();
		}
	}

	public void repaintVisible() {
		Container parent = getParent();
		if(parent != null)
			parent.repaint();
	}

	@Override
	public void propertyChanged(final String propertyName, final String newValue, final String oldValue) {
		if(propertyName.equals(TextController.MARK_TRANSFORMED_TEXT))
			UITools.repaintAll(getRoot());
	}

	public void selectVisibleAncestorOrSelf(NodeView preferred) {
		while(! preferred.isContentVisible())
			preferred = preferred.getParentView();
		selectAsTheOnlyOneSelected(preferred);
    }

    public Font getNoteFont() {
        return noteFont;
    }

    public Color getNoteForeground() {
        return noteForeground;
    }

    public Color getNoteBackground() {
        return noteBackground;
    }



	public NodeCss getDetailCss() {
		return detailCss;
	}

	public NodeCss getNoteCss() {
		return noteCss;
	}

	public int getNoteHorizontalAlignment() {
		return noteHorizontalAlignment;
	}

   public Font getDetailFont() {
        return detailFont;
    }

    public Color getDetailForeground() {
        return detailForeground;
    }

    public Color getDetailBackground() {
        return detailBackground;
    }

	public int getDetailHorizontalAlignment() {
		return detailHorizontalAlignment;
	}

	public boolean isSelected() {
	    return Controller.getCurrentController().getMapViewManager().getMapViewComponent() == MapView.this;
    }

	void selectIfSelectionIsEmpty(final NodeView nodeView) {
		if(selection.selectedNode == null)
			selectAsTheOnlyOneSelected(nodeView);
    }

	public static MapView getMapView(final Component component) {
    	if(component instanceof MapView)
    		return (MapView) component;
    	return (MapView) SwingUtilities.getAncestorOfClass(MapView.class, component);
    }

	public void select() {
		getModeController().getController().getMapViewManager().changeToMapView(this);
    }

	@Override
    public void setSize(final int width, final int height) {
		final boolean sizeChanged = getWidth() != width || getHeight() != height;
		if(sizeChanged) {
			super.setSize(width, height);
			validate();
		}
    }

	void scrollViewAfterLayout() {
		if(isDisplayable() && ! selection.selectionChanged && isFrameLayoutCompleted()) {
			if(scrollsViewAfterLayout ) {
				scrollsViewAfterLayout  = false;
				mapScroller.scrollView();
			}
			else
				setAnchorContentLocation();
		}
	}
	public void scrollBy(final int x, final int y) {
		mapScroller.scrollBy(x, y);
	}

	public void scrollNodeToVisible(final NodeView node) {
		mapScroller.scrollNodeToVisible(node);
	}

	public void setAnchorContentLocation() {
		mapScroller.setAnchorContentLocation();
	}

    public void preserveRootNodeLocationOnScreen() {
        mapScroller.anchorToRoot();
    }


	public void preserveSelectedNodeLocation() {
		if(selectedsValid)
			preserveNodeLocationOnScreen(getSelected());
    }

    public void preserveNodeLocationOnScreen(NodeView nodeView) {
        int horizontalPoint = nodeView.isTopOrLeft() ? 1 : 0;
        preserveNodeLocationOnScreen(nodeView, horizontalPoint, 0);
    }

    public void preserveNodeLocationOnScreen(final NodeView nodeView, final float horizontalPoint, final float verticalPoint) {
		mapScroller.anchorToNode(nodeView, horizontalPoint, verticalPoint);
	}

    public void setShowsSelectedAfterScroll(boolean showSelectedAfterScroll) {
        mapScroller.setShowsSelectedAfterScroll(showSelectedAfterScroll);
    }


	public void display(final NodeModel node) {
		display(node, false);
	}

	private void display(final NodeModel node, boolean unfoldParentNodes) {
		NodeModel currentRoot = currentRootView.getNode();
		if(currentRoot != node && ! node.isDescendantOf(currentRoot))
			restoreRootNode();
		final NodeView nodeView = getNodeView(node);
		if(nodeView != null)
			return;
		final NodeModel parentNode = node.getParentNode();
		if(parentNode == null)
		    return;
		display(parentNode, unfoldParentNodes);
		final NodeView parentView = getNodeView(parentNode);
		if(parentView == null)
		    return;
		if(unfoldParentNodes && parentNode.isFolded() && isSelected())
			parentNode.setFolded(false);
		else if(parentView.isFolded())
			parentView.setFolded(false);
	}


    private boolean showsConnectorLinesOrArrows(boolean isSelected) {
        final boolean showsConnectorLinesOrArrows = SHOW_CONNECTOR_LINES == showConnectors
                || HIDE_CONNECTOR_LINES == showConnectors
                || isSelected
                    && (SHOW_ARROWS_FOR_SELECTION_ONLY == showConnectors || SHOW_CONNECTORS_FOR_SELECTION_ONLY == showConnectors) ;
        return showsConnectorLinesOrArrows;
    }

    public boolean showsConnectorLines(boolean isSelected) {
	    return showConnectors == SHOW_CONNECTOR_LINES ||
	            isSelected && showConnectors == SHOW_CONNECTORS_FOR_SELECTION_ONLY;
	}

	public boolean showsIcons() {
		return iconLocation != IconLocation.HIDE;
	}


	public IconLocation getIconLocation() {
        return iconLocation;
    }

    public int getLayoutSpecificMaxNodeWidth() {
		return usesLayoutSpecificMaxNodeWidth() ? Math.max(0, getViewportSize().width - 10 * getZoomed(outlineHGap)) : 0;
	}

	public boolean usesLayoutSpecificMaxNodeWidth() {
		return isOutlineLayoutSet() && outlineViewFitsWindowWidth();
	}

	private boolean outlineViewFitsWindowWidth() {
		return outlineViewFitsWindowWidth;
	}

    public Filter getFilter() {
        return filter;
    }

    static public Color getSelectionRectangleColor() {
        return selectionRectangleColor;
    }

    static public boolean drawsRectangleForSelection() {
        return drawsRectangleForSelection;
    }

    public void onEditingStarted(ZoomableLabel label) {
    	if(label instanceof MainView) {
    		label.putClientProperty(MapView.INLINE_EDITOR_ACTIVE, Boolean.TRUE);
			if (MapView.drawsRectangleForSelection) {
				repaintSelecteds(false);
			}
		}
    }

	public void onEditingFinished(ZoomableLabel label) {
    	if(label instanceof MainView) {
    		label.putClientProperty(MapView.INLINE_EDITOR_ACTIVE, null);
			if (MapView.drawsRectangleForSelection) {
				repaintSelecteds(false);
			}
		}
	}

	boolean allowsCompactLayout() {
		return allowsCompactLayout;
	}

	boolean isAutoCompactLayoutEnabled() {
		return isAutoCompactLayoutEnabled;
	}

	@Override
	public void invalidate() {
		if(! currentRootView.isValid() && ! isPreparedForPrinting)
			scrollsViewAfterLayout = true;
		super.invalidate();
	}

    boolean isRoot(NodeView nodeView) {
        return nodeView == currentRootView;
    }

    boolean isSearchRoot(NodeView nodeView) {
        return nodeView.getNode() == currentSearchRoot;
    }

    private void setSearchRoot(NodeModel searchRoot) {
        if(searchRoot == currentSearchRoot)
            return;
        NodeView lastSearchRootView = getNodeView(currentSearchRoot);
        if(lastSearchRootView != null) {
            currentSearchRoot = null;
            lastSearchRootView.updateIcons();
        }
        currentSearchRoot = searchRoot;
        NodeView currentSearchRootView = getNodeView(searchRoot);
        if(currentSearchRootView != null)
            currentSearchRootView.updateIcons();
    }

	void setRootNode(NodeModel node) {
		NodeModel currentRootNode = currentRootView.getNode();
		if(currentRootNode == node)
			return;
        NodeView nodeView = getNodeView(node);
        RootChange rootChange;
        if(nodeView == null) {
            nodeView = NodeViewFactory.getInstance().newNodeView(node, this);
            rootChange = RootChange.ANY;
        }
        else if(SwingUtilities.isDescendingFrom(nodeView, currentRootView)){
            rootChange = RootChange.JUMP_IN;
        }
        else
            rootChange = RootChange.ANY;
		if(! currentRootNode.isRoot() && rootChange == RootChange.JUMP_IN) {
			rootsHistory.add(currentRootView);
		}
        setRootNode(nodeView, rootChange);
        validateAndScroll();
	}


	public void usePreviousViewRoot() {
	    NodeView lastRoot = currentRootView;
		NodeView newRoot;
		if(rootsHistory.size() == 0)
			newRoot = mapRootView;
		else {
			newRoot = rootsHistory.remove(rootsHistory.size() - 1);
		}
		setRootNode(newRoot, RootChange.JUMP_OUT);
		if(lastRoot.isFolded()) {
		    lastRoot.fireFoldingChanged();
		}
        validateAndScroll();
	}

    private void validateAndScroll() {
        final JViewport mapViewport = (JViewport) getParent();
        if(mapViewport != null)
            mapViewport.validate();
    }


    int calculateComponentIndex(Container parent, int index) {
        if (parent == currentRootParentView
                && index > calculateCurrentRootNodePosition())
            return index - 1;
        else
            return index;
    }

	private int calculateCurrentRootNodePosition() {
		NodeModel currentRoot = currentRootView.getNode();
		NodeModel currentParent = currentRootParentView.getNode();
		return currentParent.getIndex(currentRoot);
	}


	void restoreRootNode() {
		restoreRootNode(-1, false);
	}

	void restoreRootNodeTemporarily() {
		restoreRootNode(-1, true);
	}

	void restoreRootNode(int index) {
		restoreRootNode(index, false);
	}

	private void restoreRootNode(int index, boolean temporarily) {
	    if(currentRootView == mapRootView)
	        return;
	    if(currentRootParentView != null) {
	        remove(ROOT_NODE_COMPONENT_INDEX);
	        currentRootParentView.add(currentRootView,
	                index >= 0 ? index : calculateCurrentRootNodePosition());
	    }
	    else {
	        currentRootView.remove();
	    }
        add(mapRootView, ROOT_NODE_COMPONENT_INDEX);
	    NodeView lastRoot = currentRootView;
	    currentRootView.invalidate();
	    currentRootView = mapRootView;
	    currentRootParentView = null;
	    if(! temporarily) {
		    rootsHistory.forEach(NodeView::keepUnfolded);
            rootsHistory.clear();
            mapRootView.resetLayoutPropertiesRecursively();
            updateSelectedNode();
            if(selection.selectedNode == null)
            	selection.select(currentRootView);
            fireRootChanged();
            currentRootView.updateIcons();
            setSiblingMaxLevel(getSelected());
            if(lastRoot.getParent() != null && lastRoot.isFolded()) {
                lastRoot.fireFoldingChanged();
            }
        }
	    lastRoot.updateIcons();
	}

    private void fireRootChanged() {
        MapController mapController = modeController.getMapController();
		mapController.fireMapChanged(
                new MapChangeEvent(this, getMap(), IMapViewManager.MapChangeEventProperty.MAP_VIEW_ROOT, null, null, false));
		if(isSelected()) {
			selection.synchronizeAcrossVisibleViews(MapView::synchronizeRoot);
		}
    }

    static enum RootChange{JUMP_IN, JUMP_OUT, ANY}

	private void setRootNode(NodeView newRootView, RootChange rootChange) {
		if(currentRootView == newRootView)
			return;
		boolean newRootWasFolded = newRootView.isFolded() && ! (rootChange == RootChange.JUMP_OUT);
		if(rootChange == RootChange.JUMP_OUT)
            preserveNodeLocationOnScreen(currentRootView, 0, 0);
        else if(rootChange == RootChange.JUMP_IN)
			preserveNodeLocationOnScreen(newRootView, 0, 0);

		NodeView nextSelectedNode;
		if(rootChange == RootChange.JUMP_OUT)
			nextSelectedNode = selection.selectedNode;
		else
			nextSelectedNode = newRootView;
		if(rootChange == RootChange.ANY)
		    restoreRootNode();
		else
		    restoreRootNodeTemporarily();
		if(currentRootView != newRootView) {
		    currentRootView.invalidate();
			currentRootView = newRootView;
			currentRootParentView = newRootView.getParentView();
			remove(ROOT_NODE_COMPONENT_INDEX);
			add(newRootView, ROOT_NODE_COMPONENT_INDEX);
		}
		else
		    rootsHistory.clear();
		if(nextSelectedNode.getParent() == null || ! nextSelectedNode.isContentVisible())
		    nextSelectedNode = newRootView;
		if(newRootWasFolded) {
			newRootView.fireFoldingChanged();
	        setSiblingMaxLevel(getSelected());
		}
		newRootView.updateIcons();
		newRootView.resetLayoutPropertiesRecursively();
		fireRootChanged();
		if(nextSelectedNode.isDisplayable()) {
		    if(nextSelectedNode == selection.selectedNode)
		        nextSelectedNode.requestFocusInWindow();
		    else
		        selectAsTheOnlyOneSelected(nextSelectedNode);
		}
		revalidate();
		repaint();
		if(rootChange == RootChange.ANY || nextSelectedNode == newRootView)
		    mapScroller.scrollToRootNode();
	}

	public int getDraggingAreaWidth() {
		return getZoomed(draggingAreaWidth);
	}

    public boolean repaintsViewOnSelectionChange() {
        return repaintsViewOnSelectionChange;
    }

    public void setRepaintsViewOnSelectionChange(boolean repaintsViewOnSelectionChange) {
        this.repaintsViewOnSelectionChange = repaintsViewOnSelectionChange;
    }

    void foldingWasSet(NodeView nodeView) {
        selection.foldingWasSet(nodeView);
    }

    public TagLocation getTagLocation() {
        return tagLocation;
    }

    public float calculateNewZoom(MouseWheelEvent e) {
        float oldZoom = getZoom();
        float zoomFactor = 1f + ResourceController.getResourceController().getIntProperty(MAP_VIEW_ZOOM_STEP_PROPERTY) / 100f;
        float zoom = e.getPreciseWheelRotation() > 0 ? (oldZoom / zoomFactor) : (oldZoom * zoomFactor);
        double x = Math.round(Math.log(zoom) / Math.log(zoomFactor));
        zoom = (float) Math.pow(zoomFactor, x);
        zoom = Math.max(Math.min(zoom, 32f), 0.03f);
    	return zoom;
    }

    public void selectNodeViewBySelectionRectangle(boolean replace) {
        List<NodeView> intersectingNodes = getIntersectingNodes();
        if(! intersectingNodes.isEmpty())
            if(replace)
                selection.replace(intersectingNodes);
            else
                intersectingNodes.forEach(selection::add);
    }
    private List<NodeView> getIntersectingNodes() {
        List<NodeView> intersectingComponents = new ArrayList<>();
        if (selectionRectangle != null) {
            findNodesInSelectingRectangle(this, selectionRectangle, intersectingComponents);
        }
        return intersectingComponents;
    }

    private void findNodesInSelectingRectangle(Component comp, Rectangle rect, List<NodeView> results) {
        Rectangle compBounds = new Rectangle(0, 0, comp.getWidth(), comp.getHeight());

        if (compBounds.intersects(rect)) {
            Container parent = comp.getParent();
            if (parent instanceof NodeView && comp == ((NodeView)parent).getContent()) {
                results.add((NodeView) parent);
            } else {
                for (Component child : ((Container) comp).getComponents()) {
                    Rectangle childRect = SwingUtilities.convertRectangle(comp, rect, child);
                    findNodesInSelectingRectangle(child, childRect, results);
                }
            }
        }
    }

	static public boolean showsTagsOnMinimizedNodes() {
		return showsTagsOnMinimizedNodes;
	}
}
