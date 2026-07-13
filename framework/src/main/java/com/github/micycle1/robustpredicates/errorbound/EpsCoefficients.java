package com.github.micycle1.robustpredicates.errorbound;

/**
 * Integer coefficients {@code (a0, a1, a2)} of an error polynomial in the unit
 * roundoff {@code u = 2^-53}: a bound of the form
 * {@code (a0*u + a1*u^2 + a2*u^3) * magnitude}.
 */
public record EpsCoefficients(long a0, long a1, long a2) {

    public static final EpsCoefficients ZERO = new EpsCoefficients(0, 0, 0);
    public static final EpsCoefficients ONE_EPS = new EpsCoefficients(1, 0, 0);

    /** Lexicographically larger of the two coefficient triples. */
    public static EpsCoefficients max(EpsCoefficients a, EpsCoefficients b) {
        boolean aBigger = a.a0 > b.a0
                || (a.a0 == b.a0 && a.a1 > b.a1)
                || (a.a0 == b.a0 && a.a1 == b.a1 && a.a2 > b.a2);
        return aBigger ? a : b;
    }

    /** Coefficients of {@code (1 + A(u)) * (1 + B(u)) - 1} truncated to degree 3. */
    public static EpsCoefficients product(EpsCoefficients a, EpsCoefficients b) {
        return new EpsCoefficients(
                a.a0 + b.a0,
                a.a1 + b.a1 + a.a0 * b.a0,
                a.a2 + b.a2 + a.a0 * b.a1 + a.a1 * b.a0);
    }

    /** Multiplies the bound by {@code (1 + u)} (one extra rounding). */
    public EpsCoefficients multBy1PlusEps() {
        return new EpsCoefficients(a0, a1 + a0, a2 + a1);
    }

    /** Adds one {@code u} for the rounding of the current operation. */
    public EpsCoefficients incFirst() {
        return new EpsCoefficients(a0 + 1, a1, a2);
    }

    /** Conservatively divides the bound by {@code (1 - u)}. */
    public EpsCoefficients divBy1MinusEps() {
        return new EpsCoefficients(a0, a0 + a1, a0 + a1 + a2 + 1);
    }

    /** Smallest power of two {@code >= n} ({@code 0} for {@code n == 0}). */
    public static long roundToNextPow2(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("expects non-negative integer: " + n);
        }
        if (n == 0) {
            return 0;
        }
        long out = 1;
        while (out < n) {
            out *= 2;
        }
        return out;
    }
}
