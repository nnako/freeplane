/*
 * Created on 12 Jan 2025
 *
 * author dimitry
 */
package org.freeplane.features.map.mindmapmode;

import java.util.EnumMap;

import org.freeplane.features.map.NodeModel.Side;

public enum InsertionRelation {
	AS_CHILD, AS_SIBLING_BEFORE, AS_SIBLING_AFTER;
	static private final EnumMap<Side, InsertionRelation> bySide = new EnumMap<Side, InsertionRelation>(Side.class);
	static {
		bySide.put(Side.AS_SIBLING_BEFORE, AS_SIBLING_BEFORE);
		bySide.put(Side.AS_SIBLING_AFTER, AS_SIBLING_AFTER);
	}
	public static InsertionRelation bySide(Side side) {
		return bySide.getOrDefault(side, AS_CHILD);
	}
}
