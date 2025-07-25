/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is created by Dimitry Polivaev in 2008.
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
package org.freeplane.main.application;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.FreeplaneMenuBar;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.Hyperlink;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.ui.FrameController;
import org.freeplane.features.ui.IMapViewManager;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.overview.BookmarkToolbarPane;

class ApplicationViewController extends FrameController {
	private static final String SPLIT_PANE_LAST_LEFT_POSITION = "split_pane_last_left_position";
	private static final String SPLIT_PANE_LAST_POSITION = "split_pane_last_position";
	private static final String SPLIT_PANE_LAST_RIGHT_POSITION = "split_pane_last_right_position";
	private static final String SPLIT_PANE_LAST_TOP_POSITION = "split_pane_last_top_position";


    private static Image frameIcon(String size) {
        return new ImageIcon(ResourceController.getResourceController().getResource(
                "/images/Freeplane_frame_icon_"+ size + ".png")).getImage();
    }

	// // 	final private Controller controller;
	final private JFrame frame;
	/** Contains the value where the Note Window should be displayed (right, left, top, bottom) */
	private String mLocationPreferenceValue;
	/** Contains the Note Window Component */
	private JComponent mMindMapComponent;
	private JSplitPane mSplitPane;
	final private NavigationNextMapAction navigationNextMap;
	final private NavigationPreviousMapAction navigationPreviousMap;
	final private ApplicationResourceController resourceController;
	private MapViewDockingWindows mapViewWindows;
	private BookmarkToolbarPane mainBookmarkToolbarPane;
    public ApplicationViewController( Controller controller, final IMapViewManager mapViewController,
	                                 final JFrame frame) {
		super(controller, mapViewController, "");
//		this.controller = controller;
		navigationPreviousMap = new NavigationPreviousMapAction();
		controller.addAction(navigationPreviousMap);
		navigationNextMap = new NavigationNextMapAction();
		controller.addAction(navigationNextMap);
		controller.addAction(new NavigationMapNextViewAction());
		controller.addAction(new NavigationMapPreviousViewAction());
		resourceController = (ApplicationResourceController) ResourceController.getResourceController();
		this.frame = frame;
	}

	/**
	 * Called from the Controller, when the Location of the Note Window is changed on the Menu->View->Note Window Location
	 */
	@Override
	public void changeNoteWindowLocation() {
		saveSplitPanePosition();
		mLocationPreferenceValue = resourceController.getProperty("note_location");
		if(mMindMapComponent != null){
			insertComponentIntoSplitPane(mMindMapComponent);
		}
	}

	public String getAdjustableProperty(final String label) {
		return resourceController.getProperty(label);
	}

	@Override
	public FreeplaneMenuBar getFreeplaneMenuBar() {
		return Controller.getCurrentModeController().getUserInputListenerFactory().getMenuBar();
	}

	@Override
	public void insertComponentIntoSplitPane(final JComponent pMindMapComponent) {
		// --- Save the Component --
		mMindMapComponent = pMindMapComponent;
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		mSplitPane.setLeftComponent(null);
		mSplitPane.setRightComponent(null);
		if ("right".equals(mLocationPreferenceValue)) {
			mSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			mSplitPane.setLeftComponent(mainBookmarkToolbarPane);
			mSplitPane.setRightComponent(pMindMapComponent);
		}
		else if ("left".equals(mLocationPreferenceValue)) {
			mSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			mSplitPane.setLeftComponent(pMindMapComponent);
			mSplitPane.setRightComponent(mainBookmarkToolbarPane);
		}
		else if ("top".equals(mLocationPreferenceValue)) {
			mSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
			mSplitPane.setLeftComponent(pMindMapComponent);
			mSplitPane.setRightComponent(mainBookmarkToolbarPane);
		}
		else {
			mSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
			mSplitPane.setLeftComponent(mainBookmarkToolbarPane);
			mSplitPane.setRightComponent(pMindMapComponent);
		}
		if(focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, mSplitPane)) {
		    focusOwner.requestFocusInWindow();
		}
		mSplitPane.setContinuousLayout(true);
		mSplitPane.setOneTouchExpandable(false);
		SwingUtilities.invokeLater(this::resetDividerLocation);

	}
	private void resetDividerLocation() {
		int lastSplitPanePosition = -1;
		if ("right".equals(mLocationPreferenceValue)) {
			lastSplitPanePosition = resourceController.getIntProperty(SPLIT_PANE_LAST_RIGHT_POSITION, -1);
		}
		else if ("left".equals(mLocationPreferenceValue)) {
			lastSplitPanePosition = resourceController.getIntProperty(SPLIT_PANE_LAST_LEFT_POSITION, -1);
		}
		else if ("top".equals(mLocationPreferenceValue)) {
			lastSplitPanePosition = resourceController.getIntProperty(SPLIT_PANE_LAST_TOP_POSITION, -1);
		}
		else if ("bottom".equals(mLocationPreferenceValue)) {
			lastSplitPanePosition = resourceController.getIntProperty(SPLIT_PANE_LAST_POSITION, -1);
		}

		if (lastSplitPanePosition != -1) {
			mSplitPane.setDividerLocation(lastSplitPanePosition);
			mSplitPane.setDividerLocation(lastSplitPanePosition);
		}
		else {
			mSplitPane.setDividerLocation(0.5);
			mSplitPane.setDividerLocation(0.5);
		}
	}

	@Override
	public void openDocument(final Hyperlink link) throws IOException {
		new Browser().openDocument(link);
	}
/**
	 * Open url in WWW browser. This method hides some differences between
	 * operating systems.
	 */
	@Override
	public void openDocument(final URL url) throws Exception {
		URI uri = null;
		try {
			uri = url.toURI();
		}
		catch (URISyntaxException e) {
			uri = new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getQuery(), url.getRef());
		}
		openDocument(new Hyperlink(uri));
	}

	@Override
	public boolean quit() {
		if (!super.quit()) {
			return false;
		}
		controller.fireApplicationStopped();
		frame.dispose();
		return true;
	}

	@Override
	public void removeSplitPane() {
		saveSplitPanePosition();
		mMindMapComponent = null;
		mSplitPane.setLeftComponent(null);
		mSplitPane.setRightComponent(null);
		mSplitPane.setLeftComponent(mainBookmarkToolbarPane);
		final Controller controller = Controller.getCurrentModeController().getController();
		final IMapSelection selection = controller.getSelection();
		if(selection == null){
			return;
		}
		final NodeModel node = selection.getSelected();
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				final Component component = controller.getMapViewManager().getComponent(node);
				if (component != null) {
					component.requestFocus();
				}
			}
		});
	}

	@Override
	public void saveProperties() {
		if(mapViewWindows == null)
			return;
		saveSplitPanePosition();
		if (frame.isResizable()) {
			final int winState = frame.getExtendedState() & ~Frame.ICONIFIED;
			if (JFrame.MAXIMIZED_BOTH != (winState & JFrame.MAXIMIZED_BOTH)) {
				resourceController.setProperty("appwindow_x", String.valueOf(frame.getX()));
				resourceController.setProperty("appwindow_y", String.valueOf(frame.getY()));
				resourceController.setProperty("appwindow_width", String.valueOf(frame.getWidth()));
				resourceController.setProperty("appwindow_height", String.valueOf(frame.getHeight()));
			}
			resourceController.setProperty("appwindow_state", String.valueOf(winState));
		}
		mapViewWindows.saveLayout();
		resourceController.getLastOpenedList().saveProperties();
	}

	private void saveSplitPanePosition() {
		if (mSplitPane == null) {
			return;
		}
		if ("right".equals(mLocationPreferenceValue)) {
			resourceController.setProperty(SPLIT_PANE_LAST_RIGHT_POSITION, "" + mSplitPane.getLastDividerLocation());
		}
		else if ("left".equals(mLocationPreferenceValue)) {
			resourceController.setProperty(SPLIT_PANE_LAST_LEFT_POSITION, "" + mSplitPane.getLastDividerLocation());
		}
		else if ("top".equals(mLocationPreferenceValue)) {
			resourceController.setProperty(SPLIT_PANE_LAST_TOP_POSITION, "" + mSplitPane.getLastDividerLocation());
		}
		else { // "bottom".equals(mLocationPreferenceValue) also covered
			resourceController.setProperty(SPLIT_PANE_LAST_POSITION, "" + mSplitPane.getLastDividerLocation());
		}
	}

	@Override
	protected void setFreeplaneMenuBar(final FreeplaneMenuBar menuBar) {
	    if(Compat.isMacOsX()) {
	        System.setProperty("apple.laf.useScreenMenuBar", "true");
            frame.setJMenuBar(menuBar);
            System.setProperty("apple.laf.useScreenMenuBar", "false");
        }
	    else
	        frame.setJMenuBar(menuBar);
	}

	/*
	 * (non-Javadoc)
	 * @see freeplane.main.FreeplaneMain#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(final String frameTitle) {
		frame.setTitle(frameTitle);
		mapViewWindows.setTitle();
	}

	@Override
	public void setWaitingCursor(final boolean waiting) {
		if (waiting) {
			frame.getRootPane().getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			frame.getRootPane().getGlassPane().setVisible(true);
		}
		else {
			frame.getRootPane().getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			frame.getRootPane().getGlassPane().setVisible(false);
		}
	}

	@Override
    public void viewNumberChanged(final int number) {
		navigationPreviousMap.setEnabled(number > 1);
		navigationNextMap.setEnabled(number > 1);
	}



	@Override
	public void init(Controller controller) {
		frame.getContentPane().setLayout(new BorderLayout());
		// --- Set Note Window Location ---
		mLocationPreferenceValue = resourceController.getProperty("note_location", "bottom");
		// disable all hotkeys for JSplitPane
		mSplitPane = new JSplitPane(){
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed){
				return false;
			}

			@Override
			public void setLayout(LayoutManager layout) {
				if(layout == null || layout instanceof SplitPaneLayoutManagerDecorator)
					super.setLayout(layout);
				else if(layout instanceof LayoutManager2)
					super.setLayout(new SplitPaneLayoutManager2Decorator((LayoutManager2) layout));
				else
					super.setLayout(new SplitPaneLayoutManagerDecorator(layout));
			}

		};
		mSplitPane.setResizeWeight(1.0d);
		mapViewWindows = new MapViewDockingWindows();
		mainBookmarkToolbarPane = new BookmarkToolbarPane(mapViewWindows.getRootWindow());
		Container contentPane = frame.getContentPane();
		contentPane.setLayout(new BorderLayoutWithVisibleCenterComponent());
        contentPane.add(mSplitPane, BorderLayout.CENTER);
		mSplitPane.setLeftComponent(mainBookmarkToolbarPane);
		mSplitPane.setRightComponent(null);
		initFrame(frame);
		super.init(controller);
	}

	private void initFrame(final JFrame frame) {
		// Preserve the existing icon image under Mac OS X
		if (!Compat.isMacOsX()) {
			    frame.setIconImages(Arrays.asList(
                        frameIcon("16x16"),
                        frameIcon("32x32"),
                        frameIcon("64x64"),
                        frameIcon("128x128"),
                        frameIcon("256x256"),
                        frameIcon("512x512")
			            ));
		}
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				Controller.getCurrentController().quit();
			}
			/*
			 * fc, 14.3.2008: Completely removed, as it damaged the focus if for
			 * example the note window was active.
			 */
		});
		frame.setFocusTraversalKeysEnabled(false);
		final int win_width = ResourceController.getResourceController().getIntProperty("appwindow_width", -1);
		final int win_height = ResourceController.getResourceController().getIntProperty("appwindow_height", -1);
		final int win_x = ResourceController.getResourceController().getIntProperty("appwindow_x", -1);
		final int win_y = ResourceController.getResourceController().getIntProperty("appwindow_y", -1);
		final Rectangle frameBounds = UITools.getValidFrameBounds(frame, win_x, win_y, win_width, win_height);
        frame.setBounds(frameBounds);
		frame.applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));


		int win_state = Integer
		    .parseInt(ResourceController.getResourceController().getProperty("appwindow_state", "0"));
		win_state = ((win_state & Frame.ICONIFIED) != 0) ? Frame.NORMAL : win_state;
		frame.setExtendedState(win_state);
	}


	public void openMapsOnStart() {
	    mapViewWindows.loadLayout();
    }

	public void focusTo(MapView currentMapView, Runnable onFocus) {
	    mapViewWindows.focusMapViewLater(currentMapView, onFocus);
    }

	@Override
	public void previousMapView() {
		mapViewWindows.selectPreviousMapView();

	}

	@Override
	public void nextMapView() {
		mapViewWindows.selectNextMapView();
	}

	@Override
	public void setFullScreen(boolean fullScreen) {
		super.setFullScreen(fullScreen);
		if(fullScreen)
			mapViewWindows.setTabAreaInvisiblePolicy((JFrame) UITools.getCurrentRootComponent());
		else
			mapViewWindows.setTabAreaVisiblePolicy((JFrame)UITools.getCurrentRootComponent());
	}

	@Override
	public Component getCurrentRootComponent() {
		final Component mapViewComponent = selectedMapView();
		return mapViewComponent != null ? SwingUtilities.getRoot(mapViewComponent) : frame;
	}

	@Override
	public Component getMenuComponent() {
		return frame;
	}

	@Override
	public List<? extends Component> getMapViewVector() {
		return mapViewWindows != null ? mapViewWindows.getMapViewVector() : null;
	}

	@Override
	public void openMapNextView() {
		mapViewWindows.selectMapNextView(selectedMapView());
	}

	@Override
	public void openMapPreviousView() {
		mapViewWindows.selectMapPreviousView(selectedMapView());
	}

	private  JComponent selectedMapView() {
		return controller.getMapViewManager().getMapViewComponent();
	}
}
