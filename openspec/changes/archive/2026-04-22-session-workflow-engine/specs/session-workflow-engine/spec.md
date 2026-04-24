## MODIFIED Requirements

### Requirement: 会话创建与初始状态
系统 MUST 支持创建新会话，并在创建后初始化阶段工作流、首个步骤状态与首条 assistant 引导消息。系统 MUST 支持“自由文本 + 结构化上下文”的会话初始化方式。

#### Scenario: 创建会话成功（混合输入）
- **WHEN** 调用 `POST /api/v1/sessions/start` 且提供 `businessRequest` 与结构化上下文
- **THEN** 返回 200 与唯一 `sessionId`
- **AND** 当前阶段为首阶段且首个步骤状态为 `CURRENT`
- **AND** 会话上下文中可读取结构化字段

#### Scenario: 缺少 businessRequest
- **WHEN** `businessRequest` 缺失或为空
- **THEN** 返回 400
- **AND** 提示必填约束

### Requirement: 步骤推进状态机
系统 MUST 在用户提交消息后推进当前步骤状态，并保持 `CURRENT/PENDING/DONE` 状态转换可追踪且可重建。系统 MUST 在信息不足时保持当前步骤并继续追问，不得直接推进。

#### Scenario: 正常推进一步
- **WHEN** 当前步骤存在且用户提交信息满足步骤最小要求
- **THEN** 当前步骤标记为 `DONE`
- **AND** 下一步骤标记为 `CURRENT`
- **AND** 写入 assistant 下一步追问消息

#### Scenario: 信息不足不推进
- **WHEN** 用户回复未满足当前步骤最小信息要求
- **THEN** 当前步骤保持 `CURRENT`
- **AND** assistant 生成针对缺口的补充问题

#### Scenario: 当前阶段最后一步完成
- **WHEN** 用户完成当前阶段最后一个步骤
- **THEN** 系统生成阶段总结
- **AND** 自动推进到下一阶段并发出阶段切换提示

### Requirement: 消息与会话可追溯
系统 MUST 按时间顺序保存用户与 assistant 消息，并在查询会话时返回完整上下文。系统 MUST 返回当前阶段的解释性信息，支持前端展示“目标、事实、缺口、建议”。

#### Scenario: 查询会话详情
- **WHEN** 调用会话查询接口
- **THEN** 返回当前阶段、阶段明细、步骤状态与消息列表
- **AND** 消息条目包含角色、内容与时间戳
- **AND** 返回可展示的阶段解释字段
