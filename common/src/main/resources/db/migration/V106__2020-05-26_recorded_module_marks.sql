create table recordedmoduleregistration (
  id varchar not null,
  hib_version numeric default 0,
  module_code varchar not null,
  cats numeric(5,2),
  academic_year smallint not null,
  assessment_group varchar not null,
  occurrence varchar not null,
  scj_code varchar not null,
  needs_writing_to_sits boolean default false,
  last_written_to_sits timestamp(6),
  constraint pk_recordedmoduleregistration primary key (id)
);

create index idx_recordedmoduleregistration_um on recordedmoduleregistration (module_code, assessment_group, occurrence, academic_year);
create unique index ck_recordedmoduleregistration on recordedmoduleregistration (module_code, assessment_group, occurrence, academic_year, scj_code);

create table recordedmodulemark (
  id varchar not null,
  recorded_module_registration_id varchar not null,
  mark int,
  grade varchar,
  module_result varchar,
  comments text,
  updated_by varchar not null,
  updated_date timestamp(6) not null,
  constraint pk_recordedmodulemark primary key (id),
  constraint fk_recordedmodulemark foreign key (recorded_module_registration_id) references recordedmoduleregistration
);

create index idx_recordedmodulemark_registration on recordedmodulemark (recorded_module_registration_id);
