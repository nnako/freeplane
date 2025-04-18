/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2025 Contributors
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

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * The StepFunction2 class represents a step function y(x) using a linked list structure.
 * Each node represents a segment [x1, x2) with a constant y value.
 * Segments do not overlap, and the linked list is maintained in ascending x order.
 */
class StepFunction {
    public static final int DEFAULT_VALUE = Integer.MAX_VALUE;

    enum CombineOperation {
        MAX, MIN
    }

    private final int x1;        // Start of segment (inclusive)
    private final int x2;        // End of segment (exclusive)
    private final int y;         // Value in this segment
    private final int dx;        // X-offset for translation
    private final int dy;        // Y-offset for translation

    private final StepFunction next;

    // Operation to apply when combining with next segment
    private final CombineOperation combineOperation;

    StepFunction(int x1, int x2, int y) {
        this(x1, x2, y, 0, 0, null, null);
    }

    private StepFunction(int x1, int x2, int y, int dx, int dy, StepFunction next, CombineOperation combineOperation) {
        this.x1 = x1 < x2 ? x1 : x2 - 1;
        this.x2 = x2;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.next = next;
        this.combineOperation = combineOperation;
    }
    public int evaluate(int x) {
        int adjX = x - dx;
        boolean inThis  = adjX >= x1 && adjX < x2;
        int     curr    = inThis ? y + dy : DEFAULT_VALUE;

        int child = next == null
            ? DEFAULT_VALUE
            : next.evaluate(adjX);
        if (child != DEFAULT_VALUE) child += dy;

        if (curr == DEFAULT_VALUE)             return child;
        if (child == DEFAULT_VALUE)            return curr;
        return combineOperation == CombineOperation.MAX
             ? Math.max(curr, child)
             : Math.min(curr, child);
    }

    public int distance(StepFunction other) {
        if (other == null) {
            return 0;
        }

        Set<Integer> samplePoints = new HashSet<>(this.samplePoints());
        samplePoints.addAll(other.samplePoints());

        int minDistance = DEFAULT_VALUE;
        for (int x : samplePoints) {
            int y1 = this.evaluate(x);
            int y2 = other.evaluate(x);
            if (y1 != DEFAULT_VALUE && y2 != DEFAULT_VALUE) {
                int d = y1 - y2;
                minDistance = (minDistance == DEFAULT_VALUE) ? d : Math.min(minDistance, d);
            }
        }
        return minDistance;
    }

    // Helper to collect all true segment boundaries (with correct dx accumulation)
    private Set<Integer> samplePoints() {
        Set<Integer> points = new TreeSet<>();
        int cumDx = 0;
        StepFunction cur = this;
        while (cur != null) {
            cumDx += cur.dx;                     // add this node’s offset exactly once
            points.add(cur.x1 + cumDx);          // left‑inclusive
            points.add(cur.x2 + cumDx);          // right‑exclusive boundary
            cur = cur.next;
        }
        return points;
    }

    public StepFunction addSegmentTakingMax(int x1, int x2, int y) {
        return new StepFunction(x1, x2, y, 0, 0, this, CombineOperation.MAX);
    }

    public StepFunction addSegmentTakingMin(int x1, int x2, int y) {
        return new StepFunction(x1, x2, y, 0, 0, this, CombineOperation.MIN);
    }

    public StepFunction translate(int dx, int dy) {
        return new StepFunction(x1, x2, y, this.dx + dx, this.dy + dy, next, combineOperation);
    }

	@Override
	public String toString() {
		return "StepFunction2 [x1=" + x1 + ", x2=" + x2 + ", y=" + y + ", dx=" + dx + ", dy=" + dy
				+ ", combineOperation=" + combineOperation + ", next=" + next + "]";
	}
}