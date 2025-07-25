/*
 * Created on 11 Sep 2022
 *
 * author dimitry
 */
package org.freeplane.view.swing.map;

import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedList;

import org.freeplane.api.ChildrenSides;
import org.freeplane.api.ChildNodesAlignment;
import org.freeplane.features.map.NodeModel;

class NodeViewLayoutHelper {

	private NodeView view;
	private int topOverlap;
	private int bottomOverlap;
	private StepFunction topBoundary;
	private StepFunction bottomBoundary;
	private int minimumContentWidth = ContentSizeCalculator.UNSET;

	NodeViewLayoutHelper(NodeView view) {
		this.view = view;
	}

	Dimension calculateContentSize() {
		Dimension contentSize = ContentSizeCalculator.INSTANCE.calculateContentSize(view, minimumContentWidth);
		return usesHorizontallayout(view.getContent()) ? new Dimension(contentSize.height, contentSize.width) : contentSize;
	}

	int getAdditionalCloudHeight() {
		return CloudHeightCalculator.INSTANCE.getAdditionalCloudHeight(view);
	}

	int getComponentCount() {
		return view.getComponentCount();
	}

	NodeViewLayoutHelper getComponent(int n) {
		Component component = view.getComponent(n);
		return component instanceof NodeView ? ((NodeView) component).getLayoutHelper() : null;
	}

	MapView getMap() {
		return view.getMap();
	}

	NodeModel getNode() {
		return view.getNode();
	}

	NodeView getView() {
		return view;
	}

    int getMinimalDistanceBetweenChildren() {
        return view.getMinimalDistanceBetweenChildren();
    }

    int getBaseDistanceToChildren(int dx) {
        return view.getBaseDistanceToChildren(dx);
    }

 	int getSpaceAround() {
		return view.getSpaceAround();
	}

	ChildNodesAlignment getChildNodesAlignment() {
		return view.getChildNodesAlignment();
	}

	int getContentX() {
        Component component = view.getContent();
        return usesHorizontallayout(view) ? component.getY(): component.getX();
	}

	int getContentY() {
        Component component = view.getContent();
        return usesHorizontallayout(view) ? component.getX(): component.getY();
	}


    int getContentWidth() {
		Component component = view.getContent();
        return usesHorizontallayout(view) ? component.getHeight(): component.getWidth();
	}

	int getContentHeight() {
        Component component = view.getContent();
        return usesHorizontallayout(view) ? component.getWidth() : component.getHeight();
	}

	void setContentBounds(int x, int y, int width, int height) {
		Component component = view.getContent();
		if (usesHorizontallayout(component))
			component.setBounds(y, x, height, width);
		else
			component.setBounds(x, y, width, height);
	}

	void setContentVisible(boolean aFlag) {
		view.getContent().setVisible(aFlag);
	}

	boolean isContentVisible() {
		return view.isContentVisible();

	}

	boolean isSummary() {
		return view.isSummary();
	}

    boolean isFirstGroupNode() {
        return view.isFirstGroupNode();
    }

    boolean usesHorizontalLayout() {
        return view.usesHorizontalLayout();
    }

	boolean isLeft() {
		return view.isTopOrLeft();
	}

	boolean isRight() {
		return ! (view.isTopOrLeft() || view.isRoot());
	}

	boolean isRoot() {
		return view.isRoot();
	}

	int getHGap() {
		return view.getHGap();
	}

	int getShift() {
		return view.getShift();
	}

	boolean isFree() {
		return view.isFree();
	}


 	int getTopOverlap() {
		return topOverlap;
	}

	void setTopOverlap(int topOverlap) {
		final NodeViewLayoutHelper parentView = getParentView();
		this.topOverlap = parentView == null || usesHorizontalLayout() == parentView.usesHorizontalLayout() ?  topOverlap : 0 ;
	}

	int getBottomOverlap() {
		return bottomOverlap;
	}

	void setBottomOverlap(int bottomOverlap) {
		final NodeViewLayoutHelper parentView = getParentView();
		this.bottomOverlap = parentView == null || usesHorizontalLayout() == parentView.usesHorizontalLayout() ?  bottomOverlap : 0 ;
	}


	StepFunction getTopBoundary() {
		return topBoundary;
	}

	void setTopBoundary(StepFunction topBoundary) {
		this.topBoundary = topBoundary;
	}

	StepFunction getBottomBoundary() {
		return bottomBoundary;
	}

	void setBottomBoundary(StepFunction bottomBoundary) {
		this.bottomBoundary = bottomBoundary;
	}

	NodeViewLayoutHelper getParentView() {
		NodeView parentView = view.getParentView();
		return parentView != null ? parentView.getLayoutHelper() : null;
	}

	int getZoomed(int i) {
		return view.getZoomed(i);
	}

	int getHeight() {
		return getHeight(view);
	}

	int getWidth() {
		return getWidth(view);
	}

	int getX() {
		return getX(view);
	}

	int getY() {
		return getY(view);
	}

	void setSize(int width, int height) {
		if (usesHorizontallayout(view.getContent()))
			view.setSize(height, width);
		else
			view.setSize(width, height);
	}

	void setLocation(int x, int y) {
		if (usesHorizontallayout(view))
			view.setLocation(y, x);
		else
			view.setLocation(x, y);
	}

	private int getX(Component component) {
		return usesHorizontallayout(component) ? component.getY(): component.getX();
	}

	private int getY(Component component) {
		return usesHorizontallayout(component) ? component.getX(): component.getY();
	}

	private int getWidth(Component component) {
		return usesHorizontallayout(component) ? component.getHeight(): component.getWidth();
	}

	private int getHeight(Component component) {
		return usesHorizontallayout(component) ? component.getWidth(): component.getHeight();
	}

	String describeComponent(int i) {
	    return view.getComponent(i).toString();
	}

	String getText() {
	    return view.getNode().getText();
	}

	boolean usesHorizontallayout(Component component) {
	    NodeView parent;
        if (component == view && view.isRoot()) {
            parent = view;
        } else {
            parent = (NodeView)component.getParent();
        }
	    return parent.usesHorizontalLayout();
	}

    int getMinimumDistanceConsideringHandles() {
        return view.getMinimumDistanceConsideringHandles();
    }

    @Override
    public String toString() {
        return "NodeViewLayoutHelper [view=" + view + "]";
    }

    ChildrenSides childrenSides() {
        return view.childrenSides();
    }

    boolean isSubtreeVisible() {
       return view.isSubtreeVisible();
    }

    void calculateMinimumChildContentWidth() {
    	int max[] = {ContentSizeCalculator.UNSET, ContentSizeCalculator.UNSET};
    	int previousMax[] = {ContentSizeCalculator.UNSET, ContentSizeCalculator.UNSET};
    	int unfolded[] = {-1, -1};
    	final LinkedList<NodeView> childrenViews = view.getChildrenViews();
    	int lastIndex = childrenViews.size();
    	for (int index = 0; index < lastIndex; index++) {
    		NodeView child = childrenViews.get(index);
    		if(! isConsideredForAlignment(child))
    			continue;
			int sideIndex = child.isTopOrLeft() ? 0 : 1;
			final boolean marksSummary = child.getNode().isHiddenSummary();
			if(marksSummary || hasChildViews(child)) {
				final int updatedChildIndex = unfolded[sideIndex];
				if(updatedChildIndex >= 0) {
					final NodeView updated = childrenViews.get(updatedChildIndex);
					updated.getLayoutHelper().setMinimumContentWidth(Math.max(max[sideIndex], previousMax[sideIndex]));
				}
				if(marksSummary ) {
					unfolded[sideIndex] = -1;
					previousMax[sideIndex] = ContentSizeCalculator.UNSET;
				} else {
					unfolded[sideIndex] = index;
					previousMax[sideIndex] = max[sideIndex];
				}
				max[sideIndex] = ContentSizeCalculator.UNSET;
			}
			else {
				if(child.isContentVisible())
					max[sideIndex] = Math.max(max[sideIndex], child.getMainView().getPreferredSize().width);
				child.getLayoutHelper().setMinimumContentWidth(ContentSizeCalculator.UNSET);
			}
    	}
    	for(int sideIndex = 0; sideIndex <= 1; sideIndex++) {
    		final int updatedChildIndex = unfolded[sideIndex];
    		if(updatedChildIndex >= 0) {
    			final NodeView updated = childrenViews.get(updatedChildIndex);
    			updated.getLayoutHelper().setMinimumContentWidth(Math.max(max[sideIndex], previousMax[sideIndex]));
    		}
    	}
    }

	private void setMinimumContentWidth(int newWidth) {
		if (minimumContentWidth != newWidth) {
			minimumContentWidth = newWidth;
			view.invalidate();
		}
	}

	private boolean hasChildViews(NodeView child) {
		return child.getComponentCount() > 1;
	}

	private boolean isConsideredForAlignment(NodeView child) {
		return ! child.isFree() && child.getCloudModel() == null  && ! child.getChildNodesAlignment().isStacked();
	}

    void resetMinimumChildContentWidth() {
    	for (NodeView child : view.getChildrenViews())
    		child.getLayoutHelper().minimumContentWidth = ContentSizeCalculator.UNSET;
    }

	boolean isAutoCompactLayoutEnabled() {
		return view.isAutoCompactLayoutEnabled();
	}
}
