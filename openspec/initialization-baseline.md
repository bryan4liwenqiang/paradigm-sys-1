# OpenSpec 初始化基线

## 目标

为一个已存在代码的工程建立 OpenSpec 引入基线。该工程包含两种运行模式（standalone + Spring Boot），并且业务/领域文档仍在持续完善中。

## 当前状态快照

- OpenSpec 已接入但仍是最小状态：仅有 `openspec/config.yaml`。
- 当前无活跃变更：`openspec list --json` 返回 `{"changes":[]}`。
- 当前无 specs：`openspec list --specs --json` 显示无规格。
- 当前校验为空属于预期（尚未创建 change/spec 产物）。

## 仓库结构（与 OpenSpec 相关）

- `standalone/`
  - 单文件 Java HTTP 服务（`MappingServer.java`）
  - 静态 Web 前端（`web/index.html`, `web/app.js`, `web/styles.css`）
  - 本地会话持久化（默认 `standalone/data/sessions.bin`）
- `src/main/java/...`（Spring Boot 模式）
  - REST 控制器/服务/仓储分层
  - JPA 实体与仓储接口
- `docs/wms/`
  - 领域需求与范围文档（可作为初始 specs 的主要输入）

## 候选能力规格（第一阶段）

1. `mapping-recommendation-api`
   - 范围：`/api/v1/mapping/recommend` 的行为与接口契约。
   - 要点：明确评分、过滤逻辑与 trace 输出保证。
2. `session-workflow-engine`
   - 范围：会话生命周期、阶段推进、步骤完成、刷新行为。
3. `runtime-mode-parity`
   - 范围：standalone 与 spring 需要保持一致的行为，以及允许差异的边界。
4. `configuration-and-fallbacks`
   - 范围：环境变量、默认值、超时行为，以及 OpenRouter 不可用时的降级策略。

## 建议优先创建的变更提案

1. `formalize-api-contracts`
   - 将当前接口行为沉淀为明确的 spec 要求。
2. `stage-workflow-observability`
   - 增加可追踪性/错误可观测性要求与验收检查项。
3. `seed-vs-db-consistency`
   - 定义 standalone 种子映射与数据库映射的一致性要求。

## 建议尽早纳入规格的风险点

- standalone 与 spring 实现之间出现契约漂移。
- 业务规则隐式散落在内联逻辑/种子数据中，难以统一维护。
- 各接口的类型校验与错误模型一致性不足。
- 文件型会话持久化的兼容性与损坏恢复策略未明确定义。

## 可直接执行的初始化清单

- [x] 在 `openspec/config.yaml` 补全项目上下文与 artifact 规则。
- [ ] 创建第一个能力规格：`mapping-recommendation-api`。
- [ ] 创建第二个能力规格：`session-workflow-engine`。
- [ ] 在初始 specs 创建后执行 `openspec validate --specs`。
- [ ] 围绕明确改进目标创建第一个 change proposal。

## 推荐命令顺序

```bash
openspec list --json
openspec list --specs --json
openspec spec new mapping-recommendation-api
openspec spec new session-workflow-engine
openspec validate --specs
```
