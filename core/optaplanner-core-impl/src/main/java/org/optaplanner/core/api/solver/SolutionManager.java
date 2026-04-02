package org.optaplanner.core.api.solver;

import static org.optaplanner.core.api.solver.SolutionUpdatePolicy.UPDATE_ALL;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.analysis.ScoreAnalysis;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.optaplanner.core.impl.domain.variable.ShadowVariableUpdateHelper;
import org.optaplanner.core.impl.solver.DefaultSolutionManager;

/**
 * A stateless service to help calculate {@link Score}, {@link ConstraintMatchTotal},
 * {@link Indictment}, etc.
 * <p>
 * To create a {@link SolutionManager} instance, use {@link #create(SolverFactory)}.
 * <p>
 * These methods are thread-safe unless explicitly stated otherwise.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the actual score type
 */
public interface SolutionManager<Solution_, Score_ extends Score<Score_>> {

    // ************************************************************************
    // Static creation methods: SolverFactory
    // ************************************************************************

    /**
     * Uses a {@link SolverFactory} to build a {@link SolutionManager}.
     *
     * @param solverFactory never null
     * @return never null
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <Score_> the actual score type
     */
    static <Solution_, Score_ extends Score<Score_>> SolutionManager<Solution_, Score_> create(
            SolverFactory<Solution_> solverFactory) {
        return new DefaultSolutionManager<>(solverFactory);
    }

    /**
     * Uses a {@link SolverManager} to build a {@link SolutionManager}.
     *
     * @param solverManager never null
     * @return never null
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <Score_> the actual score type
     * @param <ProblemId_> the ID type of a submitted problem, such as {@link Long} or {@link UUID}
     */
    static <Solution_, Score_ extends Score<Score_>, ProblemId_> SolutionManager<Solution_, Score_> create(
            SolverManager<Solution_, ProblemId_> solverManager) {
        return new DefaultSolutionManager<>(solverManager);
    }

    // ************************************************************************
    // Interface methods
    // ************************************************************************

    /**
     * As defined by {@link #update(Object, SolutionUpdatePolicy)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL}.
     *
     */
    default Score_ update(Solution_ solution) {
        return update(solution, UPDATE_ALL);
    }

    /**
     * Updates the given solution according to the {@link SolutionUpdatePolicy}.
     *
     * @param solution never null
     * @param solutionUpdatePolicy never null; if unsure, pick {@link SolutionUpdatePolicy#UPDATE_ALL}
     * @return possibly null if already null and {@link SolutionUpdatePolicy} didn't cause its update
     * @see SolutionUpdatePolicy Description of individual policies with respect to performance trade-offs.
     */
    Score_ update(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy);

    /**
     * As defined by {@link #explain(Object)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL}.
     */
    default ScoreExplanation<Solution_, Score_> explain(Solution_ solution) {
        return explain(solution, UPDATE_ALL);
    }

    /**
     * Calculates and retrieves {@link ConstraintMatchTotal}s and {@link Indictment}s necessary for describing the
     * quality of a particular solution.
     *
     * @param solution never null
     * @param solutionUpdatePolicy never null; if unsure, pick {@link SolutionUpdatePolicy#UPDATE_ALL}
     * @return never null
     * @throws IllegalStateException when constraint matching is disabled or not supported by the underlying score
     *         calculator, such as {@link EasyScoreCalculator}.
     * @see SolutionUpdatePolicy Description of individual policies with respect to performance trade-offs.
     */
    ScoreExplanation<Solution_, Score_> explain(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy);

    /**
     * As defined by {@link #analyze(Object, ScoreAnalysisFetchPolicy, SolutionUpdatePolicy)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL} and {@link ScoreAnalysisFetchPolicy#FETCH_ALL}.
     */
    default ScoreAnalysis<Score_> analyze(Solution_ solution) {
        return analyze(solution, ScoreAnalysisFetchPolicy.FETCH_ALL, UPDATE_ALL);
    }

    /**
     * As defined by {@link #analyze(Object, ScoreAnalysisFetchPolicy, SolutionUpdatePolicy)},
     * using {@link SolutionUpdatePolicy#UPDATE_ALL}.
     */
    default ScoreAnalysis<Score_> analyze(Solution_ solution, ScoreAnalysisFetchPolicy scoreAnalysisFetchPolicy) {
        return analyze(solution, scoreAnalysisFetchPolicy, UPDATE_ALL);
    }

    /**
     * Calculates and retrieves information about which constraints contributed to the solution's score.
     * For a simplified, faster and JSON-friendly alternative, see {@link #analyze(Object)}.
     *
     * @param solution must be fully initialized otherwise an exception is thrown
     * @param scoreAnalysisFetchPolicy if unsure, pick {@link ScoreAnalysisFetchPolicy#FETCH_MATCH_COUNT}
     * @param solutionUpdatePolicy if unsure, pick {@link SolutionUpdatePolicy#UPDATE_ALL}
     * @throws IllegalStateException when constraint matching is disabled or not supported by the underlying score
     *         calculator, such as {@link EasyScoreCalculator}.
     * @see SolutionUpdatePolicy Description of individual policies with respect to performance trade-offs.
     */
    ScoreAnalysis<Score_> analyze(Solution_ solution, ScoreAnalysisFetchPolicy scoreAnalysisFetchPolicy,
            SolutionUpdatePolicy solutionUpdatePolicy);

    /**
     * As defined by {@link #recommendAssignment(Object, Object, Function, ScoreAnalysisFetchPolicy)},
     * using {@link ScoreAnalysisFetchPolicy#FETCH_ALL}.
     *
     * @param solution never null
     * @param evaluatedEntityOrElement never null, the planning entity or list variable element to evaluate
     * @param propositionFunction never null, extracts a proposition from each possible assignment
     * @param <EntityOrElement_> the type of the entity or element being evaluated
     * @param <Proposition_> the type of the proposition extracted by the function
     * @return never null, sorted by score descending (best first)
     */
    default <EntityOrElement_, Proposition_> List<RecommendedAssignment<Proposition_, Score_>> recommendAssignment(
            Solution_ solution,
            EntityOrElement_ evaluatedEntityOrElement,
            Function<EntityOrElement_, Proposition_> propositionFunction) {
        return recommendAssignment(solution, evaluatedEntityOrElement, propositionFunction,
                ScoreAnalysisFetchPolicy.FETCH_ALL);
    }

    /**
     * Recommends all possible assignments for a given planning entity or list variable element,
     * showing the score impact of each assignment.
     * <p>
     * For basic planning variables, the entity is the planning entity being evaluated.
     * For list variables, the element is the planning value in the list being evaluated.
     * <p>
     * The returned list is sorted by score descending (best first).
     *
     * @param solution never null
     * @param evaluatedEntityOrElement never null, the planning entity or list variable element to evaluate
     * @param propositionFunction never null, extracts a proposition from each possible assignment;
     *        the function receives the entity/element and returns a user-defined proposition object
     * @param scoreAnalysisFetchPolicy if unsure, pick {@link ScoreAnalysisFetchPolicy#FETCH_MATCH_COUNT}
     * @param <EntityOrElement_> the type of the entity or element being evaluated
     * @param <Proposition_> the type of the proposition extracted by the function
     * @return never null, sorted by score descending (best first)
     */
    <EntityOrElement_, Proposition_> List<RecommendedAssignment<Proposition_, Score_>> recommendAssignment(
            Solution_ solution,
            EntityOrElement_ evaluatedEntityOrElement,
            Function<EntityOrElement_, Proposition_> propositionFunction,
            ScoreAnalysisFetchPolicy scoreAnalysisFetchPolicy);

    // ************************************************************************
    // Static utility methods
    // ************************************************************************

    /**
     * Updates all shadow variables on the given entities, without requiring a full solver configuration.
     * <p>
     * Unlike {@link #update(Object)}, this method does not require creating a {@link SolverFactory}
     * or {@link SolutionManager} instance. It is useful for tests or standalone processing
     * where shadow variables need to be updated on individual entities.
     * <p>
     * Note: This method does not support shadow variables that rely on custom
     * {@link org.optaplanner.core.api.domain.variable.VariableListener}s,
     * as those would require a complete solution context.
     *
     * @param solutionClass the solution class with the {@link PlanningSolution} annotation
     * @param entities all entities to be updated
     * @param <Solution_> the solution type
     */
    static <Solution_> void updateShadowVariables(Class<Solution_> solutionClass, Object... entities) {
        if (Objects.requireNonNull(entities).length == 0) {
            throw new IllegalArgumentException("The entity array cannot be empty.");
        }
        ShadowVariableUpdateHelper.<Solution_> create().updateShadowVariables(solutionClass, entities);
    }

    /**
     * Updates all shadow variables on the given solution, without requiring a full solver configuration.
     * <p>
     * Unlike {@link #update(Object)}, this method does not require creating a {@link SolverFactory}
     * or {@link SolutionManager} instance. It is useful for tests or standalone processing.
     *
     * @param solution the solution whose shadow variables should be updated
     * @param <Solution_> the solution type
     */
    static <Solution_> void updateShadowVariables(Solution_ solution) {
        ShadowVariableUpdateHelper.<Solution_> create().updateShadowVariables(solution);
    }

}
