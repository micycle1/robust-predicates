package com.github.micycle1.robustpredicates.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Input generators for predicate tests: uniform random inputs in several
 * magnitude regimes, exactly degenerate configurations (collinear, cocircular,
 * coplanar, cospherical on integer grids), and near-degenerate inputs obtained
 * by perturbing single coordinates by a few ulps.
 */
public final class InputGenerators {

    private InputGenerators() {
    }

    // ------------------------------------------------------------------
    // Random inputs
    // ------------------------------------------------------------------

    /** Uniform random arguments in {@code [-scale, scale)} shifted by {@code offset}. */
    public static double[] random(Random rng, int n, double scale, double offset) {
        double[] a = new double[n];
        for (int i = 0; i < n; i++) {
            a[i] = (rng.nextDouble() * 2 - 1) * scale + offset;
        }
        return a;
    }

    /** A batch of random inputs across magnitude regimes. */
    public static List<double[]> randomBatch(Random rng, int n, int perRegime) {
        List<double[]> batch = new ArrayList<>();
        double[][] regimes = {
                {1.0, 0.0},        // unit scale
                {1e-3, 10.0},      // clustered far from origin (cancellation)
                {1e6, 0.0},        // large scale
                {1e-9, 1e-3},      // small offsets
        };
        for (double[] regime : regimes) {
            for (int i = 0; i < perRegime; i++) {
                batch.add(random(rng, n, regime[0], regime[1]));
            }
        }
        return batch;
    }

    // ------------------------------------------------------------------
    // Exactly degenerate configurations
    // ------------------------------------------------------------------

    /** Collinear orient2d inputs: three points on an integer line. */
    public static List<double[]> collinear2d(Random rng, int count) {
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int x0 = rng.nextInt(41) - 20;
            int y0 = rng.nextInt(41) - 20;
            int dx = rng.nextInt(9) - 4;
            int dy = rng.nextInt(9) - 4;
            int t1 = rng.nextInt(9) - 4;
            int t2 = rng.nextInt(9) - 4;
            out.add(new double[] {x0, y0, x0 + t1 * dx, y0 + t1 * dy, x0 + t2 * dx, y0 + t2 * dy});
        }
        return out;
    }

    /** Integer points exactly on the circle x^2 + y^2 = 25. */
    private static final int[][] CIRCLE_25 = {
            {5, 0}, {4, 3}, {3, 4}, {0, 5}, {-3, 4}, {-4, 3},
            {-5, 0}, {-4, -3}, {-3, -4}, {0, -5}, {3, -4}, {4, -3},
    };

    /** Cocircular incircle inputs: four distinct integer points on a circle. */
    public static List<double[]> cocircular(Random rng, int count) {
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int[] idx = pickDistinct(rng, CIRCLE_25.length, 4);
            out.add(new double[] {
                    CIRCLE_25[idx[0]][0], CIRCLE_25[idx[0]][1],
                    CIRCLE_25[idx[1]][0], CIRCLE_25[idx[1]][1],
                    CIRCLE_25[idx[2]][0], CIRCLE_25[idx[2]][1],
                    CIRCLE_25[idx[3]][0], CIRCLE_25[idx[3]][1],
            });
        }
        return out;
    }

    /** Coplanar orient3d inputs: four integer points in a plane. */
    public static List<double[]> coplanar(Random rng, int count) {
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int[] u = {rng.nextInt(7) - 3, rng.nextInt(7) - 3, rng.nextInt(7) - 3};
            int[] v = {rng.nextInt(7) - 3, rng.nextInt(7) - 3, rng.nextInt(7) - 3};
            int[] o = {rng.nextInt(11) - 5, rng.nextInt(11) - 5, rng.nextInt(11) - 5};
            double[] a = new double[12];
            for (int pt = 0; pt < 4; pt++) {
                int s = rng.nextInt(7) - 3;
                int t = rng.nextInt(7) - 3;
                for (int c = 0; c < 3; c++) {
                    a[pt * 3 + c] = o[c] + s * u[c] + t * v[c];
                }
            }
            out.add(a);
        }
        return out;
    }

    /** Integer points exactly on the sphere x^2 + y^2 + z^2 = 9. */
    private static final int[][] SPHERE_9 = {
            {3, 0, 0}, {-3, 0, 0}, {0, 3, 0}, {0, -3, 0}, {0, 0, 3}, {0, 0, -3},
            {1, 2, 2}, {1, 2, -2}, {1, -2, 2}, {1, -2, -2},
            {-1, 2, 2}, {-1, 2, -2}, {-1, -2, 2}, {-1, -2, -2},
            {2, 1, 2}, {2, 1, -2}, {2, -1, 2}, {2, -1, -2},
            {-2, 1, 2}, {-2, 1, -2}, {-2, -1, 2}, {-2, -1, -2},
            {2, 2, 1}, {2, 2, -1}, {2, -2, 1}, {2, -2, -1},
            {-2, 2, 1}, {-2, 2, -1}, {-2, -2, 1}, {-2, -2, -1},
    };

    /** Cospherical insphere inputs: five distinct integer points on a sphere. */
    public static List<double[]> cospherical(Random rng, int count) {
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int[] idx = pickDistinct(rng, SPHERE_9.length, 5);
            double[] a = new double[15];
            for (int pt = 0; pt < 5; pt++) {
                for (int c = 0; c < 3; c++) {
                    a[pt * 3 + c] = SPHERE_9[idx[pt]][c];
                }
            }
            out.add(a);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Near-degenerate perturbations
    // ------------------------------------------------------------------

    /**
     * Copies {@code args} and moves one coordinate by {@code k} ulps (k may be
     * negative). A zero coordinate is instead offset by {@code k * 2^-52} so
     * the perturbation stays in the normal range: like Shewchuk's original
     * routines, the exact stages assume intermediate products do not
     * underflow, so denormal coordinates are outside the supported domain.
     */
    public static double[] perturbUlps(double[] args, int index, int k) {
        double[] out = args.clone();
        double v = out[index];
        if (v == 0.0) {
            out[index] = k * 0x1.0p-52;
            return out;
        }
        for (int i = 0; i < Math.abs(k); i++) {
            v = k > 0 ? Math.nextUp(v) : Math.nextDown(v);
        }
        out[index] = v;
        return out;
    }

    /** All single-coordinate perturbations of each input by -3..3 ulps. */
    public static List<double[]> perturbAll(List<double[]> inputs, Random rng, int perInput) {
        List<double[]> out = new ArrayList<>();
        for (double[] args : inputs) {
            for (int i = 0; i < perInput; i++) {
                int index = rng.nextInt(args.length);
                int k = rng.nextInt(7) - 3;
                out.add(perturbUlps(args, index, k));
            }
        }
        return out;
    }

    /**
     * The notebook's near-cocircular example: rational points on the unit
     * circle rounded to double.
     */
    public static double[] notebookIncircle() {
        return new double[] {
                -15.0 / 17.0, 8.0 / 17.0,
                15.0 / 17.0, 8.0 / 17.0,
                -4.0 / 5.0, 3.0 / 5.0,
                4.0 / 5.0, 3.0 / 5.0,
        };
    }

    private static int[] pickDistinct(Random rng, int bound, int count) {
        int[] out = new int[count];
        outer:
        for (int i = 0; i < count; ) {
            int candidate = rng.nextInt(bound);
            for (int j = 0; j < i; j++) {
                if (out[j] == candidate) {
                    continue outer;
                }
            }
            out[i++] = candidate;
        }
        return out;
    }
}
