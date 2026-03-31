package org.optaplanner.core.api.solver;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.api.solver.change.ProblemChange;

/**
 * A ProblemFactChange represents a change in 1 or more problem facts of a {@link PlanningSolution}.
 * Problem facts used by a {@link Solver} must not be changed while it is solving,
 * but by scheduling this command to the {@link Solver}, you can change them when the time is right.
 * <p>
 * Note that the {@link Solver} clones a {@link PlanningSolution} at will.
 * So any change must be done on the problem facts and planning entities referenced by the {@link PlanningSolution}
 * of the {@link ScoreDirector}. On each change it should also notify the {@link ScoreDirector} accordingly.
 * <p>
 * <b>Difference between {@code ProblemFactChange} and {@link ProblemChange}:</b>
 * <ul>
 * <li>{@link ProblemChange} is the modern, recommended replacement for this interface.
 * Its {@code doChange} method receives both the {@link PlanningSolution working solution} and a
 * {@link org.optaplanner.core.api.solver.change.ProblemChangeDirector}, which provides high-level methods
 * ({@code addEntity}, {@code removeEntity}, {@code changeVariable}, {@code addProblemFact},
 * {@code removeProblemFact}, {@code changeProblemProperty}) to safely perform modifications.</li>
 * <li>{@code ProblemFactChange} (this interface) is the legacy approach. Its {@code doChange} method receives
 * a {@link ScoreDirector} directly, which requires the caller to manually notify the {@link ScoreDirector}
 * after every change. Failing to do so will silently corrupt the score calculation.</li>
 * <li>With {@link ProblemChange}, {@link org.optaplanner.core.api.domain.variable.VariableListener variable listeners}
 * are triggered automatically after the change is applied. With {@code ProblemFactChange}, the caller is
 * responsible for notifying the {@link ScoreDirector} so that variable listeners are invoked correctly.</li>
 * <li>{@link ProblemChange} can change both planning entities and problem facts, while the name
 * {@code ProblemFactChange} implies it was originally focused on problem facts only (though it also allowed
 * entity changes via the {@link ScoreDirector}).</li>
 * </ul>
 *
 * @deprecated Prefer {@link ProblemChange}, which provides a safer, higher-level API with automatic
 *             variable listener triggering via {@link org.optaplanner.core.api.solver.change.ProblemChangeDirector}.
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@Deprecated(forRemoval = true)
@FunctionalInterface
public interface ProblemFactChange<Solution_> {

    /**
     * Does the change on the {@link PlanningSolution} of the {@link ScoreDirector}
     * and notifies the {@link ScoreDirector} accordingly.
     * Every modification to the {@link PlanningSolution}, must be correctly notified to the {@link ScoreDirector},
     * otherwise the {@link Score} calculation will be corrupted.
     *
     * @param scoreDirector never null
     *        Contains the {@link PlanningSolution working solution} which contains the problem facts
     *        (and {@link PlanningEntity planning entities}) to change.
     *        Also needs to get notified of those changes.
     */
    void doChange(ScoreDirector<Solution_> scoreDirector);

}
