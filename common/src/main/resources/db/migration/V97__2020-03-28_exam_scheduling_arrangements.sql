alter table studentcoursedetails
    add column special_exam_arrangements boolean,
    add column special_exam_arrangements_location varchar,
    add column special_exam_arrangements_extra_time varchar;

create table assessmentcomponentexamschedule (
  id varchar not null,
  module_code varchar not null,
  assessmentcomponent_sequence varchar not null,
  profile_code varchar not null,
  slot_id varchar not null,
  sequence varchar not null,
  academic_year smallint not null,
  start_time timestamp(3) not null,
  exam_paper_code varchar not null,
  exam_paper_section varchar,
  location varchar,
  constraint pk_assessmentcomponentexamschedule primary key (id)
);

-- Loose foreign key on assessmentcomponent, but don't want to deal with managing deletion here so not enforced
create index idx_assessmentcomponentexamschedule_assessmentcomponent on assessmentcomponentexamschedule (module_code, assessmentcomponent_sequence);
create unique index idx_assessmentcomponentexamschedule on assessmentcomponentexamschedule (profile_code, slot_id, sequence);
