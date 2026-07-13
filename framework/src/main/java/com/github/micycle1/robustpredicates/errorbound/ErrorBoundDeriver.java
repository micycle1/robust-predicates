package com.github.micycle1.robustpredicates.errorbound;

import com.github.micycle1.robustpredicates.expr.Expression;
import com.github.micycle1.robustpredicates.expr.Op;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Automatic forward error-bound derivation for an expression tree.
 *
 * <p>Every subexpression is assigned an {@link ErrorTerm}: a magnitude
 * expression (built from {@code abs}, {@code +}, {@code *} and underflow-guard
 * constants over the same interned subexpressions as the main expression) and
 * an eps-polynomial coefficient triple. Rules are tried in a fixed order
 * (inputs assumed exact):
 * <ol>
 *   <li>exact leaves ({@code a = 0});</li>
 *   <li>sum/difference of two leaves — exact analysis, {@code a = u};</li>
 *   <li>product with a power-of-two constant factor — no new rounding;</li>
 *   <li>product of two leaves, {@code a = u};</li>
 *   <li>general sum/difference — worst child bound inflated by
 *       {@code (1+u)} plus one {@code u};</li>
 *   <li>Ozaki et al. lemma 3.1 for {@code (a±b)*(c±d)} with leaf operands,
 *       {@code a = (3, -(phi-14), 0)} with {@code phi = 94906264} for double;</li>
 *   <li>general product — child polynomials multiplied, inflated, plus one
 *       {@code u}.</li>
 * </ol>
 *
 * <p>With underflow protection enabled (the default), rules whose absolute
 * error can underflow append
 * an underflow-guard constant ({@code count * Double.MIN_NORMAL}) to their
 * magnitude; guards of sibling magnitudes are merged into a single guard when
 * magnitudes are summed.
 */
public final class ErrorBoundDeriver {

    /** Unit roundoff of double: {@code epsilon/2 = 2^-53}. */
    public static final double U = 0x1.0p-53;

    /** Ozaki phi constant for IEEE double (53 mantissa digits). */
    public static final long PHI = 94_906_264L;

    private final boolean underflowProtection;
    private final Map<Expression, ErrorTerm> memo = new IdentityHashMap<>();

    public ErrorBoundDeriver(boolean underflowProtection) {
        this.underflowProtection = underflowProtection;
    }

    /** Deriver with underflow protection enabled. */
    public ErrorBoundDeriver() {
        this(true);
    }

    // ------------------------------------------------------------------
    // Per-subexpression rule chain
    // ------------------------------------------------------------------

    /** Forward error bound of a subexpression (memoized). */
    public ErrorTerm derive(Expression e) {
        ErrorTerm cached = memo.get(e);
        if (cached != null) {
            return cached;
        }
        ErrorTerm term = applyRules(e);
        memo.put(e, term);
        return term;
    }

    private ErrorTerm applyRules(Expression e) {
        // Rule: exact_leaves
        if (e.isLeaf()) {
            Expression magnitude = e.op() == Op.CONSTANT
                    ? Expression.constant(Math.abs(e.value()))
                    : Expression.abs(e);
            return new ErrorTerm(magnitude, EpsCoefficients.ZERO);
        }
        boolean sumDiff = e.op() == Op.SUM || e.op() == Op.DIFFERENCE;
        boolean bothLeaves = e.left().isLeaf() && e.right().isLeaf();
        // Rule: exact_leaves_sumdiff
        if (sumDiff && bothLeaves) {
            return new ErrorTerm(Expression.abs(e), EpsCoefficients.ONE_EPS);
        }
        if (e.op() == Op.PRODUCT) {
            // Rule: pow2_product
            if (isApplicablePow2Factor(e.left()) || isApplicablePow2Factor(e.right())) {
                ErrorTerm leb = derive(e.left());
                ErrorTerm reb = derive(e.right());
                return new ErrorTerm(Expression.product(leb.magnitude(), reb.magnitude()),
                        EpsCoefficients.max(leb.a(), reb.a()));
            }
            // Rule: exact_leaves_product
            if (bothLeaves) {
                return new ErrorTerm(guarded(Expression.abs(e), 1), EpsCoefficients.ONE_EPS);
            }
        }
        // Rule: inexacts_sumdiff
        if (sumDiff) {
            ErrorTerm leb = derive(e.left());
            ErrorTerm reb = derive(e.right());
            EpsCoefficients a = EpsCoefficients.max(leb.a(), reb.a())
                    .multBy1PlusEps().incFirst();
            return new ErrorTerm(collapseUnderflowGuards(leb.magnitude(), reb.magnitude()), a);
        }
        if (e.op() == Op.PRODUCT) {
            // Rule: ozaki_simple_fp_lemma_31 for (a±b)*(c±d) with leaf operands
            if (isLeafSumDiff(e.left()) && isLeafSumDiff(e.right())) {
                return new ErrorTerm(guarded(Expression.abs(e), 1),
                        new EpsCoefficients(3, -(PHI - 14), 0));
            }
            // Rule: inexacts_product
            ErrorTerm leb = derive(e.left());
            ErrorTerm reb = derive(e.right());
            EpsCoefficients a = EpsCoefficients.product(leb.a(), reb.a())
                    .multBy1PlusEps().incFirst();
            Expression magnitude = guarded(
                    Expression.product(leb.magnitude(), reb.magnitude()), 1);
            return new ErrorTerm(magnitude, a);
        }
        throw new IllegalArgumentException(
                "no error-bound rule for operator: " + e.op());
    }

    private static boolean isLeafSumDiff(Expression e) {
        return (e.op() == Op.SUM || e.op() == Op.DIFFERENCE)
                && e.left().isLeaf() && e.right().isLeaf();
    }

    /**
     * Whether the node is a constant leaf holding {@code +/-2^k} (with
     * {@code k >= 0} under underflow protection), so that multiplying by it is
     * exact.
     */
    private boolean isApplicablePow2Factor(Expression e) {
        if (e.op() != Op.CONSTANT || e.isUnderflowGuard()) {
            return false;
        }
        double v = Math.abs(e.value());
        if (v == 0 || Double.isInfinite(v) || Double.isNaN(v)) {
            return false;
        }
        int exponent = Math.getExponent(v);
        boolean isPow2 = v == Math.scalb(1.0, exponent);
        return isPow2 && (!underflowProtection || exponent >= 0);
    }

    // ------------------------------------------------------------------
    // Underflow guard handling
    // ------------------------------------------------------------------

    private Expression guarded(Expression magnitude, int count) {
        if (!underflowProtection) {
            return magnitude;
        }
        return Expression.sum(magnitude, Expression.underflowGuard(count));
    }

    private static int countUnderflowGuards(Expression magnitude) {
        if (magnitude.op() == Op.SUM && magnitude.right().isUnderflowGuard()) {
            return magnitude.right().guardCount();
        }
        return 0;
    }

    private static Expression stripUnderflowGuards(Expression magnitude) {
        if (magnitude.op() == Op.SUM && magnitude.right().isUnderflowGuard()) {
            return magnitude.left();
        }
        return magnitude;
    }

    /**
     * Sum of two magnitudes with their underflow guards merged into a single
     * trailing guard.
     */
    private static Expression collapseUnderflowGuards(Expression lMag, Expression rMag) {
        int guards = countUnderflowGuards(lMag) + countUnderflowGuards(rMag);
        if (guards == 0) {
            return Expression.sum(lMag, rMag);
        }
        return Expression.sum(
                Expression.sum(stripUnderflowGuards(lMag), stripUnderflowGuards(rMag)),
                Expression.underflowGuard(guards));
    }

    // ------------------------------------------------------------------
    // Root finalization
    // ------------------------------------------------------------------

    /**
     * Concrete stage-A condition for a sum/difference root: a single double
     * constant and a magnitude expression such that the true error is bounded
     * by {@code constant * magnitude(inputs)}.
     */
    public record Condition(double constant, Expression magnitude) {
    }

    public Condition deriveCondition(Expression root) {
        if (root.op() != Op.SUM && root.op() != Op.DIFFERENCE) {
            throw new IllegalArgumentException(
                    "root finalization requires a sum/difference root, got " + root.op());
        }
        ErrorTerm leb = derive(root.left());
        ErrorTerm reb = derive(root.right());
        EpsCoefficients a = EpsCoefficients.max(leb.a(), reb.a())
                .divBy1MinusEps().multBy1PlusEps().multBy1PlusEps();
        long c = EpsCoefficients.roundToNextPow2(a.a0());
        long epsSquareCoeff = c == 0 ? 0
                : (a.a2() > 0 ? c * ((a.a1() + 1) / c + 1) : c * (a.a1() / c + 1));
        double constant = a.a0() * U + epsSquareCoeff * U * U;
        return new Condition(constant, collapseUnderflowGuards(leb.magnitude(), reb.magnitude()));
    }

    /** Full error-bound expression {@code constant * magnitude} for the root. */
    public Expression deriveErrorExpression(Expression root) {
        Condition condition = deriveCondition(root);
        return Expression.product(Expression.constant(condition.constant()),
                condition.magnitude());
    }
}
