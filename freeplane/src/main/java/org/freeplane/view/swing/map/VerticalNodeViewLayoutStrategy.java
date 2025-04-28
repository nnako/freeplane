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

import org.freeplane.api.ChildNodesAlignment;
import org.freeplane.api.ChildrenSides;
import org.freeplane.core.ui.components.UITools;
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
    private final int[] yBottomCoordinates;
    private final boolean[] isChildFreeNode;
    private StepFunction bottomBoundary;
    private int bottomY;
    private StepFunction leftBottomBoundary;
    private StepFunction rightBottomBoundary;
    private SummaryLevels viewLevels;
    private int totalShiftY;
    private int totalSideShiftY;
    private boolean rightSideCoordinatesAreSet;
    private boolean leftSideCoordinaresAreSet;

    final private boolean allowsCompactLayout;
    final private boolean isAutoCompactLayoutEnabled;

    private final int defaultVGap;
    private static final int SUMMARY_DEFAULT_HGAP_PX = LocationModel.DEFAULT_HGAP_PX * 7 / 12;
    private final Dimension contentSize;

    private int minimalDistanceBetweenChildren;
    private ChildNodesAlignment childNodesAlignment;
    private int level;
    private int y;
    private int vGap;
    private int visibleLaidOutChildCounter;
    private int[] groupStartIndex;
    private StepFunction[] groupStartBoundaries;
    private int[] groupUpperYCoordinate;
    private int[] groupLowerYCoordinate;
    private boolean currentSideLeft;

    private int baseDistanceToChildren;
    private boolean areChildrenSeparatedByY;
    private int[] summaryBaseX;

    private int extraGapForChildren;


    public VerticalNodeViewLayoutStrategy(NodeView view, boolean allowsCompactLayout, boolean isAutoCompactLayoutEnabled) {
        NodeViewLayoutHelper layoutHelper = view.getLayoutHelper();
        this.view = layoutHelper;
        this.contentSize = ContentSizeCalculator.INSTANCE.calculateContentSize(layoutHelper);
        childViewCount = view.getComponentCount() - 1;
        layoutChildViews(view);
        this.totalShiftY = 0;
        rightSideCoordinatesAreSet = false;
        leftSideCoordinaresAreSet = false;
        this.xCoordinates = new int[childViewCount];
        this.yCoordinates = new int[childViewCount];
        this.yBottomCoordinates = new int[childViewCount];
        this.isChildFreeNode = new boolean[childViewCount];
        this.spaceAround = view.getSpaceAround();
        this.defaultVGap = view.getMap().getZoomed(LocationModel.DEFAULT_VGAP.toBaseUnits());
        this.allowsCompactLayout = allowsCompactLayout;
        this.childNodesAlignment = view.getChildNodesAlignment();
        this.isAutoCompactLayoutEnabled = isAutoCompactLayoutEnabled && ! childNodesAlignment.isStacked();
        this.minimalDistanceBetweenChildren = view.getMinimalDistanceBetweenChildren();
        this.extraGapForChildren = calculateExtraGapForChildren(minimalDistanceBetweenChildren);

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
        totalSideShiftY = 0;
        level = viewLevels.highestSummaryLevel + 1;
        y = 0;
        bottomY = 0;
        bottomBoundary = null;
        vGap = 0;
        visibleLaidOutChildCounter = 0;
        groupStartIndex = new int[level];
        groupStartBoundaries = new StepFunction[level];
        groupUpperYCoordinate = new int[level];
        groupLowerYCoordinate = new int[level];

        for (int index = 0; index < childViewCount; index++) {
            NodeViewLayoutHelper child = view.getComponent(index);
            if (child.isLeft() == currentSideLeft) {
                int oldLevel = level;
                int childRegularHeight = child.getHeight() - child.getTopOverlap() - child.getBottomOverlap() - 2 * spaceAround;
                level = viewLevels.summaryLevels[index];
                if(index >= viewLevels.summaryLevels.length){
                    final String errorMessage = "Bad node view child components: missing node for component " + index;
                    UITools.errorMessage(errorMessage);
                    System.err.println(errorMessage);
                    for (int i = 0; i < view.getComponentCount(); i++){
                        final String component = view.describeComponent(i);
                        System.err.println(component);
                    }
                }
                boolean isFreeNode = child.isFree();
                boolean isItem = level == 0;
                int childShiftY = calculateDistance(child, NodeViewLayoutHelper::getShift);

                if (level == 0) {
                    if (isFreeNode) {
                        assignFreeChildVerticalPosition(index, childShiftY, child);
                    } else {
                        assignRegularChildVerticalPosition(index, child, childRegularHeight, childShiftY);
                        initializeSummaryGroupStart(index, oldLevel, child.isFirstGroupNode());
                 if (childRegularHeight != 0) {
                     visibleLaidOutChildCounter++;
                 }
                    }
                } else {
                    assignSummaryChildVerticalPosition(index, child, childRegularHeight, childShiftY);
                }
                if (!(isItem && isFreeNode)) {
                    updateSummaryGroupBounds(index, child, level, isItem, childRegularHeight);
                }
            }
        }
        if (childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT
                && contentSize.height > 0
                && !isFirstVisibleLaidOutChild()) {
            totalSideShiftY -= calculateAddedDistanceFromParentToChildren(minimalDistanceBetweenChildren, contentSize);
        }
        totalSideShiftY += align(contentSize.height);
        calculateRelativeCoordinatesForContentAndBothSides(laysOutLeftSide);
    }

    private void initializeSummaryGroupStart(int childIndex,
            int oldLevel,
            boolean isFirstGroupNode) {
        if (oldLevel > 0) {
            for (int j = 0; j < oldLevel; j++) {
                groupStartBoundaries[j] = bottomBoundary;
                groupStartIndex[j] = childIndex;
                groupUpperYCoordinate[j] = Integer.MAX_VALUE;
                groupLowerYCoordinate[j] = Integer.MIN_VALUE;
            }
        } else if (isFirstGroupNode) {
            groupStartIndex[0] = childIndex;
            groupStartBoundaries[0] = bottomBoundary;
        }
    }

    private void calculateRelativeCoordinatesForContentAndBothSides(boolean isLeft) {
        if (! (leftSideCoordinaresAreSet || rightSideCoordinatesAreSet)) {
            totalShiftY = totalSideShiftY;
        } else {
            int delta = totalSideShiftY - this.totalShiftY;
            if(delta != 0) {
                final boolean changeLeft;
                if (delta < 0) {
                    totalShiftY = totalSideShiftY;
                    changeLeft = !isLeft;
                    delta = -delta;
                } else {
                    changeLeft = isLeft;
                }
                for (int i = 0; i < childViewCount; i++) {
                    NodeViewLayoutHelper child = view.getComponent(i);
                    if (child.isLeft() == changeLeft
                            && (viewLevels.summaryLevels[i] > 0 || !isChildFreeNode[i])) {
                        yCoordinates[i] += delta;
                    }
                }
                if(bottomBoundary != null) {
                    bottomBoundary = bottomBoundary.translate(0, delta);
                }
            }
        }
        if (isLeft) {
            leftSideCoordinaresAreSet = true;
            leftBottomBoundary = bottomBoundary;
        } else {
            rightSideCoordinatesAreSet = true;
            rightBottomBoundary = bottomBoundary;
        }
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

    private int calculateExtraVerticalGap(int childHeight, int contentHeight, int cloudHeight) {
        if (childHeight <= 0) {
            return 0;
        }
        int extraHeight = childHeight - (contentHeight + cloudHeight);
        if (extraHeight <= 0) {
            return 0;
        }
        return Math.min(extraHeight, extraGapForChildren);
    }

    private void assignFreeChildVerticalPosition(int childIndex, int shiftY, NodeViewLayoutHelper child) {
        this.yCoordinates[childIndex] = shiftY - child.getContentY();
    }


    private void assignRegularChildVerticalPosition(int index,
                                NodeViewLayoutHelper child,
                                int childRegularHeight,
                                int childShiftY) {
        final int additionalCloudHeight = CloudHeightCalculator.INSTANCE.getAdditionalCloudHeight(child);

        handleFirstVisibleChildAlignment();

        int extraVGap = calculateExtraVerticalGap(childRegularHeight, child.getContentHeight(), additionalCloudHeight);
        int upperGap = align(extraVGap);

        int distance = adjustForAutoCompactLayout(child, index);
        if (distance != 0 && distance != StepFunction.DEFAULT_VALUE) {
            extraVGap += extraGapForChildren - upperGap;
            upperGap = extraGapForChildren;
        }


        if (isFirstVisibleLaidOutChild()) {
            extraVGap -= upperGap;
            upperGap = 0;
        }

        int childContentHeightSum = vGap;
        childContentHeightSum += adjustForLeftChildrenWithHandles(child, extraVGap);

        if (!isFirstVisibleLaidOutChild()) {
            y += upperGap;
        }

        updateTotalSideShiftY(childShiftY);

        setYCoordinate(index, childShiftY);

        updateVGap(index, childRegularHeight);

        childContentHeightSum += adjustForRightChildrenWithHandles(child, extraVGap);

        if (childRegularHeight == 0)
            return;

        y += extraVGap - upperGap;
        y += vGap;
        yBottomCoordinates[index] = y;
        updateBottomBoundary(child, index, y, CombineOperation.FALLBACK);
        y += childRegularHeight;
        bottomY = Math.max(bottomY, y);

        updateTotalSideShiftYForAlignment(child, childContentHeightSum, additionalCloudHeight, upperGap, extraVGap, childRegularHeight, distance, y);
    }

    private void handleFirstVisibleChildAlignment() {
        if (isFirstVisibleLaidOutChild()
                && childNodesAlignment == ChildNodesAlignment.AFTER_PARENT
                && contentSize.height > 0) {
            y += calculateAddedDistanceFromParentToChildren(minimalDistanceBetweenChildren, contentSize);
        }
    }

    private int adjustForAutoCompactLayout(NodeViewLayoutHelper child, int index) {
        int distance = 0;
        if (isAutoCompactLayoutEnabled && bottomBoundary != null) {
            StepFunction childTopBoundary = child.getTopBoundary();
            if (childTopBoundary != null) {
                childTopBoundary = childTopBoundary.translate(xCoordinates[index], y);
                distance = childTopBoundary.distance(bottomBoundary);
                if (distance != 0 && distance != StepFunction.DEFAULT_VALUE) {
                    y -= distance;
                }
            }
        }
        return distance;
    }

    private int adjustForLeftChildrenWithHandles(NodeViewLayoutHelper child, int extraVGap) {
        int added = 0;
        if (!isFirstVisibleLaidOutChild()
                && child.paintsChildrenOnTheLeft()
                && view.usesHorizontalLayout()) {
            int missingWidth = child.getMinimumDistanceConsideringHandles() - vGap - extraVGap;
            if (missingWidth > 0) {
                y += missingWidth;
                added += missingWidth;
            }
        }
        return added;
    }

    private void updateTotalSideShiftY(int childShiftY) {
        if ((childShiftY < 0 || isFirstVisibleLaidOutChild()) && !allowsCompactLayout) {
            totalSideShiftY += childShiftY;
        }
    }

    private void setYCoordinate(int index, int childShiftY) {
        if (childShiftY < 0 && !allowsCompactLayout) {
            yCoordinates[index] = y;
            y -= childShiftY;
        } else {
            if (!isFirstVisibleLaidOutChild() || allowsCompactLayout) {
                y += childShiftY;
            }
            yCoordinates[index] = y;
        }
    }

    private void updateVGap(int index, int childRegularHeight) {
        int summaryNodeIndex = viewLevels.findSummaryNodeIndex(index);
        if (summaryNodeIndex == SummaryLevels.NODE_NOT_FOUND
                || summaryNodeIndex - 1 == index) {
            vGap = minimalDistanceBetweenChildren;
        } else if (childRegularHeight != 0) {
            vGap = summarizedNodeDistance(minimalDistanceBetweenChildren);
        }
    }

    private int adjustForRightChildrenWithHandles(NodeViewLayoutHelper child, int extraVGap) {
        int added = 0;
        if (!child.paintsChildrenOnTheLeft() && view.usesHorizontalLayout()) {
            int missingWidth2 = child.getMinimumDistanceConsideringHandles() - vGap - extraVGap;
            if (missingWidth2 > 0) {
                y += missingWidth2;
                if (!isFirstVisibleLaidOutChild()) {
                    added += missingWidth2;
                }
            }
        }
        return added;
    }

    private void updateTotalSideShiftYForAlignment(NodeViewLayoutHelper child, int childContentHeightSum, int additionalCloudHeight, int upperGap, int extraVGap, int childRegularHeight, int distance, int y) {
        final int sideShiftY;
        switch (childNodesAlignment) {
            case FLOW:
            case AUTO:
                childContentHeightSum += child.getContentHeight() + additionalCloudHeight;
                sideShiftY = (child.getContentY() - spaceAround - child.getTopOverlap()) + upperGap + childContentHeightSum / 2;
                break;
            case AFTER_PARENT:
            case FIRST_CHILD_BY_PARENT:
            case NOT_SET:
            case STACKED_AUTO:
                return;
            default:
                childContentHeightSum += childRegularHeight;
                switch (childNodesAlignment) {
                    case BY_CENTER:
                        sideShiftY = childContentHeightSum / 2 + upperGap;
                        break;
                    default:
                        sideShiftY = childContentHeightSum + extraVGap;
                        break;
                }
        }
        if (distance <= 0) {
            if (bottomY == y)
                totalSideShiftY -= sideShiftY;
        } else if (sideShiftY > distance)
            totalSideShiftY -= sideShiftY - distance;
    }

    private void assignSummaryChildVerticalPosition(int index,
                                   NodeViewLayoutHelper child,
                                   int childRegularHeight,
                                   int childShiftY) {
        int itemLevel = level - 1;
        if (child.isFirstGroupNode()) {
            initializeGroupStartIndex(itemLevel);
        }

        initializeGroupCoordinates(itemLevel);

        int summaryY = calculateSummaryChildVerticalPosition(
                groupUpperYCoordinate[itemLevel],
                groupLowerYCoordinate[itemLevel],
                child,
                childShiftY);

        if (!child.isFree()) {
            yCoordinates[index] = summaryY;

            int deltaY = summaryY - groupUpperYCoordinate[itemLevel];

            if (isAutoCompactLayoutEnabled && groupStartBoundaries[itemLevel] != null) {
                deltaY += adjustForAutoCompactLayout(index, child, itemLevel);
            }

            if (deltaY < 0) {
                handleNegativeDeltaY(index, itemLevel, deltaY);
                summaryY -= deltaY;
            }

            if (childRegularHeight != 0) {
                updateBottomBoundary(child, index, summaryY + minimalDistanceBetweenChildren, CombineOperation.MAX);
                summaryY += childRegularHeight + minimalDistanceBetweenChildren;
            }
            y = Math.max(y, summaryY);
        }
    }

    private void initializeGroupStartIndex(int itemLevel) {
        groupStartIndex[level] = groupStartIndex[itemLevel];
    }

    private void initializeGroupCoordinates(int itemLevel) {
        if (groupUpperYCoordinate[itemLevel] == Integer.MAX_VALUE) {
            groupUpperYCoordinate[itemLevel] = y;
            groupLowerYCoordinate[itemLevel] = y;
        }
    }

    private int adjustForAutoCompactLayout(int index, NodeViewLayoutHelper child, int itemLevel) {
        StepFunction childTopBoundary = child.getTopBoundary();
        if (childTopBoundary != null) {
            childTopBoundary = childTopBoundary.translate(xCoordinates[index], y);
            int distance = childTopBoundary.distance(groupStartBoundaries[itemLevel]) - minimalDistanceBetweenChildren;
            if (distance < 0) {
                return distance;
            }
        }
        return 0;
    }

    private void handleNegativeDeltaY(int index, int itemLevel, int deltaY) {
        if (childNodesAlignment == ChildNodesAlignment.FLOW) {
            totalSideShiftY += deltaY;
        }
        y -= deltaY;
        bottomBoundary = groupStartBoundaries[itemLevel];

        // Adjust coordinates for all group items
        for (int j = groupStartIndex[itemLevel]; j <= index; j++) {
            NodeViewLayoutHelper groupItem = view.getComponent(j);
            if (groupItem.isLeft() == currentSideLeft
                    && (viewLevels.summaryLevels[j] > 0
                    || !isChildFreeNode[j])) {
                yCoordinates[j] -= deltaY;
                if (j != index) {
                    yBottomCoordinates[j] -= deltaY;
                    updateBottomBoundary(view.getComponent(j), j, yBottomCoordinates[j], CombineOperation.FALLBACK);
                }
            }
        }
    }

    private void updateBottomBoundary(NodeViewLayoutHelper child, int index, int y, CombineOperation combineOperation) {
        if(isAutoCompactLayoutEnabled) {
            StepFunction childBottomBoundary = child.getBottomBoundary();
            if(childBottomBoundary != null) {
                childBottomBoundary = childBottomBoundary.translate(xCoordinates[index], y);
                bottomBoundary = bottomBoundary == null ? childBottomBoundary : childBottomBoundary.combine(bottomBoundary, combineOperation);
            }
        }
    }

    private int calculateSummaryChildVerticalPosition(int groupUpper, int groupLower,
            NodeViewLayoutHelper child, int childShiftY) {
        int childCloudHeight = CloudHeightCalculator.INSTANCE.getAdditionalCloudHeight(child);
        int childContentHeight = child.getContentHeight() + childCloudHeight;
        return (groupUpper + groupLower) / 2
                - childContentHeight / 2 + childShiftY
                - (child.getContentY() - childCloudHeight / 2 - spaceAround);
    }

    private void updateSummaryGroupBounds(int childIndex,
            NodeViewLayoutHelper child,
            int level,
            boolean isItem,
            int childRegularHeight) {
        int childUpper = yCoordinates[childIndex];
        int childBottom = yCoordinates[childIndex] + childRegularHeight;
        if (child.isFirstGroupNode()) {
            if (isItem) {
                groupUpperYCoordinate[level] = Integer.MAX_VALUE;
                groupLowerYCoordinate[level] = Integer.MIN_VALUE;
            } else {
                groupUpperYCoordinate[level] = childUpper;
                groupLowerYCoordinate[level] = childBottom;
            }
        }
        else if (childRegularHeight != 0 || isNextNodeSummaryNode(childIndex)) {
            groupUpperYCoordinate[level] = Math.min(groupUpperYCoordinate[level], childUpper);
            groupLowerYCoordinate[level] = Math.max(childBottom, groupLowerYCoordinate[level]);
        }
    }

    private void applyLayoutToChildComponents() {
        int spaceAround = view.getSpaceAround();
        int cloudHeight = CloudHeightCalculator.INSTANCE.getAdditionalCloudHeight(view);
        int leftMostX = IntStream.of(xCoordinates).min().orElse(0);
        int contentX = Math.max(spaceAround, -leftMostX);
        int contentY = spaceAround + cloudHeight/2 - Math.min(0, totalShiftY);
        view.setContentVisible(view.isContentVisible());
        int baseY = contentY - spaceAround + totalShiftY;
        final int minYFree = calculateMinYFree(contentY, cloudHeight);
        final int minYRegular = calculateMinYRegular(baseY);
        final int shift = Math.min(minYRegular, minYFree);
        if (shift < 0) {
            contentY -= shift;
            baseY -= shift;
        }
        int topOverlap = Math.max(0, minYRegular-minYFree);
        arrangeChildComponents(contentX, contentY, baseY, cloudHeight, spaceAround, topOverlap);
    }

    private int calculateMinYFree(int contentY, int cloudHeight) {
        int minYFree = 0;
        for (int i = 0; i < childViewCount; i++) {
            final int topOverlap = view.getComponent(i).getTopOverlap();
            if (viewLevels.summaryLevels[i] == 0 && isChildFreeNode[i]) {
                minYFree = Math.min(minYFree, contentY + yCoordinates[i] - cloudHeight/2 - topOverlap);
            }
        }
        return minYFree;
    }

    private int calculateMinYRegular(int baseY) {
        int minYRegular = 0;
        for (int i = 0; i < childViewCount; i++) {
            final int topOverlap = view.getComponent(i).getTopOverlap();
            if (viewLevels.summaryLevels[i] != 0 || !isChildFreeNode[i]) {
                minYRegular = Math.min(minYRegular, baseY + yCoordinates[i] - topOverlap);
            }
        }
        return minYRegular;
    }

    private void arrangeChildComponents(int contentX, int contentY,
                                int baseY, int cloudHeight,
                                int spaceAround, int topOverlap) {
        view.setContentBounds(contentX, contentY, contentSize.width, contentSize.height);
        int width = contentX + contentSize.width + spaceAround;
        final int cloudExtra = cloudHeight/2;
        int height = contentY + contentSize.height + cloudExtra + spaceAround;
        int heightWithoutOverlap = height;
        for (int i = 0; i < childViewCount; i++) {
            NodeViewLayoutHelper child = view.getComponent(i);
            boolean free = isChildFreeNode[i];
            final int childTopOverlap = view.getComponent(i).getTopOverlap();
            int y = (viewLevels.summaryLevels[i] == 0 && free)
                    ? contentY + yCoordinates[i] - childTopOverlap
                    : baseY + yCoordinates[i] - childTopOverlap;
            if (!free) {
                heightWithoutOverlap = Math.max(
                        heightWithoutOverlap,
                        y + child.getHeight() + cloudExtra
                                - child.getBottomOverlap());
            }
            int x = contentX + xCoordinates[i];
            child.setLocation(x, y);
            width = Math.max(width, x + child.getWidth());
            height = Math.max(height, y + child.getHeight() + cloudExtra);
        }
        view.setSize(width, height);
        view.setTopOverlap(topOverlap);
        view.setBottomOverlap(height - heightWithoutOverlap);
        NodeViewLayoutHelper parentView = view.getParentView();
        if(parentView != null && isAutoCompactLayoutEnabled && width > spaceAround && height > spaceAround) {
            StepFunction viewTopBoundary;
            StepFunction viewBottomBoundary;
            if(view.usesHorizontalLayout() == parentView.usesHorizontalLayout()) {
                final int segmentStart = contentX;
                final int segmentEnd = contentX + contentSize.width;
                viewBottomBoundary = contentSize.width <= 0 ? null : StepFunction.segment(segmentStart, segmentEnd, contentY + contentSize.height + cloudExtra);
                viewBottomBoundary = leftBottomBoundary == null ? viewBottomBoundary :
                    viewBottomBoundary == null ? leftBottomBoundary.translate(contentX, baseY) :
                        viewBottomBoundary.combine(leftBottomBoundary.translate(contentX, baseY), CombineOperation.MAX);
                viewBottomBoundary = rightBottomBoundary == null ? viewBottomBoundary :
                    viewBottomBoundary == null ? rightBottomBoundary.translate(contentX, baseY) :
                        viewBottomBoundary.combine(rightBottomBoundary.translate(contentX, baseY), CombineOperation.MAX);

                viewTopBoundary = contentSize.width <= 0 ? null : StepFunction.segment(segmentStart, segmentEnd, contentY - cloudExtra);
                for (int i = childViewCount - 1; i >= 0; i--) {
                    NodeViewLayoutHelper child = view.getComponent(i);
                    StepFunction childTopBoundary = child.getTopBoundary();
                    if(childTopBoundary != null)
                        childTopBoundary = childTopBoundary.translate(child.getX(), child.getY());
                    viewTopBoundary = childTopBoundary == null ? viewTopBoundary :
                        viewTopBoundary == null ? childTopBoundary
                                : viewTopBoundary.combine(childTopBoundary , CombineOperation.MIN);
                }
            }
            else {
                viewTopBoundary = StepFunction.segment(spaceAround, width - 2 * spaceAround, spaceAround);
                viewBottomBoundary = StepFunction.segment(spaceAround, width - 2 * spaceAround, height - spaceAround);
            }
            view.setTopBoundary(viewTopBoundary);
            view.setBottomBoundary(viewBottomBoundary);
        }
    }

    private boolean isFirstVisibleLaidOutChild() {
        return visibleLaidOutChildCounter == 0;
    }
}
