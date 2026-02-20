create table if not exists projects (
  id bigserial primary key,
  code varchar(64) unique not null,
  name varchar(255) not null,
  domain varchar(64),
  compliance_level varchar(64),
  team_maturity varchar(64),
  budget_level varchar(64),
  timeline_level varchar(64),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists stages (
  id bigserial primary key,
  code varchar(64) unique not null,
  name varchar(128) not null,
  order_no int not null unique
);

create table if not exists paradigms (
  id bigserial primary key,
  code varchar(64) unique not null,
  name varchar(128) not null,
  description text
);

create table if not exists meta_methodologies (
  id bigserial primary key,
  code varchar(64) unique not null,
  name varchar(128) not null,
  meta_goal text,
  meta_object text,
  meta_mechanism text,
  meta_constraint text,
  meta_metric text,
  meta_gate text
);

create table if not exists methodologies (
  id bigserial primary key,
  code varchar(64) unique not null,
  name varchar(128) not null,
  version varchar(32) not null default '1.0.0',
  template_ref text
);

create table if not exists methods (
  id bigserial primary key,
  code varchar(64) unique not null,
  name varchar(128) not null,
  method_type varchar(64),
  tool_ref varchar(128),
  input_schema jsonb,
  output_schema jsonb
);

create table if not exists stage_paradigm_map (
  id bigserial primary key,
  stage_id bigint not null references stages(id),
  paradigm_id bigint not null references paradigms(id),
  priority int not null default 100,
  condition_expr text,
  enabled boolean not null default true,
  unique(stage_id, paradigm_id)
);

create table if not exists paradigm_meta_map (
  id bigserial primary key,
  paradigm_id bigint not null references paradigms(id),
  meta_methodology_id bigint not null references meta_methodologies(id),
  priority int not null default 100,
  condition_expr text,
  enabled boolean not null default true,
  unique(paradigm_id, meta_methodology_id)
);

create table if not exists meta_methodology_map (
  id bigserial primary key,
  meta_methodology_id bigint not null references meta_methodologies(id),
  methodology_id bigint not null references methodologies(id),
  priority int not null default 100,
  condition_expr text,
  enabled boolean not null default true,
  unique(meta_methodology_id, methodology_id)
);

create table if not exists methodology_method_map (
  id bigserial primary key,
  methodology_id bigint not null references methodologies(id),
  method_id bigint not null references methods(id),
  seq_no int not null,
  required_flag boolean not null default true,
  condition_expr text,
  enabled boolean not null default true,
  unique(methodology_id, method_id, seq_no)
);

create index if not exists idx_stage_paradigm_stage on stage_paradigm_map(stage_id, enabled, priority);
create index if not exists idx_paradigm_meta_paradigm on paradigm_meta_map(paradigm_id, enabled, priority);
create index if not exists idx_meta_methodology_meta on meta_methodology_map(meta_methodology_id, enabled, priority);
create index if not exists idx_methodology_method_m on methodology_method_map(methodology_id, enabled, seq_no);

insert into stages (code, name, order_no) values
  ('requirements', '需求阶段', 1),
  ('design', '设计阶段', 2),
  ('planning', '规划阶段', 3),
  ('development', '开发阶段', 4),
  ('testing', '测试阶段', 5),
  ('deployment', '部署阶段', 6),
  ('implementation', '实施阶段', 7),
  ('operations', '运维阶段', 8)
on conflict (code) do nothing;
