# OptaPlanner 独立化状态说明

## 版本信息

- **源码基线**: OptaPlanner 9.44.0.Final
- **当前版本**: 1.1.0
- **独立版本线起点**: 1.0.0

## JDK 支持边界

- **正式支持**: JDK 17
- **暂不支持**: JDK 21

当前版本仅支持 JDK 17。 JDK 21 的兼容性工作（包括 Byte Buddy、动态 agent、测试框架兼容）不在本轮范围内。

## 构建验证

快速构建验证（跳过测试）:
```bash
cd optaplanner
mvn clean install -Dquickly
```

运行代表性测试:
```bash
# Core 测试
mvn test -pl core/optaplanner-core

# Benchmark 测试
mvn test -pl optaplanner-benchmark

# Test 工具测试
mvn test -pl optaplanner-test
```

## CI 说明

当前 CI 配置 (`.github/workflows/ci.yml`):
- 触发方式: `pull_request` 到 `main` 分支，或 `workflow_dispatch` 手动触发
- 运行环境: Ubuntu + JDK 17
- 验证范围: core、benchmark、test 模块的代表性测试

## 与上游的差异

1. 版本号已切换到独立版本线
2. 移除了对上游 kiegroup 仓库的部分 CI 依赖
3. 仅支持 JDK 17，暂未引入 JDK 21 兼容性
4. Timefold 特性迁移（阶段 1-3）
5. Bug 修复（ConstraintAnalysis.negate, diff 一致性校验、操作符优先级）

## 1.1.0 变更摘要

### Added
- `ScoreAnalysis` record: JSON 友好的分数分解（支持 `diff()` 比较）
- `ConstraintAnalysis` record: 单个约束的得分贡献分析
- `MatchAnalysis` record: 单个约束匹配的得分详情
- `ConstraintRef` value class: 约束引用标识
- `ScoreAnalysisFetchPolicy` 枚举: 控制分析计算深度
- `ConstraintMatchPolicy` 三态枚举: 替代 boolean 约束匹配开关
- `InnerScore` record: 分数与初始化状态绑定
- `InnerScoreDirector.setWorkingSolutionWithoutUpdatingShadows()` 方法
- `SolutionManager.analyze()` 方法（3 个重载）: 按约束分数分析
- `SolutionManager.recommendAssignment()` 方法: 推荐实体分配
- `SolutionManager.updateShadowVariables()` 静态方法: 无需 SolverFactory 更新影子变量
- `ShadowVariableUpdateHelper` 影子变量更新工具类
- `RecommendedAssignment` 接口 + `DefaultRecommendedAssignment` 实现
- `RecommendationConstructor` 内部工厂接口
- `AssignmentRecommender` 核心推荐逻辑（基础变量 + 列表变量）

### Fixed
- `ConstraintAnalysis.negate()` matchCount 不应取反，保持原值
- `ConstraintAnalysis.diff()` 增加 matches 一致性校验（混合 FetchPolicy 时抛异常）
- `ScoreAnalysis.diff()` 操作符优先级加显式括号

- `AssignmentRecommender` 列表变量恢复后缺少 `triggerVariableListeners()` (已存在，无需修复)

## 后续计划

见 `docs/optaplanner-fork-roadmap-zh.md`
