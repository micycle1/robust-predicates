package com.github.micycle1.robustpredicates.filter;

import com.github.micycle1.robustpredicates.expr.Expressions;
import com.github.micycle1.robustpredicates.reference.BigDecimalReference;
import com.github.micycle1.robustpredicates.reference.InputGenerators;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticFilterTest {

    @Test
    void defaultFilterIsAlwaysUncertain() {
        StaticFilter filter = new StaticFilter(Expressions.ORIENT2D);
        assertEquals(Double.POSITIVE_INFINITY, filter.bound());
        assertEquals(Sign.UNCERTAIN, filter.apply(new double[] {0, 0, 1, 0, 0, 1}));
    }

    @Test
    void staticBoundDominatesSemiStaticBound() {
        // Within the declared domain the static bound must be an upper bound
        // of every per-call semi-static bound.
        Random rng = new Random(41);
        double[] maxima = new double[6];
        double[] minima = new double[6];
        Arrays.fill(maxima, 10.0);
        Arrays.fill(minima, -10.0);
        StaticFilter staticFilter = new StaticFilter(Expressions.ORIENT2D, maxima, minima);
        SemiStaticFilter semiStatic = new SemiStaticFilter(Expressions.ORIENT2D);
        assertTrue(Double.isFinite(staticFilter.bound()));
        for (int i = 0; i < 5000; i++) {
            double[] args = InputGenerators.random(rng, 6, 10.0, 0.0);
            assertTrue(staticFilter.bound() >= semiStatic.errorBound(args),
                    "static bound must dominate semi-static bound");
        }
    }

    @Test
    void staticFilterIsSoundAndEffective() {
        Random rng = new Random(42);
        double[] maxima = new double[6];
        double[] minima = new double[6];
        Arrays.fill(maxima, 10.0);
        Arrays.fill(minima, -10.0);
        StaticFilter filter = new StaticFilter(Expressions.ORIENT2D, maxima, minima);
        int certain = 0;
        int total = 0;
        for (int i = 0; i < 20_000; i++) {
            double[] args = InputGenerators.random(rng, 6, 10.0, 0.0);
            int result = filter.apply(args);
            total++;
            if (result != Sign.UNCERTAIN) {
                certain++;
                assertEquals(BigDecimalReference.orient2d(args), result);
            }
        }
        assertTrue(certain > 0.99 * total,
                "static filter should resolve most random inputs, got "
                        + certain + "/" + total);
    }

    @Test
    void incircleStaticFilterSound() {
        Random rng = new Random(43);
        double[] maxima = new double[8];
        double[] minima = new double[8];
        Arrays.fill(maxima, 6.0);
        Arrays.fill(minima, -6.0);
        StaticFilter filter = new StaticFilter(Expressions.INCIRCLE, maxima, minima);
        List<double[]> inputs = InputGenerators.perturbAll(
                InputGenerators.cocircular(rng, 300), rng, 4);
        for (double[] args : inputs) {
            int result = filter.apply(args);
            if (result != Sign.UNCERTAIN) {
                assertEquals(BigDecimalReference.incircle(args), result);
            }
        }
        for (int i = 0; i < 2000; i++) {
            double[] args = InputGenerators.random(rng, 8, 6.0, 0.0);
            int result = filter.apply(args);
            if (result != Sign.UNCERTAIN) {
                assertEquals(BigDecimalReference.incircle(args), result);
            }
        }
    }

    @Test
    void almostStaticFilterWidensWithUpdates() {
        Random rng = new Random(44);
        AlmostStaticFilter filter = new AlmostStaticFilter(Expressions.ORIENT2D);
        assertEquals(Sign.UNCERTAIN, filter.apply(new double[] {0, 0, 1, 0, 0, 1}));

        // Feed inputs; after updates the filter must decide well-separated
        // signs and stay sound.
        for (int i = 0; i < 200; i++) {
            filter.update(InputGenerators.random(rng, 6, 5.0, 0.0));
        }
        double bound5 = filter.bound();
        assertTrue(Double.isFinite(bound5));
        int result = filter.apply(new double[] {0, 0, 4, 0, 0, 4});
        assertEquals(1, result);

        // Widening the domain can only grow the bound.
        for (int i = 0; i < 200; i++) {
            filter.update(InputGenerators.random(rng, 6, 500.0, 0.0));
        }
        assertTrue(filter.bound() >= bound5);
        for (int i = 0; i < 2000; i++) {
            double[] args = InputGenerators.random(rng, 6, 500.0, 0.0);
            int r = filter.apply(args);
            if (r != Sign.UNCERTAIN) {
                assertEquals(BigDecimalReference.orient2d(args), r);
            }
        }
    }
}
