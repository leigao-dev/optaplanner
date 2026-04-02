package org.optaplanner.core.api.score.analysis;

import java.util.Objects;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.constraint.ConstraintRef;
import org.optaplanner.core.api.score.stream.ConstraintJustification;

/**
 * Note: Users should never create instances of this type directly.
 * It is available transitively via {@link org.optaplanner.core.api.solver.SolutionManager#analyze(Object)}.
 *
 * @param <Score_>
 */
public record MatchAnalysis<Score_ extends Score<Score_>>(ConstraintRef constraintRef, Score_ score,
        ConstraintJustification justification) implements Comparable<MatchAnalysis<Score_>> {

    public MatchAnalysis {
        Objects.requireNonNull(constraintRef);
        Objects.requireNonNull(score);
        if (justification == null) {
            throw new IllegalArgumentException(
                    "justification must not be null. "
                            + "Use justifyWith() in your ConstraintProvider to provide a justification.");
        }
    }

    MatchAnalysis<Score_> negate() {
        return new MatchAnalysis<>(constraintRef, score.negate(), justification);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(MatchAnalysis<Score_> other) {
        int constraintComparison = constraintRef.compareTo(other.constraintRef);
        if (constraintComparison != 0) {
            return constraintComparison;
        }
        int scoreComparison = score.compareTo(other.score);
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        if (justification instanceof Comparable && other.justification instanceof Comparable) {
            return ((Comparable<Object>) justification).compareTo(other.justification);
        }
        return 0;
    }
}
