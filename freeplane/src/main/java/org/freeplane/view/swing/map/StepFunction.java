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
import java.util.stream.Collectors;

/**
 * The StepFunction class represents a step function y(x) using a linked list structure.
 * Each node represents a segment [x1, x2) with a constant y value.
 * Segments do not overlap, and the linked list is maintained in ascending x order.
 */
public interface StepFunction {

    int DEFAULT_VALUE = Integer.MAX_VALUE;

    int evaluate(int x);
    Set<Integer> samplePoints();

    default int distance(StepFunction other) {
        if (other == null) return 0;
        Set<Integer> pts = new HashSet<>(samplePoints());
        pts.addAll(other.samplePoints());
        int min = DEFAULT_VALUE;
        for (int x : pts) {
            int y1 = evaluate(x);
            int y2 = other.evaluate(x);
            if (y1 != DEFAULT_VALUE && y2 != DEFAULT_VALUE) {
                int d = y1 - y2;
                min = (min == DEFAULT_VALUE) ? d : Math.min(min, d);
            }
        }
        return min;
    }

    // Static factory for the first (segment) function
    static StepFunction segment(int x1, int x2, int y) {
        return new SegmentFunction(x1, x2, y);
    }

    // Default instance methods for transformation and combination
    default StepFunction translate(int dx, int dy) {
        return new TranslatedFunction(this, dx, dy);
    }

    default StepFunction combine(StepFunction other, CombineOperation op) {
        return new CombinedFunction(this, other, op);
    }
}

//CombineOperation.java
enum CombineOperation {
 MAX, MIN
}

class SegmentFunction implements StepFunction {
    private final int x1;
    private final int x2;
    private final int y;

    public SegmentFunction(int x1, int x2, int y) {
        if (x1 >= x2) throw new IllegalArgumentException();
        this.x1 = x1;
        this.x2 = x2;
        this.y = y;
    }

    @Override
    public int evaluate(int x) {
        return (x >= x1 && x < x2) ? y : DEFAULT_VALUE;
    }

    @Override
    public Set<Integer> samplePoints() {
        Set<Integer> pts = new HashSet<>();
        pts.add(x1);
        pts.add(x2);
        return pts;
    }
}

class TranslatedFunction implements StepFunction {
    private final StepFunction inner;
    private final int dx;
    private final int dy;

    public TranslatedFunction(StepFunction inner, int dx, int dy) {
        if (inner == null) throw new IllegalArgumentException();
        this.inner = inner;
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public int evaluate(int x) {
        int val = inner.evaluate(x - dx);
        return val == DEFAULT_VALUE ? DEFAULT_VALUE : val + dy;
    }

    @Override
    public Set<Integer> samplePoints() {
        return inner.samplePoints().stream()
            .map(p -> p + dx)
            .collect(Collectors.toSet());
    }
}

class CombinedFunction implements StepFunction {
    private final StepFunction left;
    private final StepFunction right;
    private final CombineOperation op;

    public CombinedFunction(StepFunction left, StepFunction right, CombineOperation op) {
        if (left == null || right == null || op == null) throw new IllegalArgumentException();
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public int evaluate(int x) {
        int a = left.evaluate(x);
        int b = right.evaluate(x);
        if (a == DEFAULT_VALUE && b == DEFAULT_VALUE) return DEFAULT_VALUE;
        if (a == DEFAULT_VALUE) return b;
        if (b == DEFAULT_VALUE) return a;
        return op == CombineOperation.MAX
            ? Math.max(a, b)
            : Math.min(a, b);
    }

    @Override
    public Set<Integer> samplePoints() {
        Set<Integer> pts = new HashSet<>(left.samplePoints());
        pts.addAll(right.samplePoints());
        return pts;
    }
}