package org.optaplanner.core.impl.score.constraint;

import org.optaplanner.core.api.solver.ScoreAnalysisFetchPolicy;

/**
 * Determines whether constraint match tracking is enabled and whether constraint match justification is enabled.
 * This is a three-state enum that provides finer control over constraint match tracking
 * compared to the binary {@code boolean isConstraintMatchEnabled()}.
 *
 * @see org.optaplanner.core.api.score.constraint.ConstraintMatch
 * @see org.optaplanner.core.api.score.stream.ConstraintJustification
 */
public enum ConstraintMatchPolicy {

    /**
     * Constraint match tracking is fully disabled.
     */
    DISABLED(false, false),

    /**
     * Constraint match tracking is enabled, but justification objects are not collected.
     * This provides better performance when only match counts and scores are needed.
     */
    ENABLED_WITHOUT_JUSTIFICATIONS(true, false),

    /**
     * Constraint match tracking is fully enabled, including justification objects.
     */
    ENABLED(true, true);

    /**
     * Returns the match policy best suited for the given fetch policy,
     * to achieve the most performance out of the underlying solver.
     * For example, if the fetch policy specifies that only match counts are necessary
     * ({@link ScoreAnalysisFetchPolicy#FETCH_MATCH_COUNT}),
     * the solver can be configured to not produce justifications
     * ({@link #ENABLED_WITHOUT_JUSTIFICATIONS}).
     *
     * @param scoreAnalysisFetchPolicy never null
     * @return never null
     */
    public static ConstraintMatchPolicy match(ScoreAnalysisFetchPolicy scoreAnalysisFetchPolicy) {
        return switch (scoreAnalysisFetchPolicy) {
            case FETCH_MATCH_COUNT, FETCH_SHALLOW -> ENABLED_WITHOUT_JUSTIFICATIONS;
            case FETCH_ALL -> ENABLED;
        };
    }

    private final boolean enabled;
    private final boolean justificationEnabled;

    ConstraintMatchPolicy(boolean enabled, boolean justificationEnabled) {
        this.enabled = enabled;
        this.justificationEnabled = justificationEnabled;
    }

    /**
     * @return true if constraint match tracking is enabled at all
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return true if constraint match justification objects are being collected
     */
    public boolean isJustificationEnabled() {
        return justificationEnabled;
    }

}
