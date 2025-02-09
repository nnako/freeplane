/*
 * Created on 8 Feb 2025
 *
 * author dimitry
 */
package org.freeplane.features.filter;

import javax.swing.DefaultComboBoxModel;

import org.freeplane.features.filter.condition.ASelectableCondition;

public class FilterConditions {
	private DefaultComboBoxModel<ASelectableCondition> conditions;
	private int pinnedConditionsCount;
	public FilterConditions(DefaultComboBoxModel<ASelectableCondition> condifions, int pinnedConditionsCount) {
		super();
		this.conditions = condifions;
		this.pinnedConditionsCount = pinnedConditionsCount;
	}
	public DefaultComboBoxModel<ASelectableCondition> getConditions() {
		return conditions;
	}
	public int getPinnedConditionsCount() {
		return pinnedConditionsCount;
	}
	public ASelectableCondition getSelectedItem() {
		return (ASelectableCondition) conditions.getSelectedItem();
	}
	public void setSelectedItem(Object anObject) {
		conditions.setSelectedItem(anObject);
	}
	public void addElement(ASelectableCondition anObject) {
		conditions.addElement(anObject);
	}
	public void insertElementAt(ASelectableCondition anObject, int index) {
		conditions.insertElementAt(anObject, index);
	}
	public ASelectableCondition getElementAt(int i) {
		return conditions.getElementAt(i);
	}
	public void removeElementAt(int index) {
		conditions.removeElementAt(index);
	}
	public void removeAllElements() {
		conditions.removeAllElements();
	}
	public int getSize() {
		return conditions.getSize();
	}
	public int getIndexOf(Object anObject) {
		return conditions.getIndexOf(anObject);
	}
	public void setPinnedConditionsCount(int pinnedConditionsCount) {
		this.pinnedConditionsCount = pinnedConditionsCount;
	}
}
