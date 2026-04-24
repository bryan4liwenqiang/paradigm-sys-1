# stage-explainability-view Specification

## Purpose
TBD - created by archiving change session-workflow-engine. Update Purpose after archive.
## Requirements
### Requirement: 阶段解释信息输出
系统 MUST 在会话查询与消息推进后输出阶段解释信息，至少包含阶段目标、已确认事实、关键缺口与下一步建议。

#### Scenario: 查询会话详情时返回解释信息
- **WHEN** 调用会话详情接口获取当前状态
- **THEN** 响应中包含阶段解释信息结构
- **AND** 解释字段可被前端稳定解析与展示

### Requirement: 阶段完成摘要与未决项
系统 MUST 在阶段完成切换时输出阶段完成摘要与未决项，帮助用户理解进入下一阶段前的上下文。

#### Scenario: 阶段切换后查看消息历史
- **WHEN** 当前阶段完成并推进到下一阶段
- **THEN** assistant 输出本阶段完成摘要
- **AND** assistant 明确列出未决项或待补充信息

### Requirement: 解释字段降级可用
系统 MUST 在 AI 能力不可用时仍输出可理解的解释字段内容，避免阶段视图信息缺失。

#### Scenario: 未配置 OpenRouter 时获取阶段解释
- **WHEN** 环境未配置 `OPENROUTER_API_KEY`
- **THEN** 响应仍包含基础解释字段
- **AND** 字段内容由规则模板生成而非空值

