package com.github.micycle1.robustpredicates.expr;

/** Operator kinds of expression tree nodes. */
public enum Op {
    /** Leaf: input argument (1-based index). */
    ARGUMENT,
    /** Leaf: compile-time constant (including underflow guards). */
    CONSTANT,
    SUM,
    DIFFERENCE,
    PRODUCT,
    /** Unary absolute value (used in error-bound magnitude expressions). */
    ABS,
    /** Binary maximum (used in interval/static error-bound expressions). */
    MAX,
    /** Binary minimum (used in interval/static error-bound expressions). */
    MIN
}
