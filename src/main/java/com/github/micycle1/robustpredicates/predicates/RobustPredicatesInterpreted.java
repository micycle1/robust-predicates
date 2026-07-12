package com.github.micycle1.robustpredicates.predicates;

import com.github.micycle1.robustpredicates.expansion.StageB;
import com.github.micycle1.robustpredicates.expansion.StageD;
import com.github.micycle1.robustpredicates.expr.Expressions;
import com.github.micycle1.robustpredicates.filter.SemiStaticFilter;
import com.github.micycle1.robustpredicates.filter.StagedPredicate;

/**
 * The four classic predicates as interpreted staged chains
 * (stage A semi-static filter, then stage B, then exact stage D).
 *
 * <p>Sign conventions are documented on {@link Expressions}. The generated
 * flat routines in {@code com.github.micycle1.robustpredicates.generated.RobustPredicates} are the
 * specialization of exactly these chains.
 */
public final class RobustPredicatesInterpreted {

    public static final StagedPredicate ORIENT2D = chain(Expressions.ORIENT2D);
    public static final StagedPredicate ORIENT3D = chain(Expressions.ORIENT3D);
    public static final StagedPredicate INCIRCLE = chain(Expressions.INCIRCLE);
    public static final StagedPredicate INSPHERE = chain(Expressions.INSPHERE);

    private RobustPredicatesInterpreted() {
    }

    private static StagedPredicate chain(com.github.micycle1.robustpredicates.expr.Expression expression) {
        return new StagedPredicate(
                new SemiStaticFilter(expression),
                new StageB(expression),
                new StageD(expression));
    }

    /** Positive iff {@code c} is to the left of the directed line {@code a -> b}. */
    public static int orient2d(double ax, double ay, double bx, double by,
                               double cx, double cy) {
        return ORIENT2D.apply(new double[] {ax, ay, bx, by, cx, cy});
    }

    /** Sign of {@code det[q-p, r-p, s-p]}. */
    public static int orient3d(double px, double py, double pz,
                               double qx, double qy, double qz,
                               double rx, double ry, double rz,
                               double sx, double sy, double sz) {
        return ORIENT3D.apply(new double[] {px, py, pz, qx, qy, qz, rx, ry, rz, sx, sy, sz});
    }

    /**
     * Positive iff {@code d} is inside the circle through {@code a, b, c}
     * (counterclockwise {@code abc}).
     */
    public static int incircle(double ax, double ay, double bx, double by,
                               double cx, double cy, double dx, double dy) {
        return INCIRCLE.apply(new double[] {ax, ay, bx, by, cx, cy, dx, dy});
    }

    /**
     * Lifted 4x4 insphere determinant with row order {@code p, r, q, s}
     * (CGAL {@code side_of_oriented_sphere_3} convention).
     */
    public static int insphere(double px, double py, double pz,
                               double qx, double qy, double qz,
                               double rx, double ry, double rz,
                               double sx, double sy, double sz,
                               double tx, double ty, double tz) {
        return INSPHERE.apply(new double[] {
                px, py, pz, qx, qy, qz, rx, ry, rz, sx, sy, sz, tx, ty, tz});
    }
}
