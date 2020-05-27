create table variableassessmentweightingrule (
  id varchar not null,
  module_code varchar not null,
  assessment_group varchar not null,
  rule_sequence varchar not null,
  assessment_type varchar not null,
  weighting int not null,
  constraint pk_variableassessmentweightingrule primary key (id)
);

create index idx_variableassessmentweightingrule_moduleag on variableassessmentweightingrule (module_code, assessment_group);
create unique index ck_variableassessmentweightingrule on variableassessmentweightingrule (module_code, assessment_group, rule_sequence);
