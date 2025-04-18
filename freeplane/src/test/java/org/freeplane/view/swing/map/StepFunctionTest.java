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
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class StepFunctionTest {
    StepFunction function1 = new StepFunction(0, 10, 8);

    @Test
    public void testEvaluate() {
        assertThat(function1.evaluate(-1)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(function1.evaluate(0)).isEqualTo(8);
        assertThat(function1.evaluate(9)).isEqualTo(8);
        assertThat(function1.evaluate(10)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testDistance() {
        StepFunction upper = function1.translate(0, 2).addSegmentTakingMax(0, 10, 8);
        StepFunction lower = function1.translate(0, -2).addSegmentTakingMin(0, 10, 8);

        assertThat(upper.distance(lower)).isEqualTo(4);
        assertThat(lower.distance(upper)).isEqualTo(-4);
    }

    @Test
    public void testCombineUpperWithOffsets() {
        StepFunction upper = function1.translate(5, 2).addSegmentTakingMax(0, 10, 5);

        // head = [5,15) with y = max(5+2 , 8+2)
        assertThat(upper.evaluate(-1)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(upper.evaluate(0)).isEqualTo(5);
        assertThat(upper.evaluate(4)).isEqualTo(5);
        assertThat(upper.evaluate(5)).isEqualTo(10);
        assertThat(upper.evaluate(9)).isEqualTo(10);
        assertThat(upper.evaluate(10)).isEqualTo(10);
        assertThat(upper.evaluate(14)).isEqualTo(10);
        // right‐exclusive: 15 is just outside
        assertThat(upper.evaluate(15)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testCombineLowerWithOffsets() {
        StepFunction lower = function1.translate(5, 2).addSegmentTakingMin(0, 10, 5);

        // head = [5,15) with y = min(5+2 , 8+2)
        assertThat(lower.evaluate(-1)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(lower.evaluate(0)).isEqualTo(5);
        assertThat(lower.evaluate(4)).isEqualTo(5);
        assertThat(lower.evaluate(5)).isEqualTo(5);
        assertThat(lower.evaluate(9)).isEqualTo(5);
        assertThat(lower.evaluate(10)).isEqualTo(10);
        assertThat(lower.evaluate(14)).isEqualTo(10);
        // right‐exclusive
        assertThat(lower.evaluate(15)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testDefaultValue() {
        StepFunction function2 = function1.translate(12, 1).addSegmentTakingMax(0, 10, 8);

        assertThat(function2.evaluate(-1)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(function2.evaluate(0)).isEqualTo(8);
        assertThat(function2.evaluate(9)).isEqualTo(8);
        assertThat(function2.evaluate(10)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(function2.evaluate(11)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(function2.evaluate(12)).isEqualTo(9);
        assertThat(function2.evaluate(21)).isEqualTo(9);
        // exclusive: 22 is one past the end of [12,22)
        assertThat(function2.evaluate(22)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testRightExclusiveSemantics() {
        StepFunction f = new StepFunction(5, 6, 42);
        assertThat(f.evaluate(5)).isEqualTo(42);                          // left edge
        assertThat(f.evaluate(6)).isEqualTo(StepFunction.DEFAULT_VALUE);  // right edge excluded
    }

    @Test
    public void testTranslateAppliesToAllSegments() {
        StepFunction f = function1
            .addSegmentTakingMax(10, 20, 5)
            .translate(3, 7);

        // first segment [0,10)→8 becomes [3,13)→15
        assertThat(f.evaluate(2)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(f.evaluate(3)).isEqualTo(15);
        assertThat(f.evaluate(12)).isEqualTo(15);
        // second segment [10,20)→5 becomes [13,23)→12, but combine max with 15
        assertThat(f.evaluate(13)).isEqualTo(12);
        assertThat(f.evaluate(22)).isEqualTo(12);
        assertThat(f.evaluate(23)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testDistanceWithUnalignedEndpoints() {
        StepFunction f = new StepFunction(0, 10, 5);
        StepFunction g = new StepFunction(5, 15, 3);

        // overlap on [5,10): f(x)=5, g(x)=3 → distance=2
        assertThat(f.distance(g)).isEqualTo(2);
        // reversed: g.distance(f) = 3 - 5 = -2
        assertThat(g.distance(f)).isEqualTo(-2);
    }

    @Test
    public void testEvaluateDefersToNextWhenHeadUndefined() {
        StepFunction f = new StepFunction(0, 5, 10)
            .addSegmentTakingMax(5, 10, 20);

        assertThat(f.evaluate(3)).isEqualTo(10);
        assertThat(f.evaluate(7)).isEqualTo(20);
        assertThat(f.evaluate(12)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testNestedTranslationsAccumulate() {
        StepFunction f = function1
            .translate(2, 1)
            .translate(3, 4);

        // x < 5 is before the translated segment [5,15)
        assertThat(f.evaluate(4)).isEqualTo(StepFunction.DEFAULT_VALUE);
        // for 5 ≤ x < 15: original y=8 + total dy=5 → 13
        assertThat(f.evaluate(5)).isEqualTo(13);
        assertThat(f.evaluate(14)).isEqualTo(13);
        // x=15 is just outside → default
        assertThat(f.evaluate(15)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testCombineWhenNextUndefined() {
        StepFunction f = new StepFunction(0, 5, 10)
            .addSegmentTakingMax(0, 2, 20);
        // at x=3 head covers →10; next returns DEFAULT →10
        assertThat(f.evaluate(3)).isEqualTo(10);
    }

    @Test
    public void testCombineWhenCurrentUndefined() {
        StepFunction f = new StepFunction(0, 2, 20)
            .addSegmentTakingMax(0, 5, 10);
        // x=3: head=10, next=DEFAULT →10
        assertThat(f.evaluate(3)).isEqualTo(10);
        // x=1: head=10, next=20 → max=20
        assertThat(f.evaluate(1)).isEqualTo(20);
    }
}