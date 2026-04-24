# dual-mode-input-controls Specification

## Purpose
TBD - created by archiving change input-combobox. Update Purpose after archive.
## Requirements
### Requirement: 关键输入字段支持双模式控件
系统 MUST 为关键输入字段提供“下拉选择 + 手动输入”双模式控件，并允许用户在两种模式之间切换。

#### Scenario: 使用下拉选项快速填充
- **WHEN** 用户点击字段并选择预置选项
- **THEN** 字段值应立即更新为所选标准值
- **AND** 字段状态应标记为“选项模式”

#### Scenario: 切换到手动输入
- **WHEN** 用户选择“手动输入”模式并输入自定义文本
- **THEN** 字段应保存用户输入文本
- **AND** 字段状态应标记为“手动模式”

### Requirement: 双模式状态可恢复
系统 MUST 在页面刷新或会话恢复后还原字段的值与输入模式，避免用户重复录入。

#### Scenario: 草稿恢复
- **WHEN** 用户此前已保存草稿并重新进入页面
- **THEN** 字段值应恢复为最近一次输入
- **AND** 字段模式应恢复为最近一次使用的模式

