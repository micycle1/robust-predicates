package com.github.micycle1.robustpredicates.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.algorithm.CGAlgorithmsDD;

import com.github.micycle1.robustpredicates.RobustPredicates;

/**
 * Sanity check that the three benchmarked implementations actually compute the
 * same predicate, so the timings compare equivalent work. All three
 * (RobustPredicates, ProGAL, JTS) are Shewchuk-derived and share the standard
 * counterclockwise sign convention, so their signs must agree exactly.
 *
 * <p>incircle/insphere/orient3d are checked only for RobustPredicates vs.
 * ProGAL (both raw Shewchuk determinants, identical sign for any point order);
 * orient2d is additionally checked against JTS, which uses the same convention.
 */
class PredicateAgreementTest {

    private final ExactJavaPredicates progal = new ExactJavaPredicates();

    private static int sig(double v) {
        return (int) Math.signum(v);
    }

    @Test
    void orient2dAgrees() {
        double[][][] pts = Inputs.orient2d("uniform");
        for (double[][] t : pts) {
            int r = RobustPredicates.orient2d(t[0][0], t[0][1], t[1][0], t[1][1], t[2][0], t[2][1]);
            int j = CGAlgorithmsDD.orientationIndex(t[0][0], t[0][1], t[1][0], t[1][1], t[2][0], t[2][1]);
            int p = sig(progal.orient2d(t[0], t[1], t[2]));
            assertEquals(r, j, "robust vs jts");
            assertEquals(r, p, "robust vs progal");
        }
    }

    @Test
    void incircleAgrees() {
        double[][][] pts = Inputs.incircle("uniform");
        for (double[][] t : pts) {
            int r = RobustPredicates.incircle(
                t[0][0], t[0][1], t[1][0], t[1][1], t[2][0], t[2][1], t[3][0], t[3][1]);
            int p = sig(progal.incircle(t[0], t[1], t[2], t[3]));
            assertEquals(r, p, "robust vs progal");
        }
    }

    @Test
    void orient3dAgrees() {
        double[][][] pts = Inputs.orient3d("uniform");
        for (double[][] t : pts) {
            int r = RobustPredicates.orient3d(
                t[0][0], t[0][1], t[0][2], t[1][0], t[1][1], t[1][2],
                t[2][0], t[2][1], t[2][2], t[3][0], t[3][1], t[3][2]);
            int p = sig(progal.orient3d(t[0], t[1], t[2], t[3]));
            assertEquals(r, p, "robust vs progal");
        }
    }

    @Test
    void insphereAgrees() {
        double[][][] pts = Inputs.insphere("uniform");
        for (double[][] t : pts) {
            int r = RobustPredicates.insphere(
                t[0][0], t[0][1], t[0][2], t[1][0], t[1][1], t[1][2],
                t[2][0], t[2][1], t[2][2], t[3][0], t[3][1], t[3][2],
                t[4][0], t[4][1], t[4][2]);
            int p = sig(progal.insphere(t[0], t[1], t[2], t[3], t[4]));
            assertEquals(r, p, "robust vs progal");
        }
    }
}
