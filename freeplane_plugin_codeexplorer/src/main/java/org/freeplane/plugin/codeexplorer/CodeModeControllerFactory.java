package org.freeplane.plugin.codeexplorer;

import java.awt.Component;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.ui.components.resizer.UIComponentVisibilityDispatcher;
import org.freeplane.core.ui.menubuilders.generic.Entry;
import org.freeplane.core.ui.menubuilders.generic.PhaseProcessor.Phase;
import org.freeplane.core.ui.menubuilders.menu.ComponentProvider;
import org.freeplane.core.ui.menubuilders.menu.JToolbarComponentBuilder;
import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.attribute.mindmapmode.AddAttributeAction;
import org.freeplane.features.attribute.mindmapmode.MAttributeController;
import org.freeplane.features.attribute.mindmapmode.RemoveAllAttributesAction;
import org.freeplane.features.attribute.mindmapmode.RemoveFirstAttributeAction;
import org.freeplane.features.attribute.mindmapmode.RemoveLastAttributeAction;
import org.freeplane.features.clipboard.ClipboardControllers;
import org.freeplane.features.clipboard.mindmapmode.MClipboardControllers;
import org.freeplane.features.cloud.CloudController;
import org.freeplane.features.edge.EdgeController;
import org.freeplane.features.export.mindmapmode.ExportController;
import org.freeplane.features.filter.FilterController;
import org.freeplane.features.layout.LayoutController;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.map.FoldingController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.IPropertyHandler;
import org.freeplane.features.nodelocation.LocationController;
import org.freeplane.features.nodestyle.NodeGeometryModel;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.spellchecker.mindmapmode.SpellCheckerController;
import org.freeplane.features.styles.LogicalStyleController;
import org.freeplane.features.styles.MapStyle;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.features.ui.ViewController;
import org.freeplane.features.url.UrlManager;
import org.freeplane.main.application.ApplicationResourceController;
import org.freeplane.main.application.LastOpenedList;
import org.freeplane.plugin.codeexplorer.archunit.ArchUnitServer;
import org.freeplane.plugin.codeexplorer.configurator.CodeProjectController;
import org.freeplane.plugin.codeexplorer.connectors.CodeLinkController;
import org.freeplane.plugin.codeexplorer.map.CodeMapController;
import org.freeplane.plugin.codeexplorer.map.CodeMapPersistenceManager;
import org.freeplane.view.swing.features.nodehistory.NodeHistory;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.attribute.EditAttributesAction;
import org.freeplane.view.swing.ui.UserInputListenerFactory;

import com.tngtech.archunit.ArchConfiguration;

public class CodeModeControllerFactory {
	static private CodeModeController modeController;

	static public CodeModeController createModeController(ExecutorService classImportService, ArchUnitServer archUnitServer) {
	    ((ApplicationResourceController)ResourceController.getResourceController()).registerResourceLoader(CodeModeController.class.getClassLoader());
	    ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
		final Controller controller = Controller.getCurrentController();
		modeController = new CodeModeController(controller);
		CodeProjectController projectController = new CodeProjectController(modeController, archUnitServer);
        modeController.addExtension(CodeProjectController.class, projectController);
		final UserInputListenerFactory userInputListenerFactory = new UserInputListenerFactory(modeController);
		modeController.setUserInputListenerFactory(userInputListenerFactory);
		controller.addModeController(modeController);
		controller.selectModeForBuild(modeController);
		ClipboardControllers.install(new MClipboardControllers());
		new CodeMapController(modeController, classImportService);
		UrlManager.install(new CodeMapPersistenceManager());
		MapIO.install(modeController);
		new CodeIconController(modeController).install(modeController);
		AttributeController.install(new MAttributeController(modeController));
		NodeStyleController.install(new NodeStyleController(modeController));
		EdgeController.install(new EdgeController(modeController));
		new MTextController(modeController).install(modeController);
		FoldingController.install(new FoldingController());
		LinkController.install(new CodeLinkController(modeController));
		CloudController.install(new CloudController(modeController));
		LocationController.install(new LocationController());
		LayoutController.install(new CodeLayoutController());
		LogicalStyleController.install(new LogicalStyleController(modeController));
		ExportController.install(new ExportController("/xml/ExportWithXSLT.xml"));
		MapStyle.install(true);
		NodeStyleController.getController().shapeHandlers.addGetter(new Integer(0), new IPropertyHandler<NodeGeometryModel, NodeModel>() {
		    @Override
			public NodeGeometryModel getProperty(final NodeModel node, LogicalStyleController.StyleOption option, final NodeGeometryModel currentValue) {
			    return NodeGeometryModel.FORK;
		    }
		});
		SpellCheckerController.install(modeController);
		modeController.addAction(new EditAttributesAction());
        modeController.addAction(new AddAttributeAction());
        modeController.addAction(new RemoveFirstAttributeAction());
        modeController.addAction(new RemoveLastAttributeAction());
        modeController.addAction(new RemoveAllAttributesAction());
		userInputListenerFactory.setNodePopupMenu(new JPopupMenu());
		final FreeplaneToolBar toolBar = new FreeplaneToolBar("main_toolbar", SwingConstants.HORIZONTAL);
		UIComponentVisibilityDispatcher.install(toolBar, "toolbarVisible");
		userInputListenerFactory.addToolBar("/main_toolbar", ViewController.TOP, toolBar);
		userInputListenerFactory.addToolBar("/filter_toolbar", FilterController.TOOLBAR_SIDE, FilterController.getCurrentFilterController().getFilterToolbar());
		userInputListenerFactory.addToolBar("/status", ViewController.BOTTOM, controller.getViewController().getStatusBar());
		modeController.addUiBuilder(Phase.UI, "main_toolbar_zoom", new JToolbarComponentBuilder(
		        new ComponentProvider() {
		            @Override
		            public Component createComponent(Entry entry) {
		                return controller.getMapViewManager().createZoomBox();
		            }
		        }));

		NodeHistory.install(modeController);

		controller.getMapViewManager().addMapViewChangeListener(new IMapViewChangeListener() {

		    @Override
		    public void afterViewCreated(Component oldView, Component newView) {
		        ((MapView)newView).setRepaintsViewOnSelectionChange(true);
		    }

		});
        LastOpenedList lastOpenedList = ((ApplicationResourceController)ResourceController.getResourceController()).getLastOpenedList();
        modeController.getMapController().addUIMapChangeListener(lastOpenedList);
        lastOpenedList.registerMenuContributor(modeController);
		controller.selectModeForBuild(modeController);
		modeController.updateMenus("/xml/codeexplorermodemenu.xml", Collections.singleton(""));
		controller.selectModeForBuild(null);

		return modeController;
	}
}
