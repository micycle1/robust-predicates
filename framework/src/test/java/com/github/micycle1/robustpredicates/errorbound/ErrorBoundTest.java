package com.github.micycle1.robustpredicates.errorbound;

import com.github.micycle1.robustpredicates.expr.Expression;
import com.github.micycle1.robustpredicates.expr.Expressions;
import com.github.micycle1.robustpredicates.filter.SemiStaticFilter;
import com.github.micycle1.robustpredicates.filter.Sign;
import com.github.micycle1.robustpredicates.reference.BigDecimalReference;
import com.github.micycle1.robustpredicates.reference.InputGenerators;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorBoundTest {

    @Test
    void coefficientAlgebraMatchesHandComputedValues() {
        // orient2d by hand: leaf diffs have a = (1,0,0); products of two such
        // diffs hit the Ozaki rule a = (3, -(phi-14), 0).
        EpsCoefficients leafDiff = EpsCoefficients.ONE_EPS;
        assertEquals(new EpsCoefficients(1, 1, 0), leafDiff.multBy1PlusEps());
        assertEquals(new EpsCoefficients(2, 1, 0), leafDiff.multBy1PlusEps().incFirst());
        assertEquals(new EpsCoefficients(2, 3, 3),
                EpsCoefficients.product(new EpsCoefficients(1, 1, 1),
                        new EpsCoefficients(1, 1, 0)));
        assertEquals(new EpsCoefficients(3, 5, 9),
                new EpsCoefficients(3, 2, 3).divBy1MinusEps());
        assertEquals(4, EpsCoefficients.roundToNextPow2(3));
        assertEquals(0, EpsCoefficients.roundToNextPow2(0));
        assertEquals(8, EpsCoefficients.roundToNextPow2(8));
    }

    @Test
    void orient2dConditionConstant() {
        // Golden constant, derived by hand from the rules: the root's
        // children are Ozaki products with a = (3, -(phi-14), 0),
        // phi = 94906264. Finalization: div_by_1_minus_eps gives
        // (3, -94906247, -94906246), two mult_by_1_plus_eps give
        // (3, -94906241, ...); c = 4 and the eps^2 coefficient rounds to
        // 4 * (-94906241/4 + 1) = -94906236. The constant is therefore
        // slightly BELOW the classic 3u thanks to the Ozaki lemma.
        ErrorBoundDeriver deriver = new ErrorBoundDeriver();
        ErrorBoundDeriver.Condition condition =
                deriver.deriveCondition(Expressions.ORIENT2D);
        double u = ErrorBoundDeriver.U;
        assertEquals(3 * u - 94906236.0 * u * u, condition.constant());
        assertTrue(condition.constant() < 3 * u, "Ozaki rule tightens below 3u");
    }

    private void checkSoundness(Expression expr, List<double[]> inputs,
                                Function<double[], BigDecimal> exactValue) {
        ErrorBoundDeriver deriver = new ErrorBoundDeriver();
        SemiStaticFilter filter = new SemiStaticFilter(expr,
                deriver.deriveErrorExpression(expr));
        for (double[] args : inputs) {
            double approx = filter.approximateValue(args);
            double bound = filter.errorBound(args);
            BigDecimal error = exactValue.apply(args)
                    .subtract(new BigDecimal(approx)).abs();
            assertTrue(error.compareTo(new BigDecimal(bound)) <= 0,
                    () -> "unsound bound: |exact - approx| = " + error
                            + " > bound = " + bound + " for "
                            + java.util.Arrays.toString(args));
        }
    }

    @Test
    void orient2dBoundIsSound() {
        Random rng = new Random(11);
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 6, 2000));
        inputs.addAll(InputGenerators.perturbAll(InputGenerators.collinear2d(rng, 500), rng, 6));
        checkSoundness(Expressions.ORIENT2D, inputs, BigDecimalReference::orient2dValue);
    }

    @Test
    void orient3dBoundIsSound() {
        Random rng = new Random(12);
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 12, 1000));
        inputs.addAll(InputGenerators.perturbAll(InputGenerators.coplanar(rng, 300), rng, 6));
        checkSoundness(Expressions.ORIENT3D, inputs, BigDecimalReference::orient3dValue);
    }

    @Test
    void incircleBoundIsSound() {
        Random rng = new Random(13);
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 8, 1000));
        inputs.addAll(InputGenerators.perturbAll(InputGenerators.cocircular(rng, 300), rng, 6));
        inputs.add(InputGenerators.notebookIncircle());
        checkSoundness(Expressions.INCIRCLE, inputs, BigDecimalReference::incircleValue);
    }

    @Test
    void insphereBoundIsSound() {
        Random rng = new Random(14);
        List<double[]> inputs = new ArrayList<>(InputGenerators.randomBatch(rng, 15, 500));
        inputs.addAll(InputGenerators.perturbAll(InputGenerators.cospherical(rng, 200), rng, 6));
        checkSoundness(Expressions.INSPHERE, inputs, BigDecimalReference::insphereValue);
    }

    @Test
    void filterSoundAndEffective() {
        // Soundness: whenever the filter answers, the oracle agrees.
        // Effectiveness: on uniform random inputs it answers nearly always.
        Random rng = new Random(15);
        SemiStaticFilter filter = new SemiStaticFilter(Expressions.ORIENT2D);
        int certain = 0;
        int total = 0;
        for (double[] args : InputGenerators.randomBatch(rng, 6, 5000)) {
            int result = filter.apply(args);
            total++;
            if (result != Sign.UNCERTAIN) {
                certain++;
                assertEquals(BigDecimalReference.orient2d(args), result);
            }
        }
        assertTrue(certain > 0.99 * total,
                "stage A should resolve >99% of random inputs, got "
                        + certain + "/" + total);
    }

    @Test
    void notebookExampleIsUncertainAtStageA() {
        // The notebook's near-cocircular example must not be decided by the
        // stage A filter (its approximate determinant is ~1.1e-16 with a
        // bound around 1.3e-15).
        SemiStaticFilter filter = new SemiStaticFilter(Expressions.INCIRCLE);
        double[] args = InputGenerators.notebookIncircle();
        assertEquals(Sign.UNCERTAIN, filter.apply(args));
        assertTrue(filter.errorBound(args) > Math.abs(filter.approximateValue(args)));
    }
}
