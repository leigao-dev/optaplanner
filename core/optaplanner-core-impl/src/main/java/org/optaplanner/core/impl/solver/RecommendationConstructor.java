package org.optaplanner.core.impl.solver;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.analysis.ScoreAnalysis;

/**
 * Internal factory interface for creating recommendation instances.
 *
 * @param <Score_> the actual score type
 * @param <Recommendation_> the recommendation type
 * @param <Out_> the proposition type
 */
interface RecommendationConstructor<Score_ extends Score<Score_>, Recommendation_, Out_> {

    /**
     * Creates a new recommendation instance.
     *
     * @param moveIndex the index of the move (for stable sorting)
     * @param result possibly null proposition
     * @param scoreDifference never null
     * @return never null
     */
    Recommendation_ apply(long moveIndex, Out_ result, ScoreAnalysis<Score_> scoreDifference);

}
