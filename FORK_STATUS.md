# OptaPlanner 独立化状态说明

## 版本信息

- **源码基线**: OptaPlanner 9.44.0.Final
- **当前版本**: 1.0.0-SNAPSHOT
- **独立版本线起点**: 1.0.0-SNAPSHOT

## JDK 支持边界

- **正式支持**: JDK 17
- **暂不支持**: JDK 21

当前版本仅支持 JDK 17。JDK 21 的兼容性工作（包括 Byte Buddy、动态 agent、测试框架兼容）不在本轮范围内。

## 构建验证

快速构建验证（跳过测试）：
```bash
cd optaplanner
mvn clean install -Dquickly
```

运行代表性测试：
```bash
# Core 测试
mvn test -pl core/optaplanner-core

# Benchmark 测试
mvn test -pl optaplanner-benchmark

# Test 工具测试
mvn test -pl optaplanner-test
```

## CI 说明

当前 CI 配置 (`.github/workflows/ci.yml`)：
- 触发方式：`pull_request` 到 `main` 分支，或 `workflow_dispatch` 手动触发
- 运行环境：Ubuntu + JDK 17
- 验证范围：core、benchmark、test 模块的代表性测试

## 与上游的差异

1. 版本号已切换到独立版本线（1.0.0-SNAPSHOT）
2. 移除了对上游 kiegroup 仓库的部分 CI 依赖
3. 仅支持 JDK 17，暂未引入 JDK 21 兼容性
4. 不包含功能改造和 Timefold 特性迁移

## 后续计划

见 `docs/optaplanner-fork-roadmap-zh.md`
