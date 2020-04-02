-- Junk any existing data
truncate table assessmentcomponentexamschedule;
alter table assessmentcomponentexamschedule add column location_sequence varchar not null;

drop index idx_assessmentcomponentexamschedule;
create unique index idx_assessmentcomponentexamschedule on assessmentcomponentexamschedule (profile_code, slot_id, sequence, location_sequence);

create table assessmentcomponentexamschedulestudent (
  id varchar not null,
  schedule_id varchar, -- Should be not null but Hibernate
  seat_number int,
  university_id varchar not null,
  spr_code varchar not null,
  occurrence varchar not null,
  constraint pk_assessmentcomponentexamschedulestudent primary key (id),
  constraint fk_assessmentcomponentexamschedulestudent_schedule foreign key (schedule_id) references assessmentcomponentexamschedule
);

create index idx_assessmentcomponentexamschedulestudent_universityid on assessmentcomponentexamschedulestudent (university_id);
create index idx_assessmentcomponentexamschedulestudent_sprcode on assessmentcomponentexamschedulestudent (spr_code);

-- Student can't be in more than one seat
create unique index ck_assessmentcomponentexamschedulestudent on assessmentcomponentexamschedulestudent (university_id, spr_code, schedule_id);
