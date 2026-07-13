package com.github.micycle1.robustpredicates.expr;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deduplicated post-order evaluation plan for one or more expression roots:
 * every distinct node (leaves included) gets exactly one slot, children
 * always precede parents, and identical subexpressions shared between roots
 * (e.g. a determinant and its error-bound expression) occupy a single slot.
 * Because {@link Expression} nodes are interned, deduplication is by reference
 * identity.
 */
public final class EvaluationPlan {

    private final List<Expression> nodes;
    private final Map<Expression, Integer> slots;
    private final int[] rootSlots;

    private EvaluationPlan(List<Expression> nodes, Map<Expression, Integer> slots,
                           int[] rootSlots) {
        this.nodes = nodes;
        this.slots = slots;
        this.rootSlots = rootSlots;
    }

    public static EvaluationPlan of(Expression... roots) {
        List<Expression> nodes = new ArrayList<>();
        Map<Expression, Integer> slots = new IdentityHashMap<>();
        for (Expression root : roots) {
            addPostOrder(root, nodes, slots);
        }
        int[] rootSlots = new int[roots.length];
        for (int i = 0; i < roots.length; i++) {
            rootSlots[i] = slots.get(roots[i]);
        }
        return new EvaluationPlan(nodes, slots, rootSlots);
    }

    /** Iterative post-order walk assigning slots on first completion of a node. */
    private static void addPostOrder(Expression root, List<Expression> nodes,
                                     Map<Expression, Integer> slots) {
        record Frame(Expression node, boolean expanded) {
        }
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(root, false));
        while (!stack.isEmpty()) {
            Frame frame = stack.pop();
            Expression node = frame.node();
            if (slots.containsKey(node)) {
                continue;
            }
            if (frame.expanded() || node.isLeaf()) {
                if (!slots.containsKey(node)) {
                    slots.put(node, nodes.size());
                    nodes.add(node);
                }
                continue;
            }
            stack.push(new Frame(node, true));
            if (node.right() != null) {
                stack.push(new Frame(node.right(), false));
            }
            if (node.left() != null) {
                stack.push(new Frame(node.left(), false));
            }
        }
    }

    /** All nodes in dependency (post) order, leaves included. */
    public List<Expression> nodes() {
        return nodes;
    }

    public int slotOf(Expression e) {
        Integer slot = slots.get(e);
        if (slot == null) {
            throw new IllegalArgumentException("expression not in plan: " + e);
        }
        return slot;
    }

    public int slotCount() {
        return nodes.size();
    }

    /** Slot of the i-th root passed to {@link #of}. */
    public int rootSlot(int rootIndex) {
        return rootSlots[rootIndex];
    }
}
