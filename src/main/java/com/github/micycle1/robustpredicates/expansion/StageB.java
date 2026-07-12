package com.github.micycle1.robustpredicates.expansion;

import com.github.micycle1.robustpredicates.expr.Expression;
import com.github.micycle1.robustpredicates.filter.Stage;

/**
 * Intermediate exact stage (port of {@code stage_b.hpp}): assumes every
 * leaf-minus-leaf difference of the expression is exact in floating point,
 * which permits much smaller expansions. If any such difference has a nonzero
 * rounding tail the stage returns {@link com.github.micycle1.robustpredicates.filter.Sign#UNCERTAIN} and a
 * later stage (typically {@link StageD}) decides.
 */
public final class StageB implements Stage {

    private final ExpansionEvaluator evaluator;

    public StageB(Expression expression) {
        this.evaluator = new ExpansionEvaluator(expression, true);
    }

    @Override
    public int apply(double[] args) {
        return evaluator.evaluateSign(args);
    }
}
