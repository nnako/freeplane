/*
 * Created on 11 Sep 2022
 *
 * author dimitry
 */
package org.freeplane.view.swing.map;

import java.awt.Component;
import java.awt.Dimension;

import org.freeplane.api.ChildrenSides;
import org.freeplane.api.ChildNodesAlignment;
import org.freeplane.features.map.NodeModel;

class NodeViewLayoutHelper {

	private NodeView view;
	private int topOverlap;
	private int bottomOverlap;
	private StepFunction topBoundary;
	private StepFunction bottomBoundary;
	private int minimumChildContentWidth = ContentSizeCalculator.UNSET;

	NodeViewLayoutHelper(NodeView view) {
		this.view = view;
	}

	Dimension calculateContentSize() {
		final int minimumContentWidth = isAligned(view) ? getMinimumContentWidth() : ContentSizeCalculator.UNSET;
		Dimension contentSize = ContentSizeCalculator.INSTANCE.calculateContentSize(view, minimumContentWidth);
		return usesHorizontallayout(view.getContent()) ? new Dimension(contentSize.height, contentSize.width) : contentSize;
	}

	private int getMinimumContentWidth() {
		final NodeViewLayoutHelper parentView = getParentView();
		return parentView == null ? ContentSizeCalculator.UNSET : parentView.minimumChildContentWidth;
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
		this.bottomOverlap = parentView == null || usesHorizontalLayout() == parentView.usesHorizontalLayout() ?  bottomOverlap : 0 ; ;
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
    	int min = ContentSizeCalculator.UNSET;
    	for (NodeView child : view.getChildrenViews()) {
    		if(isConsideredForAlignment(child))
    			min = Math.max(min, child.getMainView().getPreferredSize().width);
    	}
    	if(minimumChildContentWidth != min) {
    		this.minimumChildContentWidth = min;
        	for (NodeView child : view.getChildrenViews()) {
        		if(isAligned(child))
        			child.invalidate();
        	}
    	}
    }

	private boolean isAligned(NodeView child) {
		return isConsideredForAlignment(child) && child.getComponentCount() > 1;
	}

	private boolean isConsideredForAlignment(NodeView child) {
		return ! child.isFree() && child.getCloudModel() == null  && ! child.getChildNodesAlignment().isStacked();
	}

    void resetMinimumChildContentWidth() {
    	this.minimumChildContentWidth = ContentSizeCalculator.UNSET;
    }

	boolean isAutoCompactLayoutEnabled() {
		return view.isAutoCompactLayoutEnabled();
	}
}
