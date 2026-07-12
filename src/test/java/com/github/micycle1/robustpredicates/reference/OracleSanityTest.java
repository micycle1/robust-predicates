package com.github.micycle1.robustpredicates.reference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Hand-computed cases pinning down the sign conventions of the oracle. */
class OracleSanityTest {

    @Test
    void orient2dConventions() {
        // Counterclockwise triangle -> positive.
        assertEquals(1, BigDecimalReference.orient2d(0, 0, 1, 0, 0, 1));
        // Clockwise -> negative.
        assertEquals(-1, BigDecimalReference.orient2d(0, 0, 0, 1, 1, 0));
        // Collinear -> zero.
        assertEquals(0, BigDecimalReference.orient2d(0, 0, 1, 1, 2, 2));
    }

    @Test
    void orient3dConventions() {
        // det[q-p, r-p, s-p] = det(I) = +1.
        assertEquals(1, BigDecimalReference.orient3d(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1));
        assertEquals(-1, BigDecimalReference.orient3d(0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1));
        // Coplanar -> zero.
        assertEquals(0, BigDecimalReference.orient3d(0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0));
    }

    @Test
    void incircleConventions() {
        // d strictly inside the circumcircle of counterclockwise abc -> positive
        // (hand-computed determinant value 0.375 for this configuration).
        assertEquals(1, BigDecimalReference.incircle(0, 0, 1, 0, 0, 1, 0.25, 0.25));
        // d far outside -> negative.
        assertEquals(-1, BigDecimalReference.incircle(0, 0, 1, 0, 0, 1, 5, 5));
        // d on the circle (cocircular integer points on x^2+y^2=25) -> zero.
        assertEquals(0, BigDecimalReference.incircle(5, 0, 0, 5, -5, 0, 3, -4));
    }

    @Test
    void insphereConventions() {
        // Unit-sphere points p,q,r,s with t at the center: hand-computed
        // determinant (rows p,r,q,s) equals -2, so the sign is negative.
        assertEquals(-1, BigDecimalReference.insphere(
                1, 0, 0, -1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0));
        // t exactly on the sphere -> zero.
        assertEquals(0, BigDecimalReference.insphere(
                1, 0, 0, -1, 0, 0, 0, 1, 0, 0, 0, 1, 0, -1, 0));
        // t far outside flips the sign relative to the center.
        assertEquals(1, BigDecimalReference.insphere(
                1, 0, 0, -1, 0, 0, 0, 1, 0, 0, 0, 1, 10, 10, 10));
    }
}
