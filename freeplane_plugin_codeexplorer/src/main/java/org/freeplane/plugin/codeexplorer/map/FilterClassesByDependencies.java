/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2013 Dimitry
 *
 *  This file author is Dimitry
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
package org.freeplane.plugin.codeexplorer.map;

import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.stream.Collectors;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.ActionUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.filter.FilterController;
import org.freeplane.features.filter.condition.ASelectableCondition;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.mode.Controller;

@SuppressWarnings("serial")
class FilterClassesByDependencies extends AFreeplaneAction {

	public FilterClassesByDependencies() {
	    super("code.FilterClassesByDependencies");
    }

	@Override
    public void actionPerformed(ActionEvent e) {
        IMapSelection selection = Controller.getCurrentController().getSelection();
        SelectedNodeDependencies selectedNodeDependencies = new SelectedNodeDependencies(selection);
        Set<String> dependentNodeIDs = selectedNodeDependencies.getSelectedClasses()
                .stream()
                .map(selectedNodeDependencies.getMap()::getClassNodeId)
                .collect(Collectors.toSet());
        if(dependentNodeIDs.isEmpty()) {
            UITools.informationMessage(TextUtils.getRawText("code.no_dependencies_found"));
            return;
        }
        ASelectableCondition condition = new DependencySnapshotCondition(dependentNodeIDs, ActionUtils.getActionTitle(this));
        Filter lastFilter = selection.getFilter();
        Filter filter = new Filter(condition, false, true, lastFilter.areDescendantsShown(), false,
                lastFilter.getFilteredElement(), null);
        FilterController filterController = FilterController.getCurrentFilterController();
        filterController.applyFilter(selection.getMap(), false, filter);
        if(! lastFilter.areAncestorsShown()) {
            AncestorsHider.hideAncestors();
        }

	}
}
