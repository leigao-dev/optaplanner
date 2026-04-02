package org.optaplanner.core.api.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.impl.testdata.domain.TestdataValue;
import org.optaplanner.core.impl.testdata.domain.list.shadow_history.TestdataListEntityWithShadowHistory;
import org.optaplanner.core.impl.testdata.domain.list.shadow_history.TestdataListSolutionWithShadowHistory;
import org.optaplanner.core.impl.testdata.domain.list.shadow_history.TestdataListValueWithShadowHistory;
import org.optaplanner.core.impl.testdata.domain.multivar.TestdataMultiVarEntity;
import org.optaplanner.core.impl.testdata.domain.multivar.TestdataMultiVarSolution;
import org.optaplanner.core.impl.testdata.domain.multivar.TestdataOtherValue;
import org.optaplanner.core.impl.testdata.domain.shadow.TestdataShadowedEntity;
import org.optaplanner.core.impl.testdata.domain.shadow.TestdataShadowedSolution;

@SuppressWarnings("unchecked")
public class SolutionManagerRecommendAssignmentTest {

    // ************************************************************************
    // Phase A: Basic tests with TestdataShadowedSolution
    // ************************************************************************

    public static final SolverFactory<TestdataShadowedSolution> SOLVER_FACTORY =
            SolverFactory.createFromXmlResource("org/optaplanner/core/api/solver/testdataRecommendAssignmentSolverConfig.xml");

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendAssignment(SolutionManagerSource src) {
        var sm = src.createSolutionManager(SOLVER_FACTORY);
        var solution = TestdataShadowedSolution.generateSolution(3, 3);
        var entity2 = solution.getEntityList().get(2);
        entity2.setValue(null);

        var recs = sm.recommendAssignment(solution, entity2, TestdataShadowedEntity::getValue);

        assertSoftly(softly -> {
            softly.assertThat(recs).hasSize(3);
            softly.assertThat(recs).allSatisfy(r -> softly.assertThat(r.proposition()).isNotNull());
            var s0 = (SimpleScore) recs.get(0).scoreAnalysisDiff().score();
            var s1 = (SimpleScore) recs.get(1).scoreAnalysisDiff().score();
            var s2 = (SimpleScore) recs.get(2).scoreAnalysisDiff().score();
            softly.assertThat(s0.compareTo(s1)).isGreaterThanOrEqualTo(0);
            softly.assertThat(s1.compareTo(s2)).isGreaterThanOrEqualTo(0);
        });
        assertThat(entity2.getValue()).isNull();
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendAssignmentAlreadyAssigned(SolutionManagerSource src) {
        var sm = src.createSolutionManager(SOLVER_FACTORY);
        var solution = TestdataShadowedSolution.generateSolution(3, 3);
        var entity2 = solution.getEntityList().get(2);
        var originalValue = entity2.getValue();

        var recs = sm.recommendAssignment(solution, entity2, TestdataShadowedEntity::getValue);

        assertSoftly(softly -> {
            softly.assertThat(recs).hasSize(3);
            softly.assertThat(recs.get(0).proposition()).isEqualTo(originalValue);
            var s0 = (SimpleScore) recs.get(0).scoreAnalysisDiff().score();
            var s1 = (SimpleScore) recs.get(1).scoreAnalysisDiff().score();
            softly.assertThat(s0.compareTo(s1)).isGreaterThanOrEqualTo(0);
        });
        assertThat(entity2.getValue()).isEqualTo(originalValue);
    }

    @Test
    void recommendAssignmentUninitializedSolution() {
        var sm = SolutionManagerSource.FROM_SOLVER_FACTORY.createSolutionManager(SOLVER_FACTORY);
        var solution = TestdataShadowedSolution.generateSolution(3, 3);
        for (var e : solution.getEntityList()) {
            e.setValue(null);
        }
        var entity2 = solution.getEntityList().get(2);

        var recs = sm.recommendAssignment(solution, entity2, TestdataShadowedEntity::getValue);

        assertThat(recs).hasSize(3);
        assertThat(recs).allSatisfy(r -> {
            assertThat(r.proposition()).isNotNull();
            assertThat(r.scoreAnalysisDiff().score()).isNotNull();
        });
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendAssignmentFetchAllVsFetchMatchCount(SolutionManagerSource src) {
        var sm = src.createSolutionManager(SOLVER_FACTORY);
        var solution = TestdataShadowedSolution.generateSolution(3, 3);
        var entity2 = solution.getEntityList().get(2);
        entity2.setValue(null);

        var fetchAll = sm.recommendAssignment(solution, entity2, TestdataShadowedEntity::getValue,
                ScoreAnalysisFetchPolicy.FETCH_ALL);
        var fetchMatchCount = sm.recommendAssignment(solution, entity2, TestdataShadowedEntity::getValue,
                ScoreAnalysisFetchPolicy.FETCH_MATCH_COUNT);

        assertSoftly(softly -> {
            softly.assertThat(fetchAll).hasSize(fetchMatchCount.size());
            for (int i = 0; i < fetchAll.size(); i++) {
                softly.assertThat(fetchMatchCount.get(i).proposition()).isEqualTo(fetchAll.get(i).proposition());
                softly.assertThat(fetchMatchCount.get(i).scoreAnalysisDiff().score())
                        .isEqualTo(fetchAll.get(i).scoreAnalysisDiff().score());
            }
            softly.assertThat(fetchAll).anySatisfy(r -> {
                var cas = r.scoreAnalysisDiff().constraintMap().values();
                softly.assertThat(cas).anySatisfy(ca -> softly.assertThat(ca.matches()).isNotNull());
            });
            softly.assertThat(fetchMatchCount).allSatisfy(r -> {
                var cas = r.scoreAnalysisDiff().constraintMap().values();
                softly.assertThat(cas).allSatisfy(ca -> softly.assertThat(ca.matches()).isNull());
            });
        });
    }

    // ************************************************************************
    // Phase C: MultiVar tests
    // ************************************************************************

    public static final SolverFactory<TestdataMultiVarSolution> MULTI_VAR_SOLVER_FACTORY =
            SolverFactory.createFromXmlResource("org/optaplanner/core/api/solver/testdataMultiVarSolverConfig.xml");

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendAssignmentMultivar(SolutionManagerSource src) {
        var sm = src.createSolutionManager(MULTI_VAR_SOLVER_FACTORY);
        var solution = buildMultiVarSolution(2, 2, 2);
        var entity = solution.getMultiVarEntityList().get(0);
        entity.setPrimaryValue(null);
        entity.setSecondaryValue(null);
        entity.setTertiaryNullableValue(null);

        var recs = sm.recommendAssignment(solution, entity,
                e -> java.util.Arrays.asList(e.getPrimaryValue(), e.getSecondaryValue(), e.getTertiaryNullableValue()));

        // 2 primary + 2 secondary + 2 other = 6 recommendations (each variable enumerated independently)
        assertThat(recs).hasSize(6);
        assertThat(recs).allSatisfy(r -> assertThat(r.proposition()).isNotNull());
    }

    @ParameterizedTest
    @EnumSource(SolutionManagerSource.class)
    void recommendAssignmentMultivarAlreadyAssigned(SolutionManagerSource src) {
        var sm = src.createSolutionManager(MULTI_VAR_SOLVER_FACTORY);
        var solution = buildMultiVarSolution(2, 2, 2);
        var entity = solution.getMultiVarEntityList().get(0);

        var recs = sm.recommendAssignment(solution, entity,
                e -> java.util.Arrays.asList(e.getPrimaryValue(), e.getSecondaryValue(), e.getTertiaryNullableValue()));

        assertThat(recs).hasSize(6);
        assertThat(recs).allSatisfy(r -> assertThat(r.proposition()).isNotNull());
    }

    @Test
    void recommendAssignmentMultivarUninitializedSolution() {
        var sm = SolutionManagerSource.FROM_SOLVER_FACTORY.createSolutionManager(MULTI_VAR_SOLVER_FACTORY);
        var solution = buildMultiVarSolution(2, 2, 2);
        for (var e : solution.getMultiVarEntityList()) {
            e.setPrimaryValue(null);
            e.setSecondaryValue(null);
            e.setTertiaryNullableValue(null);
        }
        var entity = solution.getMultiVarEntityList().get(0);

        var recs = sm.recommendAssignment(solution, entity,
                e -> java.util.Arrays.asList(e.getPrimaryValue(), e.getSecondaryValue(), e.getTertiaryNullableValue()));

        assertThat(recs).hasSize(6);
        assertThat(recs).allSatisfy(r -> assertThat(r.scoreAnalysisDiff().score()).isNotNull());
    }

    // ************************************************************************
    // Helper methods
    // ************************************************************************

    private static TestdataMultiVarSolution buildMultiVarSolution(int pvCount, int svCount, int ovCount) {
        var solution = new TestdataMultiVarSolution("solution");
        var valueList = new ArrayList<TestdataValue>();
        for (int i = 0; i < pvCount; i++) {
            valueList.add(new TestdataValue("pv" + i));
        }
        solution.setValueList(valueList);
        var otherValueList = new ArrayList<TestdataOtherValue>();
        for (int i = 0; i < ovCount; i++) {
            otherValueList.add(new TestdataOtherValue("ov" + i));
        }
        solution.setOtherValueList(otherValueList);
        var entityList = new ArrayList<TestdataMultiVarEntity>();
        for (int i = 0; i < 2; i++) {
            entityList.add(new TestdataMultiVarEntity("e" + i,
                    valueList.get(i % pvCount),
                    valueList.get((i + 1) % pvCount),
                    otherValueList.get(i % ovCount)));
        }
        solution.setMultiVarEntityList(entityList);
        return solution;
    }

    private static TestdataListSolutionWithShadowHistory buildListSolution() {
        var solution = new TestdataListSolutionWithShadowHistory();
        var valueList = new ArrayList<TestdataListValueWithShadowHistory>();
        for (int i = 0; i < 6; i++) {
            valueList.add(new TestdataListValueWithShadowHistory("v" + i));
        }
        solution.setValueList(valueList);
        var entityList = new ArrayList<TestdataListEntityWithShadowHistory>();
        entityList.add(TestdataListEntityWithShadowHistory.createWithValues("a", valueList.get(1), valueList.get(2)));
        entityList.add(TestdataListEntityWithShadowHistory.createWithValues("b", valueList.get(3), valueList.get(4)));
        entityList.add(TestdataListEntityWithShadowHistory.createWithValues("c"));
        solution.setEntityList(entityList);
        return solution;
    }

    // ************************************************************************
    // SolutionManagerSource enum
    // ************************************************************************

    public enum SolutionManagerSource {

        FROM_SOLVER_FACTORY(SolutionManager::create),
        FROM_SOLVER_MANAGER(solverFactory -> SolutionManager.create(SolverManager.create(solverFactory)));

        private final Function<SolverFactory, SolutionManager> solutionManagerConstructor;

        SolutionManagerSource(Function<SolverFactory, SolutionManager> solutionManagerConstructor) {
            this.solutionManagerConstructor = solutionManagerConstructor;
        }

        public <Solution_, Score_ extends Score<Score_>> SolutionManager<Solution_, Score_>
                createSolutionManager(SolverFactory<Solution_> solverFactory) {
            return solutionManagerConstructor.apply(solverFactory);
        }
    }

}
