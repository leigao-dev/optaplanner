package org.optaplanner.core.impl.testdata.domain.list.shadow_history;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import org.optaplanner.core.api.score.constraint.ConstraintMatch;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.optaplanner.core.impl.score.constraint.DefaultConstraintMatchTotal;
import org.optaplanner.core.impl.score.constraint.DefaultIndictment;

/**
 * Incremental score calculator for {@link TestdataListSolutionWithShadowHistory}.
 * Scoring: penalize by entity valueList.size() squared (encourages balanced distribution).
 * Fully recalculates on each variable change.
 */
public class TestdataListWithShadowHistoryIncrementalScoreCalculator
        implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataListSolutionWithShadowHistory, SimpleScore> {

    private static final String CONSTRAINT_PACKAGE = "org.optaplanner.core.impl.testdata.domain.list.shadow_history";
    private static final String CONSTRAINT_NAME = "testConstraint";

    private List<TestdataListEntityWithShadowHistory> entityList;
    private int score;
    private DefaultConstraintMatchTotal<SimpleScore> constraintMatchTotal;
    private Map<Object, Indictment<SimpleScore>> indictmentMap;

    @Override
    public void resetWorkingSolution(TestdataListSolutionWithShadowHistory workingSolution) {
        this.entityList = workingSolution.getEntityList();
        recalculate();
    }

    @Override
    public void resetWorkingSolution(TestdataListSolutionWithShadowHistory workingSolution,
            boolean constraintMatchEnabled) {
        resetWorkingSolution(workingSolution);
    }

    private void recalculate() {
        score = 0;
        constraintMatchTotal =
                new DefaultConstraintMatchTotal<>(CONSTRAINT_PACKAGE, CONSTRAINT_NAME, SimpleScore.ONE);
        indictmentMap = new HashMap<>();
        for (var entity : entityList) {
            int size = entity.getValueList().size();
            score -= size * size;
            if (size > 0) {
                ConstraintMatch<SimpleScore> constraintMatch =
                        constraintMatchTotal.addConstraintMatch(List.of(entity), SimpleScore.of(size * size));
                indictmentMap
                        .computeIfAbsent(entity, key -> new DefaultIndictment<>(key, SimpleScore.ZERO))
                        .getConstraintMatchSet()
                        .add(constraintMatch);
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
