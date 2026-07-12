package com.github.micycle1.robustpredicates.expansion;

import com.github.micycle1.robustpredicates.expr.Expression;
import com.github.micycle1.robustpredicates.filter.Stage;

/**
 * Exact terminal stage (port of {@code stage_d.hpp}): evaluates the expression
 * with full expansion arithmetic and never returns
 * {@link com.github.micycle1.robustpredicates.filter.Sign#UNCERTAIN}.
 */
public final class StageD implements Stage {

    private final ExpansionEvaluator evaluator;

    public StageD(Expression expression) {
        this.evaluator = new ExpansionEvaluator(expression, false);
    }

    @Override
    public int apply(double[] args) {
        return evaluator.evaluateSign(args);
    }
}
