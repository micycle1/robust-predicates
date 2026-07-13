package com.github.micycle1.robustpredicates.filter;

import com.github.micycle1.robustpredicates.errorbound.ErrorBoundDeriver;
import com.github.micycle1.robustpredicates.eval.ApproximateEvaluator;
import com.github.micycle1.robustpredicates.expr.EvaluationPlan;
import com.github.micycle1.robustpredicates.expr.Expression;

/**
 * Fully static stage A filter: the error
 * bound is computed once from global coordinate extrema via the
 * interval-transformed error expression, so each call only evaluates the
 * predicate expression and compares against a stored constant.
 *
 * <p>The bound is valid only for inputs whose coordinates lie within the
 * extrema supplied at construction. A default-constructed filter has an
 * infinite bound and always returns {@link Sign#UNCERTAIN}.
 */
public final class StaticFilter implements Stage {

    private final EvaluationPlan detPlan;
    private final Expression intervalErrorExpression;
    private final EvaluationPlan boundPlan;
    private final int argCount;
    private final double errorBound;

    /** Filter with an unknown domain: bound is infinite, always uncertain. */
    public StaticFilter(Expression expression) {
        this(expression, null, null);
    }

    /**
     * Filter for inputs whose i-th argument lies in
     * {@code [minima[i], maxima[i]]}.
     */
    public StaticFilter(Expression expression, double[] maxima, double[] minima) {
        this.detPlan = EvaluationPlan.of(expression);
        this.argCount = IntervalTransformer.maxArgNOf(expression);
        Expression errorExpression = new ErrorBoundDeriver().deriveErrorExpression(expression);
        this.intervalErrorExpression =
                new IntervalTransformer(argCount).transform(errorExpression);
        this.boundPlan = EvaluationPlan.of(intervalErrorExpression);
        this.errorBound = (maxima == null || minima == null)
                ? Double.POSITIVE_INFINITY
                : computeBound(maxima, minima);
    }

    private StaticFilter(StaticFilter template, double[] maxima, double[] minima) {
        this.detPlan = template.detPlan;
        this.argCount = template.argCount;
        this.intervalErrorExpression = template.intervalErrorExpression;
        this.boundPlan = template.boundPlan;
        this.errorBound = computeBound(maxima, minima);
    }

    private double computeBound(double[] maxima, double[] minima) {
        if (maxima.length != argCount || minima.length != argCount) {
            throw new IllegalArgumentException(
                    "expected " + argCount + " extrema, got " + maxima.length
                            + "/" + minima.length);
        }
        double[] extrema = new double[2 * argCount];
        System.arraycopy(maxima, 0, extrema, 0, argCount);
        System.arraycopy(minima, 0, extrema, argCount, argCount);
        return ApproximateEvaluator.evaluateRoot(boundPlan, 0, extrema);
    }

    /** A filter sharing this one's derived expressions but with new extrema. */
    public StaticFilter withExtrema(double[] maxima, double[] minima) {
        return new StaticFilter(this, maxima, minima);
    }

    /** The precomputed static error bound (for tests/diagnostics). */
    public double bound() {
        return errorBound;
    }

    public int argCount() {
        return argCount;
    }

    @Override
    public int apply(double[] args) {
        double det = ApproximateEvaluator.evaluateRoot(detPlan, 0, args);
        if (det > errorBound) {
            return 1;
        }
        if (det < -errorBound) {
            return -1;
        }
        if (errorBound == 0 && det == 0) {
            return 0;
        }
        return Sign.UNCERTAIN;
    }
}
