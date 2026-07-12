package com.github.micycle1.robustpredicates.filter;

import java.util.List;

/**
 * Chains filter stages from cheap to exact (port of
 * {@code staged_predicate.hpp}): stages are applied in order until one returns
 * a definite sign; {@link Sign#UNCERTAIN} advances to the next stage. If the
 * final stage is exact (stage D) the chain always returns a definite sign.
 */
public final class StagedPredicate implements Stage {

    private final List<Stage> stages;

    public StagedPredicate(Stage... stages) {
        this.stages = List.of(stages);
    }

    @Override
    public int apply(double[] args) {
        for (Stage stage : stages) {
            int result = stage.apply(args);
            if (result != Sign.UNCERTAIN) {
                return result;
            }
        }
        return Sign.UNCERTAIN;
    }

    @Override
    public boolean stateful() {
        return stages.stream().anyMatch(Stage::stateful);
    }

    @Override
    public void update(double[] args) {
        for (Stage stage : stages) {
            if (stage.stateful()) {
                stage.update(args);
            }
        }
    }

    /** Sign result of the given stage only (for per-stage testing/profiling). */
    public int applyStage(int index, double[] args) {
        return stages.get(index).apply(args);
    }

    public int stageCount() {
        return stages.size();
    }
}
