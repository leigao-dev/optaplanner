package org.optaplanner.core.impl.domain.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.core.impl.testdata.domain.shadow.TestdataShadowedSolution;
import org.optaplanner.core.impl.testdata.domain.shadow.inverserelation.TestdataInverseRelationEntity;
import org.optaplanner.core.impl.testdata.domain.shadow.inverserelation.TestdataInverseRelationSolution;
import org.optaplanner.core.impl.testdata.domain.shadow.inverserelation.TestdataInverseRelationValue;

/**
 * Tests for {@link SolutionManager#updateShadowVariables(Class, Object...)}
 * and {@link SolutionManager#updateShadowVariables(Object)}.
 */
public class ShadowVariableUpdateHelperTest {

    @Test
    void emptyEntitiesThrowsException() {
        // Pass empty Object[] explicitly to select (Class, Object...) overload;
        // otherwise Java resolves to (Solution_) overload since Class<X> matches Solution_
        assertThatThrownBy(() -> SolutionManager.updateShadowVariables(
                TestdataInverseRelationSolution.class, new Object[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entity array cannot be empty");
    }

    /**
     * Tests that updateShadowVariables works on a consistent solution.
     * Uses TestdataShadowedSolution which has a shadow variable updated by VariableListener.
     */
    @Test
    void solutionUpdateShadowVariableOnConsistentSolution() {
        TestdataShadowedSolution solution = TestdataShadowedSolution.generateSolution(3, 2);

        // Initially, shadow variables are not set (not solved yet)
        // After updateShadowVariables, the firstShadow should be computed
        SolutionManager.updateShadowVariables(solution);

        // Verify shadow variables are computed
        for (var entity : solution.getEntityList()) {
            if (entity.getValue() != null) {
                assertThat(entity.getFirstShadow())
                        .isEqualTo(entity.getValue().getCode() + "/firstShadow");
            }
        }
    }

    /**
     * Tests solution-based updateShadowVariables with inverse relation entities.
     * The solution must be in a consistent state before calling updateShadowVariables.
     */
    @Test
    void solutionUpdateInverseRelationConsistentSolution() {
        TestdataInverseRelationValue v0 = new TestdataInverseRelationValue("v0");
        TestdataInverseRelationValue v1 = new TestdataInverseRelationValue("v1");
        // Use constructor that maintains inverse relation consistency
        TestdataInverseRelationEntity e0 = new TestdataInverseRelationEntity("e0", v0);
        TestdataInverseRelationEntity e1 = new TestdataInverseRelationEntity("e1", v1);

        TestdataInverseRelationSolution solution = new TestdataInverseRelationSolution("solution");
        solution.setValueList(new ArrayList<>(List.of(v0, v1)));
        solution.setEntityList(new ArrayList<>(List.of(e0, e1)));

        // Initial state is consistent
        assertThat(v0.getEntities()).containsExactly(e0);
        assertThat(v1.getEntities()).containsExactly(e1);

        // Update shadow variables (should be a no-op for consistent state)
        SolutionManager.updateShadowVariables(solution);

        // State should remain consistent
        assertThat(v0.getEntities()).containsExactly(e0);
        assertThat(v1.getEntities()).containsExactly(e1);
    }

    /**
     * Tests updateShadowVariables on a solution with null-valued entities.
     */
    @Test
    void solutionUpdateWithNullValues() {
        TestdataInverseRelationValue v0 = new TestdataInverseRelationValue("v0");
        TestdataInverseRelationEntity e0 = new TestdataInverseRelationEntity("e0"); // value = null
        TestdataInverseRelationEntity e1 = new TestdataInverseRelationEntity("e1"); // value = null

        TestdataInverseRelationSolution solution = new TestdataInverseRelationSolution("solution");
        solution.setValueList(new ArrayList<>(List.of(v0)));
        solution.setEntityList(new ArrayList<>(List.of(e0, e1)));

        // Entities have null values, so inverse relation should be empty
        assertThat(v0.getEntities()).isEmpty();

        SolutionManager.updateShadowVariables(solution);

        // Nothing should change - entities with null values don't have inverse relations
        assertThat(v0.getEntities()).isEmpty();
    }

}
