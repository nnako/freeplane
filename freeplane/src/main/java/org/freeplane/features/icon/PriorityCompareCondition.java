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
package org.freeplane.features.icon;

import java.awt.FontMetrics;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import org.freeplane.core.io.xml.TreeXmlReader;
import org.freeplane.core.io.xml.TreeXmlWriter;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.condition.ASelectableCondition;
import org.freeplane.features.filter.condition.CompareConditionAdapter;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.styles.LogicalStyleController.StyleOption;
import org.freeplane.n3.nanoxml.XMLElement;

public class PriorityCompareCondition extends CompareConditionAdapter {
	static final String COMPARATION_RESULT = "COMPARATION_RESULT";
	static final String NAME = "priority_compare_condition";
	static final String SUCCEED = "SUCCEED";
	static final String VALUE = "VALUE";
	private static final IconStore STORE = IconStoreFactory.ICON_STORE;

	static ASelectableCondition load(final XMLElement element) {
		return new PriorityCompareCondition(element.getAttribute(PriorityCompareCondition.VALUE, null), Integer
		    .parseInt(element.getAttribute(PriorityCompareCondition.COMPARATION_RESULT, null)), TreeXmlReader
		    .xmlToBoolean(element.getAttribute(PriorityCompareCondition.SUCCEED, null)));
	}

	final private int comparationResult;
	final private boolean succeed;

	PriorityCompareCondition(final String value, final int comparationResult, final boolean succeed) {
		super(value, false, false, false);
		this.comparationResult = comparationResult;
		this.succeed = succeed;
	}

	public boolean isEqualityCondition()
	{
		return comparationResult == 0;
	}

	@Override
    protected List<Icon> createRenderedIcons(FontMetrics fontMetrics) {
		Icon icon = STORE.getMindIcon(getIconName()).getIcon();
		return Collections.singletonList(icon);
    }

	public boolean checkNode(final NodeModel node) {
		final Collection<NamedIcon> icons = IconController.getController().getIcons(node, StyleOption.FOR_UNSELECTED_NODE);
		for (final NamedIcon icon : icons) {
			final String iconName = icon.getName();
			if (iconName.length() != 6) {
				continue;
			}
			if (!iconName.startsWith("full-")) {
				continue;
			}
			if (iconName.charAt(5) < '0' || iconName.charAt(5) > '9') {
				continue;
			}
			final String prio = iconName.substring(5, 6);
			compareTo(prio);
			return isComparisonOK() &&  succeed == (getComparisonResult() == comparationResult);
		}
		return false;
	}

	@Override
	protected String createDescription() {
		final String priorityCondition = TextUtils.getText(PriorityConditionController.FILTER_PRIORITY);
		return super.createDescription(priorityCondition, comparationResult, succeed);
	}

	private String getIconName() {
		return "full-" + conditionValue().toString();
	}

	public void fillXML(final XMLElement child) {
		super.fillXML(child);
		child.setAttribute(PriorityCompareCondition.COMPARATION_RESULT, Integer.toString(comparationResult));
		child.setAttribute(PriorityCompareCondition.SUCCEED, TreeXmlWriter.BooleanToXml(succeed));
	}

	@Override
    protected String getName() {
	    return NAME;
    }
}
