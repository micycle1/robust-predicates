package com.github.micycle1.robustpredicates;

import com.github.micycle1.robustpredicates.expansion.StageB;
import com.github.micycle1.robustpredicates.expansion.StageD;
import com.github.micycle1.robustpredicates.expr.Expressions;
import com.github.micycle1.robustpredicates.filter.SemiStaticFilter;
import com.github.micycle1.robustpredicates.predicates.RobustPredicatesInterpreted;
import com.github.micycle1.robustpredicates.reference.InputGenerators;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The generated routines must agree with the interpreted framework bit for
 * bit — per stage (including {@code SIGN_UNCERTAIN} outcomes) and for the
 * full chain — since both are built from the same construction-time plans.
 */
class InterpreterVsGeneratedTest {

    private interface StageFn {
        int apply(double[] a);
    }

    private static void checkAgreement(List<double[]> inputs,
                                       StageFn interpreted, StageFn generated, String label) {
        for (double[] a : inputs) {
            assertEquals(interpreted.apply(a), generated.apply(a),
                    () -> label + " disagreement for " + java.util.Arrays.toString(a));
        }
    }

    private static List<double[]> inputs2d(Random rng) {
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 6, 400));
        List<double[]> collinear = InputGenerators.collinear2d(rng, 150);
        inputs.addAll(collinear);
        inputs.addAll(InputGenerators.perturbAll(collinear, rng, 4));
        return inputs;
    }

    @Test
    void orient2dAllStagesAgree() {
        Random rng = new Random(51);
        List<double[]> inputs = inputs2d(rng);
        SemiStaticFilter stageA = new SemiStaticFilter(Expressions.ORIENT2D);
        StageB stageB = new StageB(Expressions.ORIENT2D);
        StageD stageD = new StageD(Expressions.ORIENT2D);
        checkAgreement(inputs, stageA::apply,
                a -> RobustPredicates.orient2dStageA(a[0], a[1], a[2], a[3], a[4], a[5]),
                "orient2d stage A");
        checkAgreement(inputs, stageB::apply,
                a -> RobustPredicates.orient2dStageB(a[0], a[1], a[2], a[3], a[4], a[5]),
                "orient2d stage B");
        checkAgreement(inputs, stageD::apply,
                a -> RobustPredicates.orient2dStageD(a[0], a[1], a[2], a[3], a[4], a[5]),
                "orient2d stage D");
        checkAgreement(inputs, RobustPredicatesInterpreted.ORIENT2D::apply,
                a -> RobustPredicates.orient2d(a[0], a[1], a[2], a[3], a[4], a[5]),
                "orient2d chain");
    }

    @Test
    void orient3dAllStagesAgree() {
        Random rng = new Random(52);
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 12, 250));
        List<double[]> coplanar = InputGenerators.coplanar(rng, 120);
        inputs.addAll(coplanar);
        inputs.addAll(InputGenerators.perturbAll(coplanar, rng, 4));
        SemiStaticFilter stageA = new SemiStaticFilter(Expressions.ORIENT3D);
        StageB stageB = new StageB(Expressions.ORIENT3D);
        StageD stageD = new StageD(Expressions.ORIENT3D);
        checkAgreement(inputs, stageA::apply,
                a -> RobustPredicates.orient3dStageA(a[0], a[1], a[2], a[3], a[4], a[5],
                        a[6], a[7], a[8], a[9], a[10], a[11]),
                "orient3d stage A");
        checkAgreement(inputs, stageB::apply,
                a -> RobustPredicates.orient3dStageB(a[0], a[1], a[2], a[3], a[4], a[5],
                        a[6], a[7], a[8], a[9], a[10], a[11]),
                "orient3d stage B");
        checkAgreement(inputs, stageD::apply,
                a -> RobustPredicates.orient3dStageD(a[0], a[1], a[2], a[3], a[4], a[5],
                        a[6], a[7], a[8], a[9], a[10], a[11]),
                "orient3d stage D");
        checkAgreement(inputs, RobustPredicatesInterpreted.ORIENT3D::apply,
                a -> RobustPredicates.orient3d(a[0], a[1], a[2], a[3], a[4], a[5],
                        a[6], a[7], a[8], a[9], a[10], a[11]),
                "orient3d chain");
    }

    @Test
    void incircleAllStagesAgree() {
        Random rng = new Random(53);
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 8, 250));
        List<double[]> cocircular = InputGenerators.cocircular(rng, 120);
        inputs.addAll(cocircular);
        inputs.addAll(InputGenerators.perturbAll(cocircular, rng, 4));
        inputs.add(InputGenerators.notebookIncircle());
        SemiStaticFilter stageA = new SemiStaticFilter(Expressions.INCIRCLE);
        StageB stageB = new StageB(Expressions.INCIRCLE);
        StageD stageD = new StageD(Expressions.INCIRCLE);
        checkAgreement(inputs, stageA::apply,
                a -> RobustPredicates.incircleStageA(a[0], a[1], a[2], a[3], a[4], a[5],
                        a[6], a[7]),
                "incircle stage A");
        checkAgreement(inputs, stageB::apply,
                a -> RobustPredicates.incircleStageB(a[0], a[1], a[2], a[3], a[4], a[5],
                        a[6], a[7]),
                "incircle stage B");
        checkAgreement(inputs, stageD::apply,
                a -> RobustPredicates.incircleStageD(a[0], a[1], a[2], a[3], a[4], a[5],
                        a[6], a[7]),
                "incircle stage D");
        checkAgreement(inputs, RobustPredicatesInterpreted.INCIRCLE::apply,
                a -> RobustPredicates.incircle(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7]),
                "incircle chain");
    }

    @Test
    void insphereAllStagesAgree() {
        Random rng = new Random(54);
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 15, 120));
        List<double[]> cospherical = InputGenerators.cospherical(rng, 80);
        inputs.addAll(cospherical);
        inputs.addAll(InputGenerators.perturbAll(cospherical, rng, 4));
        SemiStaticFilter stageA = new SemiStaticFilter(Expressions.INSPHERE);
        StageB stageB = new StageB(Expressions.INSPHERE);
        StageD stageD = new StageD(Expressions.INSPHERE);
        checkAgreement(inputs, stageA::apply,
                a -> RobustPredicates.insphereStageA(a[0], a[1], a[2], a[3], a[4], a[5],
                        a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14]),
                "insphere stage A");
        checkAgreement(inputs, stageB::apply,
                a -> RobustPredicates.insphereStageB(a[0], a[1], a[2], a[3], a[4], a[5],
                        a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14]),
                "insphere stage B");
        checkAgreement(inputs, stageD::apply,
                a -> RobustPredicates.insphereStageD(a[0], a[1], a[2], a[3], a[4], a[5],
                        a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14]),
                "insphere stage D");
        checkAgreement(inputs, RobustPredicatesInterpreted.INSPHERE::apply,
                a -> RobustPredicates.insphere(a[0], a[1], a[2], a[3], a[4], a[5],
                        a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14]),
                "insphere chain");
    }
}
