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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.FocusManager;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.freeplane.api.TextWritingDirection;
import org.freeplane.core.awt.GraphicsHints;
import org.freeplane.core.extension.Configurable;
import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.JComboBoxFactory;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.IMapLifeCycleListener;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.IMapSelection.NodePosition;
import org.freeplane.features.map.IMapSelectionListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.styles.MapStyle;
import org.freeplane.features.styles.MapViewLayout;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.features.ui.IMapViewManager;

/**
 * Manages the list of MapViews. As this task is very complex, I exported it
 * from Controller to this class to keep Controller simple. The information
 * exchange between controller and this class is managed by observer pattern
 * (the controller observes changes to the map mapViews here).
 */
public class MapViewController implements IMapViewManager , IMapViewChangeListener, IFreeplanePropertyListener, IMapLifeCycleListener {

	private static final Color TRANSPARENT = new Color(0, true);
    private String lastModeName;
	/** reference to the current mapmapView; null is allowed, too. */
	private MapView selectedMapView;
	MapViewChangeObserverCompound mapViewChangeListeners = new MapViewChangeObserverCompound();
	/**
	 * A vector of MapView instances. They are ordered according to their screen
	 * order.
	 */
	final private Vector<MapView> mapViewVector = new Vector<MapView>();
	private float zoom;
	private boolean setZoomComboBoxRun;
	private final Controller controller;

    /**
	 * Reference to the current mode as the mapView may be null.
	 */
	public MapViewController(Controller controller){
		this.controller =controller;
		controller.setMapViewManager(this);
		controller.addMapLifeCycleListener(this);
		addMapViewChangeListener(this);
		zoomIn = new ZoomInAction(this);
		controller.addAction(zoomIn);
		zoomOut = new ZoomOutAction(this);
		controller.addAction(zoomOut);
		userDefinedZoom = TextUtils.getText("user_defined_zoom");
		zoomModel = new DefaultComboBoxModel(getZooms());
		zoomModel.addElement(userDefinedZoom);
		ResourceController resourceController = ResourceController.getResourceController();
		resourceController.addPropertyChangeListener(this);
		zoomModel.setSelectedItem("100%");
		zoomModel.addListDataListener(new  ListDataListener() {
			@Override
			public void intervalRemoved(ListDataEvent e) {
			}

			@Override
			public void intervalAdded(ListDataEvent e) {
			}

			@Override
			public void contentsChanged(ListDataEvent e) {
				if (!setZoomComboBoxRun && e.getIndex0() == -1) {
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							setZoomByItem(zoomModel.getSelectedItem());
						}
					});
				}
			}
		}) ;
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusedWindow",this::focusSelectedNode);
	}

    protected Controller getController() {
        return controller;
    }

	private void focusSelectedNode(PropertyChangeEvent evt) {
		final Window newWindow = (Window) evt.getNewValue();
		if(newWindow == null)
			return;
		focusSelectedNode(newWindow, 2);
	}

	private void focusSelectedNode(final Window newWindow, int counter) {
		if(counter > 0) {
			SwingUtilities.invokeLater(() -> focusSelectedNode(newWindow, counter - 1));
			return;
		}
		final MapView mapView = getMapView();
		if(mapView == null || SwingUtilities.getWindowAncestor(mapView) != newWindow)
			return;
		final NodeView selectedNode = mapView.getSelected();
		if(selectedNode == null)
			return;
		final Component focusOwner = newWindow.getFocusOwner();
		if(focusOwner == null)
			return;
		final Container nodeView = SwingUtilities.getAncestorOfClass(NodeView.class, focusOwner);
		if (nodeView != null && nodeView  != selectedNode)
			selectedNode.requestFocusInWindow();
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#addMapChangeListener(org.freeplane.core.frame.IMapChangeListener)
	 */
	@Override
	public void addMapSelectionListener(final IMapSelectionListener pListener) {
		mapViewChangeListeners.addListener(pListener);
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#addMapViewChangeListener(org.freeplane.core.frame.IMapViewChangeListener)
	 */
	@Override
	public void addMapViewChangeListener(final IMapViewChangeListener pListener) {
		mapViewChangeListeners.addListener(pListener);
	}

	private void addToOrChangeInMapViews(final String key, final MapView newOrChangedMapView) {
		String extension = "";
		int count = 1;
		final List<String> mapKeys = getMapKeys();
		while (mapKeys.contains(key + extension)) {
			extension = "<" + (++count) + ">";
		}
		newOrChangedMapView.setName((key + extension));
		newOrChangedMapView.setName((key + extension));
		if (!mapViewVector.contains(newOrChangedMapView)) {
			int index = selectedMapView != null ? mapViewVector.indexOf(selectedMapView) + 1 : mapViewVector.size();
			mapViewVector.add(index, newOrChangedMapView);
		}
	}

	@Override
	public void changeToMap(MapModel map) {
		if(selectedMapView != null && selectedMapView.getMap().equals(map))
			return;
		for (final MapView view : mapViewVector) {
			if (view.getMap().equals(map)) {
				changeToMapView(view);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#changeToMapView(org.freeplane.view.swing.map.MapView)
	 */
	@Override
	public boolean changeToMapView(final Component newMapViewComponent) {
		final MapView newMapView = (MapView) newMapViewComponent;
		final MapView oldMapView = selectedMapView;
		if (newMapView == oldMapView) {
			return true;
		}
		mapViewChangeListeners.beforeMapViewChange(oldMapView, newMapView);
		selectedMapView = newMapView;
		if(oldMapView != null)
			oldMapView.repaint();
		if (newMapView != null) {
		    newMapView.revalidateSelecteds();
			final ModeController modeController = newMapView.getModeController();
			lastModeName = modeController.getModeName();
			final float mapViewZoom = newMapView.getZoom();
			if (zoom != mapViewZoom) {
				setZoom(mapViewZoom);
			}
			modeController.getController().selectMode(modeController);
			newMapView.repaint();
		}
		mapViewChangeListeners.afterMapViewChange(oldMapView, newMapView);
		return true;
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#changeToMapView(java.lang.String)
	 */
	@Override
	public boolean changeToMapView(final String mapViewDisplayName) {
		MapView mapViewCandidate = null;
		for (final MapView mapView : mapViewVector) {
			final String mapViewName = mapView.getName();
			if (mapViewDisplayName == mapViewName || mapViewDisplayName != null && mapViewDisplayName.equals(mapViewName)) {
				mapViewCandidate = mapView;
				break;
			}
		}
		if (mapViewCandidate == null) {
			throw new IllegalArgumentException("Map mapView " + mapViewDisplayName + " not found.");
		}
		return changeToMapView(mapViewCandidate);
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#changeToMode(java.lang.String)
	 */
	@Override
	public boolean changeToMode(final String modeName) {
		if (modeName.equals(lastModeName)) {
			return true;
		}
		MapView mapViewCandidate = null;
		for (final MapView mapView : mapViewVector) {
			if (modeName.equals(mapView.getModeController().getModeName())) {
				mapViewCandidate = mapView;
				break;
			}
		}
		final MapView oldMapView = selectedMapView;
		final boolean changed = changeToMapView(mapViewCandidate);
		if (changed) {
			lastModeName = modeName;
			if (oldMapView == selectedMapView) {
				// if the same map remains selected post event for menu updates.
				mapViewChangeListeners.afterMapViewChange(oldMapView, selectedMapView);
			}
		}
		return changed;
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#checkIfFileIsAlreadyOpened(java.net.URL)
	 */
	@Override
	public String checkIfFileIsAlreadyOpened(final URL urlToCheck) throws MalformedURLException {
		for (final MapView mapView : mapViewVector) {
			if (getMap(mapView) != null) {
				final URL mapViewUrl = getMap(mapView).getURL();
				if (sameFile(urlToCheck, mapViewUrl)) {
					return mapView.getName();
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#close(boolean)
	 */
	@Override
	public boolean close() {
		final MapView mapView = getMapView();
		return close(mapView);
	}

	@Override
	public void closeWithoutSaving() {
		final MapView mapView = getMapView();
		closeWithoutSaving(mapView);
	}


	@Override
	public boolean close(final Component mapViewComponent) {
		return close(mapViewComponent, false);
	}

	public boolean closeWithoutSaving(final Component mapViewComponent) {
		return close(mapViewComponent, true);
	}

	private boolean close(final Component mapViewComponent, boolean forceCloseWithoutSaving) {
	    if (mapViewComponent == null) {
			return false;
		}
		MapView mapView = (MapView) mapViewComponent;
		final MapModel map = mapView.getMap();
		final int viewCount = getViews(map).size();
		if(viewCount == 1) {
			final MapController mapController = mapView.getModeController().getMapController();
			if(forceCloseWithoutSaving || saveModifiedIfNotCancelled(map)){
				mapController.closeWithoutSaving(map);
				return true;
			}
			else
				return false;

		}
		map.removeMapChangeListener(mapView);
		remove(mapView);
		mapView.restoreRootNodeTemporarily();
		mapView.getRoot().remove();
		return true;
    }

    private void remove(MapView mapView) {
		int index = mapViewVector.indexOf(mapView);
		ResourceController.getResourceController().removePropertyChangeListener(mapView);
		mapViewVector.remove(mapView);
		if (mapViewVector.isEmpty()) {
			/* Keep the current running mode */
			changeToMapView((MapView) null);
		}
		else if(mapView == selectedMapView){
			if (index > mapViewVector.size()) {
				index = mapViewVector.size();
			} else if (index <= 0) {
				index = 1;
			}
			changeToMapView((mapViewVector.get(index - 1)));
		}
		mapViewChangeListeners.afterMapViewClose(mapView);
	}

	@Override
	public String createHtmlMap() {
		final MapModel model = getMap();
		final ClickableImageCreator creator = new ClickableImageCreator(model.getRootNode(), getMapView()
		    .getModeController(), "FM$1FM");
		return creator.generateHtml();
	}

	@Override
	public RenderedImage createImage(int dpi, int imageType) {
		final MapView view = getMapView();
		if (view == null) {
			return null;
		}
		view.preparePrinting();
		final Rectangle innerBounds = view.getInnerBounds();
		return createImage(dpi, innerBounds, imageType);
	}

	@Override
	public RenderedImage createImage(final Dimension slideSize, NodeModel placedNode, NodePosition placedNodePosition, int dpi, int imageType) {
		final MapView view = getMapView();
		if (view == null) {
			return null;
		}
		final NodeView placedNodeView = view.getNodeView(placedNode);
		if (placedNodeView == null) {
			return createImage(dpi, imageType);
		}

		view.preparePrinting();
		final JComponent content = placedNodeView.getContent();
		Point contentLocation = new Point();
		UITools.convertPointToAncestor(content, contentLocation, view);
		final Rectangle printedGraphicsBounds = new Rectangle(contentLocation.x + content.getWidth() / 2 - slideSize.width / 2,
				contentLocation.y + content.getHeight() / 2 - slideSize.height / 2, slideSize.width, slideSize.height);

		final int distanceToMargin = (slideSize.width - content.getWidth()) / 2 - 10;
		if(placedNodePosition == NodePosition.LEFT){
			printedGraphicsBounds.x += distanceToMargin;
		}
		if(placedNodePosition == NodePosition.RIGHT){
			printedGraphicsBounds.x -= distanceToMargin;
		}
		return createImage(dpi, printedGraphicsBounds, imageType);
	}

	public RenderedImage createImage(int dpi, final Rectangle printedArea, int imageType) {
		final MapView view = getMapView();
		view.preparePrinting();
		final BufferedImage myImage = printToImage(dpi, view, printedArea, imageType);
		view.endPrinting();
		return myImage;
	}

	private BufferedImage printToImage(int dpi, final MapView view, final Rectangle innerBounds, int imageType) {
		double scaleFactor = (double) dpi / (double) (UITools.FONT_SCALE_FACTOR * 72);

		double scaledWidth = innerBounds.width * scaleFactor;
		double scaledHeight = innerBounds.height * scaleFactor;
		if(scaledWidth * scaledHeight > Integer.MAX_VALUE) {
		    UITools.errorMessage(TextUtils.getText("out_of_memory"));
		    return null;
        }
		int imageWidth = (int) Math.ceil(scaledWidth);
        int imageHeight = (int) Math.ceil(scaledHeight);

		final BufferedImage myImage = new BufferedImage(imageWidth, imageHeight, imageType);
		final Graphics2D g = (Graphics2D) myImage.getGraphics();
		g.scale(scaleFactor, scaleFactor);
		g.translate(-innerBounds.x, -innerBounds.y);
		g.setRenderingHint(GraphicsHints.CACHE_ICONS, Boolean.TRUE);
		view.print(g);
		return myImage;
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getBackgroundColor(org.freeplane.core.model.NodeModel)
	 */
	@Override
	public Color getBackgroundColor(final NodeModel node) {
		final MapView mapView = getMapView();
		if (mapView == null) {
			return null;
		}
		final NodeView nodeView = mapView.getNodeView(node);
		if (nodeView == null) {
			return null;
		}
		return nodeView.getTextBackground();
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getComponent(org.freeplane.core.model.NodeModel)
	 */
	@Override
	public Component getComponent(final NodeModel node) {
		if(selectedMapView == null)
			return null;
		final NodeView nodeView = selectedMapView.getNodeView(node);
		if(nodeView == null)
			return null;
		return nodeView.getMainView();
	}

	@Override
	public boolean isFoldedOnCurrentView(NodeModel node){
		if(selectedMapView == null)
			return node.isFolded();
		final NodeView nodeView = selectedMapView.getNodeView(node);
		if(nodeView == null)
			return node.isFolded();
		return nodeView.isFolded();
	}


	@Override
	public void displayOnCurrentView(NodeModel node) {
		if(selectedMapView != null)
			selectedMapView.display(node);
	}

	@Override
	public void setFoldedOnCurrentView(NodeModel node, boolean folded){
		if(selectedMapView == null || ! node.hasChildren())
			return;
		final NodeView nodeView = selectedMapView.getNodeView(node);
		if(nodeView == null)
			return;
		nodeView.setFolded(folded);

	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getFont(org.freeplane.core.model.NodeModel)
	 */
	@Override
	public Font getFont(final NodeModel node) {
		final MapView mapView = getMapView();
		if (mapView == null) {
			return null;
		}
		final NodeView nodeView = mapView.getNodeView(node);
		if (nodeView == null) {
			return null;
		}
		return nodeView.getMainView().getFont();
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getMapKeys()
	 */
	@Override
	public List<String> getMapKeys() {
		final LinkedList<String> returnValue = new LinkedList<String>();
		for (final MapView mapView : mapViewVector) {
			returnValue.add(mapView.getName());
		}
		return Collections.unmodifiableList(returnValue);
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getMaps()
	 */
	@Override
	public Map<String, MapModel> getMaps() {
		final HashMap<String, MapModel> returnValue = new HashMap<String, MapModel>(mapViewVector.size());
		for (final MapView mapView : mapViewVector) {
			returnValue.put(mapView.getName(), getMap(mapView));
		}
		return Collections.unmodifiableMap(returnValue);
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getMapSelection()
	 */
	@Override
	public IMapSelection getMapSelection() {
		final MapView mapView = getMapView();
		return mapView == null ? null : mapView.getMapSelection();
	}

	public MapView getMapView() {
		return selectedMapView;
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getMapViewComponent()
	 */
	@Override
	public JComponent getMapViewComponent() {
		return getMapView();
	}

	@Override
	public Configurable getMapViewConfiguration() {
		return getMapView();
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getMapViewVector()
	 */
	@Override
	public List<MapView> getMapViews() {
		return Collections.unmodifiableList(mapViewVector);
	}

	@Override
	public ModeController getModeController(final Component mapView) {
		return ((MapView) mapView).getModeController();
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getModel()
	 */
	@Override
	public MapModel getMap() {
		final MapView mapView = getMapView();
		return mapView == null ? null : getMap(mapView);
	}

	@Override
	public MapModel getMap(final Component mapView) {
		return ((MapView) mapView).getMap();
	}

	private MapModel getMap(final MapView mapView) {
		return mapView == null ? null : mapView.getMap();
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getSelectedComponent()
	 */
	@Override
	public Component getSelectedComponent() {
		final MapView mapView = getMapView();
		return mapView == null ? null : mapView.getSelected().getMainView();
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getViewNumber()
	 */
	public int getViewNumber() {
		return mapViewVector.size();
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#getZoom()
	 */
	@Override
	public float getZoom() {
		return zoom;
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#newMapView(org.freeplane.core.model.MapModel, org.freeplane.core.modecontroller.ModeController)
	 */
	@Override
	public void newMapView(final MapModel map, final ModeController modeController) {
		final MapView mapView = new MapView(map, modeController);
		addToOrChangeInMapViews(mapView.getName(), mapView);
		ResourceController.getResourceController().addPropertyChangeListener(mapView);
		mapViewChangeListeners.mapViewCreated(selectedMapView, mapView);
		changeToMapView(mapView);
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#nextMapView()
	 */
	public void nextMapView() {
		int index;
		final int size = mapViewVector.size();
		if (getMapView() != null) {
			index = mapViewVector.indexOf(getMapView());
		}
		else {
			index = size - 1;
		}
		if (index + 1 < size && index >= 0) {
			changeToMapView((mapViewVector.get(index + 1)));
		}
		else if (size > 0) {
			changeToMapView((mapViewVector.get(0)));
		}
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#previousMapView()
	 */
	public void previousMapView() {
		int index;
		final int size = mapViewVector.size();
		if (getMapView() != null) {
			index = mapViewVector.indexOf(getMapView());
		}
		else {
			index = 0;
		}
		if (index > 0) {
			changeToMapView((mapViewVector.get(index - 1)));
		}
		else {
			if (size > 0) {
				changeToMapView((mapViewVector.get(size - 1)));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#removeIMapViewChangeListener(org.freeplane.core.frame.IMapChangeListener)
	 */
	@Override
	public void removeMapSelectionListener(final IMapSelectionListener pListener) {
		mapViewChangeListeners.removeListener(pListener);
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#removeMapViewChangeListener(org.freeplane.core.frame.IMapViewChangeListener)
	 */
	@Override
	public void removeMapViewChangeListener(final IMapViewChangeListener pListener) {
		mapViewChangeListeners.removeListener(pListener);
	}

	private boolean sameFile(final URL urlToCheck, final URL mapViewUrl) {
		if (mapViewUrl == null) {
			return false;
		}
		if (urlToCheck.getProtocol().equals("file") && mapViewUrl.getProtocol().equals("file")) {
			return (new File(urlToCheck.getFile())).equals(new File(mapViewUrl.getFile()));
		}
		return urlToCheck.sameFile(mapViewUrl);
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#scrollNodeToVisible(org.freeplane.core.model.NodeModel)
	 */
	@Override
	public void scrollNodeToVisible(final NodeModel node) {
		final NodeView nodeView = selectedMapView.getNodeView(node);
		if (nodeView != null) {
			selectedMapView.scrollNodeToVisible(nodeView);
		}
	}

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#setZoom(float)
	 */
	@Override
	public void setZoom(final float zoom) {
		this.zoom = zoom;
		final MapView mapView = getMapView();
		if (mapView == null) {
			return;
		}
		final MapModel map = mapView.getMap();
		final MapStyle mapStyle = mapView.getModeController().getExtension(MapStyle.class);
		if(mapView.getZoom() != zoom){
			mapView.setZoom(zoom);
		}
		if(mapStyle.getZoom(map) == zoom){
			return;
		}
		mapStyle.setZoom(map, zoom);
		setZoomComboBox(zoom);
		final Object[] messageArguments = { String.valueOf(zoom * 100f) };
		final String stringResult = TextUtils.format("user_defined_zoom_status_bar", messageArguments);
		controller.getViewController().out(stringResult);
	}


	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#tryToChangeToMapView(java.lang.String)
	 */
	@Override
	public boolean tryToChangeToMapView(final String mapView) {
		if (mapView != null && getMapKeys().contains(mapView)) {
			changeToMapView(mapView);
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean tryToChangeToMapView(URL url) throws MalformedURLException {
		final MapModel currentMap = getMap();
		if(currentMap != null && url.equals(currentMap.getURL()))
			return true;
		final String mapExtensionKey = checkIfFileIsAlreadyOpened(url);
		if (mapExtensionKey != null) {
			tryToChangeToMapView(mapExtensionKey);
			return true;
		}
		return false;
    }

	/* (non-Javadoc)
	 * @see org.freeplane.core.frame.IMapViewController#updateMapViewName()
	 */
	@Override
	public void updateMapViewName() {
		final MapView r = getMapView();
		final String name = r.getMap().getTitle();
		addToOrChangeInMapViews(name, getMapView());
		changeToMapView(getMapView());
		setMapTitles();
	}

	@Override
	public boolean isLeftTreeSupported(final Component mapViewComponent) {
		return ((MapView) mapViewComponent).getLayoutType() != MapViewLayout.OUTLINE;
	}

	@Override
	public Map<String, MapModel> getMaps(final String modename) {
		final HashMap<String, MapModel> returnValue = new HashMap<String, MapModel>(mapViewVector.size());
		for (final MapView mapView : mapViewVector) {
			if (mapView.getModeController().getModeName().equals(modename)) {
				returnValue.put(mapView.getName(), getMap(mapView));
			}
		}
		return Collections.unmodifiableMap(returnValue);
	}

	@Override
	public List<Component> getViews(final MapModel map) {
		final LinkedList<Component> list = new LinkedList<Component>();
		for (final MapView view : mapViewVector) {
			if (view.getMap().equals(map)) {
				list.add(view);
			}
		}
		return list;
	}

	@Override
	public boolean containsView(MapModel map) {
		if(selectedMapView != null && selectedMapView.getMap().equals(map))
			return true;
		for (final MapView view : mapViewVector) {
			if (view.getMap().equals(map)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void afterViewChange(final Component oldMap, final Component pNewMap) {
		Controller controller = Controller.getCurrentController();
		final ModeController oldModeController = controller.getModeController();
		ModeController newModeController = oldModeController;
		if (pNewMap != null) {
			final IMapSelection mapSelection = getMapSelection();
			final NodeModel selected = mapSelection.getSelected();
			mapSelection.scrollNodeToVisible(selected);
			setZoomComboBox(getZoom());
			obtainFocusForSelected();
			newModeController = getModeController(pNewMap);
			if (newModeController != oldModeController) {
				controller.selectMode(newModeController);
			}
		}
		setMapTitles();
		controller.getViewController().viewNumberChanged(getViewNumber());
		if (pNewMap != null) {
			newModeController.setVisible(true);
		}
	}



	@Override
    public void afterViewDisplayed(Component oldView, Component newView) {
	    updateMapList();
    }

    @Override
	public void afterViewClose(final Component oldView) {
		ModeController newModeController = getModeController(oldView);
		newModeController.getUserInputListenerFactory().updateMapList();
	}

	@Override
	public void beforeViewChange(final Component oldMap, final Component newMap) {
		Controller controller = Controller.getCurrentController();
		final ModeController modeController = controller.getModeController();
		if (oldMap != null) {
			modeController.setVisible(false);
		}
	}

	private void setZoomByItem(final Object item) {
		final float zoomValue;
		if (((String) item).equals(userDefinedZoom)) {
			final float zoom =getZoom();
			final int zoomInt = Math.round(100 * zoom);
			final SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(zoomInt, 1, 3200, 1);
			JSpinner spinner = new JSpinner(spinnerNumberModel);
			final int option = JOptionPane.showConfirmDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), spinner, TextUtils.getText("enter_zoom"), JOptionPane.OK_CANCEL_OPTION);
			if(option == JOptionPane.OK_OPTION)
				zoomValue = spinnerNumberModel.getNumber().floatValue() / 100;
			else
				zoomValue = zoom;
		}
		else
			zoomValue = getZoomValue(item);
		setZoom(zoomValue);
	}

	final private String userDefinedZoom;
	final private ZoomInAction zoomIn;
	private final DefaultComboBoxModel zoomModel;
	final private ZoomOutAction zoomOut;

	private float getCurrentZoomIndex() {
		final int selectedIndex = zoomModel.getIndexOf(zoomModel.getSelectedItem());
		final int itemCount = zoomModel.getSize();
		if (selectedIndex != - 1) {
			return selectedIndex;
		}
		final float userZoom = getZoom();
		for (int i = 0; i < itemCount - 1; i++) {
			if (userZoom < getZoomValue(zoomModel.getElementAt(i))) {
				return i - 0.5f;
			}
		}
		return itemCount  - 1.5f;
	}

	public String getItemForZoom(final float f) {
		return Math.round(f * 100F) + "%";
	}

	private void setZoomComboBox(final float f) {
		setZoomComboBoxRun = true;
		try {
			final String toBeFound = getItemForZoom(f);
			zoomModel.setSelectedItem(toBeFound);
		}
		finally {
			setZoomComboBoxRun = false;
		}
	}

	public void zoomIn() {
		final float currentZoomIndex = getCurrentZoomIndex();
		if (currentZoomIndex < zoomModel.getSize() - 2) {
			setZoomByItem(zoomModel.getElementAt((int) (currentZoomIndex + 1f)));
		}
	}

	public void zoomOut() {
		final float currentZoomIndex = getCurrentZoomIndex();
		if (currentZoomIndex > 0) {
			setZoomByItem(zoomModel.getElementAt((int) (currentZoomIndex - 0.5f)));
		}
	}

	@Override
	public JComboBox createZoomBox() {
		JComboBox<Object> zoomBox = JComboBoxFactory.create(zoomModel);
		zoomBox.setPrototypeDisplayValue("1000%");
		return zoomBox;
	}

	public String[] getZooms() {
		return zooms;
	}

	private float getZoomValue(final Object item) {
		final String dirty = (String) item;
		final String cleaned = dirty.substring(0, dirty.length() - 1);
		final float zoomValue = Integer.parseInt(cleaned, 10) / 100F;
		return zoomValue;
	}

	private static final String[] zooms = ResourceController.getResourceController().getArrayProperty("predefined_zoom_levels", ", *");

	@Override
	public void obtainFocusForSelected() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (getMapView() != null) {
					final Component selectedComponent = getSelectedComponent();
					if(selectedComponent != null){
						selectedComponent.requestFocus();
					}
				}
			}
		});
	}

	@Override
	public void propertyChanged(final String propertyName, final String newValue, final String oldValue) {
		if (propertyName.equals(ModeController.VIEW_MODE_PROPERTY)
				|| propertyName.equals("workspaceTitle")) {
			setFrameTitle();
			return;
		}
	}
	@Override
	public void setMapTitles() {
		setFrameTitle();
		updateMapList();
	}

    private void updateMapList() {
        ModeController modeController = Controller.getCurrentModeController();
		if (modeController != null) {
			modeController.getUserInputListenerFactory().updateMapList();
		}
    }

	private void setFrameTitle() {
		ModeController modeController = Controller.getCurrentModeController();
		if (modeController != null) {
			final Object[] messageArguments = { TextUtils.getText(("mode_" + modeController.getModeName())) };
			final MessageFormat formatter = new MessageFormat(TextUtils.getText("mode_title"));
			String modeName = formatter.format(messageArguments);
			String viewName = "";
			final MapModel model = getMap();
			String frameTitle;
			String workspaceTitle = ResourceController.getResourceController().getProperty("workspaceTitle");
			if (model != null) {
				viewName = getMapViewComponent().getName();
				frameTitle = (workspaceTitle.isEmpty() ? "" : workspaceTitle + " - ")
						+ viewName
						+ ((model.isSaved() || model.isReadOnly()) ? "" : "*")
						+ " - " + modeName
						+ (modeController.isEditingLocked() ? format("OptionPanel.view_mode.true") :
							model.isReadOnly() ? format("read_only") : "");
				final File file = model.getFile();
				if (file != null) {
					frameTitle += " " + file.getAbsolutePath();
				}
			} else {
				frameTitle = (workspaceTitle.isEmpty() ? "" : workspaceTitle + " - ") + modeName;
			}
			controller.getViewController().setTitle(TextWritingDirection.LEFT_TO_RIGHT.isolatePathSeparators(frameTitle));
		}
		else {
			controller.getViewController().setTitle("");
		}
	}

	private String format(String propertyName) {
		return " (" + TextUtils.getText(propertyName) + ")";
	}

	@Override
	public void onRemove(MapModel map) {
		final List<Component> views = getViews(map);
		for(Component view : views)
			remove((MapView)view);
	}

	@Override
	public void moveFocusFromDescendantToSelection(Component ancestor) {
		Component focusOwner = FocusManager.getCurrentManager().getFocusOwner();
		boolean toolbarLostFocus = focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, ancestor);
		if (toolbarLostFocus) {
			final Component selectedComponent = getSelectedComponent();
			if (selectedComponent != null)
				selectedComponent.requestFocus();
		}
	}

	@Override
	public boolean isChildHidden(NodeModel node) {
		if(selectedMapView == null)
			return false;
		final NodeModel parentNode = node.getParentNode();
		if(parentNode == null)
			return false;
		final NodeView nodeView = selectedMapView.getNodeView(parentNode);
		if(nodeView == null)
			return false;
		return nodeView.isChildHidden(node);
	}

	@Override
	public boolean hasHiddenChildren(NodeModel node) {
		if(selectedMapView == null)
			return false;
		final NodeView nodeView = selectedMapView.getNodeView(node);
		if(nodeView == null)
			return false;
		return nodeView.hasHiddenChildren();
	}

	@Override
	public int getHiddenChildCount(NodeModel node) {
		if(selectedMapView == null)
			return 0;
		final NodeView nodeView = selectedMapView.getNodeView(node);
		if(nodeView == null)
			return 0;
		return nodeView.getHiddenChildCount();
	}

	@Override
	public boolean unfoldHiddenChildren(NodeModel node) {
		if(selectedMapView == null)
			return false;
		final NodeView nodeView = selectedMapView.getNodeView(node);
		if(nodeView == null)
			return false;
		return nodeView.unfoldHiddenChildren();
	}

	@Override
	public void hideChildren(NodeModel node) {
		if(selectedMapView == null)
			return;
		final NodeView nodeView = selectedMapView.getNodeView(node);
		if(nodeView == null)
			return;
		nodeView.hideChildren(node);
	}

	@Override
	public boolean showHiddenNode(NodeModel node) {
		if(selectedMapView == null)
			return false;
		final NodeModel parentNode = node.getParentNode();
		if(parentNode == null)
			return false;
		final NodeView nodeView = selectedMapView.getNodeView(parentNode);
		if(nodeView == null)
			return false;
		return nodeView.showHiddenNode(node);
	}

	@Override
	public boolean isSpotlightEnabled() {
		return selectedMapView != null && selectedMapView.isSpotlightEnabled();
	}

	@Override
	public void setViewRoot(NodeModel node) {
		changeToMap(node.getMap());
		if(selectedMapView == null)
			return;
		selectedMapView.setRootNode(node);
	}

	@Override
	public void usePreviousViewRoot() {
	    if(selectedMapView == null)
	        return;
	    selectedMapView.usePreviousViewRoot();
	}


	@Override
	public void setLayout(Component mapComponent, MapViewLayout newLayoutType) {
	    MapView map = (MapView) mapComponent;
	    MapViewLayout oldLayoutType = map.getLayoutType();
	    map.setLayoutType(newLayoutType);
	    ModeController modeController = map.getModeController();
	    final MapStyle mapStyle = modeController.getExtension(MapStyle.class);
	    mapStyle.setMapViewLayout(map.getMap(), newLayoutType);
	    map.getMapSelection().preserveNodeLocationOnScreen(map.getSelected().getNode(), 0.5f, 0.5f);
	    modeController.getMapController().fireMapChanged(
	            new MapChangeEvent(this, map.getMap(), MapStyle.MAP_LAYOUT, oldLayoutType, newLayoutType));
	    final NodeView root = map.getRoot();
	    invalidateAll(root);
	    root.revalidate();
	}

    private void invalidateAll(final Component c) {
        c.invalidate();
        if(! (c instanceof Container))
            return;
        Container c2 = (Container) c;
        for(int i = 0; i < c2.getComponentCount(); i++)
            invalidateAll(c2.getComponent(i));
    }

    @Override
    public void setMap(Component view, MapModel newMap) {
       if(! (view instanceof MapView))
           return;
       MapView mapView = (MapView) view;
       MapModel oldMap = mapView.getMap();
       if(newMap == oldMap)
           return;
       mapViewChangeListeners.beforeMapChange(oldMap, newMap);
       mapView.setMap(newMap);
       mapView.selectAsTheOnlyOneSelected(mapView.getRoot());
       mapViewChangeListeners.afterMapChange(oldMap, newMap);
    }


}
