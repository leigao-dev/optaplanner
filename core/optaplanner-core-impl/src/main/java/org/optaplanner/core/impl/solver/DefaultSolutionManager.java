package org.optaplanner.core.impl.solver;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.analysis.ScoreAnalysis;
import org.optaplanner.core.api.solver.RecommendedAssignment;
import org.optaplanner.core.api.solver.ScoreAnalysisFetchPolicy;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.core.api.solver.SolutionUpdatePolicy;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.impl.score.DefaultScoreExplanation;
import org.optaplanner.core.impl.score.constraint.ConstraintMatchPolicy;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;
import org.optaplanner.core.impl.score.director.InnerScoreDirectorFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class DefaultSolutionManager<Solution_, Score_ extends Score<Score_>>
        implements SolutionManager<Solution_, Score_> {

    private final InnerScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory;

    public <ProblemId_> DefaultSolutionManager(SolverManager<Solution_, ProblemId_> solverManager) {
        this(((DefaultSolverManager<Solution_, ProblemId_>) solverManager).getSolverFactory());
    }

    public DefaultSolutionManager(SolverFactory<Solution_> solverFactory) {
        this.scoreDirectorFactory = ((DefaultSolverFactory<Solution_>) solverFactory).getScoreDirectorFactory();
    }

    public InnerScoreDirectorFactory<Solution_, Score_> getScoreDirectorFactory() {
        return scoreDirectorFactory;
    }

    @Override
    public Score_ update(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy) {
        if (solutionUpdatePolicy == SolutionUpdatePolicy.NO_UPDATE) {
            throw new IllegalArgumentException("Can not call " + this.getClass().getSimpleName()
                    + ".update() with this solutionUpdatePolicy (" + solutionUpdatePolicy + ").");
        }
        return callScoreDirector(solution, solutionUpdatePolicy,
                s -> (Score_) s.getSolutionDescriptor().getScore(s.getWorkingSolution()),
                false);
    }

    @Override
    public ScoreExplanation<Solution_, Score_> explain(Solution_ solution, SolutionUpdatePolicy solutionUpdatePolicy) {
        Score_ currentScore = (Score_) scoreDirectorFactory.getSolutionDescriptor().getScore(solution);
        ScoreExplanation<Solution_, Score_> explanation =
                callScoreDirector(solution, solutionUpdatePolicy, DefaultScoreExplanation::new, true);
        if (!solutionUpdatePolicy.isScoreUpdateEnabled() && currentScore != null) {
            // Score update is not enabled and score is not null; this means the score is supposed to be valid.
            // Yet it is different from a freshly calculated score, suggesting previous score corruption.
            Score_ freshScore = explanation.getScore();
            if (!freshScore.equals(currentScore)) {
                throw new IllegalStateException("Current score (" + currentScore + ") and freshly calculated score ("
                        + freshScore + ") for solution (" + solution + ") do not match.\n"
                        + "Maybe run " + EnvironmentMode.FULL_ASSERT + " to check for score corruptions.\n"
                        + "Otherwise enable " + SolutionUpdatePolicy.class.getSimpleName()
                        + "." + SolutionUpdatePolicy.UPDATE_ALL + " to update the stale score.");
            }
        }
        return explanation;
    }

    @Override
    public ScoreAnalysis<Score_> analyze(Solution_ solution, ScoreAnalysisFetchPolicy scoreAnalysisFetchPolicy,
            SolutionUpdatePolicy solutionUpdatePolicy) {
        Objects.requireNonNull(scoreAnalysisFetchPolicy);
        if (solutionUpdatePolicy == SolutionUpdatePolicy.NO_UPDATE) {
            throw new IllegalArgumentException("Can not call " + this.getClass().getSimpleName()
                    + ".analyze() with this solutionUpdatePolicy (" + solutionUpdatePolicy + ").");
        }
        ConstraintMatchPolicy constraintMatchPolicy = ConstraintMatchPolicy.match(scoreAnalysisFetchPolicy);
        Score_ currentScore = (Score_) scoreDirectorFactory.getSolutionDescriptor().getScore(solution);
        ScoreAnalysis<Score_> analysis = callScoreDirector(solution, solutionUpdatePolicy,
                scoreDirector -> scoreDirector.buildScoreAnalysis(scoreAnalysisFetchPolicy), constraintMatchPolicy);
        if (!solutionUpdatePolicy.isScoreUpdateEnabled() && currentScore != null) {
            Score_ freshScore = analysis.score();
            if (!freshScore.equals(currentScore)) {
                throw new IllegalStateException("Current score (" + currentScore + ") and freshly calculated score ("
                        + freshScore + ") for solution (" + solution + ") do not match.\n"
                        + "Maybe run " + EnvironmentMode.FULL_ASSERT + " to check for score corruptions.\n"
                        + "Otherwise enable " + SolutionUpdatePolicy.class.getSimpleName()
                        + "." + SolutionUpdatePolicy.UPDATE_ALL + " to update the stale score.");
            }
        }
        return analysis;
    }

    @Override
    public <EntityOrElement_, Proposition_> List<RecommendedAssignment<Proposition_, Score_>> recommendAssignment(
            Solution_ solution,
            EntityOrElement_ evaluatedEntityOrElement,
            Function<EntityOrElement_, Proposition_> propositionFunction,
            ScoreAnalysisFetchPolicy scoreAnalysisFetchPolicy) {
        Objects.requireNonNull(solution);
        Objects.requireNonNull(evaluatedEntityOrElement);
        Objects.requireNonNull(propositionFunction);
        Objects.requireNonNull(scoreAnalysisFetchPolicy);
        ConstraintMatchPolicy constraintMatchPolicy = ConstraintMatchPolicy.match(scoreAnalysisFetchPolicy);
        return callScoreDirector(solution, SolutionUpdatePolicy.UPDATE_ALL, scoreDirector -> {
            // Build the original score analysis first
            var originalScoreAnalysis = scoreDirector.buildScoreAnalysis(scoreAnalysisFetchPolicy);
            var recommender =
                    new AssignmentRecommender<Solution_, Score_, RecommendedAssignment<Proposition_, Score_>, EntityOrElement_, Proposition_>(
                            propositionFunction,
                            (index, proposition,
                                    scoreDiff) -> (RecommendedAssignment<Proposition_, Score_>) new DefaultRecommendedAssignment<>(
                                            index, proposition, scoreDiff),
                            scoreAnalysisFetchPolicy,
                            evaluatedEntityOrElement,
                            originalScoreAnalysis);
            return recommender.apply(scoreDirector);
        }, constraintMatchPolicy);
    }

    private <Result_> Result_ callScoreDirector(Solution_ solution,
            SolutionUpdatePolicy solutionUpdatePolicy, Function<InnerScoreDirector<Solution_, Score_>, Result_> function,
            boolean enableConstraintMatch) {
        return callScoreDirector(solution, solutionUpdatePolicy, function,
                enableConstraintMatch ? ConstraintMatchPolicy.ENABLED : ConstraintMatchPolicy.DISABLED);
    }

    private <Result_> Result_ callScoreDirector(Solution_ solution,
            SolutionUpdatePolicy solutionUpdatePolicy, Function<InnerScoreDirector<Solution_, Score_>, Result_> function,
            ConstraintMatchPolicy constraintMatchPolicy) {
        Solution_ nonNullSolution = Objects.requireNonNull(solution);
        try (InnerScoreDirector<Solution_, Score_> scoreDirector =
                scoreDirectorFactory.buildScoreDirector(false, constraintMatchPolicy.isEnabled())) {
            scoreDirector.setWorkingSolution(nonNullSolution); // Init the ScoreDirector first, else NPEs may be thrown.
            if (constraintMatchPolicy.isEnabled() && !scoreDirector.getConstraintMatchPolicy().isEnabled()) {
                throw new IllegalStateException("When constraintMatchPolicy (" + constraintMatchPolicy
                        + ") requires constraint matching but the score director does not support it.");
            }
            if (solutionUpdatePolicy.isShadowVariableUpdateEnabled()) {
                scoreDirector.forceTriggerVariableListeners();
            }
            if (solutionUpdatePolicy.isScoreUpdateEnabled()) {
                scoreDirector.calculateScore();
            }
            return function.apply(scoreDirector);
        }
    }
}
