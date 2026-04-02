package org.optaplanner.core.api.score.constraint;

import java.util.Objects;

/**
 * Unique identifier of a constraint.
 *
 * @param constraintPackage never null
 * @param constraintName never null
 */
public record ConstraintRef(String constraintPackage, String constraintName) implements Comparable<ConstraintRef> {

    public ConstraintRef {
        Objects.requireNonNull(constraintPackage, "constraintPackage must not be null");
        Objects.requireNonNull(constraintName, "constraintName must not be null");
    }

    public static ConstraintRef of(String constraintPackage, String constraintName) {
        return new ConstraintRef(constraintPackage, constraintName);
    }

    public String constraintId() {
        return ConstraintMatchTotal.composeConstraintId(constraintPackage, constraintName);
    }

    @Override
    public int compareTo(ConstraintRef other) {
        int packageComparison = constraintPackage.compareTo(other.constraintPackage);
        if (packageComparison != 0) {
            return packageComparison;
        }
        return constraintName.compareTo(other.constraintName);
    }
}
