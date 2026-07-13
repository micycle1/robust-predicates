package com.github.micycle1.robustpredicates.bench;

import java.util.concurrent.TimeUnit;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.triangulate.quadedge.TrianglePredicate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import com.github.micycle1.robustpredicates.RobustPredicates;

/**
 * {@code incircle} throughput: this library vs. JTS vs. ProGAL's
 * {@link ExactJavaPredicates#incircle(double[], double[], double[], double[])}.
 *
 * <p>Two JTS variants are measured because its naming is deceptive:
 * <ul>
 *   <li>{@code jtsDD} — {@link TrianglePredicate#isInCircleDDNormalized}, the
 *       genuinely robust double-double implementation (the fair robust-vs-robust
 *       comparison).</li>
 *   <li>{@code jtsFilter} — {@link TrianglePredicate#isInCircleRobust}, which
 *       despite its name evaluates the determinant in plain {@code double}
 *       ("normalized" only subtracts the query point first). Fast, but returns
 *       the wrong sign on hard cocircular inputs — included only to show the
 *       cost of the non-exact fast path.</li>
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class IncircleBenchmark {

    @Param({"uniform", "degenerate"})
    public String dist;

    private double[][][] pts;
    private Coordinate[][] coords;
    private final ExactJavaPredicates progal = new ExactJavaPredicates();
    private int i;

    @Setup(Level.Trial)
    public void setup() {
        pts = Inputs.incircle(dist);
        coords = new Coordinate[Inputs.COUNT][4];
        for (int k = 0; k < Inputs.COUNT; k++) {
            for (int p = 0; p < 4; p++) {
                coords[k][p] = new Coordinate(pts[k][p][0], pts[k][p][1]);
            }
        }
    }

    private int advance() {
        int k = i;
        i = (i + 1) & (Inputs.COUNT - 1);
        return k;
    }

    @Benchmark
    public int robust() {
        double[][] t = pts[advance()];
        return RobustPredicates.incircle(
            t[0][0], t[0][1], t[1][0], t[1][1], t[2][0], t[2][1], t[3][0], t[3][1]);
    }

    @Benchmark
    public boolean jtsDD() {
        Coordinate[] c = coords[advance()];
        return TrianglePredicate.isInCircleDDNormalized(c[0], c[1], c[2], c[3]);
    }

    @Benchmark
    public boolean jtsFilter() {
        Coordinate[] c = coords[advance()];
        return TrianglePredicate.isInCircleRobust(c[0], c[1], c[2], c[3]);
    }

    @Benchmark
    public double progal() {
        double[][] t = pts[advance()];
        return progal.incircle(t[0], t[1], t[2], t[3]);
    }
}
