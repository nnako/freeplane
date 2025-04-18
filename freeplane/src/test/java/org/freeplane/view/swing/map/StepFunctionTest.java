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
    StepFunction base = StepFunction.segment(0, 10, 8);

    @Test
    public void testSegmentEvaluate() {
        assertThat(base.evaluate(-1)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(base.evaluate(0)).isEqualTo(8);
        assertThat(base.evaluate(9)).isEqualTo(8);
        assertThat(base.evaluate(10)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testRightExclusiveSemantics() {
        StepFunction f = StepFunction.segment(5, 6, 42);
        assertThat(f.evaluate(5)).isEqualTo(42);                          // left edge
        assertThat(f.evaluate(6)).isEqualTo(StepFunction.DEFAULT_VALUE);  // right edge excluded
    }

    @Test
    public void testTranslateSingle() {
        StepFunction t = base.translate(5, 2);
        assertThat(t.evaluate(4)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(t.evaluate(5)).isEqualTo(8 + 2);
        assertThat(t.evaluate(14)).isEqualTo(8 + 2);
        assertThat(t.evaluate(15)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testNestedTranslationsAccumulate() {
        StepFunction t = base.translate(2, 1).translate(3, 4);
        // total dx=5, dy=5
        assertThat(t.evaluate(4)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(t.evaluate(5)).isEqualTo(8 + 5);
        assertThat(t.evaluate(14)).isEqualTo(8 + 5);
        assertThat(t.evaluate(15)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testCombineMaxAndMin() {
        StepFunction s2 = StepFunction.segment(5, 15, 3);
        StepFunction maxed = base.combine(s2, CombineOperation.MAX);
        // x=2: only base defined →8
        assertThat(maxed.evaluate(2)).isEqualTo(8);
        // x=6: both defined → max(8,3)=8
        assertThat(maxed.evaluate(6)).isEqualTo(8);
        // x=10: base undef, s2 defined →3
        assertThat(maxed.evaluate(10)).isEqualTo(3);

        StepFunction mined = base.combine(s2, CombineOperation.MIN);
        // x=6: min(8,3)=3
        assertThat(mined.evaluate(6)).isEqualTo(3);
        // x=2: only base defined →8
        assertThat(mined.evaluate(2)).isEqualTo(8);
    }

    @Test
    public void testEvaluateDefersToNextWhenHeadUndefined() {
        // combine behaves like chain: head then next
        StepFunction f = StepFunction.segment(0, 5, 10)
            .combine(StepFunction.segment(5, 10, 20), CombineOperation.MAX);
        assertThat(f.evaluate(3)).isEqualTo(10);
        assertThat(f.evaluate(7)).isEqualTo(20);
        assertThat(f.evaluate(12)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testCombineWhenNextUndefined() {
        StepFunction f = StepFunction.segment(0, 5, 10)
            .combine(StepFunction.segment(0, 2, 20), CombineOperation.MAX);
        // x=3: head covers →10; other returns DEFAULT →10
        assertThat(f.evaluate(3)).isEqualTo(10);
    }

    @Test
    public void testCombineWhenCurrentUndefined() {
        StepFunction f = StepFunction.segment(0, 2, 20)
            .combine(StepFunction.segment(0, 5, 10), CombineOperation.MAX);
        // x=3: head gives 10, other undef →10
        assertThat(f.evaluate(3)).isEqualTo(10);
        // x=1: head=10, other=20 → max=20
        assertThat(f.evaluate(1)).isEqualTo(20);
    }

    @Test
    public void testDistanceWithUnalignedEndpoints() {
        StepFunction f = StepFunction.segment(0, 10, 5);
        StepFunction g = StepFunction.segment(5, 15, 3);
        // overlap on [5,10): f=5, g=3 → distance=2
        assertThat(f.distance(g)).isEqualTo(2);
        // reversed: -2
        assertThat(g.distance(f)).isEqualTo(-2);
    }

    @Test
    public void testDistanceWithTranslatedAndCombined() {
        StepFunction upper = base.translate(0, 2)
                                 .combine(base, CombineOperation.MAX);
        StepFunction lower = base.translate(0, -2)
                                 .combine(base, CombineOperation.MIN);
        assertThat(upper.distance(lower)).isEqualTo(4);
        assertThat(lower.distance(upper)).isEqualTo(-4);
    }
}
