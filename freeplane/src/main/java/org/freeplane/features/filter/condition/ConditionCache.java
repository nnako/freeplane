/*
 * Created on 5 Apr 2025
 *
 * author dimitry
 */
package org.freeplane.features.filter.condition;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.util.IdentityHashMap;

import javax.swing.SwingUtilities;

import org.freeplane.features.map.NodeModel;

class ConditionCache {
	static final ConditionCache INSTANCE = new ConditionCache();
	private IdentityHashMap<NodeModel, IdentityHashMap<ICondition, Boolean>> conditionCache = new IdentityHashMap<>();
	public boolean checkNode(NodeModel node, ICondition condition) {
		if(GraphicsEnvironment.isHeadless() || ! SwingUtilities.isEventDispatchThread())
			return condition.checkNode(node);
		if(conditionCache.isEmpty())
			EventQueue.invokeLater(conditionCache::clear);
		return conditionCache.computeIfAbsent(node, x -> new IdentityHashMap<>())
				.computeIfAbsent(condition, x -> x.checkNode(node));
	}
}
