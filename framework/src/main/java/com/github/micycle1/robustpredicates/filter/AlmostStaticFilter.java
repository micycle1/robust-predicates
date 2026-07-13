package com.github.micycle1.robustpredicates.filter;

import com.github.micycle1.robustpredicates.expr.Expression;

import java.util.Arrays;

/**
 * Static filter with incrementally maintained extrema:
 * useful in incremental algorithms where
 * inputs arrive over time. {@link #update} widens the running per-coordinate
 * extrema; the inner static bound is rebuilt only when an extremum changed.
 * Before any update the bound is infinite and the filter is always uncertain.
 */
public final class AlmostStaticFilter implements Stage {

    private final double[] maxima;
    private final double[] minima;
    private StaticFilter inner;
    private boolean dirty;

    public AlmostStaticFilter(Expression expression) {
        this.inner = new StaticFilter(expression);
        this.maxima = new double[inner.argCount()];
        this.minima = new double[inner.argCount()];
        Arrays.fill(maxima, Double.NEGATIVE_INFINITY);
        Arrays.fill(minima, Double.POSITIVE_INFINITY);
        this.dirty = false;
    }

    @Override
    public boolean stateful() {
        return true;
    }

    @Override
    public void update(double[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] > maxima[i]) {
                maxima[i] = args[i];
                dirty = true;
            }
            if (args[i] < minima[i]) {
                minima[i] = args[i];
                dirty = true;
            }
        }
    }

    @Override
    public int apply(double[] args) {
        if (dirty) {
            inner = inner.withExtrema(maxima, minima);
            dirty = false;
        }
        return inner.apply(args);
    }

    /** The current static bound (infinite until the first update). */
    public double bound() {
        if (dirty) {
            inner = inner.withExtrema(maxima, minima);
            dirty = false;
        }
        return inner.bound();
    }
}
