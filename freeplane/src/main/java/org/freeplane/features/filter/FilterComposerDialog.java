package org.freeplane.features.filter;

import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.FilterConditionEditor.Variant;
import org.freeplane.features.filter.condition.ASelectableCondition;
import org.freeplane.features.styles.ConditionalStyleModel;

@SuppressWarnings("serial")
public class FilterComposerDialog extends AFilterComposerDialog{

	final private List<ASelectableCondition> conditions ;
	private FilterConditions model;

	public FilterComposerDialog(Variant variant) {
		this(variant, null);
    }

	public FilterComposerDialog(Variant variant,
			ConditionalStyleModel context) {
        super(TextUtils.getText("filter_dialog"), true, variant, context);
        conditions = new LinkedList<ASelectableCondition>();
	}

	protected FilterConditions createModel() {
		conditions.clear();
		initializeModel();
		return model;
    }

	protected void initializeModel() {
	    if(model == null){
			model = new FilterConditions(new DefaultComboBoxModel(), 0);
		}
    }

    @Override
    protected boolean isSelectionValid(int[] selectedIndices) {
        return selectedIndices.length == 1;
    }

    @Override
	protected void applyModel(FilterConditions model, int[] selectedIndices) {
		if(this.model != model || selectedIndices.length != 1)
			throw new IllegalArgumentException();
		conditions.clear();
		for(int i : selectedIndices){
			conditions.add((ASelectableCondition) model.getElementAt(i));
		}
    }

	public List<ASelectableCondition> getConditions() {
    	return conditions;
    }

	public void addCondition(ASelectableCondition value) {
		initializeModel();
		if (model.getIndexOf(value) == -1){
			model.addElement(value);
		}
    }


	public ASelectableCondition editCondition(ASelectableCondition value) {
		initializeModel();
		if(value != null)
			setSelectedItem(value);
	    show();
	    List<ASelectableCondition> conditions = getConditions();
	    if(isSuccess())
	    	return conditions.isEmpty() ? null : conditions.get(0);
	    return value;
	}
}
