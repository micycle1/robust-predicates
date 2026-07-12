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

    /** Exact sign of {@code det[q-p, r-p, s-p]} (columns). */
    public static int orient3d(double px, double py, double pz,
                               double qx, double qy, double qz,
                               double rx, double ry, double rz,
                               double sx, double sy, double sz) {
        BigDecimal[][] m = {
                {bd(qx).subtract(bd(px)), bd(rx).subtract(bd(px)), bd(sx).subtract(bd(px))},
                {bd(qy).subtract(bd(py)), bd(ry).subtract(bd(py)), bd(sy).subtract(bd(py))},
                {bd(qz).subtract(bd(pz)), bd(rz).subtract(bd(pz)), bd(sz).subtract(bd(pz))},
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
     * Exact sign of the lifted 4x4 insphere determinant with row order
     * {@code p, r, q, s} (the C++ kernel's {@code side_of_oriented_sphere_3}).
     */
    public static int insphere(double px, double py, double pz,
                               double qx, double qy, double qz,
                               double rx, double ry, double rz,
                               double sx, double sy, double sz,
                               double tx, double ty, double tz) {
        BigDecimal[] p = liftedRow(px, py, pz, tx, ty, tz);
        BigDecimal[] q = liftedRow(qx, qy, qz, tx, ty, tz);
        BigDecimal[] r = liftedRow(rx, ry, rz, tx, ty, tz);
        BigDecimal[] s = liftedRow(sx, sy, sz, tx, ty, tz);
        BigDecimal[][] m = {p, r, q, s};
        return det4(m).signum();
    }

    public static int insphere(double[] a) {
        return insphere(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8],
                a[9], a[10], a[11], a[12], a[13], a[14]);
    }

    private static BigDecimal[] liftedRow(double x, double y, double z,
                                          double tx, double ty, double tz) {
        BigDecimal dx = bd(x).subtract(bd(tx));
        BigDecimal dy = bd(y).subtract(bd(ty));
        BigDecimal dz = bd(z).subtract(bd(tz));
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

    /** Exact value of the orient3d determinant. */
    public static BigDecimal orient3dValue(double[] a) {
        BigDecimal[][] m = {
                {bd(a[3]).subtract(bd(a[0])), bd(a[6]).subtract(bd(a[0])), bd(a[9]).subtract(bd(a[0]))},
                {bd(a[4]).subtract(bd(a[1])), bd(a[7]).subtract(bd(a[1])), bd(a[10]).subtract(bd(a[1]))},
                {bd(a[5]).subtract(bd(a[2])), bd(a[8]).subtract(bd(a[2])), bd(a[11]).subtract(bd(a[2]))},
        };
        return det3(m);
    }

    /** Exact value of the insphere determinant (rows p, r, q, s). */
    public static BigDecimal insphereValue(double[] a) {
        BigDecimal[] p = liftedRow(a[0], a[1], a[2], a[12], a[13], a[14]);
        BigDecimal[] q = liftedRow(a[3], a[4], a[5], a[12], a[13], a[14]);
        BigDecimal[] r = liftedRow(a[6], a[7], a[8], a[12], a[13], a[14]);
        BigDecimal[] s = liftedRow(a[9], a[10], a[11], a[12], a[13], a[14]);
        return det4(new BigDecimal[][] {p, r, q, s});
    }
}
