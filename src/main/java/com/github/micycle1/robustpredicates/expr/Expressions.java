package com.github.micycle1.robustpredicates.expr;

import static com.github.micycle1.robustpredicates.expr.Expression.arg;
import static com.github.micycle1.robustpredicates.expr.Expression.diff;
import static com.github.micycle1.robustpredicates.expr.Expression.product;
import static com.github.micycle1.robustpredicates.expr.Expression.sum;

/**
 * The predicate expression trees, written out with exactly the operation order
 * and sign conventions of the C++ sources
 * ({@code custom_kernel_expressions.hpp} and the {@code incircle.ipynb}
 * notebook). The evaluation order is semantically load-bearing: the forward
 * error-bound derivation and the stage B/D exactness structure depend on it,
 * so no algebraically equivalent rewrites are permitted.
 *
 * <p>Argument index conventions (1-based placeholders):
 * <ul>
 *   <li>{@link #ORIENT2D}: {@code ax, ay, bx, by, cx, cy} = {@code _1.._6};
 *       positive iff {@code c} lies to the left of the directed line
 *       {@code a -> b} (counterclockwise triangle {@code abc}).</li>
 *   <li>{@link #ORIENT3D}: {@code px, py, pz, qx, ..., sz} = {@code _1.._12};
 *       sign of {@code det[q-p, r-p, s-p]} (columns).</li>
 *   <li>{@link #INCIRCLE}: {@code ax, ay, bx, by, cx, cy, dx, dy} =
 *       {@code _1.._8}; positive iff {@code d} lies inside the circle through
 *       {@code a, b, c} when {@code abc} is counterclockwise.</li>
 *   <li>{@link #INSPHERE}: {@code px, ..., sz, tx, ty, tz} = {@code _1.._15};
 *       lifted 4x4 determinant with row order {@code p, r, q, s} (matching the
 *       CGAL {@code side_of_oriented_sphere_3} convention of the C++ kernel).</li>
 * </ul>
 */
public final class Expressions {

    private Expressions() {
    }

    // ------------------------------------------------------------------
    // Determinant helpers (exact ports of custom_kernel_expressions.hpp)
    // ------------------------------------------------------------------

    /** 2x2 determinant: {@code a00*a11 - a10*a01}. */
    public static Expression det2(Expression a00, Expression a01,
                                  Expression a10, Expression a11) {
        return diff(product(a00, a11), product(a10, a01));
    }

    /** 3x3 determinant via 2x2 minors of the first two columns. */
    public static Expression det3(Expression a00, Expression a01, Expression a02,
                                  Expression a10, Expression a11, Expression a12,
                                  Expression a20, Expression a21, Expression a22) {
        Expression m01 = diff(product(a00, a11), product(a10, a01));
        Expression m02 = diff(product(a00, a21), product(a20, a01));
        Expression m12 = diff(product(a10, a21), product(a20, a11));
        return sum(diff(product(m01, a22), product(m02, a12)), product(m12, a02));
    }

    /** 4x4 determinant via negated 2x2 minors and 3x3 cofactors. */
    public static Expression det4(Expression a00, Expression a01, Expression a02, Expression a03,
                                  Expression a10, Expression a11, Expression a12, Expression a13,
                                  Expression a20, Expression a21, Expression a22, Expression a23,
                                  Expression a30, Expression a31, Expression a32, Expression a33) {
        Expression m01 = diff(product(a10, a01), product(a00, a11));
        Expression m02 = diff(product(a20, a01), product(a00, a21));
        Expression m03 = diff(product(a30, a01), product(a00, a31));
        Expression m12 = diff(product(a20, a11), product(a10, a21));
        Expression m13 = diff(product(a30, a11), product(a10, a31));
        Expression m23 = diff(product(a30, a21), product(a20, a31));
        Expression m012 = sum(diff(product(m12, a02), product(m02, a12)), product(m01, a22));
        Expression m013 = sum(diff(product(m13, a02), product(m03, a12)), product(m01, a32));
        Expression m023 = sum(diff(product(m23, a02), product(m03, a22)), product(m02, a32));
        Expression m123 = sum(diff(product(m23, a12), product(m13, a22)), product(m12, a32));
        return diff(sum(diff(product(m123, a03), product(m023, a13)), product(m013, a23)),
                product(m012, a33));
    }

    // ------------------------------------------------------------------
    // Predicates
    // ------------------------------------------------------------------

    /** {@code (bx-ax)(cy-ay) - (cx-ax)(by-ay)} with args {@code ax..cy = _1.._6}. */
    public static final Expression ORIENT2D = buildOrient2d();

    /** 3x3 determinant of columns {@code q-p, r-p, s-p} with args {@code _1.._12}. */
    public static final Expression ORIENT3D = buildOrient3d();

    /** Classic lifted 3x3 incircle determinant with args {@code _1.._8}. */
    public static final Expression INCIRCLE = buildIncircle();

    /** Lifted 4x4 insphere determinant (rows p, r, q, s) with args {@code _1.._15}. */
    public static final Expression INSPHERE = buildInsphere();

    private static Expression buildOrient2d() {
        return det2(diff(arg(3), arg(1)), diff(arg(4), arg(2)),
                diff(arg(5), arg(1)), diff(arg(6), arg(2)));
    }

    private static Expression buildOrient3d() {
        return det3(diff(arg(4), arg(1)), diff(arg(7), arg(1)), diff(arg(10), arg(1)),
                diff(arg(5), arg(2)), diff(arg(8), arg(2)), diff(arg(11), arg(2)),
                diff(arg(6), arg(3)), diff(arg(9), arg(3)), diff(arg(12), arg(3)));
    }

    private static Expression buildIncircle() {
        Expression adx = diff(arg(1), arg(7));
        Expression ady = diff(arg(2), arg(8));
        Expression bdx = diff(arg(3), arg(7));
        Expression bdy = diff(arg(4), arg(8));
        Expression cdx = diff(arg(5), arg(7));
        Expression cdy = diff(arg(6), arg(8));
        Expression aLift = sum(product(adx, adx), product(ady, ady));
        Expression bLift = sum(product(bdx, bdx), product(bdy, bdy));
        Expression cLift = sum(product(cdx, cdx), product(cdy, cdy));
        Expression aDet = diff(product(bdx, cdy), product(bdy, cdx));
        Expression bDet = diff(product(adx, cdy), product(ady, cdx));
        Expression cDet = diff(product(adx, bdy), product(ady, bdx));
        return sum(diff(product(aLift, aDet), product(bLift, bDet)), product(cLift, cDet));
    }

    private static Expression buildInsphere() {
        Expression ptx = diff(arg(1), arg(13));
        Expression pty = diff(arg(2), arg(14));
        Expression ptz = diff(arg(3), arg(15));
        Expression pt2 = sum(sum(product(ptx, ptx), product(pty, pty)), product(ptz, ptz));
        Expression qtx = diff(arg(4), arg(13));
        Expression qty = diff(arg(5), arg(14));
        Expression qtz = diff(arg(6), arg(15));
        Expression qt2 = sum(sum(product(qtx, qtx), product(qty, qty)), product(qtz, qtz));
        Expression rtx = diff(arg(7), arg(13));
        Expression rty = diff(arg(8), arg(14));
        Expression rtz = diff(arg(9), arg(15));
        Expression rt2 = sum(sum(product(rtx, rtx), product(rty, rty)), product(rtz, rtz));
        Expression stx = diff(arg(10), arg(13));
        Expression sty = diff(arg(11), arg(14));
        Expression stz = diff(arg(12), arg(15));
        Expression st2 = sum(sum(product(stx, stx), product(sty, sty)), product(stz, stz));
        // Row order p, r, q, s as in the C++ kernel's side_of_oriented_sphere_3.
        return det4(ptx, pty, ptz, pt2,
                rtx, rty, rtz, rt2,
                qtx, qty, qtz, qt2,
                stx, sty, stz, st2);
    }
}
