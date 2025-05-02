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

import java.util.Set;
import org.junit.Test;

public class StepFunctionTest {
    StepFunction base = StepFunction.segment(0, 10, 8);

    @Test
    public void testSegmentEvaluate() {
        assertThat(base.evaluate(-1)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(base.evaluate(0)).isEqualTo(8);
        assertThat(base.evaluate(10)).isEqualTo(8);
        assertThat(base.evaluate(11)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testRightExclusiveSemantics() {
        StepFunction f = StepFunction.segment(5, 6, 42);
        assertThat(f.evaluate(4)).isEqualTo(StepFunction.DEFAULT_VALUE);  // right edge excluded
        assertThat(f.evaluate(5)).isEqualTo(42);                          // left edge
        assertThat(f.evaluate(6)).isEqualTo(42);                          // left edge
        assertThat(f.evaluate(7)).isEqualTo(StepFunction.DEFAULT_VALUE);  // right edge excluded
    }

    @Test
    public void testTranslateSingle() {
        StepFunction t = base.translate(5, 2);
        assertThat(t.evaluate(4)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(t.evaluate(5)).isEqualTo(8 + 2);
        assertThat(t.evaluate(15)).isEqualTo(8 + 2);
        assertThat(t.evaluate(16)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testNestedTranslationsAccumulate() {
        StepFunction t = base.translate(2, 1).translate(3, 4);
        // total dx=5, dy=5
        assertThat(t.evaluate(4)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(t.evaluate(5)).isEqualTo(8 + 5);
        assertThat(t.evaluate(15)).isEqualTo(8 + 5);
        assertThat(t.evaluate(16)).isEqualTo(StepFunction.DEFAULT_VALUE);
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
        assertThat(maxed.evaluate(11)).isEqualTo(3);

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

    @Test
    public void testSamplePointsSegment() {
        StepFunction s = StepFunction.segment(2, 7, 5);
        assertThat(s.samplePoints()).containsExactlyInAnyOrder(2, 7);
    }

    @Test
    public void testSamplePointsTranslated() {
        StepFunction t = StepFunction.segment(2, 7, 5).translate(3, 1);
        // endpoints shifted by dx only
        assertThat(t.samplePoints()).containsExactlyInAnyOrder(5, 10);
    }

    @Test
    public void testSamplePointsCombinedMax() {
        StepFunction base = StepFunction.segment(0, 10, 8);
        StepFunction s2 = StepFunction.segment(5, 15, 3);
        StepFunction maxed = base.combine(s2, CombineOperation.MAX);
        // only points where the max-survivor comes from left or right
        assertThat(maxed.samplePoints()).containsExactlyInAnyOrder(0, 10, 15);
    }

    @Test
    public void testSamplePointsCombinedMin() {
        StepFunction base = StepFunction.segment(0, 10, 8);
        StepFunction s2 = StepFunction.segment(5, 15, 3);
        StepFunction mined = base.combine(s2, CombineOperation.MIN);
        // only points where the min-survivor comes from left or right
        assertThat(mined.samplePoints()).containsExactlyInAnyOrder(0, 5, 15);
    }

    @Test
    public void testSamplePointsCombinedMaxEqualOverlap() {
        StepFunction f1 = StepFunction.segment(0, 10, 8);
        StepFunction f2 = StepFunction.segment(5, 15, 8);
        StepFunction combined = f1.combine(f2, CombineOperation.MAX);
        assertThat(combined.samplePoints()).containsExactlyInAnyOrder(0, 15);
    }

    @Test
    public void testSamplePointsCombinedMinEqualOverlap() {
        StepFunction f1 = StepFunction.segment(0, 10, 8);
        StepFunction f2 = StepFunction.segment(5, 15, 8);
        StepFunction combined = f1.combine(f2, CombineOperation.MIN);
        assertThat(combined.samplePoints()).containsExactlyInAnyOrder(0, 15);
    }

    @Test
    public void testSamplePointsCache() {
        StepFunction f1 = StepFunction.segment(0, 10, 8);
        StepFunction f2 = StepFunction.segment(5, 15, 3);
        StepFunction combined = f1.combine(f2, CombineOperation.MAX);
        Set<Integer> pts1 = combined.samplePoints();
        Set<Integer> pts2 = combined.samplePoints();
        assertThat(pts2).isSameAs(pts1);
    }

    @Test
    public void testCombineGapBehaviourMax() {
        StepFunction f = StepFunction.segment(0, 2, 10)
            .combine(StepFunction.segment(5, 7, 20), CombineOperation.MAX);
        // gap between x=2 and x=5: should return min(10, 20) = 10
        assertThat(f.evaluate(3)).isEqualTo(10);
        // ensure endpoints remain correct
        assertThat(f.evaluate(2)).isEqualTo(10);
        assertThat(f.evaluate(5)).isEqualTo(20);
    }

    @Test
    public void testCombineGapBehaviourMin() {
        StepFunction f = StepFunction.segment(0, 2, 10)
            .combine(StepFunction.segment(5, 7, 20), CombineOperation.MIN);
        // gap between x=2 and x=5: should return max(10, 20) = 20
        assertThat(f.evaluate(3)).isEqualTo(20);
        // ensure endpoints remain correct
        assertThat(f.evaluate(2)).isEqualTo(10);
        assertThat(f.evaluate(5)).isEqualTo(20);
    }

    @Test
    public void testCombineConstructorSwapsArgumentsMax() {
        StepFunction first = StepFunction.segment(5, 7, 20);
        StepFunction second = StepFunction.segment(0, 2, 10);
        StepFunction f = first.combine(second, CombineOperation.MAX);
        // swapped so behaves like (0,2)->(5,7)
        assertThat(f.evaluate(1)).isEqualTo(10);
        assertThat(f.evaluate(6)).isEqualTo(20);
        // gap returns min(10,20)
        assertThat(f.evaluate(3)).isEqualTo(10);
    }

    @Test
    public void testCombineConstructorSwapsArgumentsMin() {
        StepFunction first = StepFunction.segment(5, 7, 20);
        StepFunction second = StepFunction.segment(0, 2, 10);
        StepFunction f = first.combine(second, CombineOperation.MIN);
        // swapped so behaves like (0,2)->(5,7)
        assertThat(f.evaluate(1)).isEqualTo(10);
        assertThat(f.evaluate(6)).isEqualTo(20);
        // gap returns max(10,20)
        assertThat(f.evaluate(3)).isEqualTo(20);
    }

    @Test
    public void testFlattenedNestedTranslateEvaluate() {
        StepFunction base = StepFunction.segment(1, 2, 3);
        StepFunction nested = base.translate(2, 3).translate(4, 5);
        // total dx=6, dy=8
        assertThat(nested.evaluate(1 + 6)).isEqualTo(3 + 8);
        assertThat(nested.evaluate(2 + 6)).isEqualTo(3 + 8);
        // outside range remains undefined
        assertThat(nested.evaluate(1 + 5)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }

    @Test
    public void testFlattenedNestedTranslateSamplePoints() {
        StepFunction base = StepFunction.segment(2, 7, 5);
        StepFunction nested = base.translate(3, 1).translate(4, 2);
        // endpoints shifted by total dx only
        assertThat(nested.samplePoints()).containsExactlyInAnyOrder(2 + 3 + 4, 7 + 3 + 4);
    }

    @Test
    public void testCombineFallback() {
        StepFunction main = StepFunction.segment(0, 5, 10);
        StepFunction fallback = StepFunction.segment(3, 8, 20);
        StepFunction combined = main.combine(fallback, CombineOperation.FALLBACK);

        // x=2: main defined -> 10
        assertThat(combined.evaluate(2)).isEqualTo(10);
        // x=4: main defined -> 10 (even though fallback is defined)
        assertThat(combined.evaluate(4)).isEqualTo(10);
        // x=6: main undefined, fallback defined -> 20
        assertThat(combined.evaluate(6)).isEqualTo(20);
        // x=9: outside combined range -> DEFAULT_VALUE
        assertThat(combined.evaluate(9)).isEqualTo(StepFunction.DEFAULT_VALUE);
        // x=-1: outside combined range -> DEFAULT_VALUE 
        assertThat(combined.evaluate(-1)).isEqualTo(StepFunction.DEFAULT_VALUE);
        
        // Edge case: right at the combined maxX
        assertThat(combined.evaluate(8)).isEqualTo(20);
        // Edge case: right at the combined minX
        assertThat(combined.evaluate(0)).isEqualTo(10);
    }

    @Test
    public void testCombineFallbackWithGaps() {
        StepFunction main = StepFunction.segment(2, 4, 10);
        StepFunction fallback = StepFunction.segment(6, 8, 20);
        StepFunction combined = main.combine(fallback, CombineOperation.FALLBACK);

        // within main domain
        assertThat(combined.evaluate(2)).isEqualTo(10);
        assertThat(combined.evaluate(4)).isEqualTo(10);

        // within fallback domain
        assertThat(combined.evaluate(6)).isEqualTo(20);
        assertThat(combined.evaluate(8)).isEqualTo(20);

        // in the gap between main and fallback: x=5 -> fallback at fallback.minX (6)
        assertThat(combined.evaluate(5)).isEqualTo(20);

        // outside combined domain: <2 or >8 -> DEFAULT_VALUE
        assertThat(combined.evaluate(1)).isEqualTo(StepFunction.DEFAULT_VALUE);
        assertThat(combined.evaluate(9)).isEqualTo(StepFunction.DEFAULT_VALUE);
    }
}
