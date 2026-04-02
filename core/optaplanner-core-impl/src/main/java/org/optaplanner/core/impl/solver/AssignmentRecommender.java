package org.optaplanner.core.impl.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.optaplanner.core.api.domain.valuerange.CountableValueRange;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.analysis.ScoreAnalysis;
import org.optaplanner.core.api.solver.ScoreAnalysisFetchPolicy;
import org.optaplanner.core.impl.domain.entity.descriptor.EntityDescriptor;
import org.optaplanner.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import org.optaplanner.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.move.generic.ChangeMove;
import org.optaplanner.core.impl.heuristic.selector.move.generic.list.ListAssignMove;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;

/**
 * Internal class that enumerates all possible assignments for a given entity or element,
 * evaluates each one, and returns sorted recommendations.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the actual score type
 * @param <Recommendation_> the recommendation type
 * @param <In_> the input entity/element type
 * @param <Out_> the proposition type
 */
final class AssignmentRecommender<Solution_, Score_ extends Score<Score_>, Recommendation_, In_, Out_>
        implements Function<InnerScoreDirector<Solution_, Score_>, List<Recommendation_>> {

    private final Function<In_, Out_> propositionFunction;
    private final RecommendationConstructor<Score_, Recommendation_, Out_> recommendationConstructor;
    private final ScoreAnalysisFetchPolicy fetchPolicy;
    private final In_ evaluatedEntityOrElement;
    private final ScoreAnalysis<Score_> originalScoreAnalysis;

    AssignmentRecommender(
            Function<In_, Out_> propositionFunction,
            RecommendationConstructor<Score_, Recommendation_, Out_> recommendationConstructor,
            ScoreAnalysisFetchPolicy fetchPolicy,
            In_ evaluatedEntityOrElement,
            ScoreAnalysis<Score_> originalScoreAnalysis) {
        this.propositionFunction = Objects.requireNonNull(propositionFunction);
        this.recommendationConstructor = Objects.requireNonNull(recommendationConstructor);
        this.fetchPolicy = Objects.requireNonNull(fetchPolicy);
        this.evaluatedEntityOrElement = Objects.requireNonNull(evaluatedEntityOrElement);
        this.originalScoreAnalysis = Objects.requireNonNull(originalScoreAnalysis);
    }

    @Override
    public List<Recommendation_> apply(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var entity = evaluatedEntityOrElement;

        // Determine if the entity has basic variables or is a list variable element
        var entityDescriptor = findEntityDescriptor(solutionDescriptor, entity);

        List<Recommendation_> recommendations;
        if (hasListVariable(entityDescriptor)) {
            recommendations = processListVariableElement(scoreDirector, entityDescriptor, entity);
        } else {
            recommendations = processBasicVariableEntity(scoreDirector, entityDescriptor, entity);
        }

        // Sort: best score first (stable sort by index as tiebreaker)
        Collections.sort(recommendations, (a, b) -> {
            if (a instanceof Comparable && b instanceof Comparable) {
                return ((Comparable) a).compareTo(b);
            }
            return 0;
        });
        return recommendations;
    }

    private EntityDescriptor<Solution_> findEntityDescriptor(
            org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor<Solution_> solutionDescriptor,
            Object entity) {
        for (var entityDescriptor : solutionDescriptor.getGenuineEntityDescriptors()) {
            if (entityDescriptor.getEntityClass().isInstance(entity)) {
                return entityDescriptor;
            }
        }
        throw new IllegalStateException(
                "The evaluatedEntityOrElement (" + entity + ") is not a known planning entity.");
    }

    private boolean hasListVariable(EntityDescriptor<Solution_> entityDescriptor) {
        return entityDescriptor.hasAnyGenuineListVariables();
    }

    @SuppressWarnings("unchecked")
    private ListVariableDescriptor<Solution_> getListVariableDescriptor(EntityDescriptor<Solution_> entityDescriptor) {
        return (ListVariableDescriptor<Solution_>) entityDescriptor.getGenuineVariableDescriptorList().stream()
                .filter(GenuineVariableDescriptor::isListVariable)
                .findFirst()
                .orElse(null);
    }

    // ************************************************************************
    // Basic variable processing
    // ************************************************************************

    private List<Recommendation_> processBasicVariableEntity(
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            EntityDescriptor<Solution_> entityDescriptor, In_ entity) {
        var recommendations = new ArrayList<Recommendation_>();
        long moveIndex = 0;

        // Unassign the entity's current values first
        var genuineVariableDescriptors = entityDescriptor.getGenuineVariableDescriptorList();
        var undoMoves = new ArrayList<Move<Solution_>>();

        for (var variableDescriptor : genuineVariableDescriptors) {
            var currentValue = variableDescriptor.getValue(entity);
            if (currentValue != null) {
                var unassignMove = new ChangeMove<Solution_>(variableDescriptor, entity, null);
                var undoMove = unassignMove.doMove(scoreDirector);
                undoMoves.add(undoMove);
            }
        }

        // Trigger variable listeners after unassignment
        scoreDirector.triggerVariableListeners();

        // Enumerate all possible combinations (for single variable: just iterate values)
        if (genuineVariableDescriptors.size() == 1) {
            var variableDescriptor = genuineVariableDescriptors.get(0);
            moveIndex = enumerateBasicVariable(scoreDirector, entity, variableDescriptor, moveIndex, recommendations);
        } else {
            // Multiple genuine variables: enumerate each independently
            for (var variableDescriptor : genuineVariableDescriptors) {
                moveIndex = enumerateBasicVariable(scoreDirector, entity, variableDescriptor, moveIndex, recommendations);
            }
        }

        // Undo the unassignment (in reverse order)
        for (int i = undoMoves.size() - 1; i >= 0; i--) {
            undoMoves.get(i).doMoveOnly(scoreDirector);
        }
        scoreDirector.triggerVariableListeners();

        return recommendations;
    }

    private long enumerateBasicVariable(
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            In_ entity, GenuineVariableDescriptor<Solution_> variableDescriptor,
            long moveIndex, List<Recommendation_> recommendations) {
        var solution = scoreDirector.getWorkingSolution();
        var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
        var valueRange = valueRangeDescriptor.extractValueRange(solution, entity);

        if (valueRange instanceof CountableValueRange<?> countableRange) {
            var iterator = countableRange.createOriginalIterator();
            while (iterator.hasNext()) {
                var value = iterator.next();
                var move = new ChangeMove<Solution_>(variableDescriptor, entity, value);
                moveIndex = evaluateMove(scoreDirector, move, moveIndex, entity, recommendations);
            }
        } else {
            throw new IllegalStateException(
                    "Cannot enumerate values for non-countable value range on variable ("
                            + variableDescriptor.getVariableName() + ").");
        }
        return moveIndex;
    }

    // ************************************************************************
    // List variable processing
    // ************************************************************************

    private List<Recommendation_> processListVariableElement(
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            EntityDescriptor<Solution_> entityDescriptor, In_ element) {
        var recommendations = new ArrayList<Recommendation_>();
        var listVariableDescriptor = getListVariableDescriptor(entityDescriptor);
        var solution = scoreDirector.getWorkingSolution();
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();

        // Find where the element is currently assigned and remove it
        var sourceEntity = findSourceEntityForElement(scoreDirector, entityDescriptor, element);
        var currentList = listVariableDescriptor.getListVariable(sourceEntity);
        int sourceIndex = -1;
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i) == element) {
                sourceIndex = i;
                break;
            }
        }
        if (sourceIndex >= 0) {
            // Remove element from current position (matching ListUnassignMove notification pattern)
            scoreDirector.beforeListVariableChanged(listVariableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
            scoreDirector.beforeListVariableElementUnassigned(listVariableDescriptor, element);
            currentList.remove(sourceIndex);
            scoreDirector.afterListVariableElementUnassigned(listVariableDescriptor, element);
            scoreDirector.afterListVariableChanged(listVariableDescriptor, sourceEntity, sourceIndex, sourceIndex);
            scoreDirector.triggerVariableListeners();
        }

        // Enumerate all possible destinations
        long moveIndex = 0;
        for (var targetEntityDescriptor : solutionDescriptor.getGenuineEntityDescriptors()) {
            var targetListVar = getListVariableDescriptor(targetEntityDescriptor);
            if (targetListVar == null) {
                continue;
            }
            if (!targetListVar.equals(listVariableDescriptor)) {
                continue;
            }

            // Collect all working entities of this type
            var targetEntities = new ArrayList<Object>();
            solutionDescriptor.visitEntitiesByEntityClass(solution,
                    targetEntityDescriptor.getEntityClass(), targetEntities::add);

            // Iterate all working entities of this type
            for (var targetEntity : targetEntities) {
                var targetList = targetListVar.getListVariable(targetEntity);
                int targetListSize = targetList.size();

                // Try each position (0..size, since we can insert at the end)
                for (int destIndex = 0; destIndex <= targetListSize; destIndex++) {
                    var move = new ListAssignMove<Solution_>(listVariableDescriptor, element, targetEntity, destIndex);
                    moveIndex = evaluateMove(scoreDirector, move, moveIndex, element, recommendations);
                }
            }
        }

        // Restore the element to its original position (matching ListAssignMove notification pattern)
        if (sourceIndex >= 0) {
            scoreDirector.beforeListVariableChanged(listVariableDescriptor, sourceEntity, sourceIndex, sourceIndex);
            scoreDirector.beforeListVariableElementAssigned(listVariableDescriptor, element);
            currentList.add(sourceIndex, element);
            scoreDirector.afterListVariableElementAssigned(listVariableDescriptor, element);
            scoreDirector.afterListVariableChanged(listVariableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
            scoreDirector.triggerVariableListeners();
        }

        return recommendations;
    }

    private Object findSourceEntityForElement(
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            EntityDescriptor<Solution_> entityDescriptor, Object element) {
        var listVariableDescriptor = getListVariableDescriptor(entityDescriptor);
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var solution = scoreDirector.getWorkingSolution();
        var found = new Object[1];
        solutionDescriptor.visitEntitiesByEntityClass(solution,
                entityDescriptor.getEntityClass(), entity -> {
                    var list = listVariableDescriptor.getListVariable(entity);
                    for (var item : list) {
                        if (item == element) {
                            found[0] = entity;
                        }
                    }
                });
        if (found[0] == null) {
            throw new IllegalStateException(
                    "The element (" + element + ") is not currently assigned to any entity's list variable.");
        }
        return found[0];
    }

    // ************************************************************************
    // Move evaluation
    // ************************************************************************

    @SuppressWarnings("unchecked")
    private long evaluateMove(
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            Move<Solution_> move, long moveIndex,
            In_ clonedElement, List<Recommendation_> recommendations) {
        // Apply the move temporarily
        var undoMove = move.doMove(scoreDirector);
        // Calculate score analysis with the move applied
        var newScoreAnalysis = scoreDirector.buildScoreAnalysis(fetchPolicy);
        var diff = newScoreAnalysis.diff(originalScoreAnalysis);
        // Call the user's proposition function
        Out_ proposition = propositionFunction.apply(clonedElement);
        var recommendation = recommendationConstructor.apply(moveIndex, proposition, diff);
        recommendations.add(recommendation);
        // Undo the move
        undoMove.doMoveOnly(scoreDirector);
        return moveIndex + 1;
    }

}
