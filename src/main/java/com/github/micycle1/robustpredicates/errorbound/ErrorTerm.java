package com.github.micycle1.robustpredicates.errorbound;

import com.github.micycle1.robustpredicates.expr.Expression;

/**
 * Forward error bound of one subexpression: an eps-polynomial coefficient
 * triple and a magnitude expression such that
 * {@code |computed - exact| <= (a0*u + a1*u^2 + a2*u^3) * magnitude(inputs)}.
 */
public record ErrorTerm(Expression magnitude, EpsCoefficients a) {
}
