/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008-2014 Dimitry Polivaev
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
package org.freeplane.view.swing.map;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import org.freeplane.api.ChildrenSides;
import org.freeplane.api.ChildNodesAlignment;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.SummaryLevels;
import org.freeplane.features.nodelocation.LocationModel;

class VerticalNodeViewLayoutStrategy {

    static private boolean wrongChildComponentsReported = false;

    private int childViewCount;
    private final int spaceAround;
    private final NodeViewLayoutHelper view;

    private final int[] xCoordinates;
    private final int[] yCoordinates;
    private final boolean[] isChildFreeNode;
    private SummaryLevels viewLevels;
    private int top;
    private boolean rightSideCoordinatesAreSet;
    private boolean leftSideCoordinaresAreSet;

    final private CompactLayout compactLayout;

    private final int defaultVGap;
    private static final int SUMMARY_DEFAULT_HGAP_PX = LocationModel.DEFAULT_HGAP_PX * 7 / 12;
    private final Dimension contentSize;

    private int minimalDistanceBetweenChildren;
    private ChildNodesAlignment childNodesAlignment;
    private int childContentHeightSum;
    private int level;
    private int y;
    private int vGap;
    private boolean isFirstVisibleLaidOutChild;
    private int[] groupStartIndex;
    private int[] contentHeightSumAtGroupStart;
    private int[] groupUpperYCoordinate;
    private int[] groupLowerYCoordinate;
    private boolean currentSideLeft;

    private int baseDistanceToChildren;
    private boolean areChildrenSeparatedByY;
    private int[] summaryBaseX;

    // fields for content bounds calculation
    private int contentX;
    private int contentY;
    private int baseY;
    private int minY;

    public VerticalNodeViewLayoutStrategy(NodeView view, CompactLayout compactLayout) {
        NodeViewLayoutHelper layoutHelper = view.getLayoutHelper();
        this.view = layoutHelper;
        this.contentSize = ContentSizeCalculator.INSTANCE.calculateContentSize(layoutHelper);
        childViewCount = view.getComponentCount() - 1;
        layoutChildViews(view);
        this.top = 0;
        rightSideCoordinatesAreSet = false;
        leftSideCoordinaresAreSet = false;
        this.xCoordinates = new int[childViewCount];
        this.yCoordinates = new int[childViewCount];
        this.isChildFreeNode = new boolean[childViewCount];
        this.spaceAround = view.getSpaceAround();
        this.defaultVGap = view.getMap().getZoomed(LocationModel.DEFAULT_VGAP.toBaseUnits());
        this.compactLayout = compactLayout;
    }

    private void layoutChildViews(NodeView view) {
        for (int i = 0; i < childViewCount; i++) {
            final Component component = view.getComponent(i);
            if(component instanceof NodeView)
                ((NodeView) component).validateTree();
            else {
                childViewCount = i;
                if(! wrongChildComponentsReported) {
                    wrongChildComponentsReported = true;
                    final String wrongChildComponents = Arrays.toString(view.getComponents());
                    LogUtils.severe("Unexpected child components:" + wrongChildComponents, new Exception());
                }
            }
        }
    }

    private void setFreeChildNodes(final boolean laysOutLeftSide) {
        for (int i = 0; i < childViewCount; i++) {
            final NodeViewLayoutHelper child = view.getComponent(i);
            if (child.isLeft() == laysOutLeftSide)
                this.isChildFreeNode[i] = child.isFree();
        }
    }
    public void calculateLayoutData() {
        final NodeModel node = view.getNode();
        MapView map = view.getMap();
        Filter filter = map.getFilter();
        NodeModel selectionRoot = map.getRoot().getNode();
        viewLevels = childViewCount == 0 ? SummaryLevels.ignoringChildNodes(selectionRoot, node, filter) : SummaryLevels.of(selectionRoot, node, filter);
        for(boolean isLeft : viewLevels.sides)
            calculateLayoutData(isLeft);
        applyLayoutToChildComponents();
    }

    private void calculateLayoutData(final boolean isLeft) {
        setFreeChildNodes(isLeft);
        calculateLayoutX(isLeft);
        calculateLayoutY(isLeft);

    }

    private void calculateLayoutX(final boolean laysOutLeftSide) {
        currentSideLeft = laysOutLeftSide;
        baseDistanceToChildren = view.getBaseDistanceToChildren();
        childNodesAlignment = view.getChildNodesAlignment();
        areChildrenSeparatedByY = childNodesAlignment.isStacked();
        level = viewLevels.highestSummaryLevel + 1;
        summaryBaseX = new int[level];
        for (int i = 0; i < childViewCount; i++) {
            NodeViewLayoutHelper child = view.getComponent(i);
            if (child.isLeft() == currentSideLeft) {
                int oldLevel = level;
                level = viewLevels.summaryLevels[i];
                boolean isFreeNode = child.isFree();
                boolean isItem = level == 0;
                int childHGap = calculateChildHorizontalGap(child, isItem, isFreeNode, baseDistanceToChildren);
                if (isItem) {
                    assignRegularChildHorizontalPosition(i, child, oldLevel, childHGap);
                } else {
                    assignSummaryChildHorizontalPosition(i, child, childHGap);
                }
            }
        }
    }

    private int calculateChildHorizontalGap(NodeViewLayoutHelper child,
            boolean isItem,
            boolean isFreeNode,
            int baseDistanceToChildren) {
        int hGap;
        if (child.isContentVisible()) {
            hGap = calculateDistance(child, NodeViewLayoutHelper::getHGap);
        } else if (child.isSummary()) {
            hGap = child.getZoomed(SUMMARY_DEFAULT_HGAP_PX);
        } else {
            hGap = 0;
        }
        if (view.getNode().isHiddenSummary() && !child.getNode().isHiddenSummary()) {
            hGap -= child.getZoomed(SUMMARY_DEFAULT_HGAP_PX);
        }
        if (isItem && !isFreeNode && child.isSubtreeVisible()) {
            hGap += baseDistanceToChildren;
        }
        return hGap;
    }

    private void assignRegularChildHorizontalPosition(int index,
                            NodeViewLayoutHelper child,
                            int oldLevel,
                            int childHGap) {
        if (!child.isFree() && (oldLevel > 0 || child.isFirstGroupNode())) {
            summaryBaseX[0] = 0;
        }
        placeChildXCoordinate(index, child, childHGap);
    }

    private void assignSummaryChildHorizontalPosition(int index,
                            NodeViewLayoutHelper child,
                            int childHGap) {
        if (child.isFirstGroupNode()) {
            summaryBaseX[level] = 0;
        }
        placeChildXCoordinate(index, child, childHGap);
    }

    private void placeChildXCoordinate(int index,
                                    NodeViewLayoutHelper child,
                                    int childHGap) {
        int baseX;
        if (level > 0) {
            baseX = summaryBaseX[level - 1];
        } else {
            if (level == 0 && areChildrenSeparatedByY && view.childrenSides() == ChildrenSides.BOTH_SIDES) {
                baseX = contentSize.width / 2;
            } else if (child.isLeft() != (level == 0 && (child.isFree() || areChildrenSeparatedByY))) {
                baseX = 0;
            } else {
                baseX = contentSize.width;
            }
        }
        int x;
        if (child.isLeft()) {
            x = baseX - childHGap - child.getContentX() - child.getContentWidth();
            summaryBaseX[level] = Math.min(summaryBaseX[level], x + spaceAround);
        } else {
            x = baseX + childHGap - child.getContentX();
            summaryBaseX[level] = Math.max(summaryBaseX[level], x + child.getWidth() - spaceAround);
        }
        this.xCoordinates[index] = x;
    }

    private void calculateLayoutY(final boolean laysOutLeftSide) {
        currentSideLeft = laysOutLeftSide;
        minimalDistanceBetweenChildren = view.getMinimalDistanceBetweenChildren();
        childNodesAlignment = view.getChildNodesAlignment();
        childContentHeightSum = 0;
        top = 0;
        level = viewLevels.highestSummaryLevel + 1;
        y = 0;
        vGap = 0;
        isFirstVisibleLaidOutChild = true;
        groupStartIndex = new int[level];
        contentHeightSumAtGroupStart = new int[level];
        groupUpperYCoordinate = new int[level];
        groupLowerYCoordinate = new int[level];

        NodeViewLayoutHelper alignedChild = null;

        for (int index = 0; index < childViewCount; index++) {
            NodeViewLayoutHelper child = view.getComponent(index);
            if (child.isLeft() == currentSideLeft) {
                int oldLevel = level;
                int childHeight = child.getHeight() - 2 * spaceAround;
                level = viewLevels.summaryLevels[index];
                boolean isFreeNode = child.isFree();
                boolean isItem = level == 0;
                int childShiftY = calculateDistance(child, NodeViewLayoutHelper::getShift);

                if (level == 0) {
                    if (isFreeNode) {
                        assignFreeChildVerticalPosition(index, childShiftY, child);
                    } else {
                        alignedChild = child;
                        assignRegularChildVerticalPosition(index, child, childHeight, childShiftY);
                        initializeSummaryGroupStart(index,
                                oldLevel,
                                child.isFirstGroupNode(),
                                childContentHeightSum,
                                groupStartIndex,
                                groupUpperYCoordinate,
                                groupLowerYCoordinate,
                                contentHeightSumAtGroupStart);
                 if (childHeight != 0) {
                     isFirstVisibleLaidOutChild = false;
                 }
                    }
                } else {
                    assignSummaryChildVerticalPosition(index, child, childHeight, childShiftY);
                }
                if (!(isItem && isFreeNode)) {
                    updateSummaryGroupBounds(index, child, level, isItem, childHeight,
                            groupUpperYCoordinate, groupLowerYCoordinate);
                }
            }
        }
        if (childNodesAlignment == ChildNodesAlignment.LAST_CHILD_BY_PARENT && alignedChild != null) {
            top += alignedChild.getHeight()
                    - (alignedChild.getContentY() + alignedChild.getContentHeight()
                    + spaceAround + alignedChild.getBottomOverlap());
        }
        top += align(contentSize.height - childContentHeightSum);
        if (childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT
                && contentSize.height > 0
                && !isFirstVisibleLaidOutChild) {
            top -= calculateAddedDistanceFromParentToChildren(minimalDistanceBetweenChildren, contentSize);
        }
        calculateRelativeCoordinatesForContentAndBothSides(laysOutLeftSide, top);
    }

    private void calculateRelativeCoordinatesForContentAndBothSides(boolean isLeft, int topOnSide) {
        if (! (leftSideCoordinaresAreSet || rightSideCoordinatesAreSet)) {
            top = topOnSide;
        } else {
            int deltaTop = topOnSide - this.top;
            final boolean changeLeft;
            if (deltaTop < 0) {
                top = topOnSide;
                changeLeft = !isLeft;
                deltaTop = -deltaTop;
            } else {
                changeLeft = isLeft;
            }
            for (int i = 0; i < childViewCount; i++) {
                NodeViewLayoutHelper child = view.getComponent(i);
                if (child.isLeft() == changeLeft
                        && (viewLevels.summaryLevels[i] > 0 || !isChildFreeNode[i])) {
                    yCoordinates[i] += deltaTop;
                }
            }
        }
        if (isLeft)
            leftSideCoordinaresAreSet = true;
        else
            rightSideCoordinatesAreSet = true;
    }


    private int calculateAddedDistanceFromParentToChildren(final int minimalDistance,
            final Dimension contentSize) {
        boolean usesHorizontalLayout = view.usesHorizontalLayout();
        int distance = Math.max(view.getMap().getZoomed(usesHorizontalLayout ? LocationModel.DEFAULT_VGAP_PX * 2 : LocationModel.DEFAULT_VGAP_PX), minimalDistance);
        return contentSize.height + distance;
    }

    private int calculateExtraGapForChildren(final int minimalDistanceBetweenChildren) {
        if(3 * defaultVGap > minimalDistanceBetweenChildren)
            return minimalDistanceBetweenChildren + 2 * defaultVGap;
        else
            return (minimalDistanceBetweenChildren + 11 * 2 * defaultVGap) / 6;
    }

    private int align(int height) {
        if (view.isSummary()
                || childNodesAlignment == ChildNodesAlignment.NOT_SET
                || childNodesAlignment == ChildNodesAlignment.BY_CENTER
                || childNodesAlignment == ChildNodesAlignment.FLOW) {
            return height/2;
        } else if (childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT
                || childNodesAlignment == ChildNodesAlignment.LAST_CHILD_BY_PARENT) {
            return height;
        }
        return 0;
    }

    private int calculateDistance(final NodeViewLayoutHelper child, ToIntFunction<NodeViewLayoutHelper> nodeDistance) {
        if (!child.isContentVisible())
            return 0;
        int shift = nodeDistance.applyAsInt(child);
        for(NodeViewLayoutHelper ancestor = child.getParentView();
                ancestor != null && ! ancestor.isContentVisible();
                ancestor = ancestor.getParentView()) {
            if(ancestor.isFree())
                shift += nodeDistance.applyAsInt(ancestor);
        }
        return shift;
    }

    private boolean isNextNodeSummaryNode(int childViewIndex) {
        return childViewIndex + 1 < viewLevels.summaryLevels.length && viewLevels.summaryLevels[childViewIndex + 1] > 0;
    }

    private int summarizedNodeDistance(final int distance) {
        if(defaultVGap >= distance)
            return distance;
        else
            return defaultVGap + (distance - defaultVGap) / 6;
    }

    private int calculateExtraVerticalGap(int childHeight, int contentHeight, int cloudHeight, int minimalDistanceBetweenChildren) {
        if (childHeight <= 0) {
            return 0;
        }
        int extraHeight = childHeight - (contentHeight + cloudHeight);
        if (extraHeight <= 0) {
            return 0;
        }
        return Math.min(extraHeight, calculateExtraGapForChildren(minimalDistanceBetweenChildren));
    }

    private void assignFreeChildVerticalPosition(int childIndex, int shiftY, NodeViewLayoutHelper child) {
        this.yCoordinates[childIndex] = shiftY - child.getContentY();
    }


    private void assignRegularChildVerticalPosition(int index,
                                NodeViewLayoutHelper child,
                                int childHeight,
                                int childShiftY) {
        int extraVGap = calculateExtraVerticalGap(
                childHeight,
                child.getContentHeight(),
                CloudHeightCalculator.INSTANCE.getAdditionalCloudHeigth(child),
                minimalDistanceBetweenChildren);
        childContentHeightSum += vGap;
        if (isFirstVisibleLaidOutChild
                && childNodesAlignment == ChildNodesAlignment.AFTER_PARENT
                && contentSize.height > 0) {
            y += calculateAddedDistanceFromParentToChildren(minimalDistanceBetweenChildren, contentSize);
        }
        if (!isFirstVisibleLaidOutChild
                && child.paintsChildrenOnTheLeft()
                && view.usesHorizontalLayout()) {
            int missingWidth = child.getMinimumDistanceConsideringHandles() - vGap - extraVGap;
            if (missingWidth > 0) {
                top -= missingWidth;
                y += missingWidth;
                childContentHeightSum += missingWidth;
            }
        }
        top += calculateRegularChildVerticalDelta(child, childShiftY, isFirstVisibleLaidOutChild, childNodesAlignment);
        int childTopOverlap = child.getTopOverlap();
        top += childTopOverlap;
        y -= childTopOverlap;
        int upperGap = align(extraVGap);
        if (!isFirstVisibleLaidOutChild) {
            top -= upperGap;
            y += upperGap;
        }
        if (childShiftY < 0 && !compactLayout.isCompact()) {
            yCoordinates[index] = y;
            y -= childShiftY;
        } else {
            if (!isFirstVisibleLaidOutChild || compactLayout.isCompact()) {
                y += childShiftY;
            }
            yCoordinates[index] = y;
        }
        int summaryNodeIndex = viewLevels.findSummaryNodeIndex(index);
        if (summaryNodeIndex == SummaryLevels.NODE_NOT_FOUND
                || summaryNodeIndex - 1 == index) {
            vGap = minimalDistanceBetweenChildren;
        } else if (childHeight != 0) {
            vGap = summarizedNodeDistance(minimalDistanceBetweenChildren);
        }
        if (!child.paintsChildrenOnTheLeft() && view.usesHorizontalLayout()) {
            int missingWidth2 = child.getMinimumDistanceConsideringHandles() - vGap - extraVGap;
            if (missingWidth2 > 0) {
                y += missingWidth2;
                if (!isFirstVisibleLaidOutChild) {
                    childContentHeightSum += missingWidth2;
                }
            }
        }
        y += extraVGap - upperGap;
        if (childHeight != 0) {
            y += childHeight + vGap - child.getBottomOverlap();
        }
        childContentHeightSum += child.getContentHeight()
                + CloudHeightCalculator.INSTANCE.getAdditionalCloudHeigth(child);
    }

    private int calculateRegularChildVerticalDelta(NodeViewLayoutHelper child, int childShiftY, boolean isFirstVisible, ChildNodesAlignment alignment) {
        int delta = 0;
        if ((childShiftY < 0 || isFirstVisible) && !compactLayout.isCompact()) {
            delta += childShiftY;
        }
        int childCloudHeight = CloudHeightCalculator.INSTANCE.getAdditionalCloudHeigth(child);
        switch (alignment) {
            case BEFORE_PARENT:
            case LAST_CHILD_BY_PARENT:
                delta += - child.getHeight()
                         + childCloudHeight
                         + 2 * spaceAround
                         + child.getBottomOverlap()
                         + child.getContentHeight();
                break;
            case BY_CENTER:
                delta += - child.getHeight() / 2
                         + childCloudHeight / 2
                         + spaceAround
                         + child.getBottomOverlap() / 2
                         + child.getContentHeight() / 2;
                break;
            case FIRST_CHILD_BY_PARENT:
                if (isFirstVisible) {
                    delta += - (child.getContentY() - spaceAround);
                }
                break;
            default:
                if (alignment != ChildNodesAlignment.AFTER_PARENT
                        && alignment != ChildNodesAlignment.FIRST_CHILD_BY_PARENT) {
                    delta += - (child.getContentY()
                             - childCloudHeight / 2
                             - spaceAround);
                }
        }
        return delta;
    }

    private void assignSummaryChildVerticalPosition(int index,
                                   NodeViewLayoutHelper child,
                                   int childHeight,
                                   int childShiftY) {
        int itemLevel = level - 1;
        if (child.isFirstGroupNode()) {
            contentHeightSumAtGroupStart[level] = contentHeightSumAtGroupStart[itemLevel];
            groupStartIndex[level] = groupStartIndex[itemLevel];
        }
        if (groupUpperYCoordinate[itemLevel] == Integer.MAX_VALUE) {
            groupUpperYCoordinate[itemLevel] = y;
            groupLowerYCoordinate[itemLevel] = y;
        }
        int summaryY = calculateSummaryChildVerticalPosition(
                groupUpperYCoordinate[itemLevel],
                groupLowerYCoordinate[itemLevel],
                child,
                childShiftY);
        yCoordinates[index] = summaryY;
        if (!child.isFree()) {
            int deltaY = summaryY
                    - groupUpperYCoordinate[itemLevel]
                    + child.getTopOverlap();
            if (deltaY < 0) {
                top += deltaY;
                y -= deltaY;
                summaryY -= deltaY;
                for (int j = groupStartIndex[itemLevel]; j <= index; j++) {
                    NodeViewLayoutHelper groupItem = view.getComponent(j);
                    if (groupItem.isLeft() == currentSideLeft
                            && (viewLevels.summaryLevels[j] > 0
                            || !isChildFreeNode[j])) {
                        yCoordinates[j] -= deltaY;
                    }
                }
            }
            if (childHeight != 0) {
                summaryY += childHeight
                        + minimalDistanceBetweenChildren
                        - child.getBottomOverlap();
            }
            y = Math.max(y, summaryY);
        }
    }

    private int calculateSummaryChildVerticalPosition(int groupUpper, int groupLower,
            NodeViewLayoutHelper child, int childShiftY) {
        int childCloudHeight = CloudHeightCalculator.INSTANCE.getAdditionalCloudHeigth(child);
        int childContentHeight = child.getContentHeight() + childCloudHeight;
        return (groupUpper + groupLower) / 2
                - childContentHeight / 2 + childShiftY
                - (child.getContentYForSummary() - childCloudHeight / 2 - spaceAround);
    }

    private void updateSummaryGroupBounds(int childIndex,
            NodeViewLayoutHelper child,
            int level,
            boolean isItem,
            int childHeight,
            int[] groupUpper,
            int[] groupLower) {
        int childUpper = yCoordinates[childIndex] + child.getTopOverlap();
        int childBottom = yCoordinates[childIndex] + childHeight - child.getBottomOverlap();
        if (child.isFirstGroupNode()) {
            if (isItem) {
                groupUpper[level] = Integer.MAX_VALUE;
                groupLower[level] = Integer.MIN_VALUE;
            } else {
                groupUpper[level] = childUpper;
                groupLower[level] = childBottom;
            }
        }
        else if (childHeight != 0 || isNextNodeSummaryNode(childIndex)) {
            groupUpper[level] = Math.min(groupUpper[level], childUpper);
            groupLower[level] = Math.max(childBottom, groupLower[level]);
        }
    }

    private void initializeSummaryGroupStart(int childIndex,
            int oldLevel,
            boolean isFirstGroupNode,
            int childContentHeightSum,
            int[] groupStartIndex,
            int[] groupUpperYCoordinate,
            int[] groupLowerYCoordinate,
            int[] contentHeightSumAtGroupStart) {
        if (oldLevel > 0) {
            for (int j = 0; j < oldLevel; j++) {
                groupStartIndex[j] = childIndex;
                groupUpperYCoordinate[j] = Integer.MAX_VALUE;
                groupLowerYCoordinate[j] = Integer.MIN_VALUE;
                contentHeightSumAtGroupStart[j] = childContentHeightSum;
            }
        } else if (isFirstGroupNode) {
            contentHeightSumAtGroupStart[0] = childContentHeightSum;
            groupStartIndex[0] = childIndex;
        }
    }

    private void applyLayoutToChildComponents() {
        int spaceAround = view.getSpaceAround();
        int cloudHeight = CloudHeightCalculator.INSTANCE.getAdditionalCloudHeigth(view);
        computeContentBounds(cloudHeight, spaceAround);
        view.setContentBounds(contentX, contentY, contentSize.width, contentSize.height);
        arrangeChildComponents(contentX, contentY, baseY, cloudHeight, spaceAround);
        view.setTopOverlap(-minY);
    }

    /**
     * Computes contentX, contentY, baseY, minY and cloudHeight for layout.
     */
    private void computeContentBounds(int cloudHeight, int spaceAround) {
        int leftMostX = IntStream.of(xCoordinates).min().orElse(0);
        contentX = Math.max(spaceAround, -leftMostX);
        contentY = spaceAround + cloudHeight/2 - Math.min(0, top);
        view.setContentVisible(view.isContentVisible());
        baseY = contentY - spaceAround + top;
        minY = calculateMinY(contentY, baseY, cloudHeight);
        if (minY < 0) {
            contentY -= minY;
            baseY -= minY;
        }
    }

    private int calculateMinY(int contentY, int baseY, int cloudHeight) {
        int minY = 0;
        for (int i = 0; i < childViewCount; i++) {
            if (viewLevels.summaryLevels[i] == 0 && isChildFreeNode[i]) {
                minY = Math.min(minY, contentY + yCoordinates[i] - cloudHeight/2);
            } else {
                minY = Math.min(minY, baseY + yCoordinates[i]);
            }
        }
        return minY;
    }

    private void arrangeChildComponents(int contentX, int contentY,
                                int baseY, int cloudHeight,
                                int spaceAround) {
        int width = contentX + contentSize.width + spaceAround;
        int height = contentY + contentSize.height + cloudHeight/2 + spaceAround;
        int heightWithoutOverlap = height;
        for (int i = 0; i < childViewCount; i++) {
            NodeViewLayoutHelper child = view.getComponent(i);
            boolean free = isChildFreeNode[i];
            int y = (viewLevels.summaryLevels[i] == 0 && free)
                    ? contentY + yCoordinates[i]
                    : baseY + yCoordinates[i];
            if (!free) {
                heightWithoutOverlap = Math.max(
                        heightWithoutOverlap,
                        y + child.getHeight() + cloudHeight/2
                                - child.getBottomOverlap());
            }
            int x = contentX + xCoordinates[i];
            child.setLocation(x, y);
            width = Math.max(width, x + child.getWidth());
            height = Math.max(height, y + child.getHeight() + cloudHeight/2);
        }
        view.setSize(width, height);
        view.setBottomOverlap(height - heightWithoutOverlap);
    }
}
