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
package org.freeplane.features.map;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.mode.Controller;

/**
 * @author foltin
 */
class ToggleChildrenFoldedAction extends AFreeplaneAction {
	static final String NAME = "toggleChildrenFolded";
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public ToggleChildrenFoldedAction() {
		super("ToggleChildrenFoldedAction");
	}

	@Override
    public void actionPerformed(final ActionEvent e) {
		final Controller controller = Controller.getCurrentController();
		final IMapSelection mapSelection = controller.getSelection();
		Filter filter = mapSelection.getFilter();
		List<NodeModel> selectedNodes = mapSelection.getSortedSelection(true);
		MapController mapController = Controller.getCurrentModeController().getMapController();
		long unfoldedNodeCount = selectedNodes.stream()
		        .filter(node -> node.isFolded())
		        .peek(node -> {
		            node.getChildren().forEach(child -> mapController.setFolded(child, true, filter));
		            if(selectedNodes.size() == 1)
		                mapController.unfoldAndScroll(node, filter);
		            else
		                mapController.unfold(node, filter);
                })
		        .count();
		if(unfoldedNodeCount == 0) {
	        List<NodeModel> childNodes = selectedNodes.stream()
	                .map(NodeModel::getChildren)
	                .flatMap(List::stream)
	                .collect(Collectors.toList());
            if(selectedNodes.size() == 1)
                mapController.toggleFoldedAndScroll(mapSelection.getSelected(), childNodes, filter);
            else
                mapController.toggleFolded(filter, childNodes);

		}
	}
}
