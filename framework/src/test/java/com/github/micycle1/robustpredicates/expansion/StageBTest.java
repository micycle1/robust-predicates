package com.github.micycle1.robustpredicates.expansion;

import com.github.micycle1.robustpredicates.ExpansionArithmetic;

import com.github.micycle1.robustpredicates.expr.Expression;
import com.github.micycle1.robustpredicates.expr.Expressions;
import com.github.micycle1.robustpredicates.expr.EvaluationPlan;
import com.github.micycle1.robustpredicates.expr.Op;
import com.github.micycle1.robustpredicates.filter.Sign;
import com.github.micycle1.robustpredicates.reference.BigDecimalReference;
import com.github.micycle1.robustpredicates.reference.InputGenerators;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class StageBTest {

    /** True iff every leaf-minus-leaf difference of the expression is exact. */
    private static boolean allLeafDiffsExact(Expression root, double[] args) {
        for (Expression node : EvaluationPlan.of(root).nodes()) {
            if (node.op() == Op.DIFFERENCE && node.left().isLeaf() && node.right().isLeaf()) {
                double l = args[node.left().argN() - 1];
                double r = args[node.right().argN() - 1];
                if (ExpansionArithmetic.twoDiffTail(l, r, l - r) != 0.0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void uncertainExactlyWhenSomeTailIsNonzero() {
        Random rng = new Random(21);
        StageB stage = new StageB(Expressions.INCIRCLE);
        int decided = 0;
        int deferred = 0;
        for (double[] args : InputGenerators.randomBatch(rng, 8, 1500)) {
            int result = stage.apply(args);
            if (allLeafDiffsExact(Expressions.INCIRCLE, args)) {
                assertNotEquals(Sign.UNCERTAIN, result,
                        "exact differences must be decided by stage B");
                assertEquals(BigDecimalReference.incircle(args), result);
                decided++;
            } else {
                assertEquals(Sign.UNCERTAIN, result,
                        "inexact differences must defer");
                deferred++;
            }
        }
        // The mixed regimes should exercise both paths.
        assertNotEquals(0, decided, "expected some decided inputs");
        assertNotEquals(0, deferred, "expected some deferred inputs");
    }

    @Test
    void decidesDyadicGridInputsExactly() {
        // Small-integer inputs: every pairwise difference is exact, so stage B
        // must decide and agree with the oracle, including cocircular cases.
        Random rng = new Random(22);
        for (double[] args : InputGenerators.cocircular(rng, 300)) {
            StageB stage = new StageB(Expressions.INCIRCLE);
            assertEquals(0, BigDecimalReference.incircle(args), "generator sanity");
            assertEquals(0, stage.apply(args));
        }
        StageB orient2d = new StageB(Expressions.ORIENT2D);
        for (double[] args : InputGenerators.collinear2d(rng, 300)) {
            assertEquals(0, orient2d.apply(args));
        }
        StageB orient3d = new StageB(Expressions.ORIENT3D);
        for (double[] args : InputGenerators.coplanar(rng, 200)) {
            assertEquals(0, orient3d.apply(args));
        }
        StageB insphere = new StageB(Expressions.INSPHERE);
        for (double[] args : InputGenerators.cospherical(rng, 100)) {
            assertEquals(0, insphere.apply(args));
        }
    }

    @Test
    void decidedRandomInsphereMatchesOracle() {
        Random rng = new Random(23);
        StageB stage = new StageB(Expressions.INSPHERE);
        List<double[]> inputs = InputGenerators.randomBatch(rng, 15, 200);
        for (double[] args : inputs) {
            int result = stage.apply(args);
            if (result != Sign.UNCERTAIN) {
                assertEquals(BigDecimalReference.insphere(args), result);
            }
        }
    }
}
