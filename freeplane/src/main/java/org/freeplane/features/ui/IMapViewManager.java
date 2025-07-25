/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
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
package org.freeplane.features.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.RenderedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.freeplane.core.extension.Configurable;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.IMapSelection.NodePosition;
import org.freeplane.features.map.IMapSelectionListener;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.styles.MapViewLayout;

/**
 * @author Dimitry Polivaev
 * 12.01.2009
 */
public interface IMapViewManager {
	enum MapChangeEventProperty { MAP_VIEW_ROOT }

    public void addMapSelectionListener(final IMapSelectionListener pListener);

	public void addMapViewChangeListener(final IMapViewChangeListener pListener);

	/**
	 * is null if the old mode should be closed.
	 *
	 * @return true if the set command was sucessful.
	 */
	public boolean changeToMapView(final Component newMapView);

	public boolean changeToMapView(final String mapViewDisplayName);

	public void changeToMap(MapModel map);

	public boolean changeToMode(final String modeName);

	public String checkIfFileIsAlreadyOpened(final URL urlToCheck) throws MalformedURLException;

	public boolean close();
	public boolean close(final Component view);

	public String createHtmlMap();

	public RenderedImage createImage(int dpi, int imageType);

	public RenderedImage createImage(final Dimension slideSize, NodeModel placedNode, NodePosition placedNodePosition, int imageResolutionInDpi, int imageType);

	public Color getBackgroundColor(NodeModel node);

	public Component getComponent(NodeModel node);

	public boolean isFoldedOnCurrentView(NodeModel node);

	public void displayOnCurrentView(NodeModel node);

	public void setFoldedOnCurrentView(NodeModel node, boolean folded);

	public Font getFont(NodeModel node);

	/** @return an unmodifiable set of all display names of current opened maps. */
	public List<String> getMapKeys();

	public Map<String, MapModel> getMaps();

	public IMapSelection getMapSelection();

	public JComponent getMapViewComponent();

	public Configurable getMapViewConfiguration();

	public List<? extends Component> getMapViews();

	public ModeController getModeController(Component newMap);

	public MapModel getMap();

	public MapModel getMap(Component mapView);

	public Component getSelectedComponent();

	public float getZoom();

	public void newMapView(final MapModel map, ModeController modeController);

	public void removeMapSelectionListener(final IMapSelectionListener pListener);

	public void removeMapViewChangeListener(final IMapViewChangeListener pListener);

	public void scrollNodeToVisible(NodeModel node);

	public void setZoom(float zoom);

	/**
	 * This is the question whether the map is already opened. If this is the
	 * case, the map is automatically opened + returns true. Otherwise does
	 * nothing + returns false.
	 */
	public boolean tryToChangeToMapView(final String mapView);
	public boolean tryToChangeToMapView(final URL url) throws MalformedURLException;

	public void updateMapViewName();

	public boolean isLeftTreeSupported(Component mapViewComponent);

	public Map<String, MapModel> getMaps(String modename);

	public boolean containsView(MapModel map);
	public List<Component> getViews(MapModel map);
	public void obtainFocusForSelected();
	public void setMapTitles();

	public JComboBox createZoomBox();

	public void closeWithoutSaving();

	public void moveFocusFromDescendantToSelection(Component ancestor);

	public boolean isChildHidden(NodeModel nodeOnPath);

	public boolean hasHiddenChildren(NodeModel selected);

	int getHiddenChildCount(NodeModel node);

	public boolean unfoldHiddenChildren(NodeModel node);

	public void hideChildren(NodeModel node);

	public boolean showHiddenNode(NodeModel child);

	public boolean isSpotlightEnabled();

	public void setViewRoot(NodeModel node);

	public void usePreviousViewRoot();

	public void setLayout(final Component map, MapViewLayout newLayoutType);

    default boolean saveModifiedIfNotCancelled(MapModel map) {
        return true;
    }

    default boolean saveAllModifiedMaps(){
        return true;
    }

    public void setMap(Component view, MapModel map);
}
