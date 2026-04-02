package org.optaplanner.core.impl.domain.variable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.score.director.AbstractScoreDirector;
import org.optaplanner.core.impl.score.director.AbstractScoreDirectorFactory;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;

/**
 * Utility class for updating shadow variables without requiring a full solver configuration.
 * <p>
 * This is useful for scenarios where you need to update shadow variables on a solution or entities
 * but don't have a {@link org.optaplanner.core.api.solver.SolverFactory} available,
 * such as in tests or standalone processing.
 *
 * @param <Solution_> the solution type
 */
public final class ShadowVariableUpdateHelper<Solution_> {

    public static <Solution_> ShadowVariableUpdateHelper<Solution_> create() {
        return new ShadowVariableUpdateHelper<>();
    }

    private ShadowVariableUpdateHelper() {
    }

    /**
     * Updates all shadow variables on the given solution.
     * <p>
     * This discovers entity classes by scanning the solution's collection fields for types
     * annotated with {@link PlanningEntity}, builds a {@link SolutionDescriptor},
     * creates an internal score director, sets the working solution,
     * and forces all variable listeners to trigger, updating shadow variables.
     *
     * @param solution never null
     */
    @SuppressWarnings("unchecked")
    public void updateShadowVariables(Solution_ solution) {
        var solutionClass = (Class<Solution_>) Objects.requireNonNull(solution).getClass();
        var entityClassSet = discoverEntityClasses(solution);
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(solutionClass,
                entityClassSet.toArray(new Class<?>[0]));
        try (var scoreDirector = new InternalScoreDirectorFactory<>(solutionDescriptor)
                .buildScoreDirector(false, false)) {
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.forceTriggerVariableListeners();
        }
    }

    /**
     * Discovers entity classes by scanning the solution's collection and array fields/methods
     * for element types annotated with {@link PlanningEntity}.
     *
     * @param solution never null
     * @return never null, a set of discovered entity classes
     */
    @SuppressWarnings("unchecked")
    private LinkedHashSet<Class<?>> discoverEntityClasses(Solution_ solution) {
        var entityClassSet = new LinkedHashSet<Class<?>>();
        var solutionClass = solution.getClass();
        // Scan fields
        for (var field : getAllFields(solutionClass)) {
            field.setAccessible(true);
            try {
                var value = field.get(solution);
                if (value instanceof Collection<?> collection) {
                    collectEntityClassesFromCollection(collection, entityClassSet);
                } else if (value != null && value.getClass().isArray()) {
                    for (var element : (Object[]) value) {
                        if (element != null && element.getClass().isAnnotationPresent(PlanningEntity.class)) {
                            entityClassSet.add(element.getClass());
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                // Skip inaccessible fields
            }
        }
        // Scan getter methods that return collections
        for (var method : solutionClass.getMethods()) {
            if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                try {
                    var value = method.invoke(solution);
                    if (value instanceof Collection<?> collection) {
                        collectEntityClassesFromCollection(collection, entityClassSet);
                    }
                } catch (ReflectiveOperationException e) {
                    // Skip methods that fail
                }
            }
        }
        return entityClassSet;
    }

    private void collectEntityClassesFromCollection(Collection<?> collection, LinkedHashSet<Class<?>> entityClassSet) {
        for (var element : collection) {
            if (element != null && element.getClass().isAnnotationPresent(PlanningEntity.class)) {
                entityClassSet.add(element.getClass());
            }
        }
    }

    private static java.util.List<Field> getAllFields(Class<?> clazz) {
        var fields = new ArrayList<Field>();
        var current = clazz;
        while (current != null && current != Object.class) {
            for (var field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * Updates all shadow variables on the given entities, which belong to the given solution class.
     * <p>
     * This is a simplified version that builds a minimal solution descriptor for the entity classes
     * and triggers shadow variable updates. For more complex scenarios involving custom variable listeners
     * or cascading updates, use {@link #updateShadowVariables(Object)} with a complete solution instead.
     *
     * @param solutionClass never null
     * @param entities all entities to be updated
     */
    @SuppressWarnings("unchecked")
    public void updateShadowVariables(Class<Solution_> solutionClass, Object... entities) {
        Objects.requireNonNull(solutionClass);
        Objects.requireNonNull(entities);
        if (entities.length == 0) {
            throw new IllegalArgumentException("The entity array cannot be empty.");
        }
        var entityClassList = java.util.Arrays.stream(entities).map(Object::getClass).distinct().toList();
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(solutionClass,
                entityClassList.toArray(new Class<?>[0]));
        try (var scoreDirector = new InternalScoreDirectorFactory<>(solutionDescriptor)
                .buildScoreDirector(false, false)) {
            // Build a minimal solution and populate entity collections via member accessors
            var solution = (Solution_) solutionDescriptor.getSolutionClass().getConstructor().newInstance();
            for (var memberAccessor : solutionDescriptor.getEntityCollectionMemberAccessorMap().values()) {
                var elementType = getMemberAccessorCollectionElementType(memberAccessor);
                if (elementType == null) {
                    continue;
                }
                var matchedEntities = java.util.Arrays.stream(entities)
                        .filter(e -> elementType.isAssignableFrom(e.getClass()))
                        .collect(java.util.stream.Collectors.toList());
                if (!matchedEntities.isEmpty()) {
                    memberAccessor.executeSetter(solution, new java.util.ArrayList<>(matchedEntities));
                }
            }
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.forceTriggerVariableListeners();
            // Shadow variables updated in-place on the original entity objects
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to create solution instance for class " + solutionClass.getName(), e);
        }
    }

    private static Class<?> getMemberAccessorCollectionElementType(
            org.optaplanner.core.impl.domain.common.accessor.MemberAccessor memberAccessor) {
        var genericType = memberAccessor.getGenericType();
        if (genericType instanceof java.lang.reflect.ParameterizedType paramType) {
            var typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                return clazz;
            }
        }
        return null;
    }

    // ************************************************************************
    // Internal minimal score director factory and director
    // ************************************************************************

    private static class InternalScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
            extends AbstractScoreDirectorFactory<Solution_, Score_> {

        public InternalScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor) {
            super(solutionDescriptor);
        }

        @Override
        public InnerScoreDirector<Solution_, Score_> buildScoreDirector(boolean lookUpEnabled,
                boolean constraintMatchEnabledPreference) {
            return new InternalScoreDirector<>(this, lookUpEnabled, constraintMatchEnabledPreference);
        }
    }

    private static class InternalScoreDirector<Solution_, Score_ extends Score<Score_>>
            extends AbstractScoreDirector<Solution_, Score_, InternalScoreDirectorFactory<Solution_, Score_>> {

        protected InternalScoreDirector(InternalScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory,
                boolean lookUpEnabled, boolean constraintMatchEnabledPreference) {
            super(scoreDirectorFactory, lookUpEnabled, constraintMatchEnabledPreference);
        }

        @Override
        public Score_ calculateScore() {
            throw new UnsupportedOperationException(
                    "InternalScoreDirector is only used for shadow variable updates, not score calculation.");
        }

        @Override
        public boolean isConstraintMatchEnabled() {
            return false;
        }

        @Override
        public Map<String, ConstraintMatchTotal<Score_>> getConstraintMatchTotalMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<Object, Indictment<Score_>> getIndictmentMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void overwriteConstraintMatchEnabledPreference(boolean constraintMatchEnabledPreference) {
            // No-op
        }

        @Override
        public Score_ doAndProcessMove(org.optaplanner.core.impl.heuristic.move.Move<Solution_> move,
                boolean assertMoveScoreFromScratch) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void doAndProcessMove(org.optaplanner.core.impl.heuristic.move.Move<Solution_> move,
                boolean assertMoveScoreFromScratch, java.util.function.Consumer<Score_> moveProcessor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWorkingEntityListDirty(long expectedWorkingEntityListRevision) {
            return false;
        }

        @Override
        public boolean requiresFlushing() {
            return false;
        }

        @Override
        public InternalScoreDirector<Solution_, Score_> clone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public InnerScoreDirector<Solution_, Score_> createChildThreadScoreDirector(
                org.optaplanner.core.impl.solver.thread.ChildThreadType childThreadType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void assertExpectedWorkingScore(Score_ expectedWorkingScore, Object completedAction) {
            // No-op
        }

        @Override
        public void assertShadowVariablesAreNotStale(Score_ expectedWorkingScore, Object completedAction) {
            // No-op
        }

        @Override
        public void assertWorkingScoreFromScratch(Score_ workingScore, Object completedAction) {
            // No-op
        }

        @Override
        public void assertPredictedScoreFromScratch(Score_ predictedScore, Object completedAction) {
            // No-op
        }

        @Override
        public void assertExpectedUndoMoveScore(org.optaplanner.core.impl.heuristic.move.Move<Solution_> move,
                Score_ beforeMoveScore) {
            // No-op
        }

        @Override
        public void assertNonNullPlanningIds() {
            // No-op
        }
    }

}
