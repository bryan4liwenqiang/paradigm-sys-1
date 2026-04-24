# mapping-recommendation-api

## Purpose

定义 `POST /api/v1/mapping/recommend` 的统一行为契约，覆盖入参校验、推荐链路、排序规则、可解释性输出与错误语义。该规格同时约束 standalone 与 spring 两种运行模式的对外行为。

## Requirements

### Requirement: 请求参数与默认值
系统 MUST 接收 JSON 请求体，并支持 `stageCode` 与业务上下文参数。系统 MUST 在未显式提供 limit 参数时使用系统默认值。

#### Scenario: 提供最小必填参数
- **WHEN** 调用方仅提供 `stageCode`
- **THEN** 系统返回 200，并给出可计算的推荐结果
- **AND** 系统使用默认 limit 进行截断

#### Scenario: 缺少 stageCode
- **WHEN** 请求体缺少 `stageCode` 或为空
- **THEN** 系统返回 400
- **AND** 错误消息可明确指出缺失字段

### Requirement: 分层过滤与打分链路
系统 MUST 按以下链路执行推荐：`Stage -> Paradigm -> Meta -> Methodology -> Method`。每一层 MUST 先进行条件过滤，再进行打分与排序。

#### Scenario: 条件过滤
- **WHEN** 映射规则包含条件表达式
- **THEN** 仅保留条件为 true 的候选项
- **AND** 条件为 false 的候选项不得进入下一层计算

#### Scenario: 打分排序
- **WHEN** 候选项通过条件过滤
- **THEN** 系统按优先级与上下文修正规则计算分值
- **AND** 系统按分值降序排序后执行 limit 截断

### Requirement: Trace 可解释性
系统 MUST 在响应中包含规则可解释性信息，至少包含命中规则列表与因条件不满足被剔除的规则列表。

#### Scenario: 输出命中与剔除规则
- **WHEN** 一次推荐请求完成
- **THEN** 响应包含 `trace.appliedRules`
- **AND** 响应包含 `trace.droppedByCondition`

### Requirement: 运行模式行为一致性
在相同输入与等价规则数据前提下，standalone 与 spring 两种模式 MUST 产生语义一致的推荐结果集合与 trace 结构。

#### Scenario: 一致性对比
- **WHEN** 使用相同 stageCode 与上下文在两种模式下调用接口
- **THEN** 返回字段结构一致
- **AND** 推荐集合语义一致（允许在同分时出现稳定排序差异）

### Requirement: 错误模型
系统 MUST 对客户端错误与服务端错误进行区分，并向调用方返回稳定可解析的错误结构。

#### Scenario: 客户端参数错误
- **WHEN** 请求参数不合法
- **THEN** 返回 4xx 状态码
- **AND** 返回可读错误信息

#### Scenario: 服务端异常
- **WHEN** 内部计算或依赖访问失败
- **THEN** 返回 5xx 状态码
- **AND** 不泄露敏感内部实现细节
