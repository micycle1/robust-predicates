package com.github.micycle1.robustpredicates.bench;

import java.util.SplittableRandom;

/**
 * Deterministic input generation shared by the predicate benchmarks.
 *
 * <p>Two distributions are produced for each predicate:
 * <ul>
 *   <li><b>uniform</b> — coordinates drawn uniformly from {@code [-1, 1)}. The
 *       fast floating-point filter (stage A) resolves the sign almost every
 *       time, so this measures the common-case cost.</li>
 *   <li><b>degenerate</b> — tightly clustered points obtained by perturbing a
 *       shared anchor by a handful of ulps. The predicate value is then within
 *       a few ulps of zero, the stage-A filter usually fails, and the exact
 *       expansion-arithmetic stages run — this measures the worst case that the
 *       adaptive machinery exists to handle.</li>
 * </ul>
 *
 * <p>Each generator returns {@code count} coordinate tuples laid out as one
 * {@code double[]} per point (the shape ProGAL's routines consume directly);
 * the benchmarks read scalars back out for the other libraries.
 */
final class Inputs {

    private Inputs() {
    }

    /** Fixed seed so every run and every library sees identical inputs. */
    static final long SEED = 0x9E3779B97F4A7C15L;

    /** Number of distinct coordinate tuples generated per distribution. */
    static final int COUNT = 4096;

    private static double uniformCoord(SplittableRandom r) {
        return r.nextDouble() * 2.0 - 1.0;
    }

    /** Advance {@code v} by {@code steps} ulps toward +inf (steps may be 0). */
    private static double nudge(double v, int steps) {
        for (int i = 0; i < steps; i++) {
            v = Math.nextUp(v);
        }
        return v;
    }

    // ---- orient2d : tuples of 3 points x {x,y} ----------------------------

    /** {@code [count][3][2]} — a, b, c. */
    static double[][][] orient2d(String dist) {
        SplittableRandom r = new SplittableRandom(SEED);
        double[][][] out = new double[COUNT][3][2];
        boolean deg = "degenerate".equals(dist);
        for (int i = 0; i < COUNT; i++) {
            double ax = uniformCoord(r), ay = uniformCoord(r);
            out[i][0] = new double[] {ax, ay};
            if (deg) {
                // b and c are a nudged by a few ulps -> nearly collinear cluster
                out[i][1] = new double[] {nudge(ax, 1 + r.nextInt(8)), nudge(ay, 1 + r.nextInt(8))};
                out[i][2] = new double[] {nudge(ax, 1 + r.nextInt(8)), nudge(ay, 1 + r.nextInt(8))};
            } else {
                out[i][1] = new double[] {uniformCoord(r), uniformCoord(r)};
                out[i][2] = new double[] {uniformCoord(r), uniformCoord(r)};
            }
        }
        return out;
    }

    // ---- incircle : tuples of 4 points x {x,y} ----------------------------

    /** {@code [count][4][2]} — a, b, c, d. */
    static double[][][] incircle(String dist) {
        SplittableRandom r = new SplittableRandom(SEED);
        double[][][] out = new double[COUNT][4][2];
        boolean deg = "degenerate".equals(dist);
        for (int i = 0; i < COUNT; i++) {
            if (deg) {
                double ax = uniformCoord(r), ay = uniformCoord(r);
                for (int p = 0; p < 4; p++) {
                    out[i][p] = new double[] {nudge(ax, 1 + r.nextInt(8)), nudge(ay, 1 + r.nextInt(8))};
                }
            } else {
                for (int p = 0; p < 4; p++) {
                    out[i][p] = new double[] {uniformCoord(r), uniformCoord(r)};
                }
            }
        }
        return out;
    }

    // ---- orient3d : tuples of 4 points x {x,y,z} --------------------------

    /** {@code [count][4][3]} — a, b, c, d. */
    static double[][][] orient3d(String dist) {
        SplittableRandom r = new SplittableRandom(SEED);
        double[][][] out = new double[COUNT][4][3];
        boolean deg = "degenerate".equals(dist);
        for (int i = 0; i < COUNT; i++) {
            double ax = uniformCoord(r), ay = uniformCoord(r), az = uniformCoord(r);
            out[i][0] = new double[] {ax, ay, az};
            for (int p = 1; p < 4; p++) {
                if (deg) {
                    out[i][p] = new double[] {
                        nudge(ax, 1 + r.nextInt(8)), nudge(ay, 1 + r.nextInt(8)), nudge(az, 1 + r.nextInt(8))};
                } else {
                    out[i][p] = new double[] {uniformCoord(r), uniformCoord(r), uniformCoord(r)};
                }
            }
        }
        return out;
    }

    // ---- insphere : tuples of 5 points x {x,y,z} --------------------------

    /** {@code [count][5][3]} — a, b, c, d, e. */
    static double[][][] insphere(String dist) {
        SplittableRandom r = new SplittableRandom(SEED);
        double[][][] out = new double[COUNT][5][3];
        boolean deg = "degenerate".equals(dist);
        for (int i = 0; i < COUNT; i++) {
            double ax = uniformCoord(r), ay = uniformCoord(r), az = uniformCoord(r);
            out[i][0] = new double[] {ax, ay, az};
            for (int p = 1; p < 5; p++) {
                if (deg) {
                    out[i][p] = new double[] {
                        nudge(ax, 1 + r.nextInt(8)), nudge(ay, 1 + r.nextInt(8)), nudge(az, 1 + r.nextInt(8))};
                } else {
                    out[i][p] = new double[] {uniformCoord(r), uniformCoord(r), uniformCoord(r)};
                }
            }
        }
        return out;
    }
}
