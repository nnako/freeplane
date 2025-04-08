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
package org.freeplane.plugin.codeexplorer;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.features.help.OpenURLAction;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.plugin.codeexplorer.configurator.CodeProjectController;
import org.freeplane.plugin.codeexplorer.map.CodeMap;
import org.freeplane.plugin.codeexplorer.map.CodeMapController;

public class CodeModeController extends MModeController {
	static public final String MODENAME = "CodeExplorer";

	CodeModeController(final Controller controller) {
		super(controller);
        addAction(new OpenURLAction("code.explorerDocumentation",
                ResourceController.getResourceController().getProperty("code.explorerDocumentationUrl")));
		addAction(new OpenURLAction("code.introductionVideo",
		        ResourceController.getResourceController().getProperty("code.introductionVideoUrl")));

	}

	@Override
	public String getModeName() {
		return CodeModeController.MODENAME;
	}

	@Override
	public void startup() {
		final Controller controller = getController();
		controller.getMapViewManager().changeToMode(MODENAME);
		if (controller.getMap() == null) {
			CodeMapController mapController = (CodeMapController) getMapController();
            CodeMap map = mapController.newCodeMap(false);
            mapController.createMapView(map);

		}
		super.startup();
		CodeProjectController projectController = getExtension(CodeProjectController.class);
        projectController.startupController();
	}

	@Override
	public void shutdown() {
	    getExtension(CodeProjectController.class).shutdownController();
	    super.shutdown();
	}
}
