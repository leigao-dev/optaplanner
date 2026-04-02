package org.optaplanner.core.impl.testdata.domain.shadow;

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
import org.optaplanner.core.impl.testdata.domain.TestdataValue;

/**
 * A properly incremental score calculator that updates on every variable change.
 * Scoring: for each pair (left, right) of entities sharing the same value, score -= 1.
 * This includes self-pairs (entity with itself).
 */
public class TestdataShadowedIncrementalScoreCalculator2
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataShadowedSolution, SimpleScore> {

    private static final String CONSTRAINT_PACKAGE = "org.optaplanner.core.impl.testdata.domain.shadow";
    private static final String CONSTRAINT_NAME = "testConstraint";

    private List<TestdataShadowedEntity> entityList;
    private int score;
    private DefaultConstraintMatchTotal<SimpleScore> constraintMatchTotal;
    private Map<Object, Indictment<SimpleScore>> indictmentMap;

    @Override
    public void resetWorkingSolution(TestdataShadowedSolution workingSolution) {
        this.entityList = workingSolution.getEntityList();
        recalculate();
    }

    @Override
    public void resetWorkingSolution(TestdataShadowedSolution workingSolution, boolean constraintMatchEnabled) {
        resetWorkingSolution(workingSolution);
    }

    private void recalculate() {
        score = 0;
        constraintMatchTotal =
                new DefaultConstraintMatchTotal<>(CONSTRAINT_PACKAGE, CONSTRAINT_NAME, SimpleScore.ONE);
        indictmentMap = new HashMap<>();
        for (TestdataShadowedEntity left : entityList) {
            TestdataValue leftValue = left.getValue();
            if (leftValue == null) {
                continue;
            }
            for (TestdataShadowedEntity right : entityList) {
                if (Objects.equals(right.getValue(), leftValue)) {
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
        // Fully recalculate after any variable change to support recommendAssignment moves
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
