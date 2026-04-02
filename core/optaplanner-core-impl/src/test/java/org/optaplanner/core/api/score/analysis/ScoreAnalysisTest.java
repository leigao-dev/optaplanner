package org.optaplanner.core.api.score.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.constraint.ConstraintRef;

class ScoreAnalysisTest {

    @Test
    void constraintRefHasPackageAndName() {
        ConstraintRef constraintRef = ConstraintRef.of("pkg", "name");

        assertThat(constraintRef.constraintPackage()).isEqualTo("pkg");
        assertThat(constraintRef.constraintName()).isEqualTo("name");
        assertThat(constraintRef.constraintId()).isEqualTo("pkg/name");
    }

    @Test
    void constraintAnalysisAllowsNullWeight() {
        ConstraintAnalysis<SimpleScore> analysis =
                new ConstraintAnalysis<>(ConstraintRef.of("pkg", "name"), null, SimpleScore.ZERO, null, -1);

        assertThat(analysis.weight()).isNull();
        assertThat(analysis.summarize()).contains("weight=null");
    }

    @Test
    void constraintAnalysisDiffHandlesSingleSide() {
        ConstraintRef constraintRef = ConstraintRef.of("pkg", "name");
        ConstraintAnalysis<SimpleScore> left =
                new ConstraintAnalysis<>(constraintRef, SimpleScore.ONE, SimpleScore.ONE, List.of(), 1);

        ConstraintAnalysis<SimpleScore> onlyLeft = ConstraintAnalysis.diff(constraintRef, left, null);
        ConstraintAnalysis<SimpleScore> onlyRight = ConstraintAnalysis.diff(constraintRef, null, left);

        assertThat(onlyLeft.weight()).isEqualTo(SimpleScore.ONE);
        assertThat(onlyRight.weight()).isEqualTo(SimpleScore.of(-1));
    }

    @Test
    void scoreAnalysisDiffFiltersZeroDifferences() {
        ConstraintRef constraintRef = ConstraintRef.of("pkg", "name");
        ConstraintAnalysis<SimpleScore> leftConstraint =
                new ConstraintAnalysis<>(constraintRef, SimpleScore.ONE, SimpleScore.ONE, null, -1);
        ConstraintAnalysis<SimpleScore> rightConstraint =
                new ConstraintAnalysis<>(constraintRef, SimpleScore.ONE, SimpleScore.ONE, null, -1);
        ScoreAnalysis<SimpleScore> left = new ScoreAnalysis<>(SimpleScore.ONE, Map.of(constraintRef, leftConstraint), true);
        ScoreAnalysis<SimpleScore> right = new ScoreAnalysis<>(SimpleScore.ONE, Map.of(constraintRef, rightConstraint), true);

        assertThat(left.diff(right).constraintMap()).isEmpty();
    }
}
