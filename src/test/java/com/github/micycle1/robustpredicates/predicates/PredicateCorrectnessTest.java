package com.github.micycle1.robustpredicates.predicates;

import com.github.micycle1.robustpredicates.reference.BigDecimalReference;
import com.github.micycle1.robustpredicates.reference.InputGenerators;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The full staged chains (A -> B -> D) must equal the exact oracle on every
 * input and never return an uncertain sign.
 */
class PredicateCorrectnessTest {

    @Test
    void orient2dFullChain() {
        Random rng = new Random(31);
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 6, 1000));
        List<double[]> collinear = InputGenerators.collinear2d(rng, 300);
        inputs.addAll(collinear);
        inputs.addAll(InputGenerators.perturbAll(collinear, rng, 6));
        for (double[] args : inputs) {
            assertEquals(BigDecimalReference.orient2d(args),
                    RobustPredicatesInterpreted.ORIENT2D.apply(args));
        }
    }

    @Test
    void orient2dKettnerGrid() {
        // Kettner-style scan: move point a over a grid of ulp offsets around a
        // near-collinear configuration; the sign map must match the oracle.
        double ax = 0.5;
        double ay = 0.5;
        double[] base = {ax, ay, 12.0, 12.0, 24.0, 24.0};
        for (int i = 0; i < 96; i++) {
            for (int j = 0; j < 96; j++) {
                double[] args = base.clone();
                args[0] = offsetUlps(ax, i - 48);
                args[1] = offsetUlps(ay, j - 48);
                assertEquals(BigDecimalReference.orient2d(args),
                        RobustPredicatesInterpreted.ORIENT2D.apply(args));
            }
        }
    }

    private static double offsetUlps(double v, int k) {
        double out = v;
        for (int i = 0; i < Math.abs(k); i++) {
            out = k > 0 ? Math.nextUp(out) : Math.nextDown(out);
        }
        return out;
    }

    @Test
    void orient3dFullChain() {
        Random rng = new Random(32);
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 12, 500));
        List<double[]> coplanar = InputGenerators.coplanar(rng, 200);
        inputs.addAll(coplanar);
        inputs.addAll(InputGenerators.perturbAll(coplanar, rng, 6));
        for (double[] args : inputs) {
            assertEquals(BigDecimalReference.orient3d(args),
                    RobustPredicatesInterpreted.ORIENT3D.apply(args));
        }
    }

    @Test
    void incircleFullChain() {
        Random rng = new Random(33);
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 8, 500));
        List<double[]> cocircular = InputGenerators.cocircular(rng, 200);
        inputs.addAll(cocircular);
        inputs.addAll(InputGenerators.perturbAll(cocircular, rng, 6));
        inputs.add(InputGenerators.notebookIncircle());
        for (double[] args : inputs) {
            assertEquals(BigDecimalReference.incircle(args),
                    RobustPredicatesInterpreted.INCIRCLE.apply(args));
        }
    }

    @Test
    void insphereFullChain() {
        Random rng = new Random(34);
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 15, 200));
        List<double[]> cospherical = InputGenerators.cospherical(rng, 100);
        inputs.addAll(cospherical);
        inputs.addAll(InputGenerators.perturbAll(cospherical, rng, 6));
        for (double[] args : inputs) {
            assertEquals(BigDecimalReference.insphere(args),
                    RobustPredicatesInterpreted.INSPHERE.apply(args));
        }
    }

    @Test
    void namedEntryPointsAgreeWithArrayForm() {
        assertEquals(1, RobustPredicatesInterpreted.orient2d(0, 0, 1, 0, 0, 1));
        assertEquals(1, RobustPredicatesInterpreted.orient3d(
                0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1));
        assertEquals(1, RobustPredicatesInterpreted.incircle(0, 0, 1, 0, 0, 1, 0.25, 0.25));
        assertEquals(-1, RobustPredicatesInterpreted.insphere(
                1, 0, 0, -1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0));
        assertEquals(0, RobustPredicatesInterpreted.incircle(5, 0, 0, 5, -5, 0, 3, -4));
    }
}
