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

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.mode.Controller;

@SuppressWarnings("serial")
class SelectCyclesAction extends AFreeplaneAction {

    public SelectCyclesAction() {
	    super("code.SelectCyclesAction");
    }

	@Override
    public void actionPerformed(ActionEvent e) {
	       IMapSelection selection = Controller.getCurrentController().getSelection();
        CodeNode node = (CodeNode) selection.getSelected();

	        Set<CodeNode> cycleNodes = node.findCyclicDependencies();
	        if(! cycleNodes.isEmpty()) {
	            SelectedNodeDependencies selectedNodeDependencies = new SelectedNodeDependencies(selection);
	            CodeNode[] newSelection = cycleNodes.stream()
	            .map(selectedNodeDependencies::findVisibleAncestorOrSelf)
	            .toArray(CodeNode[]::new);
	            selection.replaceSelection(newSelection);
	        }
	        else
	            UITools.informationMessage(TextUtils.getRawText("code.no_cycles_found"));
 	}
}
