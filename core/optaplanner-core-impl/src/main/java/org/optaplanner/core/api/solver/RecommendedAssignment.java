package org.optaplanner.core.api.solver;

import org.optaplanner.core.api.score.analysis.ScoreAnalysis;

/**
 * Represents a recommended assignment for a planning entity or list variable element,
 * showing the impact on the score if this assignment were to be made.
 * <p>
 * Use {@link SolutionManager#recommendAssignment(Object, Object, java.util.function.Function)}
 * to obtain instances.
 *
 * @param <Proposition_> the type of the proposition extracted from the assignment
 * @param <Score_> the actual score type
 */
public interface RecommendedAssignment<Proposition_, Score_ extends org.optaplanner.core.api.score.Score<Score_>> {

    /**
     * Returns the proposition extracted from this recommended assignment by the user-provided function.
     *
     * @return possibly null if the proposition function returned null
     */
    Proposition_ proposition();

    /**
     * Returns the difference in score analysis between the solution with this assignment applied
     * and the original solution.
     *
     * @return never null
     */
    ScoreAnalysis<Score_> scoreAnalysisDiff();

}
