## MODIFIED Requirements

### Requirement: 会话创建与初始状态
系统 MUST 支持创建新会话，并在创建后初始化阶段工作流、首个步骤状态与首条 assistant 引导消息。系统 MUST 支持“自由文本 + 结构化上下文”的会话初始化方式。系统 MUST 正确消费来自“下拉选择 + 手动输入”双模式控件的输入值。

#### Scenario: 创建会话成功（混合输入）
- **WHEN** 调用 `POST /api/v1/sessions/start` 且提供 `businessRequest` 与结构化上下文
- **THEN** 返回 200 与唯一 `sessionId`
- **AND** 当前阶段为首阶段且首个步骤状态为 `CURRENT`
- **AND** 会话上下文中可读取结构化字段

#### Scenario: 缺少 businessRequest
- **WHEN** `businessRequest` 缺失或为空
- **THEN** 返回 400
- **AND** 提示必填约束

#### Scenario: 双模式输入驱动会话初始化
- **WHEN** 前端通过双模式控件提交启动请求
- **THEN** 会话引擎应按统一字段语义初始化上下文
- **AND** 后续阶段步骤推进不依赖字段来源模式
