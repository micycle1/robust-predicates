package com.github.micycle1.robustpredicates.expr;

import java.util.List;

/**
 * A predicate definition: a method name, its parameter names in argument
 * order, the expression written in the textual language of
 * {@link ExpressionParser} (parameter {@code i} binds to argument slot
 * {@code i + 1}), and a Javadoc line describing the sign convention.
 */
public record PredicateSpec(String name, List<String> params, String body, String javadoc) {

    /** The parsed expression tree of {@link #body}. */
    public Expression expression() {
        return ExpressionParser.parse(body, params);
    }
}
