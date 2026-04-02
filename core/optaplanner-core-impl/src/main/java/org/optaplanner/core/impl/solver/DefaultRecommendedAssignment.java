package org.optaplanner.core.impl.solver;

import java.util.Comparator;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.analysis.ScoreAnalysis;
import org.optaplanner.core.api.solver.RecommendedAssignment;

/**
 * Default implementation of {@link RecommendedAssignment}.
 * <p>
 * Sorted by score descending (best first), then by insertion order via unsigned long comparison.
 *
 * @param <Proposition_> the proposition type
 * @param <Score_> the actual score type
 */
public record DefaultRecommendedAssignment<Proposition_, Score_ extends Score<Score_>>(
        long index,
        Proposition_ proposition,
        ScoreAnalysis<Score_> scoreAnalysisDiff)
        implements
            RecommendedAssignment<Proposition_, Score_>,
            Comparable<DefaultRecommendedAssignment<Proposition_, Score_>> {

    private static final Comparator<DefaultRecommendedAssignment<?, ?>> COMPARATOR =
            Comparator.<DefaultRecommendedAssignment<?, ?>> comparingLong(a -> a.scoreAnalysisDiff.score().initScore())
                    .thenComparing(a -> a.scoreAnalysisDiff.score())
                    .reversed()
                    .thenComparingLong(a -> a.index);

    public DefaultRecommendedAssignment {
        if (index < 0) {
            throw new IllegalArgumentException("The index (" + index + ") must be >= 0.");
        }
        if (scoreAnalysisDiff == null) {
            throw new IllegalArgumentException("The scoreAnalysisDiff must not be null.");
        }
    }

    @Override
    public int compareTo(DefaultRecommendedAssignment<Proposition_, Score_> other) {
        return COMPARATOR.compare(this, other);
    }

}
