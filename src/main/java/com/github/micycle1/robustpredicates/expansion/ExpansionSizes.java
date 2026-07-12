package com.github.micycle1.robustpredicates.expansion;

import com.github.micycle1.robustpredicates.expr.Expression;
import com.github.micycle1.robustpredicates.expr.Op;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Worst-case expansion component counts per expression node
 * (port of {@code expansion_size_impl} in {@code expansion_eval.hpp}).
 *
 * <p>Leaf: 1. Sum: {@code l + r}. Difference: {@code l + r}, except a
 * leaf-minus-leaf difference in stage B mode is assumed exact and has size 1.
 * Product: {@code 2*l*r}, except the square of a length-2 operand which needs
 * only 6 components.
 */
public final class ExpansionSizes {

    private final Map<Expression, Integer> cache = new IdentityHashMap<>();
    private final boolean stageB;

    public ExpansionSizes(boolean stageB) {
        this.stageB = stageB;
    }

    public int sizeOf(Expression e) {
        Integer cached = cache.get(e);
        if (cached != null) {
            return cached;
        }
        int size = compute(e);
        cache.put(e, size);
        return size;
    }

    private int compute(Expression e) {
        if (e.isLeaf()) {
            return 1;
        }
        return switch (e.op()) {
            case SUM -> sizeOf(e.left()) + sizeOf(e.right());
            case DIFFERENCE -> stageB && e.left().isLeaf() && e.right().isLeaf()
                    ? 1
                    : sizeOf(e.left()) + sizeOf(e.right());
            case PRODUCT -> {
                int l = sizeOf(e.left());
                int r = sizeOf(e.right());
                yield (e.left() == e.right() && l == 2 && r == 2) ? 6 : 2 * l * r;
            }
            default -> throw new IllegalArgumentException(
                    "unsupported operator in exact expression: " + e.op());
        };
    }
}
