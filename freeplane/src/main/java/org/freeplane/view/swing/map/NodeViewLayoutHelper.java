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
	static final int UNSET = -1;

	private NodeView view;
	private int topOverlap;
	private int bottomOverlap;
	private StepFunction topBoundary;
	private StepFunction bottomBoundary;
	private int minimumChildContentSize = UNSET;

	NodeViewLayoutHelper(NodeView view) {
		this.view = view;
	}

	Dimension calculateContentSize() {
		Dimension contentSize = ContentSizeCalculator.INSTANCE.calculateContentSize(view);
		final boolean parentUsesHorizontallayout = parentUsesHorizontalLayout(view);
		int minimumWidth = getMinimumContentSize();
		if(parentUsesHorizontallayout) {
			return new Dimension(Math.max(minimumWidth, contentSize.height), contentSize.width);
		}
		else {
			if(minimumWidth != UNSET) {
				final int maximumWidth = view.getMainView().getMaximumWidth();
				if(contentSize.width < minimumWidth && contentSize.width < maximumWidth)
					contentSize.width = Math.min(minimumWidth, maximumWidth);
			}
			return contentSize;
		}
	}

    void calculateMinimumChildContentSize() {
    	int min = UNSET;
    	final boolean usesHorizontalLayout = usesHorizontalLayout();
    	for (NodeView child : view.getChildrenViews()) {
    		if(! child.isFree()) {
				final Dimension preferredSize = child.getMainView().getPreferredSize();
				min = Math.max(min, usesHorizontalLayout ? preferredSize.height : preferredSize.width);
			}
    	}
    	this.minimumChildContentSize = min;
    }

    void resetMinimumChildContentSize() {
    	this.minimumChildContentSize = UNSET;
    }

    private int getMinimumContentSize() {
		final NodeViewLayoutHelper parentView = getParentView();
		return parentView == null ? UNSET : parentView.minimumChildContentSize;
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
        return parentUsesHorizontalLayout(view) ? component.getY(): component.getX();
	}

	int getContentY() {
        Component component = view.getContent();
        return parentUsesHorizontalLayout(view) ? component.getX(): component.getY();
	}


    int getContentWidth() {
		Component component = view.getContent();
        return parentUsesHorizontalLayout(view) ? component.getHeight(): component.getWidth();
	}

	int getContentHeight() {
        Component component = view.getContent();
        return parentUsesHorizontalLayout(view) ? component.getWidth() : component.getHeight();
	}

	void setContentBounds(int x, int y, int width, int height) {
		Component component = view.getContent();
		if (parentUsesHorizontalLayout(component))
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
		if (parentUsesHorizontalLayout(view.getContent()))
			view.setSize(height, width);
		else
			view.setSize(width, height);
	}

	void setLocation(int x, int y) {
		if (parentUsesHorizontalLayout(view))
			view.setLocation(y, x);
		else
			view.setLocation(x, y);
	}

	private int getX(Component component) {
		return parentUsesHorizontalLayout(component) ? component.getY(): component.getX();
	}

	private int getY(Component component) {
		return parentUsesHorizontalLayout(component) ? component.getX(): component.getY();
	}

	private int getWidth(Component component) {
		return parentUsesHorizontalLayout(component) ? component.getHeight(): component.getWidth();
	}

	private int getHeight(Component component) {
		return parentUsesHorizontalLayout(component) ? component.getWidth(): component.getHeight();
	}

	String describeComponent(int i) {
	    return view.getComponent(i).toString();
	}

	String getText() {
	    return view.getNode().getText();
	}

	boolean parentUsesHorizontalLayout(Component component) {
	    NodeView parent;
        if (component == view && view.isRoot()) {
            parent = view;
        } else {
            parent = (NodeView)component.getParent();
        }
	    return parent.usesHorizontalLayout();
	}

    boolean usesHorizontalLayout() {
        return view.usesHorizontalLayout();
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

}
