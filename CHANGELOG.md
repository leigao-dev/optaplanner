# Changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Changed
- 切换项目版本从 9.44.0.Final 到 1.0.0-SNAPSHOT，建立独立版本线
- 明确 JDK 支持边界：当前仅支持 JDK 17

### Added
- 新增 CHANGELOG.md 作为发布记录入口
- 新增最小化 CI workflow，支持 pull_request 和 workflow_dispatch 触发

### Security
- 移除对上游 kiegroup 仓库的依赖引用

### Notes
- 当前源码基线来自 OptaPlanner 9.44.0.Final
- 当前独立版本线从 1.0.0-SNAPSHOT 开始
- 当前只支持 JDK 17，暂不引入 JDK 21 兼容性
