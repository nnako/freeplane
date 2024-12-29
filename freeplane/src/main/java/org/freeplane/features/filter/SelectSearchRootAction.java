/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2009 Dimitry Polivaev
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
package org.freeplane.features.filter;

import java.awt.event.ActionEvent;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.SelectableAction;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

/**
 * @author Dimitry Polivaev
 * Mar 30, 2009
 */
@SelectableAction(checkOnNodeChange = true)
class SelectSearchRootAction extends AFreeplaneAction {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param filterController
	 * @param quickEditor
	 */
	SelectSearchRootAction() {
		super("SelectSearchRootAction");
	}

	@Override
    public void actionPerformed(final ActionEvent e) {
		final IMapSelection selection = Controller.getCurrentController().getSelection();
		final NodeModel selected = selection.getSelected();
		if(selection.getSearchRoot() == selected) {
            selection.makeTheSearchRoot(null);
            setSelected(false);
        } else {
            selection.makeTheSearchRoot(selected);
            setSelected(true);
        }
	}

    @Override
    public void setSelected() {
        final IMapSelection selection = Controller.getCurrentController().getSelection();
        setSelected(selection.getSearchRoot() != null);
    }


}
