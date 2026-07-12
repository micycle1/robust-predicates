package com.github.micycle1.robustpredicates.expansion;

import java.util.Arrays;

/**
 * Shewchuk-style floating-point expansion arithmetic
 * (port of {@code expansion_arithmetic.hpp}).
 *
 * <p>An <em>expansion</em> is a sum of doubles stored in increasing order of
 * magnitude (zeros may be interspersed) whose components are nonoverlapping.
 * All routines operate on slices {@code [begin, end)} of flat {@code double[]}
 * buffers and return the new end index of the output expansion.
 *
 * <p>Zero elimination ({@code ze}) drops zero components as they are produced,
 * shortening the output; without it an operation writes a fixed number of
 * components. Either way the output is a valid expansion and its sign is the
 * sign of its last nonzero component ({@link #signOf}).
 *
 * <p>This class is also the runtime support library for the generated
 * {@code com.github.micycle1.robustpredicates.generated.RobustPredicates} routines.
 *
 * <p>Deviations from the C++ source (documented, value-preserving):
 * <ul>
 *   <li>No {@code MostSigOnly} mode: callers keep full-size roots and use
 *       {@link #signOf} instead.</li>
 *   <li>The in-place fast expansion sum is replaced by copying the two input
 *       ranges before summing ({@link #expansionTimes}).</li>
 *   <li>The C++ scalar-minus-expansion overload negates the wrong operand
 *       (computing {@code f - e} instead of {@code e - f}); it is unreachable
 *       for the shipped predicates. The Java equivalent (grow with
 *       {@code negE = true}) is implemented correctly.</li>
 * </ul>
 */
public final class ExpansionArithmetic {

    private ExpansionArithmetic() {
    }

    // ------------------------------------------------------------------
    // Error-free transformation tails
    // ------------------------------------------------------------------

    /** Tail of {@code x = a + b} (Knuth TwoSum). */
    public static double twoSumTail(double a, double b, double x) {
        double bVirtual = x - a;
        double aVirtual = x - bVirtual;
        double bRounded = b - bVirtual;
        double aRounded = a - aVirtual;
        return aRounded + bRounded;
    }

    /** Tail of {@code x = a + b} when {@code |a| >= |b|} (Dekker FastTwoSum). */
    public static double fastTwoSumTail(double a, double b, double x) {
        double bVirtual = x - a;
        return b - bVirtual;
    }

    /** Tail of {@code x = a - b}. */
    public static double twoDiffTail(double a, double b, double x) {
        double bVirtual = a - x;
        double aVirtual = x + bVirtual;
        double bRounded = bVirtual - b;
        double aRounded = a - aVirtual;
        return aRounded + bRounded;
    }

    /** Tail of {@code x = a * b} via fused multiply-add. */
    public static double twoProductTail(double a, double b, double x) {
        return Math.fma(a, b, -x);
    }

    // ------------------------------------------------------------------
    // Component insertion (C++ insert_ze / insert_ze_final)
    // ------------------------------------------------------------------

    private static int insert(double[] h, int out, double val, boolean ze) {
        if (ze && val == 0.0) {
            return out;
        }
        h[out] = val;
        return out + 1;
    }

    /**
     * Final (most significant) insertion: always writes when the expansion
     * would otherwise be empty, so every expansion has length {@code >= 1}.
     */
    private static int insertFinal(double[] h, int out, int start, double val, boolean ze) {
        if (ze && val == 0.0 && out != start) {
            return out;
        }
        h[out] = val;
        return out + 1;
    }

    // ------------------------------------------------------------------
    // Scalar-scalar expansions (length-2 results)
    // ------------------------------------------------------------------

    /** {@code h[hb..) = a + b} as an expansion; returns the new end. */
    public static int twoSum(double a, double b, double[] h, int hb, boolean ze) {
        double x = a + b;
        double y = twoSumTail(a, b, x);
        int hIt = insert(h, hb, y, ze);
        return insertFinal(h, hIt, hb, x, ze);
    }

    /** {@code h[hb..) = a - b} as an expansion; returns the new end. */
    public static int twoDiff(double a, double b, double[] h, int hb, boolean ze) {
        double x = a - b;
        double y = twoDiffTail(a, b, x);
        int hIt = insert(h, hb, y, ze);
        return insertFinal(h, hIt, hb, x, ze);
    }

    /** {@code h[hb..) = a * b} as an expansion; returns the new end. */
    public static int twoProduct(double a, double b, double[] h, int hb, boolean ze) {
        double x = a * b;
        double y = twoProductTail(a, b, x);
        int hIt = insert(h, hb, y, ze);
        return insertFinal(h, hIt, hb, x, ze);
    }

    // ------------------------------------------------------------------
    // Expansion (+/-) scalar: GROW-EXPANSION
    // ------------------------------------------------------------------

    /**
     * Adds the scalar {@code b} to the expansion {@code e[eBegin, eEnd)},
     * writing the result at {@code h[hBegin..)}. {@code negE}/{@code negB}
     * negate the expansion components / the scalar on the fly. May be used
     * in place ({@code h == e}, {@code hBegin == eBegin}): writes never
     * overtake reads.
     *
     * @return the new end index in {@code h}
     */
    public static int growExpansion(double[] e, int eBegin, int eEnd, double b,
                                    double[] h, int hBegin,
                                    boolean negE, boolean negB, boolean ze) {
        double q = negB ? -b : b;
        int hIt = hBegin;
        for (int i = eBegin; i < eEnd; i++) {
            double ei = negE ? -e[i] : e[i];
            double qNew = ei + q;
            double hNew = twoSumTail(ei, q, qNew);
            q = qNew;
            hIt = insert(h, hIt, hNew, ze);
        }
        return insertFinal(h, hIt, hBegin, q, ze);
    }

    // ------------------------------------------------------------------
    // Expansion (+/-) expansion
    // ------------------------------------------------------------------

    private static boolean expansionSumAdvance(double e, double b, boolean negE, boolean negB,
                                               boolean ze) {
        if (!ze) {
            return true;
        }
        double ev = negE ? -e : e;
        double bv = negB ? -b : b;
        double q = ev + bv;
        return twoSumTail(ev, bv, q) != 0.0;
    }

    /**
     * Shewchuk EXPANSION-SUM (O(m*n) repeated grow), used when at least one
     * operand has static length {@code <= 2}. {@code negF} subtracts {@code f}.
     * Output region must be disjoint from both inputs.
     *
     * @return the new end index in {@code h}
     */
    public static int expansionSum(double[] e, int eBegin, int eEnd,
                                   double[] f, int fBegin, int fEnd,
                                   double[] h, int hBegin,
                                   boolean negE, boolean negF, boolean ze) {
        int fLen = fEnd - fBegin;
        int fIt = fBegin;
        int hBeginI = hBegin;
        boolean advance = expansionSumAdvance(e[eBegin], f[fIt], negE, negF, ze);
        int hIt = growExpansion(e, eBegin, eEnd, f[fIt], h, hBegin, negE, negF, ze);
        if (advance) {
            hBeginI++;
        }
        fIt++;
        for (int i = 1; i < fLen; i++) {
            advance = expansionSumAdvance(h[hBeginI], f[fIt], false, negF, ze);
            hIt = growExpansion(h, hBeginI, hIt, f[fIt], h, hBeginI, false, negF, ze);
            if (advance) {
                hBeginI++;
            }
            fIt++;
        }
        return hIt;
    }

    /**
     * Shewchuk FAST-EXPANSION-SUM (merge by magnitude, then one TwoSum sweep),
     * used when both operands have static length {@code > 2}. {@code negE}/
     * {@code negF} negate operands on the fly. Output region must be disjoint
     * from both inputs.
     *
     * @return the new end index in {@code h}
     */
    public static int fastExpansionSum(double[] e, int eBegin, int eEnd,
                                       double[] f, int fBegin, int fEnd,
                                       double[] h, int hBegin,
                                       boolean negE, boolean negF, boolean ze) {
        int eIt = eBegin;
        int fIt = fBegin;
        double q;
        if (Math.abs(f[fIt]) > Math.abs(e[eIt])) {
            q = negE ? -e[eIt] : e[eIt];
            eIt++;
        } else {
            q = negF ? -f[fIt] : f[fIt];
            fIt++;
        }
        int hIt = hBegin;
        if (eIt < eEnd && fIt < fEnd) {
            double qNew;
            double hNew;
            if (Math.abs(f[fIt]) > Math.abs(e[eIt])) {
                double ev = negE ? -e[eIt] : e[eIt];
                qNew = ev + q;
                hNew = fastTwoSumTail(ev, q, qNew);
                eIt++;
            } else {
                double fv = negF ? -f[fIt] : f[fIt];
                qNew = fv + q;
                hNew = fastTwoSumTail(fv, q, qNew);
                fIt++;
            }
            q = qNew;
            hIt = insert(h, hIt, hNew, ze);
            while (eIt < eEnd && fIt < fEnd) {
                if (Math.abs(f[fIt]) > Math.abs(e[eIt])) {
                    double ev = negE ? -e[eIt] : e[eIt];
                    qNew = ev + q;
                    hNew = twoSumTail(ev, q, qNew);
                    eIt++;
                } else {
                    double fv = negF ? -f[fIt] : f[fIt];
                    qNew = fv + q;
                    hNew = twoSumTail(fv, q, qNew);
                    fIt++;
                }
                q = qNew;
                hIt = insert(h, hIt, hNew, ze);
            }
        }
        while (eIt < eEnd) {
            double ev = negE ? -e[eIt] : e[eIt];
            double qNew = ev + q;
            double hNew = twoSumTail(ev, q, qNew);
            hIt = insert(h, hIt, hNew, ze);
            q = qNew;
            eIt++;
        }
        while (fIt < fEnd) {
            double fv = negF ? -f[fIt] : f[fIt];
            double qNew = fv + q;
            double hNew = twoSumTail(fv, q, qNew);
            hIt = insert(h, hIt, hNew, ze);
            q = qNew;
            fIt++;
        }
        return insertFinal(h, hIt, hBegin, q, ze);
    }

    // ------------------------------------------------------------------
    // Expansion * scalar: SCALE-EXPANSION
    // ------------------------------------------------------------------

    /**
     * Multiplies the expansion {@code e[eBegin, eEnd)} by the scalar {@code b}.
     * Output region must be disjoint from the input.
     *
     * @return the new end index in {@code h}
     */
    public static int scaleExpansion(double[] e, int eBegin, int eEnd, double b,
                                     double[] h, int hBegin, boolean ze) {
        int eIt = eBegin;
        int hIt = hBegin;
        double q = e[eIt] * b;
        double hNew = twoProductTail(e[eIt], b, q);
        hIt = insert(h, hIt, hNew, ze);
        eIt++;
        for (; eIt < eEnd; eIt++) {
            double product1 = e[eIt] * b;
            double product0 = twoProductTail(e[eIt], b, product1);
            double s = q + product0;
            hNew = twoSumTail(q, product0, s);
            hIt = insert(h, hIt, hNew, ze);
            q = product1 + s;
            hNew = twoSumTail(product1, s, q);
            hIt = insert(h, hIt, hNew, ze);
        }
        return insertFinal(h, hIt, hBegin, q, ze);
    }

    // ------------------------------------------------------------------
    // Expansion * expansion
    // ------------------------------------------------------------------

    /**
     * Square of a length-2 expansion into at most 6 components
     * (port of the C++ {@code two_square_impl}).
     *
     * @return the new end index in {@code h}
     */
    public static int twoSquare(double[] e, int eBegin, int eEnd,
                                double[] h, int hBegin, boolean ze) {
        if (eBegin + 1 == eEnd) {
            // Input was zero-eliminated down to a single component.
            return twoProduct(e[eBegin], e[eBegin], h, hBegin, ze);
        }
        double[] cache = new double[5];
        cache[2] = e[eBegin] * e[eBegin];
        int hIt = hBegin;
        hIt = insert(h, hIt, twoProductTail(e[eBegin], e[eBegin], cache[2]), ze);
        double twiceLow = 2.0 * e[eBegin];
        cache[1] = e[eBegin + 1] * twiceLow;
        cache[0] = twoProductTail(e[eBegin + 1], twiceLow, cache[1]);
        growExpansion(cache, 0, 2, cache[2], cache, 2, false, false, false);
        hIt = insert(h, hIt, cache[2], ze);
        cache[1] = e[eBegin + 1] * e[eBegin + 1];
        cache[0] = twoProductTail(e[eBegin + 1], e[eBegin + 1], cache[1]);
        return expansionSum(cache, 0, 2, cache, 3, 5, h, hIt, false, false, ze);
    }

    /**
     * Default zero-elimination policy of the C++ framework: eliminate zeros
     * when the (static) result length exceeds 16.
     */
    public static boolean zeDefault(int length) {
        return length > 16;
    }

    /**
     * Full expansion product by divide-and-conquer over the shorter operand
     * (port of the C++ {@code expansion_times_impl}). Intermediate
     * zero-elimination decisions use {@link #zeDefault} on the (dynamic)
     * sub-result lengths; {@code ze} controls the final result. Output region
     * must be disjoint from both inputs and hold {@code 2 * eLen * fLen}
     * components.
     *
     * @return the new end index in {@code h}
     */
    public static int expansionTimes(double[] e, int eBegin, int eEnd,
                                     double[] f, int fBegin, int fEnd,
                                     double[] h, int hBegin, boolean ze) {
        int eLen = eEnd - eBegin;
        int fLen = fEnd - fBegin;
        if (eLen > fLen) {
            return expansionTimes(f, fBegin, fEnd, e, eBegin, eEnd, h, hBegin, ze);
        }
        if (eLen == 0) {
            return hBegin;
        }
        if (eLen == 1) {
            return scaleExpansion(f, fBegin, fEnd, e[eBegin], h, hBegin, ze);
        }
        int eMid = eBegin + eLen / 2;
        int hMid = hBegin + (eMid - eBegin) * fLen * 2;
        boolean zeLow = zeDefault((eMid - eBegin) * fLen * 2);
        boolean zeHigh = zeDefault((eEnd - eMid) * fLen * 2);
        int h1End = expansionTimes(e, eBegin, eMid, f, fBegin, fEnd, h, hBegin, zeLow);
        int h2End = expansionTimes(e, eMid, eEnd, f, fBegin, fEnd, h, hMid, zeHigh);
        // Merge the two adjacent sub-products. The C++ uses an in-place fast
        // expansion sum; copying the inputs first is simpler and equivalent.
        double[] tmpE = Arrays.copyOfRange(h, hBegin, h1End);
        double[] tmpF = Arrays.copyOfRange(h, hMid, h2End);
        return fastExpansionSum(tmpE, 0, tmpE.length, tmpF, 0, tmpF.length,
                h, hBegin, false, false, ze);
    }

    // ------------------------------------------------------------------
    // Sign extraction
    // ------------------------------------------------------------------

    /**
     * Sign of the expansion {@code e[begin, end)}: the sign of its last
     * (largest-magnitude) nonzero component, or 0 if all components are zero.
     */
    public static int signOf(double[] e, int begin, int end) {
        for (int i = end - 1; i >= begin; i--) {
            if (e[i] != 0.0) {
                return e[i] > 0.0 ? 1 : -1;
            }
        }
        return 0;
    }
}
