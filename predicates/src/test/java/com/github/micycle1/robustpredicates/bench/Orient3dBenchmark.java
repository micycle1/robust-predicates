package com.github.micycle1.robustpredicates.bench;

import java.util.concurrent.TimeUnit;

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
 * {@code orient3d} throughput: this library vs. ProGAL's
 * {@link ExactJavaPredicates#orient3d(double[], double[], double[], double[])}.
 * JTS ships no 3D predicates, so it is not compared here.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class Orient3dBenchmark {

    @Param({"uniform", "degenerate"})
    public String dist;

    private double[][][] pts;
    private final ExactJavaPredicates progal = new ExactJavaPredicates();
    private int i;

    @Setup(Level.Trial)
    public void setup() {
        pts = Inputs.orient3d(dist);
    }

    private double[][] next() {
        double[][] t = pts[i];
        i = (i + 1) & (Inputs.COUNT - 1);
        return t;
    }

    @Benchmark
    public int robust() {
        double[][] t = next();
        return RobustPredicates.orient3d(
            t[0][0], t[0][1], t[0][2], t[1][0], t[1][1], t[1][2],
            t[2][0], t[2][1], t[2][2], t[3][0], t[3][1], t[3][2]);
    }

    @Benchmark
    public double progal() {
        double[][] t = next();
        return progal.orient3d(t[0], t[1], t[2], t[3]);
    }
}
