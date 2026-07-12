package com.github.micycle1.robustpredicates.filter;

/**
 * Sign result constants shared by all filter stages.
 *
 * <p>A stage returns {@code 1}, {@code 0} or {@code -1} when it can certify the
 * sign of the predicate expression, and {@link #UNCERTAIN} ({@code -2}) when it
 * cannot. A {@link StagedPredicate} advances to the next (more expensive) stage
 * only on {@code UNCERTAIN}. This mirrors the {@code sign_uncertain} protocol of
 * the C++ framework ({@code expression_tree.hpp}).
 */
public final class Sign {
    /** A stage could not certify the sign; try the next stage. */
    public static final int UNCERTAIN = -2;

    private Sign() {
    }

    /** Sign of a double as an int in {-1, 0, 1}. */
    public static int of(double x) {
        if (x > 0) {
            return 1;
        }
        if (x < 0) {
            return -1;
        }
        return 0;
    }
}
