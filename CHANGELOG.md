# Changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.1.0] - 2026-04-02

### Added

**ScoreAnalysis 体系（借鉴 Timefold）**
- `ScoreAnalysis` record：JSON 友好的分数分解结果，支持 `diff()` 比较两个分析结果
- `ConstraintAnalysis` record：单个约束的得分贡献分析（权重、实际分数、匹配详情）
- `MatchAnalysis` record：单个约束匹配的得分详情
- `ConstraintRef` value class：约束引用标识（package + name）
- `ScoreAnalysisFetchPolicy` 枚举：控制分析计算深度（`FETCH_ALL` / `FETCH_SHALLOW` / `FETCH_MATCH_COUNT`）
- `SolutionManager.analyze()` 方法（3 个重载）：提供比 `explain()` 更轻量的按约束得分分析

**内部 API 增强**
- `ConstraintMatchPolicy` 枚举：替代 `boolean isConstraintMatchEnabled()`，支持三态（DISABLED / ENABLED_WITHOUT_JUSTIFICATIONS / ENABLED）
- `InnerScore` record：绑定分数与未赋值计数，支持初始化状态感知
- `InnerScoreDirector.setWorkingSolutionWithoutUpdatingShadows()`：分离设置解与更新影子变量

**recommendAssignment（借鉴 Timefold）**
- `RecommendedAssignment` 接口：推荐分配结果（proposition + scoreAnalysisDiff）
- `SolutionManager.recommendAssignment()` 方法：枚举实体所有可能分配并返回按分数排序的推荐列表
- 支持单变量和多变量实体的推荐

**updateShadowVariables**
- `SolutionManager.updateShadowVariables()` 静态方法：无需完整 SolverFactory 配置即可更新影子变量
- `ShadowVariableUpdateHelper`：处理 BASIC / CUSTOM_LISTENER / CASCADING_UPDATE 等影子变量类型

### Changed
- 切换项目版本从 9.44.0.Final 到独立版本线
- 明确 JDK 支持边界：当前仅支持 JDK 17

### Notes
- 源码基线来自 OptaPlanner 9.44.0.Final
- 所有新增 API 均为纯增量变更，无破坏性变更
- 1775 tests 全部通过，0 failures
