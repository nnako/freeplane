package org.freeplane.features.filter.hidden;

import org.freeplane.core.enumeration.DefaultValueSupplier;
import org.freeplane.core.extension.IExtension;
import org.freeplane.features.map.NodeModel;

public enum NodeVisibility implements IExtension, DefaultValueSupplier<NodeVisibility>{
	HIDDEN;

	public static boolean isHidden(final NodeModel node) {
		return node.getExtension(NodeVisibility.class) == HIDDEN
				&& node.getMap().getRootNode().getExtension(NodeVisibilityConfiguration.class) != NodeVisibilityConfiguration.SHOW_HIDDEN_NODES;
	}
}
