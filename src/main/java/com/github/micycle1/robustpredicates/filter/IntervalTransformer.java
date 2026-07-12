package com.github.micycle1.robustpredicates.filter;

import com.github.micycle1.robustpredicates.expr.Expression;
import com.github.micycle1.robustpredicates.expr.Op;

/**
 * Rewrites a per-call error-bound expression over concrete inputs into an
 * expression over per-coordinate extrema, using interval arithmetic
 * (port of {@code interval_error_bound.hpp}).
 *
 * <p>The transformed expression reads a {@code 2n}-element pseudo-argument
 * array laid out as {@code [max_1..max_n, min_1..min_n]}: {@code arg(i)} maps
 * to the maximum slot {@code i} and to the minimum slot {@code i + n}. The
 * top-level walk descends through the error expression's own structure
 * (constant times sums/products of magnitudes) and switches to full interval
 * min/max propagation at each {@code abs} node.
 */
public final class IntervalTransformer {

    private final int maxArgN;

    public IntervalTransformer(int maxArgN) {
        this.maxArgN = maxArgN;
    }

    /** Largest argument index used in the expression. */
    public static int maxArgNOf(Expression e) {
        if (e.isLeaf()) {
            return e.argN();
        }
        int max = maxArgNOf(e.left());
        if (e.right() != null) {
            max = Math.max(max, maxArgNOf(e.right()));
        }
        return max;
    }

    /** Transforms an error-bound expression into its extrema form. */
    public Expression transform(Expression e) {
        return switch (e.op()) {
            case ARGUMENT, CONSTANT -> e;
            case ABS -> Expression.max(
                    Expression.abs(intervalMin(e.left())),
                    Expression.abs(intervalMax(e.left())));
            case SUM -> Expression.sum(transform(e.left()), transform(e.right()));
            case DIFFERENCE -> Expression.diff(transform(e.left()), transform(e.right()));
            case PRODUCT -> Expression.product(transform(e.left()), transform(e.right()));
            case MAX -> Expression.max(transform(e.left()), transform(e.right()));
            case MIN -> Expression.min(transform(e.left()), transform(e.right()));
        };
    }

    private Expression intervalMin(Expression e) {
        return switch (e.op()) {
            case ARGUMENT -> Expression.arg(e.argN() + maxArgN);
            case CONSTANT -> e;
            case SUM -> Expression.sum(intervalMin(e.left()), intervalMin(e.right()));
            case DIFFERENCE -> Expression.diff(intervalMin(e.left()), intervalMax(e.right()));
            case PRODUCT -> {
                if (e.left() == e.right()) {
                    Expression minC = intervalMin(e.left());
                    Expression maxC = intervalMax(e.left());
                    yield Expression.min(Expression.product(minC, minC),
                            Expression.product(maxC, maxC));
                }
                Expression minL = intervalMin(e.left());
                Expression maxL = intervalMax(e.left());
                Expression minR = intervalMin(e.right());
                Expression maxR = intervalMax(e.right());
                yield Expression.min(
                        Expression.min(Expression.product(minL, minR),
                                Expression.product(minL, maxR)),
                        Expression.min(Expression.product(maxL, minR),
                                Expression.product(maxL, maxR)));
            }
            case ABS -> Expression.min(Expression.abs(intervalMin(e.left())),
                    Expression.abs(intervalMax(e.left())));
            default -> throw new IllegalArgumentException(
                    "unsupported operator in interval transform: " + e.op());
        };
    }

    private Expression intervalMax(Expression e) {
        return switch (e.op()) {
            case ARGUMENT -> e;
            case CONSTANT -> e;
            case SUM -> Expression.sum(intervalMax(e.left()), intervalMax(e.right()));
            case DIFFERENCE -> Expression.diff(intervalMax(e.left()), intervalMin(e.right()));
            case PRODUCT -> {
                if (e.left() == e.right()) {
                    Expression minC = intervalMin(e.left());
                    Expression maxC = intervalMax(e.left());
                    yield Expression.max(Expression.product(minC, minC),
                            Expression.product(maxC, maxC));
                }
                Expression minL = intervalMin(e.left());
                Expression maxL = intervalMax(e.left());
                Expression minR = intervalMin(e.right());
                Expression maxR = intervalMax(e.right());
                yield Expression.max(
                        Expression.max(Expression.product(minL, minR),
                                Expression.product(minL, maxR)),
                        Expression.max(Expression.product(maxL, minR),
                                Expression.product(maxL, maxR)));
            }
            case ABS -> Expression.max(Expression.abs(intervalMin(e.left())),
                    Expression.abs(intervalMax(e.left())));
            default -> throw new IllegalArgumentException(
                    "unsupported operator in interval transform: " + e.op());
        };
    }
}
