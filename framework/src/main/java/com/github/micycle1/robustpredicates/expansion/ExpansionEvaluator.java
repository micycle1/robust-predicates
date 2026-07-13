package com.github.micycle1.robustpredicates.expansion;

import com.github.micycle1.robustpredicates.expr.EvaluationPlan;
import com.github.micycle1.robustpredicates.expr.Expression;
import com.github.micycle1.robustpredicates.expr.Op;
import com.github.micycle1.robustpredicates.filter.Sign;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.github.micycle1.robustpredicates.ExpansionArithmetic.expansionSum;
import static com.github.micycle1.robustpredicates.ExpansionArithmetic.expansionTimes;
import static com.github.micycle1.robustpredicates.ExpansionArithmetic.fastExpansionSum;
import static com.github.micycle1.robustpredicates.ExpansionArithmetic.growExpansion;
import static com.github.micycle1.robustpredicates.ExpansionArithmetic.scaleExpansion;
import static com.github.micycle1.robustpredicates.ExpansionArithmetic.signOf;
import static com.github.micycle1.robustpredicates.ExpansionArithmetic.twoDiff;
import static com.github.micycle1.robustpredicates.ExpansionArithmetic.twoDiffTail;
import static com.github.micycle1.robustpredicates.ExpansionArithmetic.twoProduct;
import static com.github.micycle1.robustpredicates.ExpansionArithmetic.twoSquare;
import static com.github.micycle1.robustpredicates.ExpansionArithmetic.twoSum;
import static com.github.micycle1.robustpredicates.ExpansionArithmetic.zeDefault;

/**
 * Exact sign evaluation of an expression via expansion arithmetic — the
 * evaluation core shared by {@link StageB} and {@link StageD}.
 *
 * <p>All per-expression planning (deduplicated post-order, per-node sizes and
 * buffer offsets, zero-elimination and fast-sum policy decisions) happens once
 * at construction; {@link #evaluateSign} allocates one scratch buffer per call
 * and is therefore thread-safe.
 *
 * <p>In stage B mode every leaf-minus-leaf difference is assumed to be exact
 * (expansion size 1); before the main evaluation the rounding tail of each such
 * difference is checked and {@link Sign#UNCERTAIN} is returned if any is
 * nonzero. Stage D mode never returns {@link Sign#UNCERTAIN}.
 */
public final class ExpansionEvaluator {

    private final boolean stageB;
    private final List<Expression> evals;      // internal nodes, post order
    private final Map<Expression, Integer> evalIndex = new IdentityHashMap<>();
    private final int[] sizes;
    private final int[] starts;
    private final boolean[] ze;
    private final boolean[] isLeafDiff;
    private final int totalSize;
    private final int rootIndex;

    public ExpansionEvaluator(Expression root, boolean stageB) {
        this.stageB = stageB;
        EvaluationPlan plan = EvaluationPlan.of(root);
        List<Expression> internal = new ArrayList<>();
        for (Expression node : plan.nodes()) {
            if (!node.isLeaf()) {
                internal.add(node);
            }
        }
        this.evals = internal;
        ExpansionSizes sizer = new ExpansionSizes(stageB);
        this.sizes = new int[internal.size()];
        this.starts = new int[internal.size()];
        this.ze = new boolean[internal.size()];
        this.isLeafDiff = new boolean[internal.size()];
        int offset = 0;
        for (int i = 0; i < internal.size(); i++) {
            Expression node = internal.get(i);
            evalIndex.put(node, i);
            sizes[i] = sizer.sizeOf(node);
            starts[i] = offset;
            offset += sizes[i];
            ze[i] = zeDefault(sizes[i]);
            isLeafDiff[i] = node.op() == Op.DIFFERENCE
                    && node.left().isLeaf() && node.right().isLeaf();
        }
        this.totalSize = offset;
        this.rootIndex = evalIndex.get(root);
        if (evalIndex.get(root) == null) {
            throw new IllegalArgumentException("root must not be a leaf");
        }
    }

    /** Total scratch buffer size in doubles (for diagnostics). */
    public int scratchSize() {
        return totalSize;
    }

    /**
     * Immutable view of the evaluation plan (internal nodes in dependency
     * order with their sizes, buffer offsets and policy flags) — used by the
     * code generator to emit exactly the calls this evaluator would make.
     */
    public record Plan(List<Expression> evals, int[] sizes, int[] starts, boolean[] ze,
                       boolean[] leafDiff, int totalSize, int rootIndex) {
    }

    public Plan plan() {
        return new Plan(List.copyOf(evals), sizes.clone(), starts.clone(), ze.clone(),
                isLeafDiff.clone(), totalSize, rootIndex);
    }

    /**
     * Exact sign of the expression, or {@link Sign#UNCERTAIN} in stage B mode
     * when some leaf difference has a nonzero rounding tail.
     */
    public int evaluateSign(double[] args) {
        double[] buf = new double[totalSize];
        int[] ends = new int[evals.size()];

        if (stageB) {
            for (int i = 0; i < evals.size(); i++) {
                if (!isLeafDiff[i]) {
                    continue;
                }
                Expression node = evals.get(i);
                double l = leafValue(node.left(), args);
                double r = leafValue(node.right(), args);
                double x = l - r;
                if (twoDiffTail(l, r, x) != 0.0) {
                    return Sign.UNCERTAIN;
                }
                buf[starts[i]] = x;
                ends[i] = starts[i] + 1;
            }
        }

        for (int i = 0; i < evals.size(); i++) {
            if (stageB && isLeafDiff[i]) {
                continue;
            }
            Expression node = evals.get(i);
            Expression left = node.left();
            Expression right = node.right();
            boolean leftLeaf = left.isLeaf();
            boolean rightLeaf = right.isLeaf();
            int start = starts[i];
            boolean zeI = ze[i];
            switch (node.op()) {
                case SUM, DIFFERENCE -> {
                    boolean minus = node.op() == Op.DIFFERENCE;
                    if (leftLeaf && rightLeaf) {
                        double l = leafValue(left, args);
                        double r = leafValue(right, args);
                        ends[i] = minus ? twoDiff(l, r, buf, start, zeI)
                                : twoSum(l, r, buf, start, zeI);
                    } else if (leftLeaf) {
                        // scalar +/- expansion: negate the expansion for minus
                        int ri = evalIndex.get(right);
                        ends[i] = growExpansion(buf, starts[ri], ends[ri],
                                leafValue(left, args), buf, start, minus, false, zeI);
                    } else if (rightLeaf) {
                        // expansion +/- scalar: negate the scalar for minus
                        int li = evalIndex.get(left);
                        ends[i] = growExpansion(buf, starts[li], ends[li],
                                leafValue(right, args), buf, start, false, minus, zeI);
                    } else {
                        int li = evalIndex.get(left);
                        int ri = evalIndex.get(right);
                        // Fast-expansion-sum policy decided on static sizes.
                        boolean fast = sizes[li] > 2 && sizes[ri] > 2;
                        ends[i] = fast
                                ? fastExpansionSum(buf, starts[li], ends[li],
                                        buf, starts[ri], ends[ri], buf, start,
                                        false, minus, zeI)
                                : expansionSum(buf, starts[li], ends[li],
                                        buf, starts[ri], ends[ri], buf, start,
                                        false, minus, zeI);
                    }
                }
                case PRODUCT -> {
                    if (leftLeaf && rightLeaf) {
                        ends[i] = twoProduct(leafValue(left, args), leafValue(right, args),
                                buf, start, zeI);
                    } else if (leftLeaf) {
                        int ri = evalIndex.get(right);
                        ends[i] = scaleExpansion(buf, starts[ri], ends[ri],
                                leafValue(left, args), buf, start, zeI);
                    } else if (rightLeaf) {
                        int li = evalIndex.get(left);
                        ends[i] = scaleExpansion(buf, starts[li], ends[li],
                                leafValue(right, args), buf, start, zeI);
                    } else if (left == right && sizes[evalIndex.get(left)] == 2) {
                        int li = evalIndex.get(left);
                        ends[i] = twoSquare(buf, starts[li], ends[li], buf, start, zeI);
                    } else {
                        int li = evalIndex.get(left);
                        int ri = evalIndex.get(right);
                        ends[i] = expansionTimes(buf, starts[li], ends[li],
                                buf, starts[ri], ends[ri], buf, start, zeI);
                    }
                }
                default -> throw new IllegalStateException(
                        "unsupported operator in exact expression: " + node.op());
            }
        }
        return signOf(buf, starts[rootIndex], ends[rootIndex]);
    }

    private static double leafValue(Expression leaf, double[] args) {
        return leaf.op() == Op.ARGUMENT ? args[leaf.argN() - 1] : leaf.value();
    }
}
