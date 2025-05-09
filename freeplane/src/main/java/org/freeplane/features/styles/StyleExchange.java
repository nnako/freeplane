package org.freeplane.features.styles;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.undo.IActor;
import org.freeplane.core.undo.IUndoHandler;
import org.freeplane.features.attribute.AttributeRegistry;
import org.freeplane.features.edge.AutomaticEdgeColorHook;
import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;

class StyleExchange {
    private final MapModel sourceMap;
    private final MapModel targetMap;

    StyleExchange(MapModel sourceMap , final MapModel targetMap){
        this.sourceMap = sourceMap;
        this.targetMap = targetMap;

    }

    void replaceMapStylesAndAutomaticStyle() {
        final ModeController modeController = Controller.getCurrentModeController();
        final MapStyleModel oldStyleModel = targetMap.getRootNode().removeExtension(MapStyleModel.class);
        modeController.getExtension(MapStyle.class).onCreate(sourceMap);
        final MapStyleModel source = MapStyleModel.getExtension(sourceMap);
        source.setNonStyleUserPropertiesFrom(oldStyleModel);
        moveStyle(true);
        targetMap.getRootNode().getExtension(MapStyleModel.class).setProperty(MapStyleModel.FOLLOWED_TEMPLATE_LOCATION_PROPERTY,
                oldStyleModel.getProperty(MapStyleModel.FOLLOWED_TEMPLATE_LOCATION_PROPERTY));
        targetMap.getRootNode().getExtension(MapStyleModel.class).setProperty(MapStyleModel.ASSOCIATED_TEMPLATE_LOCATION_PROPERTY,
                oldStyleModel.getProperty(MapStyleModel.ASSOCIATED_TEMPLATE_LOCATION_PROPERTY));
        targetMap.getRootNode().getExtension(MapStyleModel.class).setProperty(MapStyleModel.FOLLOWED_MAP_LAST_TIME,
            	oldStyleModel.getProperty(MapStyleModel.FOLLOWED_MAP_LAST_TIME));
        modeController.getExtension(AutomaticLayoutController.class).moveExtension(modeController, sourceMap, targetMap);
        modeController.getExtension(AutomaticEdgeColorHook.class).moveExtension(modeController, sourceMap, targetMap);
        makeUndoableAndRefreshView(oldStyleModel);
    }

    void copyMapStyles(boolean mergeConditionalStyles) {
        final MapStyleModel oldStyleModel =  targetMap.getRootNode().getExtension(MapStyleModel.class);
        copyMapStylesNoUndoNoRefresh(mergeConditionalStyles);
        makeUndoableAndRefreshView(oldStyleModel);
    }

	void copyMapStylesNoUndoNoRefresh(boolean mergeConditionalStyles) {
		final ModeController modeController = Controller.getCurrentModeController();
        final MapStyleModel oldStyleModel = targetMap.getRootNode().removeExtension(MapStyleModel.class);
        modeController.getExtension(MapStyle.class).onCreate(sourceMap);
        final MapStyleModel source = MapStyleModel.getExtension(sourceMap);
        source.addUserStylesFrom(oldStyleModel);
        if(mergeConditionalStyles)
        	source.addConditionalStylesFrom(oldStyleModel);
        else
        	source.setConditionalStylesIfEmpty(oldStyleModel);
        source.setNonStyleUserPropertiesFrom(oldStyleModel);
        moveStyle(true);
        MapStyleModel styleModel = targetMap.getRootNode().getExtension(MapStyleModel.class);
        styleModel.setProperty(MapStyleModel.FOLLOWED_TEMPLATE_LOCATION_PROPERTY,
                oldStyleModel.getProperty(MapStyleModel.FOLLOWED_TEMPLATE_LOCATION_PROPERTY));
        styleModel.setProperty(MapStyleModel.ASSOCIATED_TEMPLATE_LOCATION_PROPERTY,
                oldStyleModel.getProperty(MapStyleModel.ASSOCIATED_TEMPLATE_LOCATION_PROPERTY));
        styleModel.setProperty(MapStyleModel.FOLLOWED_MAP_LAST_TIME,
        		oldStyleModel.getProperty(MapStyleModel.FOLLOWED_MAP_LAST_TIME));
	}

    private void makeUndoableAndRefreshView(final MapStyleModel oldStyleModel) {
        final MapStyleModel newStyleModel = targetMap.getRootNode().getExtension(MapStyleModel.class);
		IActor actor = new IActor() {
			@Override
			public void undo() {
				targetMap.getRootNode().putExtension(oldStyleModel);
		        LogicalStyleController.getController().refreshMapLaterUndoable(targetMap);
			}

			@Override
			public String getDescription() {
				return "moveStyle";
			}

			@Override
			public void act() {
			    targetMap.getRootNode().putExtension(newStyleModel);
			    LogicalStyleController.getController().refreshMapLaterUndoable(targetMap);
			}
		};
		Controller.getCurrentModeController().execute(actor, targetMap);
    }

    void moveStyle(boolean overwrite) {
    	final MapStyleModel source = sourceMap.getRootNode().removeExtension(MapStyleModel.class);
    	if(source == null)
    		return;
    	final IExtension undoHandler = targetMap.getExtension(IUndoHandler.class);
    	MapModel styleMap = source.getStyleMap();
        styleMap.putExtension(IUndoHandler.class, undoHandler);
        IconRegistry iconRegistry = targetMap.getIconRegistry();
        styleMap.setIconRegistry(iconRegistry);
        iconRegistry.registryMapContent(styleMap);
        AttributeRegistry attributeRegistry = targetMap.getExtension(AttributeRegistry.class);
        if(attributeRegistry != null) {
        	styleMap.putExtension(AttributeRegistry.class, attributeRegistry);
        }
        else {
        	styleMap.removeExtension(AttributeRegistry.class);
        }
    	final NodeModel targetRoot = targetMap.getRootNode();
    	final MapStyleModel target = MapStyleModel.getExtensionOrNull(targetRoot);
    	if(target == null){
    		targetRoot.addExtension(source);
    	}
    	else{
    		target.setStylesFrom(source, overwrite);
    	}
    }


}