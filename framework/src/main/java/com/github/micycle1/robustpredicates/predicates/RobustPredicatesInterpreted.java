package com.github.micycle1.robustpredicates.predicates;

import com.github.micycle1.robustpredicates.expansion.StageB;
import com.github.micycle1.robustpredicates.expansion.StageD;
import com.github.micycle1.robustpredicates.expr.Expression;
import com.github.micycle1.robustpredicates.expr.Expressions;
import com.github.micycle1.robustpredicates.filter.SemiStaticFilter;
import com.github.micycle1.robustpredicates.filter.StagedPredicate;

/**
 * The standard predicates as interpreted staged chains (stage A semi-static
 * filter, then stage B, then exact stage D), built directly from the
 * expression trees at class-load time.
 *
 * <p>Sign conventions are documented on {@link Expressions}. The generated
 * flat routines in the {@code robust-predicates} module
 * ({@code com.github.micycle1.robustpredicates.RobustPredicates}) are the
 * specialization of exactly these chains and agree with them bit for bit.
 */
public final class RobustPredicatesInterpreted {

    public static final StagedPredicate ORIENT2D = chain(Expressions.ORIENT2D);
    public static final StagedPredicate ORIENT3D = chain(Expressions.ORIENT3D);
    public static final StagedPredicate INCIRCLE = chain(Expressions.INCIRCLE);
    public static final StagedPredicate INSPHERE = chain(Expressions.INSPHERE);

    private RobustPredicatesInterpreted() {
    }

    private static StagedPredicate chain(Expression expression) {
        return new StagedPredicate(
                new SemiStaticFilter(expression),
                new StageB(expression),
                new StageD(expression));
    }

    /**
     * Positive iff {@code c} is to the left of the directed line
     * {@code a -> b} (counterclockwise {@code abc}, y axis up); zero iff
     * collinear.
     */
    public static int orient2d(double ax, double ay, double bx, double by,
                               double cx, double cy) {
        return ORIENT2D.apply(new double[] {ax, ay, bx, by, cx, cy});
    }

    /**
     * Positive iff {@code d} lies below the plane through {@code a, b, c},
     * where "below" means {@code a, b, c} appear counterclockwise when viewed
     * from above the plane; zero iff coplanar.
     */
    public static int orient3d(double ax, double ay, double az,
                               double bx, double by, double bz,
                               double cx, double cy, double cz,
                               double dx, double dy, double dz) {
        return ORIENT3D.apply(new double[] {ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz});
    }

    /**
     * Positive iff {@code d} is inside the circle through {@code a, b, c}
     * (counterclockwise {@code abc}, y axis up); zero iff cocircular.
     */
    public static int incircle(double ax, double ay, double bx, double by,
                               double cx, double cy, double dx, double dy) {
        return INCIRCLE.apply(new double[] {ax, ay, bx, by, cx, cy, dx, dy});
    }

    /**
     * Positive iff {@code e} is inside the sphere through {@code a, b, c, d},
     * where the four points are ordered so that {@code orient3d(a, b, c, d)}
     * is positive; zero iff cospherical.
     */
    public static int insphere(double ax, double ay, double az,
                               double bx, double by, double bz,
                               double cx, double cy, double cz,
                               double dx, double dy, double dz,
                               double ex, double ey, double ez) {
        return INSPHERE.apply(new double[] {
                ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, ex, ey, ez});
    }
}
