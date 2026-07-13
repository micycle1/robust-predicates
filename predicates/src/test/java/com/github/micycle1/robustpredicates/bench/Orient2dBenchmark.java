package com.github.micycle1.robustpredicates.bench;

import java.util.concurrent.TimeUnit;

import org.locationtech.jts.algorithm.CGAlgorithmsDD;
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
 * {@code orient2d} throughput: this library vs. JTS
 * {@link CGAlgorithmsDD#orientationIndex} (double-double filtered) vs. ProGAL's
 * {@link ExactJavaPredicates#orient2d(double[], double[], double[])}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class Orient2dBenchmark {

    @Param({"uniform", "degenerate"})
    public String dist;

    private double[][][] pts;
    private final ExactJavaPredicates progal = new ExactJavaPredicates();
    private int i;

    @Setup(Level.Trial)
    public void setup() {
        pts = Inputs.orient2d(dist);
    }

    private double[][] next() {
        double[][] t = pts[i];
        i = (i + 1) & (Inputs.COUNT - 1);
        return t;
    }

    @Benchmark
    public int robust() {
        double[][] t = next();
        return RobustPredicates.orient2d(
            t[0][0], t[0][1], t[1][0], t[1][1], t[2][0], t[2][1]);
    }

    @Benchmark
    public int jts() {
        double[][] t = next();
        return CGAlgorithmsDD.orientationIndex(
            t[0][0], t[0][1], t[1][0], t[1][1], t[2][0], t[2][1]);
    }

    @Benchmark
    public double progal() {
        double[][] t = next();
        return progal.orient2d(t[0], t[1], t[2]);
    }
}
