package org.optaplanner.core.api.score.analysis;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.constraint.ConstraintRef;

/**
 * Represents the breakdown of a {@link Score} into individual {@link ConstraintAnalysis} instances,
 * one for each constraint.
 * Compared to {@link org.optaplanner.core.api.score.ScoreExplanation}, this is JSON-friendly and faster to generate.
 *
 * <p>
 * In order to be fully serializable to JSON, {@link MatchAnalysis} instances must be serializable to JSON
 * and that requires any implementations of {@link org.optaplanner.core.api.score.stream.ConstraintJustification}
 * to be serializable to JSON. This is the responsibility of the user.
 *
 * <p>
 * For deserialization from JSON, the user needs to provide the deserializer themselves.
 * This is due to the fact that, once the {@link ScoreAnalysis} is received over the wire,
 * we no longer know which {@link Score} type or
 * {@link org.optaplanner.core.api.score.stream.ConstraintJustification} type was used.
 * The user has all of that information in their domain model,
 * and so they are the correct party to provide the deserializer.
 *
 * <p>
 * Note: the constructors of this record are off-limits.
 * We ask users to use exclusively {@link org.optaplanner.core.api.solver.SolutionManager#analyze(Object)}
 * to obtain instances of this record.
 *
 * @param score Score of the solution being analyzed.
 * @param constraintMap for each constraint identified by its
 *        {@link org.optaplanner.core.api.score.constraint.ConstraintRef},
 *        the {@link ConstraintAnalysis} that describes the impact of that constraint on the overall score.
 * @param isSolutionInitialized Whether the solution was fully initialized at the time of analysis.
 * @param <Score_>
 */
public record ScoreAnalysis<Score_ extends Score<Score_>>(Score_ score,
        Map<ConstraintRef, ConstraintAnalysis<Score_>> constraintMap, boolean isSolutionInitialized) {

    @SuppressWarnings("unchecked")
    private static final java.util.Comparator<ConstraintAnalysis<?>> MAP_COMPARATOR =
            (a, b) -> {
                if (a.weight() == null && b.weight() == null) {
                    return a.constraintRef().compareTo(b.constraintRef());
                }
                if (a.weight() == null) {
                    return 1;
                }
                if (b.weight() == null) {
                    return -1;
                }
                int weightComparison = ((Score) b.weight()).compareTo((Score) a.weight());
                if (weightComparison != 0) {
                    return weightComparison;
                }
                return a.constraintRef().compareTo(b.constraintRef());
            };

    static final int DEFAULT_SUMMARY_CONSTRAINT_MATCH_LIMIT = 3;

    public ScoreAnalysis(Score_ score, Map<ConstraintRef, ConstraintAnalysis<Score_>> constraintMap) {
        this(score, constraintMap, true);
    }

    public ScoreAnalysis {
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(constraintMap, "constraintMap");
        constraintMap = Collections.unmodifiableMap(constraintMap.values()
                .stream()
                .sorted(MAP_COMPARATOR)
                .collect(java.util.stream.Collectors.toMap(
                        ConstraintAnalysis::constraintRef,
                        java.util.function.Function.identity(),
                        (constraintAnalysis, otherConstraintAnalysis) -> constraintAnalysis,
                        LinkedHashMap::new)));
    }

    public ConstraintAnalysis<Score_> getConstraintAnalysis(ConstraintRef constraintRef) {
        return constraintMap.get(constraintRef);
    }

    public ConstraintAnalysis<Score_> getConstraintAnalysis(String constraintPackage, String constraintName) {
        var constraintAnalysisList = constraintMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().constraintPackage().equals(constraintPackage)
                        && entry.getKey().constraintName().equals(constraintName))
                .map(Map.Entry::getValue)
                .toList();
        return switch (constraintAnalysisList.size()) {
            case 0 -> null;
            case 1 -> constraintAnalysisList.get(0);
            default -> throw new IllegalStateException(
                    "Impossible state: Multiple constraints with the same package/name (%s/%s) are present in the score analysis."
                            .formatted(constraintPackage, constraintName));
        };
    }

    public ScoreAnalysis<Score_> diff(ScoreAnalysis<Score_> other) {
        var result = java.util.stream.Stream.concat(constraintMap.keySet().stream(),
                other.constraintMap.keySet().stream())
                .distinct()
                .flatMap(constraintRef -> {
                    var constraintAnalysis = getConstraintAnalysis(constraintRef);
                    var otherConstraintAnalysis = other.getConstraintAnalysis(constraintRef);
                    var diff = ConstraintAnalysis.diff(constraintRef, constraintAnalysis, otherConstraintAnalysis);
                    if ((diff.weight() != null && !diff.weight().isZero()) || !diff.score().isZero()) {
                        return java.util.stream.Stream.of(diff);
                    }
                    if (diff.matches() == null) {
                        if (diff.matchCount() == 0) {
                            return java.util.stream.Stream.empty();
                        } else {
                            return java.util.stream.Stream.of(diff);
                        }
                    } else if (!diff.matches().isEmpty()) {
                        return java.util.stream.Stream.of(diff);
                    } else {
                        return java.util.stream.Stream.empty();
                    }
                })
                .collect(java.util.stream.Collectors.toMap(
                        ConstraintAnalysis::constraintRef,
                        java.util.function.Function.identity(),
                        (constraintRef, otherConstraintRef) -> constraintRef,
                        java.util.HashMap::new));
        return new ScoreAnalysis<>(score.subtract(other.score()), result, isSolutionInitialized);
    }

    public String summarize() {
        return summarize(DEFAULT_SUMMARY_CONSTRAINT_MATCH_LIMIT);
    }

    public String summarize(int topMatchLimit) {
        StringBuilder builder = new StringBuilder();
        builder.append("Score analysis: score=").append(score)
                .append(", isSolutionInitialized=").append(isSolutionInitialized)
                .append(", constraints=").append(constraintMap.size()).append("\n");
        for (ConstraintAnalysis<Score_> analysis : constraintMap.values()) {
            builder.append(analysis.summarize(topMatchLimit));
        }
        return builder.toString();
    }
}
