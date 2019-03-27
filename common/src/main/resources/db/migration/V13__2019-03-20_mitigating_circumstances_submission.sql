create sequence mit_circ_sequence start 1000 increment by 1;

create table MitigatingCircumstancesSubmission(
  id varchar(255) primary key,
  key integer not null,
  createdDate timestamp(6) not null,
  creator varchar(255) not null,
  universityId varchar(255) not null,
  issueType varchar(255),
  issueTypeDetails bytea,
  startDate timestamp(6) not null,
  endDate timestamp(6) not null,
  reason bytea not null,
  constraint pk_mitigatingcircumstancessubmission primary key (id),
  constraint fk_mitcircssub_student foreign key (universityId) references member (id) on delete restrict
);

create unique index idx_mitcircssub_key on MitigatingCircumstancesSubmission (key);
create unique index idx_mitcircssub_student on MitigatingCircumstancesSubmission (universityId);