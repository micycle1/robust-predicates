package com.github.micycle1.robustpredicates.expansion;

import com.github.micycle1.robustpredicates.ExpansionArithmetic;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exactness tests for the Shewchuk primitives and compound expansion
 * operations: values are checked against unlimited-precision BigDecimal.
 */
class ExpansionArithmeticTest {

    private static final Random RNG = new Random(20260712);

    private static double randomAcrossRegimes() {
        double base = RNG.nextDouble() * 2 - 1;
        int regime = RNG.nextInt(4);
        return switch (regime) {
            case 0 -> base;
            case 1 -> base * 1e12;
            case 2 -> base * 1e-12;
            default -> base * Math.scalb(1.0, RNG.nextInt(80) - 40);
        };
    }

    private static BigDecimal sum(double[] e, int begin, int end) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = begin; i < end; i++) {
            total = total.add(new BigDecimal(e[i]));
        }
        return total;
    }

    private static void assertNonoverlappingOrder(double[] e, int begin, int end) {
        double lastMagnitude = 0;
        for (int i = begin; i < end; i++) {
            if (e[i] != 0) {
                assertTrue(Math.abs(e[i]) >= lastMagnitude,
                        "components must be in increasing magnitude order");
                lastMagnitude = Math.abs(e[i]);
            }
        }
    }

    @Test
    void tailPrimitivesAreExact() {
        for (int i = 0; i < 100_000; i++) {
            double a = randomAcrossRegimes();
            double b = randomAcrossRegimes();

            double s = a + b;
            BigDecimal exactSum = new BigDecimal(a).add(new BigDecimal(b));
            assertEquals(0, exactSum.subtract(new BigDecimal(s))
                    .compareTo(new BigDecimal(ExpansionArithmetic.twoSumTail(a, b, s))));

            double d = a - b;
            BigDecimal exactDiff = new BigDecimal(a).subtract(new BigDecimal(b));
            assertEquals(0, exactDiff.subtract(new BigDecimal(d))
                    .compareTo(new BigDecimal(ExpansionArithmetic.twoDiffTail(a, b, d))));

            double p = a * b;
            BigDecimal exactProd = new BigDecimal(a).multiply(new BigDecimal(b));
            assertEquals(0, exactProd.subtract(new BigDecimal(p))
                    .compareTo(new BigDecimal(ExpansionArithmetic.twoProductTail(a, b, p))));
        }
    }

    @Test
    void growExpansionPreservesValue() {
        for (int i = 0; i < 20_000; i++) {
            double[] e = new double[2];
            int eEnd = ExpansionArithmetic.twoSum(randomAcrossRegimes(), randomAcrossRegimes(),
                    e, 0, false);
            double b = randomAcrossRegimes();
            double[] h = new double[3];
            int hEnd = ExpansionArithmetic.growExpansion(e, 0, eEnd, b, h, 0,
                    false, false, i % 2 == 0);
            assertEquals(0, sum(e, 0, eEnd).add(new BigDecimal(b))
                    .compareTo(sum(h, 0, hEnd)));
            assertNonoverlappingOrder(h, 0, hEnd);

            // negated variants: e - b and (-e) + b
            hEnd = ExpansionArithmetic.growExpansion(e, 0, eEnd, b, h, 0, false, true, false);
            assertEquals(0, sum(e, 0, eEnd).subtract(new BigDecimal(b))
                    .compareTo(sum(h, 0, hEnd)));
            hEnd = ExpansionArithmetic.growExpansion(e, 0, eEnd, b, h, 0, true, false, false);
            assertEquals(0, sum(e, 0, eEnd).negate().add(new BigDecimal(b))
                    .compareTo(sum(h, 0, hEnd)));
        }
    }

    @Test
    void expansionSumsPreserveValue() {
        for (int i = 0; i < 10_000; i++) {
            double[] e = new double[4];
            double[] scratch = new double[2];
            ExpansionArithmetic.twoSum(randomAcrossRegimes(), randomAcrossRegimes(), scratch, 0,
                    false);
            int eEnd = ExpansionArithmetic.growExpansion(scratch, 0, 2, randomAcrossRegimes(),
                    e, 0, false, false, false);
            eEnd = ExpansionArithmetic.growExpansion(e, 0, eEnd, randomAcrossRegimes(),
                    e, 0, false, false, false);

            double[] f = new double[4];
            ExpansionArithmetic.twoProduct(randomAcrossRegimes(), randomAcrossRegimes(), scratch,
                    0, false);
            int fEnd = ExpansionArithmetic.growExpansion(scratch, 0, 2, randomAcrossRegimes(),
                    f, 0, false, false, false);
            fEnd = ExpansionArithmetic.growExpansion(f, 0, fEnd, randomAcrossRegimes(),
                    f, 0, false, false, false);

            boolean ze = i % 2 == 0;
            double[] h = new double[8];

            int hEnd = ExpansionArithmetic.fastExpansionSum(e, 0, eEnd, f, 0, fEnd, h, 0,
                    false, false, ze);
            assertEquals(0, sum(e, 0, eEnd).add(sum(f, 0, fEnd)).compareTo(sum(h, 0, hEnd)),
                    "fast expansion sum value");
            assertNonoverlappingOrder(h, 0, hEnd);

            hEnd = ExpansionArithmetic.fastExpansionSum(e, 0, eEnd, f, 0, fEnd, h, 0,
                    false, true, ze);
            assertEquals(0, sum(e, 0, eEnd).subtract(sum(f, 0, fEnd)).compareTo(sum(h, 0, hEnd)),
                    "fast expansion difference value");

            hEnd = ExpansionArithmetic.expansionSum(e, 0, eEnd, f, 0, fEnd, h, 0,
                    false, false, ze);
            assertEquals(0, sum(e, 0, eEnd).add(sum(f, 0, fEnd)).compareTo(sum(h, 0, hEnd)),
                    "expansion sum value");
            assertNonoverlappingOrder(h, 0, hEnd);

            hEnd = ExpansionArithmetic.expansionSum(e, 0, eEnd, f, 0, fEnd, h, 0,
                    false, true, ze);
            assertEquals(0, sum(e, 0, eEnd).subtract(sum(f, 0, fEnd)).compareTo(sum(h, 0, hEnd)),
                    "expansion difference value");
        }
    }

    @Test
    void scaleAndTimesPreserveValue() {
        for (int i = 0; i < 5_000; i++) {
            double[] e = new double[4];
            double[] scratch = new double[2];
            ExpansionArithmetic.twoProduct(randomAcrossRegimes(), randomAcrossRegimes(), scratch,
                    0, false);
            int eEnd = ExpansionArithmetic.growExpansion(scratch, 0, 2, randomAcrossRegimes(),
                    e, 0, false, false, false);
            eEnd = ExpansionArithmetic.growExpansion(e, 0, eEnd, randomAcrossRegimes(),
                    e, 0, false, false, false);

            double b = randomAcrossRegimes();
            double[] h = new double[8];
            int hEnd = ExpansionArithmetic.scaleExpansion(e, 0, eEnd, b, h, 0, i % 2 == 0);
            assertEquals(0, sum(e, 0, eEnd).multiply(new BigDecimal(b))
                    .compareTo(sum(h, 0, hEnd)), "scale expansion value");
            assertNonoverlappingOrder(h, 0, hEnd);

            double[] f = new double[2];
            int fEnd = ExpansionArithmetic.twoSum(randomAcrossRegimes(), randomAcrossRegimes(),
                    f, 0, false);
            double[] hp = new double[2 * 4 * 2];
            int hpEnd = ExpansionArithmetic.expansionTimes(e, 0, eEnd, f, 0, fEnd, hp, 0,
                    i % 2 == 0);
            assertEquals(0, sum(e, 0, eEnd).multiply(sum(f, 0, fEnd))
                    .compareTo(sum(hp, 0, hpEnd)), "expansion times value");
            assertNonoverlappingOrder(hp, 0, hpEnd);
        }
    }

    @Test
    void twoSquarePreservesValue() {
        for (int i = 0; i < 20_000; i++) {
            double[] e = new double[2];
            int eEnd = ExpansionArithmetic.twoProduct(randomAcrossRegimes(),
                    randomAcrossRegimes(), e, 0, false);
            double[] h = new double[6];
            int hEnd = ExpansionArithmetic.twoSquare(e, 0, eEnd, h, 0, i % 2 == 0);
            BigDecimal value = sum(e, 0, eEnd);
            assertEquals(0, value.multiply(value).compareTo(sum(h, 0, hEnd)),
                    "two-square value");
            assertNonoverlappingOrder(h, 0, hEnd);
        }
    }

    @Test
    void signOfScansForLastNonzero() {
        assertEquals(1, ExpansionArithmetic.signOf(new double[] {0.5, 0, 0}, 0, 3));
        assertEquals(-1, ExpansionArithmetic.signOf(new double[] {1.0, -2.0}, 0, 2));
        assertEquals(0, ExpansionArithmetic.signOf(new double[] {0, 0}, 0, 2));
    }
}
