/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2010.
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
package org.freeplane.plugin.macos;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenFilesHandler;
import java.awt.desktop.OpenURIEvent;
import java.awt.desktop.OpenURIHandler;
import java.awt.desktop.PreferencesEvent;
import java.awt.desktop.PreferencesHandler;
import java.awt.desktop.QuitEvent;
import java.awt.desktop.QuitHandler;
import java.awt.desktop.QuitResponse;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.function.Predicate;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.Hyperlink;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.ViewController;
import org.freeplane.main.application.MacOptions;


public class MacChanges implements  AboutHandler, OpenFilesHandler, PreferencesHandler, OpenURIHandler, QuitHandler{

	private static Desktop fmMacApplication;

	private final Controller controller;

	private final static Predicate<Window> toggleFullScreenMethod = getToggleFullScreenMethod();

	private static Predicate<Window> getToggleFullScreenMethod(){
		try {
			Class<? extends Object> app = Class.forName("com.apple.eawt.Application");
			Object application = app.getMethod("getApplication").invoke(null);
			Method toggleFullScreenMethod = application.getClass().getMethod("requestToggleFullScreen", Window.class);
			return window -> {
				try {
					toggleFullScreenMethod.invoke(application, window);
					return true;
				}
				catch (Exception e) {
					LogUtils.severe(e);
					return false;
				}
			};
		}
		catch (Exception e) {
			LogUtils.warn(e);
			return window -> false;
		}
	}

	static public void apply(Controller controller) {
		new MacChanges(controller);
	}

	public static void setFullScreen(JFrame window, boolean requestFullScreen) {
		boolean hasFullScreen = window.getY() == 0;
		if(hasFullScreen == requestFullScreen || toggleFullScreenMethod.test(window))
			window.getRootPane().putClientProperty(ViewController.FULLSCREEN_ENABLED_PROPERTY, requestFullScreen);
	}

	private MacChanges(Controller controller) {
		this.controller = controller;
		if(fmMacApplication==null){
		    String helpMenuTitle = TextUtils.getRawText("menu_help");
		    ResourceController resourceController = ResourceController.getResourceController();
		    final URL macProperties = this.getClass().getResource("freeplane_mac.properties");
		    Controller.getCurrentController().getResourceController().addDefaults(macProperties);
		    if(resourceController.getBooleanProperty("add_emojis_to_menu"))
		    	resourceController.putResourceString("menu_help", helpMenuTitle + " ");

		    // if a handleOpen comes here, directly, we know that FM is currently starting.
		    fmMacApplication = Desktop.getDesktop();
			fmMacApplication.setAboutHandler(this);
			fmMacApplication.setPreferencesHandler(this);
			fmMacApplication.setOpenFileHandler(this);
			fmMacApplication.setOpenURIHandler(this);
			fmMacApplication.setQuitHandler(this);
			// wait until handleOpenFile finishes if it was called in event thread
			try {
				EventQueue.invokeAndWait(new Runnable() {
					@Override
					public void run() {
					};
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	private MModeController getModeController() {
		return (MModeController) controller.getModeController(MModeController.MODENAME);
	}


	@Override
	public void handleQuitRequestWith(QuitEvent event, QuitResponse response) {
		try {
			if(! isStarting())
				Controller.getCurrentController().quit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		response.cancelQuit();
	}


	@Override
	public void openURI(OpenURIEvent event) {
		URI uri = event.getURI();

		try {
			if(isStarting()) {
				// restore at startup:
			    MacOptions.macFilesToOpen.add(uri.toString());
			} else {
				// Direct loading
				LinkController.getController().loadHyperlink(new Hyperlink(uri));
			}
		} catch (Exception e) {
			LogUtils.warn(e);
		}
	}


	@Override
	public void handlePreferences(PreferencesEvent event) {
		SwingUtilities.invokeLater(this::showPreferences);
	}

	private void showPreferences() {
		final MModeController modeController = getModeController();
		if(modeController != null) {
			AFreeplaneAction action = modeController.getAction("ShowPreferencesAction");
			if(action == null)
				return;
			Component menuComponent = UITools.getMenuComponent();
			if(menuComponent == null || ! menuComponent.isShowing())
				return;
			action.actionPerformed(null);
		}
	}


	@Override
	public void openFiles(OpenFilesEvent event) {
		for(File file : event.getFiles()){
			String filePath = file.getPath();
			openFile(filePath);
		}
	}

	private void openFile(String filePath) {
		try {
			if(isStarting()) {
				// restore at startup:
                MacOptions.macFilesToOpen.add(filePath);

			} else {
				// Direct loading
				getModeController().getMapController().openMap(Compat.fileToUrl(new File(filePath)));
			}
		} catch (Exception e) {
			LogUtils.warn(e);
		}
	}

	private boolean isStarting() {
		return controller.getViewController() == null;
	}


	@Override
	public void handleAbout(AboutEvent event) {
		final MModeController modeController = getModeController();
		if(modeController != null) {
			AFreeplaneAction action = modeController.getController().getAction("AboutAction");
			if(action != null)
				action.actionPerformed(null);
		}
	}
}
