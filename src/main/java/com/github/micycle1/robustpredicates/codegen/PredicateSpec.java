package com.github.micycle1.robustpredicates.codegen;

import com.github.micycle1.robustpredicates.expr.Expression;

import java.util.List;

/**
 * One predicate to generate: a method name, its expression tree, parameter
 * names in argument order ({@code arg(i)} binds to {@code paramNames[i-1]})
 * and a Javadoc line for the public driver method.
 */
public record PredicateSpec(String name, Expression expression, List<String> paramNames,
                            String javadoc) {
}
