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
package org.freeplane.features.ui;

import java.awt.Component;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.freeplane.core.ui.components.FreeplaneMenuBar;
import org.freeplane.core.util.Hyperlink;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;

/**
 * @author Dimitry Polivaev
 * 24.12.2012
 */
public interface ViewController {
	public static final String FULLSCREEN_ENABLED_PROPERTY = "fullscreen_enabled";
	public static final String STANDARD_STATUS_INFO_KEY = "standard";
	public static final int BOTTOM = 3;
	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int TOP = 0;

	public void changeNoteWindowLocation();

	public void err(final String msg);

	public FreeplaneMenuBar getFreeplaneMenuBar();

	/**
	 */
	public JComponent getStatusBar();

	public void init(Controller controller);

	public void insertComponentIntoSplitPane(JComponent noteViewerComponent);

	public boolean isMenubarVisible();

	boolean isFullScreenEnabled();

	public void openDocument(Hyperlink link) throws IOException;

	public void openDocument(URL url) throws Exception;

	public void out(final String msg);

	public void addStatusInfo(final String key, final String info);

	public void addStatusInfo(final String key, Icon icon);

	public void addStatusInfo(final String key, final String info, Icon icon);

	public void addStatusInfo(final String key, final String info, Icon icon, final String tooltip);

	public void addStatusComponent(final String key, Component component);

	public void removeStatus(final String key);

	/**
	 *
	 */
	public void removeSplitPane();

	public void saveProperties();

	public void selectMode(final ModeController oldModeController, final ModeController newModeController);

	public void setMenubarVisible(final boolean visible);

	/**
	 * Set the Frame title with mode and file if exist
	 */
	public void setTitle(String frameTitle);

	/**
	 * @param b
	 */
	public void setWaitingCursor(boolean b);

	public void viewNumberChanged(final int number);

	public void addObjectTypeInfo(Object value);

	public boolean quit();

	public boolean isDispatchThread();

	public void invokeLater(Runnable runnable);

	public ExecutorService getMainThreadExecutorService();

	public void invokeAndWait(Runnable runnable) throws InterruptedException, InvocationTargetException;

	public boolean isMapOverviewVisible();

	public void setMapOverviewVisible(boolean b);

	public boolean isBookmarksToolbarVisible();

	public void setBookmarksToolbarVisible(boolean b);

	public boolean areScrollbarsVisible();

	public void setScrollbarsVisible(boolean b);

	public void previousMapView();

	public void nextMapView();
	public Component getCurrentRootComponent();
	public Component getMenuComponent();
	public List<? extends Component> getMapViewVector();

	public void openMapNextView();

	public void openMapPreviousView();
}
