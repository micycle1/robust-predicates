package com.github.micycle1.robustpredicates.reference;

import java.math.BigDecimal;

/**
 * Exact reference predicates. Every input double is converted exactly via
 * {@code new BigDecimal(double)} and all arithmetic is unlimited-precision
 * add/subtract/multiply, so the returned sign is the true sign of the
 * predicate polynomial evaluated at the given doubles.
 *
 * <p>The determinants are expanded by first-row Laplace cofactors —
 * deliberately a different evaluation order than the framework's expression
 * trees — over the same matrices, so signs agree by construction while the
 * computation path is independent.
 */
public final class BigDecimalReference {

    private BigDecimalReference() {
    }

    private static BigDecimal bd(double x) {
        return new BigDecimal(x);
    }

    private static BigDecimal det2(BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal d) {
        // | a b |
        // | c d |
        return a.multiply(d).subtract(b.multiply(c));
    }

    private static BigDecimal det3(BigDecimal[][] m) {
        return m[0][0].multiply(det2(m[1][1], m[1][2], m[2][1], m[2][2]))
                .subtract(m[0][1].multiply(det2(m[1][0], m[1][2], m[2][0], m[2][2])))
                .add(m[0][2].multiply(det2(m[1][0], m[1][1], m[2][0], m[2][1])));
    }

    private static BigDecimal det4(BigDecimal[][] m) {
        BigDecimal result = BigDecimal.ZERO;
        for (int j = 0; j < 4; j++) {
            BigDecimal[][] minor = new BigDecimal[3][3];
            for (int r = 1; r < 4; r++) {
                int mc = 0;
                for (int c = 0; c < 4; c++) {
                    if (c == j) {
                        continue;
                    }
                    minor[r - 1][mc++] = m[r][c];
                }
            }
            BigDecimal term = m[0][j].multiply(det3(minor));
            result = (j % 2 == 0) ? result.add(term) : result.subtract(term);
        }
        return result;
    }

    /** Exact sign of {@code (bx-ax)(cy-ay) - (cx-ax)(by-ay)}. */
    public static int orient2d(double ax, double ay, double bx, double by,
                               double cx, double cy) {
        BigDecimal[][] m = {
                {bd(bx).subtract(bd(ax)), bd(by).subtract(bd(ay))},
                {bd(cx).subtract(bd(ax)), bd(cy).subtract(bd(ay))},
        };
        return det2(m[0][0], m[0][1], m[1][0], m[1][1]).signum();
    }

    public static int orient2d(double[] args) {
        return orient2d(args[0], args[1], args[2], args[3], args[4], args[5]);
    }

    /** Exact sign of the 3x3 determinant with rows {@code a-d, b-d, c-d}. */
    public static int orient3d(double ax, double ay, double az,
                               double bx, double by, double bz,
                               double cx, double cy, double cz,
                               double dx, double dy, double dz) {
        BigDecimal[][] m = {
                {bd(ax).subtract(bd(dx)), bd(ay).subtract(bd(dy)), bd(az).subtract(bd(dz))},
                {bd(bx).subtract(bd(dx)), bd(by).subtract(bd(dy)), bd(bz).subtract(bd(dz))},
                {bd(cx).subtract(bd(dx)), bd(cy).subtract(bd(dy)), bd(cz).subtract(bd(dz))},
        };
        return det3(m).signum();
    }

    public static int orient3d(double[] a) {
        return orient3d(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11]);
    }

    /** Exact sign of the classic lifted 3x3 incircle determinant (rows a-d, b-d, c-d). */
    public static int incircle(double ax, double ay, double bx, double by,
                               double cx, double cy, double dx, double dy) {
        BigDecimal adx = bd(ax).subtract(bd(dx));
        BigDecimal ady = bd(ay).subtract(bd(dy));
        BigDecimal bdx = bd(bx).subtract(bd(dx));
        BigDecimal bdy = bd(by).subtract(bd(dy));
        BigDecimal cdx = bd(cx).subtract(bd(dx));
        BigDecimal cdy = bd(cy).subtract(bd(dy));
        BigDecimal[][] m = {
                {adx, ady, adx.multiply(adx).add(ady.multiply(ady))},
                {bdx, bdy, bdx.multiply(bdx).add(bdy.multiply(bdy))},
                {cdx, cdy, cdx.multiply(cdx).add(cdy.multiply(cdy))},
        };
        return det3(m).signum();
    }

    public static int incircle(double[] a) {
        return incircle(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7]);
    }

    /**
     * Exact sign of the lifted 4x4 insphere determinant with rows
     * {@code a-e, b-e, c-e, d-e}.
     */
    public static int insphere(double ax, double ay, double az,
                               double bx, double by, double bz,
                               double cx, double cy, double cz,
                               double dx, double dy, double dz,
                               double ex, double ey, double ez) {
        BigDecimal[][] m = {
                liftedRow(ax, ay, az, ex, ey, ez),
                liftedRow(bx, by, bz, ex, ey, ez),
                liftedRow(cx, cy, cz, ex, ey, ez),
                liftedRow(dx, dy, dz, ex, ey, ez),
        };
        return det4(m).signum();
    }

    public static int insphere(double[] a) {
        return insphere(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8],
                a[9], a[10], a[11], a[12], a[13], a[14]);
    }

    private static BigDecimal[] liftedRow(double x, double y, double z,
                                          double ex, double ey, double ez) {
        BigDecimal dx = bd(x).subtract(bd(ex));
        BigDecimal dy = bd(y).subtract(bd(ey));
        BigDecimal dz = bd(z).subtract(bd(ez));
        BigDecimal lift = dx.multiply(dx).add(dy.multiply(dy)).add(dz.multiply(dz));
        return new BigDecimal[] {dx, dy, dz, lift};
    }

    /** Exact value of the incircle determinant (for error-bound soundness tests). */
    public static BigDecimal incircleValue(double[] a) {
        BigDecimal adx = bd(a[0]).subtract(bd(a[6]));
        BigDecimal ady = bd(a[1]).subtract(bd(a[7]));
        BigDecimal bdx = bd(a[2]).subtract(bd(a[6]));
        BigDecimal bdy = bd(a[3]).subtract(bd(a[7]));
        BigDecimal cdx = bd(a[4]).subtract(bd(a[6]));
        BigDecimal cdy = bd(a[5]).subtract(bd(a[7]));
        BigDecimal[][] m = {
                {adx, ady, adx.multiply(adx).add(ady.multiply(ady))},
                {bdx, bdy, bdx.multiply(bdx).add(bdy.multiply(bdy))},
                {cdx, cdy, cdx.multiply(cdx).add(cdy.multiply(cdy))},
        };
        return det3(m);
    }

    /** Exact value of the orient2d determinant. */
    public static BigDecimal orient2dValue(double[] a) {
        return det2(bd(a[2]).subtract(bd(a[0])), bd(a[3]).subtract(bd(a[1])),
                bd(a[4]).subtract(bd(a[0])), bd(a[5]).subtract(bd(a[1])));
    }

    /** Exact value of the orient3d determinant (rows a-d, b-d, c-d). */
    public static BigDecimal orient3dValue(double[] a) {
        BigDecimal[][] m = {
                {bd(a[0]).subtract(bd(a[9])), bd(a[1]).subtract(bd(a[10])), bd(a[2]).subtract(bd(a[11]))},
                {bd(a[3]).subtract(bd(a[9])), bd(a[4]).subtract(bd(a[10])), bd(a[5]).subtract(bd(a[11]))},
                {bd(a[6]).subtract(bd(a[9])), bd(a[7]).subtract(bd(a[10])), bd(a[8]).subtract(bd(a[11]))},
        };
        return det3(m);
    }

    /** Exact value of the insphere determinant (rows a-e, b-e, c-e, d-e). */
    public static BigDecimal insphereValue(double[] a) {
        BigDecimal[][] m = {
                liftedRow(a[0], a[1], a[2], a[12], a[13], a[14]),
                liftedRow(a[3], a[4], a[5], a[12], a[13], a[14]),
                liftedRow(a[6], a[7], a[8], a[12], a[13], a[14]),
                liftedRow(a[9], a[10], a[11], a[12], a[13], a[14]),
        };
        return det4(m);
    }
}
