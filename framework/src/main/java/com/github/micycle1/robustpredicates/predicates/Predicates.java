package com.github.micycle1.robustpredicates.predicates;

import com.github.micycle1.robustpredicates.expr.Expression;
import com.github.micycle1.robustpredicates.expr.Expressions;

// NOTE experimental

/**
 * Higher-level geometric predicate wrappers.
 *
 * <p>This class interprets the signs of the low-level expression trees in
 * {@link Expressions}. It does not perform arithmetic itself; callers supply
 * the robust expression-sign evaluator used by the library.
 */
public final class Predicates {

    private Predicates() {
    }

    /**
     * Evaluates the sign of an expression.
     *
     * <p>Implementations must return exactly {@code -1}, {@code 0}, or
     * {@code +1}; in particular, the result must be exact with respect to the
     * predicate's sign classification.
     */
    @FunctionalInterface
    public interface SignEvaluator {

        int sign(Expression expression, double... arguments);
    }

    public enum Sign {
        NEGATIVE(-1),
        ZERO(0),
        POSITIVE(1);

        private final int value;

        Sign(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public Sign negate() {
            return switch (this) {
                case NEGATIVE -> POSITIVE;
                case ZERO -> ZERO;
                case POSITIVE -> NEGATIVE;
            };
        }

        static Sign of(int value) {
            return switch (Integer.compare(value, 0)) {
                case -1 -> NEGATIVE;
                case 0 -> ZERO;
                case 1 -> POSITIVE;
                default -> throw new AssertionError("unreachable");
            };
        }
    }

    /**
     * Classification relative to a geometric region.
     */
    public enum Location {
        INSIDE,
        BOUNDARY,
        OUTSIDE,
        DEGENERATE
    }

    // ---------------------------------------------------------------------
    // Orientation
    // ---------------------------------------------------------------------

    /**
     * Returns the orientation of triangle {@code abc}.
     *
     * <ul>
     *   <li>{@link Sign#POSITIVE}: counterclockwise</li>
     *   <li>{@link Sign#NEGATIVE}: clockwise</li>
     *   <li>{@link Sign#ZERO}: collinear</li>
     * </ul>
     */
    public static Sign orient2d(
            SignEvaluator evaluator,
            double ax, double ay,
            double bx, double by,
            double cx, double cy) {

        return evaluate(evaluator, Expressions.ORIENT2D,
                ax, ay, bx, by, cx, cy);
    }

    /**
     * Returns the orientation of tetrahedron {@code abcd}.
     *
     * <p>The sign convention is that of {@link Expressions#ORIENT3D}: positive
     * iff {@code d} lies below the plane through {@code a, b, c} with
     * {@code a, b, c} counterclockwise seen from above.
     */
    public static Sign orient3d(
            SignEvaluator evaluator,
            double ax, double ay, double az,
            double bx, double by, double bz,
            double cx, double cy, double cz,
            double dx, double dy, double dz) {

        return evaluate(evaluator, Expressions.ORIENT3D,
                ax, ay, az,
                bx, by, bz,
                cx, cy, cz,
                dx, dy, dz);
    }

    public static boolean areCollinear2d(
            SignEvaluator evaluator,
            double ax, double ay,
            double bx, double by,
            double cx, double cy) {

        return orient2d(evaluator, ax, ay, bx, by, cx, cy) == Sign.ZERO;
    }

    public static boolean areCoplanar3d(
            SignEvaluator evaluator,
            double ax, double ay, double az,
            double bx, double by, double bz,
            double cx, double cy, double cz,
            double dx, double dy, double dz) {

        return orient3d(evaluator,
                ax, ay, az,
                bx, by, bz,
                cx, cy, cz,
                dx, dy, dz) == Sign.ZERO;
    }

    // ---------------------------------------------------------------------
    // Triangle containment
    // ---------------------------------------------------------------------

    /**
     * Classifies {@code p} relative to triangle {@code abc}.
     *
     * <p>The result is independent of whether {@code abc} is clockwise or
     * counterclockwise. A collinear triangle returns
     * {@link Location#DEGENERATE}.
     */
    public static Location pointInTriangle2d(
            SignEvaluator evaluator,
            double ax, double ay,
            double bx, double by,
            double cx, double cy,
            double px, double py) {

        int triangle = sign(evaluator, Expressions.ORIENT2D,
                ax, ay, bx, by, cx, cy);

        if (triangle == 0) {
            return Location.DEGENERATE;
        }

        int abp = sign(evaluator, Expressions.ORIENT2D,
                ax, ay, bx, by, px, py);
        int bcp = sign(evaluator, Expressions.ORIENT2D,
                bx, by, cx, cy, px, py);
        int cap = sign(evaluator, Expressions.ORIENT2D,
                cx, cy, ax, ay, px, py);

        if (abp * triangle < 0 || bcp * triangle < 0 || cap * triangle < 0) {
            return Location.OUTSIDE;
        }

        if (abp == 0 || bcp == 0 || cap == 0) {
            return Location.BOUNDARY;
        }

        return Location.INSIDE;
    }

    // ---------------------------------------------------------------------
    // Circumcircle / incircle
    // ---------------------------------------------------------------------

    /**
     * Classifies {@code d} relative to the circumcircle through {@code a},
     * {@code b}, and {@code c}.
     *
     * <p>Unlike the raw {@link Expressions#INCIRCLE} sign, this result is
     * independent of the orientation of {@code abc}. If {@code a}, {@code b},
     * and {@code c} are collinear, the circumcircle is undefined and this
     * method returns {@link Location#DEGENERATE}.
     */
    public static Location pointInCircumcircle2d(
            SignEvaluator evaluator,
            double ax, double ay,
            double bx, double by,
            double cx, double cy,
            double dx, double dy) {

        int orientation = sign(evaluator, Expressions.ORIENT2D,
                ax, ay, bx, by, cx, cy);

        if (orientation == 0) {
            return Location.DEGENERATE;
        }

        int incircle = sign(evaluator, Expressions.INCIRCLE,
                ax, ay, bx, by, cx, cy, dx, dy);

        // INCIRCLE is positive inside only for counterclockwise abc.
        int normalized = orientation * incircle;

        return locationFromInsidePositiveSign(normalized);
    }

    // ---------------------------------------------------------------------
    // Diametral circle
    // ---------------------------------------------------------------------

    /**
     * Classifies {@code p} relative to the circle having {@code ab} as its
     * diameter.
     *
     * <p>This uses {@code (a-p)·(b-p)}:
     * <ul>
     *   <li>negative: inside;</li>
     *   <li>zero: on the diametral circle;</li>
     *   <li>positive: outside.</li>
     * </ul>
     *
     * <p>If {@code a == b}, the diameter is zero and the result is
     * {@link Location#DEGENERATE}.
     */
    public static Location pointInDiametralCircle2d(
            SignEvaluator evaluator,
            double ax, double ay,
            double bx, double by,
            double px, double py) {

        if (orient2d(evaluator, ax, ay, bx, by, ax, ay) == Sign.ZERO
                && ax == bx && ay == by) {
            return Location.DEGENERATE;
        }

        int dot = sign(evaluator, Expressions.DIAMETRAL_CIRCLE_2D,
                ax, ay, bx, by, px, py);

        return switch (dot) {
            case -1 -> Location.INSIDE;
            case 0 -> Location.BOUNDARY;
            case 1 -> Location.OUTSIDE;
            default -> throw new AssertionError("sign() must return -1, 0, or +1");
        };
    }

    // ---------------------------------------------------------------------
    // Raw oriented sphere result
    // ---------------------------------------------------------------------

    /**
     * Returns the raw oriented insphere sign using the convention documented
     * by {@link Expressions#INSPHERE}: positive iff {@code e} is inside the
     * sphere through {@code a, b, c, d} when {@code orient3d(a, b, c, d)} is
     * positive.
     *
     * <p>Use this rather than assuming that positive always means "inside":
     * the sign depends on the orientation of the defining tetrahedron.
     */
    public static Sign orientedInsphere(
            SignEvaluator evaluator,
            double ax, double ay, double az,
            double bx, double by, double bz,
            double cx, double cy, double cz,
            double dx, double dy, double dz,
            double ex, double ey, double ez) {

        return evaluate(evaluator, Expressions.INSPHERE,
                ax, ay, az,
                bx, by, bz,
                cx, cy, cz,
                dx, dy, dz,
                ex, ey, ez);
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private static Location locationFromInsidePositiveSign(int sign) {
        return switch (sign) {
            case 1 -> Location.INSIDE;
            case 0 -> Location.BOUNDARY;
            case -1 -> Location.OUTSIDE;
            default -> throw new AssertionError("sign() must return -1, 0, or +1");
        };
    }

    private static Sign evaluate(
            SignEvaluator evaluator,
            Expression expression,
            double... arguments) {

        return Sign.of(sign(evaluator, expression, arguments));
    }

    private static int sign(
            SignEvaluator evaluator,
            Expression expression,
            double... arguments) {

        int sign = evaluator.sign(expression, arguments);
        if (sign < -1 || sign > 1) {
            throw new IllegalArgumentException(
                    "SignEvaluator must return -1, 0, or +1; got " + sign);
        }
        return sign;
    }
}