package com.github.micycle1.robustpredicates;

import org.junit.jupiter.api.Test;

import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance tests ported from Dan Shapero's C++ {@code predicates} library
 * (https://github.com/danshapero/predicates, {@code test/orient2d.cpp},
 * {@code test/orient3d.cpp}, {@code test/incircle.cpp}).
 *
 * <p>They exercise the public runtime {@link RobustPredicates} entry points
 * directly (not the interpreted framework). The C++ predicates return a
 * signed {@code double}; here the equivalent sign is the {@code int} in
 * {@code {-1, 0, 1}}, so {@code p > 0} becomes {@code == 1}, {@code p < 0}
 * becomes {@code == -1} and {@code p == 0.0} becomes {@code == 0}.
 */
class PortedPredicateAcceptanceTest {

    /** Callback matching the C++ {@code Continuation(p, x, i, j)} signature. */
    @FunctionalInterface
    private interface GridCheck {
        void accept(int p, double[] q, int i, int j);
    }

    /**
     * Port of {@code perturb2d}: sweep an {@code nx * ny} grid of adjacent
     * representable doubles starting at {@code x0}, advancing each coordinate
     * by one ulp toward +inf. {@code Math.nextUp(v)} equals the C++
     * {@code nextafter(v, v + 1)} for every value visited here.
     */
    private static void perturb2d(ToIntFunction<double[]> predicate, double[] x0,
                                  int nx, int ny, GridCheck check) {
        double[] x = {x0[0], x0[1]};
        for (int i = 0; i < ny; i++) {
            x[0] = x0[0];
            for (int j = 0; j < nx; j++) {
                int p = predicate.applyAsInt(x);
                check.accept(p, x, i, j);
                x[0] = Math.nextUp(x[0]);
            }
            x[1] = Math.nextUp(x[1]);
        }
    }

    private static int orient2d(double[] a, double[] b, double[] c) {
        return RobustPredicates.orient2d(a[0], a[1], b[0], b[1], c[0], c[1]);
    }

    private static int incircle(double[] a, double[] b, double[] c, double[] d) {
        return RobustPredicates.incircle(a[0], a[1], b[0], b[1], c[0], c[1], d[0], d[1]);
    }

    // ---- orient2d.cpp ------------------------------------------------------

    @Test
    void orient2dEasyCase() {
        double[] x1 = {0.0, 0.0};
        double[] x2 = {1.0, 1.0};
        double[] x3 = {0.0, 1.0};

        assertTrue(orient2d(x1, x2, x3) > 0);
        assertTrue(orient2d(x1, x3, x2) < 0);
    }

    @Test
    void orient2dNearCollinearGrid() {
        // Three nearly collinear points on the diagonal y = x. Sweep the test
        // point over a 256 x 256 ulp grid around (0.5, 0.5); its sign must match
        // which side of the diagonal it lies on, and this must be invariant under
        // cyclic permutation of the predicate's arguments (as in exact arithmetic).
        double[] q1 = {0.5, 0.5};
        double[] q2 = {12.0, 12.0};
        double[] q3 = {24.0, 24.0};

        ToIntFunction<double[]> predicate1 = q -> orient2d(q, q2, q3);
        ToIntFunction<double[]> predicate2 = q -> orient2d(q3, q, q2);
        ToIntFunction<double[]> predicate3 = q -> orient2d(q2, q3, q);

        GridCheck correct = (p, q, i, j) -> {
            if (q[1] > q[0]) assertTrue(p > 0);
            else if (q[1] < q[0]) assertTrue(p < 0);
            else assertEquals(0, p);
        };

        perturb2d(predicate1, q1, 256, 256, correct);
        perturb2d(predicate2, q1, 256, 256, correct);
        perturb2d(predicate3, q1, 256, 256, correct);
    }

    // ---- orient3d.cpp ------------------------------------------------------

    @Test
    void orient3dCoplanarRegression() {
        // Reported by GitHub user mlivesu: four coplanar points that a buggy
        // `splitter` construction mis-signed. The exact answer is zero, and it
        // must stay zero under reordering.
        double[] x0 = {7.878286361694336, -21.723194122314453, -13.910694122314453};
        double[] x1 = {7.827281951904297, -21.42486572265625, -13.61236572265625};
        double[] x2 = {7.860530853271484, -21.5969181060791, -13.784418106079102};
        double[] x3 = {7.077847957611084, -21.82772445678711, -14.01522445678711};

        assertEquals(0, RobustPredicates.orient3d(
                x0[0], x0[1], x0[2], x1[0], x1[1], x1[2],
                x2[0], x2[1], x2[2], x3[0], x3[1], x3[2]));
        assertEquals(0, RobustPredicates.orient3d(
                x2[0], x2[1], x2[2], x3[0], x3[1], x3[2],
                x0[0], x0[1], x0[2], x1[0], x1[1], x1[2]));
    }

    // ---- incircle.cpp ------------------------------------------------------

    @Test
    void incircleEasyCase() {
        double[] x1 = {1.0, 0.0};
        double[] x2 = {0.0, 1.0};
        double[] x3 = {-1.0, 0.0};

        double[] p = {0.0, Math.nextAfter(-1.0, 0.0)};   // just inside the unit circle
        double[] q = {0.0, Math.nextAfter(-1.0, -2.0)};  // just outside

        assertTrue(incircle(x1, x2, x3, p) > 0);
        assertTrue(incircle(x1, x2, x3, q) < 0);
    }

    @Test
    void incircleNearCocircularGrid() {
        // 325 is a perfect square and a sum of two squares in many ways, giving
        // integer points on a circle of radius n. The test point sweeps a
        // 256 x 256 ulp grid sitting almost exactly on that circle, so the
        // configuration is near-cocircular. The robust sign must agree with the
        // (confident) naive radius comparison wherever the latter is strict.
        final int n = 325;
        int[] s = sumsOfSquares(n);

        int k = s[s.length / 2];
        double[] x = {(double) k, -Math.sqrt((double) n * n - (double) k * k)};

        double[] q1 = {s[0] - x[0], Math.sqrt((double) n * n - (double) s[0] * s[0]) - x[1]};
        double[] q2 = {s[1] - x[0], Math.sqrt((double) n * n - (double) s[1] * s[1]) - x[1]};
        double[] q3 = {s[2] - x[0], Math.sqrt((double) n * n - (double) s[2] * s[2]) - x[1]};

        ToIntFunction<double[]> predicate = q -> incircle(q1, q2, q3, q);

        GridCheck correct = (p, q, i, j) -> {
            double yx = q[0] - x[0];
            double yy = q[1] - x[1];
            double r2 = yx * yx + yy * yy;
            if (r2 < (double) n * n) assertTrue(p > 0);
            else if (r2 > (double) n * n) assertTrue(p < 0);
        };

        double[] q4 = {0.0, 0.0};
        perturb2d(predicate, q4, 256, 256, correct);
    }

    /** All k in (0, n) such that n*n - k*k is a perfect square. */
    private static int[] sumsOfSquares(int n) {
        int[] buf = new int[n];
        int count = 0;
        for (int k = 1; k < n; k++) {
            if (isSquare(n * n - k * k)) {
                buf[count++] = k;
            }
        }
        int[] out = new int[count];
        System.arraycopy(buf, 0, out, 0, count);
        return out;
    }

    private static boolean isSquare(int k) {
        int q = (int) Math.round(Math.sqrt(1.0 * k));
        return q * q == k;
    }
}
