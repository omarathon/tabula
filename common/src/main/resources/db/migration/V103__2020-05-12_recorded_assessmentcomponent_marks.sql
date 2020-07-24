create table recordedassessmentcomponentstudent (
  id varchar not null,
  hib_version numeric default 0,
  module_code varchar not null,
  assessment_group varchar not null,
  occurrence varchar not null,
  sequence varchar not null,
  academic_year smallint not null,
  university_id varchar not null,
  needs_writing_to_sits boolean default false,
  last_written_to_sits timestamp(6),
  constraint pk_recordedassessmentcomponentstudent primary key (id)
);

create index idx_recordedassessmentcomponentstudent_uag on recordedassessmentcomponentstudent (module_code, assessment_group, occurrence, sequence, academic_year);
create unique index ck_recordedassessmentcomponentstudent on recordedassessmentcomponentstudent (module_code, assessment_group, occurrence, sequence, academic_year, university_id);

create table recordedassessmentcomponentstudentmark (
  id varchar not null,
  recorded_assessment_component_student_id varchar not null,
  mark int not null,
  grade varchar,
  comments text,
  updated_by varchar not null,
  updated_date timestamp(6) not null,
  constraint pk_recordedassessmentcomponentstudentmark primary key (id),
  constraint fk_recordedassessmentcomponentstudentmark foreign key (recorded_assessment_component_student_id) references recordedassessmentcomponentstudent
);

create index idx_recordedassessmentcomponentstudentmark_student on recordedassessmentcomponentstudentmark (recorded_assessment_component_student_id);
