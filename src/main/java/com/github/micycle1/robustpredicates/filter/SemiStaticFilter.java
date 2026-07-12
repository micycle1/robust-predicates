package com.github.micycle1.robustpredicates.filter;

import com.github.micycle1.robustpredicates.errorbound.ErrorBoundDeriver;
import com.github.micycle1.robustpredicates.eval.ApproximateEvaluator;
import com.github.micycle1.robustpredicates.expr.EvaluationPlan;
import com.github.micycle1.robustpredicates.expr.Expression;

/**
 * Stage A: per-call floating-point filter (port of
 * {@code semi_static_filter.hpp} combined with the automatically derived
 * forward error bound).
 *
 * <p>Evaluates the predicate expression and its error-bound expression in one
 * shared plan (common subexpressions such as the coordinate differences are
 * computed once) and certifies the sign only when the approximate determinant
 * lies strictly outside the error bound.
 */
public final class SemiStaticFilter implements Stage {

    private final EvaluationPlan plan;
    private final int detSlot;
    private final int errSlot;

    public SemiStaticFilter(Expression expression) {
        this(expression, new ErrorBoundDeriver().deriveErrorExpression(expression));
    }

    /** Filter with an explicitly supplied error-bound expression. */
    public SemiStaticFilter(Expression expression, Expression errorExpression) {
        this.plan = EvaluationPlan.of(expression, errorExpression);
        this.detSlot = plan.rootSlot(0);
        this.errSlot = plan.rootSlot(1);
    }

    @Override
    public int apply(double[] args) {
        double[] out = new double[plan.slotCount()];
        ApproximateEvaluator.evaluate(plan, args, out);
        double det = out[detSlot];
        double errorBound = out[errSlot];
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

    /** The evaluated error bound for the given inputs (for tests/diagnostics). */
    public double errorBound(double[] args) {
        double[] out = new double[plan.slotCount()];
        ApproximateEvaluator.evaluate(plan, args, out);
        return out[errSlot];
    }

    /** The approximate determinant value for the given inputs. */
    public double approximateValue(double[] args) {
        double[] out = new double[plan.slotCount()];
        ApproximateEvaluator.evaluate(plan, args, out);
        return out[detSlot];
    }
}
