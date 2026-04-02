package org.optaplanner.core.api.score.analysis;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.constraint.ConstraintRef;
import org.optaplanner.core.api.score.stream.ConstraintJustification;

/**
 * Note: Users should never create instances of this type directly.
 * It is available transitively via {@link org.optaplanner.core.api.solver.SolutionManager#analyze(Object)}.
 *
 * @param <Score_>
 * @param matches null if analysis not available;
 *        empty if constraint has no matches, but still non-zero constraint weight;
 *        non-empty if constraint has matches.
 *        This is a {@link List} to simplify access to individual elements,
 *        but it contains no duplicates just like {@link LinkedHashSet} wouldn't.
 * @param matchCount
 *        <ul>
 *        <li>For regular constraint analysis:
 *        -1 if analysis not available,
 *        0 if constraint has no matches,
 *        positive if constraint has matches.
 *        Equal to the size of the {@link #matches} list.</li>
 *        <li>For a {@link ScoreAnalysis#diff(ScoreAnalysis) diff of constraint analyses}:
 *        positive if the constraint has more matches in the new analysis,
 *        zero if the number of matches is the same in both,
 *        negative otherwise.
 *        Need not be equal to the size of the {@link #matches} list.</li>
 *        </ul>
 */
public record ConstraintAnalysis<Score_ extends Score<Score_>>(ConstraintRef constraintRef, Score_ weight, Score_ score,
        List<MatchAnalysis<Score_>> matches, int matchCount) {

    public ConstraintAnalysis {
        Objects.requireNonNull(constraintRef);
        Objects.requireNonNull(score);
    }

    public ConstraintAnalysis(ConstraintRef constraintRef, Score_ weight, Score_ score,
            List<MatchAnalysis<Score_>> matches) {
        this(constraintRef, weight, score, matches, matches == null ? -1 : matches.size());
    }

    public String constraintName() {
        return constraintRef.constraintName();
    }

    public String summarize() {
        return summarize(3);
    }

    public String summarize(int topMatchLimit) {
        StringBuilder builder = new StringBuilder();
        builder.append(constraintName());
        builder.append(": weight=").append(weight == null ? "null" : weight);
        builder.append(", score=").append(score);
        if (matchCount >= 0) {
            builder.append(", matchCount=").append(matchCount);
        }
        builder.append("\n");
        if (matches != null) {
            int count = 0;
            for (MatchAnalysis<Score_> match : matches) {
                if (count >= topMatchLimit) {
                    builder.append("  ... and ").append(matches.size() - topMatchLimit).append(" more\n");
                    break;
                }
                builder.append("  ").append(match.score()).append(": ")
                        .append(match.justification()).append("\n");
                count++;
            }
        }
        return builder.toString();
    }

    ConstraintAnalysis<Score_> negate() {
        Score_ negatedWeight = weight == null ? null : weight.negate();
        if (matches == null) {
            // matchCount is already -1 (unavailable); keep it as-is.
            return new ConstraintAnalysis<>(constraintRef, negatedWeight, score.negate(), null, matchCount);
        }
        List<MatchAnalysis<Score_>> negatedMatches = matches.stream().map(MatchAnalysis::negate).collect(Collectors.toList());
        // matchCount equals list size; negating score does not change the list size.
        return new ConstraintAnalysis<>(constraintRef, negatedWeight, score.negate(), negatedMatches, matchCount);
    }

    static <S extends Score<S>> ConstraintAnalysis<S> diff(
            ConstraintRef ref,
            ConstraintAnalysis<S> left,
            ConstraintAnalysis<S> right) {
        if (left == null && right == null) {
            return null;
        }
        if (left == null) {
            return right.negate();
        }
        if (right == null) {
            return left;
        }
        S weightDiff;
        if (left.weight == null && right.weight == null) {
            weightDiff = null;
        } else if (left.weight == null) {
            weightDiff = right.weight.negate();
        } else if (right.weight == null) {
            weightDiff = left.weight;
        } else {
            weightDiff = (S) left.weight.subtract(right.weight);
        }
        S scoreDiff = (S) left.score.subtract(right.score);
        if ((left.matches == null) != (right.matches == null)) {
            throw new IllegalStateException(
                    "Impossible state: constraint analysis matches availability differs between left (%s) and right (%s) for constraint (%s)."
                            .formatted(left.matches == null ? "null" : "not null",
                                    right.matches == null ? "null" : "not null", ref));
        }
        if (left.matches != null && right.matches != null) {
            Map<ConstraintJustification, List<MatchAnalysis<S>>> leftMap = mapMatchesToJustifications(left);
            Map<ConstraintJustification, List<MatchAnalysis<S>>> rightMap = mapMatchesToJustifications(right);
            LinkedHashSet<ConstraintJustification> allJustifications = new LinkedHashSet<>();
            allJustifications.addAll(leftMap.keySet());
            allJustifications.addAll(rightMap.keySet());
            List<MatchAnalysis<S>> matchDiffs = new ArrayList<>();
            for (ConstraintJustification justification : allJustifications) {
                List<MatchAnalysis<S>> leftMatches = leftMap.getOrDefault(justification, List.of());
                List<MatchAnalysis<S>> rightMatches = rightMap.getOrDefault(justification, List.of());
                S leftScore = leftMatches.stream().map(MatchAnalysis::score)
                        .reduce(left.score.zero(), Score::add);
                S rightScore = rightMatches.stream().map(MatchAnalysis::score)
                        .reduce(right.score.zero(), Score::add);
                S diffScore = (S) leftScore.subtract(rightScore);
                if (!diffScore.isZero()) {
                    matchDiffs.add(new MatchAnalysis<>(ref, diffScore, justification));
                }
            }
            return new ConstraintAnalysis<>(ref, weightDiff, scoreDiff, matchDiffs);
        }
        int matchCountDiff = left.matchCount - right.matchCount;
        return new ConstraintAnalysis<>(ref, weightDiff, scoreDiff, null, matchCountDiff);
    }

    private static <S extends Score<S>> Map<ConstraintJustification, List<MatchAnalysis<S>>> mapMatchesToJustifications(
            ConstraintAnalysis<S> analysis) {
        return analysis.matches.stream().collect(Collectors.groupingBy(MatchAnalysis::justification));
    }
}
