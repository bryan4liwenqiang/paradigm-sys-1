## Why

当前会话引擎在“业务诉求 -> 阶段互动 -> 阶段展示”链路上不够灵活：输入以自由文本为主、阶段步骤偏固定、阶段解释信息不足，导致复杂场景下澄清效率和信息完整度不稳定。现在需要把会话引擎升级为“结构化输入 + 动态编排 + 可解释展示”，支撑更细致的需求澄清。

## What Changes

- 增强会话输入模型：在保留 `businessRequest` 的前提下，引入可选结构化上下文（目标、时限、角色、约束等）。
- 增强步骤推进逻辑：引入“必答步骤 + 条件分支步骤”与步骤完成判定，支持信息不足时继续追问。
- 增强阶段展示语义：输出阶段目标、已确认事实、关键缺口、下一步建议等解释性信息。
- 保持现有会话主流程和 API 主路径稳定，优先以向后兼容方式扩展字段。

## Capabilities

### New Capabilities

- `session-input-modeling`: 定义会话输入从纯文本到“文本+结构化画像”的扩展行为与兼容策略。
- `stage-explainability-view`: 定义阶段解释信息的输出语义与展示契约。

### Modified Capabilities

- `session-workflow-engine`: 扩展步骤编排与完成判定要求，支持动态分支与“信息不足不推进”机制。

## Impact

- `standalone/MappingServer.java`: 会话创建、步骤推进、阶段总结与响应组装逻辑。
- `standalone/web/app.js`: 启动表单、阶段视图渲染、缺口与建议展示。
- `standalone/web/index.html`: 启动会话输入区与阶段解释区结构。
- `openspec/specs/session-workflow-engine/spec.md`: 需求行为更新与场景补充。
- API 影响：`POST /api/v1/sessions/start` 与会话查询响应可能增加可选字段（向后兼容，不移除既有字段）。

### Non-goals

- 不在本次变更中重写推荐引擎打分模型。
- 不在本次变更中引入新的外部持久化存储（如 Redis）。
- 不在本次变更中重构 Spring 模式全部实现，仅先明确契约与行为边界。
