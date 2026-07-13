package com.github.micycle1.robustpredicates.expr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable, interned arithmetic expression tree node.
 *
 * <p>Every node is interned in a global pool so that structural equality
 * coincides with reference equality ({@code ==}). All derived machinery
 * (evaluation plans, error-bound memoization, square detection in expansion
 * sizing) relies on that property.
 *
 * <p>Operand order is never canonicalized: the exact operation order and signs
 * of an expression are semantically load-bearing for the forward error-bound
 * analysis and for the exactness structure of stages B and D.
 *
 * <p>Node kinds:
 * <ul>
 *   <li>{@link Op#ARGUMENT}: a 1-based input placeholder, see
 *       {@link #argN()};</li>
 *   <li>{@link Op#CONSTANT}: a fixed value; an <em>underflow guard</em> is a
 *       constant with {@link #guardCount()} {@code > 0} and value
 *       {@code guardCount * Double.MIN_NORMAL};</li>
 *   <li>binary {@link Op#SUM}/{@link Op#DIFFERENCE}/{@link Op#PRODUCT}/
 *       {@link Op#MAX}/{@link Op#MIN} and unary {@link Op#ABS}.</li>
 * </ul>
 */
public final class Expression {

    private final Op op;
    private final Expression left;   // null for leaves; child for ABS
    private final Expression right;  // null for leaves and ABS
    private final int argN;          // >= 1 for ARGUMENT, else 0
    private final double value;      // CONSTANT only
    private final int guardCount;    // > 0 marks an underflow guard constant
    private final boolean nonNegative;

    private record Key(Op op, Expression left, Expression right, int argN, long valueBits,
                       int guardCount) {
    }

    private static final Map<Key, Expression> POOL = new ConcurrentHashMap<>();

    private Expression(Op op, Expression left, Expression right, int argN, double value,
                       int guardCount) {
        this.op = op;
        this.left = left;
        this.right = right;
        this.argN = argN;
        this.value = value;
        this.guardCount = guardCount;
        this.nonNegative = computeNonNegative();
    }

    private boolean computeNonNegative() {
        return switch (op) {
            case ARGUMENT -> false;
            case CONSTANT -> value >= 0;
            case ABS -> true;
            case SUM, MIN -> left.nonNegative && right.nonNegative;
            case DIFFERENCE -> false;
            // A product is non-negative if both factors are, or if it is a square.
            case PRODUCT -> (left.nonNegative && right.nonNegative) || left == right;
            case MAX -> left.nonNegative || right.nonNegative;
        };
    }

    private static Expression intern(Op op, Expression left, Expression right, int argN,
                                     double value, int guardCount) {
        Key key = new Key(op, left, right, argN, Double.doubleToLongBits(value), guardCount);
        return POOL.computeIfAbsent(key,
                k -> new Expression(op, left, right, argN, value, guardCount));
    }

    // ------------------------------------------------------------------
    // Factories
    // ------------------------------------------------------------------

    /** Input placeholder with 1-based index. */
    public static Expression arg(int oneBasedIndex) {
        if (oneBasedIndex < 1) {
            throw new IllegalArgumentException("argument index is 1-based: " + oneBasedIndex);
        }
        return intern(Op.ARGUMENT, null, null, oneBasedIndex, 0.0, 0);
    }

    /** Constant leaf. */
    public static Expression constant(double v) {
        return intern(Op.CONSTANT, null, null, 0, v, 0);
    }

    /**
     * Underflow guard constant with the given multiplier: value is
     * {@code count * Double.MIN_NORMAL}.
     */
    public static Expression underflowGuard(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("guard count must be >= 1: " + count);
        }
        return intern(Op.CONSTANT, null, null, 0, count * Double.MIN_NORMAL, count);
    }

    public static Expression sum(Expression l, Expression r) {
        return intern(Op.SUM, l, r, 0, 0.0, 0);
    }

    public static Expression diff(Expression l, Expression r) {
        return intern(Op.DIFFERENCE, l, r, 0, 0.0, 0);
    }

    public static Expression product(Expression l, Expression r) {
        return intern(Op.PRODUCT, l, r, 0, 0.0, 0);
    }

    public static Expression abs(Expression e) {
        return intern(Op.ABS, e, null, 0, 0.0, 0);
    }

    public static Expression max(Expression l, Expression r) {
        return intern(Op.MAX, l, r, 0, 0.0, 0);
    }

    public static Expression min(Expression l, Expression r) {
        return intern(Op.MIN, l, r, 0, 0.0, 0);
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public Op op() {
        return op;
    }

    /** Left child of a binary node, or the single child of {@link Op#ABS}. */
    public Expression left() {
        return left;
    }

    public Expression right() {
        return right;
    }

    /** 1-based argument index; 0 for non-argument nodes. */
    public int argN() {
        return argN;
    }

    public double value() {
        if (op != Op.CONSTANT) {
            throw new IllegalStateException("value() on non-constant node " + op);
        }
        return value;
    }

    /** Underflow-guard multiplier; 0 if this node is not an underflow guard. */
    public int guardCount() {
        return guardCount;
    }

    public boolean isLeaf() {
        return op == Op.ARGUMENT || op == Op.CONSTANT;
    }

    public boolean isUnderflowGuard() {
        return guardCount > 0;
    }

    /** Statically-known non-negativity. */
    public boolean nonNegative() {
        return nonNegative;
    }

    @Override
    public String toString() {
        return switch (op) {
            case ARGUMENT -> "_" + argN;
            case CONSTANT -> isUnderflowGuard() ? ("guard(" + guardCount + ")")
                    : Double.toString(value);
            case ABS -> "|" + left + "|";
            case SUM -> "(" + left + " + " + right + ")";
            case DIFFERENCE -> "(" + left + " - " + right + ")";
            case PRODUCT -> "(" + left + " * " + right + ")";
            case MAX -> "max(" + left + ", " + right + ")";
            case MIN -> "min(" + left + ", " + right + ")";
        };
    }
}
