package org.optaplanner.core.impl.score.director;

import java.util.Objects;

import org.optaplanner.core.api.score.Score;

/**
 * Wraps a {@link Score} together with an unassigned variable count,
 * providing a combined view of score quality and solution initialization state.
 * <p>
 * This is used internally by the solver to track both the raw score and initialization progress
 * in a single comparable object.
 *
 * @param <Score_> the actual score type
 * @param raw the raw score, never null
 * @param unassignedCount the number of uninitialized planning variables, >= 0
 */
public record InnerScore<Score_ extends Score<Score_>>(Score_ raw, int unassignedCount)
        implements
            Comparable<InnerScore<Score_>> {

    public InnerScore {
        Objects.requireNonNull(raw);
        if (unassignedCount < 0) {
            throw new IllegalArgumentException("The unassignedCount (" + unassignedCount + ") must be >= 0.");
        }
    }

    /**
     * Creates an InnerScore for a fully assigned solution (no uninitialized variables).
     *
     * @param score never null
     * @return never null
     */
    public static <Score_ extends Score<Score_>> InnerScore<Score_> fullyAssigned(Score_ score) {
        return new InnerScore<>(score, 0);
    }

    /**
     * Creates an InnerScore with the given unassigned count.
     *
     * @param score never null
     * @param unassignedCount >= 0
     * @return never null
     */
    public static <Score_ extends Score<Score_>> InnerScore<Score_> withUnassignedCount(Score_ score,
            int unassignedCount) {
        return new InnerScore<>(score, unassignedCount);
    }

    /**
     * @return true if all planning variables are initialized (unassignedCount == 0)
     */
    public boolean isFullyAssigned() {
        return unassignedCount == 0;
    }

    /**
     * Compares two InnerScore instances.
     * Solutions with fewer unassigned variables are considered better.
     * If unassigned counts are equal, the raw score is compared.
     */
    @Override
    public int compareTo(InnerScore<Score_> other) {
        int uninitializedCountComparison = Integer.compare(unassignedCount, other.unassignedCount);
        if (uninitializedCountComparison != 0) {
            return -uninitializedCountComparison; // Fewer unassigned is better
        }
        return raw.compareTo(other.raw);
    }

    @Override
    public String toString() {
        return isFullyAssigned() ? raw.toString() : "-" + unassignedCount + "init/" + raw;
    }

}
