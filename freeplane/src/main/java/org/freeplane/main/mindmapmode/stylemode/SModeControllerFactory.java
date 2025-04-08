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
package org.freeplane.main.mindmapmode.stylemode;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.util.Collections;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.SetAcceleratorOnNextClickAction;
import org.freeplane.core.ui.ShowSelectionAsRectangleAction;
import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.ui.components.resizer.CollapseableBoxBuilder;
import org.freeplane.core.ui.components.resizer.UIComponentVisibilityDispatcher;
import org.freeplane.core.ui.components.resizer.JResizer.Direction;
import org.freeplane.core.ui.menubuilders.action.ComponentBuilder;
import org.freeplane.core.ui.menubuilders.generic.Entry;
import org.freeplane.core.ui.menubuilders.generic.EntryVisitor;
import org.freeplane.core.ui.menubuilders.generic.PhaseProcessor.Phase;
import org.freeplane.core.ui.menubuilders.menu.ComponentProvider;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.attribute.ModelessAttributeController;
import org.freeplane.features.attribute.mindmapmode.MAttributeController;
import org.freeplane.features.clipboard.ClipboardControllers;
import org.freeplane.features.clipboard.mindmapmode.MClipboardControllers;
import org.freeplane.features.cloud.CloudController;
import org.freeplane.features.cloud.mindmapmode.MCloudController;
import org.freeplane.features.edge.EdgeController;
import org.freeplane.features.edge.mindmapmode.MEdgeController;
import org.freeplane.features.filter.FilterController;
import org.freeplane.features.format.FormatController;
import org.freeplane.features.format.ScannerController;
import org.freeplane.features.highlight.HighlightController;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.layout.LayoutController;
import org.freeplane.features.layout.mindmapmode.MLayoutController;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.INodeChangeListener;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodelocation.LocationController;
import org.freeplane.features.nodelocation.mindmapmode.MLocationController;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;
import org.freeplane.features.note.NoteController;
import org.freeplane.features.note.mindmapmode.MNoteController;
import org.freeplane.features.spellchecker.mindmapmode.SpellCheckerController;
import org.freeplane.features.styles.LogicalStyleController;
import org.freeplane.features.styles.MapStyle;
import org.freeplane.features.styles.MapStyleModel;
import org.freeplane.features.styles.MapViewLayout;
import org.freeplane.features.styles.mindmapmode.MLogicalStyleController;
import org.freeplane.features.styles.mindmapmode.MUIFactory;
import org.freeplane.features.styles.mindmapmode.ShowFormatPanelAction;
import org.freeplane.features.styles.mindmapmode.styleeditorpanel.StyleEditorPanel;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.features.ui.ToggleToolbarAction;
import org.freeplane.features.ui.ViewController;
import org.freeplane.features.url.mindmapmode.MFileManager;
import org.freeplane.main.mindmapmode.stylemode.ExtensionInstaller.Context;
import org.freeplane.view.swing.map.MapViewController;
import org.freeplane.view.swing.map.ViewLayoutTypeAction;
import org.freeplane.view.swing.map.attribute.EditAttributesAction;
import org.freeplane.view.swing.map.mindmapmode.MMapViewController;
import org.freeplane.view.swing.ui.DefaultNodeMouseMotionListener;
import org.freeplane.view.swing.ui.UserInputListenerFactory;
import org.freeplane.view.swing.ui.mindmapmode.MMapMouseListener;
import org.freeplane.view.swing.ui.mindmapmode.MNodeMouseWheelListener;

/**
 * @author Dimitry Polivaev 24.11.2008
 */
public class SModeControllerFactory {
	private static SModeControllerFactory instance;

	public static SModeControllerFactory getInstance() {
		if (instance == null) {
			instance = new SModeControllerFactory();
		}
		return instance;
	}

	private SModeController modeController;
	private ExtensionInstaller extentionInstaller;

	Controller createController(final JDialog dialog) {
		Controller currentController = Controller.getCurrentController();
		final Controller controller = new Controller(ResourceController.getResourceController()) {

		    @Override
            public void quit() {
		        if(((SModeController) getModeController()).tryToCloseDialog())
		            SwingUtilities.invokeLater(currentController::quit);
		    }

		};
		Controller.setCurrentController(controller);
		final MapViewController mapViewController = new MMapViewController(controller);
		final DialogController viewController = new DialogController(controller, mapViewController, dialog);
		controller.setViewController(viewController);
		controller.addExtension(HighlightController.class, new HighlightController());
		controller.addAction(currentController.getAction("AboutAction"));
		FilterController.install();
		TextController.install();
		controller.addAction(new ViewLayoutTypeAction(MapViewLayout.OUTLINE));
		controller.addAction(new ShowSelectionAsRectangleAction());
		modeController = new SModeController(controller);
		modeController.createOptionPanelControls();
		controller.selectModeForBuild(modeController);
		ClipboardControllers.install(new MClipboardControllers());
        modeController.addAction(new NewUserStyleAction(false));
        modeController.addAction(new NewUserStyleAction(true));
		modeController.addAction(new DeleteUserStyleAction());
		modeController.addAction(new NewLevelStyleAction());
		modeController.addAction(new DeleteLevelStyleAction());
		modeController.addAction(new SetAcceleratorOnNextClickAction());
		final UserInputListenerFactory userInputListenerFactory = new UserInputListenerFactory(modeController);
		userInputListenerFactory.setNodeMouseMotionListener(new DefaultNodeMouseMotionListener());
		userInputListenerFactory.setNodeMouseWheelListener(
		    new MNodeMouseWheelListener(userInputListenerFactory.getMapMouseWheelListener()));
		modeController.setUserInputListenerFactory(userInputListenerFactory);
		controller.addExtension(ModelessAttributeController.class, new ModelessAttributeController());
		MMapController mapController = new MMapController(modeController);
		ModelessAttributeController.installConditions();
		new MTextController(modeController).install(modeController);
		SpellCheckerController.install(modeController);
		new MIconController(modeController).install(modeController);
		NodeStyleController.install(new MNodeStyleController(modeController));
		LocationController.install(new MLocationController());
		LayoutController.install(new MLayoutController());
		EdgeController.install(new MEdgeController(modeController));
		CloudController.install(new MCloudController(modeController));
		NoteController.install(new MNoteController(modeController));
		LinkController.install(new MLinkController(modeController));
		MFileManager.install(new MFileManager(mapController));
		final MLogicalStyleController logicalStyleController = new MLogicalStyleController(modeController);
		logicalStyleController.initS();
		LogicalStyleController.install(logicalStyleController);
		AttributeController.install(new MAttributeController(modeController));
		FormatController.install(new FormatController());
		IconController.installConditionControllers();
		final ScannerController scannerController = new ScannerController();
		ScannerController.install(scannerController);
		scannerController.addParsersForStandardFormats();
		modeController.addAction(new EditAttributesAction());
		userInputListenerFactory.setMapMouseListener(new MMapMouseListener());
		final JPopupMenu popupmenu = new JPopupMenu();
		userInputListenerFactory.setNodePopupMenu(popupmenu);
		final FreeplaneToolBar toolBar = new FreeplaneToolBar("main_toolbar", SwingConstants.HORIZONTAL);
		UIComponentVisibilityDispatcher.install(toolBar, "dialog_toolbarVisible");
		userInputListenerFactory.addToolBar("/main_toolbar", ViewController.TOP, toolBar);
		userInputListenerFactory.addToolBar("/icon_toolbar", ViewController.LEFT, ((MIconController) IconController
		    .getController()).getIconToolBarScrollPane());
		userInputListenerFactory.addToolBar("/status", ViewController.BOTTOM, controller.getViewController()
		    .getStatusBar());
		Box closingButtons = Box.createHorizontalBox();
		closingButtons.add(Box.createHorizontalGlue());
		JButton okButton = TranslatedElementFactory.createButton(controller.getAction("styledialog.ok"), "styledialog.ok.text");
		closingButtons.add(okButton);
		closingButtons.add(Box.createHorizontalStrut(10));
		JButton cancelButton = TranslatedElementFactory.createButton(controller.getAction("styledialog.cancel"), "styledialog.cancel.text");
		closingButtons.add(cancelButton);
		closingButtons.add(Box.createHorizontalGlue());
		userInputListenerFactory.addToolBar("/closingbuttons", ViewController.BOTTOM, closingButtons);
		modeController.addAction(new ToggleToolbarAction("ToggleLeftToolbarAction", "/icon_toolbar"));
		MapStyle.install(false);
		controller.addModeController(modeController);
		controller.selectModeForBuild(modeController);
		if (extentionInstaller != null)
			extentionInstaller.installExtensions(controller, Context.STYLE);
		final SModeController modeController = this.modeController;
		final StyleEditorPanel styleEditorPanel = new StyleEditorPanel(modeController, null, false);
		modeController.addAction(new ShowFormatPanelAction());
		mapController.addNodeSelectionListener(new INodeSelectionListener() {
			@Override
			public void onSelect(final NodeModel node) {
				final IMapSelection selection = controller.getSelection();
				if (selection == null) {
					return;
				}
				if (selection.size() == 1 && node.depth() >= 2) {
					return;
				}
				final NodeModel nextSelection;
				if (node.depth() < 2) {
					if (node.depth() == 1 && node.hasChildren()) {
						nextSelection = node.getChildAt(0);
					}
					else {
						nextSelection = (node.getMap().getRootNode().getChildAt(0).getChildAt(0));
					}
				}
				else {
					nextSelection = node;
				}
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						selection.selectAsTheOnlyOneSelected(nextSelection);
					}
				});
			}

			@Override
			public void onDeselect(final NodeModel node) {
			}
		});
		mapController.addUINodeChangeListener(new INodeChangeListener() {
			@Override
			public void nodeChanged(NodeChangeEvent event) {
				final NodeModel node = event.getNode();
				if (MapStyleModel.isPredefinedStyleNode(node)) {
					mapController
					    .fireMapChanged(new MapChangeEvent(this, node.getMap(), MapStyle.MAP_STYLES, null, null));
				}
			}
		});
		final JScrollPane styleScrollPane = new JScrollPane(styleEditorPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		UITools.setScrollbarIncrement(styleScrollPane);
		//		styleEditorPanel.setPreferredSize(new Dimension(200, 200));
		Box resisableTabs = new CollapseableBoxBuilder("dialog_styleScrollPaneVisible").createBox(styleScrollPane, Direction.RIGHT);
		userInputListenerFactory.addToolBar("/format", ViewController.RIGHT, resisableTabs);
		modeController.addExtension(MUIFactory.class, new MUIFactory());
		final Set<String> emptySet = Collections.emptySet();
		MUIFactory uiFactory = new MUIFactory();
		mapController.addUINodeChangeListener(uiFactory);
		mapController.addNodeSelectionListener(uiFactory);

		modeController.addUiBuilder(Phase.ACTIONS, "main_toolbar_font_name", new ComponentBuilder(
	              new ComponentProvider() {
	                  @Override
	                  public Component createComponent(Entry entry) {
	                      final Container fontBox = uiFactory.createFontBox();
	                      return fontBox;
	                  }
	              }), EntryVisitor.EMTPY);


	          modeController.addUiBuilder(Phase.ACTIONS, "main_toolbar_font_size", new ComponentBuilder(
	              new ComponentProvider() {
	                  @Override
	                  public Component createComponent(Entry entry) {
	                      return uiFactory.createSizeBox();
	                  }
	              }), EntryVisitor.EMTPY);


		modeController.updateMenus("/xml/stylemodemenu.xml", emptySet);
		this.modeController = null;
		return controller;
	}

	public static void install() {
		ModeController modeController = Controller.getCurrentModeController();
		modeController.addAction(new EditStylesAction());
	}

	public void setExtensionInstaller(ExtensionInstaller extentionInstaller) {
		this.extentionInstaller = extentionInstaller;
	}
}
