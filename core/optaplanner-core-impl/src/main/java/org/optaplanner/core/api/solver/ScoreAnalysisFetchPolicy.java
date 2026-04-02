package org.optaplanner.core.api.solver;

/**
 * Controls the depth of score analysis returned by
 * {@link SolutionManager#analyze(Object, ScoreAnalysisFetchPolicy)}.
 *
 * @since 1.0
 */
public enum ScoreAnalysisFetchPolicy {

    /**
     * Full analysis with all match details (justifications, scores).
     */
    FETCH_ALL,

    /**
     * No match analysis, no match count — constraint weight and score only.
     */
    FETCH_SHALLOW,

    /**
     * No match analysis but includes match count per constraint.
     */
    FETCH_MATCH_COUNT

}
