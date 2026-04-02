package org.optaplanner.core.impl.testdata.domain.multivar;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import org.optaplanner.core.api.score.constraint.ConstraintMatch;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.optaplanner.core.impl.score.constraint.DefaultConstraintMatchTotal;
import org.optaplanner.core.impl.score.constraint.DefaultIndictment;

/**
 * Incremental score calculator for {@link TestdataMultiVarSolution}.
 * Scoring: for each pair of entities sharing the same primaryValue, secondaryValue,
 * or tertiaryNullableValue, score -= 1 (including self-pairs).
 * Fully recalculates on each variable change.
 */
public class TestdataMultiVarIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataMultiVarSolution, SimpleScore> {

    private static final String CONSTRAINT_PACKAGE = "org.optaplanner.core.impl.testdata.domain.multivar";
    private static final String CONSTRAINT_NAME = "testConstraint";

    private List<TestdataMultiVarEntity> entityList;
    private int score;
    private DefaultConstraintMatchTotal<SimpleScore> constraintMatchTotal;
    private Map<Object, Indictment<SimpleScore>> indictmentMap;

    @Override
    public void resetWorkingSolution(TestdataMultiVarSolution workingSolution) {
        this.entityList = workingSolution.getMultiVarEntityList();
        recalculate();
    }

    @Override
    public void resetWorkingSolution(TestdataMultiVarSolution workingSolution, boolean constraintMatchEnabled) {
        resetWorkingSolution(workingSolution);
    }

    private void recalculate() {
        score = 0;
        constraintMatchTotal =
                new DefaultConstraintMatchTotal<>(CONSTRAINT_PACKAGE, CONSTRAINT_NAME, SimpleScore.ONE);
        indictmentMap = new HashMap<>();
        for (var left : entityList) {
            for (var right : entityList) {
                boolean shared = false;
                if (left.getPrimaryValue() != null && Objects.equals(left.getPrimaryValue(), right.getPrimaryValue())) {
                    shared = true;
                }
                if (left.getSecondaryValue() != null && Objects.equals(left.getSecondaryValue(), right.getSecondaryValue())) {
                    shared = true;
                }
                if (left.getTertiaryNullableValue() != null
                        && Objects.equals(left.getTertiaryNullableValue(), right.getTertiaryNullableValue())) {
                    shared = true;
                }
                if (shared) {
                    score -= 1;
                    ConstraintMatch<SimpleScore> constraintMatch =
                            constraintMatchTotal.addConstraintMatch(List.of(left, right), SimpleScore.ONE);
                    Stream.of(left, right)
                            .forEach(entity -> indictmentMap
                                    .computeIfAbsent(entity, key -> new DefaultIndictment<>(key, SimpleScore.ZERO))
                                    .getConstraintMatchSet()
                                    .add(constraintMatch));
                }
            }
        }
    }

    @Override
    public void beforeEntityAdded(Object entity) {
    }

    @Override
    public void afterEntityAdded(Object entity) {
    }

    @Override
    public void beforeVariableChanged(Object entity, String variableName) {
    }

    @Override
    public void afterVariableChanged(Object entity, String variableName) {
        recalculate();
    }

    @Override
    public void beforeEntityRemoved(Object entity) {
    }

    @Override
    public void afterEntityRemoved(Object entity) {
    }

    @Override
    public SimpleScore calculateScore() {
        return SimpleScore.of(score);
    }

    @Override
    public Collection<ConstraintMatchTotal<SimpleScore>> getConstraintMatchTotals() {
        return Collections.singleton(constraintMatchTotal);
    }

    @Override
    public Map<Object, Indictment<SimpleScore>> getIndictmentMap() {
        return indictmentMap;
    }
}
