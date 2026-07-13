package com.github.micycle1.robustpredicates.expr;

import com.github.micycle1.robustpredicates.eval.ApproximateEvaluator;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionTest {

    @Test
    void interningMakesStructuralEqualityIdentity() {
        Expression a = Expression.diff(Expression.arg(3), Expression.arg(1));
        Expression b = Expression.diff(Expression.arg(3), Expression.arg(1));
        assertSame(a, b);
        assertSame(Expression.product(a, a), Expression.product(b, b));
    }

    @Test
    void metadataFlags() {
        Expression x = Expression.arg(1);
        Expression square = Expression.product(x, x);
        assertTrue(square.nonNegative(), "square must be non-negative");
        assertTrue(Expression.abs(x).nonNegative());
        assertTrue(Expression.sum(square, square).nonNegative());
        assertFalse(Expression.diff(square, square).nonNegative());
        assertTrue(Expression.underflowGuard(3).isUnderflowGuard());
        assertEquals(3 * Double.MIN_NORMAL, Expression.underflowGuard(3).value());
    }

    @Test
    void planDeduplicatesSharedSubexpressions() {
        // incircle: the six coordinate differences are each used in a lift and
        // in determinant terms; the plan must hold each only once.
        EvaluationPlan plan = EvaluationPlan.of(Expressions.INCIRCLE);
        long leafDiffs = plan.nodes().stream()
                .filter(n -> n.op() == Op.DIFFERENCE
                        && n.left().isLeaf() && n.right().isLeaf())
                .count();
        assertEquals(6, leafDiffs);
        // 8 args + 6 diffs + 3 lifts(2 sums each... structure sanity: just
        // check total node count is stable and small.
        assertEquals(8 + 6 + 6 + 3 + 6 + 3 + 3 + 2, plan.slotCount());
    }

    @Test
    void approximateEvaluationMatchesDirectFormula() {
        Random rng = new Random(42);
        EvaluationPlan plan = EvaluationPlan.of(Expressions.ORIENT2D);
        for (int i = 0; i < 1000; i++) {
            double[] a = new double[6];
            for (int j = 0; j < 6; j++) {
                a[j] = rng.nextDouble() * 20 - 10;
            }
            double direct = (a[2] - a[0]) * (a[5] - a[1]) - (a[4] - a[0]) * (a[3] - a[1]);
            assertEquals(direct, ApproximateEvaluator.evaluateRoot(plan, 0, a));
        }
    }

    @Test
    void approximateIncircleMatchesDirectFormula() {
        Random rng = new Random(7);
        EvaluationPlan plan = EvaluationPlan.of(Expressions.INCIRCLE);
        for (int i = 0; i < 1000; i++) {
            double[] a = new double[8];
            for (int j = 0; j < 8; j++) {
                a[j] = rng.nextDouble() * 20 - 10;
            }
            double adx = a[0] - a[6];
            double ady = a[1] - a[7];
            double bdx = a[2] - a[6];
            double bdy = a[3] - a[7];
            double cdx = a[4] - a[6];
            double cdy = a[5] - a[7];
            double aLift = adx * adx + ady * ady;
            double bLift = bdx * bdx + bdy * bdy;
            double cLift = cdx * cdx + cdy * cdy;
            double aDet = bdx * cdy - bdy * cdx;
            double bDet = adx * cdy - ady * cdx;
            double cDet = adx * bdy - ady * bdx;
            double direct = aLift * aDet - bLift * bDet + cLift * cDet;
            assertEquals(direct, ApproximateEvaluator.evaluateRoot(plan, 0, a));
        }
    }
}
