package com.github.micycle1.robustpredicates.expansion;

import com.github.micycle1.robustpredicates.expr.Expressions;
import com.github.micycle1.robustpredicates.reference.BigDecimalReference;
import com.github.micycle1.robustpredicates.reference.InputGenerators;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Stage D is exact: its sign must equal the BigDecimal oracle on every input,
 * including exactly degenerate ones. Any mismatch is a bug.
 */
class StageDTest {

    private static void check(StageD stage, List<double[]> inputs,
                              ToIntFunction<double[]> oracle, String label) {
        for (double[] args : inputs) {
            assertEquals(oracle.applyAsInt(args), stage.apply(args),
                    () -> label + " mismatch for " + java.util.Arrays.toString(args));
        }
    }

    private static List<double[]> withPerturbations(List<double[]> exact, Random rng) {
        List<double[]> all = new ArrayList<>(exact);
        all.addAll(InputGenerators.perturbAll(exact, rng, 4));
        return all;
    }

    @Test
    void orient2dMatchesOracle() {
        Random rng = new Random(1);
        StageD stage = new StageD(Expressions.ORIENT2D);
        check(stage, InputGenerators.randomBatch(rng, 6, 500),
                BigDecimalReference::orient2d, "orient2d/random");
        check(stage, withPerturbations(InputGenerators.collinear2d(rng, 200), rng),
                BigDecimalReference::orient2d, "orient2d/collinear");
    }

    @Test
    void orient3dMatchesOracle() {
        Random rng = new Random(2);
        StageD stage = new StageD(Expressions.ORIENT3D);
        check(stage, InputGenerators.randomBatch(rng, 12, 300),
                BigDecimalReference::orient3d, "orient3d/random");
        check(stage, withPerturbations(InputGenerators.coplanar(rng, 150), rng),
                BigDecimalReference::orient3d, "orient3d/coplanar");
    }

    @Test
    void incircleMatchesOracle() {
        Random rng = new Random(3);
        StageD stage = new StageD(Expressions.INCIRCLE);
        check(stage, InputGenerators.randomBatch(rng, 8, 300),
                BigDecimalReference::incircle, "incircle/random");
        check(stage, withPerturbations(InputGenerators.cocircular(rng, 150), rng),
                BigDecimalReference::incircle, "incircle/cocircular");
        check(stage, List.of(InputGenerators.notebookIncircle()),
                BigDecimalReference::incircle, "incircle/notebook");
    }

    @Test
    void insphereMatchesOracle() {
        Random rng = new Random(4);
        StageD stage = new StageD(Expressions.INSPHERE);
        check(stage, InputGenerators.randomBatch(rng, 15, 100),
                BigDecimalReference::insphere, "insphere/random");
        check(stage, withPerturbations(InputGenerators.cospherical(rng, 80), rng),
                BigDecimalReference::insphere, "insphere/cospherical");
    }
}
