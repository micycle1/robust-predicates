package com.github.micycle1.robustpredicates.eval;

import com.github.micycle1.robustpredicates.expr.EvaluationPlan;
import com.github.micycle1.robustpredicates.expr.Expression;

import java.util.List;

/**
 * Plain floating-point evaluation of an {@link EvaluationPlan}
 * (Java analogue of {@code expression_eval.hpp}).
 */
public final class ApproximateEvaluator {

    private ApproximateEvaluator() {
    }

    /**
     * Evaluates every node of the plan into {@code out} (one slot per node).
     *
     * @param args predicate inputs; {@code arg(i)} reads {@code args[i - 1]}
     * @param out  scratch array of length {@code plan.slotCount()}
     */
    public static void evaluate(EvaluationPlan plan, double[] args, double[] out) {
        List<Expression> nodes = plan.nodes();
        for (int i = 0; i < nodes.size(); i++) {
            Expression node = nodes.get(i);
            out[i] = switch (node.op()) {
                case ARGUMENT -> args[node.argN() - 1];
                case CONSTANT -> node.value();
                case ABS -> Math.abs(out[plan.slotOf(node.left())]);
                case SUM -> out[plan.slotOf(node.left())] + out[plan.slotOf(node.right())];
                case DIFFERENCE -> out[plan.slotOf(node.left())] - out[plan.slotOf(node.right())];
                case PRODUCT -> out[plan.slotOf(node.left())] * out[plan.slotOf(node.right())];
                case MAX -> Math.max(out[plan.slotOf(node.left())], out[plan.slotOf(node.right())]);
                case MIN -> Math.min(out[plan.slotOf(node.left())], out[plan.slotOf(node.right())]);
            };
        }
    }

    /** Convenience: evaluates the plan and returns the value of its i-th root. */
    public static double evaluateRoot(EvaluationPlan plan, int rootIndex, double[] args) {
        double[] out = new double[plan.slotCount()];
        evaluate(plan, args, out);
        return out[plan.rootSlot(rootIndex)];
    }
}
