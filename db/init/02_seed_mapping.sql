-- Seed: requirements stage mapping chain
-- Paradigm -> MetaMethodology -> Methodology -> Method

insert into paradigms (code, name, description) values
  ('VALUE_DRIVEN', '价值驱动范式', '以业务价值和收益优先级驱动需求识别与排序'),
  ('DOMAIN_DRIVEN', '领域驱动范式', '以领域语言、边界上下文和业务规则驱动需求建模'),
  ('RULE_CONSTRAINT', '规则约束范式', '以业务规则和合规约束驱动需求定义')
on conflict (code) do nothing;

insert into meta_methodologies (code, name, meta_goal, meta_object, meta_mechanism, meta_constraint, meta_metric, meta_gate) values
  (
    'VALUE_DISCOVERY_META',
    '价值发现元方法论',
    '最大化业务价值',
    '需求候选项',
    '价值评分与优先级排序',
    '时间与预算约束',
    '价值点数与交付周期',
    '高价值需求优先进入设计'
  ),
  (
    'SEMANTIC_MODEL_META',
    '语义建模元方法论',
    '形成一致业务语义',
    '实体/事件/规则',
    '术语抽取与领域建模',
    '领域边界清晰',
    '语义冲突率',
    '需求术语评审通过'
  ),
  (
    'COMPLIANCE_META',
    '合规建模元方法论',
    '降低合规风险',
    '规则与约束条款',
    '规则结构化与校验',
    '监管合规条款',
    '合规覆盖率',
    '关键约束必须落地'
  )
on conflict (code) do nothing;

insert into methodologies (code, name, version, template_ref) values
  ('WSJF_METHODLOGY', 'WSJF优先级方法论', '1.0.0', 'tpl://methodology/wsjf'),
  ('EVENT_STORMING_METHODLOGY', '事件风暴方法论', '1.0.0', 'tpl://methodology/event-storming'),
  ('RULE_TABLE_METHODLOGY', '规则表方法论', '1.0.0', 'tpl://methodology/rule-table')
on conflict (code) do nothing;

insert into methods (code, name, method_type, tool_ref, input_schema, output_schema) values
  (
    'INTERVIEW_METHOD',
    '结构化访谈',
    'elicitation',
    'tool://interview',
    '{"inputs":["stakeholders","business_goals"]}'::jsonb,
    '{"outputs":["raw_requirements"]}'::jsonb
  ),
  (
    'VALUE_SCORING_METHOD',
    '价值评分',
    'prioritization',
    'tool://wsjf',
    '{"inputs":["requirements","business_value","risk_reduction","job_size"]}'::jsonb,
    '{"outputs":["priority_rank"]}'::jsonb
  ),
  (
    'WORKSHOP_METHOD',
    '需求工作坊',
    'collaboration',
    'tool://workshop',
    '{"inputs":["stakeholders","scope"]}'::jsonb,
    '{"outputs":["aligned_scope"]}'::jsonb
  ),
  (
    'EVENT_MODELING_METHOD',
    '事件建模',
    'modeling',
    'tool://event-storming',
    '{"inputs":["business_events","actors"]}'::jsonb,
    '{"outputs":["domain_model"]}'::jsonb
  ),
  (
    'RULE_TABLE_METHOD',
    '决策表梳理',
    'analysis',
    'tool://rule-table',
    '{"inputs":["rules","conditions","actions"]}'::jsonb,
    '{"outputs":["decision_table"]}'::jsonb
  ),
  (
    'COMPLIANCE_CHECK_METHOD',
    '合规约束检查',
    'governance',
    'tool://compliance-check',
    '{"inputs":["requirements","regulations"]}'::jsonb,
    '{"outputs":["compliance_findings"]}'::jsonb
  )
on conflict (code) do nothing;

-- requirements -> paradigms
insert into stage_paradigm_map (stage_id, paradigm_id, priority, condition_expr, enabled)
select s.id, p.id, m.priority, m.condition_expr, true
from (
  values
    ('requirements', 'VALUE_DRIVEN', 100, 'timelineLevel == "tight"'),
    ('requirements', 'DOMAIN_DRIVEN', 120, ''),
    ('requirements', 'RULE_CONSTRAINT', 80, 'complianceLevel == "L3"')
) as m(stage_code, paradigm_code, priority, condition_expr)
join stages s on s.code = m.stage_code
join paradigms p on p.code = m.paradigm_code
on conflict (stage_id, paradigm_id) do nothing;

-- paradigm -> meta
insert into paradigm_meta_map (paradigm_id, meta_methodology_id, priority, condition_expr, enabled)
select p.id, mm.id, m.priority, m.condition_expr, true
from (
  values
    ('VALUE_DRIVEN', 'VALUE_DISCOVERY_META', 100, ''),
    ('DOMAIN_DRIVEN', 'SEMANTIC_MODEL_META', 100, ''),
    ('RULE_CONSTRAINT', 'COMPLIANCE_META', 90, 'complianceLevel == "L3"')
) as m(paradigm_code, meta_code, priority, condition_expr)
join paradigms p on p.code = m.paradigm_code
join meta_methodologies mm on mm.code = m.meta_code
on conflict (paradigm_id, meta_methodology_id) do nothing;

-- meta -> methodology
insert into meta_methodology_map (meta_methodology_id, methodology_id, priority, condition_expr, enabled)
select mm.id, md.id, m.priority, m.condition_expr, true
from (
  values
    ('VALUE_DISCOVERY_META', 'WSJF_METHODLOGY', 100, ''),
    ('SEMANTIC_MODEL_META', 'EVENT_STORMING_METHODLOGY', 100, ''),
    ('COMPLIANCE_META', 'RULE_TABLE_METHODLOGY', 80, 'domain == "finance"')
) as m(meta_code, methodology_code, priority, condition_expr)
join meta_methodologies mm on mm.code = m.meta_code
join methodologies md on md.code = m.methodology_code
on conflict (meta_methodology_id, methodology_id) do nothing;

-- methodology -> methods
insert into methodology_method_map (methodology_id, method_id, seq_no, required_flag, condition_expr, enabled)
select md.id, me.id, m.seq_no, m.required_flag, m.condition_expr, true
from (
  values
    ('WSJF_METHODLOGY', 'INTERVIEW_METHOD', 1, true, ''),
    ('WSJF_METHODLOGY', 'VALUE_SCORING_METHOD', 2, true, ''),
    ('EVENT_STORMING_METHODLOGY', 'WORKSHOP_METHOD', 1, true, ''),
    ('EVENT_STORMING_METHODLOGY', 'EVENT_MODELING_METHOD', 2, true, ''),
    ('RULE_TABLE_METHODLOGY', 'RULE_TABLE_METHOD', 1, true, ''),
    ('RULE_TABLE_METHODLOGY', 'COMPLIANCE_CHECK_METHOD', 2, true, 'complianceLevel == "L3"')
) as m(methodology_code, method_code, seq_no, required_flag, condition_expr)
join methodologies md on md.code = m.methodology_code
join methods me on me.code = m.method_code
on conflict (methodology_id, method_id, seq_no) do nothing;

-- Optional demo project context
insert into projects (code, name, domain, compliance_level, team_maturity, budget_level, timeline_level)
values ('DEMO_PROJ_001', '演示项目-智能需求平台', 'finance', 'L3', 'medium', 'medium', 'tight')
on conflict (code) do nothing;
