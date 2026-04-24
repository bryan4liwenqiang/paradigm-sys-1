## ADDED Requirements

### Requirement: 混合输入模型
系统 MUST 支持在会话创建时同时接收自由文本诉求与结构化上下文字段，并将两者统一纳入会话上下文。

#### Scenario: 文本与结构化字段同时提交
- **WHEN** 调用 `POST /api/v1/sessions/start` 时提交 `businessRequest` 与结构化字段
- **THEN** 系统创建会话成功并持久化两类输入
- **AND** 返回的会话详情可反映结构化字段已被记录

### Requirement: 向后兼容
系统 MUST 对旧调用方保持兼容，在仅提交 `businessRequest` 时仍可按既有流程创建与推进会话。

#### Scenario: 仅提交旧字段
- **WHEN** 请求仅包含 `businessRequest` 且不包含任何结构化字段
- **THEN** 系统返回成功并保持现有阶段推进行为
- **AND** 不要求客户端同步升级即可继续使用

### Requirement: 缺口优先追问
系统 MUST 在结构化字段缺失时优先围绕缺失维度发起追问，以提升阶段信息完整度。

#### Scenario: 关键字段缺失
- **WHEN** 会话上下文缺失目标、时限或约束等关键字段
- **THEN** assistant 在当前或后续步骤优先追问缺失项
- **AND** 不因缺失非必填字段而直接拒绝创建会话
