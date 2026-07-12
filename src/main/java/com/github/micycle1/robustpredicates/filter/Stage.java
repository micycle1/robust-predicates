package com.github.micycle1.robustpredicates.filter;

/**
 * One stage of a staged robust predicate. A stage certifies the sign of the
 * predicate expression ({@code 1}, {@code 0}, {@code -1}) or returns
 * {@link Sign#UNCERTAIN} to defer to the next, more expensive stage.
 */
public interface Stage {

    int apply(double[] args);

    /** Whether this stage carries per-instance state (e.g. running extrema). */
    default boolean stateful() {
        return false;
    }

    /** Feeds new inputs to stateful stages (e.g. to widen extrema). */
    default void update(double[] args) {
    }
}
